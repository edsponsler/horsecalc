package com.horsecalc.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.horsecalc.app.model.Coin

sealed interface HorseCalcEvent {
    data class UpdateCoinCount(val coin: Coin, val delta: Int) : HorseCalcEvent
    data class SetCoinCount(val coin: Coin, val count: Int) : HorseCalcEvent
    data class ImageCaptured(val uri: Uri, val context: Context) : HorseCalcEvent
    data object ClearImage : HorseCalcEvent
    data object ToggleDetectionOverlays : HorseCalcEvent

    data class AddPlayer(val name: String) : HorseCalcEvent
    data class RemovePlayer(val playerId: String) : HorseCalcEvent
    data class UpdatePlayerCards(val playerId: String, val cards: Int) : HorseCalcEvent
    data class UpdatePlayerName(val playerId: String, val name: String) : HorseCalcEvent
    data class ApplyPreset(val preset: WinnersPreset) : HorseCalcEvent

    data class ToggleHandedOut(val playerId: String) : HorseCalcEvent
    data class NavigateToStep(val step: AppStep) : HorseCalcEvent
    data object CalculateDistribution : HorseCalcEvent
    data object ResetGame : HorseCalcEvent
}

enum class WinnersPreset(val displayName: String, val description: String) {
    SOLO("1 Winner (100%)", "1 player holds all 4 winning cards"),
    SPLIT_2_2("2 Winners (50% / 50%)", "2 players hold 2 cards each"),
    SPLIT_3_1("2 Winners (75% / 25%)", "1 player holds 3 cards, 1 holds 1 card"),
    SPLIT_2_1_1("3 Winners (50% / 25% / 25%)", "1 player with 2 cards, 2 with 1 card"),
    FOUR_WAY("4 Winners (25% each)", "4 players hold 1 card each")
}
