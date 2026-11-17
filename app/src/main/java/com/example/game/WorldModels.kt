package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.CosmeticDef
import com.example.data.GameCatalog
import com.example.data.PetDef
import com.example.data.RelicItemDef
import com.example.data.SaveSlot
import com.example.data.SkullFormDef
import com.example.data.WorldZone
import com.example.ui.theme.MokayFlame

enum class EnemySpecies(val displayName: String, val spriteKey: String) {
    LAUGHING_CAT("Laughing Cat", "cat"),
    STREET_DINO("Street Dino", "dino"),
    MOLOTOV_MOKAY("Molotov Mokay", "molotov")
}

data class GameSettings(
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val cameraZoom: Float = 2.05f, // Cerca (2.30), Normal (2.05), Amplio (1.72)
    val highParticles: Boolean = true,
    val ambientLighting: Boolean = true,
    val buttonScale: Float = 1.0f,
    val hudOpacity: Float = 0.92f,
    val leftHandedControls: Boolean = false
)

data class HeroAppearance(
    val heroName: String = "Boxy",
    val headgearId: String = "mokay_cap_classic",
    val jacketId: String = "mokay_hoodie_flame",
    val pantsShoesId: String = "mokay_kicks_street",
    val backpackId: String = "mokay_pack_original"
)

data class DashAfterimage(
    val x: Float,
    val y: Float,
    val facingLeft: Boolean,
    val activeSkullId: String,
    val ttlSec: Float = 0.22f
)

data class PetCompanionState(
    val petId: String,
    val x: Float,
    val y: Float,
    val facingLeft: Boolean = false,
    val attackCooldownMs: Long = 0L
)

data class WorldEnemy(
    val id: String,
    val name: String,
    val species: EnemySpecies,
    val zone: WorldZone,
    val level: Int,
    val isBoss: Boolean,
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val patrolMinX: Float,
    val patrolMaxX: Float,
    val facingLeft: Boolean = true,
    val hp: Int,
    val maxHp: Int,
    val atk: Int,
    val rewardSparks: Int,
    val rewardXp: Int,
    val radius: Float = 20f,
    val alive: Boolean = true,
    val hitFlashMs: Long = 0L,
    val aiCooldownMs: Long = 0L
)

data class ArrowProjectile(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int,
    val color: Color,
    val fromPlayer: Boolean = true,
    val ttlSec: Float = 1.25f,
    val radius: Float = 7f,
    val gravity: Float = 0f
)

data class FloatingText(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val ttlSec: Float = 1.1f
)

data class ActionParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float = 6f,
    val ttlSec: Float = 0.45f
)

enum class SlashVisualStyle {
    ARC,
    HORIZONTAL_LUNGE,
    DIAGONAL_CLEAVE,
    UPWARD_UPPERCUT,
    PARRY_SHIELD,
    WHIRLWIND_NOVA,
    ZIGZAG_BOLT
}

data class SlashArcEffect(
    val id: Long,
    val x: Float,
    val y: Float,
    val dirX: Float,
    val dirY: Float,
    val color: Color,
    val radius: Float = 66f,
    val ttlSec: Float = 0.28f,
    val maxTtlSec: Float = 0.28f,
    val style: SlashVisualStyle = SlashVisualStyle.ARC
)

data class GameSessionState(
    val slotId: String = "quicksave",
    val slotLabel: String = "Guardado Rápido",
    val worldSeed: Long = 202606L,
    val appearance: HeroAppearance = HeroAppearance(),
    val settings: GameSettings = GameSettings(),
    val activeSkullId: String = "boxy_classic",
    val secondarySkullId: String = "boxy_flame",
    val unlockedSkulls: Set<String> = setOf("boxy_classic", "boxy_flame"),
    val equippedRelics: List<String> = listOf("rel_gold_match"),
    val playerX: Float = SkulStageGenerator.SPAWN_X,
    val playerY: Float = SkulStageGenerator.SPAWN_Y,
    val playerVx: Float = 0f,
    val playerVy: Float = 0f,
    val facingLeft: Boolean = false,
    val onGround: Boolean = true,
    val jumpsRemaining: Int = 1,
    val dropThroughUntilMs: Long = 0L,
    val walkPhase: Float = 0f,
    val swingProgress: Float = 0f,
    val comboStep: Int = 0,
    val isDefending: Boolean = false,
    val dashTimerSec: Float = 0f,
    val dashDirX: Float = 1f,
    val dashCooldownUntilMs: Long = 0L,
    val skill1CooldownUntilMs: Long = 0L,
    val skill2CooldownUntilMs: Long = 0L,
    val swapCooldownUntilMs: Long = 0L,
    val hp: Int = 150,
    val stamina: Float = 100f,
    val heroLevel: Int = 1,
    val heroXp: Int = 0,
    val statPoints: Int = 1,
    val atkBonus: Int = 0,
    val hpBonus: Int = 0,
    val spdBonus: Int = 0,
    val weaponLevel: Int = 1,
    val shieldLevel: Int = 1,
    val currentZone: WorldZone = WorldZone.ALDEA_MOKAY,
    val currentRoomIndex: Int = 0,
    val roomsClearedCount: Long = 0L,
    val skullSwapsCount: Long = 0L,
    val sparksCurrency: Long = 120L,
    val totalEnemiesDefeated: Long = 0L,
    val molotovBossDefeated: Boolean = false,
    val playTimeSeconds: Long = 0L,
    val equippedPets: List<String> = listOf("mokay"),
    val unlockedPets: Set<String> = setOf("mokay"),
    val unlockedCosmetics: Set<String> = setOf(
        "mokay_cap_classic",
        "mokay_hoodie_flame",
        "mokay_kicks_street",
        "mokay_pack_original"
    ),
    val unlockedGestures: Set<String> = setOf("TAP_STRIKE", "SWIPE_DASH", "DIAGONAL_SLASH", "UP_SLAM", "DOWN_PARRY"),
    val completedMissionIds: Set<String> = emptySet(),
    val claimedMissionIds: Set<String> = emptySet(),
    val inventoryCounts: Map<String, Int> = mapOf(
        "food_apple" to 3,
        "drink_oasis" to 3
    ),
    val stageData: GeneratedStageData = SkulStageGenerator.generateRoom(WorldZone.ALDEA_MOKAY, 0, 202606L),
    val roomCleared: Boolean = true,
    val petStates: List<PetCompanionState> = listOf(
        PetCompanionState("mokay", SkulStageGenerator.SPAWN_X - 38f, SkulStageGenerator.SPAWN_Y - 30f)
    ),
    val enemies: List<WorldEnemy> = emptyList(),
    val projectiles: List<ArrowProjectile> = emptyList(),
    val afterimages: List<DashAfterimage> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val particles: List<ActionParticle> = emptyList(),
    val slashEffects: List<SlashArcEffect> = emptyList(),
    val gestureCooldowns: Map<String, Long> = emptyMap(),
    val invulnerableUntilMs: Long = 0L,
    val parryUntilMs: Long = 0L,
    val nextEntityId: Long = 200L,
    val animFrame: Int = 0,
    val activeBanner: String? = "Campamento Aldea · ¡Usa el Portal a la derecha para iniciar tu Run Skul!",
    val bannerTtlSec: Float = 4.5f,
    val lastGestureLabel: String? = null,
    val lastGestureUntilMs: Long = 0L,
    val tvInfoDialog: String? = null
) {
    fun activeSkull(): SkullFormDef = GameCatalog.skullById(activeSkullId)
    fun secondarySkull(): SkullFormDef = GameCatalog.skullById(secondarySkullId)
    fun activeRelics(): List<RelicItemDef> = equippedRelics.mapNotNull { GameCatalog.relicById(it) }
    fun inventoryManager(): InventoryManager = InventoryManager.fromInventoryMap(inventoryCounts)

    fun activeCosmetics(): List<CosmeticDef> = listOfNotNull(
        GameCatalog.cosmeticById(appearance.headgearId),
        GameCatalog.cosmeticById(appearance.jacketId),
        GameCatalog.cosmeticById(appearance.pantsShoesId),
        GameCatalog.cosmeticById(appearance.backpackId)
    )

    fun activePetDefs(): List<PetDef> = equippedPets.take(3).map { GameCatalog.petById(it) }

    fun effectiveAtk(): Int {
        val gearAtk = activeCosmetics().sumOf { it.bonusAtk }
        val relicAtk = activeRelics().sumOf { it.bonusAtk }
        val invMgr = inventoryManager()
        val toolBonus = (invMgr.getToolLevel(CraftableTool.AXE) - 1).coerceAtLeast(0) * 5 +
            if (activeSkull().rangedBasic) invMgr.getToolLevel(CraftableTool.BOW) * 6 else 0
        val base = 22 + (heroLevel - 1) * 5 + atkBonus * 5 + (weaponLevel - 1) * 12 + gearAtk + relicAtk + toolBonus
        val skullMult = activeSkull().atkMult
        val petPct = activePetDefs().sumOf { it.bonusAtkPct.toDouble() }.toFloat()
        return (base * skullMult * (1f + petPct)).toInt()
    }

    fun effectiveCritPct(): Float {
        return activeRelics().sumOf { it.bonusCritPct.toDouble() }.toFloat().coerceIn(0f, 0.75f)
    }

    fun effectiveCooldownFactor(): Float {
        val red = activeRelics().sumOf { it.cooldownReductionPct.toDouble() }.toFloat().coerceIn(0f, 0.50f)
        return 1f - red
    }

    fun effectiveDashDamage(): Int = activeRelics().sumOf { it.dashDamage }

    fun effectiveMaxHp(): Int {
        val gearHp = activeCosmetics().sumOf { it.bonusHp }
        val relicHp = activeRelics().sumOf { it.bonusHp }
        return 130 + (heroLevel - 1) * 20 + hpBonus * 18 + gearHp + relicHp + activeSkull().bonusHp
    }

    fun effectiveMaxStamina(): Float {
        val gearStam = activeCosmetics().sumOf { it.bonusStamina }
        return (95 + (heroLevel - 1) * 8 + gearStam).toFloat()
    }

    fun effectiveMoveSpeed(): Float {
        val gearSpd = activeCosmetics().sumOf { it.bonusSpeed }
        val relicPct = activeRelics().sumOf { it.bonusSpeedPct.toDouble() }.toFloat()
        return (195f + spdBonus * 12f + gearSpd) * activeSkull().speedMult * (1f + relicPct)
    }

    fun effectiveExtraJumps(): Int = activeSkull().extraJumps

    fun effectiveDefensePct(): Float {
        val gearDef = activeCosmetics().sumOf { it.defensePct.toDouble() }.toFloat()
        val petDef = activePetDefs().sumOf { it.bonusDefPct.toDouble() }.toFloat()
        val blockBonus = if (isDefending) (0.55f + shieldLevel * 0.06f) else 0f
        return (gearDef + petDef + blockBonus).coerceIn(0f, 0.90f)
    }

    fun effectiveStaminaRegenPerSec(): Float {
        val petRegen = activePetDefs().sumOf { it.bonusStaminaRegen.toDouble() }.toFloat()
        return 14f + petRegen
    }

    fun missionProgressCount(missionId: String): Int {
        return when (missionId) {
            "m_first_clear" -> roomsClearedCount.toInt()
            "m_swap_skulls" -> skullSwapsCount.toInt()
            "m_reach_desert" -> if (currentZone.ordinal >= WorldZone.DESIERTO_DORADO.ordinal) 1 else 0
            "m_defeat_crew" -> totalEnemiesDefeated.toInt()
            "m_upgrade_weapon" -> if (weaponLevel >= 2 || equippedRelics.size >= 2) 1 else 0
            "m_defeat_molotov" -> if (molotovBossDefeated) 1 else 0
            else -> 0
        }
    }

    fun toSaveSlot(targetSlotId: String = slotId, targetLabel: String = slotLabel): SaveSlot {
        return SaveSlot(
            id = targetSlotId,
            slotLabel = targetLabel,
            heroName = appearance.heroName,
            headgearId = appearance.headgearId,
            jacketId = appearance.jacketId,
            pantsShoesId = appearance.pantsShoesId,
            backpackId = appearance.backpackId,
            activeSkullId = activeSkullId,
            secondarySkullId = secondarySkullId,
            unlockedSkulls = unlockedSkulls.toList(),
            equippedRelics = equippedRelics.take(6),
            heroLevel = heroLevel.toLong(),
            heroXp = heroXp.toLong(),
            statPoints = statPoints.toLong(),
            atkBonus = atkBonus.toLong(),
            hpBonus = hpBonus.toLong(),
            spdBonus = spdBonus.toLong(),
            weaponLevel = weaponLevel.toLong(),
            shieldLevel = shieldLevel.toLong(),
            worldSeed = worldSeed,
            playerX = playerX.toDouble(),
            playerY = playerY.toDouble(),
            currentZone = currentZone.id,
            currentRoomIndex = currentRoomIndex.toLong(),
            roomsClearedCount = roomsClearedCount,
            skullSwapsCount = skullSwapsCount,
            sparksCurrency = sparksCurrency,
            totalEnemiesDefeated = totalEnemiesDefeated,
            molotovBossDefeated = molotovBossDefeated,
            playTimeSeconds = playTimeSeconds,
            equippedPets = equippedPets.take(3),
            unlockedPets = unlockedPets.toList(),
            unlockedCosmetics = unlockedCosmetics.toList(),
            unlockedGestures = unlockedGestures.toList(),
            completedMissionIds = completedMissionIds.toList(),
            claimedMissionIds = claimedMissionIds.toList(),
            inventoryCounts = inventoryCounts.mapValues { it.value.toLong() }
        )
    }

    companion object {
        fun fromSaveSlot(slot: SaveSlot): GameSessionState {
            val app = HeroAppearance(
                heroName = slot.heroName.ifBlank { "Boxy" },
                headgearId = slot.headgearId.ifBlank { "mokay_cap_classic" },
                jacketId = slot.jacketId.ifBlank { "mokay_hoodie_flame" },
                pantsShoesId = slot.pantsShoesId.ifBlank { "mokay_kicks_street" },
                backpackId = slot.backpackId.ifBlank { "mokay_pack_original" }
            )
            val seed = slot.worldSeed.coerceAtLeast(1L)
            val zone = WorldZone.byId(slot.currentZone)
            val roomIdx = slot.currentRoomIndex.toInt().coerceAtLeast(0)
            val stage = SkulStageGenerator.generateRoom(zone, roomIdx, seed)
            val enemies = WorldSimulation.spawnEnemiesForStage(zone, roomIdx, stage)
            val unlockedSkulls = (slot.unlockedSkulls + listOf("boxy_classic", "boxy_flame")).toSet()
            val activeSkull = slot.activeSkullId.takeIf { it in unlockedSkulls } ?: "boxy_classic"
            val secondarySkull = slot.secondarySkullId.takeIf { it in unlockedSkulls && it != activeSkull } ?: "boxy_flame"
            val unlockedPets = (slot.unlockedPets + "mokay").toSet()
            val equippedPets = slot.equippedPets.filter { it in unlockedPets }.distinct().take(3).ifEmpty { listOf("mokay") }
            val px = slot.playerX.toFloat().coerceIn(48f, SkulStageGenerator.STAGE_WIDTH - 48f)
            val py = slot.playerY.toFloat().coerceIn(64f, SkulStageGenerator.STAGE_HEIGHT - 120f)

            val base = GameSessionState(
                slotId = slot.id.ifBlank { "quicksave" },
                slotLabel = slot.slotLabel.ifBlank { "Guardado Rápido" },
                worldSeed = seed,
                appearance = app,
                activeSkullId = activeSkull,
                secondarySkullId = secondarySkull,
                unlockedSkulls = unlockedSkulls,
                equippedRelics = slot.equippedRelics.take(6),
                playerX = px,
                playerY = py,
                heroLevel = slot.heroLevel.toInt().coerceAtLeast(1),
                heroXp = slot.heroXp.toInt().coerceAtLeast(0),
                statPoints = slot.statPoints.toInt().coerceAtLeast(0),
                atkBonus = slot.atkBonus.toInt().coerceAtLeast(0),
                hpBonus = slot.hpBonus.toInt().coerceAtLeast(0),
                spdBonus = slot.spdBonus.toInt().coerceAtLeast(0),
                weaponLevel = slot.weaponLevel.toInt().coerceIn(1, 5),
                shieldLevel = slot.shieldLevel.toInt().coerceIn(1, 5),
                currentZone = zone,
                currentRoomIndex = roomIdx,
                roomsClearedCount = slot.roomsClearedCount,
                skullSwapsCount = slot.skullSwapsCount,
                sparksCurrency = slot.sparksCurrency,
                totalEnemiesDefeated = slot.totalEnemiesDefeated,
                molotovBossDefeated = slot.molotovBossDefeated,
                playTimeSeconds = slot.playTimeSeconds,
                equippedPets = equippedPets,
                unlockedPets = unlockedPets,
                unlockedCosmetics = (slot.unlockedCosmetics + listOf(
                    "mokay_cap_classic", "mokay_hoodie_flame", "mokay_kicks_street", "mokay_pack_original"
                )).toSet(),
                unlockedGestures = (slot.unlockedGestures + listOf("TAP_STRIKE", "SWIPE_DASH", "DIAGONAL_SLASH", "UP_SLAM", "DOWN_PARRY")).toSet(),
                completedMissionIds = slot.completedMissionIds.toSet(),
                claimedMissionIds = slot.claimedMissionIds.toSet(),
                inventoryCounts = slot.inventoryCounts.mapValues { it.value.toInt() },
                stageData = stage,
                roomCleared = enemies.isEmpty(),
                petStates = equippedPets.mapIndexed { idx, pid -> PetCompanionState(pid, px - (idx + 1) * 32f, py - 24f) },
                enemies = enemies,
                jumpsRemaining = GameCatalog.skullById(activeSkull).extraJumps
            )
            return base.copy(hp = base.effectiveMaxHp(), stamina = base.effectiveMaxStamina())
        }
    }
}
