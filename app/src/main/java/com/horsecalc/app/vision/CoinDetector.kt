package com.horsecalc.app.vision

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.horsecalc.app.model.Coin
import com.horsecalc.app.model.CoinPot

data class DetectedCoin(
    val coin: Coin,
    val bounds: RectF,
    val confidence: Float
)

data class CoinDetectionResult(
    val pot: CoinPot,
    val detectedCoins: List<DetectedCoin> = emptyList(),
    val confidenceScore: Float = 1.0f,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)

/**
 * Interface for analyzing an image of the pot and detecting the coins.
 * Supports on-device ML (e.g. ML Kit / TFLite / OpenCV) or cloud vision.
 */
interface CoinDetector {
    suspend fun detectCoins(imageUri: Uri, context: Context): CoinDetectionResult
}
