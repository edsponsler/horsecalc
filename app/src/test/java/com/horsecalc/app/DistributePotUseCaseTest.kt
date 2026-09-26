package com.horsecalc.app

import com.horsecalc.app.domain.DistributePotUseCase
import com.horsecalc.app.model.CoinPot
import com.horsecalc.app.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistributePotUseCaseTest {

    private val useCase = DistributePotUseCase()

    @Test
    fun `single winner holding all 4 cards receives entire pot`() {
        // Pot: 20 quarters ($5.00), 10 dimes ($1.00), 8 nickels ($0.40) = $6.40
        val pot = CoinPot(quarters = 20, dimes = 10, nickels = 8)
        val winner = Player(name = "Champion", winningCards = 4)

        val result = useCase(pot, listOf(winner))

        assertEquals(1, result.shares.size)
        val share = result.shares.first()
        assertEquals(640, share.targetCents)
        assertEquals(640, share.actualCents)
        assertEquals(20, share.coins.quarters)
        assertEquals(10, share.coins.dimes)
        assertEquals(8, share.coins.nickels)
        assertEquals(0, result.rolloverPot.totalCoins)
        assertTrue(result.isExactMatch)
    }

    @Test
    fun `two winners with 2 cards each split pot evenly`() {
        // Pot: 20 quarters ($5.00), 10 dimes ($1.00), 8 nickels ($0.40) = $6.40
        // Per share = $6.40 / 4 = $1.60. 2 cards = $3.20 each.
        val pot = CoinPot(quarters = 20, dimes = 10, nickels = 8)
        val p1 = Player(name = "Alice", winningCards = 2)
        val p2 = Player(name = "Bob", winningCards = 2)

        val result = useCase(pot, listOf(p1, p2))

        assertEquals(2, result.shares.size)
        assertEquals(320, result.shares[0].targetCents)
        assertEquals(320, result.shares[0].actualCents)
        assertEquals(320, result.shares[1].targetCents)
        assertEquals(320, result.shares[1].actualCents)
        assertEquals(0, result.rolloverPot.totalCoins)
        assertTrue(result.isExactMatch)
    }

    @Test
    fun `two winners with 3 and 1 cards receive 75 percent and 25 percent`() {
        // Pot: 16 quarters ($4.00), 12 dimes ($1.20), 8 nickels ($0.40) = $5.60
        // Per card = $1.40.
        // P1 (3 cards) = $4.20. P2 (1 card) = $1.40.
        val pot = CoinPot(quarters = 16, dimes = 12, nickels = 8)
        val p1 = Player(name = "High Roller", winningCards = 3)
        val p2 = Player(name = "Lucky", winningCards = 1)

        val result = useCase(pot, listOf(p1, p2))

        assertEquals(420, result.shares[0].actualCents)
        assertEquals(140, result.shares[1].actualCents)
        assertEquals(pot.totalCents, result.totalCentsDistributed)
        assertTrue(result.isExactMatch)
    }

    @Test
    fun `four winners holding 1 card each receive 25 percent each`() {
        // Pot: 12 quarters, 8 dimes, 8 nickels = 300 + 80 + 40 = 420 cents ($4.20)
        // Per card = $1.05
        val pot = CoinPot(quarters = 12, dimes = 8, nickels = 8)
        val players = listOf(
            Player(name = "Player 1", winningCards = 1),
            Player(name = "Player 2", winningCards = 1),
            Player(name = "Player 3", winningCards = 1),
            Player(name = "Player 4", winningCards = 1)
        )

        val result = useCase(pot, players)

        assertEquals(4, result.shares.size)
        result.shares.forEach { share ->
            assertEquals(105, share.actualCents)
            assertEquals(105, share.targetCents)
            assertTrue(share.isExact)
        }
        assertEquals(420, result.totalCentsDistributed)
        assertEquals(0, result.rolloverPot.totalCoins)
        assertTrue(result.isExactMatch)
    }

    @Test
    fun `extra nickel is placed back in the pot when pot does not divide evenly by 4`() {
        // Pot: 9 quarters ($2.25), 9 dimes ($0.90), 8 nickels ($0.40) = $3.55 (355 cents)
        // 355 / 20 = 17. 17 * 5 = 85 cents per card ($0.85).
        // 4 cards * 85 cents = 340 cents.
        // Remainder = 355 - 340 = 15 cents (3 extra nickels).
        val pot = CoinPot(quarters = 9, dimes = 9, nickels = 8)
        val players = listOf(
            Player(name = "P1", winningCards = 2),
            Player(name = "P2", winningCards = 1),
            Player(name = "P3", winningCards = 1)
        )

        val result = useCase(pot, players)

        assertEquals(15, result.rolloverPot.totalCents)
        assertEquals(340, result.totalCentsDistributed)
        assertEquals(170, result.shares[0].actualCents) // 2 cards = $1.70
        assertEquals(85, result.shares[1].actualCents)  // 1 card = $0.85
        assertEquals(85, result.shares[2].actualCents)  // 1 card = $0.85
        assertTrue(result.message!!.contains("placed back in the pot"))
        assertTrue(result.isExactMatch)
    }

    @Test
    fun `exact single extra nickel rollover case`() {
        // Pot: 9 quarters = 225 cents ($2.25)
        // 225 / 20 = 11. 11 * 5 = 55 cents per card ($0.55).
        // 4 * 55 = 220 cents. Remainder = 5 cents (exact 1 nickel).
        // Pot has 8 quarters + 1 quarter (25c), or let's provide nickels:
        // Pot: 8 quarters (200c), 5 nickels (25c) = 225 cents.
        val pot = CoinPot(quarters = 8, dimes = 0, nickels = 5)
        val p1 = Player(name = "P1", winningCards = 4)

        val result = useCase(pot, listOf(p1))

        assertEquals(5, result.rolloverPot.totalCents)
        assertEquals(1, result.rolloverPot.nickels)
        assertEquals(220, result.shares[0].actualCents)
        assertTrue(result.message!!.contains("Extra nickel ($0.05) placed back in the pot"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws exception if total cards do not equal 4`() {
        val pot = CoinPot(quarters = 10, dimes = 5, nickels = 5)
        val players = listOf(
            Player(name = "P1", winningCards = 2),
            Player(name = "P2", winningCards = 1)
            // Total = 3 cards!
        )
        useCase(pot, players)
    }
}
