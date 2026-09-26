package com.horsecalc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.horsecalc.app.model.Player
import com.horsecalc.app.ui.theme.AlertRed
import com.horsecalc.app.ui.theme.DerbyGold
import com.horsecalc.app.ui.theme.SurfaceCard
import com.horsecalc.app.ui.theme.SurfaceCardBorder
import com.horsecalc.app.ui.theme.TextMuted
import com.horsecalc.app.ui.theme.TextPrimary
import com.horsecalc.app.ui.theme.TurfGreenPrimary

@Composable
fun WinnerCardPicker(
    player: Player,
    onCardsChange: (Int) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    maxAllowedCards: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player Avatar & Card Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DerbyGold.copy(alpha = 0.15f))
                        .border(1.dp, DerbyGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏇",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${player.winningCards} ${if (player.winningCards == 1) "Card" else "Cards"} • ${player.sharePercentageText} of pot",
                        style = MaterialTheme.typography.bodySmall,
                        color = DerbyGold
                    )
                }
            }

            // Controls & remove
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { onCardsChange(player.winningCards - 1) },
                    enabled = player.winningCards > 1,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TurfGreenPrimary.copy(alpha = 0.4f),
                        contentColor = TextPrimary,
                        disabledContainerColor = SurfaceCardBorder,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease cards",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${player.winningCards}",
                    modifier = Modifier.padding(horizontal = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                FilledIconButton(
                    onClick = { onCardsChange(player.winningCards + 1) },
                    enabled = player.winningCards < maxAllowedCards,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TurfGreenPrimary,
                        contentColor = TextPrimary,
                        disabledContainerColor = SurfaceCardBorder,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase cards",
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (canRemove) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove winner",
                            tint = AlertRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
