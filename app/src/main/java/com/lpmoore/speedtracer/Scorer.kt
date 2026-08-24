package com.lpmoore.speedtracer

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class RoundResult(
    val score: Int,
    val accuracyPct: Int,
    val coveragePct: Int,
    val timeMs: Long,
    val interrupted: Boolean
)

/** Pure scoring; no Android deps so it is unit-testable. */
object Scorer {
    const val TIME_LIMIT_MS = 3000L
    const val SECTORS = 36

    /** points: x,y interleaved. tolerance: px distance from the edge that still counts as "on". */
    fun score(
        points: FloatArray, count: Int,
        cx: Float, cy: Float, radius: Float, tolerance: Float,
        elapsedMs: Long, interrupted: Boolean
    ): RoundResult {
        val n = count / 2
        if (n < 2) return RoundResult(0, 0, 0, elapsedMs, interrupted)

        var devSum = 0.0
        val covered = BooleanArray(SECTORS)
        for (i in 0 until n) {
            val dx = points[2 * i] - cx
            val dy = points[2 * i + 1] - cy
            val dev = abs(hypot(dx, dy) - radius)
            devSum += dev
            if (dev <= tolerance) {
                val ang = atan2(dy, dx) + Math.PI            // 0..2π
                val sector = ((ang / (2 * Math.PI)) * SECTORS).toInt().coerceIn(0, SECTORS - 1)
                covered[sector] = true
            }
        }
        val meanDev = devSum / n
        val accuracy = (1.0 - meanDev / tolerance).coerceIn(0.0, 1.0)
        val coverage = covered.count { it }.toDouble() / SECTORS
        // Faster = better, but never worth less than half.
        val speed = 1.0 - 0.5 * (elapsedMs.toDouble() / TIME_LIMIT_MS).coerceIn(0.0, 1.0)

        val score = (1000 * accuracy * coverage * speed).roundToInt()
        return RoundResult(
            score = score,
            accuracyPct = (accuracy * 100).roundToInt(),
            coveragePct = (coverage * 100).roundToInt(),
            timeMs = elapsedMs,
            interrupted = interrupted
        )
    }

    /** Coverage-only check used to end a round early once a full loop is drawn. */
    fun coverage(points: FloatArray, count: Int, cx: Float, cy: Float, radius: Float, tolerance: Float): Double {
        val covered = BooleanArray(SECTORS)
        for (i in 0 until count / 2) {
            val dx = points[2 * i] - cx
            val dy = points[2 * i + 1] - cy
            if (abs(hypot(dx, dy) - radius) <= tolerance) {
                val ang = atan2(dy, dx) + Math.PI
                covered[((ang / (2 * Math.PI)) * SECTORS).toInt().coerceIn(0, SECTORS - 1)] = true
            }
        }
        return covered.count { it }.toDouble() / SECTORS
    }
}
