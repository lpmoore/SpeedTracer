package com.lpmoore.speedtracer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ScorerTest {
    private fun ring(cx: Float, cy: Float, r: Float, n: Int = 200): FloatArray {
        val a = FloatArray(n * 2)
        for (i in 0 until n) {
            val t = 2 * Math.PI * i / n
            a[2 * i] = cx + (r * cos(t)).toFloat()
            a[2 * i + 1] = cy + (r * sin(t)).toFloat()
        }
        return a
    }

    @Test fun perfectFastTraceScoresHigh() {
        val pts = ring(200f, 200f, 100f)
        val r = Scorer.score(pts, pts.size, 200f, 200f, 100f, 45f, 500, false)
        assertEquals(100, r.accuracyPct); assertEquals(100, r.coveragePct)
        assertTrue(r.score > 900)
    }

    @Test fun halfCircleHalfCoverage() {
        val pts = ring(200f, 200f, 100f, 200).copyOf(200)
        val r = Scorer.score(pts, 200, 200f, 200f, 100f, 45f, 3000, false)
        assertEquals(50, r.coveragePct)
    }

    @Test fun tooFewPointsScoresZero() {
        assertEquals(0, Scorer.score(FloatArray(2), 2, 0f, 0f, 10f, 10f, 0, true).score)
    }

    @Test fun explosionTierMatchesPlanBands() {
        assertEquals(0, ExplosionView.tierForScore(500))
        assertEquals(1, ExplosionView.tierForScore(600))
        assertEquals(2, ExplosionView.tierForScore(700))
        assertEquals(3, ExplosionView.tierForScore(800))
        assertEquals(4, ExplosionView.tierForScore(900))
        assertEquals(5, ExplosionView.tierForScore(1000))
        assertEquals(0, ExplosionView.tierForScore(499))
    }
}
