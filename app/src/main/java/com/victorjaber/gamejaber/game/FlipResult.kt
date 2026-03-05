package com.victorjaber.gamejaber.game

sealed interface FlipResult {
    data object Ignored : FlipResult
    data object FirstCard : FlipResult
    data class Match(val firstIndex: Int, val secondIndex: Int) : FlipResult
    data class Mismatch(val firstIndex: Int, val secondIndex: Int) : FlipResult
    data object GameFinished : FlipResult
}
