package com.example.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R

object PixelSpriteEngine {
    private val cache = HashMap<String, ImageBitmap>()

    private val spriteDrawableMap = mapOf(
        "mokay" to R.drawable.sprite_mokay,
        "caja" to R.drawable.sprite_caja,
        "bloom" to R.drawable.sprite_bloom,
        "floko" to R.drawable.sprite_floko,
        "birdy" to R.drawable.sprite_birdy,
        "zenkay" to R.drawable.sprite_zenkay,
        "dino" to R.drawable.sprite_dino,
        "cat" to R.drawable.sprite_cat,
        "molotov" to R.drawable.sprite_molotov
    )

    private val sharedPalette = mapOf(
        '.' to 0x00000000, // Transparent
        'K' to 0xFF1A1A1A.toInt(), // Outline Black
        'W' to 0xFFFFFFFF.toInt(), // Pure White
        'C' to 0xFFF4E7D1.toInt(), // Mokay Cream / Wood
        'O' to 0xFFFF6A3D.toInt(), // Mokay Flame Orange
        'Y' to 0xFFFFC24D.toInt(), // Mokay Spark Yellow
        'R' to 0xFFD93829.toInt(), // Deep Red
        'L' to 0xFFB7B0E6.toInt(), // Mokay Lavender Purple
        'D' to 0xFF6A3AC8.toInt(), // Sombravia Purple
        'P' to 0xFFE97893.toInt(), // Bloom Pink
        'p' to 0xFFC74E6E.toInt(), // Bloom Dark Pink
        'G' to 0xFF4BBE4E.toInt(), // Green
        'g' to 0xFF2D7A36.toInt(), // Dark Green
        'S' to 0xFF7DD6F3.toInt(), // Birdy Sky / Glitch Cyan
        's' to 0xFF3E94B8.toInt(), // Deep Blue
        'B' to 0xFF785233.toInt(), // Brown
        'b' to 0xFF4A311D.toInt(), // Dark Brown
        'M' to 0xFF9EA4B8.toInt(), // Metal Grey
        'm' to 0xFF3E4254.toInt(), // Dark Metal
        'Q' to 0xFFF5B7C9.toInt()  // Rose Quartz
    )

    fun preloadCharacters(context: Context) {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        spriteDrawableMap.forEach { (id, resId) ->
            val rightKey = "hd_${id}_r"
            val leftKey = "hd_${id}_l"
            if (!cache.containsKey(rightKey)) {
                val bmp = BitmapFactory.decodeResource(context.resources, resId, opts)
                if (bmp != null) {
                    cache[rightKey] = bmp.asImageBitmap()
                    val flipMatrix = Matrix().apply { preScale(-1f, 1f) }
                    val flipped = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, flipMatrix, false)
                    cache[leftKey] = flipped.asImageBitmap()
                }
            }
        }
    }

    private fun bakeSprite(key: String, rows: List<String>, extra: Map<Char, Int> = emptyMap()): ImageBitmap {
        cache[key]?.let { return it }
        val h = rows.size
        val w = rows.firstOrNull()?.length ?: 16
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) {
            val row = rows[y]
            for (x in 0 until w) {
                val ch = if (x < row.length) row[x] else '.'
                val color = extra[ch] ?: sharedPalette[ch] ?: 0x00000000
                bmp.setPixel(x, y, color)
            }
        }
        val img = bmp.asImageBitmap()
        cache[key] = img
        return img
    }

    fun getCharacterSprite(characterId: String, frame: Int, facingLeft: Boolean = false): ImageBitmap {
        val dirKey = if (facingLeft) "l" else "r"
        cache["hd_${characterId}_$dirKey"]?.let { return it }
        return getFallbackPixelSprite(characterId, frame)
    }

    private fun getFallbackPixelSprite(characterId: String, frame: Int): ImageBitmap {
        val f = frame % 2
        val key = "char_${characterId}_$f"
        cache[key]?.let { return it }
        val rows = when (characterId) {
            "mokay" -> listOf(
                ".......O........",
                "......OYO.......",
                ".....OYYYO......",
                "....OOYYOOO.....",
                "....OKWKWKO.....",
                "....OKKKKKO.....",
                ".....OOOOO......",
                "...KK.CCC.KK....",
                "..LLK.CCC.KLL...",
                "......CCC.......",
                ".....K...K......",
                "....LL...LL....."
            )
            else -> listOf(
                ".....KKKKKK.....",
                "....KLLLLLLK....",
                "....KLWKKWLK....",
                "....KLLLLLLK....",
                "...KKOOOOOOKK...",
                "....KLLLLLLK....",
                ".....K....K.....",
                "....LL....LL...."
            )
        }
        return bakeSprite(key, rows)
    }

    fun getEnemySprite(type: String, frame: Int): ImageBitmap {
        val f = frame % 2
        val key = "enemy_${type}_$f"
        cache[key]?.let { return it }
        val rows = when (type) {
            "BOSS" -> if (f == 0) listOf(
                "....YYY..YYY....",
                "...KDDDDDDDDK...",
                "..KDDLLLLLLDDK..",
                "..KDLWWKKWWLDK..",
                "..KDLYYKKYYLDK..",
                ".KKDDDDDDDDDDKK.",
                "KKDRRRRRRRRRRDKK",
                "K.KDRRRRRRRRDK.K",
                "..KDmmDDDDmmDK..",
                "..KDDDDDDDDDDK..",
                "...KKK....KKK...",
                "..KDDK....KDDK..",
                ".KDDDK....KDDDK.",
                "................",
                "................",
                "................"
            ) else listOf(
                "...YYY....YYY...",
                "..KDDDDDDDDDDK..",
                "..KDDLLLLLLDDK..",
                "..KDLWWKKWWLDK..",
                "..KDLYYKKYYLDK..",
                "KKKDDDDDDDDDDKKK",
                "K.KDRRRRRRRRDK.K",
                "..KDmmDDDDmmDK..",
                "..KDDDDDDDDDDK..",
                "..KKK......KKK..",
                ".KDDK......KDDK.",
                ".KDDDK....KDDDK.",
                "................",
                "................",
                "................",
                "................"
            )

            "GLITCH" -> if (f == 0) listOf(
                "....K......K....",
                ".....K....K.....",
                "...KKKKKKKKKK...",
                "..KmmmmmmmmmmK..",
                "..KmSSSSSSSSmK..",
                "..KmSWKmmKWKmK..",
                "..KmSPPPPPPSmK..",
                "..KmmmmmmmmmmK..",
                "...KKDDDDDDKK...",
                "..SKKDDDDDDKKS..",
                "....KDDDDDDK....",
                "....K.K..K.K....",
                "...KK......KK...",
                "................",
                "................",
                "................"
            ) else listOf(
                ".....K....K.....",
                "......K..K......",
                "...KKKKKKKKKK...",
                "..KmmmmmmmmmmK..",
                "..KmPPPPPPPPmK..",
                "..KmSWKmmKWKmK..",
                "..KmSSSSSSSSmK..",
                "..KmmmmmmmmmmK..",
                "..SKKDDDDDDKKS..",
                "...KKDDDDDDKK...",
                "....KDDDDDDK....",
                "...K.K....K.K...",
                "..KK........KK..",
                "................",
                "................",
                "................"
            )

            "STATIC" -> if (f == 0) listOf(
                "....KKKKKKKK....",
                "...KMMMMMMMMK...",
                "..KMWKmKWKmWMK..",
                "..KMWmMmMmMmMK..",
                "..KMmMWmWmMMmK..",
                "..KMMMMMMMMMMK..",
                "...KKmmmmmmKK...",
                "..mKKmOOOOmmKKm.",
                "....KmmmmmmK....",
                "....K.K..K.K....",
                "...mm......mm...",
                "................",
                "................",
                "................",
                "................",
                "................"
            ) else listOf(
                "....KKKKKKKK....",
                "...KMMMMMMMMK...",
                "..KMmKWKmKWKmK..",
                "..KMmMWMmWMmMK..",
                "..KMWmMmMmMWmK..",
                "..KMMMMMMMMMMK..",
                "...KKmmmmmmKK...",
                ".mKKmmOOOOmmKKm.",
                "....KmmmmmmK....",
                "...K.K....K.K...",
                "..mm........mm..",
                "................",
                "................",
                "................",
                "................",
                "................"
            )

            "SPECTER", "VOID" -> if (f == 0) listOf(
                ".....KKKKKK.....",
                "...KKDDDDDDKK...",
                "..KDDLLLLLLDDK..",
                "..KDLLWKKWLLDK..",
                "..KDLLLLLLLLDK..",
                "..KDDLLLLLLDDK..",
                "..KDDDDDDDDDDK..",
                "...KDDDDDDDDK...",
                "...KD.KDDK.DK...",
                "....K..KK..K....",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"
            ) else listOf(
                ".....KKKKKK.....",
                "...KKDDDDDDKK...",
                "..KDDLLLLLLDDK..",
                "..KDLLWKKWLLDK..",
                "..KDLLLLLLLLDK..",
                "..KDDLLLLLLDDK..",
                "..KDDDDDDDDDDK..",
                "..K.KDDDDDDK.K..",
                ".....K.KK.K.....",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"
            )

            else -> if (f == 0) listOf( // SHADE / GRUNT
                ".....KKKKKK.....",
                "...KKDDDDDDKK...",
                "..KDDmmmmmmDDK..",
                "..KDmYYYYYYmDK..",
                "..KDmYKmmKYmDK..",
                "..KDDmmmmmmDDK..",
                "..KDDDDDDDDDDK..",
                "...KD..DD..DK...",
                "...KK......KK...",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"
            ) else listOf(
                ".....KKKKKK.....",
                "...KKDDDDDDKK...",
                "..KDDmmmmmmDDK..",
                "..KDmYYYYYYmDK..",
                "..KDmYKmmKYmDK..",
                "..KDDmmmmmmDDK..",
                "..KDDDDDDDDDDK..",
                "..KD........DK..",
                "..KK........KK..",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"
            )
        }
        return bakeSprite(key, rows)
    }

    fun getChestSprite(opened: Boolean): ImageBitmap {
        val key = "chest_$opened"
        cache[key]?.let { return it }
        val rows = if (!opened) listOf(
            "................",
            "................",
            "..KKKKKKKKKKKK..",
            ".KBBBBBBBBBBBBK.",
            ".KBYYYYYYYYYYBK.",
            ".KBBBBBBBBBBBBK.",
            ".KKKKKKYYKKKKKK.",
            ".KbbbbKYYKbbbbK.",
            ".KbbbbKKKKbbbbK.",
            ".KbbbbbbbbbbbbK.",
            ".KYYYYYYYYYYYYK.",
            "..KKKKKKKKKKKK..",
            "................",
            "................",
            "................",
            "................"
        ) else listOf(
            "..KKKKKKKKKKKK..",
            ".KbbbbbbbbbbbbK.",
            ".KbbYYYYYYYYbbK.",
            "..KKKKKKKKKKKK..",
            ".KBBBBBBBBBBBBK.",
            ".KbbbYYYYYYbbbK.",
            ".KbbbYWWWWYbbbK.",
            ".KbbbbYYYYbbbbK.",
            ".KbbbbbbbbbbbbK.",
            ".KYYYYYYYYYYYYK.",
            "..KKKKKKKKKKKK..",
            "................",
            "................",
            "................",
            "................",
            "................"
        )
        return bakeSprite(key, rows)
    }

    fun getAnvilSprite(frame: Int): ImageBitmap {
        val f = frame % 2
        val key = "anvil_$f"
        cache[key]?.let { return it }
        val rows = if (f == 0) listOf(
            ".......O........",
            "......OYO.......",
            ".....OYYYO......",
            "....KOOOOOK.....",
            "..KKKKKKKKKKKK..",
            ".KMMMMMMMMMMMMK.",
            "..KKmmmmmmmmKK..",
            "....KmmmmmmK....",
            "....KmmmmmmK....",
            "..KKmmmmmmmmKK..",
            ".KMMMMMMMMMMMMK.",
            ".KKKKKKKKKKKKKK.",
            "................",
            "................",
            "................",
            "................"
        ) else listOf(
            "......O.........",
            ".....OYO.Y......",
            "....OOYYYO......",
            "....KOOOOOK.....",
            "..KKKKKKKKKKKK..",
            ".KMMMMMMMMMMMMK.",
            "..KKmmmmmmmmKK..",
            "....KmmmmmmK....",
            "....KmmmmmmK....",
            "..KKmmmmmmmmKK..",
            ".KMMMMMMMMMMMMK.",
            ".KKKKKKKKKKKKKK.",
            "................",
            "................",
            "................",
            "................"
        )
        return bakeSprite(key, rows)
    }

    fun getSparkPickupSprite(): ImageBitmap {
        return bakeSprite(
            "pickup_spark",
            listOf(
                ".......Y........",
                "......YYY.......",
                ".....YYYYY......",
                "....YYOWOYY.....",
                "..YYYYWWWYYYY...",
                "....YYOWOYY.....",
                ".....YYYYY......",
                "......YYY.......",
                ".......Y........",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"
            )
        )
    }
}
