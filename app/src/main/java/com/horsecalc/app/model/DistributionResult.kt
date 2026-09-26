package com.horsecalc.app.model

import java.util.Locale

/**
 * Complete result of the pot distribution calculation.
 *
 * @param originalPot The initial pot of coins before distribution.
 * @param shares The coin payout assigned to each player.
 * @param rolloverPot Coins left over in the pot for the next game (e.g. extra nickel).
 * @param message User-friendly status or instructions (e.g. extra nickel rollover note).
 * @param isExactMatch Whether every player received their exact monetary entitlement.
 */
data class DistributionResult(
    val originalPot: CoinPot,
    val shares: List<PayoutShare>,
    val rolloverPot: CoinPot = CoinPot(),
    val message: String? = null,
    val isExactMatch: Boolean = true
) {
    val totalCoinsDistributed: Int
        get() = shares.sumOf { it.coins.totalCoins }

    val totalCentsDistributed: Int
        get() = shares.sumOf { it.coins.totalCents }

    val rolloverCents: Int
        get() = rolloverPot.totalCents

    val formattedDistributed: String
        get() = "$${String.format(Locale.US, "%.2f", totalCentsDistributed / 100.0)}"

    val formattedRollover: String
        get() = "$${String.format(Locale.US, "%.2f", rolloverCents / 100.0)}"
}
