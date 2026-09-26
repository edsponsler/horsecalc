package com.horsecalc.app.model

import java.util.Locale

/**
 * Represents the pool of coins currently in the pot.
 */
data class CoinPot(
    val quarters: Int = 0,
    val dimes: Int = 0,
    val nickels: Int = 0
) {
    init {
        require(quarters >= 0) { "Quarters count cannot be negative" }
        require(dimes >= 0) { "Dimes count cannot be negative" }
        require(nickels >= 0) { "Nickels count cannot be negative" }
    }

    val totalCoins: Int
        get() = quarters + dimes + nickels

    val totalCents: Int
        get() = (quarters * Coin.QUARTER.valueCents) +
                (dimes * Coin.DIME.valueCents) +
                (nickels * Coin.NICKEL.valueCents)

    val formattedTotal: String
        get() = "$${String.format(Locale.US, "%.2f", totalCents / 100.0)}"

    fun isEmpty(): Boolean = totalCoins == 0

    operator fun plus(other: CoinPot): CoinPot =
        CoinPot(
            quarters = this.quarters + other.quarters,
            dimes = this.dimes + other.dimes,
            nickels = this.nickels + other.nickels
        )

    operator fun minus(other: CoinPot): CoinPot =
        CoinPot(
            quarters = (this.quarters - other.quarters).coerceAtLeast(0),
            dimes = (this.dimes - other.dimes).coerceAtLeast(0),
            nickels = (this.nickels - other.nickels).coerceAtLeast(0)
        )
}
