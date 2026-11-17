package com.example.game

import com.example.data.TileKind
import com.example.data.WorldZone
import kotlin.math.roundToInt
import kotlin.random.Random

data class ResourceDensityProfile(
    val crateDensity: Float,
    val minChests: Int,
    val maxChests: Int,
    val torchDensity: Float,
    val platformCountRange: IntRange,
    val materialWeights: Map<ResourceMaterial, Int>,
    val yieldMultiplier: Float
)

data class EnemySpawnRule(
    val species: EnemySpecies,
    val variantLabel: String,
    val weight: Float,
    val levelBonus: Int = 0
)

data class BiomeDefinition(
    val zone: WorldZone,
    val biomeName: String,
    val floorTile: TileKind,
    val subfloorTile: TileKind,
    val resourceDensity: ResourceDensityProfile,
    val enemyRules: List<EnemySpawnRule>,
    val enemyCountRange: IntRange,
    val hasCeilingOverhangs: Boolean = false,
    val hasDesertPillars: Boolean = false
)

data class ProceduralEnemySpawn(
    val id: String,
    val name: String,
    val species: EnemySpecies,
    val col: Int,
    val floorRow: Int,
    val level: Int,
    val isBoss: Boolean = false,
    val patrolHalfSpanTiles: Int = 4
)

object BiomeRegistry {
    private val definitions: Map<WorldZone, BiomeDefinition> = mapOf(
        WorldZone.ALDEA_MOKAY to BiomeDefinition(
            zone = WorldZone.ALDEA_MOKAY,
            biomeName = "Aldea Campamento",
            floorTile = TileKind.GRASS_TOP,
            subfloorTile = TileKind.SOLID_BRICK,
            resourceDensity = ResourceDensityProfile(
                crateDensity = 0.15f,
                minChests = 1,
                maxChests = 1,
                torchDensity = 0.25f,
                platformCountRange = 3..3,
                materialWeights = mapOf(
                    ResourceMaterial.WOOD to 55,
                    ResourceMaterial.STONE to 30,
                    ResourceMaterial.COPPER to 15
                ),
                yieldMultiplier = 1.0f
            ),
            enemyRules = emptyList(),
            enemyCountRange = 0..0
        ),
        WorldZone.BOSQUE_ESMERALDA to BiomeDefinition(
            zone = WorldZone.BOSQUE_ESMERALDA,
            biomeName = "Bosque Esmeralda",
            floorTile = TileKind.GRASS_TOP,
            subfloorTile = TileKind.SOLID_BRICK,
            resourceDensity = ResourceDensityProfile(
                crateDensity = 0.48f,
                minChests = 1,
                maxChests = 2,
                torchDensity = 0.35f,
                platformCountRange = 7..9,
                materialWeights = mapOf(
                    ResourceMaterial.WOOD to 62,
                    ResourceMaterial.STONE to 23,
                    ResourceMaterial.COPPER to 15
                ),
                yieldMultiplier = 1.15f
            ),
            enemyRules = listOf(
                EnemySpawnRule(EnemySpecies.LAUGHING_CAT, "Laughing Cat del Bosque", weight = 0.68f, levelBonus = 0),
                EnemySpawnRule(EnemySpecies.STREET_DINO, "Street Dino Explorador", weight = 0.32f, levelBonus = 1)
            ),
            enemyCountRange = 4..5
        ),
        WorldZone.DESIERTO_DORADO to BiomeDefinition(
            zone = WorldZone.DESIERTO_DORADO,
            biomeName = "Desierto Dorado",
            floorTile = TileKind.SAND_STONE,
            subfloorTile = TileKind.SAND_STONE,
            resourceDensity = ResourceDensityProfile(
                crateDensity = 0.34f,
                minChests = 1,
                maxChests = 2,
                torchDensity = 0.30f,
                platformCountRange = 5..7,
                materialWeights = mapOf(
                    ResourceMaterial.STONE to 46,
                    ResourceMaterial.COPPER to 36,
                    ResourceMaterial.WOOD to 10,
                    ResourceMaterial.IRON to 8
                ),
                yieldMultiplier = 1.25f
            ),
            enemyRules = listOf(
                EnemySpawnRule(EnemySpecies.STREET_DINO, "Street Dino Dunas", weight = 0.52f, levelBonus = 1),
                EnemySpawnRule(EnemySpecies.MOLOTOV_MOKAY, "Molotov Punk Nómada", weight = 0.28f, levelBonus = 1),
                EnemySpawnRule(EnemySpecies.LAUGHING_CAT, "Laughing Cat Arena", weight = 0.20f, levelBonus = 0)
            ),
            enemyCountRange = 4..6,
            hasDesertPillars = true
        ),
        WorldZone.CAVERNAS_MINA to BiomeDefinition(
            zone = WorldZone.CAVERNAS_MINA,
            biomeName = "Cavernas Mina",
            floorTile = TileKind.SOLID_BRICK,
            subfloorTile = TileKind.SOLID_BRICK,
            resourceDensity = ResourceDensityProfile(
                crateDensity = 0.42f,
                minChests = 2,
                maxChests = 3,
                torchDensity = 0.65f,
                platformCountRange = 6..8,
                materialWeights = mapOf(
                    ResourceMaterial.IRON to 42,
                    ResourceMaterial.STONE to 28,
                    ResourceMaterial.CRYSTAL to 20,
                    ResourceMaterial.COPPER to 10
                ),
                yieldMultiplier = 1.40f
            ),
            enemyRules = listOf(
                EnemySpawnRule(EnemySpecies.STREET_DINO, "Street Dino Blindado", weight = 0.40f, levelBonus = 1),
                EnemySpawnRule(EnemySpecies.MOLOTOV_MOKAY, "Molotov Punk Minero", weight = 0.40f, levelBonus = 1),
                EnemySpawnRule(EnemySpecies.LAUGHING_CAT, "Laughing Cat Sombra", weight = 0.20f, levelBonus = 1)
            ),
            enemyCountRange = 4..6,
            hasCeilingOverhangs = true
        ),
        WorldZone.BARRIO_SOMBRAVIA to BiomeDefinition(
            zone = WorldZone.BARRIO_SOMBRAVIA,
            biomeName = "Ciudadela Sombravía",
            floorTile = TileKind.SHADOW_FLOOR,
            subfloorTile = TileKind.SOLID_BRICK,
            resourceDensity = ResourceDensityProfile(
                crateDensity = 0.40f,
                minChests = 2,
                maxChests = 3,
                torchDensity = 0.70f,
                platformCountRange = 6..8,
                materialWeights = mapOf(
                    ResourceMaterial.CRYSTAL to 45,
                    ResourceMaterial.IRON to 35,
                    ResourceMaterial.COPPER to 20
                ),
                yieldMultiplier = 1.55f
            ),
            enemyRules = listOf(
                EnemySpawnRule(EnemySpecies.MOLOTOV_MOKAY, "Molotov Punk Élite", weight = 0.45f, levelBonus = 2),
                EnemySpawnRule(EnemySpecies.STREET_DINO, "Street Dino Alpha", weight = 0.35f, levelBonus = 1),
                EnemySpawnRule(EnemySpecies.LAUGHING_CAT, "Laughing Cat Caos", weight = 0.20f, levelBonus = 1)
            ),
            enemyCountRange = 5..6
        )
    )

    fun forZone(zone: WorldZone): BiomeDefinition =
        definitions[zone] ?: definitions.getValue(WorldZone.BOSQUE_ESMERALDA)

    fun pickEnemyRule(zone: WorldZone, rng: Random): EnemySpawnRule? {
        val rules = forZone(zone).enemyRules
        if (rules.isEmpty()) return null
        val totalWeight = rules.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.01f)
        var roll = rng.nextFloat() * totalWeight
        for (rule in rules) {
            roll -= rule.weight
            if (roll <= 0f) return rule
        }
        return rules.last()
    }

    fun rollResourceLoot(
        zone: WorldZone,
        isChest: Boolean,
        axeLevel: Int,
        pickaxeLevel: Int,
        rng: Random = Random.Default
    ): Map<ResourceMaterial, Int> {
        val biome = forZone(zone)
        val mult = biome.resourceDensity.yieldMultiplier
        val weights = biome.resourceDensity.materialWeights
        val gains = mutableMapOf<ResourceMaterial, Int>()

        fun pickWeighted(): ResourceMaterial {
            val total = weights.values.sum().coerceAtLeast(1)
            var roll = rng.nextInt(total)
            for ((mat, w) in weights) {
                roll -= w
                if (roll < 0) return mat
            }
            return weights.keys.first()
        }

        val rolls = if (isChest) 3 else 2
        repeat(rolls) {
            val mat = pickWeighted()
            val toolBonus = if (mat == ResourceMaterial.WOOD) axeLevel else pickaxeLevel
            val baseQty = if (isChest) 2 else 1
            val qty = ((baseQty + toolBonus * 0.6f) * mult).roundToInt().coerceAtLeast(1)
            gains[mat] = (gains[mat] ?: 0) + qty
        }
        return gains
    }
}
