package com.victorjaber.gamejaber

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
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
        const val MISMATCH_DELAY_MS = 800L
        const val CARD_FLIP_DURATION = 200L
        const val CARD_MATCH_SCALE_DURATION = 300L
        const val SCREEN_FADE_DURATION = 300L
        const val STAGGER_DELAY = 30L
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
    private lateinit var progressTimer: ProgressBar

    private lateinit var textResultTitle: TextView
    private lateinit var textResultEmoji: TextView
    private lateinit var textResultMessage: TextView
    private lateinit var textResultScore: TextView
    private lateinit var textNewRecord: TextView

    private lateinit var cardsGrid: GridLayout

    private val cardButtons = mutableListOf<MaterialButton>()
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunning = false
    private var finishShown = false
    private var gameDuration = 90

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
        progressTimer = findViewById(R.id.progressTimer)

        textResultTitle = findViewById(R.id.textResultTitle)
        textResultEmoji = findViewById(R.id.textResultEmoji)
        textResultMessage = findViewById(R.id.textResultMessage)
        textResultScore = findViewById(R.id.textResultScore)
        textNewRecord = findViewById(R.id.textNewRecord)

        cardsGrid = findViewById(R.id.cardsGrid)
    }

    private fun bindActions() {
        findViewById<MaterialButton>(R.id.buttonStart).setOnClickListener {
            animateButtonPress(it) { startNewGame() }
        }

        findViewById<MaterialButton>(R.id.buttonPlayAgain).setOnClickListener {
            animateButtonPress(it) { startNewGame() }
        }

        findViewById<MaterialButton>(R.id.buttonHome).setOnClickListener {
            animateButtonPress(it) { showHome() }
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

        fadeTransition(screenStart, screenGame, screenFinish)

        textGameStatus.setText(R.string.status_ready)
        textGameStatus.setTextColor(getColorCompat(R.color.status_neutral))
        updateBestScoreText()

        animateScreenEntrance(screenStart)
    }

    private fun startNewGame() {
        finishShown = false
        gameDuration = 90

        fadeTransition(screenGame, screenStart, screenFinish)

        viewModel.startGame(pairCount = 8, durationSeconds = gameDuration)
        textGameStatus.setText(R.string.status_ready)
        textGameStatus.setTextColor(getColorCompat(R.color.status_neutral))
        progressTimer.max = gameDuration
        progressTimer.progress = gameDuration
        startTimer()

        handler.post { animateCardsEntrance() }
    }

    private fun showFinish(state: GameUiState) {
        finishShown = true
        stopTimer()

        val score = calculateScore(state)
        val previousBest = scoreRepository.getBestScore()
        val isNewRecord = score > previousBest
        scoreRepository.saveBestScore(score)

        fadeTransition(screenFinish, screenStart, screenGame)

        if (state.allMatched) {
            textResultTitle.setText(R.string.result_title_win)
            textResultEmoji.setText(R.string.result_emoji_win)
        } else {
            textResultTitle.setText(R.string.result_title_lose)
            textResultEmoji.setText(R.string.result_emoji_lose)
        }

        textResultMessage.text = if (state.allMatched) {
            getString(R.string.win_message)
        } else {
            getString(R.string.lose_message)
        }
        textResultScore.text = getString(R.string.score_value, score)

        if (isNewRecord && score > 0) {
            textNewRecord.visibility = View.VISIBLE
            animatePulse(textNewRecord)
        } else {
            textNewRecord.visibility = View.GONE
        }

        updateBestScoreText()
        animateFinishScreen()
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

        progressTimer.progress = state.secondsRemaining
        updateTimerColor(state.secondsRemaining)

        renderCards(state.cards)
    }

    private fun updateTimerColor(seconds: Int) {
        val color = when {
            seconds <= 10 -> getColorCompat(R.color.timer_danger)
            seconds <= 30 -> getColorCompat(R.color.timer_warning)
            else -> getColorCompat(R.color.stat_icon_time)
        }
        textTimer.setTextColor(color)

        if (seconds <= 10 && seconds > 0) {
            animatePulse(textTimer)
        }
    }

    private fun renderCards(cards: List<Card>) {
        if (cards.isEmpty()) return

        if (cardButtons.size != cards.size) {
            cardButtons.clear()
            cardsGrid.removeAllViews()
            cardsGrid.columnCount = 4
            cardsGrid.rowCount = 4

            cards.forEachIndexed { index, _ ->
                val button = MaterialButton(this).apply {
                    val lp = GridLayout.LayoutParams().apply {
                        val margin = dpToPx(5)
                        val size = calculateCardSize()
                        width = size
                        height = size
                        rowSpec = GridLayout.spec(index / 4)
                        columnSpec = GridLayout.spec(index % 4)
                        setMargins(margin, margin, margin, margin)
                    }
                    layoutParams = lp
                    insetTop = 0
                    insetBottom = 0
                    cornerRadius = dpToPx(16)
                    setPadding(0, 0, 0, 0)
                    elevation = dpToPx(4).toFloat()
                    stateListAnimator = null
                    setOnClickListener { onCardClicked(index) }
                }
                cardButtons.add(button)
                cardsGrid.addView(button)
            }
        }

        cards.forEachIndexed { index, card ->
            val button = cardButtons[index]
            when {
                card.isMatched -> {
                    button.setBackgroundResource(R.drawable.bg_card_matched)
                    button.backgroundTintList = null
                    button.text = card.symbol
                    button.isEnabled = false
                    button.alpha = 0.8f
                    button.setTextColor(getColorCompat(R.color.white))
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
                    button.elevation = dpToPx(2).toFloat()
                }

                card.isFaceUp -> {
                    button.setBackgroundResource(R.drawable.bg_card_front)
                    button.backgroundTintList = null
                    button.text = card.symbol
                    button.isEnabled = true
                    button.alpha = 1f
                    button.setTextColor(getColorCompat(R.color.text_primary))
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
                    button.elevation = dpToPx(6).toFloat()
                }

                else -> {
                    button.setBackgroundResource(R.drawable.bg_card_back)
                    button.backgroundTintList = null
                    button.text = "?"
                    button.isEnabled = true
                    button.alpha = 1f
                    button.setTextColor(getColorCompat(R.color.card_text_light))
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                    button.elevation = dpToPx(4).toFloat()
                }
            }
            button.setTypeface(null, Typeface.BOLD)
        }
    }

    private fun onCardClicked(index: Int) {
        val button = cardButtons.getOrNull(index) ?: return
        animateCardFlip(button)

        when (val result = viewModel.onCardTapped(index)) {
            is FlipResult.FirstCard -> {
                textGameStatus.setText(R.string.status_first_card)
                textGameStatus.setTextColor(getColorCompat(R.color.status_neutral))
            }

            is FlipResult.Match -> {
                textGameStatus.setText(R.string.status_match)
                textGameStatus.setTextColor(getColorCompat(R.color.status_good))
                animateMatchCelebration(result.firstIndex, result.secondIndex)
            }

            is FlipResult.Mismatch -> {
                textGameStatus.setText(R.string.status_mismatch)
                textGameStatus.setTextColor(getColorCompat(R.color.status_bad))
                animateShake(textGameStatus)
                disableCardBoard()
                handler.postDelayed({
                    viewModel.hideMismatch(result.firstIndex, result.secondIndex)
                    enableCardBoard()
                }, MISMATCH_DELAY_MS)
            }

            is FlipResult.GameFinished -> {
                textGameStatus.setText(R.string.status_match)
                textGameStatus.setTextColor(getColorCompat(R.color.status_good))
                animateAllCardsVictory()
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
        val best = scoreRepository.getBestScore()
        textBestScore.text = if (best > 0) {
            getString(R.string.best_score, best)
        } else {
            getString(R.string.best_score_none)
        }
    }

    private fun calculateCardSize(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val totalHorizontalPadding = dpToPx(20 * 2)
        val totalMargins = dpToPx(5 * 2 * 4)
        val available = screenWidth - totalHorizontalPadding - totalMargins
        return max(dpToPx(64), available / 4)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun getColorCompat(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    // ====== ANIMATIONS ======

    private fun animateButtonPress(view: View, onComplete: () -> Unit) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
            )
            duration = 80
        }
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            )
            duration = 80
            interpolator = OvershootInterpolator(2f)
        }
        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            start()
        }
        handler.postDelayed(onComplete, 160)
    }

    private fun animateCardFlip(button: MaterialButton) {
        val flipOut = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0f).apply {
            duration = CARD_FLIP_DURATION / 2
            interpolator = AccelerateDecelerateInterpolator()
        }
        val flipIn = ObjectAnimator.ofFloat(button, "scaleX", 0f, 1f).apply {
            duration = CARD_FLIP_DURATION / 2
            interpolator = DecelerateInterpolator()
        }
        AnimatorSet().apply {
            playSequentially(flipOut, flipIn)
            start()
        }
    }

    private fun animateMatchCelebration(firstIndex: Int, secondIndex: Int) {
        listOf(firstIndex, secondIndex).forEach { index ->
            cardButtons.getOrNull(index)?.let { button ->
                val bounce = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.15f, 1f),
                        ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.15f, 1f)
                    )
                    duration = CARD_MATCH_SCALE_DURATION
                    interpolator = OvershootInterpolator(3f)
                }
                bounce.start()
            }
        }
    }

    private fun animateShake(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, -8f, 8f, -6f, 6f, -3f, 3f, 0f).apply {
            duration = 400
            start()
        }
    }

    private fun animatePulse(view: View) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)
            )
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun animateAllCardsVictory() {
        cardButtons.forEachIndexed { index, button ->
            handler.postDelayed({
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.2f, 1f),
                        ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.2f, 1f),
                        ObjectAnimator.ofFloat(button, "rotation", 0f, 10f, -10f, 0f)
                    )
                    duration = 400
                    interpolator = OvershootInterpolator(2f)
                    start()
                }
            }, index * 50L)
        }
    }

    private fun animateCardsEntrance() {
        cardButtons.forEachIndexed { index, button ->
            button.alpha = 0f
            button.scaleX = 0.5f
            button.scaleY = 0.5f
            handler.postDelayed({
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(button, "alpha", 0f, 1f),
                        ObjectAnimator.ofFloat(button, "scaleX", 0.5f, 1f),
                        ObjectAnimator.ofFloat(button, "scaleY", 0.5f, 1f)
                    )
                    duration = 250
                    interpolator = OvershootInterpolator(2f)
                    start()
                }
            }, index * STAGGER_DELAY)
        }
    }

    private fun animateScreenEntrance(screen: View) {
        screen.alpha = 0f
        screen.translationY = 40f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(screen, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(screen, "translationY", 40f, 0f)
            )
            duration = SCREEN_FADE_DURATION
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun animateFinishScreen() {
        val emoji = textResultEmoji
        emoji.scaleX = 0f
        emoji.scaleY = 0f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(emoji, "scaleX", 0f, 1f),
                ObjectAnimator.ofFloat(emoji, "scaleY", 0f, 1f)
            )
            duration = 500
            interpolator = OvershootInterpolator(3f)
            startDelay = 100
            start()
        }

        val score = textResultScore
        score.scaleX = 0.5f
        score.scaleY = 0.5f
        score.alpha = 0f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(score, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(score, "scaleX", 0.5f, 1f),
                ObjectAnimator.ofFloat(score, "scaleY", 0.5f, 1f)
            )
            duration = 400
            startDelay = 300
            interpolator = OvershootInterpolator(2f)
            start()
        }

        screenFinish.alpha = 0f
        screenFinish.translationY = 30f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(screenFinish, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(screenFinish, "translationY", 30f, 0f)
            )
            duration = SCREEN_FADE_DURATION
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun fadeTransition(show: View, vararg hide: View) {
        show.visibility = View.VISIBLE
        show.alpha = 0f
        ObjectAnimator.ofFloat(show, "alpha", 0f, 1f).apply {
            duration = SCREEN_FADE_DURATION
            start()
        }
        hide.forEach { view ->
            if (view.visibility == View.VISIBLE) {
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
                    duration = SCREEN_FADE_DURATION / 2
                    start()
                }
                handler.postDelayed({ view.visibility = View.GONE }, SCREEN_FADE_DURATION / 2)
            } else {
                view.visibility = View.GONE
            }
        }
    }

    private fun showStartupError(throwable: Throwable) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColorCompat(R.color.background_top))
            setPadding(dpToPx(32), dpToPx(32), dpToPx(32), dpToPx(32))
        }

        val emoji = TextView(this).apply {
            text = getString(R.string.error_emoji)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 64f)
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = getString(R.string.error_title)
            setTextColor(getColorCompat(R.color.status_bad))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }

        val message = TextView(this).apply {
            text = "${throwable.javaClass.simpleName}: ${throwable.message ?: "sem mensagem"}"
            setTextColor(getColorCompat(R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(12), 0, 0)
        }

        root.addView(emoji)
        root.addView(title)
        root.addView(message)
        setContentView(root)
    }
}
