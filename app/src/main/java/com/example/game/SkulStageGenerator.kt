package com.example.game

import com.example.data.TileKind
import com.example.data.WorldZone
import kotlin.random.Random

enum class PortalRewardKind(val title: String, val subtitle: String) {
    COMBAT_GOLD("Portal Combate", "Oro y Batalla"),
    SKULL_RELIC("Portal Cabeza", "Fósforos y Reliquias"),
    NEXT_ACT("Siguiente Acto", "Avanzar Capítulo"),
    BOSS_GATE("Jefe Molotov", "Arena Final")
}

data class RoomPortal(
    val id: String,
    val x: Float,
    val y: Float,
    val nextZone: WorldZone,
    val nextRoomIndex: Int,
    val rewardKind: PortalRewardKind
)

data class GeneratedStageData(
    val tiles: IntArray, // STAGE_COLS * STAGE_ROWS
    val zone: WorldZone,
    val roomIndex: Int,
    val portals: List<RoomPortal>,
    val biome: BiomeDefinition = BiomeRegistry.forZone(zone),
    val enemySpawns: List<ProceduralEnemySpawn> = emptyList()
)

object SkulStageGenerator {
    const val STAGE_COLS = 54
    const val STAGE_ROWS = 16
    const val TILE_SIZE = 36f
    const val STAGE_WIDTH = STAGE_COLS * TILE_SIZE   // 1944f
    const val STAGE_HEIGHT = STAGE_ROWS * TILE_SIZE // 576f

    const val SPAWN_X = 160f
    const val SPAWN_Y = 12 * TILE_SIZE // Standing cleanly on row 13 ground

    fun idx(col: Int, row: Int): Int = row * STAGE_COLS + col

    fun generateRoom(zone: WorldZone, roomIndex: Int, seed: Long): GeneratedStageData {
        val biome = BiomeRegistry.forZone(zone)
        val tiles = IntArray(STAGE_COLS * STAGE_ROWS) { TileKind.AIR.id }

        // Solid outer side walls and biome-specific 3-row floor (rows 13..15)
        for (c in 0 until STAGE_COLS) {
            for (r in 13 until STAGE_ROWS) {
                tiles[idx(c, r)] = if (r == 13) biome.floorTile.id else biome.subfloorTile.id
            }
        }
        for (r in 0 until STAGE_ROWS) {
            tiles[idx(0, r)] = TileKind.SOLID_BRICK.id
            tiles[idx(STAGE_COLS - 1, r)] = TileKind.SOLID_BRICK.id
        }

        val portals = mutableListOf<RoomPortal>()

        if (zone == WorldZone.ALDEA_MOKAY) {
            tiles[idx(8, 12)] = TileKind.BED.id
            tiles[idx(11, 12)] = TileKind.TV.id
            tiles[idx(14, 12)] = TileKind.KITCHEN.id
            tiles[idx(19, 12)] = TileKind.TORCH.id
            tiles[idx(24, 12)] = TileKind.SKULL_ALTAR.id
            tiles[idx(30, 12)] = TileKind.CHEST.id
            tiles[idx(34, 12)] = TileKind.CRATE.id

            for (c in 17..23) tiles[idx(c, 10)] = TileKind.PLATFORM_WOOD.id
            for (c in 27..33) tiles[idx(c, 8)] = TileKind.PLATFORM_WOOD.id
            for (c in 34..40) tiles[idx(c, 10)] = TileKind.PLATFORM_WOOD.id
            tiles[idx(29, 7)] = TileKind.TORCH.id
            tiles[idx(36, 9)] = TileKind.TORCH.id
            tiles[idx(38, 9)] = TileKind.CRATE.id

            portals += RoomPortal(
                id = "hub_to_act1",
                x = 46 * TILE_SIZE,
                y = 13 * TILE_SIZE,
                nextZone = WorldZone.BOSQUE_ESMERALDA,
                nextRoomIndex = 1,
                rewardKind = PortalRewardKind.COMBAT_GOLD
            )
            portals += RoomPortal(
                id = "hub_to_relic",
                x = 50 * TILE_SIZE,
                y = 13 * TILE_SIZE,
                nextZone = WorldZone.BOSQUE_ESMERALDA,
                nextRoomIndex = 1,
                rewardKind = PortalRewardKind.SKULL_RELIC
            )
            return GeneratedStageData(tiles, zone, 0, portals, biome, emptyList())
        }

        // BOSS ROOM in Act 4 Room 2+ (Teatro de las Máscaras & Sombravía Arena)
        if (zone == WorldZone.BARRIO_SOMBRAVIA && roomIndex >= 2) {
            for (c in 8..16) tiles[idx(c, 10)] = TileKind.PLATFORM_WOOD.id
            for (c in 21..33) tiles[idx(c, 8)] = TileKind.PLATFORM_WOOD.id
            for (c in 38..46) tiles[idx(c, 10)] = TileKind.PLATFORM_WOOD.id
            tiles[idx(6, 12)] = TileKind.TORCH.id
            tiles[idx(19, 12)] = TileKind.TORCH.id
            tiles[idx(35, 12)] = TileKind.TORCH.id
            tiles[idx(47, 12)] = TileKind.TORCH.id
            tiles[idx(12, 9)] = TileKind.TORCH.id
            tiles[idx(27, 7)] = TileKind.TORCH.id
            tiles[idx(42, 9)] = TileKind.TORCH.id
            tiles[idx(10, 9)] = TileKind.CRATE.id
            tiles[idx(44, 9)] = TileKind.CRATE.id

            portals += RoomPortal(
                id = "boss_victory_portal",
                x = 48 * TILE_SIZE,
                y = 13 * TILE_SIZE,
                nextZone = WorldZone.ALDEA_MOKAY,
                nextRoomIndex = 0,
                rewardKind = PortalRewardKind.NEXT_ACT
            )
            val bossSpawns = listOf(
                ProceduralEnemySpawn("boss_molotov", "Molotov Mokay Rey Punk", EnemySpecies.MOLOTOV_MOKAY, 41, 13, 7, isBoss = true, patrolHalfSpanTiles = 6),
                ProceduralEnemySpawn("en_guard_cat", "Laughing Cat Escolta", EnemySpecies.LAUGHING_CAT, 28, 13, 5, isBoss = false, patrolHalfSpanTiles = 5)
            )
            return GeneratedStageData(tiles, zone, roomIndex, portals, biome, bossSpawns)
        }

        // PROCEDURAL BIOME ROOM GENERATION
        val roomSeed = seed * 1009L + roomIndex * 7919L + zone.ordinal * 3571L
        val rng = Random(roomSeed)
        val density = biome.resourceDensity

        // 1. Cave ceiling stone overhangs (Cavernas Mina)
        if (biome.hasCeilingOverhangs) {
            for (c in 1 until STAGE_COLS - 1) {
                val hang = when {
                    c % 9 in 1..3 -> 2
                    c % 5 == 0 -> 1
                    else -> 0
                }
                for (r in 0..hang) {
                    tiles[idx(c, r)] = TileKind.SOLID_BRICK.id
                }
            }
        }

        // 2. Desert sandstone step formations (Desierto Dorado)
        if (biome.hasDesertPillars) {
            val pillarCols = listOf(14, 29, 39)
            for (pc in pillarCols) {
                if (rng.nextFloat() < 0.75f) {
                    tiles[idx(pc, 12)] = TileKind.SAND_STONE.id
                    tiles[idx(pc + 1, 12)] = TileKind.SAND_STONE.id
                }
            }
        }

        // 3. Procedurally placed multi-tiered wooden platforms based on biome platformCountRange
        val targetPlatforms = rng.nextInt(
            density.platformCountRange.first,
            density.platformCountRange.last + 1
        )
        val anchorSlots = listOf(
            7..13 to 10,
            15..22 to 8,
            16..22 to 11,
            24..31 to 9,
            26..33 to 6,
            33..40 to 10,
            36..43 to 7,
            38..44 to 11,
            9..15 to 7
        )
        val chosenSlots = anchorSlots.shuffled(rng).take(targetPlatforms)
        val platformWalkAnchors = mutableListOf<Pair<Int, Int>>() // (midCol, row)

        chosenSlots.forEach { (baseRange, baseRow) ->
            val rowOffset = rng.nextInt(-1, 2)
            val row = (baseRow + rowOffset).coerceIn(5, 11)
            for (c in baseRange) {
                tiles[idx(c, row)] = TileKind.PLATFORM_WOOD.id
            }
            val midCol = (baseRange.first + baseRange.last) / 2
            platformWalkAnchors += midCol to row

            // Torch placement driven by biome torchDensity
            if (rng.nextFloat() < density.torchDensity && tiles[idx(baseRange.first + 1, row - 1)] == TileKind.AIR.id) {
                tiles[idx(baseRange.first + 1, row - 1)] = TileKind.TORCH.id
            }
            // Crate placement driven by biome crateDensity
            if (rng.nextFloat() < density.crateDensity && tiles[idx(midCol, row - 1)] == TileKind.AIR.id) {
                tiles[idx(midCol, row - 1)] = TileKind.CRATE.id
            }
        }

        // 4. Ground resources (crates, chests, torches) strictly respecting biome densities
        val groundResourceCols = (10..43 step 4).toList()
        for (gc in groundResourceCols) {
            if (tiles[idx(gc, 12)] != TileKind.AIR.id) continue
            val roll = rng.nextFloat()
            if (roll < density.crateDensity * 0.55f) {
                tiles[idx(gc, 12)] = TileKind.CRATE.id
            } else if (roll > 1f - density.torchDensity * 0.38f) {
                tiles[idx(gc, 12)] = TileKind.TORCH.id
            }
        }

        val targetChests = rng.nextInt(density.minChests, density.maxChests + 1)
        val chestCandidates = (platformWalkAnchors.map { it.first to (it.second - 1) } +
            listOf(21 to 12, 32 to 12, 42 to 12)).shuffled(rng)
        var chestsPlaced = 0
        for ((cc, cr) in chestCandidates) {
            if (chestsPlaced >= targetChests) break
            if (cr in 2..12 && tiles[idx(cc, cr)] == TileKind.AIR.id &&
                (tiles[idx(cc, cr + 1)] != TileKind.AIR.id)
            ) {
                tiles[idx(cc, cr)] = TileKind.CHEST.id
                chestsPlaced++
            }
        }

        // 5. Procedurally generate biome-weighted enemies on valid surfaces
        val enemyCount = rng.nextInt(biome.enemyCountRange.first, biome.enemyCountRange.last + 1)
        val baseLevel = zone.ordinal * 2 + (roomIndex - 1)
        val enemySpawns = mutableListOf<ProceduralEnemySpawn>()
        val spawnSurfaceCandidates = mutableListOf(
            14 to 13,
            21 to 13,
            28 to 13,
            35 to 13,
            40 to 13
        )
        platformWalkAnchors.take(3).forEach { (mc, mr) ->
            spawnSurfaceCandidates += mc to mr
        }
        val chosenEnemySpots = spawnSurfaceCandidates.shuffled(rng).take(enemyCount)
        chosenEnemySpots.forEachIndexed { idxNum, (ec, floorRow) ->
            val rule = BiomeRegistry.pickEnemyRule(zone, rng) ?: return@forEachIndexed
            enemySpawns += ProceduralEnemySpawn(
                id = "${zone.id}_${roomIndex}_e$idxNum",
                name = rule.variantLabel,
                species = rule.species,
                col = ec,
                floorRow = floorRow,
                level = (baseLevel + rule.levelBonus).coerceAtLeast(1),
                isBoss = false,
                patrolHalfSpanTiles = if (floorRow < 13) 3 else 5
            )
        }

        // 6. Dual Exit Portals at right edge of room
        val chapters = WorldZone.chapterOrder()
        val curIdx = chapters.indexOf(zone).coerceAtLeast(1)
        val advanceAct = roomIndex >= 2
        val targetZone = if (advanceAct && curIdx < chapters.lastIndex) chapters[curIdx + 1] else zone
        val targetRoom = if (advanceAct) 1 else roomIndex + 1

        portals += RoomPortal(
            id = "portal_a_${zone.id}_$roomIndex",
            x = 46 * TILE_SIZE,
            y = 13 * TILE_SIZE,
            nextZone = targetZone,
            nextRoomIndex = targetRoom,
            rewardKind = if (targetZone == WorldZone.BARRIO_SOMBRAVIA && targetRoom >= 2) PortalRewardKind.BOSS_GATE
            else if (advanceAct) PortalRewardKind.NEXT_ACT else PortalRewardKind.COMBAT_GOLD
        )
        portals += RoomPortal(
            id = "portal_b_${zone.id}_$roomIndex",
            x = 50 * TILE_SIZE,
            y = 13 * TILE_SIZE,
            nextZone = targetZone,
            nextRoomIndex = targetRoom,
            rewardKind = if (targetZone == WorldZone.BARRIO_SOMBRAVIA && targetRoom >= 2) PortalRewardKind.BOSS_GATE
            else PortalRewardKind.SKULL_RELIC
        )

        return GeneratedStageData(tiles, zone, roomIndex, portals, biome, enemySpawns)
    }

    fun tileAt(data: GeneratedStageData, col: Int, row: Int): TileKind {
        if (col !in 0 until STAGE_COLS || row !in 0 until STAGE_ROWS) return TileKind.SOLID_BRICK
        return TileKind.byId(data.tiles[idx(col, row)])
    }

    fun isSolid(data: GeneratedStageData, col: Int, row: Int): Boolean {
        return tileAt(data, col, row).solid
    }
}
