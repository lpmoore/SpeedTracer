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
    private lateinit var explosionView: ExplosionView
    private lateinit var tesseractView: TesseractView
    private lateinit var soundSynth: SoundSynth
    private lateinit var levelText: TextView
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

        store = ScoreStore(getSharedPreferences("scores", MODE_PRIVATE))
        game = findViewById(R.id.gameView)
        explosionView = findViewById(R.id.explosionView)
        tesseractView = findViewById(R.id.tesseractView)
        soundSynth = SoundSynth()
        levelText = findViewById(R.id.levelText)
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
        refreshLevel()
        refreshBest()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundSynth.release()
    }

    private fun refreshLevel() {
        val level = store.getLevel()
        levelText.text = "Level $level"
        game.currentLevel = level
    }

    // Step 6 of the game loop: every round is a fresh random circle.
    private fun startRound() {
        startButton.visibility = View.GONE
        resultPanel.visibility = View.GONE
        refreshLevel()
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

        if (result.score == 1000) {
            soundSynth.play(SoundSynth.SoundType.PORTAL)
            tesseractView.show {
                handlePostRound(result)
            }
        } else if (result.score >= 500) {
            val soundType = when (ExplosionView.tierForScore(result.score)) {
                0 -> SoundSynth.SoundType.CRACK
                1 -> SoundSynth.SoundType.BLAST
                2 -> SoundSynth.SoundType.BOOM
                3 -> SoundSynth.SoundType.RUMBLE
                else -> SoundSynth.SoundType.DETONATION
            }
            soundSynth.play(soundType)

            val (cx, cy) = game.circleCenter()
            explosionView.explode(cx, cy, result.score) {
                handlePostRound(result)
            }
        } else {
            handlePostRound(result)
        }
    }

    private fun handlePostRound(result: RoundResult) {
        val currentLevel = store.getLevel()
        if (store.checkPromotion(currentLevel)) {
            val nextLevel = currentLevel + 1
            store.setLevel(nextLevel)
            soundSynth.play(SoundSynth.SoundType.LEVEL_UP)
            AlertDialog.Builder(this)
                .setTitle(R.string.level_up_title)
                .setMessage(getString(R.string.level_up_message, nextLevel))
                .setPositiveButton(R.string.level_up_button) { _, _ ->
                    refreshLevel()
                    refreshBest()
                    resultPanel.visibility = View.VISIBLE
                }
                .setCancelable(false)
                .show()
        } else {
            resultPanel.visibility = View.VISIBLE
        }
    }

    private fun refreshBest() {
        val level = store.getLevel()
        val b = store.best(level)
        bestText.text = if (b > 0) "Best $b" else ""
    }

    private fun showHistory() {
        val level = store.getLevel()
        val entries = store.all(level)
        val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
        val text = if (entries.isEmpty()) getString(R.string.history_empty)
        else entries.joinToString("\n") {
            String.format(Locale.US, "%4d   acc %3d%%  cov %3d%%  %.2fs   %s",
                it.score, it.accuracyPct, it.coveragePct, it.timeMs / 1000.0, fmt.format(Date(it.at)))
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.history_title, level))
            .setMessage(text)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        if (game.state == GameView.State.READY || game.state == GameView.State.TRACING) {
            game.reset()
            startButton.visibility = View.VISIBLE
            val level = store.getLevel()
            onTick(level * 3000L)
        }
    }
}
