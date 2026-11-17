package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.TileKind
import com.example.data.WorldZone
import com.example.ui.theme.BirdySky
import com.example.ui.theme.BloomPink
import com.example.ui.theme.DinoEmerald
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.MolotovCrimson
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroMint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders rich Mokayverse pixel-art backdrops, biome landmarks (Marea Viva, Sombravia,
 * Aldea Mokay, Bosque, Desierto, Minas), detailed stage tiles, and Skul portal archways.
 */
object WorldSceneryRenderer {
    private val signPaint = Paint().apply {
        isAntiAlias = true
        textSize = 9f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    private val signBgPaint = Paint().apply { isAntiAlias = true }

    private val SombraviaViolet = Color(0xFF9E5CFF)
    private val SombraviaDeep = Color(0xFF1F1033)
    private val MareaTeal = Color(0xFF28B5B5)
    private val MareaDeep = Color(0xFF134E6F)
    private val CoralPink = Color(0xFFFF6E9C)

    fun DrawScope.drawDetailedParallaxBackdrop(
        zone: WorldZone,
        camX: Float,
        viewW: Float,
        viewH: Float,
        nowMs: Long,
        highParticles: Boolean
    ) {
        val pulse = sin((nowMs / 420.0).toDouble()).toFloat()
        val sway = sin((nowMs / 900.0).toDouble()).toFloat()

        // 1. Sky Horizon Glow
        val horizonCol = when (zone) {
            WorldZone.ALDEA_MOKAY -> MareaTeal.copy(alpha = 0.22f)
            WorldZone.BOSQUE_ESMERALDA -> DinoEmerald.copy(alpha = 0.20f)
            WorldZone.DESIERTO_DORADO -> MokayFlame.copy(alpha = 0.28f)
            WorldZone.CAVERNAS_MINA -> MareaTeal.copy(alpha = 0.24f)
            WorldZone.BARRIO_SOMBRAVIA -> SombraviaViolet.copy(alpha = 0.30f)
        }
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, horizonCol)),
            topLeft = Offset(0f, viewH * 0.42f),
            size = Size(viewW, viewH * 0.58f)
        )

        // 2. Celestial Body (Sun / Crescent / Eclipsed Shadow Sun)
        val cx = (viewW * 0.78f - camX * 0.04f).coerceIn(80f, viewW - 60f)
        val cy = viewH * 0.20f
        when (zone) {
            WorldZone.BARRIO_SOMBRAVIA -> {
                // Eclipsed crimson-violet moon + swirling runes
                drawCircle(SombraviaViolet.copy(alpha = 0.16f), 68f + pulse * 4f, Offset(cx, cy))
                drawCircle(MolotovCrimson.copy(alpha = 0.42f), 42f, Offset(cx, cy))
                drawCircle(SombraviaDeep, 30f, Offset(cx, cy))
                drawCircle(SombraviaViolet.copy(alpha = 0.7f), 32f, Offset(cx, cy), style = Stroke(2.5f))
            }
            WorldZone.DESIERTO_DORADO -> {
                drawCircle(MokayFlame.copy(alpha = 0.20f), 74f, Offset(cx, cy))
                drawCircle(MokaySpark.copy(alpha = 0.36f), 50f, Offset(cx, cy))
                drawCircle(MokaySpark, 31f, Offset(cx, cy))
                drawCircle(MokayCream, 18f, Offset(cx, cy))
            }
            WorldZone.CAVERNAS_MINA -> {
                // Giant cavern crystal cluster hanging at top sky instead of sun
                for (i in 0..8) {
                    val sx = (i * 190f - (camX * 0.08f) % 190f)
                    val len = 46f + (i % 3) * 24f
                    drawTriangle(Offset(sx, 0f), Offset(sx + 34f, 0f), Offset(sx + 17f, len), Color(0xFF1E2240))
                    drawTriangle(Offset(sx + 10f, 0f), Offset(sx + 24f, 0f), Offset(sx + 17f, len * 0.7f), MareaTeal.copy(alpha = 0.45f))
                }
            }
            else -> {
                drawCircle(MokaySpark.copy(alpha = 0.16f), 62f + pulse * 3f, Offset(cx, cy))
                drawCircle(MokayCream.copy(alpha = 0.90f), 26f, Offset(cx, cy))
            }
        }

        // 3. Far Parallax Mountains / Skyline / Reef Spires (scroll 0.12x)
        val farShift = (camX * 0.12f) % 240f
        for (i in -1..((viewW / 240f).toInt() + 2)) {
            val bx = i * 240f - farShift
            when (zone) {
                WorldZone.ALDEA_MOKAY -> {
                    // Distant Mokay mountains + Torre de las Corrientes lighthouse silhouette
                    drawTriangle(Offset(bx - 30f, viewH), Offset(bx + 150f, viewH), Offset(bx + 60f, viewH - 170f), Color(0xFF212C50))
                    drawTriangle(Offset(bx + 90f, viewH), Offset(bx + 270f, viewH), Offset(bx + 185f, viewH - 140f), Color(0xFF26345E))
                    if ((i + 10) % 3 == 0) {
                        // Lighthouse tower with sweeping cyan beam
                        val tx = bx + 120f
                        drawRect(Color(0xFF2B3B6B), Offset(tx, viewH - 210f), Size(28f, 170f))
                        drawRect(MareaTeal.copy(alpha = 0.75f), Offset(tx + 4f, viewH - 222f), Size(20f, 14f))
                        drawTriangle(
                            Offset(tx + 24f, viewH - 216f),
                            Offset(tx + 185f, viewH - 245f + sway * 18f),
                            Offset(tx + 185f, viewH - 175f + sway * 18f),
                            BirdySky.copy(alpha = 0.14f)
                        )
                    }
                }
                WorldZone.BOSQUE_ESMERALDA -> {
                    // Massive ancient forest tree trunks + volumetric sunbeams
                    drawRect(Color(0xFF15322C), Offset(bx + 20f, 0f), Size(38f, viewH))
                    drawRect(Color(0xFF1A3D34), Offset(bx + 135f, viewH * 0.15f), Size(28f, viewH))
                    drawCircle(Color(0xFF1E4C3C), 72f, Offset(bx + 38f, viewH * 0.25f))
                    drawCircle(Color(0xFF245945), 58f, Offset(bx + 148f, viewH * 0.30f))
                    if ((i + 10) % 2 == 0) {
                        // Volumetric diagonal light shaft
                        drawQuad(
                            Offset(bx + 70f, 0f),
                            Offset(bx + 112f, 0f),
                            Offset(bx + 210f, viewH),
                            Offset(bx + 145f, viewH),
                            MokaySpark.copy(alpha = 0.08f)
                        )
                    }
                }
                WorldZone.DESIERTO_DORADO -> {
                    // Dunes + Stepped Mokay Pyramid
                    drawTriangle(Offset(bx - 40f, viewH), Offset(bx + 200f, viewH), Offset(bx + 75f, viewH - 135f), Color(0xFF6B3D2C))
                    if ((i + 10) % 2 == 1) {
                        val px = bx + 110f
                        drawRect(Color(0xFF8C5336), Offset(px, viewH - 145f), Size(110f, 145f))
                        drawRect(Color(0xFFA8663E), Offset(px + 18f, viewH - 175f), Size(74f, 30f))
                        drawRect(Color(0xFFC77D48), Offset(px + 36f, viewH - 198f), Size(38f, 23f))
                        drawCircle(MokaySpark.copy(alpha = 0.7f), 8f, Offset(px + 55f, viewH - 210f))
                    }
                }
                WorldZone.CAVERNAS_MINA -> {
                    // Deep cavern arches & glowing Gruta de las Perlas stalagmites
                    drawRect(Color(0xFF1A1F38), Offset(bx, viewH - 190f), Size(46f, 190f))
                    drawTriangle(Offset(bx + 80f, viewH), Offset(bx + 145f, viewH), Offset(bx + 112f, viewH - 155f), Color(0xFF202747))
                    drawTriangle(Offset(bx + 94f, viewH), Offset(bx + 130f, viewH), Offset(bx + 112f, viewH - 110f), MareaTeal.copy(alpha = 0.35f))
                }
                WorldZone.BARRIO_SOMBRAVIA -> {
                    // Sombravia Skyline: Torre de los Ecos + gothic spires + neon windows
                    val h = 165f + ((abs(i) * 43) % 70)
                    drawRect(Color(0xFF220E36), Offset(bx + 10f, viewH - h), Size(86f, h))
                    drawTriangle(Offset(bx + 6f, viewH - h), Offset(bx + 100f, viewH - h), Offset(bx + 53f, viewH - h - 46f), Color(0xFF33154F))
                    // Glowing gothic windows
                    drawRect(SombraviaViolet.copy(alpha = 0.45f), Offset(bx + 26f, viewH - h + 24f), Size(14f, 24f))
                    drawRect(MolotovCrimson.copy(alpha = 0.45f), Offset(bx + 60f, viewH - h + 24f), Size(14f, 24f))
                    // Torre de los Ecos Violet Flame Beacon on every 2nd spire
                    if ((i + 10) % 2 == 0) {
                        val fy = viewH - h - 54f
                        drawCircle(SombraviaViolet.copy(alpha = 0.25f), 28f + pulse * 4f, Offset(bx + 53f, fy))
                        drawCircle(SombraviaViolet, 10f, Offset(bx + 53f, fy))
                        drawCircle(MokayCream, 4f, Offset(bx + 53f, fy))
                    }
                }
            }
        }

        // 4. Midground Parallax Buildings / Coral Reef / Oasis / Sombravia Neon & Forest (scroll 0.28x)
        val midShift = (camX * 0.28f) % 260f
        for (i in -1..((viewW / 260f).toInt() + 2)) {
            val mx = i * 260f - midShift
            when (zone) {
                WorldZone.ALDEA_MOKAY -> {
                    // Marea Viva Canal Water Strip & Floating Bioluminescent Corals
                    drawRect(MareaDeep.copy(alpha = 0.72f), Offset(mx, viewH - 76f), Size(262f, 76f))
                    drawRect(MareaTeal.copy(alpha = 0.35f), Offset(mx, viewH - 76f), Size(262f, 8f))
                    // Shimmering water highlights
                    drawRect(BirdySky.copy(alpha = 0.4f), Offset(mx + 24f + sway * 8f, viewH - 58f), Size(44f, 4f))
                    drawRect(MokayCream.copy(alpha = 0.3f), Offset(mx + 140f - sway * 6f, viewH - 42f), Size(56f, 4f))
                    // Coral / Village shop silhouette
                    drawRect(Color(0xFF313B66), Offset(mx + 50f, viewH - 135f), Size(92f, 62f))
                    drawTriangle(Offset(mx + 42f, viewH - 135f), Offset(mx + 150f, viewH - 135f), Offset(mx + 96f, viewH - 165f), MokayFlame.copy(alpha = 0.75f))
                    drawRect(MokaySpark.copy(alpha = 0.7f), Offset(mx + 68f, viewH - 115f), Size(18f, 18f))
                    drawRect(BirdySky.copy(alpha = 0.7f), Offset(mx + 106f, viewH - 115f), Size(18f, 18f))
                }
                WorldZone.BOSQUE_ESMERALDA -> {
                    // Layered mossy hills, waterfalls & glowing mushrooms
                    drawCircle(Color(0xFF1D4D3A), 95f, Offset(mx + 90f, viewH + 10f))
                    if ((i + 10) % 2 == 0) {
                        // Cascading Pixel Waterfall
                        drawRect(MareaTeal.copy(alpha = 0.55f), Offset(mx + 185f, viewH - 155f), Size(28f, 155f))
                        val foamY = (nowMs / 16L) % 30L
                        drawRect(MokayCream.copy(alpha = 0.55f), Offset(mx + 190f, viewH - 140f + foamY), Size(6f, 14f))
                        drawRect(BirdySky.copy(alpha = 0.55f), Offset(mx + 202f, viewH - 110f + foamY), Size(6f, 14f))
                    }
                    // Glowing Coral/Forest Mushroom silhouette
                    drawRect(MokayCream.copy(alpha = 0.45f), Offset(mx + 44f, viewH - 56f), Size(10f, 24f))
                    drawCircle(CoralPink.copy(alpha = 0.65f), 16f, Offset(mx + 49f, viewH - 58f))
                }
                WorldZone.DESIERTO_DORADO -> {
                    // Oasis Plaza Coral pool + Palm trees + Market Banners
                    drawRect(MareaTeal.copy(alpha = 0.55f), Offset(mx + 20f, viewH - 54f), Size(140f, 24f))
                    // Swaying Palm Tree
                    drawRect(Color(0xFF6B4226), Offset(mx + 190f, viewH - 145f), Size(12f, 115f))
                    drawCircle(DinoEmerald.copy(alpha = 0.85f), 26f, Offset(mx + 196f + sway * 3f, viewH - 148f))
                    drawCircle(RetroMint.copy(alpha = 0.65f), 18f, Offset(mx + 178f + sway * 3f, viewH - 138f))
                }
                WorldZone.CAVERNAS_MINA -> {
                    // Glowing Perlas & Crystal Groves
                    drawCircle(MareaTeal.copy(alpha = 0.18f), 42f, Offset(mx + 110f, viewH - 92f))
                    drawTriangle(Offset(mx + 88f, viewH - 40f), Offset(mx + 112f, viewH - 40f), Offset(mx + 96f, viewH - 122f), BirdySky.copy(alpha = 0.65f))
                    drawTriangle(Offset(mx + 108f, viewH - 40f), Offset(mx + 136f, viewH - 40f), Offset(mx + 124f, viewH - 108f), BloomPink.copy(alpha = 0.65f))
                    // Giant glowing pearl orb
                    drawCircle(MokayCream.copy(alpha = 0.85f), 11f + pulse * 1.2f, Offset(mx + 200f, viewH - 72f))
                }
                WorldZone.BARRIO_SOMBRAVIA -> {
                    // Bosque Susurrante gnarled dark trees & Mercado de las Tentaciones neon signs
                    drawRect(Color(0xFF2A1442), Offset(mx + 35f, viewH - 125f), Size(96f, 125f))
                    // Glowing shop awning & neon border
                    drawRect(MolotovCrimson.copy(alpha = 0.8f), Offset(mx + 28f, viewH - 132f), Size(110f, 14f))
                    drawRect(SombraviaViolet.copy(alpha = 0.65f), Offset(mx + 48f, viewH - 104f), Size(70f, 26f), style = Stroke(2f))
                    drawRect(MokaySpark.copy(alpha = 0.7f), Offset(mx + 56f, viewH - 96f), Size(54f, 10f))
                    // Gnarled Shadow Tree with glowing eyes
                    drawRect(Color(0xFF190B2B), Offset(mx + 195f, viewH - 120f), Size(18f, 120f))
                    drawCircle(Color(0xFF281144), 38f, Offset(mx + 204f, viewH - 130f))
                    drawCircle(MokaySpark, 3f, Offset(mx + 196f, viewH - 128f))
                    drawCircle(MokaySpark, 3f, Offset(mx + 210f, viewH - 128f))
                }
            }
        }

        // 5. Atmospheric Floating Particles (Fireflies / Bubbles / Shadow Wisps)
        if (highParticles) {
            val count = 14
            for (p in 0 until count) {
                val baseX = ((p * 173 + nowMs * 0.025f * (1 + p % 2)) % viewW)
                val baseY = ((p * 89 + sin((nowMs / 700.0 + p).toDouble()).toFloat() * 18f) % (viewH * 0.82f)).coerceAtLeast(14f)
                val pCol = when (zone) {
                    WorldZone.ALDEA_MOKAY -> MokaySpark
                    WorldZone.BOSQUE_ESMERALDA -> RetroMint
                    WorldZone.DESIERTO_DORADO -> MokayFlame
                    WorldZone.CAVERNAS_MINA -> BirdySky
                    WorldZone.BARRIO_SOMBRAVIA -> SombraviaViolet
                }
                drawCircle(pCol.copy(alpha = 0.55f), if (p % 3 == 0) 3.2f else 2f, Offset(baseX, baseY))
            }
        }
    }

    /**
     * World-space Mokayverse landmarks behind platforms & characters!
     */
    fun DrawScope.drawBiomeWorldLandmarks(
        zone: WorldZone,
        roomIndex: Int,
        groundY: Float,
        ts: Float,
        nowMs: Long
    ) {
        val pulse = sin((nowMs / 320.0).toDouble()).toFloat()
        when (zone) {
            WorldZone.ALDEA_MOKAY -> {
                // 1. Detailed Boxy's Mokay Villa House behind Bed / TV / Kitchen
                val hx = 220f
                val hw = 360f
                // Timber frame & warm plaster wall
                drawRect(Color(0xFF4F3424), Offset(hx, groundY - 156f), Size(hw, 156f))
                drawRect(Color(0xFFE3D0B1), Offset(hx + 12f, groundY - 134f), Size(hw - 24f, 134f))
                // Interior cozy wainscoting & wallpaper trim
                drawRect(Color(0xFFA67449), Offset(hx + 12f, groundY - 42f), Size(hw - 24f, 42f))
                drawRect(MokayLavender.copy(alpha = 0.35f), Offset(hx + 12f, groundY - 134f), Size(hw - 24f, 10f))
                // Chimney + animated smoke puffs
                drawRect(Color(0xFF6B4232), Offset(hx + 286f, groundY - 210f), Size(28f, 60f))
                val sm = ((nowMs / 40L) % 35L).toFloat()
                drawCircle(MokayCream.copy(alpha = 0.45f), 8f, Offset(hx + 300f + sm * 0.25f, groundY - 218f - sm))
                drawCircle(MokayCream.copy(alpha = 0.30f), 11f, Offset(hx + 306f + sm * 0.35f, groundY - 234f - sm))
                // Shingled Mokay Flame Roof + Crest
                drawRect(MokayFlame, Offset(hx - 18f, groundY - 178f), Size(hw + 36f, 38f))
                drawRect(MolotovCrimson, Offset(hx - 18f, groundY - 178f), Size(hw + 36f, 10f))
                drawRect(MokaySpark, Offset(hx - 18f, groundY - 144f), Size(hw + 36f, 6f))
                // Windows + MOKAY Banner Sign
                for (wxOff in listOf(42f, 266f)) {
                    drawRect(Color(0xFF2B314A), Offset(hx + wxOff, groundY - 108f), Size(48f, 42f))
                    drawRect(MokaySpark.copy(alpha = 0.88f), Offset(hx + wxOff + 5f, groundY - 103f), Size(38f, 32f))
                    drawLine(Color(0xFF4F3424), Offset(hx + wxOff + 24f, groundY - 108f), Offset(hx + wxOff + 24f, groundY - 66f), 3f)
                    drawLine(Color(0xFF4F3424), Offset(hx + wxOff, groundY - 87f), Offset(hx + wxOff + 48f, groundY - 87f), 3f)
                }
                signBgPaint.color = MokayCharcoal.toArgb()
                drawContext.canvas.nativeCanvas.drawRect(hx + 128f, groundY - 156f, hx + 232f, groundY - 134f, signBgPaint)
                signPaint.color = MokaySpark.toArgb()
                signPaint.textSize = 10f
                drawContext.canvas.nativeCanvas.drawText("CASA MOKAY", hx + 180f, groundY - 141f, signPaint)

                // 2. Marea Viva Aquatic Pier & Coral Fountain near right side of camp!
                val fx = 1280f
                drawRect(MareaDeep, Offset(fx - 70f, groundY - 18f), Size(140f, 18f))
                drawRect(MareaTeal, Offset(fx - 64f, groundY - 22f), Size(128f, 8f))
                // Glowing Coral Fountain Spire
                drawRect(MokayLavender, Offset(fx - 14f, groundY - 82f), Size(28f, 62f))
                drawCircle(CoralPink, 16f, Offset(fx, groundY - 86f))
                drawCircle(BirdySky.copy(alpha = 0.6f + pulse * 0.2f), 11f, Offset(fx, groundY - 86f))
                // Floating water droplets
                val drop = ((nowMs / 18L) % 24L).toFloat()
                drawCircle(BirdySky, 3.5f, Offset(fx - 22f, groundY - 78f + drop))
                drawCircle(BirdySky, 3.5f, Offset(fx + 22f, groundY - 78f + drop))
            }

            WorldZone.BOSQUE_ESMERALDA -> {
                // Giant Mokay Spirit Treehouse & Mossy Ruin Pillars
                for (px in listOf(480f, 1120f)) {
                    drawRect(Color(0xFF4F3727), Offset(px - 22f, groundY - 220f), Size(44f, 220f))
                    drawCircle(DinoEmerald.copy(alpha = 0.85f), 58f, Offset(px, groundY - 220f))
                    drawCircle(RetroMint.copy(alpha = 0.55f), 36f, Offset(px + 28f, groundY - 200f))
                    // Hanging lantern
                    drawLine(MokaySpark, Offset(px + 26f, groundY - 180f), Offset(px + 26f, groundY - 156f), 2f)
                    drawCircle(MokaySpark, 6f + pulse * 1.2f, Offset(px + 26f, groundY - 150f))
                }
            }

            WorldZone.DESIERTO_DORADO -> {
                // Desert Oasis Coral Shrine + Market Awnings
                for (ox in listOf(520f, 1220f)) {
                    drawRect(Color(0xFF9C6B3B), Offset(ox - 18f, groundY - 150f), Size(36f, 150f))
                    drawTriangle(Offset(ox - 24f, groundY - 150f), Offset(ox + 24f, groundY - 150f), Offset(ox, groundY - 184f), MokaySpark)
                    // Colorful Mokay Street Market Tent
                    drawRect(MokayFlame, Offset(ox + 60f, groundY - 78f), Size(92f, 18f))
                    drawRect(MokayCream, Offset(ox + 76f, groundY - 78f), Size(18f, 18f))
                    drawRect(MokayCream, Offset(ox + 112f, groundY - 78f), Size(18f, 18f))
                    drawRect(Color(0xFF5B3B22), Offset(ox + 64f, groundY - 60f), Size(6f, 60f))
                    drawRect(Color(0xFF5B3B22), Offset(ox + 142f, groundY - 60f), Size(6f, 60f))
                }
            }

            WorldZone.CAVERNAS_MINA -> {
                // Gruta de las Perlas Giant Oyster & Glowing Crystal Mine Beams
                val gx = 760f
                drawCircle(MareaTeal.copy(alpha = 0.22f), 54f + pulse * 4f, Offset(gx, groundY - 52f))
                drawOval(MokayLavender, Offset(gx - 38f, groundY - 46f), Size(76f, 46f))
                drawCircle(MokayCream, 16f, Offset(gx, groundY - 34f))
                drawCircle(BirdySky.copy(alpha = 0.6f), 8f, Offset(gx - 4f, groundY - 38f))

                // Mine timber frame supports
                for (mx in listOf(380f, 1180f)) {
                    drawRect(Color(0xFF5C3E24), Offset(mx, groundY - 210f), Size(14f, 210f))
                    drawRect(Color(0xFF5C3E24), Offset(mx + 120f, groundY - 210f), Size(14f, 210f))
                    drawRect(Color(0xFF734F30), Offset(mx - 10f, groundY - 216f), Size(154f, 16f))
                    drawCircle(MokaySpark, 6f, Offset(mx + 67f, groundY - 192f))
                }
            }

            WorldZone.BARRIO_SOMBRAVIA -> {
                // SOMBRAVIA LANDMARKS: Teatro de las Máscaras + Neon Tentaciones + Estación Niebla!
                // 1. Plaza del Reflejo Violet Brazier Fountain
                val px = 440f
                drawRect(Color(0xFF2D1847), Offset(px - 42f, groundY - 28f), Size(84f, 28f))
                drawRect(SombraviaViolet, Offset(px - 14f, groundY - 76f), Size(28f, 48f))
                drawCircle(SombraviaViolet.copy(alpha = 0.28f), 34f + pulse * 5f, Offset(px, groundY - 88f))
                drawCircle(MolotovCrimson, 14f, Offset(px, groundY - 86f))
                drawCircle(MokaySpark, 7f, Offset(px, groundY - 88f))

                // 2. Teatro de las Máscaras Stage Facade (highlighted in Boss Arena too!)
                val tx = 920f
                drawRect(Color(0xFF24113B), Offset(tx - 110f, groundY - 165f), Size(220f, 165f))
                drawRect(MolotovCrimson, Offset(tx - 118f, groundY - 178f), Size(236f, 24f))
                drawRect(RetroGold, Offset(tx - 118f, groundY - 156f), Size(236f, 5f))
                // Draped curtains
                drawRect(Color(0xFF8C1D2E), Offset(tx - 96f, groundY - 151f), Size(32f, 151f))
                drawRect(Color(0xFF8C1D2E), Offset(tx + 64f, groundY - 151f), Size(32f, 151f))
                signBgPaint.color = MokayCharcoal.toArgb()
                drawContext.canvas.nativeCanvas.drawRect(tx - 84f, groundY - 176f, tx + 84f, groundY - 158f, signBgPaint)
                signPaint.color = MokaySpark.toArgb()
                signPaint.textSize = 9.5f
                drawContext.canvas.nativeCanvas.drawText(
                    if (roomIndex >= 2) "ARENA MOLOTOV PUNK" else "TEATRO MÁSCARAS",
                    tx, groundY - 164f, signPaint
                )

                // 3. Mercado de las Tentaciones Neon Signs ("ÉXITO", "FAMA")
                val sx = 1390f
                drawRect(Color(0xFF1F1033), Offset(sx - 56f, groundY - 138f), Size(112f, 138f))
                drawRect(BloomPink, Offset(sx - 48f, groundY - 128f), Size(96f, 24f), style = Stroke(2.5f))
                signPaint.color = BloomPink.toArgb()
                signPaint.textSize = 9f
                drawContext.canvas.nativeCanvas.drawText("FAMA RÁPIDA", sx, groundY - 112f, signPaint)
                drawRect(BirdySky, Offset(sx - 44f, groundY - 94f), Size(88f, 20f), style = Stroke(2f))
                signPaint.color = BirdySky.toArgb()
                drawContext.canvas.nativeCanvas.drawText("¿SEGURO?", sx, groundY - 81f, signPaint)

                // 4. Estación Niebla Retro Steam Train silhouette on far left
                val lx = 72f
                drawRect(Color(0xFF1E1A2E), Offset(lx, groundY - 56f), Size(98f, 44f))
                drawRect(Color(0xFF332B4D), Offset(lx + 62f, groundY - 86f), Size(36f, 34f))
                drawRect(Color(0xFF1A1626), Offset(lx + 12f, groundY - 82f), Size(14f, 26f))
                drawCircle(MolotovCrimson, 6f, Offset(lx + 4f, groundY - 38f))
                drawCircle(MokayCharcoal, 11f, Offset(lx + 22f, groundY - 11f))
                drawCircle(MokayCharcoal, 11f, Offset(lx + 52f, groundY - 11f))
                drawCircle(MokayCharcoal, 13f, Offset(lx + 82f, groundY - 13f))
            }
        }
    }

    /**
     * Detailed pixel-art Skul Exit Portals with carved stone archway, swirling vortex & reward crest.
     */
    fun DrawScope.drawDetailedPortal(
        portal: RoomPortal,
        active: Boolean,
        nowMs: Long
    ) {
        val col = when (portal.rewardKind) {
            PortalRewardKind.COMBAT_GOLD -> MokaySpark
            PortalRewardKind.SKULL_RELIC -> MokayLavender
            PortalRewardKind.NEXT_ACT -> RetroMint
            PortalRewardKind.BOSS_GATE -> MolotovCrimson
        }
        val pulse = sin((nowMs / 150.0).toDouble()).toFloat() * 4f
        val cx = portal.x
        val cy = portal.y - 38f

        // Carved stone archway pillars + crown keystone
        drawRect(Color(0xFF2B2E45), Offset(cx - 29f, portal.y - 76f), Size(58f, 76f))
        drawRect(Color(0xFF424769), Offset(cx - 29f, portal.y - 76f), Size(58f, 76f), style = Stroke(2f))
        drawRect(if (active) col else Color(0xFF555973), Offset(cx - 10f, portal.y - 83f), Size(20f, 10f))

        // Portal swirling energy layers
        drawOval(
            color = if (active) col.copy(alpha = 0.24f) else Color(0x44181924),
            topLeft = Offset(cx - 22f, cy - 34f),
            size = Size(44f, 70f)
        )
        drawOval(
            color = if (active) col else Color(0xFF50546E),
            topLeft = Offset(cx - 19f, cy - 31f),
            size = Size(38f, 64f),
            style = Stroke(width = if (active) 3.2f else 2f)
        )
        if (active) {
            drawOval(
                color = MokayCream.copy(alpha = 0.55f),
                topLeft = Offset(cx - 11f - pulse * 0.3f, cy - 20f - pulse * 0.4f),
                size = Size(22f + pulse * 0.6f, 40f + pulse * 0.8f)
            )
            // Floating orbiting rune spark
            val ang = (nowMs / 240.0)
            drawCircle(
                col,
                3.5f,
                Offset(cx + cos(ang).toFloat() * 17f, cy + sin(ang).toFloat() * 24f)
            )
        }

        // Styled Portal Nameplate
        signBgPaint.color = MokayCharcoal.copy(alpha = 0.90f).toArgb()
        drawContext.canvas.nativeCanvas.drawRect(cx - 48f, cy - 60f, cx + 48f, cy - 43f, signBgPaint)
        signPaint.color = (if (active) col else Color(0xFF9EA4B8)).toArgb()
        signPaint.textSize = 8.5f
        drawContext.canvas.nativeCanvas.drawText(
            if (active) portal.rewardKind.title else "Bloqueado",
            cx, cy - 48f, signPaint
        )
    }

    /**
     * Detailed pixel-art stage tiles & Hub furniture.
     */
    fun DrawScope.drawDetailedStageTile(
        tile: TileKind,
        zone: WorldZone,
        col: Int,
        row: Int,
        wx: Float,
        wy: Float,
        ts: Float,
        nowMs: Long
    ) {
        when (tile) {
            TileKind.PLATFORM_WOOD -> {
                // Beveled timber planks + iron brackets + biome trim (vines/coral/neon)
                drawRect(Color(0xFF6B4226), Offset(wx, wy), Size(ts + 0.5f, 12f))
                drawRect(Color(0xFFB57B4A), Offset(wx, wy + 1f), Size(ts + 0.5f, 6f))
                drawRect(Color(0xFF3E2718), Offset(wx + 2f, wy + 2f), Size(4f, 4f))
                drawRect(Color(0xFF3E2718), Offset(wx + ts - 6f, wy + 2f), Size(4f, 4f))
                if (zone == WorldZone.BOSQUE_ESMERALDA && col % 2 == 0) {
                    drawRect(DinoEmerald, Offset(wx + 8f, wy + 11f), Size(5f, 7f))
                } else if (zone == WorldZone.BARRIO_SOMBRAVIA && col % 2 == 0) {
                    drawRect(SombraviaViolet, Offset(wx + 10f, wy + 10f), Size(14f, 3f))
                }
            }
            TileKind.GRASS_TOP -> {
                drawRect(Color(0xFF543822), Offset(wx, wy), Size(ts + 0.5f, ts + 0.5f))
                drawRect(Color(0xFF6E4B2E), Offset(wx + 4f, wy + 14f), Size(10f, 6f))
                drawRect(Color(0xFF3E7E36), Offset(wx, wy), Size(ts + 0.5f, 10f))
                drawRect(Color(0xFF60B54F), Offset(wx, wy), Size(ts + 0.5f, 5f))
                // Pixel grass tufts & tiny flowers
                if (col % 3 == 0) {
                    drawRect(Color(0xFF7AD665), Offset(wx + 6f, wy - 3f), Size(4f, 4f))
                    drawRect(MokaySpark, Offset(wx + 22f, wy - 4f), Size(4f, 4f))
                }
            }
            TileKind.SAND_STONE -> {
                drawRect(Color(0xFF9E6E36), Offset(wx, wy), Size(ts + 0.5f, ts + 0.5f))
                drawRect(Color(0xFFD9AE62), Offset(wx, wy), Size(ts + 0.5f, 10f))
                drawRect(Color(0xFFF0CB7D), Offset(wx + 4f, wy + 2f), Size(ts - 8f, 3f))
                drawRect(Color(0xFF7D5427), Offset(wx, wy + 18f), Size(ts + 0.5f, 2f))
            }
            TileKind.SHADOW_FLOOR -> {
                drawRect(Color(0xFF241336), Offset(wx, wy), Size(ts + 0.5f, ts + 0.5f))
                drawRect(Color(0xFF422163), Offset(wx, wy), Size(ts + 0.5f, 9f))
                drawRect(SombraviaViolet.copy(alpha = 0.7f), Offset(wx, wy), Size(ts + 0.5f, 3f))
                drawRect(Color(0xFF170B24), Offset(wx + ts / 2f, wy + 10f), Size(2f, ts - 10f))
            }
            TileKind.SOLID_BRICK -> {
                drawRect(Color(0xFF353952), Offset(wx, wy), Size(ts + 0.5f, ts + 0.5f))
                drawRect(Color(0xFF4C5273), Offset(wx + 2f, wy + 2f), Size(ts - 4f, 13f))
                drawRect(Color(0xFF272A3D), Offset(wx, wy + 17f), Size(ts + 0.5f, 2f))
                if (zone == WorldZone.CAVERNAS_MINA && (col + row) % 5 == 0) {
                    drawRect(MareaTeal, Offset(wx + 10f, wy + 8f), Size(7f, 6f))
                }
            }
            TileKind.CHEST -> {
                val bmp = PixelSpriteEngine.getChestSprite(false)
                drawImage(
                    bmp, IntOffset.Zero, IntSize(bmp.width, bmp.height),
                    IntOffset(wx.roundToInt() + 1, wy.roundToInt() + 2),
                    IntSize(ts.roundToInt() - 2, ts.roundToInt() - 2),
                    filterQuality = FilterQuality.None
                )
            }
            TileKind.CRATE -> {
                drawRect(Color(0xFF9C6938), Offset(wx + 3f, wy + 4f), Size(ts - 6f, ts - 4f))
                drawRect(Color(0xFF66401E), Offset(wx + 3f, wy + 4f), Size(ts - 6f, ts - 4f), style = Stroke(2.5f))
                drawLine(Color(0xFF66401E), Offset(wx + 4f, wy + 5f), Offset(wx + ts - 4f, wy + ts - 1f), 2.5f)
                drawLine(Color(0xFF66401E), Offset(wx + ts - 4f, wy + 5f), Offset(wx + 4f, wy + ts - 1f), 2.5f)
                drawRect(MokaySpark, Offset(wx + ts / 2f - 3f, wy + ts / 2f - 2f), Size(6f, 6f))
            }
            TileKind.BED -> {
                // Detailed Bed: Wooden headboard, pillow, Mokay-flame quilt
                drawRect(Color(0xFF5E3A20), Offset(wx - 2f, wy + 4f), Size(8f, ts - 4f))
                drawRect(Color(0xFF7D4E2D), Offset(wx + 2f, wy + 18f), Size(ts + 12f, ts - 18f))
                drawRect(MokayCream, Offset(wx + 6f, wy + 14f), Size(13f, 9f))
                drawRect(MokayFlame, Offset(wx + 19f, wy + 14f), Size(ts - 6f, 14f))
                drawRect(MokaySpark, Offset(wx + 25f, wy + 18f), Size(10f, 5f))
            }
            TileKind.KITCHEN -> {
                // Pixel Kitchen Counter & Stove with bubbling pot
                drawRect(Color(0xFFCED8E3), Offset(wx + 2f, wy + 6f), Size(ts - 4f, ts - 6f))
                drawRect(MokayCharcoal, Offset(wx + 6f, wy + 14f), Size(ts - 12f, 14f))
                drawRect(MokayFlame, Offset(wx + 9f, wy + 17f), Size(ts - 18f, 6f))
                drawRect(Color(0xFF6F7B91), Offset(wx + 9f, wy - 2f), Size(18f, 9f))
                val steam = ((nowMs / 50L + col) % 10L).toFloat()
                drawCircle(MokayCream.copy(alpha = 0.6f), 3f, Offset(wx + 18f, wy - 6f - steam))
            }
            TileKind.TV -> {
                // Retro CRT TV with rabbit-ear antennas & animated Mokay spark screen
                drawLine(MokayCream, Offset(wx + 16f, wy + 6f), Offset(wx + 9f, wy - 4f), 2f)
                drawLine(MokayCream, Offset(wx + 20f, wy + 6f), Offset(wx + 27f, wy - 4f), 2f)
                drawRect(Color(0xFF2B3047), Offset(wx + 3f, wy + 5f), Size(ts - 6f, ts - 8f))
                val screen = if ((nowMs / 360L) % 2L == 0L) BirdySky else MokayLavender
                drawRect(screen, Offset(wx + 6f, wy + 8f), Size(ts - 12f, ts - 16f))
                drawCircle(MokayFlame, 4f, Offset(wx + ts / 2f, wy + ts / 2f - 1f))
            }
            TileKind.SKULL_ALTAR -> {
                // Ornate Skull/Match Altar pedestal with levitating glowing match flame
                val bob = sin((nowMs / 210.0).toDouble()).toFloat() * 3.5f
                drawRect(Color(0xFF3E4166), Offset(wx + 2f, wy + 14f), Size(ts - 4f, ts - 14f))
                drawRect(MokayLavender, Offset(wx + 6f, wy + 12f), Size(ts - 12f, 5f))
                drawCircle(MokaySpark.copy(alpha = 0.25f), 16f, Offset(wx + ts / 2f, wy + 2f + bob))
                drawRect(MokayFlame, Offset(wx + ts / 2f - 6f, wy - 6f + bob), Size(12f, 14f))
                drawCircle(MokaySpark, 5f, Offset(wx + ts / 2f, wy - 6f + bob))
            }
            TileKind.TORCH -> {
                val flick = sin((nowMs / 95.0 + col).toDouble()).toFloat() * 2.2f
                val flameCol = if (zone == WorldZone.BARRIO_SOMBRAVIA) SombraviaViolet else MokayFlame
                val glowCol = if (zone == WorldZone.BARRIO_SOMBRAVIA) MokayLavender else MokaySpark
                drawCircle(flameCol.copy(alpha = 0.26f), 24f, Offset(wx + ts / 2f, wy + 12f))
                drawRect(Color(0xFF423127), Offset(wx + 15f, wy + 13f), Size(6f, ts - 13f))
                drawCircle(flameCol, 7.5f, Offset(wx + ts / 2f, wy + 10f + flick))
                drawCircle(glowCol, 4f, Offset(wx + ts / 2f, wy + 10f + flick))
            }
            TileKind.AIR -> Unit
        }
    }

    private fun DrawScope.drawTriangle(p1: Offset, p2: Offset, p3: Offset, color: Color) {
        val path = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
        drawPath(path, color)
    }

    private fun DrawScope.drawQuad(p1: Offset, p2: Offset, p3: Offset, p4: Offset, color: Color) {
        val path = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            close()
        }
        drawPath(path, color)
    }
}
