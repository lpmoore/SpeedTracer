package com.lpmoore.speedtracer

import android.content.Context
import android.content.SharedPreferences
import java.lang.reflect.Proxy
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

    @Test fun scoreStoreLevelPersistenceAndPromotion() {
        val prefsMap = mutableMapOf<String, Any?>()

        val editorProxy = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "putString" -> { prefsMap[args[0] as String] = args[1]; proxy }
                "putInt" -> { prefsMap[args[0] as String] = args[1]; proxy }
                "apply" -> Unit
                else -> null
            }
        } as SharedPreferences.Editor

        val prefsProxy = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getString" -> prefsMap[args[0] as String] as? String ?: args[1] as? String
                "getInt" -> prefsMap[args[0] as String] as? Int ?: args[1] as? Int
                "edit" -> editorProxy
                else -> null
            }
        } as SharedPreferences

        val store = ScoreStore(prefsProxy)

        // Default level is 1
        assertEquals(1, store.getLevel())

        // Set level to 2 and check persistence
        store.setLevel(2)
        assertEquals(2, store.getLevel())

        // Check promotion check behavior on Level 2 (requires 6 out of 10 >= 750)
        assertEquals(false, store.checkPromotion(2))

        // Add 6 passing scores
        for (i in 1..6) {
            store.add(RoundResult(800, 95, 95, 1200L, false), 2)
        }
        assertEquals(true, store.checkPromotion(2))

        // Check that Level 1 is unaffected and is still false
        assertEquals(false, store.checkPromotion(1))
    }
}
