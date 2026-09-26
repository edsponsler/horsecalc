package com.horsecalc.app.ui.viewmodel

import android.net.Uri
import com.horsecalc.app.model.CoinPot
import com.horsecalc.app.model.DistributionResult
import com.horsecalc.app.model.Player

import com.horsecalc.app.vision.DetectedCoin

enum class AppStep {
    POT_INTAKE,
    WINNERS_SETUP,
    PAYOUT_SUMMARY
}

data class HorseCalcUiState(
    val step: AppStep = AppStep.POT_INTAKE,
    val pot: CoinPot = CoinPot(quarters = 16, dimes = 10, nickels = 8),
    val capturedImageUri: Uri? = null,
    val isAnalyzingImage: Boolean = false,
    val detectedCoins: List<DetectedCoin> = emptyList(),
    val detectionConfidence: Float? = null,
    val imageAspectRatio: Float? = null,
    val showDetectionOverlays: Boolean = true,
    val players: List<Player> = listOf(
        Player(name = "Player 1", winningCards = 2),
        Player(name = "Player 2", winningCards = 2)
    ),
    val distributionResult: DistributionResult? = null,
    val errorMessage: String? = null
) {
    val totalWinningCardsAssigned: Int
        get() = players.sumOf { it.winningCards }

    val isCardsAssignmentValid: Boolean
        get() = totalWinningCardsAssigned == 4

    val remainingCardsToAssign: Int
        get() = 4 - totalWinningCardsAssigned
}
