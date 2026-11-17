package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.WorldZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class BiomeCueKind {
    BIRD_CHIRP,
    LEAF_RUSTLE,
    WOODLAND_CHIME,
    SAND_GUST,
    DESERT_FLUTE,
    MIRAGE_BELL,
    WATER_DRIP,
    CRYSTAL_ECHO,
    DEEP_RUMBLE,
    SHADOW_PULSE,
    NEON_SPARK,
    CAMPFIRE_CRACKLE,
    VILLAGE_CHORD
}

data class BiomeAudioCue(
    val zone: WorldZone,
    val kind: BiomeCueKind,
    val label: String,
    val durationMs: Int,
    val baseFreqHz: Double
)

data class BiomeAudioProfile(
    val zone: WorldZone,
    val atmosphereName: String,
    val minIntervalMs: Long,
    val maxIntervalMs: Long,
    val cues: List<BiomeCueKind>
)

object BiomeAudioManager {
    @Volatile
    var ambientEnabled: Boolean = true

    @Volatile
    var ambientVolume: Float = 0.42f

    @Volatile
    var activeZone: WorldZone = WorldZone.ALDEA_MOKAY

    @Volatile
    var isPaused: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ambientJob: Job? = null

    private val profiles: Map<WorldZone, BiomeAudioProfile> = mapOf(
        WorldZone.ALDEA_MOKAY to BiomeAudioProfile(
            zone = WorldZone.ALDEA_MOKAY,
            atmosphereName = "Fogata y Brisa de Aldea",
            minIntervalMs = 2800L,
            maxIntervalMs = 4600L,
            cues = listOf(BiomeCueKind.CAMPFIRE_CRACKLE, BiomeCueKind.VILLAGE_CHORD, BiomeCueKind.LEAF_RUSTLE)
        ),
        WorldZone.BOSQUE_ESMERALDA to BiomeAudioProfile(
            zone = WorldZone.BOSQUE_ESMERALDA,
            atmosphereName = "Bosque Vivo y Hojas",
            minIntervalMs = 2200L,
            maxIntervalMs = 3900L,
            cues = listOf(BiomeCueKind.BIRD_CHIRP, BiomeCueKind.LEAF_RUSTLE, BiomeCueKind.WOODLAND_CHIME)
        ),
        WorldZone.DESIERTO_DORADO to BiomeAudioProfile(
            zone = WorldZone.DESIERTO_DORADO,
            atmosphereName = "Viento de Dunas y Flauta",
            minIntervalMs = 2500L,
            maxIntervalMs = 4400L,
            cues = listOf(BiomeCueKind.SAND_GUST, BiomeCueKind.DESERT_FLUTE, BiomeCueKind.MIRAGE_BELL)
        ),
        WorldZone.CAVERNAS_MINA to BiomeAudioProfile(
            zone = WorldZone.CAVERNAS_MINA,
            atmosphereName = "Eco Subterráneo y Cristales",
            minIntervalMs = 1900L,
            maxIntervalMs = 3500L,
            cues = listOf(BiomeCueKind.WATER_DRIP, BiomeCueKind.CRYSTAL_ECHO, BiomeCueKind.DEEP_RUMBLE)
        ),
        WorldZone.BARRIO_SOMBRAVIA to BiomeAudioProfile(
            zone = WorldZone.BARRIO_SOMBRAVIA,
            atmosphereName = "Pulso Sombravía",
            minIntervalMs = 2300L,
            maxIntervalMs = 3800L,
            cues = listOf(BiomeCueKind.SHADOW_PULSE, BiomeCueKind.NEON_SPARK, BiomeCueKind.DEEP_RUMBLE)
        )
    )

    fun profileForZone(zone: WorldZone): BiomeAudioProfile =
        profiles[zone] ?: profiles.getValue(WorldZone.BOSQUE_ESMERALDA)

    fun setZone(zone: WorldZone) {
        activeZone = zone
    }

    fun start() {
        if (ambientJob?.isActive == true) return
        ambientJob = scope.launch {
            var tick = 0L
            while (isActive) {
                val zone = activeZone
                val profile = profileForZone(zone)
                val waitMs = Random.nextLong(profile.minIntervalMs, profile.maxIntervalMs + 1L)
                delay(waitMs)
                if (!isPaused && ambientEnabled && RetroSoundEngine.soundEnabled && RetroSoundEngine.volume > 0.02f) {
                    val cue = buildProceduralCue(zone, System.currentTimeMillis() + tick++)
                    playSynthesizedCue(cue)
                }
            }
        }
    }

    fun stop() {
        ambientJob?.cancel()
        ambientJob = null
    }

    fun buildProceduralCue(zone: WorldZone, seed: Long): BiomeAudioCue {
        val rng = Random(seed)
        val profile = profileForZone(zone)
        val kind = profile.cues[rng.nextInt(profile.cues.size)]
        return when (kind) {
            BiomeCueKind.BIRD_CHIRP -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Trino del Bosque",
                durationMs = rng.nextInt(260, 420),
                baseFreqHz = rng.nextDouble(1380.0, 2150.0)
            )
            BiomeCueKind.LEAF_RUSTLE -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Brisa entre Hojas",
                durationMs = rng.nextInt(420, 680),
                baseFreqHz = rng.nextDouble(280.0, 460.0)
            )
            BiomeCueKind.WOODLAND_CHIME -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Armonía Esmeralda",
                durationMs = 520,
                baseFreqHz = listOf(523.25, 587.33, 659.25, 783.99, 880.0)[rng.nextInt(5)]
            )
            BiomeCueKind.SAND_GUST -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Ráfaga del Desierto",
                durationMs = rng.nextInt(580, 900),
                baseFreqHz = rng.nextDouble(160.0, 290.0)
            )
            BiomeCueKind.DESERT_FLUTE -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Flauta de Dunas",
                durationMs = 640,
                baseFreqHz = listOf(293.66, 311.13, 369.99, 440.0)[rng.nextInt(4)]
            )
            BiomeCueKind.MIRAGE_BELL -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Eco de Espejismo",
                durationMs = 560,
                baseFreqHz = listOf(587.33, 622.25, 739.99)[rng.nextInt(3)]
            )
            BiomeCueKind.WATER_DRIP -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Gota de Caverna",
                durationMs = 460,
                baseFreqHz = rng.nextDouble(720.0, 1080.0)
            )
            BiomeCueKind.CRYSTAL_ECHO -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Resonancia Cristal",
                durationMs = 680,
                baseFreqHz = listOf(659.25, 783.99, 987.77, 1174.66)[rng.nextInt(4)]
            )
            BiomeCueKind.DEEP_RUMBLE -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Eco Profundo",
                durationMs = 740,
                baseFreqHz = rng.nextDouble(58.0, 86.0)
            )
            BiomeCueKind.SHADOW_PULSE -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Pulso Sombravía",
                durationMs = 700,
                baseFreqHz = listOf(110.0, 130.81, 146.83)[rng.nextInt(3)]
            )
            BiomeCueKind.NEON_SPARK -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Chispa Eléctrica",
                durationMs = 300,
                baseFreqHz = rng.nextDouble(860.0, 1340.0)
            )
            BiomeCueKind.CAMPFIRE_CRACKLE -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Crepitar de Fogata",
                durationMs = 360,
                baseFreqHz = rng.nextDouble(340.0, 620.0)
            )
            BiomeCueKind.VILLAGE_CHORD -> BiomeAudioCue(
                zone = zone,
                kind = kind,
                label = "Acorde de Aldea",
                durationMs = 620,
                baseFreqHz = listOf(329.63, 392.0, 440.0)[rng.nextInt(3)]
            )
        }
    }

    fun synthesizePcmSamples(cue: BiomeAudioCue, sampleRate: Int = 22050): ShortArray {
        val totalSamples = ((sampleRate * cue.durationMs) / 1000).coerceAtLeast(256)
        val out = ShortArray(totalSamples)
        val gain = (Short.MAX_VALUE * 0.14f * ambientVolume.coerceIn(0f, 1f) * RetroSoundEngine.volume.coerceIn(0f, 1f))
        val rng = Random(cue.kind.ordinal * 997L + cue.baseFreqHz.toLong())
        var phaseA = 0.0
        var phaseB = 0.0
        var filteredNoise = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val sec = i.toDouble() / sampleRate
            val sampleVal = when (cue.kind) {
                BiomeCueKind.BIRD_CHIRP -> {
                    // Two quick upward melodic chirps
                    val localT = (t * 2.0) % 1.0
                    val f = cue.baseFreqHz * (0.85 + 0.45 * sin(localT * PI))
                    phaseA += 2.0 * PI * f / sampleRate
                    val env = sin(localT * PI) * (1.0 - t * 0.25)
                    sin(phaseA) * env * 0.85
                }
                BiomeCueKind.LEAF_RUSTLE, BiomeCueKind.SAND_GUST -> {
                    // Smoothly swept filtered breeze noise
                    val raw = rng.nextDouble(-1.0, 1.0)
                    val alpha = if (cue.kind == BiomeCueKind.SAND_GUST) 0.045 else 0.09
                    filteredNoise += (raw - filteredNoise) * alpha
                    phaseA += 2.0 * PI * cue.baseFreqHz / sampleRate
                    val env = sin(t * PI)
                    (filteredNoise * 0.85 + sin(phaseA) * 0.15) * env
                }
                BiomeCueKind.WOODLAND_CHIME, BiomeCueKind.CRYSTAL_ECHO, BiomeCueKind.MIRAGE_BELL, BiomeCueKind.VILLAGE_CHORD -> {
                    // Soft harmonic bell/lute chime with shimmering fifth/octave overtone
                    phaseA += 2.0 * PI * cue.baseFreqHz / sampleRate
                    phaseB += 2.0 * PI * (cue.baseFreqHz * 1.498) / sampleRate
                    val attack = (t / 0.06).coerceAtMost(1.0)
                    val decay = exp(-t * 4.2)
                    (sin(phaseA) * 0.72 + sin(phaseB) * 0.28) * attack * decay
                }
                BiomeCueKind.DESERT_FLUTE -> {
                    // Breathy flute pitch glide across Hijaz interval
                    val glide = if (t < 0.5) 1.0 else 1.06
                    phaseA += 2.0 * PI * (cue.baseFreqHz * glide) / sampleRate
                    val vibrato = 1.0 + 0.012 * sin(2.0 * PI * 5.5 * sec)
                    val env = sin(t * PI)
                    sin(phaseA * vibrato) * 0.75 * env
                }
                BiomeCueKind.WATER_DRIP -> {
                    // Resonant water plink followed by quieter cave echo
                    val echoIdx = (t * 2.5).toInt()
                    val subT = (t * 2.5) % 1.0
                    val echoScale = when (echoIdx) {
                        0 -> 1.0
                        1 -> 0.48
                        else -> 0.22
                    }
                    val f = cue.baseFreqHz * (0.78 + 0.55 * subT)
                    phaseA += 2.0 * PI * f / sampleRate
                    sin(phaseA) * exp(-subT * 7.5) * echoScale
                }
                BiomeCueKind.DEEP_RUMBLE, BiomeCueKind.SHADOW_PULSE -> {
                    phaseA += 2.0 * PI * cue.baseFreqHz / sampleRate
                    phaseB += 2.0 * PI * (cue.baseFreqHz * 1.5) / sampleRate
                    val env = sin(t * PI)
                    (sin(phaseA) * 0.75 + sin(phaseB) * 0.25) * env
                }
                BiomeCueKind.NEON_SPARK, BiomeCueKind.CAMPFIRE_CRACKLE -> {
                    val pop = if (rng.nextFloat() < 0.08f) rng.nextDouble(-1.0, 1.0) else 0.0
                    phaseA += 2.0 * PI * cue.baseFreqHz / sampleRate
                    val decay = exp(-t * 6.0)
                    (pop * 0.65 + sin(phaseA) * 0.35) * decay
                }
            }
            out[i] = (sampleVal * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun playSynthesizedCue(cue: BiomeAudioCue) {
        try {
            val sampleRate = 22050
            val buffer = synthesizePcmSamples(cue, sampleRate)
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, buffer.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            scope.launch {
                delay(cue.durationMs + 60L)
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
            // Safe no-op on unit test / headless JVM
        }
    }
}
