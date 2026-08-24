package com.lpmoore.speedtracer

import android.content.Context

class ScoreStore(context: Context) {
    private val prefs = context.getSharedPreferences("scores", Context.MODE_PRIVATE)

    data class Entry(val score: Int, val accuracyPct: Int, val coveragePct: Int, val timeMs: Long, val at: Long)

    fun add(r: RoundResult) {
        val list = all().toMutableList()
        list.add(0, Entry(r.score, r.accuracyPct, r.coveragePct, r.timeMs, System.currentTimeMillis()))
        val trimmed = list.take(MAX)
        prefs.edit().putString(KEY, trimmed.joinToString("\n") {
            "${it.score}|${it.accuracyPct}|${it.coveragePct}|${it.timeMs}|${it.at}"
        }).apply()
    }

    fun all(): List<Entry> = prefs.getString(KEY, "")!!
        .lineSequence().filter { it.isNotBlank() }
        .mapNotNull { line ->
            val p = line.split("|")
            if (p.size != 5) null else runCatching {
                Entry(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3].toLong(), p[4].toLong())
            }.getOrNull()
        }.toList()

    fun best(): Int = all().maxOfOrNull { it.score } ?: 0

    companion object { private const val KEY = "history"; private const val MAX = 20 }
}
