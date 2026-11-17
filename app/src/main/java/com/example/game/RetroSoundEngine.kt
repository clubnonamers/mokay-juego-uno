package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sin

object RetroSoundEngine {
    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var volume: Float = 0.7f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun playSlash() = playTone(560.0, 220.0, 75)
    fun playBow() = playTone(760.0, 340.0, 65)
    fun playJump() = playTone(260.0, 520.0, 70)
    fun playDash() = playTone(440.0, 720.0, 65)
    fun playSwap() = playTone(380.0, 880.0, 110)
    fun playSkill() = playTone(300.0, 960.0, 140)
    fun playShield() = playTone(240.0, 180.0, 80)
    fun playChop() = playTone(260.0, 140.0, 60)
    fun playMine() = playTone(480.0, 320.0, 65)
    fun playBuild() = playTone(420.0, 640.0, 85)
    fun playEatDrink() = playTone(520.0, 780.0, 95)
    fun playHit() = playTone(180.0, 85.0, 90)
    fun playQuestComplete() {
        playTone(523.25, 659.25, 110)
        scope.launch {
            kotlinx.coroutines.delay(95L)
            playTone(783.99, 1046.50, 150)
        }
    }

    fun playLevelUp() {
        playTone(440.0, 660.0, 110)
        scope.launch {
            kotlinx.coroutines.delay(100L)
            playTone(660.0, 880.0, 160)
        }
    }

    private fun playTone(startFreq: Double, endFreq: Double, durationMs: Int) {
        if (!soundEnabled || volume <= 0.02f) return
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(numSamples)
                val amp = (Short.MAX_VALUE * 0.22f * volume.coerceIn(0f, 1f)).toInt()
                var phase = 0.0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / numSamples
                    val freq = startFreq + (endFreq - startFreq) * t
                    phase += 2.0 * Math.PI * freq / sampleRate
                    val sq = if (sin(phase) >= 0.0) 1.0 else -1.0
                    val env = 1.0 - t
                    buffer[i] = (sq * amp * env).toInt().toShort()
                }
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
                kotlinx.coroutines.delay(durationMs + 40L)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Safe no-op on unit test / headless JVM without Android audio mixer
            }
        }
    }
}
