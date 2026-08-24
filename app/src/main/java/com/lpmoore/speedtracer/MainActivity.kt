package com.lpmoore.speedtracer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), GameView.Listener {

    private lateinit var game: GameView
    private lateinit var timerText: TextView
    private lateinit var bestText: TextView
    private lateinit var startButton: Button
    private lateinit var resultPanel: View
    private lateinit var resultScore: TextView
    private lateinit var resultDetails: TextView
    private lateinit var store: ScoreStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        store = ScoreStore(this)
        game = findViewById(R.id.gameView)
        timerText = findViewById(R.id.timerText)
        bestText = findViewById(R.id.bestText)
        startButton = findViewById(R.id.startButton)
        resultPanel = findViewById(R.id.resultPanel)
        resultScore = findViewById(R.id.resultScore)
        resultDetails = findViewById(R.id.resultDetails)

        game.listener = this
        startButton.setOnClickListener { startRound() }
        findViewById<Button>(R.id.againButton).setOnClickListener { startRound() }
        findViewById<Button>(R.id.historyButton).setOnClickListener { showHistory() }
        refreshBest()
    }

    // Step 6 of the game loop: every round is a fresh random circle.
    private fun startRound() {
        startButton.visibility = View.GONE
        resultPanel.visibility = View.GONE
        game.post { game.startRound() }   // ensure the view is laid out
    }

    override fun onTick(remainingMs: Long) {
        timerText.text = String.format(Locale.US, "%.1f", remainingMs / 1000.0)
    }

    override fun onRoundFinished(result: RoundResult) {
        store.add(result)
        refreshBest()
        resultScore.text = result.score.toString()
        resultDetails.text = buildString {
            append("Accuracy ${result.accuracyPct}%  •  Coverage ${result.coveragePct}%\n")
            append(String.format(Locale.US, "Time %.2fs", result.timeMs / 1000.0))
            if (result.interrupted) append("  (interrupted)")
        }
        resultPanel.visibility = View.VISIBLE
    }

    private fun refreshBest() {
        val b = store.best()
        bestText.text = if (b > 0) "Best $b" else ""
    }

    private fun showHistory() {
        val entries = store.all()
        val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
        val text = if (entries.isEmpty()) getString(R.string.history_empty)
        else entries.joinToString("\n") {
            String.format(Locale.US, "%4d   acc %3d%%  cov %3d%%  %.2fs   %s",
                it.score, it.accuracyPct, it.coveragePct, it.timeMs / 1000.0, fmt.format(Date(it.at)))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.history)
            .setMessage(text)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        if (game.state == GameView.State.READY || game.state == GameView.State.TRACING) {
            game.reset()
            startButton.visibility = View.VISIBLE
            onTick(Scorer.TIME_LIMIT_MS)
        }
    }
}
