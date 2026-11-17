package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.data.GameCatalog
import com.example.ui.theme.BirdySky
import com.example.ui.theme.BloomPink
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.MolotovCrimson
import kotlin.math.abs
import kotlin.math.sin

private val mokayWordPaint = Paint().apply {
    isAntiAlias = true
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
    textAlign = Paint.Align.CENTER
}

internal fun DrawScope.drawStarSparkle(center: Offset, radius: Float, color: Color) {
    val inner = radius * 0.28f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path, color)
}

/**
 * Smooth 2D Vector Renderer for BOXY ("MOKAY LA CAJA - Guardiana de la Chispa").
 * Drawn with crisp cartoon ink outlines, open matchbox drawer with 5 matchsticks,
 * dotted sandpaper strike strip, starry expressive eyes, MOKAY badge, lilac gloves,
 * and chunky sneakers identical to the original Mokayverse character sheet.
 */
fun DrawScope.drawBoxyHero(
    centerX: Float,
    bottomY: Float,
    pixelSize: Float,
    appearance: HeroAppearance,
    activeSkullId: String = "boxy_classic",
    facingLeft: Boolean = false,
    walkPhase: Float = 0f,
    swingProgress: Float = 0f,
    isDashing: Boolean = false,
    isDefending: Boolean = false,
    hitFlash: Boolean = false,
    alpha: Float = 1f
) {
    val u = pixelSize * 1.05f
    val skull = GameCatalog.skullById(activeSkullId)
    val breathe = sin(walkPhase * 0.5f) * 0.45f * u
    val stride = sin(walkPhase) * 2.2f * u
    val strokeW = (u * 0.65f).coerceAtLeast(1.4f)

    val headgear = GameCatalog.cosmeticById(appearance.headgearId)
    val jacket = GameCatalog.cosmeticById(appearance.jacketId)
    val kicks = GameCatalog.cosmeticById(appearance.pantsShoesId)
    val backpack = GameCatalog.cosmeticById(appearance.backpackId)

    val ink = (if (hitFlash) MokayCream else MokayCharcoal).copy(alpha = alpha)
    val boxBodyCol = when (activeSkullId) {
        "boxy_flame" -> Color(0xFFFF865C)
        "boxy_tank" -> Color(0xFF838BB0)
        "boxy_bloom" -> Color(0xFFED8FAF)
        "boxy_birdy" -> Color(0xFF87DCF8)
        else -> MokayLavender
    }.copy(alpha = alpha)
    val rimOrange = skull.accentColor.copy(alpha = alpha)
    val cream = MokayCream.copy(alpha = alpha)
    val gloveLilac = MokayLavender.copy(alpha = alpha)
    val shoeMain = (kicks?.primaryColor ?: MokayLavender).copy(alpha = alpha)
    val shoeSole = (kicks?.secondaryColor ?: MokayCream).copy(alpha = alpha)

    // 0. Ground shadow
    if (alpha > 0.55f) {
        drawOval(
            color = Color(0x440D0D18),
            topLeft = Offset(centerX - 9.2f * u, bottomY - 2.2f * u),
            size = Size(18.4f * u, 3.8f * u)
        )
    }

    withTransform({
        if (facingLeft) {
            scale(-1f, 1f, pivot = Offset(centerX, bottomY))
        }
    }) {
        val bodyLeft = centerX - 5.2f * u
        val bodyTop = bottomY - 17.5f * u - breathe
        val bodyW = 10.8f * u
        val bodyH = 12.6f * u

        // 1. Backpack behind matchbox
        if (backpack != null) {
            val packRect = Offset(bodyLeft - 2.8f * u, bodyTop + 3.2f * u)
            drawRoundRect(
                color = backpack.primaryColor.copy(alpha = alpha),
                topLeft = packRect,
                size = Size(3.6f * u, 6.4f * u),
                cornerRadius = CornerRadius(1.1f * u, 1.1f * u)
            )
            drawRoundRect(
                color = ink,
                topLeft = packRect,
                size = Size(3.6f * u, 6.4f * u),
                cornerRadius = CornerRadius(1.1f * u, 1.1f * u),
                style = Stroke(strokeW)
            )
            drawRoundRect(
                color = backpack.secondaryColor.copy(alpha = alpha),
                topLeft = Offset(packRect.x + 0.6f * u, packRect.y + 1.1f * u),
                size = Size(2.2f * u, 1.8f * u),
                cornerRadius = CornerRadius(0.5f * u, 0.5f * u)
            )
        }

        // 2. Back rubber-hose arm + lilac glove
        val backArmPath = Path().apply {
            moveTo(bodyLeft - 0.6f * u, bodyTop + 6.4f * u)
            quadraticTo(bodyLeft - 3.6f * u, bodyTop + 8.6f * u, bodyLeft - 4.0f * u, bodyTop + 5.6f * u)
        }
        drawPath(backArmPath, ink, style = Stroke(width = 1.45f * u, cap = StrokeCap.Round))
        drawOval(
            color = gloveLilac,
            topLeft = Offset(bodyLeft - 5.4f * u, bodyTop + 3.8f * u),
            size = Size(2.9f * u, 2.7f * u)
        )
        drawOval(
            color = ink,
            topLeft = Offset(bodyLeft - 5.4f * u, bodyTop + 3.8f * u),
            size = Size(2.9f * u, 2.7f * u),
            style = Stroke(strokeW * 0.9f)
        )

        // 3. Curved cartoon legs + chunky Mokay sneakers
        val leftFootX = centerX - 3.6f * u - stride * 0.45f
        val rightFootX = centerX + 1.8f * u + stride * 0.45f
        listOf(
            Triple(centerX - 2.3f * u, leftFootX, bottomY - 5.0f * u),
            Triple(centerX + 2.4f * u, rightFootX, bottomY - 5.0f * u)
        ).forEach { (hipX, footX, hipY) ->
            val legPath = Path().apply {
                moveTo(hipX, hipY)
                quadraticTo(hipX - 0.3f * u, bottomY - 2.5f * u, footX + 1.1f * u, bottomY - 1.9f * u)
            }
            drawPath(legPath, ink, style = Stroke(width = 1.55f * u, cap = StrokeCap.Round))

            // Chunky sneaker body + cream sole
            val shoeTop = bottomY - 2.8f * u
            drawRoundRect(
                color = shoeMain,
                topLeft = Offset(footX - 0.6f * u, shoeTop),
                size = Size(4.1f * u, 2.3f * u),
                cornerRadius = CornerRadius(1.0f * u, 1.0f * u)
            )
            drawRoundRect(
                color = shoeSole,
                topLeft = Offset(footX - 0.6f * u, bottomY - 1.0f * u),
                size = Size(4.1f * u, 0.95f * u),
                cornerRadius = CornerRadius(0.45f * u, 0.45f * u)
            )
            drawRoundRect(
                color = ink,
                topLeft = Offset(footX - 0.6f * u, shoeTop),
                size = Size(4.1f * u, 2.7f * u),
                cornerRadius = CornerRadius(1.0f * u, 1.0f * u),
                style = Stroke(strokeW * 0.9f)
            )
        }

        // 4. Open Matchbox Top Drawer + 5 wooden matchsticks with red/orange bulbous heads
        val drawerLeft = bodyLeft + 0.3f * u
        val drawerTop = bodyTop - 3.6f * u
        val drawerPath = Path().apply {
            moveTo(drawerLeft - 0.8f * u, bodyTop + 0.3f * u)
            lineTo(drawerLeft + 0.5f * u, drawerTop)
            lineTo(bodyLeft + bodyW + 0.2f * u, drawerTop - 0.4f * u)
            lineTo(bodyLeft + bodyW - 0.2f * u, bodyTop + 0.2f * u)
            close()
        }
        drawPath(drawerPath, cream)
        drawPath(drawerPath, ink, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Inner dark drawer cavity
        val cavityPath = Path().apply {
            moveTo(drawerLeft + 1.2f * u, bodyTop + 0.1f * u)
            lineTo(drawerLeft + 1.6f * u, drawerTop + 0.6f * u)
            lineTo(bodyLeft + bodyW - 0.4f * u, drawerTop + 0.3f * u)
            lineTo(bodyLeft + bodyW - 0.6f * u, bodyTop + 0.1f * u)
            close()
        }
        drawPath(cavityPath, Color(0xFF4F382B).copy(alpha = alpha))

        // 5 matchsticks standing cleanly in drawer
        for (m in 0..4) {
            val mx = drawerLeft + 1.75f * u + m * 1.65f * u
            val myTop = drawerTop + 0.55f * u - (if (m % 2 == 0) 0.15f * u else 0f)
            // Wooden stick
            drawRoundRect(
                color = Color(0xFFE5A967).copy(alpha = alpha),
                topLeft = Offset(mx, myTop + 1.1f * u),
                size = Size(1.05f * u, 2.0f * u),
                cornerRadius = CornerRadius(0.25f * u, 0.25f * u)
            )
            // Bulbous match head
            val headCol = (if (m % 2 == 0) MokayFlame else MolotovCrimson).copy(alpha = alpha)
            drawOval(
                color = headCol,
                topLeft = Offset(mx - 0.18f * u, myTop),
                size = Size(1.42f * u, 1.55f * u)
            )
            drawOval(
                color = ink,
                topLeft = Offset(mx - 0.18f * u, myTop),
                size = Size(1.42f * u, 1.55f * u),
                style = Stroke(strokeW * 0.55f)
            )
            drawCircle(Color.White.copy(alpha = alpha * 0.7f), 0.22f * u, Offset(mx + 0.32f * u, myTop + 0.45f * u))
        }

        // Active Skul-Form vector flair (Flames, Birdy Wings, Tank Pauldrons, Bloom Halo)
        when (activeSkullId) {
            "boxy_flame" -> {
                val fl = sin(walkPhase * 1.7f) * 0.7f * u
                val flamePath = Path().apply {
                    moveTo(centerX - 3.2f * u, drawerTop - 0.2f * u)
                    quadraticTo(centerX - 2.0f * u, drawerTop - 4.2f * u + fl, centerX, drawerTop - 5.2f * u - fl)
                    quadraticTo(centerX + 2.4f * u, drawerTop - 4.0f * u + fl, centerX + 3.4f * u, drawerTop - 0.2f * u)
                    close()
                }
                drawPath(flamePath, MokayFlame.copy(alpha = alpha))
                drawPath(flamePath, ink, style = Stroke(strokeW * 0.8f))
            }
            "boxy_birdy" -> {
                val wingPath = Path().apply {
                    moveTo(bodyLeft - 0.4f * u, bodyTop + 3.8f * u)
                    quadraticTo(bodyLeft - 4.2f * u, bodyTop + 1.0f * u - stride * 0.4f, bodyLeft - 3.4f * u, bodyTop + 6.8f * u)
                    close()
                }
                drawPath(wingPath, BirdySky.copy(alpha = alpha))
                drawPath(wingPath, ink, style = Stroke(strokeW * 0.8f))
            }
            "boxy_tank" -> {
                drawCircle(MokaySpark.copy(alpha = alpha), 1.5f * u, Offset(bodyLeft + 0.6f * u, bodyTop + 2.4f * u))
                drawCircle(MokaySpark.copy(alpha = alpha), 1.5f * u, Offset(bodyLeft + bodyW - 0.6f * u, bodyTop + 2.4f * u))
            }
            "boxy_bloom" -> {
                drawOval(
                    color = BloomPink.copy(alpha = alpha),
                    topLeft = Offset(centerX - 2.8f * u, drawerTop - 2.2f * u),
                    size = Size(5.6f * u, 1.3f * u),
                    style = Stroke(strokeW)
                )
            }
        }

        // 5. Left 3D Sandpaper Strike Edge Panel
        val strikeSide = Path().apply {
            moveTo(bodyLeft - 1.6f * u, bodyTop + 0.6f * u)
            lineTo(bodyLeft + 0.6f * u, bodyTop)
            lineTo(bodyLeft + 0.6f * u, bodyTop + bodyH)
            lineTo(bodyLeft - 1.6f * u, bodyTop + bodyH - 0.5f * u)
            close()
        }
        drawPath(strikeSide, cream)
        drawPath(strikeSide, ink, style = Stroke(strokeW, join = StrokeJoin.Round))

        val sandpaper = Path().apply {
            moveTo(bodyLeft - 1.25f * u, bodyTop + 1.1f * u)
            lineTo(bodyLeft + 0.2f * u, bodyTop + 0.6f * u)
            lineTo(bodyLeft + 0.2f * u, bodyTop + bodyH - 0.6f * u)
            lineTo(bodyLeft - 1.25f * u, bodyTop + bodyH - 1.0f * u)
            close()
        }
        drawPath(sandpaper, Color(0xFF3E1C13).copy(alpha = alpha))
        for (dotRow in 0..5) {
            val dy = bodyTop + 1.8f * u + dotRow * 1.7f * u
            drawCircle(MokayFlame.copy(alpha = alpha), 0.28f * u, Offset(bodyLeft - 0.65f * u, dy))
            drawCircle(MokayFlame.copy(alpha = alpha), 0.24f * u, Offset(bodyLeft - 0.25f * u, dy + 0.7f * u))
        }

        // 6. Front Boxy Panel (Orange outer frame, Cream trim, Lavender face)
        drawRoundRect(
            color = rimOrange,
            topLeft = Offset(bodyLeft + 0.5f * u, bodyTop),
            size = Size(bodyW - 0.5f * u, bodyH),
            cornerRadius = CornerRadius(0.6f * u, 0.6f * u)
        )
        drawRoundRect(
            color = cream,
            topLeft = Offset(bodyLeft + 1.0f * u, bodyTop + 0.5f * u),
            size = Size(bodyW - 1.5f * u, bodyH - 1.0f * u),
            cornerRadius = CornerRadius(0.45f * u, 0.45f * u)
        )
        drawRoundRect(
            color = boxBodyCol,
            topLeft = Offset(bodyLeft + 1.45f * u, bodyTop + 0.95f * u),
            size = Size(bodyW - 2.4f * u, bodyH - 1.9f * u),
            cornerRadius = CornerRadius(0.35f * u, 0.35f * u)
        )
        drawRoundRect(
            color = ink,
            topLeft = Offset(bodyLeft + 0.5f * u, bodyTop),
            size = Size(bodyW - 0.5f * u, bodyH),
            cornerRadius = CornerRadius(0.6f * u, 0.6f * u),
            style = Stroke(strokeW)
        )

        // Optional Streetwear Jacket / Vest trim on lower front torso
        if (jacket != null && jacket.id != "mokay_hoodie_flame") {
            drawRoundRect(
                color = jacket.primaryColor.copy(alpha = alpha * 0.92f),
                topLeft = Offset(bodyLeft + 1.45f * u, bodyTop + 8.3f * u),
                size = Size(bodyW - 2.4f * u, 3.3f * u),
                cornerRadius = CornerRadius(0.3f * u, 0.3f * u)
            )
            drawRect(
                color = jacket.secondaryColor.copy(alpha = alpha),
                topLeft = Offset(bodyLeft + 1.65f * u, bodyTop + 8.4f * u),
                size = Size(bodyW - 2.8f * u, 0.65f * u)
            )
        }

        // 7. Big Expressive Cartoon Eyes + 4-Point Star Sparkle + Wide Grin
        val faceCx = bodyLeft + 0.5f * u + (bodyW - 0.5f * u) * 0.52f
        val eyeY = bodyTop + 4.3f * u
        val eyeW = 2.45f * u
        val eyeH = 3.15f * u

        // Left & Right White Sclera Ovals
        listOf(faceCx - 2.35f * u, faceCx - 0.05f * u).forEachIndexed { idx, ex ->
            drawOval(
                color = Color.White.copy(alpha = alpha),
                topLeft = Offset(ex, eyeY - eyeH * 0.5f),
                size = Size(eyeW, eyeH)
            )
            drawOval(
                color = ink,
                topLeft = Offset(ex, eyeY - eyeH * 0.5f),
                size = Size(eyeW, eyeH),
                style = Stroke(strokeW * 0.72f)
            )
            // Pupil
            val pxCenter = ex + eyeW * (if (idx == 0) 0.58f else 0.52f)
            drawOval(
                color = ink,
                topLeft = Offset(pxCenter - 0.72f * u, eyeY - 0.95f * u),
                size = Size(1.44f * u, 1.95f * u)
            )
            // Signature 4-point white star glint inside pupil!
            drawStarSparkle(Offset(pxCenter - 0.12f * u, eyeY - 0.18f * u), 0.58f * u, Color.White.copy(alpha = alpha))
        }

        // Expressive slanted cartoon eyebrows
        drawLine(ink, Offset(faceCx - 2.4f * u, eyeY - 2.0f * u), Offset(faceCx - 0.4f * u, eyeY - 1.45f * u), strokeWidth = strokeW * 1.15f, cap = StrokeCap.Round)
        drawLine(ink, Offset(faceCx + 2.4f * u, eyeY - 2.0f * u), Offset(faceCx + 0.4f * u, eyeY - 1.45f * u), strokeWidth = strokeW * 1.15f, cap = StrokeCap.Round)

        // Wide Expressive Cartoon Smile + Teeth + Red-Orange Tongue
        val mouthRectTop = bodyTop + 6.45f * u
        val mouthPath = Path().apply {
            moveTo(faceCx - 2.8f * u, mouthRectTop)
            quadraticTo(faceCx, mouthRectTop + 0.5f * u, faceCx + 2.8f * u, mouthRectTop)
            quadraticTo(faceCx + 2.5f * u, mouthRectTop + 2.55f * u, faceCx, mouthRectTop + 2.55f * u)
            quadraticTo(faceCx - 2.5f * u, mouthRectTop + 2.55f * u, faceCx - 2.8f * u, mouthRectTop)
            close()
        }
        drawPath(mouthPath, ink)
        // Upper teeth arc
        val teethPath = Path().apply {
            moveTo(faceCx - 2.3f * u, mouthRectTop + 0.25f * u)
            quadraticTo(faceCx, mouthRectTop + 0.6f * u, faceCx + 2.3f * u, mouthRectTop + 0.25f * u)
            lineTo(faceCx + 2.1f * u, mouthRectTop + 0.85f * u)
            quadraticTo(faceCx, mouthRectTop + 1.15f * u, faceCx - 2.1f * u, mouthRectTop + 0.85f * u)
            close()
        }
        drawPath(teethPath, Color.White.copy(alpha = alpha))
        // Tongue
        drawOval(
            color = MokayFlame.copy(alpha = alpha),
            topLeft = Offset(faceCx - 1.45f * u, mouthRectTop + 1.35f * u),
            size = Size(2.9f * u, 1.05f * u)
        )

        // Signature orange MOKAY badge & star spark on lower chest
        mokayWordPaint.color = ink.toArgb()
        mokayWordPaint.textSize = (1.85f * u).coerceAtLeast(5f)
        drawContext.canvas.nativeCanvas.drawText("MOKAY", faceCx + 0.1f * u, bodyTop + 11.2f * u, mokayWordPaint)
        mokayWordPaint.color = MokayFlame.copy(alpha = alpha).toArgb()
        drawContext.canvas.nativeCanvas.drawText("MOKAY", faceCx, bodyTop + 11.05f * u, mokayWordPaint)
        drawStarSparkle(Offset(faceCx + 2.85f * u, bodyTop + 9.45f * u), 0.72f * u, MokaySpark.copy(alpha = alpha))

        // 8. Vector Streetwear Headgear on Boxy
        if (headgear != null) {
            val h1 = headgear.primaryColor.copy(alpha = alpha)
            val h2 = headgear.secondaryColor.copy(alpha = alpha)
            when (headgear.id) {
                "mokay_cap_classic" -> {
                    val capTop = drawerTop - 1.1f * u
                    drawRoundRect(
                        color = h1,
                        topLeft = Offset(drawerLeft + 0.6f * u, capTop),
                        size = Size(bodyW - 1.0f * u, 1.85f * u),
                        cornerRadius = CornerRadius(0.9f * u, 0.9f * u)
                    )
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(drawerLeft + 0.6f * u, capTop),
                        size = Size(bodyW - 1.0f * u, 1.85f * u),
                        cornerRadius = CornerRadius(0.9f * u, 0.9f * u),
                        style = Stroke(strokeW * 0.8f)
                    )
                    // Cap visor pointing forward
                    drawRoundRect(
                        color = h2,
                        topLeft = Offset(bodyLeft + bodyW - 2.0f * u, capTop + 0.9f * u),
                        size = Size(4.4f * u, 0.95f * u),
                        cornerRadius = CornerRadius(0.5f * u, 0.5f * u)
                    )
                }
                "mokay_beanie_knit" -> {
                    val beanieTop = drawerTop - 1.5f * u
                    drawRoundRect(
                        color = h1,
                        topLeft = Offset(drawerLeft + 0.4f * u, beanieTop),
                        size = Size(bodyW - 0.6f * u, 2.0f * u),
                        cornerRadius = CornerRadius(1.0f * u, 1.0f * u)
                    )
                    drawCircle(h2, 0.9f * u, Offset(centerX + 0.2f * u, beanieTop - 0.3f * u))
                }
                "mokay_bandana_paisley" -> {
                    drawRoundRect(
                        color = h1,
                        topLeft = Offset(drawerLeft + 0.2f * u, drawerTop + 0.1f * u),
                        size = Size(bodyW - 0.2f * u, 1.1f * u),
                        cornerRadius = CornerRadius(0.4f * u, 0.4f * u)
                    )
                    val knotTail = Path().apply {
                        moveTo(drawerLeft + 0.2f * u, drawerTop + 0.6f * u)
                        lineTo(drawerLeft - 2.4f * u, drawerTop + 0.2f * u)
                        lineTo(drawerLeft - 1.9f * u, drawerTop + 1.6f * u)
                        close()
                    }
                    drawPath(knotTail, h2)
                }
            }
        }

        // 9. Front Animated Arm + Lilac Glove + Vector Weapon
        val swing = if (swingProgress > 0f) abs(sin(swingProgress * Math.PI.toFloat())) else 0f
        val handCx = bodyLeft + bodyW + 2.1f * u + swing * 2.6f * u
        val handCy = bodyTop + 5.6f * u - swing * 2.4f * u
        val frontArm = Path().apply {
            moveTo(bodyLeft + bodyW - 0.3f * u, bodyTop + 6.4f * u)
            quadraticTo(bodyLeft + bodyW + 1.8f * u, bodyTop + 8.0f * u, handCx, handCy)
        }
        drawPath(frontArm, ink, style = Stroke(width = 1.45f * u, cap = StrokeCap.Round))
        drawOval(
            color = gloveLilac,
            topLeft = Offset(handCx - 1.4f * u, handCy - 1.3f * u),
            size = Size(2.8f * u, 2.6f * u)
        )
        drawOval(
            color = ink,
            topLeft = Offset(handCx - 1.4f * u, handCy - 1.3f * u),
            size = Size(2.8f * u, 2.6f * u),
            style = Stroke(strokeW * 0.9f)
        )

        val bladeCol = when (activeSkullId) {
            "boxy_flame" -> MokayFlame
            "boxy_tank" -> MolotovCrimson
            "boxy_bloom" -> BloomPink
            "boxy_birdy" -> BirdySky
            else -> MokaySpark
        }.copy(alpha = alpha)

        if (activeSkullId == "boxy_birdy" || activeSkullId == "boxy_bloom") {
            // Sleek curved Bow / Posca Blaster
            drawArc(
                color = bladeCol,
                startAngle = -65f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(handCx - 0.6f * u, handCy - 3.6f * u),
                size = Size(3.2f * u, 7.2f * u),
                style = Stroke(width = 1.1f * u, cap = StrokeCap.Round)
            )
            drawLine(cream, Offset(handCx + 0.8f * u, handCy - 3.1f * u), Offset(handCx + 0.8f * u, handCy + 3.1f * u), strokeWidth = strokeW * 0.5f)
        } else if (activeSkullId == "boxy_tank") {
            // Vector Hammer
            drawLine(cream, Offset(handCx + 0.8f * u, handCy + 1.2f * u), Offset(handCx + 2.0f * u, handCy - 4.4f * u), strokeWidth = 0.95f * u, cap = StrokeCap.Round)
            drawRoundRect(
                color = bladeCol,
                topLeft = Offset(handCx + 0.4f * u, handCy - 6.0f * u),
                size = Size(3.6f * u, 2.4f * u),
                cornerRadius = CornerRadius(0.5f * u, 0.5f * u)
            )
            drawRoundRect(
                color = ink,
                topLeft = Offset(handCx + 0.4f * u, handCy - 6.0f * u),
                size = Size(3.6f * u, 2.4f * u),
                cornerRadius = CornerRadius(0.5f * u, 0.5f * u),
                style = Stroke(strokeW * 0.7f)
            )
        } else {
            // Crisp curved Mokay Vector Sword Blade + Guard
            val bladePath = Path().apply {
                moveTo(handCx + 0.4f * u, handCy - 0.2f * u)
                lineTo(handCx + 1.6f * u, handCy - 6.2f * u)
                quadraticTo(handCx + 2.4f * u, handCy - 4.5f * u, handCx + 1.8f * u, handCy - 0.2f * u)
                close()
            }
            drawPath(bladePath, bladeCol)
            drawPath(bladePath, ink, style = Stroke(strokeW * 0.75f, join = StrokeJoin.Round))
            drawRoundRect(
                color = cream,
                topLeft = Offset(handCx - 0.3f * u, handCy - 0.4f * u),
                size = Size(2.8f * u, 0.85f * u),
                cornerRadius = CornerRadius(0.3f * u, 0.3f * u)
            )
        }
    }

    // 10. Dash Glow / Defensive Shield Aura
    if (isDashing) {
        drawCircle(
            color = rimOrange.copy(alpha = 0.26f),
            radius = 14f * u,
            center = Offset(centerX, bottomY - 9.5f * u)
        )
    }
    if (isDefending) {
        drawCircle(
            color = BirdySky.copy(alpha = 0.24f),
            radius = 15f * u,
            center = Offset(centerX, bottomY - 10.5f * u)
        )
        drawCircle(
            color = BirdySky,
            radius = 15f * u,
            center = Offset(centerX, bottomY - 10.5f * u),
            style = Stroke(width = 2.6f)
        )
        drawCircle(
            color = MokayCream.copy(alpha = 0.8f),
            radius = 12.2f * u,
            center = Offset(centerX, bottomY - 10.5f * u),
            style = Stroke(width = 1.4f)
        )
    }
}

@Composable
fun BoxyAvatarPreview(
    appearance: HeroAppearance,
    activeSkullId: String = "boxy_classic",
    modifier: Modifier = Modifier,
    walkPhase: Float = 0f
) {
    Canvas(modifier = modifier) {
        val unit = (size.minDimension / 24.5f).coerceAtLeast(1.8f)
        drawBoxyHero(
            centerX = size.width / 2f,
            bottomY = size.height * 0.91f,
            pixelSize = unit,
            appearance = appearance,
            activeSkullId = activeSkullId,
            walkPhase = walkPhase
        )
    }
}
