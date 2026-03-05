package com.victorjaber.gamejaber.game

data class GameUiState(
    val cards: List<Card> = emptyList(),
    val moves: Int = 0,
    val matches: Int = 0,
    val totalPairs: Int = 8,
    val secondsRemaining: Int = 90,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val allMatched: Boolean = false
)
