package com.victorjaber.gamejaber.data

import android.content.Context

class ScoreRepository(context: Context) {

    private val prefs = context.getSharedPreferences("memory_game_prefs", Context.MODE_PRIVATE)

    fun getBestScore(): Int = prefs.getInt(KEY_BEST_SCORE, 0)

    fun saveBestScore(score: Int) {
        if (score <= getBestScore()) return
        prefs.edit().putInt(KEY_BEST_SCORE, score).apply()
    }

    private companion object {
        const val KEY_BEST_SCORE = "best_score"
    }
}
