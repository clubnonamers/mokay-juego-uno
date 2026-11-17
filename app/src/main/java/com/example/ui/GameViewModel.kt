package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SaveRepository
import com.example.data.SaveSlot
import com.example.game.BiomeAudioManager
import com.example.game.CraftableTool
import com.example.game.GameSessionState
import com.example.game.GameSettings
import com.example.game.GestureRecognizer
import com.example.game.RetroSoundEngine
import com.example.game.WorldSimulation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(
    private val saveRepository: SaveRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _sessionState = MutableStateFlow(GameSessionState())
    val sessionState: StateFlow<GameSessionState> = _sessionState.asStateFlow()

    private val _saveSlots = MutableStateFlow<List<SaveSlot>>(emptyList())
    val saveSlots: StateFlow<List<SaveSlot>> = _saveSlots.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _openMenuTab = MutableStateFlow<MenuTab?>(null)
    val openMenuTab: StateFlow<MenuTab?> = _openMenuTab.asStateFlow()

    private var joystickX: Float = 0f
    private var joystickY: Float = 0f
    private var defendingHeld: Boolean = false
    private var autoLoadedInitialSlot = false

    init {
        BiomeAudioManager.setZone(_sessionState.value.currentZone)
        BiomeAudioManager.start()
        observeCloudSaves()
        startGameLoop()
    }

    override fun onCleared() {
        BiomeAudioManager.stop()
        super.onCleared()
    }

    private fun observeCloudSaves() {
        viewModelScope.launch {
            saveRepository.observeSaveSlots(currentUserId)
                .catch { e ->
                    _statusMessage.value = e.localizedMessage ?: "Error al sincronizar guardado."
                }
                .collect { slots ->
                    _saveSlots.value = slots
                    if (!autoLoadedInitialSlot && slots.isNotEmpty()) {
                        autoLoadedInitialSlot = true
                        val quick = slots.firstOrNull { it.id == "quicksave" } ?: slots.first()
                        _sessionState.value = GameSessionState.fromSaveSlot(quick)
                    }
                }
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            var lastTimeMs = System.currentTimeMillis()
            var frameTick = 0
            while (isActive) {
                val nowMs = System.currentTimeMillis()
                val dt = ((nowMs - lastTimeMs).coerceIn(8L, 48L)) / 1000f
                lastTimeMs = nowMs
                frameTick++

                BiomeAudioManager.setZone(_sessionState.value.currentZone)
                BiomeAudioManager.isPaused = _openMenuTab.value != null

                if (_openMenuTab.value == null && _sessionState.value.tvInfoDialog == null) {
                    _sessionState.update { cur ->
                        val stepped = WorldSimulation.step(
                            state = cur,
                            inputX = joystickX,
                            inputY = joystickY,
                            defending = defendingHeld,
                            dtSeconds = dt,
                            nowMs = nowMs
                        )
                        val nextPlayTime = if (frameTick % 60 == 0) stepped.playTimeSeconds + 1L else stepped.playTimeSeconds
                        val nextAnim = if (frameTick % 12 == 0) (stepped.animFrame + 1) % 2 else stepped.animFrame
                        stepped.copy(playTimeSeconds = nextPlayTime, animFrame = nextAnim)
                    }
                }
                delay(16L)
            }
        }
    }

    fun setJoystick(x: Float, y: Float) {
        joystickX = x.coerceIn(-1f, 1f)
        joystickY = y.coerceIn(-1f, 1f)
    }

    fun setDefending(held: Boolean) {
        defendingHeld = held
    }

    fun jumpPressed() {
        _sessionState.update { WorldSimulation.performJump(it) }
    }

    fun dashPressed() {
        val nowMs = System.currentTimeMillis()
        _sessionState.update { WorldSimulation.performDash(it, nowMs) }
    }

    fun attackPressed() {
        val nowMs = System.currentTimeMillis()
        _sessionState.update { WorldSimulation.performAttack(it, null, nowMs) }
    }

    fun skillPressed(slot: Int) {
        val nowMs = System.currentTimeMillis()
        _sessionState.update { WorldSimulation.performSkill(it, slot, nowMs) }
    }

    fun swapSkullPressed() {
        val nowMs = System.currentTimeMillis()
        _sessionState.update { WorldSimulation.performSwapSkull(it, nowMs) }
    }

    fun interactPressed() {
        _sessionState.update { WorldSimulation.performInteract(it) }
    }

    fun onGestureAttack(points: List<Offset>, holdMs: Long) {
        val nowMs = System.currentTimeMillis()
        _sessionState.update { cur ->
            val rec = GestureRecognizer.classify(points, holdMs, cur.unlockedGestures)
            WorldSimulation.performAttack(cur, rec, nowMs)
        }
    }

    fun eatFirstAvailableFood() {
        _sessionState.update { cur ->
            val foodId = listOf("food_pizza", "food_sandwich", "food_apple")
                .firstOrNull { (cur.inventoryCounts[it] ?: 0) > 0 } ?: "food_apple"
            val (next, msg) = WorldSimulation.useConsumable(cur, foodId)
            next.copy(activeBanner = msg, bannerTtlSec = 2.4f)
        }
    }

    fun drinkFirstAvailableDrink() {
        _sessionState.update { cur ->
            val drinkId = listOf("drink_turbo", "drink_soda", "drink_oasis")
                .firstOrNull { (cur.inventoryCounts[it] ?: 0) > 0 } ?: "drink_oasis"
            val (next, msg) = WorldSimulation.useConsumable(cur, drinkId)
            next.copy(activeBanner = msg, bannerTtlSec = 2.4f)
        }
    }

    fun openMenu(tab: MenuTab) {
        _statusMessage.value = null
        _openMenuTab.value = tab
    }

    fun closeMenu() {
        _openMenuTab.value = null
        _statusMessage.value = null
    }

    fun dismissTvDialog() {
        _sessionState.update { it.copy(tvInfoDialog = null) }
    }

    fun updateHeroName(name: String) {
        _sessionState.update { cur ->
            cur.copy(appearance = cur.appearance.copy(heroName = name.take(18)))
        }
    }

    fun equipSkull(skullId: String, toSlot: Int) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.unlockOrEquipSkull(cur, skullId, toSlot)
            _statusMessage.value = msg
            next
        }
    }

    fun equipOrBuyCosmetic(id: String) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.equipOrBuyCosmetic(cur, id)
            _statusMessage.value = msg
            next
        }
    }

    fun buyOrToggleRelic(id: String) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.buyOrToggleRelic(cur, id)
            _statusMessage.value = msg
            next
        }
    }

    fun upgradeWeaponOrShield(isShield: Boolean) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.upgradeWeaponOrShield(cur, isShield)
            _statusMessage.value = msg
            next
        }
    }

    fun craftTool(tool: CraftableTool) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.craftToolUpgrade(cur, tool)
            _statusMessage.value = msg
            next
        }
    }

    fun craftArrows() {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.craftArrows(cur)
            _statusMessage.value = msg
            next
        }
    }

    fun buyConsumable(id: String) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.buyConsumable(cur, id)
            _statusMessage.value = msg
            next
        }
    }

    fun useConsumable(id: String) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.useConsumable(cur, id)
            _statusMessage.value = msg
            next
        }
    }

    fun allocateStat(key: String) {
        _sessionState.update { WorldSimulation.allocateStatPoint(it, key) }
    }

    fun claimMission(id: String) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.claimMission(cur, id)
            _statusMessage.value = msg
            next
        }
    }

    fun summonMolotovBoss() {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.summonMolotovBoss(cur)
            _statusMessage.value = msg
            next
        }
        _openMenuTab.value = null
    }

    fun togglePet(id: String) {
        _sessionState.update { cur ->
            val (next, msg) = WorldSimulation.toggleEquipPet(cur, id)
            _statusMessage.value = msg
            next
        }
    }

    fun updateSettings(settings: GameSettings) {
        RetroSoundEngine.soundEnabled = settings.soundEnabled
        RetroSoundEngine.volume = settings.soundVolume
        BiomeAudioManager.ambientEnabled = settings.soundEnabled
        _sessionState.update { it.copy(settings = settings) }
    }

    fun quickSave() {
        saveToSlot("quicksave", "Guardado Rápido")
    }

    fun saveToSlot(slotId: String, label: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val snapshot = _sessionState.value.toSaveSlot(slotId, label)
            val result = saveRepository.saveToSlot(currentUserId, slotId, snapshot)
            _isSyncing.value = false
            _statusMessage.value = result.fold(
                onSuccess = { "¡Guardado rápido en la nube ($label)!" },
                onFailure = { err -> err.localizedMessage ?: "No se pudo guardar en la nube." }
            )
            _sessionState.update { it.copy(activeBanner = _statusMessage.value, bannerTtlSec = 2.6f) }
        }
    }

    fun loadSlot(slot: SaveSlot) {
        _sessionState.value = GameSessionState.fromSaveSlot(slot)
        _statusMessage.value = "Partida cargada: ${slot.slotLabel}"
        _openMenuTab.value = null
    }

    fun deleteSlot(slotId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = saveRepository.deleteSlot(currentUserId, slotId)
            _isSyncing.value = false
            _statusMessage.value = res.fold(
                onSuccess = { "Ranura eliminada." },
                onFailure = { err -> err.localizedMessage ?: "Error al borrar." }
            )
        }
    }
}
