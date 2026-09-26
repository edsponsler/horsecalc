package com.horsecalc.app.model

import java.util.UUID

/**
 * A participant in the horse race game who holds winning cards.
 *
 * @param id Unique identifier.
 * @param name Display name for the player (e.g. "Player 1", "Ed", etc.).
 * @param winningCards Number of winning cards held (1 to 4).
 */
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val winningCards: Int = 1
) {
    init {
        require(winningCards in 1..4) { "Winning cards per player must be between 1 and 4" }
    }

    val sharePercentage: Double
        get() = winningCards * 0.25

    val sharePercentageText: String
        get() = "${(winningCards * 25)}%"
}
