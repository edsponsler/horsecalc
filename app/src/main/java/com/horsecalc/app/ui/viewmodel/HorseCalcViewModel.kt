package com.horsecalc.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horsecalc.app.domain.DistributePotUseCase
import com.horsecalc.app.model.Coin
import com.horsecalc.app.model.CoinPot
import com.horsecalc.app.model.Player
import com.horsecalc.app.vision.CoinDetector
import com.horsecalc.app.vision.CvCoinDetector
import com.horsecalc.app.vision.SimulatedCoinDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HorseCalcViewModel(
    private val distributePotUseCase: DistributePotUseCase = DistributePotUseCase(),
    private val coinDetector: CoinDetector = CvCoinDetector()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HorseCalcUiState())
    val uiState: StateFlow<HorseCalcUiState> = _uiState.asStateFlow()

    fun onEvent(event: HorseCalcEvent) {
        when (event) {
            is HorseCalcEvent.UpdateCoinCount -> updateCoinDelta(event.coin, event.delta)
            is HorseCalcEvent.SetCoinCount -> setCoin(event.coin, event.count)
            is HorseCalcEvent.ImageCaptured -> processImage(event.uri, event.context)
            is HorseCalcEvent.ClearImage -> clearImage()
            is HorseCalcEvent.ToggleDetectionOverlays -> toggleDetectionOverlays()
            is HorseCalcEvent.AddPlayer -> addPlayer(event.name)
            is HorseCalcEvent.RemovePlayer -> removePlayer(event.playerId)
            is HorseCalcEvent.UpdatePlayerCards -> updateCards(event.playerId, event.cards)
            is HorseCalcEvent.UpdatePlayerName -> updateName(event.playerId, event.name)
            is HorseCalcEvent.ApplyPreset -> applyPreset(event.preset)
            is HorseCalcEvent.ToggleHandedOut -> toggleHandedOut(event.playerId)
            is HorseCalcEvent.NavigateToStep -> navigateTo(event.step)
            is HorseCalcEvent.CalculateDistribution -> calculateDistribution()
            is HorseCalcEvent.ResetGame -> resetGame()
        }
    }

    private fun updateCoinDelta(coin: Coin, delta: Int) {
        _uiState.update { current ->
            val p = current.pot
            val updatedPot = when (coin) {
                Coin.QUARTER -> p.copy(quarters = (p.quarters + delta).coerceAtLeast(0))
                Coin.DIME -> p.copy(dimes = (p.dimes + delta).coerceAtLeast(0))
                Coin.NICKEL -> p.copy(nickels = (p.nickels + delta).coerceAtLeast(0))
            }
            current.copy(pot = updatedPot)
        }
    }

    private fun setCoin(coin: Coin, count: Int) {
        _uiState.update { current ->
            val p = current.pot
            val validCount = count.coerceAtLeast(0)
            val updatedPot = when (coin) {
                Coin.QUARTER -> p.copy(quarters = validCount)
                Coin.DIME -> p.copy(dimes = validCount)
                Coin.NICKEL -> p.copy(nickels = validCount)
            }
            current.copy(pot = updatedPot)
        }
    }

    private fun processImage(uri: Uri, context: Context) {
        _uiState.update { it.copy(capturedImageUri = uri, isAnalyzingImage = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = coinDetector.detectCoins(uri, context)
                val ratio = if (result.imageHeight > 0) result.imageWidth.toFloat() / result.imageHeight.toFloat() else null
                _uiState.update {
                    it.copy(
                        pot = result.pot,
                        detectedCoins = result.detectedCoins,
                        detectionConfidence = result.confidenceScore,
                        imageAspectRatio = ratio,
                        isAnalyzingImage = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzingImage = false,
                        errorMessage = "Failed to detect coins from image: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun clearImage() {
        _uiState.update {
            it.copy(
                capturedImageUri = null,
                detectedCoins = emptyList(),
                detectionConfidence = null,
                imageAspectRatio = null
            )
        }
    }

    private fun toggleDetectionOverlays() {
        _uiState.update { it.copy(showDetectionOverlays = !it.showDetectionOverlays) }
    }

    private fun addPlayer(name: String) {
        _uiState.update { current ->
            if (current.players.size >= 4) return@update current
            val remainingCards = current.remainingCardsToAssign
            val initialCards = if (remainingCards > 0) 1 else 1
            val newPlayer = Player(name = name.ifBlank { "Player ${current.players.size + 1}" }, winningCards = initialCards)
            current.copy(players = current.players + newPlayer)
        }
    }

    private fun removePlayer(playerId: String) {
        _uiState.update { current ->
            if (current.players.size <= 1) return@update current
            current.copy(players = current.players.filterNot { it.id == playerId })
        }
    }

    private fun updateCards(playerId: String, cards: Int) {
        _uiState.update { current ->
            val validCards = cards.coerceIn(1, 4)
            val updated = current.players.map {
                if (it.id == playerId) it.copy(winningCards = validCards) else it
            }
            current.copy(players = updated)
        }
    }

    private fun updateName(playerId: String, name: String) {
        _uiState.update { current ->
            val updated = current.players.map {
                if (it.id == playerId) it.copy(name = name) else it
            }
            current.copy(players = updated)
        }
    }

    private fun applyPreset(preset: WinnersPreset) {
        val newPlayers = when (preset) {
            WinnersPreset.SOLO -> listOf(
                Player(name = "Champion", winningCards = 4)
            )
            WinnersPreset.SPLIT_2_2 -> listOf(
                Player(name = "Winner 1", winningCards = 2),
                Player(name = "Winner 2", winningCards = 2)
            )
            WinnersPreset.SPLIT_3_1 -> listOf(
                Player(name = "Winner 1", winningCards = 3),
                Player(name = "Winner 2", winningCards = 1)
            )
            WinnersPreset.SPLIT_2_1_1 -> listOf(
                Player(name = "Winner 1", winningCards = 2),
                Player(name = "Winner 2", winningCards = 1),
                Player(name = "Winner 3", winningCards = 1)
            )
            WinnersPreset.FOUR_WAY -> listOf(
                Player(name = "Winner 1", winningCards = 1),
                Player(name = "Winner 2", winningCards = 1),
                Player(name = "Winner 3", winningCards = 1),
                Player(name = "Winner 4", winningCards = 1)
            )
        }
        _uiState.update { it.copy(players = newPlayers, errorMessage = null) }
    }

    private fun toggleHandedOut(playerId: String) {
        _uiState.update { current ->
            val res = current.distributionResult ?: return@update current
            val updatedShares = res.shares.map {
                if (it.player.id == playerId) it.copy(isHandedOut = !it.isHandedOut) else it
            }
            current.copy(distributionResult = res.copy(shares = updatedShares))
        }
    }

    private fun navigateTo(step: AppStep) {
        _uiState.update { it.copy(step = step) }
    }

    private fun calculateDistribution() {
        val current = _uiState.value
        if (!current.isCardsAssignmentValid) {
            _uiState.update {
                it.copy(errorMessage = "Total winning cards must equal exactly 4. Currently assigned: ${current.totalWinningCardsAssigned}")
            }
            return
        }

        try {
            val result = distributePotUseCase(current.pot, current.players)
            _uiState.update {
                it.copy(
                    distributionResult = result,
                    step = AppStep.PAYOUT_SUMMARY,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = e.localizedMessage ?: "Failed to calculate distribution")
            }
        }
    }

    private fun resetGame() {
        _uiState.update {
            val rolloverPot = it.distributionResult?.rolloverPot ?: CoinPot()
            HorseCalcUiState(
                step = AppStep.POT_INTAKE,
                pot = rolloverPot,
                players = listOf(
                    Player(name = "Player 1", winningCards = 2),
                    Player(name = "Player 2", winningCards = 2)
                )
            )
        }
    }
}
