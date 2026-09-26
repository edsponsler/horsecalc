package com.horsecalc.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horsecalc.app.ui.components.PayoutCard
import com.horsecalc.app.ui.theme.CoinBronze
import com.horsecalc.app.ui.theme.DerbyGold
import com.horsecalc.app.ui.theme.SuccessGreen
import com.horsecalc.app.ui.theme.SurfaceCard
import com.horsecalc.app.ui.theme.SurfaceCardBorder
import com.horsecalc.app.ui.theme.TextMuted
import com.horsecalc.app.ui.theme.TextPrimary
import com.horsecalc.app.ui.theme.TextSecondary
import com.horsecalc.app.ui.theme.TurfGreenPrimary
import com.horsecalc.app.ui.viewmodel.AppStep
import com.horsecalc.app.ui.viewmodel.HorseCalcEvent
import com.horsecalc.app.ui.viewmodel.HorseCalcUiState

@Composable
fun PayoutScreen(
    state: HorseCalcUiState,
    onEvent: (HorseCalcEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val result = state.distributionResult ?: return
    val scrollState = rememberScrollState()

    val totalWinners = result.shares.size
    val paidWinners = result.shares.count { it.isHandedOut }
    val allPaid = paidWinners == totalWinners

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
                onClick = { onEvent(HorseCalcEvent.NavigateToStep(AppStep.WINNERS_SETUP)) },
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
                    text = "Pot Distribution",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Exact coins per winner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pot Summary Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Pot Paid Out",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = result.formattedDistributed,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Coins Handed Out",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "$paidWinners of $totalWinners winners",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (allPaid) SuccessGreen else DerbyGold
                        )
                    }
                }

                // Extra Nickel / Rollover Callout
                if (result.rolloverCents > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DerbyGold.copy(alpha = 0.15f))
                            .border(1.dp, DerbyGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = DerbyGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (result.rolloverCents == 5) "Extra Nickel Left in Pot" else "Extra Change Left in Pot",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DerbyGold
                                )
                                Text(
                                    text = "${result.formattedRollover} (${result.rolloverPot.nickels} nickel${if (result.rolloverPot.nickels != 1) "s" else ""}) remains in the pot for the next game",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Winner Payouts (Tap when distributed)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Payout Cards for each winner
        result.shares.forEach { share ->
            PayoutCard(
                share = share,
                onToggleHandedOut = { onEvent(HorseCalcEvent.ToggleHandedOut(share.player.id)) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Next Game / Reset Button
        Button(
            onClick = { onEvent(HorseCalcEvent.ResetGame) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allPaid) TurfGreenPrimary else SurfaceCard
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.Autorenew, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (result.rolloverCents > 0)
                    "Start Next Game (Keep ${result.formattedRollover} in Pot)"
                else
                    "Start Next Game",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (allPaid) Color.White else TextPrimary
            )
        }
    }
}
