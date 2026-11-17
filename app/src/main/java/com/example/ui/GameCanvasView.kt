package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameCatalog
import com.example.data.GestureSkillId
import com.example.game.BoxyAvatarPreview
import com.example.game.GestureRecognizer
import com.example.game.VectorCharacterBadge
import com.example.game.WorldCanvasRenderer.renderWorld
import com.example.ui.theme.BirdySky
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.MolotovCrimson
import com.example.ui.theme.RetroBgDark
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroMint
import com.example.ui.theme.RetroSurface
import com.example.ui.theme.RetroSurfaceElevated
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun GameCanvasScreen(
    viewModel: GameViewModel,
    onSignOut: () -> Unit
) {
    val state by viewModel.sessionState.collectAsStateWithLifecycle()
    val saveSlots by viewModel.saveSlots.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val openMenuTab by viewModel.openMenuTab.collectAsStateWithLifecycle()

    var gestureStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val liveGestureBadge = remember(gestureStroke, state.unlockedGestures) {
        if (gestureStroke.size < 2) null
        else {
            val g = GestureRecognizer.classify(gestureStroke, 100L, state.unlockedGestures)
            when (g.skill) {
                GestureSkillId.DOWN_PARRY -> "↓ GUARDIA PARRY"
                GestureSkillId.UP_SLAM -> "↑ GANCHO ASCENDENTE"
                GestureSkillId.DIAGONAL_SLASH -> if (g.dirX < 0f) "↖ CORTE DIAGONAL" else "↗ CORTE DIAGONAL"
                GestureSkillId.SWIPE_DASH -> if (g.dirX < 0f) "← ESTOCADA IZQ" else "→ ESTOCADA DER"
                GestureSkillId.CIRCLE_NOVA -> "◯ TORBELLINO"
                GestureSkillId.Z_SLASH -> "⚡ RELÁMPAGO Z"
                else -> null
            }
        }
    }

    val activeBoss = remember(state.enemies) {
        state.enemies.firstOrNull { it.isBoss && it.alive }
    }
    val nextMission = remember(state.claimedMissionIds, state.roomsClearedCount, state.skullSwapsCount, state.totalEnemiesDefeated, state.molotovBossDefeated) {
        GameCatalog.missions.firstOrNull { it.id !in state.claimedMissionIds }
    }
    val nearPortal = remember(state.stageData, state.playerX) {
        state.stageData.portals.firstOrNull { abs(it.x - state.playerX) <= 64f }
    }

    val btnScale = state.settings.buttonScale.coerceIn(0.8f, 1.25f)
    val hudAlpha = state.settings.hudOpacity.coerceIn(0.45f, 1f)
    val nowMs = System.currentTimeMillis()
    val activeSkull = state.activeSkull()
    val secondarySkull = state.secondarySkull()

    Box(modifier = Modifier.fillMaxSize().background(RetroBgDark)) {
        // 1. 2D Side-Scrolling Skul-Style Canvas + Touch-Gesture Combat
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startTime = System.currentTimeMillis()
                        val pts = mutableListOf(down.position)
                        gestureStroke = pts.toList()
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                            ch.consume()
                            pts += ch.position
                            gestureStroke = pts.toList()
                        }
                        val holdMs = System.currentTimeMillis() - startTime
                        viewModel.onGestureAttack(pts, holdMs)
                        gestureStroke = emptyList()
                    }
                }
                .testTag("game_world_canvas")
        ) {
            renderWorld(state = state, gestureStroke = gestureStroke, nowMs = System.currentTimeMillis())
        }

        // 2. Landscape Skul HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Boxy Card + Active & Secondary Skull Cards
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    Surface(
                        color = RetroSurface.copy(alpha = 0.92f),
                        border = BorderStroke(2.dp, activeSkull.accentColor),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.clickable { viewModel.openMenu(MenuTab.BOXY) }.testTag("hud_boxy_card")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(RetroBgDark, MaterialTheme.shapes.small)
                                    .border(1.5.dp, activeSkull.primaryColor, MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                BoxyAvatarPreview(
                                    appearance = state.appearance,
                                    activeSkullId = state.activeSkullId,
                                    walkPhase = state.walkPhase,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(Modifier.width(7.dp))
                            Column(modifier = Modifier.width(136.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${state.appearance.heroName} Nv.${state.heroLevel}", style = MaterialTheme.typography.labelSmall, color = MokayCream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${state.sparksCurrency}⚡", style = MaterialTheme.typography.labelSmall, color = MokaySpark)
                                }
                                Spacer(Modifier.height(2.dp))
                                val maxHp = state.effectiveMaxHp().coerceAtLeast(1)
                                LinearProgressIndicator(
                                    progress = { (state.hp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f) },
                                    color = MolotovCrimson,
                                    trackColor = RetroBgDark,
                                    modifier = Modifier.fillMaxWidth().height(6.dp)
                                )
                                Spacer(Modifier.height(2.dp))
                                val maxStam = state.effectiveMaxStamina().coerceAtLeast(1f)
                                LinearProgressIndicator(
                                    progress = { (state.stamina / maxStam).coerceIn(0f, 1f) },
                                    color = BirdySky,
                                    trackColor = RetroBgDark,
                                    modifier = Modifier.fillMaxWidth().height(5.dp)
                                )
                                Text("HP ${state.hp}/$maxHp · Rel ${state.equippedRelics.size}/6", style = MaterialTheme.typography.labelSmall, color = RetroMint)
                            }
                        }
                    }

                    // Dual Skull Swap Badge (2D Vector)
                    Surface(
                        color = RetroSurfaceElevated.copy(alpha = 0.90f),
                        border = BorderStroke(1.5.dp, secondarySkull.accentColor),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.clickable { viewModel.swapSkullPressed() }.testTag("hud_skulls_badge")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(RetroBgDark, MaterialTheme.shapes.small)
                                    .border(1.5.dp, activeSkull.accentColor, MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                BoxyAvatarPreview(
                                    appearance = state.appearance,
                                    activeSkullId = activeSkull.id,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Icon(Icons.Filled.SwapHoriz, contentDescription = "Cambiar Cabeza", tint = MokaySpark, modifier = Modifier.size(14.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(RetroBgDark, MaterialTheme.shapes.small)
                                    .border(1.dp, secondarySkull.accentColor, MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                BoxyAvatarPreview(
                                    appearance = state.appearance,
                                    activeSkullId = secondarySkull.id,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Center: Mascotas Strip + Mission Tracker + Boss HP / Banner
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = RetroSurface.copy(alpha = 0.90f),
                            border = BorderStroke(1.5.dp, MokayLavender),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.clickable { viewModel.openMenu(MenuTab.PETS) }.testTag("hud_pets_strip")
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Pets, contentDescription = "Mascotas", tint = MokaySpark, modifier = Modifier.size(14.dp))
                                state.equippedPets.take(3).forEach { pid ->
                                    val pet = GameCatalog.petById(pid)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(RetroBgDark, MaterialTheme.shapes.small)
                                            .border(1.dp, pet.accentColor, MaterialTheme.shapes.small),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        VectorCharacterBadge(
                                            characterId = pet.id,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text("${state.equippedPets.size}/3", style = MaterialTheme.typography.labelSmall, color = MokayCream)
                            }
                        }

                        if (nextMission != null) {
                            val prog = state.missionProgressCount(nextMission.id).coerceAtMost(nextMission.targetCount)
                            Surface(
                                color = RetroSurfaceElevated.copy(alpha = 0.90f),
                                border = BorderStroke(1.5.dp, MokaySpark),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.clickable { viewModel.openMenu(MenuTab.MISSIONS) }.testTag("hud_mission_pill")
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Flag, contentDescription = null, tint = MokaySpark, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${nextMission.title} ($prog/${nextMission.targetCount})", style = MaterialTheme.typography.labelSmall, color = MokayCream, maxLines = 1)
                                }
                            }
                        }
                    }

                    if (activeBoss != null) {
                        Spacer(Modifier.height(4.dp))
                        Surface(color = Color(0xCC290910), border = BorderStroke(2.dp, MolotovCrimson), shape = MaterialTheme.shapes.small, modifier = Modifier.width(240.dp)) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("${activeBoss.name} · ${activeBoss.hp}/${activeBoss.maxHp}", style = MaterialTheme.typography.labelSmall, color = MokaySpark)
                                LinearProgressIndicator(progress = { (activeBoss.hp.toFloat() / activeBoss.maxHp.toFloat()).coerceIn(0f, 1f) }, color = MolotovCrimson, trackColor = RetroBgDark, modifier = Modifier.fillMaxWidth().height(6.dp))
                            }
                        }
                    } else if (state.activeBanner != null) {
                        Spacer(Modifier.height(4.dp))
                        Surface(color = RetroSurfaceElevated.copy(alpha = 0.90f), border = BorderStroke(1.dp, MokaySpark), shape = MaterialTheme.shapes.small) {
                            Text(state.activeBanner!!, style = MaterialTheme.typography.labelSmall, color = MokayCream, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                    }

                    val gestureTag = liveGestureBadge ?: state.lastGestureLabel
                    if (gestureTag != null) {
                        Spacer(Modifier.height(4.dp))
                        val isParry = gestureTag.contains("PARRY")
                        Surface(
                            color = (if (isParry) BirdySky else MokayFlame).copy(alpha = 0.92f),
                            border = BorderStroke(1.5.dp, MokayCream),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("hud_gesture_badge")
                        ) {
                            Text(
                                text = gestureTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MokayCharcoal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Right: Act & Room + Quick Menu Bar
                Column(horizontalAlignment = Alignment.End) {
                    Surface(color = RetroSurface.copy(alpha = 0.90f), border = BorderStroke(1.5.dp, state.currentZone.accentColor), shape = MaterialTheme.shapes.small) {
                        val clearTag = if (state.roomCleared) "✓ Limpia" else "${state.enemies.count { it.alive }} Enemigos"
                        Text(
                            text = "${state.currentZone.title} · S.${state.currentRoomIndex} · $clearTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = state.currentZone.accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        HudMenuChip(Icons.Filled.Face, "Boxy", MokayFlame, "menu_btn_boxy") { viewModel.openMenu(MenuTab.BOXY) }
                        HudMenuChip(Icons.Filled.Build, "Reliquias", RetroMint, "menu_btn_shop") { viewModel.openMenu(MenuTab.SHOP) }
                        HudMenuChip(Icons.Filled.Flag, "Misiones", MokaySpark, "menu_btn_missions") { viewModel.openMenu(MenuTab.MISSIONS) }
                        HudMenuChip(Icons.Filled.Settings, "Ajustes", BirdySky, "menu_btn_settings") { viewModel.openMenu(MenuTab.SETTINGS) }
                        HudMenuChip(Icons.Filled.Save, if (isSyncing) "..." else "Guardar", MokayLavender, "quick_save_button") { viewModel.quickSave() }
                    }
                }
            }

            // BOTTOM CONTROLS ROW (Left-Handed swap, Button Scale & HUD Opacity)
            val joystickBlock = @Composable {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(hudAlpha)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val foodTotal = listOf("food_apple", "food_sandwich", "food_pizza").sumOf { state.inventoryCounts[it] ?: 0 }
                        val drinkTotal = listOf("drink_oasis", "drink_soda", "drink_turbo").sumOf { state.inventoryCounts[it] ?: 0 }
                        Surface(
                            color = RetroSurface.copy(alpha = 0.9f),
                            border = BorderStroke(1.5.dp, RetroMint),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.clickable { viewModel.eatFirstAvailableFood() }.testTag("quick_eat_button")
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Fastfood, contentDescription = "Comer", tint = RetroMint, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("x$foodTotal", style = MaterialTheme.typography.labelSmall, color = MokayCream)
                            }
                        }
                        Surface(
                            color = RetroSurface.copy(alpha = 0.9f),
                            border = BorderStroke(1.5.dp, BirdySky),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.clickable { viewModel.drinkFirstAvailableDrink() }.testTag("quick_drink_button")
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalDrink, contentDescription = "Beber", tint = BirdySky, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("x$drinkTotal", style = MaterialTheme.typography.labelSmall, color = MokayCream)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    SkulJoystick(
                        onMove = viewModel::setJoystick,
                        modifier = Modifier.size((92 * btnScale).dp)
                    )
                }
            }

            val skulActionPad = @Composable {
                val cd1 = ((state.skill1CooldownUntilMs - nowMs).coerceAtLeast(0L) / 1000f)
                val cd2 = ((state.skill2CooldownUntilMs - nowMs).coerceAtLeast(0L) / 1000f)
                val cdSwap = ((state.swapCooldownUntilMs - nowMs).coerceAtLeast(0L) / 1000f)
                val cdDash = ((state.dashCooldownUntilMs - nowMs).coerceAtLeast(0L) / 1000f)

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.alpha(hudAlpha)
                ) {
                    Surface(
                        color = RetroSurface.copy(alpha = 0.80f),
                        border = BorderStroke(1.dp, MokayLavender.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Gestos: ←/→ Estocada · ↗ Diagonal · ↑ Gancho · ↓ Parry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MokayCream.copy(alpha = 0.88f),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    // Upper Skill + Swap + Context Interact / Shield Row
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        SkulActionButton(
                            label = if (cd1 > 0.1f) "${"%.1f".format(cd1)}s" else "HAB 1",
                            icon = Icons.Filled.FlashOn,
                            bg = if (cd1 > 0.1f) RetroSurfaceElevated else activeSkull.accentColor,
                            contentCol = if (cd1 > 0.1f) MokayCream else MokayCharcoal,
                            scale = btnScale,
                            widthDp = 68f,
                            testTag = "btn_skill_1",
                            onClick = { viewModel.skillPressed(1) }
                        )
                        SkulActionButton(
                            label = if (cd2 > 0.1f) "${"%.1f".format(cd2)}s" else "HAB 2",
                            icon = Icons.Filled.Bolt,
                            bg = if (cd2 > 0.1f) RetroSurfaceElevated else activeSkull.primaryColor,
                            contentCol = if (cd2 > 0.1f) MokayCream else MokayCharcoal,
                            scale = btnScale,
                            widthDp = 68f,
                            testTag = "btn_skill_2",
                            onClick = { viewModel.skillPressed(2) }
                        )
                        SkulActionButton(
                            label = if (cdSwap > 0.1f) "${"%.1f".format(cdSwap)}s" else "CAMBIAR",
                            icon = Icons.Filled.SwapHoriz,
                            bg = if (cdSwap > 0.1f) RetroSurfaceElevated else RetroGold,
                            contentCol = if (cdSwap > 0.1f) MokayCream else MokayCharcoal,
                            scale = btnScale,
                            widthDp = 76f,
                            testTag = "btn_swap_skull",
                            onClick = viewModel::swapSkullPressed
                        )
                        SkulActionButton(
                            label = if (nearPortal != null && state.roomCleared) "PORTAL" else "USAR",
                            icon = Icons.Filled.DoorSliding,
                            bg = if (nearPortal != null && state.roomCleared) RetroMint else MokayLavender,
                            contentCol = MokayCharcoal,
                            scale = btnScale,
                            widthDp = 72f,
                            testTag = "btn_interact",
                            onClick = viewModel::interactPressed
                        )
                    }

                    // Primary Platformer Combat Row: DEFENDER + DASH + SALTAR + ATACAR
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        // Hold DEFENDER Shield
                        Surface(
                            color = if (state.isDefending) BirdySky else RetroSurfaceElevated,
                            border = BorderStroke(2.dp, BirdySky),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .size(width = (74 * btnScale).dp, height = (48 * btnScale).dp)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        down.consume()
                                        viewModel.setDefending(true)
                                        while (true) {
                                            val ev = awaitPointerEvent()
                                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!ch.pressed) break
                                            ch.consume()
                                        }
                                        viewModel.setDefending(false)
                                    }
                                }
                                .testTag("btn_defend")
                        ) {
                            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = "Defender", tint = if (state.isDefending) MokayCharcoal else BirdySky, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("ESCUDO", style = MaterialTheme.typography.labelSmall, color = if (state.isDefending) MokayCharcoal else MokayCream)
                            }
                        }

                        SkulActionButton(
                            label = if (cdDash > 0.1f) "${"%.1f".format(cdDash)}s" else "DASH",
                            icon = Icons.Filled.Bolt,
                            bg = if (cdDash > 0.1f) RetroSurfaceElevated else BirdySky,
                            contentCol = if (cdDash > 0.1f) MokayCream else MokayCharcoal,
                            scale = btnScale,
                            widthDp = 70f,
                            testTag = "btn_dash",
                            onClick = viewModel::dashPressed
                        )

                        SkulActionButton(
                            label = "SALTAR",
                            icon = Icons.Filled.ArrowUpward,
                            bg = RetroMint,
                            contentCol = MokayCharcoal,
                            scale = btnScale,
                            widthDp = 74f,
                            testTag = "btn_jump",
                            onClick = viewModel::jumpPressed
                        )

                        SkulActionButton(
                            label = if (activeSkull.rangedBasic) "DISPARO" else "ATACAR",
                            icon = Icons.Filled.SportsKabaddi,
                            bg = MokayFlame,
                            contentCol = MokayCharcoal,
                            scale = btnScale,
                            widthDp = 82f,
                            testTag = "btn_attack",
                            onClick = viewModel::attackPressed
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (state.settings.leftHandedControls) {
                    skulActionPad()
                    joystickBlock()
                } else {
                    joystickBlock()
                    skulActionPad()
                }
            }
        }

        // 3. Unified Skul Menu Dialog
        if (openMenuTab != null) {
            RetroMenuDialog(
                initialTab = openMenuTab!!,
                state = state,
                saveSlots = saveSlots,
                isSyncing = isSyncing,
                statusMessage = statusMessage,
                onDismiss = viewModel::closeMenu,
                onUpdateHeroName = viewModel::updateHeroName,
                onEquipSkull = viewModel::equipSkull,
                onEquipOrBuyCosmetic = viewModel::equipOrBuyCosmetic,
                onBuyOrToggleRelic = viewModel::buyOrToggleRelic,
                onUpgradeWeaponOrShield = viewModel::upgradeWeaponOrShield,
                onCraftTool = viewModel::craftTool,
                onCraftArrows = viewModel::craftArrows,
                onBuyConsumable = viewModel::buyConsumable,
                onUseConsumable = viewModel::useConsumable,
                onAllocateStat = viewModel::allocateStat,
                onClaimMission = viewModel::claimMission,
                onSummonMolotovBoss = viewModel::summonMolotovBoss,
                onTogglePet = viewModel::togglePet,
                onUpdateSettings = viewModel::updateSettings,
                onSaveToSlot = viewModel::saveToSlot,
                onLoadSlot = viewModel::loadSlot,
                onDeleteSlot = viewModel::deleteSlot,
                onSignOut = onSignOut
            )
        }

        if (state.tvInfoDialog != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissTvDialog,
                containerColor = RetroSurface,
                title = { Text("Televisor Mokayverse", style = MaterialTheme.typography.titleMedium, color = MokaySpark) },
                text = { Text(state.tvInfoDialog!!, style = MaterialTheme.typography.bodyMedium, color = MokayCream) },
                confirmButton = {
                    Button(
                        onClick = viewModel::dismissTvDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = MokayFlame, contentColor = MokayCharcoal)
                    ) { Text("¡Entendido!", style = MaterialTheme.typography.labelSmall) }
                }
            )
        }
    }
}

@Composable
private fun SkulActionButton(
    label: String,
    icon: ImageVector,
    bg: Color,
    contentCol: Color,
    scale: Float,
    widthDp: Float,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = bg,
        border = BorderStroke(2.dp, MokayCream),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(width = (widthDp * scale).dp, height = (48 * scale).dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = contentCol, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentCol, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HudMenuChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = RetroSurface.copy(alpha = 0.90f),
        border = BorderStroke(1.5.dp, accent),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.clickable(onClick = onClick).testTag(testTag)
    ) {
        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MokayCream)
        }
    }
}

@Composable
private fun SkulJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.width * 0.36f

                    fun update(pos: Offset) {
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val mag = hypot(dx, dy).coerceAtLeast(1f)
                        val clamped = minOf(mag, maxR)
                        knobX = (dx / mag) * clamped
                        knobY = (dy / mag) * clamped
                        onMove((knobX / maxR).coerceIn(-1f, 1f), (knobY / maxR).coerceIn(-1f, 1f))
                    }
                    update(down.position)

                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        ch.consume()
                        update(ch.position)
                    }
                    knobX = 0f
                    knobY = 0f
                    onMove(0f, 0f)
                }
            }
            .testTag("virtual_joystick")
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.44f
        drawCircle(RetroSurface.copy(alpha = 0.78f), r, Offset(cx, cy))
        drawCircle(MokayLavender.copy(alpha = 0.8f), r, Offset(cx, cy), style = Stroke(3f))
        drawCircle(MokayFlame, r * 0.44f, Offset(cx + knobX, cy + knobY))
        drawCircle(MokayCream, r * 0.22f, Offset(cx + knobX, cy + knobY))
    }
}
