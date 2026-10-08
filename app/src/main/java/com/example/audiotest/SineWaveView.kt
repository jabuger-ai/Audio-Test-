package com.example.audiotest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

class SineWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#00E5FF") // Cyan wave
        pathEffect = CornerPathEffect(10f)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#33FFFFFF")
    }

    private val wavePath = Path()
    private val fillPath = Path()

    var isSynthesizing: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var currentFrequency: Float = 440f
        set(value) {
            field = value
            if (isSynthesizing) {
                invalidate()
            }
        }

    var volume: Float = 0.5f
        set(value) {
            field = value.coerceIn(0.05f, 1.0f)
            if (isSynthesizing) {
                invalidate()
            }
        }

    private var animPhase: Float = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val fillGradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.parseColor("#4D00E5FF"),
            Color.parseColor("#00000000"),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = fillGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f

        // Draw center baseline
        canvas.drawLine(0f, centerY, width, centerY, centerLinePaint)

        if (!isSynthesizing) {
            // Draw a flat line when audio is off
            canvas.drawLine(0f, centerY, width, centerY, wavePaint)
            return
        }

        // Advance animation phase for continuous scrolling wave motion
        animPhase += 0.15f
        val twoPi = (PI * 2).toFloat()
        if (animPhase > twoPi) {
            animPhase -= twoPi
        }

        wavePath.reset()
        fillPath.reset()

        fillPath.moveTo(0f, height)
        wavePath.moveTo(0f, centerY)

        val amplitude = (centerY * 0.7f * volume).coerceAtLeast(10f)
        // Map frequency to number of visible cycles across the view
        val normalizedCycles = (currentFrequency / 150f).coerceIn(1.5f, 12f)
        val step = 4f

        var x = 0f
        var first = true

        while (x <= width) {
            val progress = x / width
            val angle = (progress * normalizedCycles * PI * 2) + animPhase
            val y = centerY + (sin(angle) * amplitude).toFloat()

            if (first) {
                wavePath.moveTo(x, y)
                fillPath.lineTo(x, y)
                first = false
            } else {
                wavePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            x += step
        }

        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(wavePath, wavePaint)

        // Continuously request redraw while synthesizing for smooth wave animation
        postInvalidateOnAnimation()
    }
}
