package com.horsecalc.app.domain

import com.horsecalc.app.model.Coin
import com.horsecalc.app.model.CoinPot
import com.horsecalc.app.model.DistributionResult
import com.horsecalc.app.model.PayoutShare
import com.horsecalc.app.model.Player
import kotlin.math.abs

/**
 * Core domain use case that distributes the physical coins in the pot to winning players
 * based on the 4 winning cards (25% per card).
 *
 * Handles:
 * 1. Validation that total winning cards equal 4.
 * 2. 25% share calculation per card (rounded down to nearest 5-cent increment).
 * 3. Extraction of leftover remainder coins (e.g. extra nickel) placed back in the pot.
 * 4. Exact integer coin allocation solving (Quarters, Dimes, Nickels) partitioned among players.
 * 5. Fair, balanced denomination distribution among equal or proportional shares.
 */
class DistributePotUseCase(
    private val calculatePotUseCase: CalculatePotUseCase = CalculatePotUseCase()
) {

    operator fun invoke(pot: CoinPot, players: List<Player>): DistributionResult {
        require(players.isNotEmpty()) { "At least one winning player is required" }
        val totalCards = players.sumOf { it.winningCards }
        require(totalCards == 4) {
            "Total winning cards across all players must equal exactly 4 (current: $totalCards)"
        }

        val potSummary = calculatePotUseCase(pot)
        val perShareCents = potSummary.perShareCents
        val remainderCents = potSummary.remainderCents

        // 1. Separate leftover / rollover coins (extra nickels/dimes back to the pot)
        val (rolloverPot, availableForPlayers) = extractRolloverCoins(pot, remainderCents)

        // 2. Prepare target cents for each player
        val targets = players.map { player ->
            player to (player.winningCards * perShareCents)
        }

        // 3. Solve exact physical coin allocation
        val allocation = solveAllocation(
            available = availableForPlayers,
            targets = targets
        )

        val shares = if (allocation != null) {
            players.zip(allocation).map { (player, coins) ->
                PayoutShare(
                    player = player,
                    coins = coins,
                    targetCents = player.winningCards * perShareCents,
                    isHandedOut = false
                )
            }
        } else {
            // Fallback greedy allocation if no exact physical partition exists
            solveGreedyFallback(availableForPlayers, targets)
        }

        val message = when {
            remainderCents == 5 -> "Extra nickel ($0.05) placed back in the pot for the next game."
            remainderCents > 5 -> "Extra ${Coin.NICKEL.format(remainderCents)} placed back in the pot for the next game."
            else -> "Pot evenly distributed with no leftover coins."
        }

        val isExactMatch = shares.all { it.isExact }

        return DistributionResult(
            originalPot = pot,
            shares = shares,
            rolloverPot = rolloverPot,
            message = message,
            isExactMatch = isExactMatch
        )
    }

    /**
     * Extracts coins for the remainder (e.g. 5, 10, or 15 cents), prioritizing nickels.
     */
    private fun extractRolloverCoins(pot: CoinPot, remainderCents: Int): Pair<CoinPot, CoinPot> {
        if (remainderCents <= 0) return CoinPot() to pot

        var rem = remainderCents
        var qAvail = pot.quarters
        var dAvail = pot.dimes
        var nAvail = pot.nickels

        var qRoll = 0
        var dRoll = 0
        var nRoll = 0

        // Prefer taking nickels for the extra nickel rollover rule
        while (rem >= 5 && nAvail > 0) {
            nRoll++
            nAvail--
            rem -= 5
        }

        // If not enough nickels, use dimes
        while (rem >= 10 && dAvail > 0) {
            dRoll++
            dAvail--
            rem -= 10
        }

        // If still needed (rare), use quarters
        while (rem >= 25 && qAvail > 0) {
            qRoll++
            qAvail--
            rem -= 25
        }

        val rollover = CoinPot(quarters = qRoll, dimes = dRoll, nickels = nRoll)
        val available = CoinPot(quarters = qAvail, dimes = dAvail, nickels = nAvail)
        return rollover to available
    }

    /**
     * Backtracking constraint solver to find an exact, balanced assignment of coins.
     */
    private fun solveAllocation(
        available: CoinPot,
        targets: List<Pair<Player, Int>>
    ): List<CoinPot>? {
        val totalNeeded = targets.sumOf { it.second }
        if (available.totalCents < totalNeeded) return null

        val result = mutableListOf<CoinPot>()
        val found = backtrack(
            targetIndex = 0,
            targets = targets,
            remQ = available.quarters,
            remD = available.dimes,
            remN = available.nickels,
            totalQ = available.quarters,
            totalD = available.dimes,
            totalN = available.nickels,
            currentSolution = result
        )

        return if (found) result else null
    }

    private fun backtrack(
        targetIndex: Int,
        targets: List<Pair<Player, Int>>,
        remQ: Int,
        remD: Int,
        remN: Int,
        totalQ: Int,
        totalD: Int,
        totalN: Int,
        currentSolution: MutableList<CoinPot>
    ): Boolean {
        if (targetIndex == targets.size) {
            // All players satisfied. All remaining coins must be 0
            return remQ == 0 && remD == 0 && remN == 0
        }

        val (player, targetCents) = targets[targetIndex]
        val isLast = (targetIndex == targets.size - 1)

        if (isLast) {
            // For the last player, their coins must be EXACTLY all remaining coins
            val lastPot = CoinPot(quarters = remQ, dimes = remD, nickels = remN)
            if (lastPot.totalCents == targetCents) {
                currentSolution.add(lastPot)
                return true
            }
            return false
        }

        // Proportional targets to guide fair heuristic search
        val shareRatio = player.winningCards / 4.0
        val idealQ = (totalQ * shareRatio).toInt()
        val idealD = (totalD * shareRatio).toInt()
        val idealN = (totalN * shareRatio).toInt()

        // Generate valid (q, d, n) combinations for this player
        val candidates = mutableListOf<CoinPot>()
        val maxQ = minOf(remQ, targetCents / 25)

        for (q in maxQ downTo 0) {
            val afterQ = targetCents - (q * 25)
            val maxD = minOf(remD, afterQ / 10)
            for (d in maxD downTo 0) {
                val afterD = afterQ - (d * 10)
                if (afterD % 5 == 0) {
                    val n = afterD / 5
                    if (n <= remN) {
                        candidates.add(CoinPot(quarters = q, dimes = d, nickels = n))
                    }
                }
            }
        }

        // Sort candidates by closeness to ideal proportional coin counts
        candidates.sortBy { c ->
            abs(c.quarters - idealQ) * 3 +
            abs(c.dimes - idealD) * 2 +
            abs(c.nickels - idealN)
        }

        for (cand in candidates) {
            currentSolution.add(cand)
            val success = backtrack(
                targetIndex = targetIndex + 1,
                targets = targets,
                remQ = remQ - cand.quarters,
                remD = remD - cand.dimes,
                remN = remN - cand.nickels,
                totalQ = totalQ,
                totalD = totalD,
                totalN = totalN,
                currentSolution = currentSolution
            )
            if (success) return true
            currentSolution.removeAt(currentSolution.size - 1)
        }

        return false
    }

    /**
     * Fallback greedy allocator when exact physical partition is impossible without external change.
     */
    private fun solveGreedyFallback(
        available: CoinPot,
        targets: List<Pair<Player, Int>>
    ): List<PayoutShare> {
        var remQ = available.quarters
        var remD = available.dimes
        var remN = available.nickels

        return targets.map { (player, targetCents) ->
            var needed = targetCents
            val q = minOf(remQ, needed / 25)
            needed -= q * 25
            remQ -= q

            val d = minOf(remD, needed / 10)
            needed -= d * 10
            remD -= d

            val n = minOf(remN, needed / 5)
            needed -= n * 5
            remN -= n

            PayoutShare(
                player = player,
                coins = CoinPot(quarters = q, dimes = d, nickels = n),
                targetCents = targetCents,
                isHandedOut = false
            )
        }
    }
}
