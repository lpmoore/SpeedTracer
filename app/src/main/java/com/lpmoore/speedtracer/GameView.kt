package com.lpmoore.speedtracer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.random.Random

class GameView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    enum class State { IDLE, READY, TRACING, FINISHED }

    interface Listener {
        fun onTick(remainingMs: Long)
        fun onRoundFinished(result: RoundResult)
    }

    var listener: Listener? = null
    var state = State.IDLE
        private set

    private val density = resources.displayMetrics.density
    private val minRadius = 5f * density      // Step 2: 5dp .. 100dp
    private val maxRadius = 100f * density
    private val dotDurationMs = 700L

    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private var tolerance = 0f

    // Interleaved x,y trace points; grown on demand to avoid per-frame allocation.
    private var points = FloatArray(2048)
    private var pointCount = 0

    private var roundStart = 0L   // when the circle appeared
    private var traceStart = 0L   // first touch; countdown starts here
    private var lastBeepSecond = -1
    private var tone: ToneGenerator? = null

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = ContextCompat.getColor(context, R.color.circle)
    }
    private val tracePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.trace)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dot)
    }

    private val frame = object : Runnable {
        override fun run() {
            if (state == State.TRACING) {
                val elapsed = SystemClock.uptimeMillis() - traceStart
                val remaining = Scorer.TIME_LIMIT_MS - elapsed
                listener?.onTick(max(0L, remaining))
                beepIfNewSecond(remaining)
                if (remaining <= 0) { finish(interrupted = false); return }
            }
            invalidate()
            postOnAnimation(this)
        }
    }

    fun startRound() {
        radius = minRadius + Random.nextFloat() * (maxRadius - minRadius)
        tolerance = max(radius * 0.45f, 10f * density)
        val margin = radius + 32f * density
        cx = margin + Random.nextFloat() * (width - 2 * margin)
        cy = margin + Random.nextFloat() * (height - 2 * margin)
        pointCount = 0
        lastBeepSecond = -1
        roundStart = SystemClock.uptimeMillis()
        traceStart = 0L
        state = State.READY
        if (tone == null) tone = try { ToneGenerator(AudioManager.STREAM_MUSIC, 70) } catch (_: RuntimeException) { null }
        listener?.onTick(Scorer.TIME_LIMIT_MS)
        removeCallbacks(frame)
        postOnAnimation(frame)
        invalidate()
    }

    fun reset() {
        state = State.IDLE
        removeCallbacks(frame)
        invalidate()
    }

    // Step 3: touch handling
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (state == State.READY) {
                    state = State.TRACING
                    traceStart = SystemClock.uptimeMillis()
                    addPoint(event.x, event.y)
                    return true
                }
                return state == State.TRACING
            }
            MotionEvent.ACTION_MOVE -> {
                if (state != State.TRACING) return false
                // Historical points give a much denser sample than the current event alone.
                for (h in 0 until event.historySize) addPoint(event.getHistoricalX(h), event.getHistoricalY(h))
                addPoint(event.x, event.y)
                if (Scorer.coverage(points, pointCount, cx, cy, radius, tolerance) >= 0.97) {
                    finish(interrupted = false)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (state == State.TRACING) finish(interrupted = true)
                return true
            }
        }
        return false
    }

    private fun addPoint(x: Float, y: Float) {
        if (pointCount + 2 > points.size) points = points.copyOf(points.size * 2)
        points[pointCount++] = x
        points[pointCount++] = y
    }

    private fun beepIfNewSecond(remainingMs: Long) {
        val sec = ((remainingMs + 999) / 1000).toInt()   // 3,2,1
        if (sec != lastBeepSecond && sec in 1..3) {
            lastBeepSecond = sec
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        }
    }

    private fun finish(interrupted: Boolean) {
        if (state != State.TRACING) return
        state = State.FINISHED
        removeCallbacks(frame)
        val elapsed = (SystemClock.uptimeMillis() - traceStart).coerceAtMost(Scorer.TIME_LIMIT_MS)
        tone?.startTone(if (interrupted) ToneGenerator.TONE_PROP_NACK else ToneGenerator.TONE_PROP_ACK, 150)
        listener?.onTick(Scorer.TIME_LIMIT_MS - elapsed)
        invalidate()
        listener?.onRoundFinished(
            Scorer.score(points, pointCount, cx, cy, radius, tolerance, elapsed, interrupted)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (state == State.IDLE) return

        canvas.drawCircle(cx, cy, radius, circlePaint)

        // Brief red center dot that fades out after the circle appears.
        val sinceStart = SystemClock.uptimeMillis() - roundStart
        if (sinceStart < dotDurationMs) {
            dotPaint.alpha = (255 * (1f - sinceStart.toFloat() / dotDurationMs)).toInt()
            canvas.drawCircle(cx, cy, 4f * density, dotPaint)
        }

        if (pointCount >= 4) {
            // Draw as connected segments: reuse the point array as line pairs.
            for (i in 0 until pointCount - 2 step 2) {
                canvas.drawLine(points[i], points[i + 1], points[i + 2], points[i + 3], tracePaint)
            }
        } else if (pointCount == 2) {
            canvas.drawPoint(points[0], points[1], tracePaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frame)
        tone?.release(); tone = null
    }
}
