package com.victorjaber.gamejaber

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.victorjaber.gamejaber.data.ScoreRepository
import com.victorjaber.gamejaber.game.Card
import com.victorjaber.gamejaber.game.FlipResult
import com.victorjaber.gamejaber.game.GameUiState
import com.victorjaber.gamejaber.game.GameViewModel
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "MainActivity"
    }

    private val viewModel: GameViewModel by viewModels()

    private lateinit var scoreRepository: ScoreRepository

    private lateinit var screenStart: LinearLayout
    private lateinit var screenGame: LinearLayout
    private lateinit var screenFinish: LinearLayout

    private lateinit var textBestScore: TextView
    private lateinit var textTimer: TextView
    private lateinit var textMoves: TextView
    private lateinit var textMatches: TextView
    private lateinit var textGameStatus: TextView

    private lateinit var textResultMessage: TextView
    private lateinit var textResultScore: TextView

    private lateinit var cardsGrid: GridLayout

    private val cardButtons = mutableListOf<MaterialButton>()
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunning = false
    private var finishShown = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            viewModel.tick()
            val state = viewModel.uiState.value
            if (state?.isRunning == true) {
                handler.postDelayed(this, 1000)
            } else {
                timerRunning = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            scoreRepository = ScoreRepository(applicationContext)
            bindViews()
            bindActions()
            observeState()
            showHome()
        } catch (throwable: Throwable) {
            Log.e(TAG, "Falha na inicialização da tela", throwable)
            showStartupError(throwable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    private fun bindViews() {
        screenStart = findViewById(R.id.screenStart)
        screenGame = findViewById(R.id.screenGame)
        screenFinish = findViewById(R.id.screenFinish)

        textBestScore = findViewById(R.id.textBestScore)
        textTimer = findViewById(R.id.textTimer)
        textMoves = findViewById(R.id.textMoves)
        textMatches = findViewById(R.id.textMatches)
        textGameStatus = findViewById(R.id.textGameStatus)

        textResultMessage = findViewById(R.id.textResultMessage)
        textResultScore = findViewById(R.id.textResultScore)

        cardsGrid = findViewById(R.id.cardsGrid)
    }

    private fun bindActions() {
        findViewById<MaterialButton>(R.id.buttonStart).setOnClickListener {
            startNewGame()
        }

        findViewById<MaterialButton>(R.id.buttonPlayAgain).setOnClickListener {
            startNewGame()
        }

        findViewById<MaterialButton>(R.id.buttonHome).setOnClickListener {
            showHome()
        }
    }

    private fun observeState() {
        viewModel.uiState.observe(this) { state ->
            renderState(state)
            if (state.isFinished && !finishShown) {
                showFinish(state)
            }
        }
    }

    private fun showHome() {
        stopTimer()
        finishShown = false
        screenStart.visibility = LinearLayout.VISIBLE
        screenGame.visibility = LinearLayout.GONE
        screenFinish.visibility = LinearLayout.GONE
        textGameStatus.setText(R.string.status_ready)
        textGameStatus.setTextColor(getColorCompat(R.color.status_neutral))
        updateBestScoreText()
    }

    private fun startNewGame() {
        finishShown = false
        screenStart.visibility = LinearLayout.GONE
        screenFinish.visibility = LinearLayout.GONE
        screenGame.visibility = LinearLayout.VISIBLE

        viewModel.startGame(pairCount = 8, durationSeconds = 90)
        textGameStatus.setText(R.string.status_ready)
        textGameStatus.setTextColor(getColorCompat(R.color.status_neutral))
        startTimer()
    }

    private fun showFinish(state: GameUiState) {
        finishShown = true
        stopTimer()

        val score = calculateScore(state)
        scoreRepository.saveBestScore(score)

        screenStart.visibility = LinearLayout.GONE
        screenGame.visibility = LinearLayout.GONE
        screenFinish.visibility = LinearLayout.VISIBLE

        textResultMessage.text = if (state.allMatched) {
            getString(R.string.win_message)
        } else {
            getString(R.string.lose_message)
        }
        textResultScore.text = getString(R.string.score, score)
        updateBestScoreText()
    }

    private fun calculateScore(state: GameUiState): Int {
        val bonusWin = if (state.allMatched) 1000 else 0
        return max(0, (state.matches * 500) - (state.moves * 15) + (state.secondsRemaining * 25) + bonusWin)
    }

    private fun startTimer() {
        if (timerRunning) return
        timerRunning = true
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun stopTimer() {
        timerRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    private fun renderState(state: GameUiState) {
        textTimer.text = getString(R.string.time_left, state.secondsRemaining)
        textMoves.text = getString(R.string.moves, state.moves)
        textMatches.text = getString(R.string.matches, state.matches, state.totalPairs)
        renderCards(state.cards)
    }

    private fun renderCards(cards: List<Card>) {
        if (cards.isEmpty()) return

        if (cardButtons.size != cards.size) {
            cardButtons.clear()
            cardsGrid.removeAllViews()
            cardsGrid.columnCount = 4
            cardsGrid.rowCount = 4

            cards.forEachIndexed { index, _ ->
                val button = MaterialButton(this)
                val layoutParams = GridLayout.LayoutParams().apply {
                    val margin = dpToPx(6)
                    val size = calculateCardSize()
                    width = size
                    height = size
                    rowSpec = GridLayout.spec(index / 4)
                    columnSpec = GridLayout.spec(index % 4)
                    setMargins(margin, margin, margin, margin)
                }
                button.layoutParams = layoutParams
                button.insetTop = 0
                button.insetBottom = 0
                button.cornerRadius = dpToPx(14)
                button.setPadding(0, 0, 0, 0)
                button.setOnClickListener { onCardClicked(index) }
                cardButtons.add(button)
                cardsGrid.addView(button)
            }
        }

        cards.forEachIndexed { index, card ->
            val button = cardButtons[index]
            when {
                card.isMatched -> {
                    button.backgroundTintList = ColorStateList.valueOf(getColorCompat(R.color.card_matched))
                    button.text = card.symbol
                    button.isEnabled = false
                    button.alpha = 0.85f
                    button.setTextColor(getColorCompat(R.color.text_primary))
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                }

                card.isFaceUp -> {
                    button.backgroundTintList = ColorStateList.valueOf(getColorCompat(R.color.card_front))
                    button.text = card.symbol
                    button.isEnabled = true
                    button.alpha = 1f
                    button.setTextColor(getColorCompat(R.color.text_primary))
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                }

                else -> {
                    button.backgroundTintList = ColorStateList.valueOf(getColorCompat(R.color.card_back))
                    button.text = "?"
                    button.isEnabled = true
                    button.alpha = 1f
                    button.setTextColor(getColorCompat(R.color.card_text_light))
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                }
            }
        }
    }

    private fun onCardClicked(index: Int) {
        when (val result = viewModel.onCardTapped(index)) {
            is FlipResult.FirstCard -> {
                textGameStatus.setText(R.string.status_first_card)
                textGameStatus.setTextColor(getColorCompat(R.color.status_neutral))
            }

            is FlipResult.Match -> {
                textGameStatus.setText(R.string.status_match)
                textGameStatus.setTextColor(getColorCompat(R.color.status_good))
            }

            is FlipResult.Mismatch -> {
                textGameStatus.setText(R.string.status_mismatch)
                textGameStatus.setTextColor(getColorCompat(R.color.status_bad))
                disableCardBoard()
                handler.postDelayed({
                    viewModel.hideMismatch(result.firstIndex, result.secondIndex)
                    enableCardBoard()
                }, 700)
            }

            is FlipResult.GameFinished -> {
                textGameStatus.setText(R.string.status_match)
                textGameStatus.setTextColor(getColorCompat(R.color.status_good))
            }

            else -> Unit
        }
    }

    private fun disableCardBoard() {
        cardButtons.forEach { it.isEnabled = false }
    }

    private fun enableCardBoard() {
        val state = viewModel.uiState.value ?: return
        state.cards.forEachIndexed { index, card ->
            cardButtons.getOrNull(index)?.isEnabled = !card.isMatched
        }
    }

    private fun updateBestScoreText() {
        textBestScore.text = getString(R.string.best_score, scoreRepository.getBestScore())
    }

    private fun calculateCardSize(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val totalHorizontalPadding = dpToPx(24 * 2) + dpToPx(20 * 2)
        val totalMargins = dpToPx(6 * 2 * 4)
        val available = screenWidth - totalHorizontalPadding - totalMargins
        return max(dpToPx(70), available / 4)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun getColorCompat(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    private fun showStartupError(throwable: Throwable) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColorCompat(R.color.background_bottom))
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
        }

        val title = TextView(this).apply {
            text = "Erro ao abrir o app"
            setTextColor(getColorCompat(R.color.status_bad))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = "${throwable.javaClass.simpleName}: ${throwable.message ?: "sem mensagem"}"
            setTextColor(getColorCompat(R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(12), 0, 0)
        }

        root.addView(title)
        root.addView(message)
        setContentView(root)
    }
}
