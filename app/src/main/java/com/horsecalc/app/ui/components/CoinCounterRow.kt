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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
import com.horsecalc.app.model.Coin
import com.horsecalc.app.ui.theme.CoinBronze
import com.horsecalc.app.ui.theme.CoinSilver
import com.horsecalc.app.ui.theme.DerbyGold
import com.horsecalc.app.ui.theme.SurfaceCard
import com.horsecalc.app.ui.theme.SurfaceCardBorder
import com.horsecalc.app.ui.theme.TextMuted
import com.horsecalc.app.ui.theme.TextPrimary
import com.horsecalc.app.ui.theme.TextSecondary
import com.horsecalc.app.ui.theme.TurfGreenPrimary

@Composable
fun CoinCounterRow(
    coin: Coin,
    count: Int,
    onDeltaChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (coinBg, coinText) = when (coin) {
        Coin.QUARTER -> CoinSilver to Color(0xFF1E293B)
        Coin.DIME -> CoinSilver.copy(alpha = 0.85f) to Color(0xFF1E293B)
        Coin.NICKEL -> CoinBronze to Color.White
    }

    val subtotalCents = count * coin.valueCents
    val formattedSubtotal = "$${String.format(java.util.Locale.US, "%.2f", subtotalCents / 100.0)}"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Coin icon & info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(coinBg)
                        .border(2.dp, DerbyGold.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (coin) {
                            Coin.QUARTER -> "25¢"
                            Coin.DIME -> "10¢"
                            Coin.NICKEL -> "5¢"
                        },
                        color = coinText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = coin.pluralName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Subtotal: $formattedSubtotal",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Controls: [-] Count [+]
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { onDeltaChange(-1) },
                    enabled = count > 0,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TurfGreenPrimary.copy(alpha = 0.5f),
                        contentColor = TextPrimary,
                        disabledContainerColor = SurfaceCardBorder,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease ${coin.displayName}",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "$count",
                    modifier = Modifier.padding(horizontal = 14.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                FilledIconButton(
                    onClick = { onDeltaChange(1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TurfGreenPrimary,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase ${coin.displayName}",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
