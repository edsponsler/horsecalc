package com.horsecalc.app.vision

import com.horsecalc.app.model.Coin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating relative coin sizing and denomination classification.
 *
 * Verifies that the classifier handles:
 * - Single-denomination pots (e.g. only quarters or only dimes) without forcing 3 clusters.
 * - Two-denomination pots (e.g. dimes and quarters only, or nickels and quarters only).
 * - Three-denomination pots (quarters, nickels, dimes) with noisy physical radius measurements.
 */
class CoinClassificationTest {

    @Test
    fun testSingleDenominationQuarters() {
        // 5 quarters with subtle measurement noise
        val radii = listOf(22.0f, 22.4f, 21.9f, 22.6f, 22.1f)
        val classifications = CvCoinDetector.classifyRadii(radii)

        assertEquals(5, classifications.size)
        assertTrue(classifications.all { it == Coin.QUARTER })
    }

    @Test
    fun testSingleDenominationDimes() {
        // 4 dimes with subtle noise
        val radii = listOf(16.0f, 16.2f, 15.9f, 16.3f)
        val classifications = CvCoinDetector.classifyRadii(radii)

        assertEquals(4, classifications.size)
        assertTrue(classifications.all { it == Coin.DIME })
    }

    @Test
    fun testSingleDenominationNickels() {
        // 4 nickels with subtle noise
        val radii = listOf(19.0f, 19.3f, 19.1f, 18.9f)
        val classifications = CvCoinDetector.classifyRadii(radii)

        assertEquals(4, classifications.size)
        assertTrue(classifications.all { it == Coin.NICKEL })
    }

    @Test
    fun testTwoDenominationsDimesAndQuarters() {
        // 3 dimes (~16px) and 3 quarters (~22px), NO nickels
        val radii = listOf(16.0f, 16.2f, 16.1f, 22.0f, 22.3f, 22.1f)
        val classifications = CvCoinDetector.classifyRadii(radii)

        val dimes = classifications.count { it == Coin.DIME }
        val nickels = classifications.count { it == Coin.NICKEL }
        val quarters = classifications.count { it == Coin.QUARTER }

        assertEquals(3, dimes)
        assertEquals(0, nickels)
        assertEquals(3, quarters)
    }

    @Test
    fun testTwoDenominationsNickelsAndQuarters() {
        // 2 nickels (~19.2px) and 3 quarters (~22.2px)
        val radii = listOf(19.0f, 19.3f, 22.1f, 22.4f, 22.0f)
        val classifications = CvCoinDetector.classifyRadii(radii)

        val dimes = classifications.count { it == Coin.DIME }
        val nickels = classifications.count { it == Coin.NICKEL }
        val quarters = classifications.count { it == Coin.QUARTER }

        assertEquals(0, dimes)
        assertEquals(2, nickels)
        assertEquals(3, quarters)
    }

    @Test
    fun testThreeDenominationsMixedPot() {
        // Standard mixed pot: 4 dimes, 3 nickels, 5 quarters
        val dimeRadii = listOf(16.0f, 16.2f, 15.8f, 16.3f)
        val nickelRadii = listOf(19.0f, 19.2f, 18.9f)
        val quarterRadii = listOf(22.1f, 22.5f, 21.9f, 22.4f, 22.2f)

        val allRadii = (dimeRadii + nickelRadii + quarterRadii).shuffled()
        val classifications = CvCoinDetector.classifyRadii(allRadii)

        val dimes = classifications.count { it == Coin.DIME }
        val nickels = classifications.count { it == Coin.NICKEL }
        val quarters = classifications.count { it == Coin.QUARTER }

        assertEquals(4, dimes)
        assertEquals(3, nickels)
        assertEquals(5, quarters)
    }

    @Test
    fun testPerspectiveTiltCorrectionWoodTable() {
        // Ground truth 23 coins from real wooden table photo with ~13% perspective tilt
        data class TestCoin(val r: Float, val x: Float, val y: Float, val expected: Coin)

        val coins = listOf(
            TestCoin(65.5f, 466f, 398f, Coin.QUARTER),
            TestCoin(70.0f, 634f, 610f, Coin.QUARTER),
            TestCoin(69.0f, 214f, 710f, Coin.QUARTER),
            TestCoin(70.0f, 450f, 634f, Coin.QUARTER),
            TestCoin(61.0f, 246f, 290f, Coin.QUARTER),
            TestCoin(64.0f, 582f, 314f, Coin.QUARTER),
            TestCoin(51.5f, 334f, 670f, Coin.DIME),
            TestCoin(53.0f, 438f, 762f, Coin.DIME),
            TestCoin(47.5f, 278f, 578f, Coin.DIME),
            TestCoin(49.0f, 334f, 786f, Coin.DIME),
            TestCoin(59.5f, 614f, 446f, Coin.NICKEL),
            TestCoin(60.5f, 514f, 518f, Coin.NICKEL),
            TestCoin(62.0f, 318f, 186f, Coin.QUARTER),
            TestCoin(66.0f, 378f, 506f, Coin.QUARTER),
            TestCoin(47.0f, 298f, 418f, Coin.DIME),
            TestCoin(47.5f, 430f, 170f, Coin.DIME),
            TestCoin(57.0f, 214f, 490f, Coin.NICKEL),
            TestCoin(56.5f, 466f, 262f, Coin.NICKEL),
            TestCoin(62.5f, 554f, 726f, Coin.NICKEL),
            TestCoin(54.0f, 542f, 178f, Coin.NICKEL),
            TestCoin(56.0f, 362f, 322f, Coin.NICKEL),
            TestCoin(47.5f, 186f, 386f, Coin.DIME),
            TestCoin(57.0f, 154f, 598f, Coin.NICKEL)
        )

        val radii = coins.map { it.r }
        val centers = coins.map { Pair(it.x, it.y) }
        val classifications = CvCoinDetector.classifyRadii(
            radii = radii,
            nominalDime = 48.0f,
            centers = centers,
            imageWidth = 768f,
            imageHeight = 1020f
        )

        assertEquals(23, classifications.size)
        val quarters = classifications.count { it == Coin.QUARTER }
        val dimes = classifications.count { it == Coin.DIME }
        val nickels = classifications.count { it == Coin.NICKEL }

        assertEquals(8, quarters)
        assertEquals(7, dimes)
        assertEquals(8, nickels)

        for (i in coins.indices) {
            assertEquals("Mismatch at coin #$i", coins[i].expected, classifications[i])
        }
    }

    @Test
    fun testPerspectiveTiltCorrectionTablecloth() {
        // Ground truth 21 coins from real tablecloth photo (7 Quarters, 8 Dimes, 6 Nickels)
        data class TestCoin(val r: Float, val x: Float, val y: Float, val expected: Coin)

        val coins = listOf(
            TestCoin(54.5f, 346f, 470f, Coin.QUARTER),
            TestCoin(42.0f, 514f, 630f, Coin.DIME),
            TestCoin(57.0f, 690f, 502f, Coin.QUARTER),
            TestCoin(44.0f, 574f, 698f, Coin.DIME),
            TestCoin(40.5f, 222f, 570f, Coin.DIME),
            TestCoin(41.5f, 298f, 602f, Coin.DIME),
            TestCoin(56.0f, 522f, 518f, Coin.QUARTER),
            TestCoin(41.0f, 406f, 630f, Coin.DIME),
            TestCoin(60.5f, 694f, 690f, Coin.QUARTER),
            TestCoin(55.5f, 126f, 546f, Coin.QUARTER),
            TestCoin(56.0f, 86f, 666f, Coin.QUARTER),
            TestCoin(42.0f, 418f, 542f, Coin.DIME),
            TestCoin(43.0f, 610f, 410f, Coin.DIME),
            TestCoin(51.0f, 454f, 398f, Coin.NICKEL),
            TestCoin(40.5f, 358f, 366f, Coin.DIME),
            TestCoin(56.5f, 610f, 310f, Coin.QUARTER),
            TestCoin(52.5f, 622f, 590f, Coin.NICKEL),
            TestCoin(52.5f, 374f, 722f, Coin.NICKEL),
            TestCoin(53.0f, 498f, 762f, Coin.NICKEL),
            TestCoin(51.5f, 214f, 670f, Coin.NICKEL),
            TestCoin(48.5f, 150f, 750f, Coin.NICKEL)
        )

        val radii = coins.map { it.r }
        val centers = coins.map { Pair(it.x, it.y) }
        val classifications = CvCoinDetector.classifyRadii(
            radii = radii,
            nominalDime = 42.0f,
            centers = centers,
            imageWidth = 768f,
            imageHeight = 1020f
        )

        assertEquals(21, classifications.size)
        val quarters = classifications.count { it == Coin.QUARTER }
        val dimes = classifications.count { it == Coin.DIME }
        val nickels = classifications.count { it == Coin.NICKEL }

        assertEquals(7, quarters)
        assertEquals(8, dimes)
        assertEquals(6, nickels)

        for (i in coins.indices) {
            assertEquals("Mismatch at tablecloth coin #$i", coins[i].expected, classifications[i])
        }
    }

    @Test
    fun testPerspectiveTiltCorrectionTablecloth2() {
        // Ground truth 33 coins from crowded tablecloth 2 photo (10 Quarters, 11 Dimes, 12 Nickels)
        data class TestCoin(val r: Float, val x: Float, val y: Float, val expected: Coin)

        val coins = listOf(
            // 10 Quarters
            TestCoin(47.75f, 498f, 518f, Coin.QUARTER),
            TestCoin(47.0f, 506f, 678f, Coin.QUARTER),
            TestCoin(45.75f, 510f, 354f, Coin.QUARTER),
            TestCoin(46.25f, 134f, 690f, Coin.QUARTER),
            TestCoin(46.75f, 418f, 734f, Coin.QUARTER),
            TestCoin(47.0f, 254f, 566f, Coin.QUARTER),
            TestCoin(45.25f, 170f, 390f, Coin.QUARTER),
            TestCoin(48.5f, 690f, 498f, Coin.QUARTER),
            TestCoin(46.0f, 310f, 158f, Coin.QUARTER),
            TestCoin(48.5f, 342f, 334f, Coin.QUARTER),
            // 11 Dimes
            TestCoin(35.5f, 426f, 342f, Coin.DIME),
            TestCoin(34.0f, 274f, 230f, Coin.DIME),
            TestCoin(35.0f, 326f, 610f, Coin.DIME),
            TestCoin(35.0f, 438f, 410f, Coin.DIME),
            TestCoin(34.0f, 274f, 470f, Coin.DIME),
            TestCoin(35.75f, 102f, 558f, Coin.DIME),
            TestCoin(33.5f, 158f, 274f, Coin.DIME),
            TestCoin(35.75f, 382f, 562f, Coin.DIME),
            TestCoin(37.0f, 562f, 610f, Coin.DIME),
            TestCoin(35.0f, 586f, 478f, Coin.DIME),
            TestCoin(33.75f, 302f, 682f, Coin.DIME),
            // 12 Nickels
            TestCoin(41.75f, 378f, 242f, Coin.NICKEL),
            TestCoin(43.0f, 186f, 474f, Coin.NICKEL),
            TestCoin(43.0f, 614f, 550f, Coin.NICKEL),
            TestCoin(41.25f, 258f, 378f, Coin.NICKEL),
            TestCoin(41.75f, 466f, 274f, Coin.NICKEL),
            TestCoin(41.75f, 402f, 162f, Coin.NICKEL),
            TestCoin(42.0f, 166f, 602f, Coin.NICKEL),
            TestCoin(41.75f, 602f, 402f, Coin.NICKEL),
            TestCoin(41.0f, 234f, 298f, Coin.NICKEL),
            TestCoin(43.25f, 366f, 462f, Coin.NICKEL),
            TestCoin(41.5f, 226f, 686f, Coin.NICKEL),
            TestCoin(41.75f, 442f, 602f, Coin.NICKEL)
        )

        val radii = coins.map { it.r }
        val centers = coins.map { Pair(it.x, it.y) }
        val classifications = CvCoinDetector.classifyRadii(
            radii = radii,
            nominalDime = 35.0f,
            centers = centers,
            imageWidth = 768f,
            imageHeight = 1020f
        )

        assertEquals(33, classifications.size)
        val quarters = classifications.count { it == Coin.QUARTER }
        val dimes = classifications.count { it == Coin.DIME }
        val nickels = classifications.count { it == Coin.NICKEL }

        assertEquals(10, quarters)
        assertEquals(11, dimes)
        assertEquals(12, nickels)

        for (i in coins.indices) {
            assertEquals("Mismatch at tablecloth 2 coin #$i", coins[i].expected, classifications[i])
        }
    }
}

