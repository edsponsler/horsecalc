package com.horsecalc.app.vision

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.horsecalc.app.model.Coin
import com.horsecalc.app.model.CoinPot
import kotlinx.coroutines.delay

/**
 * Standard simulated coin detector for demonstration, testing, and fallback.
 * Simulates real-time image analysis latency and provides a realistic coin pot.
 */
class SimulatedCoinDetector : CoinDetector {

    override suspend fun detectCoins(imageUri: Uri, context: Context): CoinDetectionResult {
        // Simulate CV / ML processing delay
        delay(800)

        // Realistic game pot sample: e.g. 14 Quarters, 18 Dimes, 13 Nickels ($5.95)
        val samplePot = CoinPot(quarters = 14, dimes = 18, nickels = 13)

        val detectedList = mutableListOf<DetectedCoin>()
        repeat(samplePot.quarters) {
            detectedList.add(DetectedCoin(Coin.QUARTER, RectF(10f, 10f, 60f, 60f), 0.94f))
        }
        repeat(samplePot.dimes) {
            detectedList.add(DetectedCoin(Coin.DIME, RectF(70f, 10f, 110f, 50f), 0.91f))
        }
        repeat(samplePot.nickels) {
            detectedList.add(DetectedCoin(Coin.NICKEL, RectF(120f, 10f, 165f, 55f), 0.93f))
        }

        return CoinDetectionResult(
            pot = samplePot,
            detectedCoins = detectedList,
            confidenceScore = 0.93f,
            imageWidth = 600,
            imageHeight = 800
        )
    }
}
