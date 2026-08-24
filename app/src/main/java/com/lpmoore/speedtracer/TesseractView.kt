package com.lpmoore.speedtracer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

class TesseractView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    // 16 vertices of a 4D hypercube
    private val vertices = Array(16) { i ->
        floatArrayOf(
            if ((i and 1) != 0) 1f else -1f,
            if ((i and 2) != 0) 1f else -1f,
            if ((i and 4) != 0) 1f else -1f,
            if ((i and 8) != 0) 1f else -1f
        )
    }

    // 32 edges of a tesseract
    private val edges = ArrayList<Pair<Int, Int>>().apply {
        for (i in 0 until 16) {
            for (bit in 0 until 4) {
                val j = i xor (1 shl bit)
                if (i < j) add(i to j)
            }
        }
    }

    private val bgPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF1A1A24")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF1A1A24")
        textAlign = Paint.Align.CENTER
    }

    private var active = false
    private var progress = 0f  // background fade-in progress (0..1)
    private var rotAngle = 0f  // base rotation angle driven by animator

    private var rotationAnimator: ValueAnimator? = null
    private var fadeAnimator: ValueAnimator? = null
    private var onDismissListener: (() -> Unit)? = null

    init {
        visibility = GONE
    }

    fun show(onDismiss: () -> Unit) {
        active = true
        onDismissListener = onDismiss
        visibility = VISIBLE

        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        rotationAnimator?.cancel()
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 10000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotAngle = Math.toRadians((it.animatedValue as Float).toDouble()).toFloat()
                invalidate()
            }
            start()
        }
    }

    fun dismiss() {
        if (!active) return
        active = false
        rotationAnimator?.cancel()
        fadeAnimator?.cancel()

        fadeAnimator = ValueAnimator.ofFloat(progress, 0f).apply {
            duration = 500
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
addListener(object : android.animation.AnimatorListenerAdapter() {
    override fun onAnimationEnd(animation: android.animation.Animator) {
        visibility = GONE
        onDismissListener?.invoke()
        onDismissListener = null
    }
})
            })
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!active || progress < 0.8f) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            dismiss()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        if (progress <= 0f) return

        // 1. Draw white background fade-in
        bgPaint.alpha = (progress * 255).toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cx = width / 2f
        val cy = height / 2f
        val scale = min(width, height) * 0.28f * progress

        // 2. Project vertices
        // We will rotate in XW (4D plane), YZ (3D/4D plane), and XZ planes to create rich motion.
        val axw = rotAngle
        val ayz = rotAngle * 0.7f
        val axz = rotAngle * 0.4f

        val cosXW = cos(axw)
        val sinXW = sin(axw)
        val cosYZ = cos(ayz)
        val sinYZ = sin(ayz)
        val cosXZ = cos(axz)
        val sinXZ = sin(axz)

        val projected = Array(16) { floatArrayOf(0f, 0f, 0f, 0f) } // (screenX, screenY, depth_z, depth_w)

        for (i in 0 until 16) {
            val v = vertices[i]
            val x = v[0]
            val y = v[1]
            val z = v[2]
            val w = v[3]

            // Rotate XW
            val x1 = x * cosXW - w * sinXW
            val w1 = x * sinXW + w * cosXW

            // Rotate YZ
            val y1 = y * cosYZ - z * sinYZ
            val z1 = y * sinYZ + z * cosYZ

            // Rotate XZ
            val x2 = x1 * cosXZ - z1 * sinXZ
            val z2 = x1 * sinXZ + z1 * cosXZ

            // Perspective 4D to 3D (W projection)
            // Distance constant D_w = 2.0 (since coords are -1..1, D_w - w1 is 1.0..3.0)
            val dw = 2.0f
            val x3d = x2 / (dw - w1)
            val y3d = y1 / (dw - w1)
            val z3d = z2 / (dw - w1)

            // Perspective 3D to 2D (Z projection)
            // Distance constant D_z = 2.0
            val dz = 2.0f
            val px = x3d / (dz - z3d)
            val py = y3d / (dz - z3d)

            projected[i][0] = cx + px * scale
            projected[i][1] = cy + py * scale
            projected[i][2] = z3d
            projected[i][3] = w1
        }

        // 3. Draw edges with depth sorting / styling
        // Draw edges with alpha/stroke width proportional to the average depth of their endpoints
        val paintAlpha = (progress * 255).toInt()
        for (edge in edges) {
            val p1 = projected[edge.first]
            val p2 = projected[edge.second]

            // Average depth from z and w perspective
            // Depth ranges from approximately -1.0 to 1.0
            val avgZ = (p1[2] + p2[2]) / 2f
            val avgW = (p1[3] + p2[3]) / 2f

            // Map avgZ (-1 to 1) and avgW (-1 to 1) to thickness
            // Front edges are thicker, back edges are thinner
            val depthFactor = ((avgZ + 1f) / 2f + (avgW + 1f) / 2f) / 2f // 0..1 range
            val strokeWidth = (1.5f + 4.5f * depthFactor) * density
            val alphaFactor = 0.3f + 0.7f * depthFactor

            linePaint.strokeWidth = strokeWidth
            linePaint.alpha = (paintAlpha * alphaFactor).toInt()

            canvas.drawLine(p1[0], p1[1], p2[0], p2[1], linePaint)
        }

        // 4. Draw Easter Egg text
        textPaint.alpha = (progress * 255).toInt()

        textPaint.textSize = 36f.sp()
        canvas.drawText("PERFECT SCORE", cx, cy - scale - 60f * density, textPaint)

        textPaint.textSize = 18f.sp()
        canvas.drawText("Tesseract Boss Defeated", cx, cy - scale - 30f * density, textPaint)

        textPaint.textSize = 14f.sp()
        textPaint.alpha = ((0.5f + 0.3f * sin(rotAngle * 4.0).toFloat()) * progress * 255).toInt()
        canvas.drawText("Tap anywhere to return", cx, cy + scale + 80f * density, textPaint)
    }

private fun Float.sp(): Float = this * resources.displayMetrics.scaledDensity

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rotationAnimator?.cancel()
        fadeAnimator?.cancel()
    }
}
