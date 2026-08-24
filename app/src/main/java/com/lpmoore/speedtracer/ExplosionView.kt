package com.lpmoore.speedtracer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Transparent overlay that renders a particle explosion at a given point.
 * Explosion scale is driven by score (750–1000).
 *
 * Usage:
 *   explosionView.explode(cx, cy, score)
 */
class ExplosionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        val angle: Float,       // radians
        val speed: Float,       // px/unit-time (scaled by progress)
        val maxRadius: Float,   // px, particle dot size
        val colorIndex: Int     // 0=core 1=mid 2=outer
    )

    private val density = resources.displayMetrics.density

    private val colors = intArrayOf(
        ContextCompat.getColor(context, R.color.explosion_core),
        ContextCompat.getColor(context, R.color.explosion_mid),
        ContextCompat.getColor(context, R.color.explosion_outer)
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var particles: List<Particle> = emptyList()
    private var progress = 0f   // 0..1 driven by animator
    private var originX = 0f
    private var originY = 0f
    private var animator: ValueAnimator? = null

    /**
     * @param score 750–1000. Controls particle count, speed, and size.
     *              Clamped; scores below 750 produce no explosion.
     */
    fun explode(cx: Float, cy: Float, score: Int) {
        if (score < 750) { visibility = GONE; return }

        animator?.cancel()
        originX = cx
        originY = cy

        // Scale factor: 0.0 at 750, 1.0 at 1000. Bumps every 50 pts.
        val tier = ((score - 750) / 50).coerceIn(0, 5)   // 0..5
        val scale = (tier + 1) / 6f                        // 1/6 .. 1.0

        val particleCount = (30 + 50 * scale).toInt()
        val maxSpeed = (200f + 400f * scale) * density
        val maxDotRadius = (4f + 8f * scale) * density
        val durationMs = (600L + (600L * scale).toLong())

        particles = List(particleCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = maxSpeed * (0.4f + 0.6f * Random.nextFloat())
            val dotRadius = maxDotRadius * (0.3f + 0.7f * Random.nextFloat())
            val colorIndex = when {
                dotRadius > maxDotRadius * 0.7f -> 0  // core (bright)
                dotRadius > maxDotRadius * 0.4f -> 1  // mid
                else -> 2                              // outer
            }
            Particle(angle, speed, dotRadius, colorIndex)
        }

        progress = 0f
        visibility = VISIBLE

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            // Hide the view once animation ends
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    visibility = GONE
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (particles.isEmpty()) return
        val fade = (1f - progress).coerceIn(0f, 1f)   // particles fade out

        for (p in particles) {
            val dist = p.speed * progress
            val x = originX + cos(p.angle) * dist
            val y = originY + sin(p.angle) * dist
            paint.color = colors[p.colorIndex]
            paint.alpha = (255 * fade * fade).toInt()   // quadratic fade
            canvas.drawCircle(x, y, p.maxRadius * (1f - progress * 0.5f), paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
