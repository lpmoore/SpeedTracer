package com.lpmoore.speedtracer

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

/**
 * A zero-asset procedural audio synthesizer that generates retro/arcade
 * sound effects using Android's AudioTrack API in a background thread.
 */
class SoundSynth {
    private val executor = Executors.newSingleThreadExecutor()
    private val sampleRate = 44100

    enum class SoundType {
        TICK, CRACK, BLAST, BOOM, RUMBLE, DETONATION, PORTAL, LEVEL_UP
    }

    fun play(type: SoundType) {
        executor.submit {
            val samples = when (type) {
                SoundType.TICK -> generateTick()
                SoundType.CRACK -> generateNoise(0.08f, 0.4f)
                SoundType.BLAST -> generateNoise(0.18f, 0.6f)
                SoundType.BOOM -> generateBoom(0.4f, 150f, 40f)
                SoundType.RUMBLE -> generateRumble(0.7f)
                SoundType.DETONATION -> generateDetonation()
                SoundType.PORTAL -> generatePortal(1.5f)
                SoundType.LEVEL_UP -> generateLevelUp()
            }
            playPcm(samples)
        }
    }

    private fun playPcm(samples: ShortArray) {
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val size = maxOf(minBuffer, samples.size * 2)
        @Suppress("DEPRECATION")
        val track = try {
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                size,
                AudioTrack.MODE_STATIC
            )
        } catch (e: Exception) {
            return
        }

        track.write(samples, 0, samples.size)
        track.play()
        // Wait for it to finish playing before releasing
        val durationMs = (samples.size * 1000L) / sampleRate
        try {
            Thread.sleep(durationMs + 100)
        } catch (_: InterruptedException) {}
        try {
            track.stop()
            track.release()
        } catch (_: Exception) {}
    }

    private fun generateTick(): ShortArray {
        val duration = 0.03f
        val len = (sampleRate * duration).toInt()
        val data = ShortArray(len)
        for (i in 0 until len) {
            val progress = i.toFloat() / len
            val vol = 1f - progress
            val phase = 2.0 * PI * 2000.0 * i / sampleRate
            data[i] = (sin(phase) * 32767 * vol * 0.15f).toInt().toShort()
        }
        return data
    }

    private fun generateNoise(duration: Float, volume: Float): ShortArray {
        val len = (sampleRate * duration).toInt()
        val data = ShortArray(len)
        for (i in 0 until len) {
            val progress = i.toFloat() / len
            val vol = (1f - progress) * volume
            val noise = Random.nextFloat() * 2f - 1f
            data[i] = (noise * 32767 * vol * 0.35f).toInt().toShort()
        }
        return data
    }

    private fun generateBoom(duration: Float, startFreq: Float, endFreq: Float): ShortArray {
        val len = (sampleRate * duration).toInt()
        val data = ShortArray(len)
        var phase = 0.0
        for (i in 0 until len) {
            val progress = i.toFloat() / len
            val vol = (1f - progress)
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / sampleRate
            val sine = sin(phase).toFloat()
            val noise = Random.nextFloat() * 2f - 1f
            val mixed = sine * 0.8f + noise * 0.2f
            data[i] = (mixed * 32767f * vol * 0.5f).toInt().toShort()
        }
        return data
    }

    private fun generateRumble(duration: Float): ShortArray {
        val len = (sampleRate * duration).toInt()
        val data = ShortArray(len)
        var noiseLP = 0f
        for (i in 0 until len) {
            val progress = i.toFloat() / len
            val mod = 0.7f + 0.3f * sin(2.0 * PI * 15.0 * i / sampleRate).toFloat()
            val vol = (1f - progress) * mod
            val noise = Random.nextFloat() * 2f - 1f
            noiseLP = noiseLP * 0.9f + noise * 0.1f
            data[i] = (noiseLP * 32767f * vol * 0.7f).toInt().toShort()
        }
        return data
    }

    private fun generateDetonation(): ShortArray {
        val duration = 1.0f
        val len = (sampleRate * duration).toInt()
        val data = ShortArray(len)
        var phaseMain = 0.0
        var phaseRing = 0.0
        var noiseLP = 0f
        for (i in 0 until len) {
            val progress = i.toFloat() / len
            val vol = 1f - progress

            val freqMain = 180f - 140f * progress.coerceAtMost(0.5f) / 0.5f
            phaseMain += 2.0 * PI * freqMain / sampleRate
            val sMain = sin(phaseMain).toFloat()

            val sRing = if (progress < 0.3f) {
                phaseRing += 2.0 * PI * 1200.0 / sampleRate
                sin(phaseRing).toFloat() * (1f - progress / 0.3f) * 0.15f
            } else 0f

            val noise = Random.nextFloat() * 2f - 1f
            noiseLP = noiseLP * 0.92f + noise * 0.08f
            val nVol = if (progress < 0.1f) 0.6f else (1f - progress) * 0.3f

            val mixed = sMain * 0.5f + sRing + noiseLP * nVol
            data[i] = (mixed * 32767f * vol * 0.6f).toInt().toShort()
        }
        return data
    }

    private fun generatePortal(duration: Float): ShortArray {
        val len = (sampleRate * duration).toInt()
        val data = ShortArray(len)
        val phases = DoubleArray(5)
        for (i in 0 until len) {
            val progress = i.toFloat() / len
            val vol = if (progress < 0.2f) progress / 0.2f else 1f - (progress - 0.2f) / 0.8f

            var mixed = 0.0
            for (h in 0 until 5) {
                val base = 150.0 * (h + 1)
                val freq = base * (1.0 + progress.toDouble() * 1.5)
                phases[h] += 2.0 * PI * freq / sampleRate
                mixed += sin(phases[h])
            }
            mixed /= 5.0
            val vibrato = 1.0 + 0.15 * sin(2.0 * PI * 8.0 * i / sampleRate)
            data[i] = ((mixed * vibrato).toFloat() * 32767f * vol * 0.4f).toInt().toShort()
        }
        return data
    }

    private fun generateLevelUp(): ShortArray {
        val duration = 0.8f
        val len = (sampleRate * duration).toInt()
        val data = ShortArray(len)
        val noteDur = len / 4
        var phase = 0.0
        for (i in 0 until len) {
            val noteIndex = i / noteDur
            val noteFreq = when (noteIndex) {
                0 -> 523.25  // C5
                1 -> 659.25  // E5
                2 -> 783.99  // G5
                else -> 1046.50 // C6
            }
            val progressInNote = (i % noteDur).toFloat() / noteDur
            val noteVol = 1f - progressInNote * 0.3f
            phase += 2.0 * PI * noteFreq / sampleRate
            data[i] = (sin(phase).toFloat() * 32767f * noteVol * 0.35f).toInt().toShort()
        }
        return data
    }

    fun release() {
        executor.shutdown()
    }
}
