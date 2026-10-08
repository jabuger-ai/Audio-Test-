package com.example.audiotest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

class SynthesizerEngine(
    private val onFrequencyUpdate: ((Float) -> Unit)? = null
) {
    companion object {
        const val SAMPLE_RATE = 44100
        private const val TWO_PI = 2.0 * PI
    }

    @Volatile
    var centerFrequency: Float = 440.0f

    @Volatile
    var modulationDepth: Float = 150.0f

    @Volatile
    var modulationRate: Float = 1.0f

    @Volatile
    var volume: Float = 0.5f

    private val isRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            audioThread = Thread { runAudioLoop() }.apply {
                name = "AudioSynthesizerThread"
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            audioThread?.join(1000)
            audioThread = null
        }
    }

    val isSynthesizing: Boolean
        get() = isRunning.get()

    private fun runAudioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(1024)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        val pcmBuffer = ShortArray(bufferSize / 2)
        var carrierPhase = 0.0
        var lfoPhase = 0.0

        var updateCounter = 0
        val updateIntervalSamples = SAMPLE_RATE / 30 // Update UI ~30 times per second

        while (isRunning.get()) {
            val currentCenter = centerFrequency
            val currentDepth = modulationDepth
            val currentLfoRate = modulationRate
            val currentVol = volume

            val lfoStep = (TWO_PI * currentLfoRate) / SAMPLE_RATE

            for (i in pcmBuffer.indices) {
                // Low Frequency Oscillator (sine wave modulation)
                val lfoValue = sin(lfoPhase).toFloat()
                val instantaneousFreq = (currentCenter + currentDepth * lfoValue).coerceAtLeast(10.0f)

                // Advance carrier phase based on instantaneous frequency
                val carrierStep = (TWO_PI * instantaneousFreq) / SAMPLE_RATE
                carrierPhase += carrierStep
                if (carrierPhase >= TWO_PI) {
                    carrierPhase -= TWO_PI
                }

                lfoPhase += lfoStep
                if (lfoPhase >= TWO_PI) {
                    lfoPhase -= TWO_PI
                }

                // Calculate sine sample
                val sampleValue = (sin(carrierPhase) * currentVol * Short.MAX_VALUE).toInt()
                pcmBuffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                updateCounter++
                if (updateCounter >= updateIntervalSamples) {
                    updateCounter = 0
                    onFrequencyUpdate?.invoke(instantaneousFreq)
                }
            }

            track.write(pcmBuffer, 0, pcmBuffer.size)
        }

        try {
            track.stop()
            track.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
