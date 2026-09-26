package com.horsecalc.app.model

import java.util.Locale

/**
 * The calculated payout for a single winner.
 *
 * @param player The player receiving the payout.
 * @param coins The exact coins assigned to this player.
 * @param targetCents The theoretical monetary target (cents) based on their card share.
 * @param isHandedOut Whether the coins have been physically handed to the player.
 */
data class PayoutShare(
    val player: Player,
    val coins: CoinPot,
    val targetCents: Int,
    val isHandedOut: Boolean = false
) {
    val actualCents: Int
        get() = coins.totalCents

    val formattedTarget: String
        get() = "$${String.format(Locale.US, "%.2f", targetCents / 100.0)}"

    val formattedActual: String
        get() = "$${String.format(Locale.US, "%.2f", actualCents / 100.0)}"

    /**
     * Checks if actual coins match theoretical target exactly.
     */
    val isExact: Boolean
        get() = actualCents == targetCents
}
