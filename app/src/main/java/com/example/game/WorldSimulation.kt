package com.example.game

import com.example.data.ConsumableKind
import com.example.data.CosmeticSlot
import com.example.data.GameCatalog
import com.example.data.GestureSkillId
import com.example.data.TileKind
import com.example.data.WorldZone
import com.example.ui.theme.BirdySky
import com.example.ui.theme.BloomPink
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.MolotovCrimson
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroMint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

object WorldSimulation {
    private const val GRAVITY = 1040f
    private const val JUMP_VELOCITY = -455f
    private const val MAX_FALL_SPEED = 620f
    private const val DASH_SPEED = 540f
    private const val DASH_DURATION = 0.17f
    private const val HERO_HALF_W = 11f
    private const val HERO_HEIGHT = 34f

    fun spawnEnemiesForStage(zone: WorldZone, roomIndex: Int, stage: GeneratedStageData): List<WorldEnemy> {
        if (zone == WorldZone.ALDEA_MOKAY) return emptyList()
        val ts = SkulStageGenerator.TILE_SIZE
        return stage.enemySpawns.map { spawn ->
            val maxHp = if (spawn.isBoss) 960 else 65 + spawn.level * 32
            val atk = if (spawn.isBoss) 36 else 9 + spawn.level * 4
            val wx = spawn.col * ts
            val wy = spawn.floorRow * ts
            val halfSpanPx = spawn.patrolHalfSpanTiles * ts
            WorldEnemy(
                id = spawn.id,
                name = spawn.name,
                species = spawn.species,
                zone = zone,
                level = spawn.level,
                isBoss = spawn.isBoss,
                x = wx,
                y = wy,
                patrolMinX = (wx - halfSpanPx).coerceAtLeast(60f),
                patrolMaxX = (wx + halfSpanPx).coerceAtMost(SkulStageGenerator.STAGE_WIDTH - 60f),
                hp = maxHp,
                maxHp = maxHp,
                atk = atk,
                rewardSparks = if (spawn.isBoss) 300 else 22 + spawn.level * 7,
                rewardXp = if (spawn.isBoss) 280 else 28 + spawn.level * 10,
                radius = if (spawn.isBoss) 32f else 19f
            )
        }
    }

    fun step(
        state: GameSessionState,
        inputX: Float,
        inputY: Float,
        defending: Boolean,
        dtSeconds: Float,
        nowMs: Long
    ): GameSessionState {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)
        val ts = SkulStageGenerator.TILE_SIZE

        // Stamina drain/regen & timed Downward Parry Shield stance
        val maxStam = state.effectiveMaxStamina()
        var stam = state.stamina
        val manualDefend = defending && stam > 2f
        val parryActive = nowMs < state.parryUntilMs
        val actuallyDefending = manualDefend || parryActive
        stam = if (manualDefend && !parryActive) {
            (stam - 14f * dt).coerceAtLeast(0f)
        } else {
            (stam + state.effectiveStaminaRegenPerSec() * dt).coerceAtMost(maxStam)
        }

        // Dash & Horizontal Movement
        var dashRem = (state.dashTimerSec - dt).coerceAtLeast(0f)
        val afterimages = state.afterimages.mapNotNull {
            val r = it.ttlSec - dt
            if (r <= 0f) null else it.copy(ttlSec = r)
        }.toMutableList()

        val facingLeft = when {
            dashRem > 0f -> state.dashDirX < 0f
            inputX < -0.15f -> true
            inputX > 0.15f -> false
            else -> state.facingLeft
        }

        val moveVx = if (dashRem > 0f) {
            state.dashDirX * DASH_SPEED
        } else {
            val speed = state.effectiveMoveSpeed() * if (actuallyDefending) 0.55f else 1f
            if (abs(inputX) > 0.14f) inputX * speed else 0f
        }

        // Drop through one-way platforms if pushing down hard
        val dropThroughUntil = if (inputY > 0.72f && state.onGround) nowMs + 220L else state.dropThroughUntilMs

        // Vertical Gravity
        val nextVy = if (dashRem > 0f) 0f else (state.playerVy + GRAVITY * dt).coerceAtMost(MAX_FALL_SPEED)

        // Horizontal Collision
        var px = (state.playerX + moveVx * dt).coerceIn(48f, SkulStageGenerator.STAGE_WIDTH - 48f)
        if (hitsSolidBlock(state.stageData, px, state.playerY)) {
            px = state.playerX
        }

        // Vertical Collision + One-way Platform landing
        val prevY = state.playerY
        var py = prevY + nextVy * dt
        var finalVy = nextVy
        var onGround = false
        var jumpsLeft = state.jumpsRemaining

        if (nextVy >= 0f) {
            val footRow = (py / ts).toInt().coerceIn(0, SkulStageGenerator.STAGE_ROWS - 1)
            val prevFootRow = ((prevY - 2f) / ts).toInt().coerceIn(0, SkulStageGenerator.STAGE_ROWS - 1)
            val leftCol = ((px - HERO_HALF_W) / ts).toInt().coerceIn(0, SkulStageGenerator.STAGE_COLS - 1)
            val rightCol = ((px + HERO_HALF_W) / ts).toInt().coerceIn(0, SkulStageGenerator.STAGE_COLS - 1)

            var landedRow = -1
            for (c in leftCol..rightCol) {
                val tile = SkulStageGenerator.tileAt(state.stageData, c, footRow)
                if (tile.solid) {
                    landedRow = footRow
                    break
                }
                if (tile.oneWayPlatform && nowMs >= dropThroughUntil && prevFootRow < footRow) {
                    landedRow = footRow
                    break
                }
            }
            if (landedRow >= 0) {
                py = landedRow * ts
                finalVy = 0f
                onGround = true
                jumpsLeft = state.effectiveExtraJumps()
            }
        } else {
            // Ceiling solid hit
            val headRow = ((py - HERO_HEIGHT) / ts).toInt().coerceIn(0, SkulStageGenerator.STAGE_ROWS - 1)
            val centerCol = (px / ts).toInt().coerceIn(0, SkulStageGenerator.STAGE_COLS - 1)
            if (SkulStageGenerator.isSolid(state.stageData, centerCol, headRow)) {
                py = (headRow + 1) * ts + HERO_HEIGHT
                finalVy = 0f
            }
        }

        val walkPhase = if (abs(moveVx) > 20f && onGround) state.walkPhase + dt * 13f else state.walkPhase + dt * 2.5f
        val swingProgress = if (state.swingProgress > 0f) (state.swingProgress - dt * 4.8f).coerceAtLeast(0f) else 0f

        var bannerTtl = (state.bannerTtlSec - dt).coerceAtLeast(0f)
        var banner = if (bannerTtl <= 0f) null else state.activeBanner

        var hp = state.hp.coerceAtMost(state.effectiveMaxHp())
        var nextId = state.nextEntityId
        val projectiles = state.projectiles.toMutableList()
        val floating = state.floatingTexts.mapNotNull {
            val r = it.ttlSec - dt
            if (r <= 0f) null else it.copy(y = it.y - 24f * dt, ttlSec = r)
        }.toMutableList()
        val slashes = state.slashEffects.mapNotNull {
            val r = it.ttlSec - dt
            if (r <= 0f) null else it.copy(ttlSec = r)
        }.toMutableList()
        val particles = state.particles.mapNotNull {
            val r = it.ttlSec - dt
            if (r <= 0f) null else it.copy(x = it.x + it.vx * dt, y = it.y + it.vy * dt, ttlSec = r)
        }.toMutableList()

        // Dash ghost trail + Dash Relic damage!
        val enemies = state.enemies.toMutableList()
        var sparks = state.sparksCurrency
        var defeatedCount = state.totalEnemiesDefeated
        var bossDefeated = state.molotovBossDefeated
        var heroLvl = state.heroLevel
        var heroXp = state.heroXp
        var statPts = state.statPoints
        var unlockedGestures = state.unlockedGestures
        var invulUntil = state.invulnerableUntilMs

        if (dashRem > 0f) {
            if (Random.nextFloat() < 0.55f) {
                afterimages += DashAfterimage(px, py, facingLeft, state.activeSkullId)
            }
            val dashDmg = state.effectiveDashDamage()
            if (dashDmg > 0) {
                for (i in enemies.indices) {
                    val e = enemies[i]
                    if (e.alive && abs(e.x - px) <= 28f && abs(e.y - py) <= 36f && nowMs >= e.hitFlashMs) {
                        val rem = e.hp - dashDmg
                        floating += FloatingText(nextId++, "-$dashDmg DASH", e.x, e.y - 36f, BirdySky)
                        if (rem <= 0) {
                            enemies[i] = e.copy(alive = false, hp = 0)
                            defeatedCount++
                            if (e.isBoss) bossDefeated = true
                            sparks += e.rewardSparks
                        } else {
                            enemies[i] = e.copy(hp = rem, hitFlashMs = nowMs + 220L)
                        }
                    }
                }
            }
        }

        // Companion Mascotas Mokay float beside Boxy & fire auto-projectiles
        val updatedPets = state.equippedPets.take(3).mapIndexed { index, petId ->
            val existing = state.petStates.firstOrNull { it.petId == petId }
                ?: PetCompanionState(petId, px - 36f, py - 30f)
            val side = if (facingLeft) 1f else -1f
            val tx = px + side * (32f + index * 24f)
            val ty = py - 28f - index * 10f + sin((nowMs / 250.0 + index).toDouble()).toFloat() * 4f
            val nx = existing.x + (tx - existing.x) * (dt * 7.5f).coerceAtMost(1f)
            val ny = existing.y + (ty - existing.y) * (dt * 7.5f).coerceAtMost(1f)
            val petDef = GameCatalog.petById(petId)
            var cd = existing.attackCooldownMs
            if (petDef.firesProjectiles && nowMs >= cd) {
                val target = enemies.filter { it.alive }.minByOrNull { hypot(it.x - nx, it.y - ny) }
                if (target != null && hypot(target.x - nx, target.y - ny) <= 320f) {
                    val dx = target.x - nx
                    val dy = (target.y - 18f) - ny
                    val dm = max(1f, hypot(dx, dy))
                    projectiles += ArrowProjectile(
                        id = nextId++,
                        x = nx,
                        y = ny,
                        vx = (dx / dm) * 340f,
                        vy = (dy / dm) * 340f,
                        damage = (state.effectiveAtk() * 0.50f).roundToInt().coerceAtLeast(8),
                        color = petDef.accentColor,
                        fromPlayer = true
                    )
                    cd = nowMs + 1500L
                }
            }
            PetCompanionState(petId, nx, ny, tx > px, cd)
        }

        // Projectiles update
        val remainingProj = mutableListOf<ArrowProjectile>()
        for (proj in projectiles) {
            val rem = proj.ttlSec - dt
            if (rem <= 0f) continue
            val nx = proj.x + proj.vx * dt
            val nvy = proj.vy + proj.gravity * dt
            val ny = proj.y + nvy * dt
            if (proj.fromPlayer) {
                val hitIdx = enemies.indexOfFirst { it.alive && hypot(it.x - nx, (it.y - 18f) - ny) <= it.radius + proj.radius }
                if (hitIdx >= 0) {
                    val e = enemies[hitIdx]
                    val newHp = e.hp - proj.damage
                    floating += FloatingText(nextId++, "-${proj.damage}", e.x, e.y - 38f, proj.color)
                    RetroSoundEngine.playHit()
                    if (newHp <= 0) {
                        enemies[hitIdx] = e.copy(alive = false, hp = 0)
                        defeatedCount++
                        if (e.isBoss) bossDefeated = true
                        sparks += e.rewardSparks
                        val lvlRes = applyXpGain(heroLvl, heroXp, statPts, unlockedGestures, e.rewardXp)
                        heroLvl = lvlRes.level; heroXp = lvlRes.xp; statPts = lvlRes.statPoints; unlockedGestures = lvlRes.unlockedGestures
                        if (lvlRes.leveledUp) {
                            hp = state.effectiveMaxHp()
                            RetroSoundEngine.playLevelUp()
                            banner = "¡BOXY SUBIÓ A NIVEL $heroLvl!"
                            bannerTtl = 3.2f
                        }
                    } else {
                        enemies[hitIdx] = e.copy(hp = newHp, hitFlashMs = nowMs + 140L)
                    }
                } else if (!SkulStageGenerator.isSolid(state.stageData, (nx / ts).toInt(), (ny / ts).toInt())) {
                    remainingProj += proj.copy(x = nx, y = ny, vy = nvy, ttlSec = rem)
                }
            } else {
                // Enemy Molotov bottle projectile (Parry Stance reflects it back!)
                val hitRadius = HERO_HALF_W + proj.radius + (if (parryActive) 18f else 4f)
                if (hypot(px - nx, (py - 16f) - ny) <= hitRadius) {
                    if (parryActive) {
                        RetroSoundEngine.playShield()
                        floating += FloatingText(nextId++, "¡PARRY REFLEJO!", px, py - 46f, BirdySky)
                        slashes += SlashArcEffect(
                            id = nextId++,
                            x = px,
                            y = py - 18f,
                            dirX = if (facingLeft) -1f else 1f,
                            dirY = 0f,
                            color = BirdySky,
                            radius = 52f,
                            style = SlashVisualStyle.PARRY_SHIELD
                        )
                        remainingProj += proj.copy(
                            x = nx,
                            y = ny,
                            vx = -proj.vx * 1.45f,
                            vy = -35f,
                            gravity = 0f,
                            damage = (state.effectiveAtk() * 1.45f).roundToInt().coerceAtLeast(18),
                            color = BirdySky,
                            fromPlayer = true,
                            ttlSec = 1.4f
                        )
                    } else if (nowMs >= invulUntil && dashRem <= 0f) {
                        val dmg = (proj.damage * (1f - state.effectiveDefensePct())).roundToInt().coerceAtLeast(2)
                        hp -= dmg
                        invulUntil = nowMs + 750L
                        if (actuallyDefending) RetroSoundEngine.playShield() else RetroSoundEngine.playHit()
                        floating += FloatingText(
                            nextId++,
                            if (actuallyDefending) "ESCUDO -$dmg" else "-$dmg",
                            px, py - 42f,
                            if (actuallyDefending) BirdySky else MolotovCrimson
                        )
                    } else if (!SkulStageGenerator.isSolid(state.stageData, (nx / ts).toInt(), (ny / ts).toInt())) {
                        remainingProj += proj.copy(x = nx, y = ny, vy = nvy, ttlSec = rem)
                    }
                } else if (!SkulStageGenerator.isSolid(state.stageData, (nx / ts).toInt(), (ny / ts).toInt())) {
                    remainingProj += proj.copy(x = nx, y = ny, vy = nvy, ttlSec = rem)
                }
            }
        }

        // Enemies Platformer AI (Laughing Cat, Street Dino, Molotov Mokay)
        for (i in enemies.indices) {
            val e = enemies[i]
            if (!e.alive) continue
            val dx = px - e.x
            val distX = abs(dx)
            val distY = abs(py - e.y)
            var ex = e.x
            var ey = e.y
            var fLeft = e.facingLeft
            var aiCd = e.aiCooldownMs

            if (distX < 420f && distY < 160f) {
                fLeft = dx < 0f
                val spd = when (e.species) {
                    EnemySpecies.LAUGHING_CAT -> 96f
                    EnemySpecies.STREET_DINO -> 68f
                    EnemySpecies.MOLOTOV_MOKAY -> if (e.isBoss) 78f else 54f
                }
                if (e.species != EnemySpecies.MOLOTOV_MOKAY || distX > 120f) {
                    val stepX = if (dx > 0f) spd * dt else -spd * dt
                    val cand = (ex + stepX).coerceIn(e.patrolMinX, e.patrolMaxX)
                    if (!SkulStageGenerator.isSolid(state.stageData, (cand / ts).toInt(), ((ey - 8f) / ts).toInt())) {
                        ex = cand
                    }
                }
                if (e.species == EnemySpecies.LAUGHING_CAT && nowMs >= aiCd && distX < 240f) {
                    floating += FloatingText(nextId++, "BWAHAHA!", ex, ey - 42f, MokaySpark)
                    aiCd = nowMs + 2700L
                } else if (e.species == EnemySpecies.MOLOTOV_MOKAY && nowMs >= aiCd && distX < 320f) {
                    val dirSign = if (dx >= 0f) 1f else -1f
                    remainingProj += ArrowProjectile(
                        id = nextId++,
                        x = ex,
                        y = ey - 24f,
                        vx = dirSign * 210f,
                        vy = -175f,
                        damage = e.atk,
                        color = MolotovCrimson,
                        fromPlayer = false,
                        ttlSec = 1.9f,
                        gravity = 320f
                    )
                    aiCd = nowMs + if (e.isBoss) 1350L else 2200L
                }

                // Melee Contact Damage / Active Downward Parry Counter
                if (distX <= e.radius + HERO_HALF_W && distY <= 34f) {
                    if (parryActive && nowMs >= invulUntil) {
                        RetroSoundEngine.playShield()
                        invulUntil = nowMs + 420L
                        val counterDmg = (state.effectiveAtk() * GestureSkillId.DOWN_PARRY.damageMultiplier).roundToInt().coerceAtLeast(16)
                        val pushDir = if (ex >= px) 1f else -1f
                        floating += FloatingText(nextId++, "¡PARRY -$counterDmg!", ex, ey - 46f, BirdySky)
                        slashes += SlashArcEffect(
                            id = nextId++,
                            x = px,
                            y = py - 18f,
                            dirX = pushDir,
                            dirY = 0f,
                            color = BirdySky,
                            radius = 60f,
                            style = SlashVisualStyle.PARRY_SHIELD
                        )
                        val remEnemyHp = e.hp - counterDmg
                        if (remEnemyHp <= 0) {
                            enemies[i] = e.copy(alive = false, hp = 0)
                            defeatedCount++
                            if (e.isBoss) bossDefeated = true
                            sparks += e.rewardSparks
                            val lvlRes = applyXpGain(heroLvl, heroXp, statPts, unlockedGestures, e.rewardXp)
                            heroLvl = lvlRes.level; heroXp = lvlRes.xp; statPts = lvlRes.statPoints; unlockedGestures = lvlRes.unlockedGestures
                            if (lvlRes.leveledUp) {
                                hp = state.effectiveMaxHp()
                                RetroSoundEngine.playLevelUp()
                                banner = "¡BOXY SUBIÓ A NIVEL $heroLvl!"
                                bannerTtl = 3.2f
                            }
                            continue
                        } else {
                            ex = (ex + pushDir * 30f).coerceIn(e.patrolMinX, e.patrolMaxX)
                            enemies[i] = e.copy(x = ex, y = ey, hp = remEnemyHp, hitFlashMs = nowMs + 200L, facingLeft = fLeft, aiCooldownMs = aiCd)
                            continue
                        }
                    } else if (nowMs >= invulUntil && dashRem <= 0f) {
                        val dmg = (e.atk * (1f - state.effectiveDefensePct())).roundToInt().coerceAtLeast(2)
                        hp -= dmg
                        invulUntil = nowMs + 850L
                        if (actuallyDefending) RetroSoundEngine.playShield() else RetroSoundEngine.playHit()
                        floating += FloatingText(
                            nextId++,
                            if (actuallyDefending) "BLOQUEADO -$dmg" else "-$dmg",
                            px, py - 44f,
                            if (actuallyDefending) BirdySky else MolotovCrimson
                        )
                    }
                }
            }
            enemies[i] = e.copy(x = ex, y = ey, facingLeft = fLeft, aiCooldownMs = aiCd)
        }

        // Defeat -> Skul-style respawn back at Aldea Camp (keeps unlocked Skulls, Cosmetics, Sparks & XP!)
        if (hp <= 0) {
            val camp = SkulStageGenerator.generateRoom(WorldZone.ALDEA_MOKAY, 0, state.worldSeed)
            return syncMissions(
                state.copy(
                    currentZone = WorldZone.ALDEA_MOKAY,
                    currentRoomIndex = 0,
                    stageData = camp,
                    enemies = emptyList(),
                    projectiles = emptyList(),
                    roomCleared = true,
                    playerX = SkulStageGenerator.SPAWN_X,
                    playerY = SkulStageGenerator.SPAWN_Y,
                    playerVx = 0f,
                    playerVy = 0f,
                    hp = state.effectiveMaxHp(),
                    stamina = maxStam,
                    activeBanner = "Boxy volvió al Campamento · ¡Conservas tu progreso, mejora y reintenta!",
                    bannerTtlSec = 4.2f
                )
            )
        }

        val wasCleared = state.roomCleared
        val nowCleared = enemies.none { it.alive }
        var roomsCleared = state.roomsClearedCount
        if (!wasCleared && nowCleared && state.currentZone != WorldZone.ALDEA_MOKAY) {
            roomsCleared++
            RetroSoundEngine.playQuestComplete()
            banner = "¡SALA LIMPIA! Cruza un Portal a la derecha (INTERACTUAR)"
            bannerTtl = 4.0f
        }

        return syncMissions(
            state.copy(
                playerX = px,
                playerY = py,
                playerVx = moveVx,
                playerVy = finalVy,
                facingLeft = facingLeft,
                onGround = onGround,
                jumpsRemaining = jumpsLeft,
                dropThroughUntilMs = dropThroughUntil,
                walkPhase = walkPhase,
                swingProgress = swingProgress,
                isDefending = actuallyDefending,
                dashTimerSec = dashRem,
                hp = hp,
                stamina = stam,
                heroLevel = heroLvl,
                heroXp = heroXp,
                statPoints = statPts,
                unlockedGestures = unlockedGestures,
                roomCleared = nowCleared,
                roomsClearedCount = roomsCleared,
                sparksCurrency = sparks,
                totalEnemiesDefeated = defeatedCount,
                molotovBossDefeated = bossDefeated,
                petStates = updatedPets,
                enemies = enemies,
                projectiles = remainingProj,
                afterimages = afterimages,
                floatingTexts = floating,
                particles = particles,
                slashEffects = slashes,
                invulnerableUntilMs = invulUntil,
                nextEntityId = nextId,
                activeBanner = banner,
                bannerTtlSec = bannerTtl,
                lastGestureLabel = if (nowMs >= state.lastGestureUntilMs) null else state.lastGestureLabel
            )
        )
    }

    fun performJump(state: GameSessionState): GameSessionState {
        if (!state.onGround && state.jumpsRemaining <= 0) return state
        RetroSoundEngine.playJump()
        val remJumps = if (state.onGround) state.effectiveExtraJumps() else (state.jumpsRemaining - 1).coerceAtLeast(0)
        val particles = state.particles + List(4) {
            ActionParticle(state.playerX + (it - 1.5f) * 6f, state.playerY, (it - 1.5f) * 35f, -35f, MokayCream, 4f, 0.3f)
        }
        return state.copy(
            playerVy = JUMP_VELOCITY,
            onGround = false,
            jumpsRemaining = remJumps,
            particles = particles
        )
    }

    fun performDash(state: GameSessionState, nowMs: Long = System.currentTimeMillis()): GameSessionState {
        if (nowMs < state.dashCooldownUntilMs) return state
        RetroSoundEngine.playDash()
        val dir = if (state.facingLeft) -1f else 1f
        val cd = (520L * state.effectiveCooldownFactor()).toLong()
        return state.copy(
            dashTimerSec = DASH_DURATION,
            dashDirX = dir,
            dashCooldownUntilMs = nowMs + cd,
            invulnerableUntilMs = maxOf(state.invulnerableUntilMs, nowMs + 220L),
            afterimages = state.afterimages + DashAfterimage(state.playerX, state.playerY, state.facingLeft, state.activeSkullId)
        )
    }

    fun performAttack(
        state: GameSessionState,
        gesture: RecognizedGesture? = null,
        nowMs: Long = System.currentTimeMillis()
    ): GameSessionState {
        val skull = state.activeSkull()
        val skill = gesture?.skill ?: GestureSkillId.TAP_STRIKE
        val readyAt = state.gestureCooldowns[skill.id] ?: 0L
        if (nowMs < readyAt) return state

        // Directional swipes re-orient Boxy toward the swipe vector
        val newFacingLeft = when {
            gesture != null && (skill == GestureSkillId.SWIPE_DASH || skill == GestureSkillId.DIAGONAL_SLASH || skill == GestureSkillId.Z_SLASH) && abs(gesture.dirX) > 0.15f ->
                gesture.dirX < 0f
            gesture != null && skill == GestureSkillId.UP_SLAM && abs(gesture.dirX) > 0.25f ->
                gesture.dirX < 0f
            else -> state.facingLeft
        }
        val dirX = if (newFacingLeft) -1f else 1f
        var nextId = state.nextEntityId
        val floating = state.floatingTexts.toMutableList()
        val particles = state.particles.toMutableList()
        val projectiles = state.projectiles.toMutableList()
        val afterimages = state.afterimages.toMutableList()

        // Ranged Skull Forms (Boxy Bloom Neón & Boxy Birdy Aéreo) shoot rapid projectiles on tap!
        if (skull.rangedBasic && skill == GestureSkillId.TAP_STRIKE) {
            RetroSoundEngine.playBow()
            projectiles += ArrowProjectile(
                id = nextId++,
                x = state.playerX + dirX * 14f,
                y = state.playerY - 18f,
                vx = dirX * 420f,
                vy = 0f,
                damage = state.effectiveAtk(),
                color = skull.primaryColor,
                fromPlayer = true
            )
            return state.copy(
                facingLeft = newFacingLeft,
                swingProgress = 1f,
                comboStep = (state.comboStep + 1) % 3,
                projectiles = projectiles,
                gestureCooldowns = state.gestureCooldowns + (skill.id to (nowMs + 210L)),
                nextEntityId = nextId,
                lastGestureLabel = "TOQUE · DISPARO",
                lastGestureUntilMs = nowMs + 800L
            )
        }

        // DEFENSIVE MANEUVER: Downward Swipe -> Shield Parry & Projectile Deflect Stance
        if (skill == GestureSkillId.DOWN_PARRY) {
            RetroSoundEngine.playShield()
            val deflectedProj = projectiles.map { p ->
                if (!p.fromPlayer && hypot(p.x - state.playerX, p.y - (state.playerY - 18f)) <= 120f) {
                    floating += FloatingText(nextId++, "¡PARRY REFLEJO!", p.x, p.y - 22f, BirdySky)
                    p.copy(
                        vx = -p.vx * 1.45f,
                        vy = -30f,
                        gravity = 0f,
                        damage = (state.effectiveAtk() * 1.45f).roundToInt().coerceAtLeast(18),
                        color = BirdySky,
                        fromPlayer = true,
                        ttlSec = 1.4f
                    )
                } else {
                    p
                }
            }
            val enemies = state.enemies.map { e ->
                if (e.alive && abs(e.x - state.playerX) <= 72f && abs(e.y - state.playerY) <= 58f) {
                    val push = if (e.x >= state.playerX) 1f else -1f
                    e.copy(
                        x = (e.x + push * 26f).coerceIn(e.patrolMinX, e.patrolMaxX),
                        hitFlashMs = nowMs + 120L
                    )
                } else {
                    e
                }
            }
            repeat(8) { idx ->
                val ang = idx * (Math.PI * 2.0 / 8.0)
                particles += ActionParticle(
                    x = state.playerX + cos(ang).toFloat() * 18f,
                    y = state.playerY - 18f + sin(ang).toFloat() * 18f,
                    vx = cos(ang).toFloat() * 85f,
                    vy = sin(ang).toFloat() * 85f,
                    color = BirdySky
                )
            }
            val parrySlashes = state.slashEffects + SlashArcEffect(
                id = nextId++,
                x = state.playerX,
                y = state.playerY - 18f,
                dirX = dirX,
                dirY = 1f,
                color = BirdySky,
                radius = 58f,
                style = SlashVisualStyle.PARRY_SHIELD
            )
            floating += FloatingText(nextId++, "¡GUARDIA PARRY!", state.playerX, state.playerY - 46f, BirdySky)
            return syncMissions(
                state.copy(
                    facingLeft = newFacingLeft,
                    isDefending = true,
                    parryUntilMs = nowMs + 750L,
                    playerVy = if (!state.onGround) 460f else state.playerVy,
                    stamina = (state.stamina + 12f).coerceAtMost(state.effectiveMaxStamina()),
                    enemies = enemies,
                    projectiles = deflectedProj,
                    floatingTexts = floating,
                    particles = particles,
                    slashEffects = parrySlashes,
                    gestureCooldowns = state.gestureCooldowns + (skill.id to (nowMs + skill.cooldownMs)),
                    nextEntityId = nextId,
                    lastGestureLabel = "↓ GUARDIA PARRY",
                    lastGestureUntilMs = nowMs + 1150L
                )
            )
        }

        // SWORD ATTACK MANEUVERS BY SWIPE DIRECTION
        RetroSoundEngine.playSlash()

        // Horizontal Left/Right Swipe: Forward Sword Dash Lunge
        var px = state.playerX
        var py = state.playerY
        var vy = state.playerVy
        var onGround = state.onGround
        var invulUntil = state.invulnerableUntilMs

        if (skill == GestureSkillId.SWIPE_DASH) {
            afterimages += DashAfterimage(px, py, newFacingLeft, state.activeSkullId)
            val stepPx = 14f
            repeat(3) {
                val candX = (px + dirX * stepPx).coerceIn(48f, SkulStageGenerator.STAGE_WIDTH - 48f)
                if (!hitsSolidBlock(state.stageData, candX, py)) {
                    px = candX
                }
            }
            invulUntil = maxOf(invulUntil, nowMs + 160L)
        } else if (skill == GestureSkillId.UP_SLAM) {
            vy = -390f
            onGround = false
        } else if (skill == GestureSkillId.DIAGONAL_SLASH) {
            val hopStep = (px + dirX * 22f).coerceIn(48f, SkulStageGenerator.STAGE_WIDTH - 48f)
            if (!hitsSolidBlock(state.stageData, hopStep, py)) {
                px = hopStep
            }
            vy = if (onGround) -265f else minOf(vy, -190f)
            onGround = false
        }

        val comboMult = 1f + state.comboStep * 0.22f
        val rangeX = when (skill) {
            GestureSkillId.CIRCLE_NOVA -> 125f
            GestureSkillId.HOLD_ULTIMATE -> 155f
            GestureSkillId.SWIPE_DASH -> 108f
            GestureSkillId.DIAGONAL_SLASH -> 108f
            GestureSkillId.Z_SLASH -> 115f
            GestureSkillId.UP_SLAM -> 88f
            else -> 82f
        }
        val rangeY = when (skill) {
            GestureSkillId.UP_SLAM -> 108f
            GestureSkillId.DIAGONAL_SLASH -> 92f
            GestureSkillId.CIRCLE_NOVA, GestureSkillId.HOLD_ULTIMATE -> 88f
            else -> 72f
        }
        val bonusCrit = if (skill == GestureSkillId.DIAGONAL_SLASH) 0.25f else 0f

        val enemies = state.enemies.toMutableList()
        var sparks = state.sparksCurrency
        var defeated = state.totalEnemiesDefeated
        var bossDefeated = state.molotovBossDefeated
        var lvl = state.heroLevel
        var xp = state.heroXp
        var pts = state.statPoints
        var gest = state.unlockedGestures
        var hp = state.hp
        var banner = state.activeBanner
        var bannerTtl = state.bannerTtlSec

        for (i in enemies.indices) {
            val e = enemies[i]
            if (!e.alive) continue
            val dx = e.x - px
            val dy = abs(e.y - py)
            val inFrontOrNova = skill == GestureSkillId.CIRCLE_NOVA ||
                skill == GestureSkillId.HOLD_ULTIMATE ||
                skill == GestureSkillId.UP_SLAM ||
                (dx * dirX > -32f)
            if (abs(dx) <= rangeX + e.radius && dy <= rangeY && inFrontOrNova) {
                val isCrit = Random.nextFloat() < (state.effectiveCritPct() + bonusCrit).coerceAtMost(0.95f)
                val rawDmg = state.effectiveAtk() * skill.damageMultiplier * comboMult * (if (isCrit) 1.65f else 1f)
                val dmg = rawDmg.roundToInt()
                val remHp = e.hp - dmg
                floating += FloatingText(
                    nextId++,
                    if (isCrit) "¡CRIT -$dmg!" else "-$dmg",
                    e.x, e.y - 38f,
                    if (isCrit) MokaySpark else skull.accentColor
                )
                repeat(4) {
                    val ang = Random.nextFloat() * 6.28f
                    particles += ActionParticle(e.x, e.y - 16f, cos(ang) * 90f, sin(ang) * 90f, skull.accentColor)
                }
                if (remHp <= 0) {
                    enemies[i] = e.copy(alive = false, hp = 0)
                    defeated++
                    if (e.isBoss) bossDefeated = true
                    sparks += e.rewardSparks
                    val res = applyXpGain(lvl, xp, pts, gest, e.rewardXp)
                    lvl = res.level; xp = res.xp; pts = res.statPoints; gest = res.unlockedGestures
                    if (res.leveledUp) {
                        hp = state.effectiveMaxHp()
                        RetroSoundEngine.playLevelUp()
                        banner = "¡NIVEL $lvl! Mejora a Boxy en Tienda/Armería"
                        bannerTtl = 3.5f
                    }
                } else {
                    val knockback = if (skill == GestureSkillId.SWIPE_DASH) 24f else 16f
                    enemies[i] = e.copy(x = (e.x + dirX * knockback).coerceIn(e.patrolMinX, e.patrolMaxX), hp = remHp, hitFlashMs = nowMs + 140L)
                }
            }
        }

        val slashStyle = when (skill) {
            GestureSkillId.SWIPE_DASH -> SlashVisualStyle.HORIZONTAL_LUNGE
            GestureSkillId.DIAGONAL_SLASH -> SlashVisualStyle.DIAGONAL_CLEAVE
            GestureSkillId.UP_SLAM -> SlashVisualStyle.UPWARD_UPPERCUT
            GestureSkillId.CIRCLE_NOVA, GestureSkillId.HOLD_ULTIMATE -> SlashVisualStyle.WHIRLWIND_NOVA
            GestureSkillId.Z_SLASH -> SlashVisualStyle.ZIGZAG_BOLT
            else -> SlashVisualStyle.ARC
        }
        val slashColor = when (skill) {
            GestureSkillId.DIAGONAL_SLASH -> MokaySpark
            GestureSkillId.UP_SLAM -> RetroMint
            GestureSkillId.Z_SLASH -> BirdySky
            else -> skull.accentColor
        }
        val slashes = state.slashEffects + SlashArcEffect(
            id = nextId++,
            x = px + if (slashStyle == SlashVisualStyle.WHIRLWIND_NOVA) 0f else dirX * 26f,
            y = py - if (slashStyle == SlashVisualStyle.UPWARD_UPPERCUT) 30f else 18f,
            dirX = dirX,
            dirY = gesture?.dirY ?: 0f,
            color = slashColor,
            radius = if (skill == GestureSkillId.CIRCLE_NOVA || skill == GestureSkillId.HOLD_ULTIMATE) 95f else 58f,
            style = slashStyle
        )

        val gestureLabel = when (skill) {
            GestureSkillId.SWIPE_DASH -> if (newFacingLeft) "← ESTOCADA IZQ" else "→ ESTOCADA DER"
            GestureSkillId.DIAGONAL_SLASH -> if (newFacingLeft) "↖ CORTE DIAGONAL" else "↗ CORTE DIAGONAL"
            GestureSkillId.UP_SLAM -> "↑ GANCHO ASCENDENTE"
            GestureSkillId.CIRCLE_NOVA -> "◯ TORBELLINO"
            GestureSkillId.Z_SLASH -> "⚡ RELÁMPAGO Z"
            GestureSkillId.HOLD_ULTIMATE -> "🔥 SUPERNOVA BOXY"
            else -> "TOQUE · CORTE"
        }

        return syncMissions(
            state.copy(
                playerX = px,
                playerY = py,
                playerVy = vy,
                onGround = onGround,
                facingLeft = newFacingLeft,
                swingProgress = 1f,
                comboStep = (state.comboStep + 1) % 3,
                enemies = enemies,
                sparksCurrency = sparks,
                totalEnemiesDefeated = defeated,
                molotovBossDefeated = bossDefeated,
                heroLevel = lvl,
                heroXp = xp,
                statPoints = pts,
                unlockedGestures = gest,
                hp = hp,
                afterimages = afterimages,
                floatingTexts = floating,
                particles = particles,
                slashEffects = slashes,
                invulnerableUntilMs = invulUntil,
                gestureCooldowns = state.gestureCooldowns + (skill.id to (nowMs + skill.cooldownMs)),
                nextEntityId = nextId,
                activeBanner = banner,
                bannerTtlSec = bannerTtl,
                lastGestureLabel = gestureLabel,
                lastGestureUntilMs = nowMs + 1000L
            )
        )
    }

    fun performSkill(state: GameSessionState, slot: Int, nowMs: Long = System.currentTimeMillis()): GameSessionState {
        val skull = state.activeSkull()
        val readyAt = if (slot == 1) state.skill1CooldownUntilMs else state.skill2CooldownUntilMs
        if (nowMs < readyAt) return state
        if (state.stamina < 14f) {
            return state.copy(
                floatingTexts = state.floatingTexts + FloatingText(state.nextEntityId, "¡Sin Estamina! Toma bebida", state.playerX, state.playerY - 44f, BirdySky),
                nextEntityId = state.nextEntityId + 1
            )
        }

        RetroSoundEngine.playSkill()
        val dirX = if (state.facingLeft) -1f else 1f
        var nextId = state.nextEntityId
        val projectiles = state.projectiles.toMutableList()
        val floating = state.floatingTexts.toMutableList()
        val enemies = state.enemies.toMutableList()
        var sparks = state.sparksCurrency
        var defeated = state.totalEnemiesDefeated
        var bossDefeated = state.molotovBossDefeated

        floating += FloatingText(
            nextId++,
            if (slot == 1) skull.skill1Name else skull.skill2Name,
            state.playerX, state.playerY - 46f, skull.accentColor
        )

        if (slot == 1) {
            // Skill 1: Multi-projectile fan barrage matching active Boxy Skull
            val count = if (skull.id == "boxy_birdy") 6 else 4
            for (k in 0 until count) {
                val spreadVy = (k - count / 2f) * 55f
                projectiles += ArrowProjectile(
                    id = nextId++,
                    x = state.playerX + dirX * 14f,
                    y = state.playerY - 18f,
                    vx = dirX * 400f,
                    vy = spreadVy,
                    damage = (state.effectiveAtk() * 1.15f).roundToInt(),
                    color = skull.accentColor,
                    fromPlayer = true
                )
            }
        } else {
            // Skill 2: Heavy AoE Explosion around Boxy!
            val dmg = (state.effectiveAtk() * 2.25f).roundToInt()
            for (i in enemies.indices) {
                val e = enemies[i]
                if (!e.alive) continue
                if (hypot(e.x - state.playerX, e.y - state.playerY) <= 155f) {
                    val rem = e.hp - dmg
                    floating += FloatingText(nextId++, "-$dmg", e.x, e.y - 36f, skull.primaryColor)
                    if (rem <= 0) {
                        enemies[i] = e.copy(alive = false, hp = 0)
                        defeated++
                        if (e.isBoss) bossDefeated = true
                        sparks += e.rewardSparks
                    } else {
                        enemies[i] = e.copy(hp = rem, hitFlashMs = nowMs + 180L)
                    }
                }
            }
        }

        val slashes = state.slashEffects + SlashArcEffect(
            id = nextId++,
            x = state.playerX,
            y = state.playerY - 16f,
            dirX = dirX,
            dirY = 0f,
            color = skull.primaryColor,
            radius = if (slot == 2) 135f else 68f
        )

        val baseCd = if (slot == 1) skull.skill1CdMs else skull.skill2CdMs
        val finalCd = (baseCd * state.effectiveCooldownFactor()).toLong()
        return syncMissions(
            state.copy(
                stamina = (state.stamina - 14f).coerceAtLeast(0f),
                swingProgress = 1f,
                enemies = enemies,
                sparksCurrency = sparks,
                totalEnemiesDefeated = defeated,
                molotovBossDefeated = bossDefeated,
                projectiles = projectiles,
                floatingTexts = floating,
                slashEffects = slashes,
                skill1CooldownUntilMs = if (slot == 1) nowMs + finalCd else state.skill1CooldownUntilMs,
                skill2CooldownUntilMs = if (slot == 2) nowMs + finalCd else state.skill2CooldownUntilMs,
                nextEntityId = nextId
            )
        )
    }

    /**
     * Signature SKUL Mechanic: Swap between Boxy's 2 Skull/Match Forms and unleash Swap Skill AoE!
     */
    fun performSwapSkull(state: GameSessionState, nowMs: Long = System.currentTimeMillis()): GameSessionState {
        if (state.activeSkullId == state.secondarySkullId) {
            return state.copy(
                floatingTexts = state.floatingTexts + FloatingText(state.nextEntityId, "Equipa 2 Cabezas en Menú", state.playerX, state.playerY - 46f, MokaySpark),
                nextEntityId = state.nextEntityId + 1
            )
        }
        if (nowMs < state.swapCooldownUntilMs) return state

        RetroSoundEngine.playSwap()
        val incomingSkull = state.secondarySkull()
        val newActive = incomingSkull.id
        val newSecondary = state.activeSkullId
        var nextId = state.nextEntityId
        val floating = state.floatingTexts.toMutableList()
        val enemies = state.enemies.toMutableList()
        var sparks = state.sparksCurrency
        var defeated = state.totalEnemiesDefeated
        var bossDefeated = state.molotovBossDefeated

        floating += FloatingText(nextId++, "¡SWAP: ${incomingSkull.swapSkillName}!", state.playerX, state.playerY - 48f, incomingSkull.accentColor)

        val swapDmg = (state.effectiveAtk() * 1.75f).roundToInt()
        for (i in enemies.indices) {
            val e = enemies[i]
            if (e.alive && hypot(e.x - state.playerX, e.y - state.playerY) <= 130f) {
                val rem = e.hp - swapDmg
                floating += FloatingText(nextId++, "-$swapDmg", e.x, e.y - 36f, incomingSkull.primaryColor)
                if (rem <= 0) {
                    enemies[i] = e.copy(alive = false, hp = 0)
                    defeated++
                    if (e.isBoss) bossDefeated = true
                    sparks += e.rewardSparks
                } else {
                    enemies[i] = e.copy(hp = rem, hitFlashMs = nowMs + 160L)
                }
            }
        }

        val slashes = state.slashEffects + SlashArcEffect(
            id = nextId++,
            x = state.playerX,
            y = state.playerY - 16f,
            dirX = 1f,
            dirY = 0f,
            color = incomingSkull.accentColor,
            radius = 120f
        )

        return syncMissions(
            state.copy(
                activeSkullId = newActive,
                secondarySkullId = newSecondary,
                skullSwapsCount = state.skullSwapsCount + 1L,
                swapCooldownUntilMs = nowMs + (2200L * state.effectiveCooldownFactor()).toLong(),
                enemies = enemies,
                sparksCurrency = sparks,
                totalEnemiesDefeated = defeated,
                molotovBossDefeated = bossDefeated,
                floatingTexts = floating,
                slashEffects = slashes,
                nextEntityId = nextId,
                activeBanner = "${incomingSkull.name} · ${incomingSkull.swapSkillName}",
                bannerTtlSec = 2.4f
            )
        )
    }

    /**
     * Context Interact: Enter cleared Room Portal (with Combat Gold OR Skull/Relic reward!), open Chests/Crates, or use Hub House Furniture.
     */
    fun performInteract(state: GameSessionState): GameSessionState {
        // 1. Check nearby Portals when room is cleared!
        val nearPortal = state.stageData.portals.firstOrNull { abs(it.x - state.playerX) <= 58f }
        if (nearPortal != null) {
            if (!state.roomCleared) {
                return state.copy(
                    floatingTexts = state.floatingTexts + FloatingText(state.nextEntityId, "¡Derrota a los enemigos primero!", state.playerX, state.playerY - 46f, MolotovCrimson),
                    nextEntityId = state.nextEntityId + 1
                )
            }
            RetroSoundEngine.playSwap()
            val nextStage = SkulStageGenerator.generateRoom(nearPortal.nextZone, nearPortal.nextRoomIndex, state.worldSeed)
            val nextEnemies = spawnEnemiesForStage(nearPortal.nextZone, nearPortal.nextRoomIndex, nextStage)

            var sparks = state.sparksCurrency
            var unlockedSkulls = state.unlockedSkulls
            var relics = state.equippedRelics
            val rewardNote = when (nearPortal.rewardKind) {
                PortalRewardKind.COMBAT_GOLD -> {
                    sparks += 35L
                    "+35⚡ Oro"
                }
                PortalRewardKind.SKULL_RELIC -> {
                    val lockedSkull = GameCatalog.skullForms.firstOrNull { it.id !in unlockedSkulls }
                    val availRelic = GameCatalog.relics.firstOrNull { it.id !in relics }
                    when {
                        lockedSkull != null -> {
                            unlockedSkulls = unlockedSkulls + lockedSkull.id
                            "¡Desbloqueada Cabeza ${lockedSkull.name}!"
                        }
                        availRelic != null && relics.size < 6 -> {
                            relics = relics + availRelic.id
                            "¡Reliquia ${availRelic.name}!"
                        }
                        else -> {
                            sparks += 45L
                            "+45⚡ Botín"
                        }
                    }
                }
                PortalRewardKind.NEXT_ACT -> "¡Nuevo Acto!"
                PortalRewardKind.BOSS_GATE -> "¡Jefe Molotov Mokay!"
            }

            return syncMissions(
                state.copy(
                    currentZone = nearPortal.nextZone,
                    currentRoomIndex = nearPortal.nextRoomIndex,
                    stageData = nextStage,
                    enemies = nextEnemies,
                    roomCleared = nextEnemies.isEmpty(),
                    playerX = SkulStageGenerator.SPAWN_X,
                    playerY = SkulStageGenerator.SPAWN_Y,
                    playerVx = 0f,
                    playerVy = 0f,
                    sparksCurrency = sparks,
                    unlockedSkulls = unlockedSkulls,
                    equippedRelics = relics,
                    projectiles = emptyList(),
                    activeBanner = "${nearPortal.nextZone.title} (Sala ${nearPortal.nextRoomIndex}) · $rewardNote",
                    bannerTtlSec = 4.0f
                )
            )
        }

        // 2. Check nearby interactive tile (Chest, Crate, Bed, Kitchen, TV, Skull Altar)
        val ts = SkulStageGenerator.TILE_SIZE
        val pCol = (state.playerX / ts).toInt()
        val pRow = ((state.playerY - 12f) / ts).toInt()
        for (dc in -1..1) {
            for (dr in -1..1) {
                val c = pCol + dc
                val r = pRow + dr
                val tile = SkulStageGenerator.tileAt(state.stageData, c, r)
                when (tile) {
                    TileKind.BED -> {
                        RetroSoundEngine.playEatDrink()
                        return state.copy(
                            hp = state.effectiveMaxHp(),
                            stamina = state.effectiveMaxStamina(),
                            activeBanner = "Boxy descansó · 100% Vida y Estamina",
                            bannerTtlSec = 2.6f
                        )
                    }
                    TileKind.KITCHEN -> {
                        RetroSoundEngine.playEatDrink()
                        val inv = state.inventoryCounts.toMutableMap()
                        inv["food_sandwich"] = (inv["food_sandwich"] ?: 0) + 1
                        return state.copy(
                            inventoryCounts = inv,
                            activeBanner = "Preparaste +1 Sándwich Mokay en la Cocina",
                            bannerTtlSec = 2.6f
                        )
                    }
                    TileKind.TV -> {
                        return state.copy(
                            tvInfoDialog = "MOKAYVERSE SKUL TV:\n• Boxy puede llevar 2 Cabezas/Fósforos a la vez y usar CAMBIAR para lanzar un ataque de relevo.\n• Presiona DASH para atravesar ataques enemigos sin recibir daño.\n• Limpia todos los enemigos de cada sala y entra por un Portal a la derecha (Combate o Cabeza/Reliquia)."
                        )
                    }
                    TileKind.CHEST, TileKind.CRATE -> {
                        RetroSoundEngine.playBuild()
                        val newTiles = state.stageData.tiles.copyOf()
                        newTiles[SkulStageGenerator.idx(c, r)] = TileKind.AIR.id
                        val invMgr = state.inventoryManager()
                        val axeLvl = invMgr.getToolLevel(CraftableTool.AXE).coerceAtLeast(1)
                        val pickLvl = invMgr.getToolLevel(CraftableTool.PICKAXE).coerceAtLeast(1)
                        val loot = BiomeRegistry.rollResourceLoot(
                            zone = state.currentZone,
                            isChest = tile == TileKind.CHEST,
                            axeLevel = axeLvl,
                            pickaxeLevel = pickLvl
                        )
                        invMgr.addMaterials(loot)
                        val inv = invMgr.writeToInventoryMap(state.inventoryCounts).toMutableMap()
                        val bonusSparks = if (tile == TileKind.CHEST) 45L else 18L
                        if (tile == TileKind.CHEST) {
                            inv["food_apple"] = (inv["food_apple"] ?: 0) + 1
                            inv["drink_oasis"] = (inv["drink_oasis"] ?: 0) + 1
                        }
                        val lootSummary = loot.entries.joinToString(" ") { (mat, q) -> "+$q ${mat.displayName}" }
                        return state.copy(
                            stageData = state.stageData.copy(tiles = newTiles),
                            sparksCurrency = state.sparksCurrency + bonusSparks,
                            inventoryCounts = inv,
                            floatingTexts = state.floatingTexts + FloatingText(
                                state.nextEntityId,
                                "+$bonusSparks⚡ $lootSummary",
                                c * ts + 18f, r * ts, MokaySpark
                            ),
                            nextEntityId = state.nextEntityId + 1
                        )
                    }
                    else -> {}
                }
            }
        }
        return state
    }

    fun unlockOrEquipSkull(state: GameSessionState, skullId: String, toSlot: Int): Pair<GameSessionState, String> {
        val def = GameCatalog.skullById(skullId)
        var sparks = state.sparksCurrency
        var unlocked = state.unlockedSkulls
        if (skullId !in unlocked) {
            if (sparks < def.costSparks) return state to "Faltan ${def.costSparks}⚡ para desbloquear ${def.name}."
            sparks -= def.costSparks
            unlocked = unlocked + skullId
            RetroSoundEngine.playBuild()
        }
        val next = if (toSlot == 1) {
            state.copy(sparksCurrency = sparks, unlockedSkulls = unlocked, activeSkullId = skullId)
        } else {
            state.copy(sparksCurrency = sparks, unlockedSkulls = unlocked, secondarySkullId = skullId)
        }
        return syncMissions(next.copy(jumpsRemaining = next.effectiveExtraJumps())) to "${def.name} equipada en Ranura $toSlot."
    }

    fun buyOrToggleRelic(state: GameSessionState, relicId: String): Pair<GameSessionState, String> {
        val def = GameCatalog.relicById(relicId) ?: return state to "Reliquia inválida."
        if (relicId in state.equippedRelics) {
            return state.copy(equippedRelics = state.equippedRelics - relicId) to "${def.name} retirada."
        }
        if (state.equippedRelics.size >= 6) return state to "Máximo 6 Reliquias Skul equipadas."
        if (state.sparksCurrency < def.costSparks) return state to "Necesitas ${def.costSparks}⚡."
        RetroSoundEngine.playBuild()
        val updated = state.copy(
            sparksCurrency = state.sparksCurrency - def.costSparks,
            equippedRelics = state.equippedRelics + relicId
        )
        return syncMissions(updated.copy(hp = updated.hp.coerceAtMost(updated.effectiveMaxHp()))) to "¡Reliquia equipada: ${def.name}!"
    }

    fun upgradeWeaponOrShield(state: GameSessionState, isShield: Boolean): Pair<GameSessionState, String> {
        val cur = if (isShield) state.shieldLevel else state.weaponLevel
        if (cur >= 5) return state to "Nivel máximo (Nv.5)."
        val nextLvl = cur + 1
        val cost = 35 * nextLvl
        if (state.sparksCurrency < cost) return state to "Necesitas ${cost}⚡."
        RetroSoundEngine.playBuild()
        val updated = if (isShield) {
            state.copy(shieldLevel = nextLvl, sparksCurrency = state.sparksCurrency - cost)
        } else {
            state.copy(weaponLevel = nextLvl, sparksCurrency = state.sparksCurrency - cost)
        }
        return syncMissions(updated) to "¡Mejorado a Nv.$nextLvl!"
    }

    fun craftToolUpgrade(state: GameSessionState, tool: CraftableTool): Pair<GameSessionState, String> {
        val invMgr = state.inventoryManager()
        var sparks = state.sparksCurrency
        val res = invMgr.craftNextToolLevel(tool, sparks) { spent ->
            sparks = (sparks - spent).coerceAtLeast(0L)
        }
        return when (res) {
            is CraftResult.Success -> {
                RetroSoundEngine.playBuild()
                val updated = state.copy(
                    sparksCurrency = sparks,
                    inventoryCounts = invMgr.writeToInventoryMap(state.inventoryCounts)
                )
                syncMissions(updated) to res.message
            }
            is CraftResult.Failure -> state to res.reason
        }
    }

    fun craftArrows(state: GameSessionState): Pair<GameSessionState, String> {
        val invMgr = state.inventoryManager()
        if (!invMgr.craftArrowsBundle(10)) {
            return state to "Necesitas 3 Madera y 2 Piedra para fabricar 10 Flechas."
        }
        RetroSoundEngine.playBuild()
        return state.copy(
            inventoryCounts = invMgr.writeToInventoryMap(state.inventoryCounts)
        ) to "¡Fabricadas +10 Flechas (Total: ${invMgr.arrowsCount})!"
    }

    fun buyConsumable(state: GameSessionState, id: String): Pair<GameSessionState, String> {
        val def = GameCatalog.consumableById(id) ?: return state to "Objeto inválido."
        if (state.sparksCurrency < def.costSparks) return state to "Necesitas ${def.costSparks}⚡."
        val inv = state.inventoryCounts.toMutableMap()
        inv[id] = (inv[id] ?: 0) + 1
        RetroSoundEngine.playBuild()
        return state.copy(sparksCurrency = state.sparksCurrency - def.costSparks, inventoryCounts = inv) to "Comprado: ${def.name}"
    }

    fun useConsumable(state: GameSessionState, id: String): Pair<GameSessionState, String> {
        val def = GameCatalog.consumableById(id) ?: return state to "Objeto no encontrado."
        val qty = state.inventoryCounts[id] ?: 0
        if (qty <= 0) return state to "No tienes ${def.name}."
        RetroSoundEngine.playEatDrink()
        val inv = state.inventoryCounts.toMutableMap()
        inv[id] = qty - 1
        return if (def.kind == ConsumableKind.FOOD) {
            val newHp = (state.hp + def.restoreAmount).coerceAtMost(state.effectiveMaxHp())
            state.copy(
                hp = newHp,
                inventoryCounts = inv,
                floatingTexts = state.floatingTexts + FloatingText(state.nextEntityId, "+${def.restoreAmount} HP", state.playerX, state.playerY - 40f, RetroMint),
                nextEntityId = state.nextEntityId + 1
            ) to "Comiste ${def.name} (+${def.restoreAmount} Vida)"
        } else {
            val newStam = (state.stamina + def.restoreAmount).coerceAtMost(state.effectiveMaxStamina())
            state.copy(
                stamina = newStam,
                inventoryCounts = inv,
                floatingTexts = state.floatingTexts + FloatingText(state.nextEntityId, "+${def.restoreAmount} EST", state.playerX, state.playerY - 40f, BirdySky),
                nextEntityId = state.nextEntityId + 1
            ) to "Bebiste ${def.name} (+${def.restoreAmount} Estamina)"
        }
    }

    fun equipOrBuyCosmetic(state: GameSessionState, id: String): Pair<GameSessionState, String> {
        val item = GameCatalog.cosmeticById(id) ?: return state to "Prenda desconocida."
        var sparks = state.sparksCurrency
        var unlocked = state.unlockedCosmetics
        if (id !in unlocked) {
            if (state.heroLevel < item.unlockLevel) return state to "Requiere Nivel ${item.unlockLevel}."
            if (sparks < item.costSparks) return state to "Necesitas ${item.costSparks}⚡."
            sparks -= item.costSparks
            unlocked = unlocked + id
            RetroSoundEngine.playBuild()
        }
        val app = when (item.slot) {
            CosmeticSlot.HEADGEAR -> state.appearance.copy(headgearId = id)
            CosmeticSlot.JACKET -> state.appearance.copy(jacketId = id)
            CosmeticSlot.PANTS_SHOES -> state.appearance.copy(pantsShoesId = id)
            CosmeticSlot.BACKPACK -> state.appearance.copy(backpackId = id)
        }
        return state.copy(sparksCurrency = sparks, unlockedCosmetics = unlocked, appearance = app) to "Equipado Mokay Streetwear: ${item.name}"
    }

    fun toggleEquipPet(state: GameSessionState, petId: String): Pair<GameSessionState, String> {
        if (petId !in state.unlockedPets) {
            val pet = GameCatalog.petById(petId)
            return state to "Bloqueada · ${pet.unlockHint}"
        }
        val current = state.equippedPets.toMutableList()
        return if (petId in current) {
            if (current.size <= 1) return state to "Lleva al menos 1 Mascota activa."
            current.remove(petId)
            state.copy(equippedPets = current) to "${GameCatalog.petById(petId).name} guardada."
        } else {
            if (current.size >= 3) current.removeAt(0)
            current.add(petId)
            state.copy(equippedPets = current) to "¡${GameCatalog.petById(petId).name} equipada (${current.size}/3)!"
        }
    }

    fun claimMission(state: GameSessionState, missionId: String): Pair<GameSessionState, String> {
        val def = GameCatalog.missions.firstOrNull { it.id == missionId } ?: return state to "Misión inválida."
        if (missionId in state.claimedMissionIds) return state to "Recompensa ya reclamada."
        if (state.missionProgressCount(missionId) < def.targetCount) return state to "Aún no completas esta misión."
        RetroSoundEngine.playQuestComplete()

        val xpRes = applyXpGain(state.heroLevel, state.heroXp, state.statPoints, state.unlockedGestures, def.rewardXp)
        val unlockedPets = if (def.unlocksPetId != null) state.unlockedPets + def.unlocksPetId else state.unlockedPets
        val equippedPets = if (def.unlocksPetId != null && state.equippedPets.size < 3) {
            (state.equippedPets + def.unlocksPetId).distinct().take(3)
        } else state.equippedPets
        val unlockedSkulls = if (def.unlocksSkullId != null) state.unlockedSkulls + def.unlocksSkullId else state.unlockedSkulls

        return syncMissions(
            state.copy(
                sparksCurrency = state.sparksCurrency + def.rewardSparks,
                heroLevel = xpRes.level,
                heroXp = xpRes.xp,
                statPoints = xpRes.statPoints,
                unlockedGestures = xpRes.unlockedGestures,
                hp = if (xpRes.leveledUp) state.effectiveMaxHp() else state.hp,
                unlockedPets = unlockedPets,
                equippedPets = equippedPets,
                unlockedSkulls = unlockedSkulls,
                claimedMissionIds = state.claimedMissionIds + missionId,
                completedMissionIds = state.completedMissionIds + missionId
            )
        ) to "¡Misión cumplida! +${def.rewardSparks}⚡ +${def.rewardXp} XP"
    }

    fun summonMolotovBoss(state: GameSessionState): Pair<GameSessionState, String> {
        val bossStage = SkulStageGenerator.generateRoom(WorldZone.BARRIO_SOMBRAVIA, 2, state.worldSeed)
        val bossEnemies = spawnEnemiesForStage(WorldZone.BARRIO_SOMBRAVIA, 2, bossStage)
        return state.copy(
            currentZone = WorldZone.BARRIO_SOMBRAVIA,
            currentRoomIndex = 2,
            stageData = bossStage,
            enemies = bossEnemies,
            roomCleared = false,
            playerX = SkulStageGenerator.SPAWN_X,
            playerY = SkulStageGenerator.SPAWN_Y,
            projectiles = emptyList(),
            activeBanner = "¡ARENA FINAL: MOLOTOV MOKAY REY PUNK!",
            bannerTtlSec = 4.0f
        ) to "¡Enfrentamiento contra Molotov Mokay Rey Punk!"
    }

    fun allocateStatPoint(state: GameSessionState, statKey: String): GameSessionState {
        if (state.statPoints <= 0) return state
        val updated = when (statKey) {
            "ATK" -> state.copy(atkBonus = state.atkBonus + 1, statPoints = state.statPoints - 1)
            "HP" -> state.copy(hpBonus = state.hpBonus + 1, statPoints = state.statPoints - 1)
            "SPD" -> state.copy(spdBonus = state.spdBonus + 1, statPoints = state.statPoints - 1)
            else -> state
        }
        return updated.copy(hp = updated.effectiveMaxHp())
    }

    private fun syncMissions(state: GameSessionState): GameSessionState {
        val completed = state.completedMissionIds.toMutableSet()
        for (m in GameCatalog.missions) {
            if (state.missionProgressCount(m.id) >= m.targetCount) completed += m.id
        }
        return if (completed != state.completedMissionIds) state.copy(completedMissionIds = completed) else state
    }

    private fun hitsSolidBlock(data: GeneratedStageData, px: Float, py: Float): Boolean {
        val ts = SkulStageGenerator.TILE_SIZE
        val leftC = ((px - HERO_HALF_W) / ts).toInt()
        val rightC = ((px + HERO_HALF_W) / ts).toInt()
        val midRow = ((py - 16f) / ts).toInt()
        val topRow = ((py - HERO_HEIGHT + 4f) / ts).toInt()
        return SkulStageGenerator.isSolid(data, leftC, midRow) ||
            SkulStageGenerator.isSolid(data, rightC, midRow) ||
            SkulStageGenerator.isSolid(data, leftC, topRow) ||
            SkulStageGenerator.isSolid(data, rightC, topRow)
    }

    private fun applyXpGain(
        curLevel: Int,
        curXp: Int,
        curPts: Int,
        curGestures: Set<String>,
        xpGain: Int
    ): LevelRes {
        var lvl = curLevel
        var xp = curXp + xpGain
        var pts = curPts
        val gest = curGestures.toMutableSet()
        var leveled = false
        while (xp >= GameCatalog.xpRequiredForLevel(lvl)) {
            xp -= GameCatalog.xpRequiredForLevel(lvl)
            lvl++
            pts++
            leveled = true
            GestureSkillId.entries.filter { it.minLevel <= lvl }.forEach { gest += it.id }
        }
        return LevelRes(lvl, xp, pts, gest, leveled)
    }

    private data class LevelRes(
        val level: Int,
        val xp: Int,
        val statPoints: Int,
        val unlockedGestures: Set<String>,
        val leveledUp: Boolean
    )
}
