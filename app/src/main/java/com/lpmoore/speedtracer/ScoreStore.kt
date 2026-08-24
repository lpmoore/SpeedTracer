package com.lpmoore.speedtracer

import android.content.SharedPreferences

class ScoreStore(private val prefs: SharedPreferences) {

    data class Entry(val score: Int, val accuracyPct: Int, val coveragePct: Int, val timeMs: Long, val at: Long)

    fun getLevel(): Int = prefs.getInt(KEY_LEVEL, 1)

    fun setLevel(level: Int) {
        prefs.edit().putInt(KEY_LEVEL, level).apply()
    }

    fun add(r: RoundResult, level: Int = getLevel()) {
        val list = all(level).toMutableList()
        list.add(0, Entry(r.score, r.accuracyPct, r.coveragePct, r.timeMs, System.currentTimeMillis()))
        val trimmed = list.take(MAX)
        prefs.edit().putString(historyKey(level), trimmed.joinToString("\n") {
            "${it.score}|${it.accuracyPct}|${it.coveragePct}|${it.timeMs}|${it.at}"
        }).apply()
    }

    fun all(level: Int = getLevel()): List<Entry> {
        val key = historyKey(level)
        return prefs.getString(key, "")!!
            .lineSequence().filter { it.isNotBlank() }
            .mapNotNull { line ->
                val p = line.split("|")
                if (p.size != 5) null else runCatching {
                    Entry(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3].toLong(), p[4].toLong())
                }.getOrNull()
            }.toList()
    }

    fun best(level: Int = getLevel()): Int = all(level).maxOfOrNull { it.score } ?: 0

    fun checkPromotion(level: Int = getLevel()): Boolean {
        val recent = all(level).take(10)
        val required = when (level) {
            1 -> 5
            2 -> 6
            3 -> 7
            else -> 8
        }
        return recent.count { it.score >= 750 } >= required
    }

    private fun historyKey(level: Int): String = if (level == 1) "history" else "history_level_$level"

    companion object {
        private const val KEY_LEVEL = "current_level"
        private const val MAX = 20
    }
}
