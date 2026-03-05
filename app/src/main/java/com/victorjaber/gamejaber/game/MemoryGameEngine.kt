package com.victorjaber.gamejaber.game

import kotlin.random.Random

class MemoryGameEngine {

    private val symbols = listOf("🐶", "🐱", "🦊", "🐼", "🐵", "🐸", "🐧", "🐨", "🐙", "🦁")

    private val cards = mutableListOf<Card>()
    private var firstFlippedIndex: Int? = null
    private var boardLocked = false

    var movesCount: Int = 0
        private set

    var matchesCount: Int = 0
        private set

    var pairCount: Int = 8
        private set

    fun startNewGame(newPairCount: Int = 8) {
        pairCount = newPairCount.coerceIn(2, symbols.size)
        movesCount = 0
        matchesCount = 0
        firstFlippedIndex = null
        boardLocked = false

        val selectedSymbols = symbols.shuffled(Random(System.currentTimeMillis())).take(pairCount)
        val deck = selectedSymbols.flatMapIndexed { index, symbol ->
            listOf(
                Card(id = index * 2, symbol = symbol),
                Card(id = index * 2 + 1, symbol = symbol)
            )
        }.shuffled(Random(System.currentTimeMillis()))

        cards.clear()
        cards.addAll(deck)
    }

    fun snapshot(): List<Card> = cards.toList()

    fun flipCard(index: Int): FlipResult {
        if (index !in cards.indices || boardLocked) return FlipResult.Ignored

        val card = cards[index]
        if (card.isFaceUp || card.isMatched) return FlipResult.Ignored

        cards[index] = card.copy(isFaceUp = true)

        val firstIndex = firstFlippedIndex
        if (firstIndex == null) {
            firstFlippedIndex = index
            return FlipResult.FirstCard
        }

        movesCount += 1
        val firstCard = cards[firstIndex]
        val secondCard = cards[index]

        return if (firstCard.symbol == secondCard.symbol) {
            cards[firstIndex] = firstCard.copy(isMatched = true)
            cards[index] = secondCard.copy(isMatched = true)
            firstFlippedIndex = null
            matchesCount += 1
            if (matchesCount == pairCount) FlipResult.GameFinished
            else FlipResult.Match(firstIndex, index)
        } else {
            firstFlippedIndex = null
            boardLocked = true
            FlipResult.Mismatch(firstIndex, index)
        }
    }

    fun hideMismatch(firstIndex: Int, secondIndex: Int) {
        if (firstIndex !in cards.indices || secondIndex !in cards.indices) return
        val firstCard = cards[firstIndex]
        val secondCard = cards[secondIndex]

        if (!firstCard.isMatched) {
            cards[firstIndex] = firstCard.copy(isFaceUp = false)
        }
        if (!secondCard.isMatched) {
            cards[secondIndex] = secondCard.copy(isFaceUp = false)
        }
        boardLocked = false
    }
}
