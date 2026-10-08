package com.example.audiotest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SynthesizerEngineTest {

    @Test
    fun testDefaultParameters() {
        val engine = SynthesizerEngine()
        assertEquals(440.0f, engine.centerFrequency, 0.001f)
        assertEquals(150.0f, engine.modulationDepth, 0.001f)
        assertEquals(1.0f, engine.modulationRate, 0.001f)
        assertEquals(0.5f, engine.volume, 0.001f)
        assertFalse(engine.isSynthesizing)
    }

    @Test
    fun testInstantaneousFrequencyCalculation() {
        val center = 440.0f
        val depth = 150.0f
        
        // At LFO phase = 0, sin(0) = 0 -> freq = 440 Hz
        val freqAtPhase0 = center + depth * sin(0.0).toFloat()
        assertEquals(440.0f, freqAtPhase0, 0.001f)

        // At LFO phase = PI / 2, sin(PI/2) = 1 -> freq = 590 Hz
        val freqAtPhasePeak = center + depth * sin(PI / 2).toFloat()
        assertEquals(590.0f, freqAtPhasePeak, 0.001f)

        // At LFO phase = 3 * PI / 2, sin(3*PI/2) = -1 -> freq = 290 Hz
        val freqAtPhaseTrough = center + depth * sin(3 * PI / 2).toFloat()
        assertEquals(290.0f, freqAtPhaseTrough, 0.001f)
    }

    @Test
    fun testParameterUpdates() {
        val engine = SynthesizerEngine()
        engine.centerFrequency = 500.0f
        engine.modulationDepth = 200.0f
        engine.modulationRate = 2.5f
        engine.volume = 0.8f

        assertEquals(500.0f, engine.centerFrequency, 0.001f)
        assertEquals(200.0f, engine.modulationDepth, 0.001f)
        assertEquals(2.5f, engine.modulationRate, 0.001f)
        assertEquals(0.8f, engine.volume, 0.001f)
    }
}
