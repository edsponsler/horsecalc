package com.horsecalc.app.domain

import com.horsecalc.app.model.CoinPot

/**
 * Use case to validate and calculate pot values.
 */
class CalculatePotUseCase {

    /**
     * Calculates the pot summary and per-card share target (25% rounded down to nearest nickel).
     *
     * @param pot Current coins in the pot.
     * @return [PotSummary] with total value, per-share value, and leftover cents.
     */
    operator fun invoke(pot: CoinPot): PotSummary {
        val totalCents = pot.totalCents
        // Each card represents 25% (1/4) of the pot.
        // Payouts must be in multiples of 5 cents (smallest coin is a nickel).
        val centsPerShare = (totalCents / 20) * 5
        val totalPayable = centsPerShare * 4
        val remainderCents = totalCents - totalPayable

        return PotSummary(
            totalCents = totalCents,
            perShareCents = centsPerShare,
            remainderCents = remainderCents,
            hasExtraNickel = remainderCents >= 5
        )
    }

    data class PotSummary(
        val totalCents: Int,
        val perShareCents: Int,
        val remainderCents: Int,
        val hasExtraNickel: Boolean
    )
}
