package com.BloodSouls.Souls

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import java.io.ByteArrayOutputStream
import kotlin.math.sin

object SoundEffects {
    private var attackSound: Sound? = null
    private var hitSound: Sound? = null
    private var victorySound: Sound? = null
    private var gameOverSound: Sound? = null

    init {
        try {
            attackSound = createSynthesizedSound(frequency = 320f, durationMs = 120, noise = true)
            hitSound = createSynthesizedSound(frequency = 160f, durationMs = 200, noise = false)
            victorySound = createSynthesizedSound(frequency = 523.25f, durationMs = 450, noise = false)
            gameOverSound = createSynthesizedSound(frequency = 110f, durationMs = 600, noise = false)
        } catch (e: Exception) {
            Gdx.app.error("SoundEffects", "Audio synthesis error: ${e.message}")
        }
    }

    fun playAttack() {
        try { attackSound?.play(0.6f) } catch (_: Exception) {}
    }

    fun playHit() {
        try { hitSound?.play(0.8f) } catch (_: Exception) {}
    }

    fun playVictory() {
        try { victorySound?.play(0.8f) } catch (_: Exception) {}
    }

    fun playGameOver() {
        try { gameOverSound?.play(0.8f) } catch (_: Exception) {}
    }

    private fun createSynthesizedSound(frequency: Float, durationMs: Int, noise: Boolean): Sound? {
        val sampleRate = 22050
        val numSamples = (sampleRate * durationMs / 1000)
        val pcmData = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = (1.0 - (i.toDouble() / numSamples))
            val wave = if (noise) {
                (Math.random() * 2.0 - 1.0) * envelope
            } else {
                sin(2.0 * Math.PI * frequency * t) * envelope
            }
            val sampleVal = (wave * 32767).toInt().coerceIn(-32768, 32767).toShort()
            pcmData[i * 2] = (sampleVal.toInt() and 0xFF).toByte()
            pcmData[i * 2 + 1] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }

        val wavHeader = createWavHeader(pcmData.size, sampleRate, 1, 16)
        val fullWav = wavHeader + pcmData

        val tempFile = FileHandle.tempFile("synth_sound_${frequency.toInt()}_$durationMs.wav")
        tempFile.writeBytes(fullWav, false)
        val sound = Gdx.audio.newSound(tempFile)
        tempFile.delete()
        return sound
    }

    private fun createWavHeader(dataLen: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalLen = dataLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray())
        out.write(intToBytes(totalLen))
        out.write("WAVEfmt ".toByteArray())
        out.write(intToBytes(16))
        out.write(shortToBytes(1.toShort()))
        out.write(shortToBytes(channels.toShort()))
        out.write(intToBytes(sampleRate))
        out.write(intToBytes(byteRate))
        out.write(shortToBytes(blockAlign.toShort()))
        out.write(shortToBytes(bitsPerSample.toShort()))
        out.write("data".toByteArray())
        out.write(intToBytes(dataLen))
        return out.toByteArray()
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToBytes(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    fun dispose() {
        attackSound?.dispose()
        hitSound?.dispose()
        victorySound?.dispose()
        gameOverSound?.dispose()
    }
}
