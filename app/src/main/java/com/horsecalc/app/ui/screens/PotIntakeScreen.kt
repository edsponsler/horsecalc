package com.horsecalc.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.horsecalc.app.model.Coin
import com.horsecalc.app.ui.components.CoinCounterRow
import com.horsecalc.app.ui.theme.DerbyGold
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
import java.io.File

@Composable
fun PotIntakeScreen(
    state: HorseCalcUiState,
    onEvent: (HorseCalcEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var imageIntrinsicRatio by remember { mutableFloatStateOf(1f) }
    var showFullscreenModal by remember { mutableStateOf(false) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onEvent(HorseCalcEvent.ImageCaptured(it, context)) }
    }

    // Direct Camera Capture launcher (Full Resolution via FileProvider)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraTempUri?.let { onEvent(HorseCalcEvent.ImageCaptured(it, context)) }
        }
    }

    fun launchCamera() {
        try {
            val photoFile = File(context.cacheDir, "pot_snap_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraTempUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            galleryLauncher.launch("image/*")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Horse Race Pot",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Capture or verify pot coins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DerbyGold.copy(alpha = 0.15f))
                    .border(1.dp, DerbyGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = state.pot.formattedTotal,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = DerbyGold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Camera / Image Intake Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.capturedImageUri != null) {
                    // Photo Preview Container - Uses ContentScale.Fit so NO coins are ever cropped out!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 320.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceHover)
                            .clickable { showFullscreenModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = state.capturedImageUri,
                            contentDescription = "Pot image",
                            contentScale = ContentScale.Fit,
                            onSuccess = { res ->
                                val d = res.result.drawable
                                if (d.intrinsicHeight > 0) {
                                    imageIntrinsicRatio = d.intrinsicWidth.toFloat() / d.intrinsicHeight.toFloat()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 320.dp)
                        )

                        // Computer Vision Detection Overlay Rings
                        if (state.showDetectionOverlays && state.detectedCoins.isNotEmpty()) {
                            Canvas(
                                modifier = Modifier.matchParentSize()
                            ) {
                                val canvasW = size.width
                                val canvasH = size.height
                                val effectiveRatio = state.imageAspectRatio ?: imageIntrinsicRatio
                                if (canvasW > 0 && canvasH > 0 && effectiveRatio > 0) {
                                    val canvasRatio = canvasW / canvasH
                                    val (fitW, fitH, offsetX, offsetY) = if (canvasRatio > effectiveRatio) {
                                        val h = canvasH
                                        val w = h * effectiveRatio
                                        listOf(w, h, (canvasW - w) / 2f, 0f)
                                    } else {
                                        val w = canvasW
                                        val h = w / effectiveRatio
                                        listOf(w, h, 0f, (canvasH - h) / 2f)
                                    }

                                    for (detected in state.detectedCoins) {
                                        val ringColor = when (detected.coin) {
                                            Coin.QUARTER -> Color(0xFFFFD700) // Gold
                                            Coin.DIME -> Color(0xFF4FC3F7)    // Cyan / Silver
                                            Coin.NICKEL -> Color(0xFFFFB74D)  // Bronze / Amber
                                        }
                                        val cx = offsetX + ((detected.bounds.left + detected.bounds.right) / 2f) * fitW
                                        val cy = offsetY + ((detected.bounds.top + detected.bounds.bottom) / 2f) * fitH
                                        val rx = ((detected.bounds.right - detected.bounds.left) / 2f) * fitW
                                        val ry = ((detected.bounds.bottom - detected.bounds.top) / 2f) * fitH
                                        val r = (rx + ry) / 2f

                                        // Draw detection halo
                                        drawCircle(
                                            color = ringColor.copy(alpha = 0.25f),
                                            radius = r,
                                            center = Offset(cx, cy)
                                        )
                                        // Draw crisp bounding ring
                                        drawCircle(
                                            color = ringColor.copy(alpha = 0.95f),
                                            radius = r,
                                            center = Offset(cx, cy),
                                            style = Stroke(width = 2.5.dp.toPx())
                                        )
                                        // Center locator dot
                                        drawCircle(
                                            color = ringColor,
                                            radius = 3.dp.toPx(),
                                            center = Offset(cx, cy)
                                        )
                                    }
                                }
                            }
                        }

                        // Tap to expand hint
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Zoom",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tap to Inspect",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Analysis spinner overlay
                        if (state.isAnalyzingImage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SurfaceCard.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = DerbyGold)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Analyzing pot with Computer Vision...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detection overlay toolbar & status
                    if (state.detectedCoins.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TurfGreenPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CV Detected: ${state.detectedCoins.size} coins",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEvent(HorseCalcEvent.ToggleDetectionOverlays) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.showDetectionOverlays) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Overlays",
                                    tint = DerbyGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.showDetectionOverlays) "Tags On" else "Tags Off",
                                    fontSize = 12.sp,
                                    color = DerbyGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEvent(HorseCalcEvent.ClearImage) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Photo", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                state.capturedImageUri?.let {
                                    onEvent(HorseCalcEvent.ImageCaptured(it, context))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-scan")
                        }
                    }
                } else {
                    // Empty state photo prompt
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TurfGreenPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = DerbyGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Capture or Select Pot Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Full-frame image analysis automatically counts quarters, dimes, and nickels",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { launchCamera() },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Snap Photo")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From Gallery")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Coin Tally / Verification Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Coin Tally Verification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${state.pot.totalCoins} coins total",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Counter Rows for Quarters, Dimes, Nickels
        CoinCounterRow(
            coin = Coin.QUARTER,
            count = state.pot.quarters,
            onDeltaChange = { delta -> onEvent(HorseCalcEvent.UpdateCoinCount(Coin.QUARTER, delta)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        CoinCounterRow(
            coin = Coin.DIME,
            count = state.pot.dimes,
            onDeltaChange = { delta -> onEvent(HorseCalcEvent.UpdateCoinCount(Coin.DIME, delta)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        CoinCounterRow(
            coin = Coin.NICKEL,
            count = state.pot.nickels,
            onDeltaChange = { delta -> onEvent(HorseCalcEvent.UpdateCoinCount(Coin.NICKEL, delta)) }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Next Step Button
        Button(
            onClick = { onEvent(HorseCalcEvent.NavigateToStep(AppStep.WINNERS_SETUP)) },
            enabled = state.pot.totalCoins > 0,
            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenPrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Continue to Winners (${state.pot.formattedTotal})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }

    // Fullscreen Inspect Modal
    if (showFullscreenModal && state.capturedImageUri != null) {
        Dialog(
            onDismissRequest = { showFullscreenModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                AsyncImage(
                    model = state.capturedImageUri,
                    contentDescription = "Full-size pot inspection",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                IconButton(
                    onClick = { showFullscreenModal = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
