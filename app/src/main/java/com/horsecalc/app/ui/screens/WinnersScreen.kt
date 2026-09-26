package com.horsecalc.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horsecalc.app.ui.components.WinnerCardPicker
import com.horsecalc.app.ui.theme.AlertRed
import com.horsecalc.app.ui.theme.DerbyGold
import com.horsecalc.app.ui.theme.SuccessGreen
import com.horsecalc.app.ui.theme.SurfaceCard
import com.horsecalc.app.ui.theme.SurfaceCardBorder
import com.horsecalc.app.ui.theme.SurfaceHover
import com.horsecalc.app.ui.theme.TextMuted
import com.horsecalc.app.ui.theme.TextPrimary
import com.horsecalc.app.ui.theme.TextSecondary
import com.horsecalc.app.ui.theme.TurfGreenPrimary
import com.horsecalc.app.ui.viewmodel.AppStep
import com.horsecalc.app.ui.viewmodel.HorseCalcEvent
import com.horsecalc.app.ui.viewmodel.HorseCalcUiState
import com.horsecalc.app.ui.viewmodel.WinnersPreset

@Composable
fun WinnersScreen(
    state: HorseCalcUiState,
    onEvent: (HorseCalcEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val presetScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onEvent(HorseCalcEvent.NavigateToStep(AppStep.POT_INTAKE)) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Winning Cards",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Assign the 4 winning cards (25% each)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quota Status Card (4 cards total)
        val isQuotaFilled = state.isCardsAssignmentValid
        val statusBorder = if (isQuotaFilled) SuccessGreen else AlertRed

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.5.dp, statusBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isQuotaFilled) "Quota Complete: 4 of 4 Cards Assigned"
                               else "${state.totalWinningCardsAssigned} of 4 Cards Assigned",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isQuotaFilled) SuccessGreen else DerbyGold
                    )
                    Text(
                        text = if (isQuotaFilled) "Ready to calculate exact coin distribution"
                               else "Must equal exactly 4 winning cards matching the horse",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isQuotaFilled) SuccessGreen.copy(alpha = 0.2f) else DerbyGold.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${state.totalWinningCardsAssigned}/4",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isQuotaFilled) SuccessGreen else DerbyGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Presets Carousel
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(presetScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WinnersPreset.entries.forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHover)
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                        .clickable { onEvent(HorseCalcEvent.ApplyPreset(preset)) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Winners List
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Participating Winners",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (state.players.size < 4) {
                OutlinedButton(
                    onClick = { onEvent(HorseCalcEvent.AddPlayer("Player ${state.players.size + 1}")) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Player", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        state.players.forEach { player ->
            WinnerCardPicker(
                player = player,
                onCardsChange = { newCount ->
                    onEvent(HorseCalcEvent.UpdatePlayerCards(player.id, newCount))
                },
                onRemove = { onEvent(HorseCalcEvent.RemovePlayer(player.id)) },
                canRemove = state.players.size > 1,
                maxAllowedCards = 4 - (state.totalWinningCardsAssigned - player.winningCards),
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.errorMessage,
                color = AlertRed,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Calculate Button
        Button(
            onClick = { onEvent(HorseCalcEvent.CalculateDistribution) },
            enabled = state.isCardsAssignmentValid,
            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenPrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Calculate Coin Distribution",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
