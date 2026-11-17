package com.example

import androidx.compose.ui.geometry.Offset
import com.example.data.GameCatalog
import com.example.data.GestureSkillId
import com.example.data.TileKind
import com.example.data.WorldZone
import com.example.game.ArrowProjectile
import com.example.game.BiomeAudioManager
import com.example.game.BiomeCueKind
import com.example.game.BiomeRegistry
import com.example.game.CraftResult
import com.example.game.CraftableTool
import com.example.game.EnemySpecies
import com.example.game.GameSessionState
import com.example.game.GestureRecognizer
import com.example.game.HeroAppearance
import com.example.game.InventoryManager
import com.example.game.PortalRewardKind
import com.example.game.RecognizedGesture
import com.example.game.ResourceMaterial
import com.example.game.SkulStageGenerator
import com.example.game.SlashVisualStyle
import com.example.game.WorldEnemy
import com.example.game.WorldSimulation
import com.example.ui.theme.MolotovCrimson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameProgressionUnitTest {

    @Test
    fun skulStageGenerator_createsHubHouseFurniturePlatformsAndDualPortals() {
        val hub = SkulStageGenerator.generateRoom(WorldZone.ALDEA_MOKAY, 0, 202606L)
        assertEquals(SkulStageGenerator.STAGE_COLS * SkulStageGenerator.STAGE_ROWS, hub.tiles.size)
        val tileSet = hub.tiles.toSet()
        assertTrue(TileKind.BED.id in tileSet)
        assertTrue(TileKind.KITCHEN.id in tileSet)
        assertTrue(TileKind.TV.id in tileSet)
        assertTrue(TileKind.SKULL_ALTAR.id in tileSet)
        assertTrue(TileKind.PLATFORM_WOOD.id in tileSet)
        assertEquals(2, hub.portals.size)

        val forestRoom = SkulStageGenerator.generateRoom(WorldZone.BOSQUE_ESMERALDA, 1, 202606L)
        assertTrue(forestRoom.portals.any { it.rewardKind == PortalRewardKind.COMBAT_GOLD })
        assertTrue(forestRoom.portals.any { it.rewardKind == PortalRewardKind.SKULL_RELIC })
        assertTrue(TileKind.CHEST.id in forestRoom.tiles.toSet() || TileKind.CRATE.id in forestRoom.tiles.toSet())
    }

    @Test
    fun boxyMovement_doubleJumpDashInvulnerabilityAndPlatformLanding() {
        val initial = GameSessionState(
            playerX = SkulStageGenerator.SPAWN_X,
            playerY = SkulStageGenerator.SPAWN_Y,
            activeSkullId = "boxy_birdy", // 3 extra jumps!
            unlockedSkulls = setOf("boxy_classic", "boxy_birdy")
        )
        val jump1 = WorldSimulation.performJump(initial)
        assertFalse(jump1.onGround)
        assertTrue(jump1.playerVy < 0f)

        val jump2 = WorldSimulation.performJump(jump1)
        assertTrue(jump2.playerVy < 0f)
        assertEquals(initial.effectiveExtraJumps() - 1, jump2.jumpsRemaining)

        val dashed = WorldSimulation.performDash(jump2, nowMs = 5000L)
        assertTrue(dashed.dashTimerSec > 0f)
        assertTrue(dashed.invulnerableUntilMs > 5000L)
        assertTrue(dashed.afterimages.isNotEmpty())
    }

    @Test
    fun skullSwapComboAndPortalTransition_workLikeSkul() {
        val nearCat = WorldEnemy(
            id = "cat_test",
            name = "Laughing Cat",
            species = EnemySpecies.LAUGHING_CAT,
            zone = WorldZone.BOSQUE_ESMERALDA,
            level = 2,
            isBoss = false,
            x = 300f,
            y = SkulStageGenerator.SPAWN_Y,
            patrolMinX = 200f,
            patrolMaxX = 500f,
            hp = 45,
            maxHp = 45,
            atk = 10,
            rewardSparks = 25,
            rewardXp = 30
        )
        val combatState = GameSessionState(
            currentZone = WorldZone.BOSQUE_ESMERALDA,
            currentRoomIndex = 1,
            playerX = 260f,
            playerY = SkulStageGenerator.SPAWN_Y,
            activeSkullId = "boxy_classic",
            secondarySkullId = "boxy_flame",
            enemies = listOf(nearCat),
            roomCleared = false
        )

        val swapped = WorldSimulation.performSwapSkull(combatState, nowMs = 3000L)
        assertEquals("boxy_flame", swapped.activeSkullId)
        assertEquals("boxy_classic", swapped.secondarySkullId)
        assertEquals(1L, swapped.skullSwapsCount)
        assertFalse(swapped.enemies.first().alive)

        // Step once to mark roomCleared and enter right-side Skull/Relic portal
        val stepped = WorldSimulation.step(swapped, 0f, 0f, false, 0.016f, 3050L)
        assertTrue(stepped.roomCleared)
        assertEquals(1L, stepped.roomsClearedCount)

        val skullPortal = stepped.stageData.portals.first { it.rewardKind == PortalRewardKind.SKULL_RELIC }
        val atPortal = stepped.copy(playerX = skullPortal.x, playerY = skullPortal.y)
        val nextRoom = WorldSimulation.performInteract(atPortal)
        assertEquals(skullPortal.nextZone, nextRoom.currentZone)
        assertEquals(skullPortal.nextRoomIndex, nextRoom.currentRoomIndex)
        assertTrue(nextRoom.unlockedSkulls.size > stepped.unlockedSkulls.size)
    }

    @Test
    fun missionsPetsRelicsAndConsumables_progressCorrectly() {
        val state = GameSessionState(
            hp = 50,
            stamina = 15f,
            sparksCurrency = 300L,
            roomsClearedCount = 1L,
            skullSwapsCount = 5L,
            inventoryCounts = mapOf("food_apple" to 2, "drink_oasis" to 2)
        )

        val (afterFood, _) = WorldSimulation.useConsumable(state, "food_apple")
        assertEquals(85, afterFood.hp)

        val (afterDrink, _) = WorldSimulation.useConsumable(afterFood, "drink_oasis")
        assertEquals(60f, afterDrink.stamina, 0.01f)

        val (afterRelic, _) = WorldSimulation.buyOrToggleRelic(afterDrink, "rel_dash_boots")
        assertTrue("rel_dash_boots" in afterRelic.equippedRelics)
        assertTrue(afterRelic.effectiveDashDamage() >= 28)

        val (afterM1, _) = WorldSimulation.claimMission(afterRelic, "m_first_clear")
        assertTrue("bloom" in afterM1.unlockedPets)
        assertTrue("bloom" in afterM1.equippedPets)

        val (afterM2, _) = WorldSimulation.claimMission(afterM1, "m_swap_skulls")
        assertTrue("birdy" in afterM2.unlockedPets)
        assertEquals(3, afterM2.equippedPets.size)
    }

    @Test
    fun gestureAttacksSkillsAndSaveSlotRoundTrip_preserveBoxyState() {
        val unlocked = setOf("TAP_STRIKE", "SWIPE_DASH", "CIRCLE_NOVA", "Z_SLASH")
        val circlePts = (0..18).map { i ->
            val a = (2 * Math.PI * i) / 18.0
            Offset(120f + 50f * cos(a).toFloat(), 120f + 50f * sin(a).toFloat())
        }
        assertEquals(GestureSkillId.CIRCLE_NOVA, GestureRecognizer.classify(circlePts, 220L, unlocked).skill)

        val dino = WorldEnemy(
            id = "dino_1",
            name = "Street Dino",
            species = EnemySpecies.STREET_DINO,
            zone = WorldZone.DESIERTO_DORADO,
            level = 4,
            isBoss = false,
            x = 290f,
            y = SkulStageGenerator.SPAWN_Y,
            patrolMinX = 200f,
            patrolMaxX = 500f,
            hp = 200,
            maxHp = 200,
            atk = 18,
            rewardSparks = 35,
            rewardXp = 45
        )
        val initial = GameSessionState(
            currentZone = WorldZone.DESIERTO_DORADO,
            currentRoomIndex = 1,
            playerX = 240f,
            playerY = SkulStageGenerator.SPAWN_Y,
            weaponLevel = 3,
            activeSkullId = "boxy_flame",
            secondarySkullId = "boxy_tank",
            unlockedSkulls = setOf("boxy_classic", "boxy_flame", "boxy_tank"),
            enemies = listOf(dino),
            appearance = HeroAppearance(
                heroName = "Boxy",
                headgearId = "mokay_bandana_paisley",
                jacketId = "mokay_vest_spiked"
            )
        )

        val afterNova = WorldSimulation.performAttack(
            initial,
            RecognizedGesture(GestureSkillId.CIRCLE_NOVA, 1f, 0f),
            nowMs = 5000L
        )
        assertTrue(afterNova.enemies.first().hp < dino.hp)

        val afterSkill1 = WorldSimulation.performSkill(afterNova, 1, nowMs = 5500L)
        assertTrue(afterSkill1.projectiles.any { it.fromPlayer })

        val slot = afterSkill1.toSaveSlot("quicksave", "Guardado Rápido")
        val restored = GameSessionState.fromSaveSlot(slot)
        assertEquals("boxy_flame", restored.activeSkullId)
        assertEquals("boxy_tank", restored.secondarySkullId)
        assertEquals(3, restored.weaponLevel)
        assertEquals("mokay_bandana_paisley", restored.appearance.headgearId)
        assertTrue(GameCatalog.cosmeticById(restored.appearance.jacketId)?.brandLine?.contains("MOKAY") == true)
    }

    @Test
    fun inventoryManager_collectsAndCombinesMaterialsIntoUpgradedTools() {
        val inv = InventoryManager(
            initialMaterials = mapOf(
                ResourceMaterial.WOOD to 10,
                ResourceMaterial.STONE to 8,
                ResourceMaterial.COPPER to 5
            ),
            initialToolLevels = mapOf(
                CraftableTool.AXE to 1,
                CraftableTool.PICKAXE to 1,
                CraftableTool.BOW to 0
            ),
            initialArrows = 5
        )

        // Upgrade Axe to Nv.2 (cost: 6 Wood + 4 Stone)
        val axeRes = inv.craftNextToolLevel(CraftableTool.AXE, availableSparks = 200L)
        assertTrue(axeRes is CraftResult.Success)
        assertEquals(2, inv.getToolLevel(CraftableTool.AXE))
        assertEquals(4, inv.getMaterialCount(ResourceMaterial.WOOD))
        assertEquals(4, inv.getMaterialCount(ResourceMaterial.STONE))

        // Insufficient wood for Bow Nv.1 (needs 6 Wood, 3 Stone) -> fails gracefully
        val bowFail = inv.craftNextToolLevel(CraftableTool.BOW, availableSparks = 200L)
        assertTrue(bowFail is CraftResult.Failure)

        // Collect extra wood and craft Bow Nv.1 + upgrade Pickaxe Nv.2 -> Nv.3
        inv.addMaterial(ResourceMaterial.WOOD, 12)
        inv.addMaterial(ResourceMaterial.STONE, 8)
        val bowRes = inv.craftNextToolLevel(CraftableTool.BOW, availableSparks = 200L)
        assertTrue(bowRes is CraftResult.Success)
        assertEquals(1, inv.getToolLevel(CraftableTool.BOW))
        assertEquals(20, inv.arrowsCount) // +15 starter arrows on Bow craft

        assertTrue(inv.craftArrowsBundle(10))
        assertEquals(30, inv.arrowsCount)

        // Verify GameSessionState + SaveSlot persistence round-trip
        val state = GameSessionState(
            sparksCurrency = 300L,
            inventoryCounts = inv.writeToInventoryMap(emptyMap())
        )
        val (upgradedState, _) = WorldSimulation.craftToolUpgrade(state, CraftableTool.PICKAXE)
        assertEquals(2, upgradedState.inventoryManager().getToolLevel(CraftableTool.PICKAXE))

        val restored = GameSessionState.fromSaveSlot(upgradedState.toSaveSlot())
        assertEquals(2, restored.inventoryManager().getToolLevel(CraftableTool.AXE))
        assertEquals(2, restored.inventoryManager().getToolLevel(CraftableTool.PICKAXE))
        assertEquals(1, restored.inventoryManager().getToolLevel(CraftableTool.BOW))
        assertEquals(30, restored.inventoryManager().arrowsCount)
    }

    @Test
    fun proceduralBiomeSystem_assignsUniqueTerrainResourceDensitiesAndEnemyRosters() {
        val forest = SkulStageGenerator.generateRoom(WorldZone.BOSQUE_ESMERALDA, 1, 1001L)
        val desert = SkulStageGenerator.generateRoom(WorldZone.DESIERTO_DORADO, 1, 1001L)
        val caves = SkulStageGenerator.generateRoom(WorldZone.CAVERNAS_MINA, 1, 1001L)

        // 1. Unique floor tiles and biome-specific terrain formations
        assertEquals(TileKind.GRASS_TOP, SkulStageGenerator.tileAt(forest, 10, 13))
        assertEquals(TileKind.SAND_STONE, SkulStageGenerator.tileAt(desert, 10, 13))
        assertEquals(TileKind.SOLID_BRICK, SkulStageGenerator.tileAt(caves, 10, 13))
        assertTrue(SkulStageGenerator.tileAt(caves, 10, 1) == TileKind.SOLID_BRICK)
        assertTrue(SkulStageGenerator.tileAt(forest, 10, 1) == TileKind.AIR)

        // 2. Resource density & biome material distributions
        val forestBiome = BiomeRegistry.forZone(WorldZone.BOSQUE_ESMERALDA)
        val caveBiome = BiomeRegistry.forZone(WorldZone.CAVERNAS_MINA)
        assertTrue(forestBiome.resourceDensity.crateDensity > BiomeRegistry.forZone(WorldZone.DESIERTO_DORADO).resourceDensity.crateDensity)
        assertTrue(caveBiome.resourceDensity.minChests >= 2)
        assertTrue(caveBiome.resourceDensity.torchDensity > forestBiome.resourceDensity.torchDensity)

        val lootRng = Random(42L)
        val forestLootTotals = mutableMapOf<ResourceMaterial, Int>()
        val caveLootTotals = mutableMapOf<ResourceMaterial, Int>()
        repeat(40) {
            BiomeRegistry.rollResourceLoot(WorldZone.BOSQUE_ESMERALDA, isChest = false, axeLevel = 1, pickaxeLevel = 1, rng = lootRng)
                .forEach { (m, q) -> forestLootTotals[m] = (forestLootTotals[m] ?: 0) + q }
            BiomeRegistry.rollResourceLoot(WorldZone.CAVERNAS_MINA, isChest = false, axeLevel = 1, pickaxeLevel = 1, rng = lootRng)
                .forEach { (m, q) -> caveLootTotals[m] = (caveLootTotals[m] ?: 0) + q }
        }
        assertTrue((forestLootTotals[ResourceMaterial.WOOD] ?: 0) > (forestLootTotals[ResourceMaterial.CRYSTAL] ?: 0))
        assertTrue((caveLootTotals[ResourceMaterial.IRON] ?: 0) > (caveLootTotals[ResourceMaterial.WOOD] ?: 0))

        // 3. Biome-specific enemy rosters
        val forestEnemies = WorldSimulation.spawnEnemiesForStage(WorldZone.BOSQUE_ESMERALDA, 1, forest)
        assertTrue(forestEnemies.size in forestBiome.enemyCountRange)
        assertTrue(forestEnemies.all { it.species == EnemySpecies.LAUGHING_CAT || it.species == EnemySpecies.STREET_DINO })

        val desertEnemies = WorldSimulation.spawnEnemiesForStage(WorldZone.DESIERTO_DORADO, 1, desert)
        val caveEnemies = WorldSimulation.spawnEnemiesForStage(WorldZone.CAVERNAS_MINA, 1, caves)
        assertTrue(desertEnemies.size in desert.biome.enemyCountRange)
        assertTrue(caveEnemies.size in caves.biome.enemyCountRange)
        assertTrue(desertEnemies.all { it.zone == WorldZone.DESIERTO_DORADO })
        assertTrue(caveEnemies.all { it.zone == WorldZone.CAVERNAS_MINA })

        // 4. Deterministic seed generation
        val forestSameSeed = SkulStageGenerator.generateRoom(WorldZone.BOSQUE_ESMERALDA, 1, 1001L)
        val forestDiffSeed = SkulStageGenerator.generateRoom(WorldZone.BOSQUE_ESMERALDA, 1, 9999L)
        assertTrue(forest.tiles.contentEquals(forestSameSeed.tiles))
        assertEquals(forest.enemySpawns, forestSameSeed.enemySpawns)
        assertFalse(forest.tiles.contentEquals(forestDiffSeed.tiles))
    }

    @Test
    fun directionalSwipeCombat_triggersSwordManeuversAndDefensiveParry() {
        val unlocked = GestureSkillId.entries.map { it.id }.toSet()

        // 1. Directional Swipe Classifications
        val rightSwipe = GestureRecognizer.classify(
            listOf(Offset(100f, 200f), Offset(170f, 204f), Offset(240f, 202f)),
            120L,
            unlocked
        )
        assertEquals(GestureSkillId.SWIPE_DASH, rightSwipe.skill)
        assertTrue(rightSwipe.dirX > 0.8f)

        val leftSwipe = GestureRecognizer.classify(
            listOf(Offset(260f, 200f), Offset(180f, 198f), Offset(110f, 202f)),
            120L,
            unlocked
        )
        assertEquals(GestureSkillId.SWIPE_DASH, leftSwipe.skill)
        assertTrue(leftSwipe.dirX < -0.8f)

        val diagUpSwipe = GestureRecognizer.classify(
            listOf(Offset(120f, 260f), Offset(175f, 210f), Offset(220f, 165f)),
            140L,
            unlocked
        )
        assertEquals(GestureSkillId.DIAGONAL_SLASH, diagUpSwipe.skill)
        assertTrue(diagUpSwipe.dirX > 0f && diagUpSwipe.dirY < 0f)

        val upSwipe = GestureRecognizer.classify(
            listOf(Offset(200f, 290f), Offset(202f, 220f), Offset(198f, 140f)),
            110L,
            unlocked
        )
        assertEquals(GestureSkillId.UP_SLAM, upSwipe.skill)

        val downSwipe = GestureRecognizer.classify(
            listOf(Offset(200f, 130f), Offset(202f, 205f), Offset(199f, 275f)),
            110L,
            unlocked
        )
        assertEquals(GestureSkillId.DOWN_PARRY, downSwipe.skill)

        // 2. Left/Right Directional Sword Lunge + Diagonal Cleave + Upward Uppercut
        val enemyLeft = WorldEnemy(
            id = "e_left",
            name = "Street Dino",
            species = EnemySpecies.STREET_DINO,
            zone = WorldZone.BOSQUE_ESMERALDA,
            level = 2,
            isBoss = false,
            x = 220f,
            y = SkulStageGenerator.SPAWN_Y,
            patrolMinX = 120f,
            patrolMaxX = 480f,
            hp = 240,
            maxHp = 240,
            atk = 15,
            rewardSparks = 20,
            rewardXp = 25
        )
        val baseState = GameSessionState(
            currentZone = WorldZone.BOSQUE_ESMERALDA,
            currentRoomIndex = 1,
            playerX = 310f,
            playerY = SkulStageGenerator.SPAWN_Y,
            facingLeft = false,
            hp = 130,
            stamina = 40f,
            enemies = listOf(enemyLeft),
            projectiles = listOf(
                ArrowProjectile(
                    id = 9001L,
                    x = 345f,
                    y = SkulStageGenerator.SPAWN_Y - 18f,
                    vx = -200f,
                    vy = 0f,
                    damage = 20,
                    color = MolotovCrimson,
                    fromPlayer = false
                )
            )
        )

        // Swipe Left turns Boxy left, lunges left, and hits enemyLeft
        val afterLeftLunge = WorldSimulation.performAttack(baseState, leftSwipe, nowMs = 4000L)
        assertTrue(afterLeftLunge.facingLeft)
        assertTrue(afterLeftLunge.playerX < baseState.playerX)
        assertTrue(afterLeftLunge.afterimages.isNotEmpty())
        assertTrue(afterLeftLunge.enemies.first().hp < enemyLeft.hp)
        assertEquals(SlashVisualStyle.HORIZONTAL_LUNGE, afterLeftLunge.slashEffects.last().style)

        // Swipe Up performs rising uppercut launch
        val afterUppercut = WorldSimulation.performAttack(afterLeftLunge, upSwipe, nowMs = 5000L)
        assertFalse(afterUppercut.onGround)
        assertTrue(afterUppercut.playerVy < -300f)
        assertEquals(SlashVisualStyle.UPWARD_UPPERCUT, afterUppercut.slashEffects.last().style)

        // Swipe Diagonal performs crescent hop cleave
        val afterDiag = WorldSimulation.performAttack(baseState, diagUpSwipe, nowMs = 6000L)
        assertFalse(afterDiag.onGround)
        assertTrue(afterDiag.playerVy < -200f)
        assertEquals(SlashVisualStyle.DIAGONAL_CLEAVE, afterDiag.slashEffects.last().style)

        // 3. Swipe Down triggers Defensive Parry Stance, reflects enemy projectile & counters contact
        val afterParry = WorldSimulation.performAttack(baseState, downSwipe, nowMs = 7000L)
        assertTrue(afterParry.isDefending)
        assertTrue(afterParry.parryUntilMs > 7000L)
        assertTrue(afterParry.stamina > baseState.stamina)
        assertEquals(SlashVisualStyle.PARRY_SHIELD, afterParry.slashEffects.last().style)
        assertTrue(afterParry.projectiles.first().fromPlayer) // Reflected hostile projectile!
        assertTrue(afterParry.projectiles.first().vx > 0f)

        // Enemy melee contact during active parry deals 0 damage to Boxy & counter-damages enemy!
        val closeContact = afterParry.copy(
            enemies = listOf(enemyLeft.copy(x = afterParry.playerX + 10f, hp = 240))
        )
        val steppedParry = WorldSimulation.step(closeContact, 0f, 0f, false, 0.016f, 7050L)
        assertEquals(closeContact.hp, steppedParry.hp) // Zero damage taken on Parry!
        assertTrue(steppedParry.enemies.first().hp < 240) // Counter-shockwave damaged attacker!
    }

    @Test
    fun biomeAudioManager_producesDistinctProceduralSoundCuesAndWaveformsForForestDesertCaves() {
        val forestProfile = BiomeAudioManager.profileForZone(WorldZone.BOSQUE_ESMERALDA)
        val desertProfile = BiomeAudioManager.profileForZone(WorldZone.DESIERTO_DORADO)
        val caveProfile = BiomeAudioManager.profileForZone(WorldZone.CAVERNAS_MINA)

        // Distinct cue sets per biome
        assertTrue(BiomeCueKind.BIRD_CHIRP in forestProfile.cues)
        assertTrue(BiomeCueKind.DESERT_FLUTE in desertProfile.cues)
        assertTrue(BiomeCueKind.WATER_DRIP in caveProfile.cues)
        assertFalse(forestProfile.cues.toSet() == desertProfile.cues.toSet())
        assertFalse(desertProfile.cues.toSet() == caveProfile.cues.toSet())
        assertFalse(forestProfile.cues.toSet() == caveProfile.cues.toSet())

        // Procedural cue construction & PCM synthesis yield valid, non-silent audio buffers
        listOf(
            WorldZone.ALDEA_MOKAY,
            WorldZone.BOSQUE_ESMERALDA,
            WorldZone.DESIERTO_DORADO,
            WorldZone.CAVERNAS_MINA,
            WorldZone.BARRIO_SOMBRAVIA
        ).forEach { zone ->
            val cue = BiomeAudioManager.buildProceduralCue(zone, seed = 77L + zone.ordinal)
            assertEquals(zone, cue.zone)
            assertTrue(cue.durationMs in 200..1000)
            assertTrue(cue.baseFreqHz > 40.0)

            val pcm = BiomeAudioManager.synthesizePcmSamples(cue, sampleRate = 22050)
            assertTrue(pcm.size >= 4000)
            assertTrue("Expected audible waveform for ${zone.name}", pcm.any { it != 0.toShort() })
        }
    }
}
