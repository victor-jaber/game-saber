package com.victorjaber.gamejaber.game

data class Card(
    val id: Int,
    val symbol: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)
