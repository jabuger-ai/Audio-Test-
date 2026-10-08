package com.example.audiotest

class NativeSynthesizerEngine(
    private var onFrequencyUpdate: ((Float) -> Unit)? = null,
) {
    companion object {
        init {
            System.loadLibrary("audio_synth")
        }
    }

    private var nativeEnginePtr: Long = 0L

    init {
        nativeEnginePtr = nativeCreate()
    }

    // Called from C++ JNI
    @Suppress("unused")
    fun onFrequencyUpdateFromNative(freq: Float) {
        onFrequencyUpdate?.invoke(freq)
    }

    fun start(): Boolean {
        return if (nativeEnginePtr != 0L) {
            nativeStart(nativeEnginePtr)
        } else false
    }

    fun stop() {
        if (nativeEnginePtr != 0L) {
            nativeStop(nativeEnginePtr)
        }
    }

    val isSynthesizing: Boolean
        get() = (nativeEnginePtr != 0L) && nativeIsSynthesizing(nativeEnginePtr)

    var centerFrequency: Float = 440f
        set(value) {
            field = value
            if (nativeEnginePtr != 0L) {
                nativeSetCenterFrequency(nativeEnginePtr, value)
            }
        }

    var modulationDepth: Float = 150f
        set(value) {
            field = value
            if (nativeEnginePtr != 0L) {
                nativeSetModulationDepth(nativeEnginePtr, value)
            }
        }

    var modulationRate: Float = 1f
        set(value) {
            field = value
            if (nativeEnginePtr != 0L) {
                nativeSetModulationRate(nativeEnginePtr, value)
            }
        }

    var volume: Float = 0.5f
        set(value) {
            field = value
            if (nativeEnginePtr != 0L) {
                nativeSetVolume(nativeEnginePtr, value)
            }
        }

    fun destroy() {
        if (nativeEnginePtr != 0L) {
            nativeDestroy(nativeEnginePtr)
            nativeEnginePtr = 0L
        }
    }

    // JNI Declarations
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(ptr: Long)
    private external fun nativeStart(ptr: Long): Boolean
    private external fun nativeStop(ptr: Long)
    private external fun nativeSetCenterFrequency(ptr: Long, freq: Float)
    private external fun nativeSetModulationDepth(ptr: Long, depth: Float)
    private external fun nativeSetModulationRate(ptr: Long, rate: Float)
    private external fun nativeSetVolume(ptr: Long, volume: Float)
    private external fun nativeIsSynthesizing(ptr: Long): Boolean
}
