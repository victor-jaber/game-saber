package com.victorjaber.gamejaber.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {

    private val engine = MemoryGameEngine()

    private val _uiState = MutableLiveData(GameUiState())
    val uiState: LiveData<GameUiState> = _uiState

    fun startGame(pairCount: Int = 8, durationSeconds: Int = 90) {
        engine.startNewGame(pairCount)
        _uiState.value = GameUiState(
            cards = engine.snapshot(),
            moves = 0,
            matches = 0,
            totalPairs = pairCount,
            secondsRemaining = durationSeconds,
            isRunning = true,
            isFinished = false,
            allMatched = false
        )
    }

    fun onCardTapped(index: Int): FlipResult {
        val current = _uiState.value ?: return FlipResult.Ignored
        if (!current.isRunning || current.isFinished) return FlipResult.Ignored

        val result = engine.flipCard(index)
        val allMatched = result is FlipResult.GameFinished
        _uiState.value = current.copy(
            cards = engine.snapshot(),
            moves = engine.movesCount,
            matches = engine.matchesCount,
            isRunning = !allMatched,
            isFinished = allMatched,
            allMatched = allMatched
        )
        return result
    }

    fun hideMismatch(firstIndex: Int, secondIndex: Int) {
        val current = _uiState.value ?: return
        engine.hideMismatch(firstIndex, secondIndex)
        _uiState.value = current.copy(
            cards = engine.snapshot(),
            moves = engine.movesCount,
            matches = engine.matchesCount
        )
    }

    fun tick() {
        val current = _uiState.value ?: return
        if (!current.isRunning || current.isFinished) return

        val newSeconds = current.secondsRemaining - 1
        if (newSeconds <= 0) {
            _uiState.value = current.copy(
                secondsRemaining = 0,
                isRunning = false,
                isFinished = true,
                allMatched = false
            )
            return
        }

        _uiState.value = current.copy(secondsRemaining = newSeconds)
    }
}
