package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.data.GestureSkillId
import com.example.data.TileKind
import com.example.data.WorldZone
import com.example.game.WorldSceneryRenderer.drawBiomeWorldLandmarks
import com.example.game.WorldSceneryRenderer.drawDetailedParallaxBackdrop
import com.example.game.WorldSceneryRenderer.drawDetailedPortal
import com.example.game.WorldSceneryRenderer.drawDetailedStageTile
import com.example.ui.theme.BirdySky
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.MolotovCrimson
import com.example.ui.theme.RetroDanger
import com.example.ui.theme.RetroMint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object WorldCanvasRenderer {
    private val labelPaint = Paint().apply {
        isAntiAlias = true
        textSize = 10f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    fun cameraTopLeft(state: GameSessionState, viewW: Float, viewH: Float): Offset {
        val zoom = state.settings.cameraZoom.coerceIn(1.5f, 2.5f)
        val visibleW = viewW / zoom
        val visibleH = viewH / zoom
        val camX = (state.playerX - visibleW / 2f).coerceIn(0f, max(0f, SkulStageGenerator.STAGE_WIDTH - visibleW))
        val camY = (state.playerY - visibleH * 0.62f).coerceIn(0f, max(0f, SkulStageGenerator.STAGE_HEIGHT - visibleH))
        return Offset(camX, camY)
    }

    fun DrawScope.renderWorld(
        state: GameSessionState,
        gestureStroke: List<Offset>,
        nowMs: Long
    ) {
        val viewW = size.width
        val viewH = size.height
        val zoom = state.settings.cameraZoom.coerceIn(1.5f, 2.5f)
        val cam = cameraTopLeft(state, viewW, viewH)
        val visibleW = viewW / zoom
        val visibleH = viewH / zoom

        // 1. Rich Multi-Layered Mokayverse Parallax Sky & Horizons
        drawRect(
            brush = Brush.verticalGradient(listOf(state.currentZone.skyTop, state.currentZone.skyBottom)),
            topLeft = Offset.Zero,
            size = Size(viewW, viewH)
        )
        drawDetailedParallaxBackdrop(
            zone = state.currentZone,
            camX = cam.x,
            viewW = viewW,
            viewH = viewH,
            nowMs = nowMs,
            highParticles = state.settings.highParticles
        )

        val ts = SkulStageGenerator.TILE_SIZE
        val startCol = ((cam.x / ts).toInt() - 1).coerceIn(0, SkulStageGenerator.STAGE_COLS - 1)
        val endCol = (((cam.x + visibleW) / ts).toInt() + 1).coerceIn(0, SkulStageGenerator.STAGE_COLS - 1)
        val startRow = ((cam.y / ts).toInt() - 1).coerceIn(0, SkulStageGenerator.STAGE_ROWS - 1)
        val endRow = (((cam.y + visibleH) / ts).toInt() + 1).coerceIn(0, SkulStageGenerator.STAGE_ROWS - 1)

        withTransform({
            scale(zoom, zoom, Offset.Zero)
            translate(-cam.x, -cam.y)
        }) {
            // 2. Mokayverse Biome Landmarks (Casa Mokay + Marea Viva, Sombravia Teatro/Estación, Gruta Perlas, Oasis)
            drawBiomeWorldLandmarks(
                zone = state.currentZone,
                roomIndex = state.currentRoomIndex,
                groundY = 13 * ts,
                ts = ts,
                nowMs = nowMs
            )

            // 3. Detailed Stage Tiles (Platforms, Biome Floors, Chests, Crates, Furniture, Skull Altar, Lamps)
            for (r in startRow..endRow) {
                for (c in startCol..endCol) {
                    val tile = SkulStageGenerator.tileAt(state.stageData, c, r)
                    if (tile != TileKind.AIR) {
                        drawDetailedStageTile(tile, state.currentZone, c, r, c * ts, r * ts, ts, nowMs)
                    }
                }
            }

            // 4. Ornate Dual Exit Portals (Combat Gold vs Skull/Relic)
            for (portal in state.stageData.portals) {
                drawDetailedPortal(portal = portal, active = state.roomCleared, nowMs = nowMs)
            }

            // 5. Companion Mokay Pets (2D Vector)
            state.petStates.forEach { pet ->
                with(VectorCharacterRenderer) {
                    drawVectorPet(
                        petId = pet.petId,
                        centerX = pet.x,
                        centerY = pet.y,
                        sizePx = 30f,
                        facingLeft = pet.facingLeft,
                        nowMs = nowMs
                    )
                }
            }

            // 6. Dash Afterimages (Skul Speed Ghost Trail!)
            for (ghost in state.afterimages) {
                drawBoxyHero(
                    centerX = ghost.x,
                    bottomY = ghost.y,
                    pixelSize = 1.85f,
                    appearance = state.appearance,
                    activeSkullId = ghost.activeSkullId,
                    facingLeft = ghost.facingLeft,
                    alpha = (ghost.ttlSec / 0.22f).coerceIn(0f, 0.55f)
                )
            }

            // 7. Protagonist BOXY!
            val heroFlash = nowMs < state.invulnerableUntilMs && ((nowMs / 70L) % 2L == 0L)
            drawBoxyHero(
                centerX = state.playerX,
                bottomY = state.playerY,
                pixelSize = 1.95f,
                appearance = state.appearance,
                activeSkullId = state.activeSkullId,
                facingLeft = state.facingLeft,
                walkPhase = state.walkPhase,
                swingProgress = state.swingProgress,
                isDashing = state.dashTimerSec > 0f,
                isDefending = state.isDefending,
                hitFlash = heroFlash
            )

            // 8. Enemies: Laughing Cat, Street Dino, Molotov Mokay (2D Vector)
            for (e in state.enemies) {
                if (!e.alive) continue
                val sizePx = if (e.isBoss) 68f else 42f
                val bounce = abs(sin((nowMs / 160.0 + e.x * 0.02f).toDouble()).toFloat()) * (if (e.isBoss) 3.5f else 2f)
                val flash = nowMs < e.hitFlashMs

                with(VectorCharacterRenderer) {
                    drawVectorEnemy(
                        species = e.species,
                        isBoss = e.isBoss,
                        centerX = e.x,
                        bottomY = e.y - bounce,
                        heightPx = sizePx,
                        facingLeft = e.facingLeft,
                        walkPhase = (nowMs / 110f) + e.x * 0.05f,
                        hitFlash = flash
                    )
                }

                val barW = if (e.isBoss) 60f else 36f
                val barY = e.y - sizePx - bounce - 7f
                drawRect(MokayCharcoal, Offset(e.x - barW / 2f - 1f, barY - 1f), Size(barW + 2f, 5f))
                drawRect(
                    color = if (e.isBoss) MolotovCrimson else RetroDanger,
                    topLeft = Offset(e.x - barW / 2f, barY),
                    size = Size(barW * (e.hp.toFloat() / e.maxHp.toFloat()).coerceIn(0f, 1f), 3f)
                )
            }

            // 9. Projectiles, Slash Arcs, Particles, Floating Combat Text
            for (proj in state.projectiles) {
                drawCircle(proj.color, proj.radius, Offset(proj.x, proj.y))
                drawCircle(MokayCream, proj.radius * 0.45f, Offset(proj.x, proj.y))
            }
            for (fx in state.slashEffects) {
                val prog = 1f - (fx.ttlSec / fx.maxTtlSec).coerceIn(0f, 1f)
                val alpha = (1f - prog).coerceIn(0f, 1f)
                val r = fx.radius * (0.45f + 0.55f * prog)
                when (fx.style) {
                    SlashVisualStyle.PARRY_SHIELD -> {
                        drawCircle(fx.color.copy(alpha = alpha * 0.30f), r, Offset(fx.x, fx.y))
                        drawCircle(fx.color.copy(alpha = alpha), r, Offset(fx.x, fx.y), style = Stroke(4.2f))
                        drawCircle(MokayCream.copy(alpha = alpha * 0.85f), r * 0.72f, Offset(fx.x, fx.y), style = Stroke(2.2f))
                    }
                    SlashVisualStyle.HORIZONTAL_LUNGE -> {
                        val startAngle = if (fx.dirX >= 0f) -45f else 135f
                        drawArc(
                            color = fx.color.copy(alpha = alpha),
                            startAngle = startAngle,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(fx.x - r, fx.y - r * 0.6f),
                            size = Size(r * 2f, r * 1.2f),
                            style = Stroke(width = 5.5f, cap = StrokeCap.Round)
                        )
                        drawLine(
                            color = MokayCream.copy(alpha = alpha),
                            start = Offset(fx.x - fx.dirX * r * 0.8f, fx.y),
                            end = Offset(fx.x + fx.dirX * r * 0.95f, fx.y),
                            strokeWidth = 3.2f,
                            cap = StrokeCap.Round
                        )
                    }
                    SlashVisualStyle.DIAGONAL_CLEAVE -> {
                        val startAngle = if (fx.dirX >= 0f) -95f else 185f
                        drawArc(
                            color = fx.color.copy(alpha = alpha),
                            startAngle = startAngle,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(fx.x - r, fx.y - r),
                            size = Size(r * 2f, r * 2f),
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                        drawLine(
                            color = MokayCream.copy(alpha = alpha),
                            start = Offset(fx.x - fx.dirX * r * 0.4f, fx.y + r * 0.5f),
                            end = Offset(fx.x + fx.dirX * r * 0.85f, fx.y - r * 0.75f),
                            strokeWidth = 3.5f,
                            cap = StrokeCap.Round
                        )
                    }
                    SlashVisualStyle.UPWARD_UPPERCUT -> {
                        drawArc(
                            color = fx.color.copy(alpha = alpha),
                            startAngle = 200f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(fx.x - r * 0.85f, fx.y - r),
                            size = Size(r * 1.7f, r * 2f),
                            style = Stroke(width = 5.8f, cap = StrokeCap.Round)
                        )
                        drawLine(
                            color = MokayCream.copy(alpha = alpha),
                            start = Offset(fx.x, fx.y + r * 0.45f),
                            end = Offset(fx.x, fx.y - r * 0.9f),
                            strokeWidth = 3.2f,
                            cap = StrokeCap.Round
                        )
                    }
                    SlashVisualStyle.ZIGZAG_BOLT -> {
                        val s = r * 0.6f
                        val bolt = Path().apply {
                            moveTo(fx.x - s, fx.y - s)
                            lineTo(fx.x + s * 0.7f, fx.y - s * 0.25f)
                            lineTo(fx.x - s * 0.5f, fx.y + s * 0.25f)
                            lineTo(fx.x + s, fx.y + s)
                        }
                        drawPath(bolt, fx.color.copy(alpha = alpha), style = Stroke(5f, cap = StrokeCap.Round))
                        drawPath(bolt, MokayCream.copy(alpha = alpha), style = Stroke(2.2f, cap = StrokeCap.Round))
                    }
                    SlashVisualStyle.WHIRLWIND_NOVA, SlashVisualStyle.ARC -> {
                        drawCircle(fx.color.copy(alpha = alpha), r, Offset(fx.x, fx.y), style = Stroke(4.2f))
                        if (fx.style == SlashVisualStyle.WHIRLWIND_NOVA) {
                            drawCircle(MokayCream.copy(alpha = alpha * 0.75f), r * 0.65f, Offset(fx.x, fx.y), style = Stroke(2.4f))
                        }
                    }
                }
            }
            for (pt in state.particles) {
                drawRect(pt.color, Offset(pt.x - pt.size / 2f, pt.y - pt.size / 2f), Size(pt.size, pt.size))
            }
            for (ft in state.floatingTexts) {
                val alpha = (ft.ttlSec / 1.1f).coerceIn(0f, 1f)
                labelPaint.color = ft.color.copy(alpha = alpha).toArgb()
                labelPaint.textSize = 10.5f
                drawContext.canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, labelPaint)
            }
        }

        // Ambient Edge Vignette when ambientLighting is enabled
        if (state.settings.ambientLighting) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0x440B0B17)),
                    center = Offset(viewW / 2f, viewH / 2f),
                    radius = max(viewW, viewH) * 0.72f
                ),
                topLeft = Offset.Zero,
                size = Size(viewW, viewH)
            )
        }

        // Gesture Ink Trail (color-coded by recognized attack or defensive parry!)
        if (gestureStroke.size >= 2) {
            val preview = GestureRecognizer.classify(gestureStroke, 100L, state.unlockedGestures)
            val trailOuterColor = when (preview.skill) {
                GestureSkillId.DOWN_PARRY -> BirdySky
                GestureSkillId.UP_SLAM -> RetroMint
                GestureSkillId.DIAGONAL_SLASH -> MokaySpark
                GestureSkillId.Z_SLASH -> BirdySky
                else -> MokayFlame
            }
            val path = Path().apply {
                moveTo(gestureStroke.first().x, gestureStroke.first().y)
                for (i in 1 until gestureStroke.size) lineTo(gestureStroke[i].x, gestureStroke[i].y)
            }
            drawPath(path, trailOuterColor.copy(alpha = 0.50f), style = Stroke(11f, cap = StrokeCap.Round))
            drawPath(path, MokayCream, style = Stroke(4f, cap = StrokeCap.Round))

            // Directional arrowhead tip on end of swipe
            val tip = gestureStroke.last()
            val prev = gestureStroke[max(0, gestureStroke.size - 3)]
            val ang = atan2(tip.y - prev.y, tip.x - prev.x)
            val arrowLen = 14f
            val a1 = ang + 2.55f
            val a2 = ang - 2.55f
            drawLine(
                color = trailOuterColor,
                start = tip,
                end = Offset(tip.x + cos(a1) * arrowLen, tip.y + sin(a1) * arrowLen),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = trailOuterColor,
                start = tip,
                end = Offset(tip.x + cos(a2) * arrowLen, tip.y + sin(a2) * arrowLen),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
        }
    }
}
