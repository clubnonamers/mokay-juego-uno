package com.example.game

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
import com.example.ui.theme.BirdySky
import com.example.ui.theme.BloomPink
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.MolotovCrimson
import com.example.ui.theme.RetroMint
import kotlin.math.abs
import kotlin.math.sin

/**
 * Smooth 2D Vector Renderers for all Mokayverse enemies (Street Dino, Laughing Cat, Molotov Mokay)
 * and companion characters (Mokay el Fósforo, Bloom, Birdy, Floko, Zenkay) faithful to the original
 * Mokay character reference sheets.
 */
object VectorCharacterRenderer {

    fun DrawScope.drawVectorEnemy(
        species: EnemySpecies,
        isBoss: Boolean,
        centerX: Float,
        bottomY: Float,
        heightPx: Float,
        facingLeft: Boolean,
        walkPhase: Float,
        hitFlash: Boolean
    ) {
        val u = heightPx / 24f
        val strokeW = (u * 0.72f).coerceAtLeast(1.4f)
        val alpha = if (hitFlash) 0.55f else 1f
        val ink = (if (hitFlash) MokayCream else MokayCharcoal).copy(alpha = alpha)

        drawOval(
            color = Color(0x50000000),
            topLeft = Offset(centerX - 8f * u, bottomY - 2f * u),
            size = Size(16f * u, 3.6f * u)
        )
        if (isBoss) {
            drawCircle(MolotovCrimson.copy(alpha = 0.22f), 14f * u, Offset(centerX, bottomY - 12f * u))
        }

        withTransform({
            if (facingLeft) {
                scale(-1f, 1f, pivot = Offset(centerX, bottomY))
            }
        }) {
            when (species) {
                EnemySpecies.STREET_DINO -> drawStreetDino(centerX, bottomY, u, strokeW, walkPhase, ink, alpha)
                EnemySpecies.LAUGHING_CAT -> drawLaughingCat(centerX, bottomY, u, strokeW, walkPhase, ink, alpha)
                EnemySpecies.MOLOTOV_MOKAY -> drawMolotovMokay(centerX, bottomY, u, strokeW, walkPhase, isBoss, ink, alpha)
            }
        }
    }

    fun DrawScope.drawVectorPet(
        petId: String,
        centerX: Float,
        centerY: Float,
        sizePx: Float,
        facingLeft: Boolean,
        nowMs: Long
    ) {
        val u = sizePx / 20f
        val strokeW = (u * 0.7f).coerceAtLeast(1.2f)
        val hop = abs(sin((nowMs / 190.0 + petId.hashCode()).toDouble()).toFloat()) * 2.5f
        val bottomY = centerY + sizePx * 0.45f - hop

        withTransform({
            if (facingLeft) {
                scale(-1f, 1f, pivot = Offset(centerX, bottomY))
            }
        }) {
            when (petId) {
                "mokay" -> drawMokayMatchstick(centerX, bottomY, u, strokeW, nowMs)
                "bloom" -> drawBloomBuddy(centerX, bottomY, u, strokeW, nowMs)
                "birdy" -> drawBirdyBuddy(centerX, bottomY, u, strokeW, nowMs)
                "floko" -> drawFlokoBuddy(centerX, bottomY, u, strokeW)
                else -> drawZenkayBuddy(centerX, bottomY, u, strokeW)
            }
        }
    }

    /** T-REX 'STREET DINO': Backwards snapback, emerald snout + teeth, orange hoodie, lavender-teal zigzag vest, cargo chain & multicolor sneakers. */
    private fun DrawScope.drawStreetDino(
        cx: Float,
        by: Float,
        u: Float,
        strokeW: Float,
        walkPhase: Float,
        ink: Color,
        alpha: Float
    ) {
        val dinoGreen = Color(0xFF39945C).copy(alpha = alpha)
        val dinoTealSpot = Color(0xFF1F6652).copy(alpha = alpha)
        val cream = MokayCream.copy(alpha = alpha)
        val lavender = MokayLavender.copy(alpha = alpha)
        val hoodieOrange = MokayFlame.copy(alpha = alpha)
        val stride = sin(walkPhase) * 1.4f * u

        // Tail sweeping behind
        val tailPath = Path().apply {
            moveTo(cx - 2.5f * u, by - 5.5f * u)
            quadraticTo(cx - 11.5f * u, by - 4.5f * u, cx - 10.0f * u, by - 12.2f * u)
            quadraticTo(cx - 8.2f * u, by - 8.0f * u, cx - 2.2f * u, by - 8.2f * u)
            close()
        }
        drawPath(tailPath, dinoGreen)
        drawPath(tailPath, ink, style = Stroke(strokeW, join = StrokeJoin.Round))
        drawCircle(dinoTealSpot, 1.0f * u, Offset(cx - 8.0f * u, by - 8.2f * u))

        // Chunky colorful sneakers (Teal, Cream, Coral)
        listOf(cx - 3.6f * u - stride * 0.4f, cx + 0.8f * u + stride * 0.4f).forEach { fx ->
            drawRoundRect(Color(0xFF359E8E).copy(alpha = alpha), Offset(fx, by - 3.0f * u), Size(4.0f * u, 2.2f * u), CornerRadius(0.9f * u))
            drawRoundRect(cream, Offset(fx, by - 1.1f * u), Size(4.0f * u, 1.0f * u), CornerRadius(0.4f * u))
            drawCircle(hoodieOrange, 0.65f * u, Offset(fx + 2.6f * u, by - 2.3f * u))
            drawRoundRect(ink, Offset(fx, by - 3.0f * u), Size(4.0f * u, 2.9f * u), CornerRadius(0.9f * u), style = Stroke(strokeW * 0.8f))
        }

        // Black cargo pants + silver wallet chain
        drawRoundRect(ink, Offset(cx - 4.0f * u, by - 8.8f * u), Size(8.0f * u, 6.2f * u), CornerRadius(1.2f * u))
        val chainPath = Path().apply {
            moveTo(cx - 3.2f * u, by - 8.2f * u)
            quadraticTo(cx - 1.0f * u, by - 5.5f * u, cx + 1.2f * u, by - 7.8f * u)
        }
        drawPath(chainPath, Color(0xFFB5B9C4).copy(alpha = alpha), style = Stroke(strokeW * 0.85f, cap = StrokeCap.Round))

        // Orange hoodie + Lavender varsity vest with teal/black zigzag stripe
        drawRoundRect(hoodieOrange, Offset(cx - 4.4f * u, by - 15.2f * u), Size(8.8f * u, 7.0f * u), CornerRadius(1.8f * u))
        drawRoundRect(lavender, Offset(cx - 4.6f * u, by - 14.5f * u), Size(9.2f * u, 5.8f * u), CornerRadius(1.4f * u))
        drawRect(hoodieOrange, Offset(cx - 1.2f * u, by - 14.5f * u), Size(2.4f * u, 5.8f * u))
        // Zigzag vest stripe
        val zig = Path().apply {
            moveTo(cx - 4.4f * u, by - 11.4f * u)
            lineTo(cx - 2.8f * u, by - 12.6f * u)
            lineTo(cx - 1.4f * u, by - 11.4f * u)
            moveTo(cx + 1.4f * u, by - 11.4f * u)
            lineTo(cx + 2.8f * u, by - 12.6f * u)
            lineTo(cx + 4.4f * u, by - 11.4f * u)
        }
        drawPath(zig, dinoTealSpot, style = Stroke(strokeW * 1.1f, cap = StrokeCap.Round))
        drawRoundRect(ink, Offset(cx - 4.6f * u, by - 14.5f * u), Size(9.2f * u, 5.8f * u), CornerRadius(1.4f * u), style = Stroke(strokeW))

        // Cream varsity sleeve + clawed hand
        drawOval(cream, Offset(cx - 5.8f * u, by - 14.0f * u), Size(3.0f * u, 4.4f * u))
        drawOval(ink, Offset(cx - 5.8f * u, by - 14.0f * u), Size(3.0f * u, 4.4f * u), style = Stroke(strokeW * 0.8f))
        drawCircle(dinoGreen, 1.3f * u, Offset(cx + 4.8f * u, by - 11.2f * u))
        drawCircle(ink, 1.3f * u, Offset(cx + 4.8f * u, by - 11.2f * u), style = Stroke(strokeW * 0.75f))

        // T-Rex Head + Wide Roaring Jaw + Sharp White Teeth
        val headPath = Path().apply {
            moveTo(cx - 3.6f * u, by - 14.8f * u)
            quadraticTo(cx - 4.2f * u, by - 20.5f * u, cx + 1.0f * u, by - 20.6f * u)
            lineTo(cx + 6.6f * u, by - 19.6f * u)
            quadraticTo(cx + 7.4f * u, by - 17.6f * u, cx + 5.6f * u, by - 17.0f * u)
            lineTo(cx + 0.8f * u, by - 17.2f * u)
            lineTo(cx + 5.4f * u, by - 15.2f * u)
            quadraticTo(cx + 3.2f * u, by - 13.6f * u, cx - 2.4f * u, by - 14.4f * u)
            close()
        }
        drawPath(headPath, dinoGreen)
        // Roaring open mouth interior
        val jawPath = Path().apply {
            moveTo(cx + 0.8f * u, by - 17.2f * u)
            lineTo(cx + 5.6f * u, by - 17.0f * u)
            lineTo(cx + 4.6f * u, by - 15.1f * u)
            close()
        }
        drawPath(jawPath, Color(0xFF6B1E1A).copy(alpha = alpha))
        // Sharp triangular teeth
        for (t in 0..3) {
            val tx = cx + 1.6f * u + t * 1.0f * u
            drawCircle(Color.White.copy(alpha = alpha), 0.38f * u, Offset(tx, by - 17.0f * u))
            drawCircle(Color.White.copy(alpha = alpha), 0.34f * u, Offset(tx, by - 15.5f * u))
        }
        drawPath(headPath, ink, style = Stroke(strokeW, join = StrokeJoin.Round))
        // Fierce Dino Eye
        drawCircle(cream, 1.0f * u, Offset(cx + 0.5f * u, by - 19.2f * u))
        drawCircle(ink, 0.45f * u, Offset(cx + 0.7f * u, by - 19.2f * u))

        // Reversed Backwards Snapback Cap (Orange crown, Lavender brim behind!)
        drawArc(hoodieOrange, 180f, 180f, true, Offset(cx - 3.6f * u, by - 22.8f * u), Size(6.4f * u, 4.4f * u))
        drawArc(ink, 180f, 180f, true, Offset(cx - 3.6f * u, by - 22.8f * u), Size(6.4f * u, 4.4f * u), style = Stroke(strokeW * 0.85f))
        drawRoundRect(lavender, Offset(cx - 6.8f * u, by - 20.9f * u), Size(4.2f * u, 1.15f * u), CornerRadius(0.5f * u))
        drawRoundRect(ink, Offset(cx - 6.8f * u, by - 20.9f * u), Size(4.2f * u, 1.15f * u), CornerRadius(0.5f * u), style = Stroke(strokeW * 0.7f))
    }

    /** 'LAUGHING CAT': Sleek black cat, orange knit beanie, wide BWAHAHA grin, lavender-teal vest & S-tail. */
    private fun DrawScope.drawLaughingCat(
        cx: Float,
        by: Float,
        u: Float,
        strokeW: Float,
        walkPhase: Float,
        ink: Color,
        alpha: Float
    ) {
        val catBlack = Color(0xFF17171D).copy(alpha = alpha)
        val beanieOrange = MokayFlame.copy(alpha = alpha)
        val lavender = MokayLavender.copy(alpha = alpha)
        val teal = Color(0xFF48B5A3).copy(alpha = alpha)
        val bounce = abs(sin(walkPhase)) * 1.1f * u

        // Curved black cat S-tail
        val tail = Path().apply {
            moveTo(cx - 3.0f * u, by - 5.0f * u)
            cubicTo(cx - 8.5f * u, by - 4.0f * u, cx - 7.5f * u, by - 12.0f * u, cx - 10.2f * u, by - 10.5f * u)
        }
        drawPath(tail, catBlack, style = Stroke(width = 1.6f * u, cap = StrokeCap.Round))

        // Cat legs + paws
        drawRoundRect(catBlack, Offset(cx - 3.0f * u, by - 5.5f * u), Size(2.2f * u, 5.2f * u), CornerRadius(0.9f * u))
        drawRoundRect(catBlack, Offset(cx + 0.8f * u, by - 5.5f * u), Size(2.2f * u, 5.2f * u), CornerRadius(0.9f * u))

        // Torso + Lavender Vest with teal trim
        drawRoundRect(catBlack, Offset(cx - 4.2f * u, by - 12.2f * u - bounce), Size(8.4f * u, 7.4f * u), CornerRadius(2.2f * u))
        drawRoundRect(lavender, Offset(cx - 4.0f * u, by - 11.8f * u - bounce), Size(8.0f * u, 6.2f * u), CornerRadius(1.5f * u))
        drawRect(teal, Offset(cx - 4.0f * u, by - 11.4f * u - bounce), Size(8.0f * u, 1.1f * u))
        drawRoundRect(ink, Offset(cx - 4.0f * u, by - 11.8f * u - bounce), Size(8.0f * u, 6.2f * u), CornerRadius(1.5f * u), style = Stroke(strokeW * 0.8f))

        // Round Black Cat Head + Pointed Ears
        val headCy = by - 16.2f * u - bounce
        val earLeft = Path().apply {
            moveTo(cx - 4.4f * u, headCy - 1.5f * u)
            lineTo(cx - 5.2f * u, headCy - 6.0f * u)
            lineTo(cx - 1.4f * u, headCy - 3.6f * u)
            close()
        }
        val earRight = Path().apply {
            moveTo(cx + 4.4f * u, headCy - 1.5f * u)
            lineTo(cx + 5.2f * u, headCy - 6.0f * u)
            lineTo(cx + 1.4f * u, headCy - 3.6f * u)
            close()
        }
        drawPath(earLeft, catBlack)
        drawPath(earRight, catBlack)
        drawOval(catBlack, Offset(cx - 5.2f * u, headCy - 4.4f * u), Size(10.4f * u, 8.8f * u))

        // Orange Knit Beanie between ears
        drawArc(beanieOrange, 180f, 180f, true, Offset(cx - 3.5f * u, headCy - 6.8f * u), Size(7.0f * u, 4.6f * u))
        drawRoundRect(beanieOrange, Offset(cx - 3.7f * u, headCy - 4.8f * u), Size(7.4f * u, 1.4f * u), CornerRadius(0.6f * u))
        drawRoundRect(ink, Offset(cx - 3.7f * u, headCy - 4.8f * u), Size(7.4f * u, 1.4f * u), CornerRadius(0.6f * u), style = Stroke(strokeW * 0.7f))

        // Happy Closed ^ ^ Eyes
        drawArc(Color.White.copy(alpha = alpha), 195f, 150f, false, Offset(cx - 3.2f * u, headCy - 2.3f * u), Size(2.4f * u, 1.8f * u), style = Stroke(strokeW * 0.9f, cap = StrokeCap.Round))
        drawArc(Color.White.copy(alpha = alpha), 195f, 150f, false, Offset(cx + 0.8f * u, headCy - 2.3f * u), Size(2.4f * u, 1.8f * u), style = Stroke(strokeW * 0.9f, cap = StrokeCap.Round))

        // Huge Laughing Open Grin ("BWAHAHAHA!") + Teeth + Tongue
        val laughMouth = Path().apply {
            moveTo(cx - 3.6f * u, headCy + 0.1f * u)
            lineTo(cx + 3.6f * u, headCy + 0.1f * u)
            quadraticTo(cx + 3.2f * u, headCy + 3.8f * u, cx, headCy + 3.8f * u)
            quadraticTo(cx - 3.2f * u, headCy + 3.8f * u, cx - 3.6f * u, headCy + 0.1f * u)
            close()
        }
        drawPath(laughMouth, Color(0xFF451218).copy(alpha = alpha))
        drawRect(Color.White.copy(alpha = alpha), Offset(cx - 2.8f * u, headCy + 0.2f * u), Size(5.6f * u, 0.8f * u))
        drawOval(Color(0xFFE85C68).copy(alpha = alpha), Offset(cx - 1.9f * u, headCy + 1.8f * u), Size(3.8f * u, 1.6f * u))
        drawPath(laughMouth, Color.White.copy(alpha = alpha * 0.45f), style = Stroke(strokeW * 0.5f))
    }

    /** 'MOLOTOV MOKAY - THE LITERAL PUNK': Glass bottle with orange fuel, bandana fuse, studded denim vest & combat boots. */
    private fun DrawScope.drawMolotovMokay(
        cx: Float,
        by: Float,
        u: Float,
        strokeW: Float,
        walkPhase: Float,
        isBoss: Boolean,
        ink: Color,
        alpha: Float
    ) {
        val bottleGlass = Color(0xFFD0DED7).copy(alpha = alpha)
        val amberFuel = Color(0xFFE67824).copy(alpha = alpha)
        val bandanaRust = Color(0xFFB3382C).copy(alpha = alpha)
        val denimDark = Color(0xFF2B3D4B).copy(alpha = alpha)
        val bootBrown = Color(0xFF4B3328).copy(alpha = alpha)
        val skinPale = Color(0xFFDADCD2).copy(alpha = alpha)
        val stride = sin(walkPhase) * 1.2f * u

        // Heavy spiked combat boots
        listOf(cx - 3.6f * u - stride * 0.35f, cx + 0.6f * u + stride * 0.35f).forEach { bx ->
            drawRoundRect(bootBrown, Offset(bx, by - 3.4f * u), Size(3.2f * u, 3.3f * u), CornerRadius(0.8f * u))
            drawRoundRect(ink, Offset(bx, by - 3.4f * u), Size(3.2f * u, 3.3f * u), CornerRadius(0.8f * u), style = Stroke(strokeW * 0.85f))
            drawCircle(Color(0xFFB5B9C4).copy(alpha = alpha), 0.35f * u, Offset(bx + 0.4f * u, by - 2.1f * u))
        }

        // Bottle Glass Body (wide base tapering cleanly up to bottle neck)
        val bottlePath = Path().apply {
            moveTo(cx - 2.0f * u, by - 21.0f * u)
            lineTo(cx + 2.0f * u, by - 21.0f * u)
            lineTo(cx + 2.2f * u, by - 16.5f * u)
            quadraticTo(cx + 5.0f * u, by - 13.5f * u, cx + 4.8f * u, by - 6.0f * u)
            quadraticTo(cx + 4.8f * u, by - 4.0f * u, cx, by - 4.0f * u)
            quadraticTo(cx - 4.8f * u, by - 4.0f * u, cx - 4.8f * u, by - 6.0f * u)
            quadraticTo(cx - 5.0f * u, by - 13.5f * u, cx - 2.2f * u, by - 16.5f * u)
            close()
        }
        drawPath(bottlePath, bottleGlass)

        // Glowing Amber Fuel filling lower half of bottle
        val fuelPath = Path().apply {
            moveTo(cx - 4.5f * u, by - 12.2f * u)
            quadraticTo(cx, by - 11.4f * u, cx + 4.5f * u, by - 12.2f * u)
            lineTo(cx + 4.6f * u, by - 5.8f * u)
            quadraticTo(cx, by - 4.3f * u, cx - 4.6f * u, by - 5.8f * u)
            close()
        }
        drawPath(fuelPath, amberFuel)
        drawPath(bottlePath, ink, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Studded Punk Denim Vest wrapping lower bottle torso
        drawRoundRect(denimDark, Offset(cx - 5.0f * u, by - 11.8f * u), Size(10.0f * u, 6.4f * u), CornerRadius(1.1f * u))
        drawRect(amberFuel, Offset(cx - 1.2f * u, by - 11.8f * u), Size(2.4f * u, 6.4f * u))
        drawRoundRect(ink, Offset(cx - 5.0f * u, by - 11.8f * u), Size(10.0f * u, 6.4f * u), CornerRadius(1.1f * u), style = Stroke(strokeW * 0.85f))

        // Silver Shoulder Spikes!
        listOf(cx - 5.2f * u, cx + 5.2f * u).forEachIndexed { idx, sx ->
            val dir = if (idx == 0) -1f else 1f
            val spike = Path().apply {
                moveTo(sx, by - 11.5f * u)
                lineTo(sx + dir * 2.0f * u, by - 12.8f * u)
                lineTo(sx, by - 9.8f * u)
                close()
            }
            drawPath(spike, Color(0xFFBDC2CC).copy(alpha = alpha))
            drawPath(spike, ink, style = Stroke(strokeW * 0.65f))
        }
        // Arms + Pale punk hands
        drawCircle(skinPale, 1.3f * u, Offset(cx - 6.0f * u, by - 7.6f * u))
        drawCircle(ink, 1.3f * u, Offset(cx - 6.0f * u, by - 7.6f * u), style = Stroke(strokeW * 0.7f))
        drawCircle(skinPale, 1.3f * u, Offset(cx + 6.0f * u, by - 7.6f * u))
        drawCircle(ink, 1.3f * u, Offset(cx + 6.0f * u, by - 7.6f * u), style = Stroke(strokeW * 0.7f))

        // Flowing Crimson-Rust Paisley Bandana Fuse coming out of bottle top!
        val sway = sin((walkPhase * 1.5f)) * 1.2f * u
        val bandanaPath = Path().apply {
            moveTo(cx - 1.5f * u, by - 20.8f * u)
            quadraticTo(cx - 4.5f * u, by - 25.0f * u, cx - 7.5f * u, by - 23.0f * u + sway)
            quadraticTo(cx - 5.0f * u, by - 20.2f * u, cx - 1.0f * u, by - 19.6f * u)
            close()
        }
        drawPath(bandanaPath, bandanaRust)
        drawPath(bandanaPath, ink, style = Stroke(strokeW * 0.8f, join = StrokeJoin.Round))
        drawCircle(MokaySpark.copy(alpha = alpha), 0.5f * u, Offset(cx - 4.8f * u, by - 22.8f * u + sway * 0.5f))

        // Serious Punk Eyes & Angry Brow on Glass Face
        val faceY = by - 14.5f * u
        drawOval(ink, Offset(cx - 2.1f * u, faceY - 0.8f * u), Size(1.0f * u, 1.5f * u))
        drawOval(ink, Offset(cx + 1.1f * u, faceY - 0.8f * u), Size(1.0f * u, 1.5f * u))
        drawLine(ink, Offset(cx - 2.6f * u, faceY - 1.4f * u), Offset(cx - 0.6f * u, faceY - 0.9f * u), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(ink, Offset(cx + 2.6f * u, faceY - 1.4f * u), Offset(cx + 0.6f * u, faceY - 0.9f * u), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(ink, Offset(cx - 1.3f * u, faceY + 1.4f * u), Offset(cx + 1.3f * u, faceY + 1.4f * u), strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)

        if (isBoss) {
            // Golden Mokay Crown icon above bottle
            val crown = Path().apply {
                moveTo(cx - 3.2f * u, by - 22.5f * u)
                lineTo(cx - 3.8f * u, by - 25.5f * u)
                lineTo(cx - 1.4f * u, by - 23.8f * u)
                lineTo(cx, by - 26.5f * u)
                lineTo(cx + 1.4f * u, by - 23.8f * u)
                lineTo(cx + 3.8f * u, by - 25.5f * u)
                lineTo(cx + 3.2f * u, by - 22.5f * u)
                close()
            }
            drawPath(crown, MokaySpark)
            drawPath(crown, ink, style = Stroke(strokeW * 0.8f))
        }
    }

    /** 'MOKAY EL FÓSFORO': Wooden matchstick body, roaring orange/gold flame head, expressive cartoon eyes & lilac gloves/shoes. */
    private fun DrawScope.drawMokayMatchstick(
        cx: Float,
        by: Float,
        u: Float,
        strokeW: Float,
        nowMs: Long
    ) {
        val ink = MokayCharcoal
        val woodCream = MokayCream
        val lilac = MokayLavender
        val flameFlicker = sin((nowMs / 150.0).toDouble()).toFloat() * 0.8f * u

        // Chunky lilac shoes & stick legs
        drawRoundRect(lilac, Offset(cx - 3.6f * u, by - 2.3f * u), Size(3.0f * u, 2.0f * u), CornerRadius(0.9f * u))
        drawRoundRect(lilac, Offset(cx + 0.6f * u, by - 2.3f * u), Size(3.0f * u, 2.0f * u), CornerRadius(0.9f * u))
        drawRoundRect(ink, Offset(cx - 3.6f * u, by - 2.3f * u), Size(3.0f * u, 2.0f * u), CornerRadius(0.9f * u), style = Stroke(strokeW * 0.8f))
        drawRoundRect(ink, Offset(cx + 0.6f * u, by - 2.3f * u), Size(3.0f * u, 2.0f * u), CornerRadius(0.9f * u), style = Stroke(strokeW * 0.8f))
        drawLine(ink, Offset(cx - 1.5f * u, by - 4.5f * u), Offset(cx - 2.0f * u, by - 2.2f * u), strokeWidth = 1.2f * u, cap = StrokeCap.Round)
        drawLine(ink, Offset(cx + 1.5f * u, by - 4.5f * u), Offset(cx + 2.0f * u, by - 2.2f * u), strokeWidth = 1.2f * u, cap = StrokeCap.Round)

        // Cream wooden matchstick body + grain lines
        drawRoundRect(woodCream, Offset(cx - 2.0f * u, by - 11.2f * u), Size(4.0f * u, 7.2f * u), CornerRadius(0.7f * u))
        drawRoundRect(ink, Offset(cx - 2.0f * u, by - 11.2f * u), Size(4.0f * u, 7.2f * u), CornerRadius(0.7f * u), style = Stroke(strokeW))
        drawLine(Color(0xFFCFA572), Offset(cx - 0.7f * u, by - 10.6f * u), Offset(cx - 0.7f * u, by - 4.8f * u), strokeWidth = strokeW * 0.45f)
        drawLine(Color(0xFFCFA572), Offset(cx + 0.7f * u, by - 10.6f * u), Offset(cx + 0.7f * u, by - 4.8f * u), strokeWidth = strokeW * 0.45f)

        // Rubber-hose arms + Lilac Gloves
        drawLine(ink, Offset(cx - 2.0f * u, by - 9.4f * u), Offset(cx - 4.6f * u, by - 8.2f * u), strokeWidth = 1.15f * u, cap = StrokeCap.Round)
        drawLine(ink, Offset(cx + 2.0f * u, by - 9.4f * u), Offset(cx + 4.8f * u, by - 10.6f * u), strokeWidth = 1.15f * u, cap = StrokeCap.Round)
        drawCircle(lilac, 1.3f * u, Offset(cx - 4.8f * u, by - 8.0f * u))
        drawCircle(ink, 1.3f * u, Offset(cx - 4.8f * u, by - 8.0f * u), style = Stroke(strokeW * 0.75f))
        drawCircle(lilac, 1.3f * u, Offset(cx + 5.0f * u, by - 10.6f * u))
        drawCircle(ink, 1.3f * u, Offset(cx + 5.0f * u, by - 10.6f * u), style = Stroke(strokeW * 0.75f))

        // Blazing Flame Head (Orange outer flame + Yellow inner tongue + Round Head)
        val flameHead = Path().apply {
            moveTo(cx - 4.6f * u, by - 14.0f * u)
            quadraticTo(cx - 5.2f * u, by - 18.8f * u, cx - 2.4f * u, by - 19.8f * u)
            quadraticTo(cx - 1.2f * u, by - 22.8f * u + flameFlicker, cx + 0.5f * u, by - 23.8f * u - flameFlicker)
            quadraticTo(cx + 2.8f * u, by - 21.5f * u, cx + 4.6f * u, by - 17.8f * u)
            quadraticTo(cx + 5.4f * u, by - 13.8f * u, cx, by - 11.0f * u)
            quadraticTo(cx - 4.2f * u, by - 11.2f * u, cx - 4.6f * u, by - 14.0f * u)
            close()
        }
        drawPath(flameHead, MokayFlame)
        val innerGold = Path().apply {
            moveTo(cx - 2.2f * u, by - 16.5f * u)
            quadraticTo(cx, by - 21.5f * u - flameFlicker, cx + 2.2f * u, by - 16.5f * u)
            close()
        }
        drawPath(innerGold, MokaySpark)
        drawPath(flameHead, ink, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Cartoon Eyes + Grin inside Match Head
        val faceY = by - 14.8f * u
        drawOval(Color.White, Offset(cx - 2.3f * u, faceY - 1.2f * u), Size(1.9f * u, 2.3f * u))
        drawOval(Color.White, Offset(cx + 0.3f * u, faceY - 1.2f * u), Size(1.9f * u, 2.3f * u))
        drawOval(ink, Offset(cx - 1.6f * u, faceY - 0.6f * u), Size(0.95f * u, 1.3f * u))
        drawOval(ink, Offset(cx + 0.9f * u, faceY - 0.6f * u), Size(0.95f * u, 1.3f * u))
        drawArc(ink, 0f, 180f, true, Offset(cx - 2.0f * u, faceY + 0.9f * u), Size(4.0f * u, 2.2f * u))
    }

    private fun DrawScope.drawBloomBuddy(cx: Float, by: Float, u: Float, strokeW: Float, nowMs: Long) {
        val bob = sin((nowMs / 210.0).toDouble()).toFloat() * 0.6f * u
        val cy = by - 9.0f * u + bob
        drawCircle(BloomPink, 5.2f * u, Offset(cx, cy))
        drawCircle(MokayCharcoal, 5.2f * u, Offset(cx, cy), style = Stroke(strokeW))
        drawCircle(MokaySpark, 2.0f * u, Offset(cx, cy - 5.2f * u))
        drawRoundRect(MokayCharcoal, Offset(cx - 3.2f * u, cy - 1.4f * u), Size(6.4f * u, 2.2f * u), CornerRadius(1.0f * u))
        drawCircle(BirdySky, 0.7f * u, Offset(cx - 1.3f * u, cy - 0.3f * u))
        drawCircle(BirdySky, 0.7f * u, Offset(cx + 1.3f * u, cy - 0.3f * u))
    }

    private fun DrawScope.drawBirdyBuddy(cx: Float, by: Float, u: Float, strokeW: Float, nowMs: Long) {
        val flap = sin((nowMs / 130.0).toDouble()).toFloat() * 1.4f * u
        val cy = by - 9.2f * u
        val wing = Path().apply {
            moveTo(cx - 3.6f * u, cy)
            lineTo(cx - 8.0f * u, cy - 2.8f * u + flap)
            lineTo(cx - 4.4f * u, cy + 2.2f * u)
            close()
        }
        drawPath(wing, BirdySky)
        drawPath(wing, MokayCharcoal, style = Stroke(strokeW * 0.8f))
        drawCircle(BirdySky, 4.8f * u, Offset(cx, cy))
        drawCircle(MokayCharcoal, 4.8f * u, Offset(cx, cy), style = Stroke(strokeW))
        drawRoundRect(MokayFlame, Offset(cx - 2.5f * u, cy - 2.2f * u), Size(5.0f * u, 1.8f * u), CornerRadius(0.7f * u))
        drawCircle(MokayCream, 1.1f * u, Offset(cx + 1.1f * u, cy - 1.3f * u))
        drawCircle(MokayCharcoal, 0.55f * u, Offset(cx + 1.2f * u, cy - 1.3f * u))
        val beak = Path().apply {
            moveTo(cx + 4.4f * u, cy - 0.4f * u)
            lineTo(cx + 6.8f * u, cy + 0.4f * u)
            lineTo(cx + 4.4f * u, cy + 1.2f * u)
            close()
        }
        drawPath(beak, MokaySpark)
    }

    private fun DrawScope.drawFlokoBuddy(cx: Float, by: Float, u: Float, strokeW: Float) {
        val cy = by - 9.0f * u
        drawCircle(RetroMint, 4.8f * u, Offset(cx, cy))
        drawCircle(MokayCream, 3.0f * u, Offset(cx - 2.6f * u, cy - 2.4f * u))
        drawCircle(MokayCream, 3.0f * u, Offset(cx + 2.6f * u, cy - 2.4f * u))
        drawCircle(MokayCharcoal, 4.8f * u, Offset(cx, cy), style = Stroke(strokeW))
        drawCircle(MokayCharcoal, 0.7f * u, Offset(cx - 1.4f * u, cy))
        drawCircle(MokayCharcoal, 0.7f * u, Offset(cx + 1.4f * u, cy))
    }

    private fun DrawScope.drawZenkayBuddy(cx: Float, by: Float, u: Float, strokeW: Float) {
        val cy = by - 9.0f * u
        drawCircle(MokaySpark, 4.8f * u, Offset(cx, cy))
        drawCircle(MokayCharcoal, 4.8f * u, Offset(cx, cy), style = Stroke(strokeW))
        drawRoundRect(MokayLavender, Offset(cx - 4.6f * u, cy - 3.2f * u), Size(9.2f * u, 1.5f * u), CornerRadius(0.6f * u))
        drawLine(MokayCharcoal, Offset(cx - 2.2f * u, cy - 0.4f * u), Offset(cx - 0.6f * u, cy - 0.4f * u), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(MokayCharcoal, Offset(cx + 0.6f * u, cy - 0.4f * u), Offset(cx + 2.2f * u, cy - 0.4f * u), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
fun VectorCharacterBadge(
    characterId: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        when (characterId) {
            "cat" -> with(VectorCharacterRenderer) {
                drawVectorEnemy(EnemySpecies.LAUGHING_CAT, false, cx, size.height * 0.90f, size.height * 0.80f, false, 0f, false)
            }
            "dino" -> with(VectorCharacterRenderer) {
                drawVectorEnemy(EnemySpecies.STREET_DINO, false, cx, size.height * 0.90f, size.height * 0.80f, false, 0f, false)
            }
            "molotov" -> with(VectorCharacterRenderer) {
                drawVectorEnemy(EnemySpecies.MOLOTOV_MOKAY, false, cx, size.height * 0.90f, size.height * 0.80f, false, 0f, false)
            }
            else -> with(VectorCharacterRenderer) {
                drawVectorPet(characterId, cx, cy, size.minDimension * 0.86f, false, 0L)
            }
        }
    }
}
