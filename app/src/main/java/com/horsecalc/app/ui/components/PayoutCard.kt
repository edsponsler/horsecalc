package com.horsecalc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import com.horsecalc.app.model.PayoutShare
import com.horsecalc.app.ui.theme.CoinBronze
import com.horsecalc.app.ui.theme.CoinSilver
import com.horsecalc.app.ui.theme.DerbyGold
import com.horsecalc.app.ui.theme.SuccessGreen
import com.horsecalc.app.ui.theme.SurfaceCard
import com.horsecalc.app.ui.theme.SurfaceCardBorder
import com.horsecalc.app.ui.theme.SurfaceHover
import com.horsecalc.app.ui.theme.TextMuted
import com.horsecalc.app.ui.theme.TextPrimary
import com.horsecalc.app.ui.theme.TextSecondary

@Composable
fun PayoutCard(
    share: PayoutShare,
    onToggleHandedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = share.isHandedOut

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDone) SurfaceHover.copy(alpha = 0.6f) else SurfaceCard)
            .border(
                1.5.dp,
                if (isDone) SuccessGreen.copy(alpha = 0.5f) else SurfaceCardBorder,
                RoundedCornerShape(18.dp)
            )
            .clickable { onToggleHandedOut() }
            .padding(18.dp)
    ) {
        Column {
            // Header: Player name + share % + Total $
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Checkbox indicator
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDone) SuccessGreen else Color.Transparent)
                            .border(
                                2.dp,
                                if (isDone) SuccessGreen else TextMuted,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Handed out",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = share.player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) TextSecondary else TextPrimary
                        )
                        Text(
                            text = "${share.player.winningCards} cards (${share.player.sharePercentageText})",
                            style = MaterialTheme.typography.bodySmall,
                            color = DerbyGold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = share.formattedActual,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDone) SuccessGreen else DerbyGold
                    )
                    Text(
                        text = "${share.coins.totalCoins} coins",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Denomination Breakdown Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoinPill(
                    label = "Quarters",
                    count = share.coins.quarters,
                    coinColor = CoinSilver,
                    textColor = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                CoinPill(
                    label = "Dimes",
                    count = share.coins.dimes,
                    coinColor = CoinSilver.copy(alpha = 0.85f),
                    textColor = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                CoinPill(
                    label = "Nickels",
                    count = share.coins.nickels,
                    coinColor = CoinBronze,
                    textColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CoinPill(
    label: String,
    count: Int,
    coinColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHover)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
