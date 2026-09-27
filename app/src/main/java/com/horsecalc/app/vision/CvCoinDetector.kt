package com.horsecalc.app.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import com.horsecalc.app.model.Coin
import com.horsecalc.app.model.CoinPot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot

/**
 * On-device Computer Vision Coin Detector for physical game pots.
 *
 * Analyzes top-down pot photos by:
 * 1. Correcting for EXIF orientation so portrait/landscape photos match the screen.
 * 2. Downsampling to a balanced resolution (~600-800px) for fast sub-second analysis.
 * 3. Detecting circular coin contours using radial gradient accumulator voting (Hough Transform).
 * 4. Filtering bare surface background while accommodating warm ambient room lighting.
 * 5. Suppressing overlapping / duplicate detections via Non-Maximum Suppression (NMS).
 * 6. Classifying coins into Quarters (24.26mm), Nickels (21.21mm), and Dimes (17.91mm)
 *    using 1D K-Means radius clustering calibrated to US coin physical ratios.
 */
class CvCoinDetector : CoinDetector {

    override suspend fun detectCoins(imageUri: Uri, context: Context): CoinDetectionResult =
        withContext(Dispatchers.Default) {
            val bitmap = loadAndOrientBitmap(imageUri, context)
                ?: return@withContext CoinDetectionResult(
                    pot = CoinPot(),
                    detectedCoins = emptyList(),
                    confidenceScore = 0f,
                    imageWidth = 0,
                    imageHeight = 0
                )

            val width = bitmap.width
            val height = bitmap.height

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val gray = FloatArray(width * height)
            val isBareSurface = BooleanArray(width * height)

            // Sample border pixels to detect background surface type
            var borderLumSum = 0f
            var borderRSum = 0f
            var borderBSum = 0f
            var borderCount = 0
            val borderStep = if (width > 500) 8 else 4
            val borderMargin = if (width > 500) 30 else 15
            for (y in 0 until height step borderStep) {
                for (x in 0 until width step borderStep) {
                    if (x < borderMargin || x > width - borderMargin || y < borderMargin || y > height - borderMargin) {
                        val c = pixels[y * width + x]
                        val r = (c shr 16) and 0xFF
                        val g = (c shr 8) and 0xFF
                        val b = c and 0xFF
                        borderLumSum += (0.299f * r + 0.587f * g + 0.114f * b)
                        borderRSum += r
                        borderBSum += b
                        borderCount++
                    }
                }
            }
            val avgBgLum = if (borderCount > 0) borderLumSum / borderCount else 128f
            val avgBgR = if (borderCount > 0) borderRSum / borderCount else 128f
            val avgBgB = if (borderCount > 0) borderBSum / borderCount else 128f

            val isDarkBackground = avgBgLum < 65.0f
            val isWoodTable = !isDarkBackground && (avgBgR - avgBgB > 45 && avgBgR > 2.0f * avgBgB)

            for (i in pixels.indices) {
                val color = pixels[i]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                gray[i] = lum

                if (isDarkBackground) {
                    // On dark/grey mat, pixels near background luminance are bare surface
                    isBareSurface[i] = (lum < avgBgLum + 22.0f)
                } else if (isWoodTable) {
                    // Bare wood table has high warm saturation and medium/dark lum
                    val diffRb = r - b
                    isBareSurface[i] = (diffRb > 55 && r > 1.42f * b && lum < 195f)
                } else {
                    // Tablecloth / fabric: neutral surface
                    isBareSurface[i] = false
                }
            }

            // Compute Sobel gradients across all coin / boundary pixels
            val edges = mutableListOf<EdgePoint>()
            val gradThreshold = if (isDarkBackground) 30.0f else 30.0f

            val step = if (width > 500) 2 else 1
            for (y in 2 until height - 2 step step) {
                val rowIdx = y * width
                for (x in 2 until width - 2 step step) {
                    val idx = rowIdx + x
                    // Ignore gradient if in bare surface background
                    if (isBareSurface[idx]) continue

                    val sx = gray[idx + 1] - gray[idx - 1]
                    val sy = gray[idx + width] - gray[idx - width]
                    val mag = hypot(sx, sy)

                    if (mag > gradThreshold) {
                        edges.add(EdgePoint(x, y, sx / mag, sy / mag))
                    }
                }
            }

            // Estimate radius range based on image dimensions
            val minRadius = (width * 0.024f).toInt().coerceAtLeast(if (width > 500) 26 else 14)
            val maxRadius = (width * 0.095f).toInt().coerceAtLeast(minRadius + 24)
            val binSize = if (width > 500) 4 else 3

            // 2D Accumulator voting for circle centers
            val accGrid = HashMap<Long, HashMap<Int, Int>>()

            for (edge in edges) {
                for (r in minRadius..maxRadius step 2) {
                    for (sign in intArrayOf(-1, 1)) {
                        val cx = (edge.x + sign * r * edge.dx).toInt()
                        val cy = (edge.y + sign * r * edge.dy).toInt()

                        if (cx in 0 until width && cy in 0 until height) {
                            val cIdx = cy * width + cx
                            // Center of coin should not be bare background
                            if (!isBareSurface[cIdx]) {
                                val cellKey = (cx / binSize).toLong() shl 32 or (cy / binSize).toLong()
                                val rMap = accGrid.getOrPut(cellKey) { HashMap() }
                                rMap[r] = (rMap[r] ?: 0) + 1
                            }
                        }
                    }
                }
            }

            // Neighbor smoothing to merge split votes across bin boundaries
            val smoothedGrid = HashMap<Long, HashMap<Int, Int>>()
            for ((cellKey, rMap) in accGrid) {
                val bx = (cellKey shr 32).toInt()
                val by = (cellKey and 0xFFFFFFFFL).toInt()

                for ((r, v) in rMap) {
                    val sMap = smoothedGrid.getOrPut(cellKey) { HashMap() }
                    sMap[r] = (sMap[r] ?: 0) + v

                    val neighbors = arrayOf(
                        Pair(bx + 1, by), Pair(bx - 1, by),
                        Pair(bx, by + 1), Pair(bx, by - 1)
                    )
                    for (n in neighbors) {
                        val nKey = (n.first.toLong() shl 32) or (n.second.toLong() and 0xFFFFFFFFL)
                        val nsMap = smoothedGrid.getOrPut(nKey) { HashMap() }
                        nsMap[r] = (nsMap[r] ?: 0) + (v / 2)
                    }
                }
            }

            // Peak extraction
            val minVotes = if (isDarkBackground || isWoodTable) 34 else 35
            val candidateCircles = mutableListOf<CircleCandidate>()

            for ((cellKey, rMap) in smoothedGrid) {
                val cx = ((cellKey shr 32).toInt()) * binSize + binSize / 2
                val cy = ((cellKey and 0xFFFFFFFFL).toInt()) * binSize + binSize / 2
                var bestR = 0
                var bestVotes = 0
                for ((r, votes) in rMap) {
                    if (votes > bestVotes) {
                        bestVotes = votes
                        bestR = r
                    }
                }
                if (bestVotes >= minVotes && cx in 0 until width && cy in 0 until height) {
                    val cIdx = cy * width + cx
                    if (!isBareSurface[cIdx]) {
                        candidateCircles.add(CircleCandidate(cx, cy, bestR, bestVotes))
                    }
                }
            }

            // Sort by vote count descending
            candidateCircles.sortByDescending { it.votes }

            // Non-Maximum Suppression (NMS) to eliminate duplicate overlapping circles and inner stamps
            val detected = mutableListOf<CircleCandidate>()
            for (cand in candidateCircles) {
                var overlaps = false
                for (det in detected) {
                    val dist = hypot((cand.cx - det.cx).toFloat(), (cand.cy - det.cy).toFloat())
                    val minDist = minOf(cand.radius, det.radius) * 1.25f
                    val concentricDist = (cand.radius + det.radius) * 0.65f
                    if (dist < minDist || dist < concentricDist || dist < det.radius * 0.85f) {
                        overlaps = true
                        break
                    }
                }
                if (!overlaps) {
                    detected.add(cand)
                }
            }

            if (detected.isEmpty()) {
                return@withContext CoinDetectionResult(
                    pot = CoinPot(),
                    detectedCoins = emptyList(),
                    confidenceScore = 0.5f,
                    imageWidth = width,
                    imageHeight = height
                )
            }

            // Sub-pixel radial gradient profile refinement (0.5px steps) and false pattern rejection
            val refinedDetected = mutableListOf<CircleCandidate>()
            for (cand in detected) {
                var bestR = cand.radius.toFloat()
                var maxGradSum = 0f
                val searchMin = (cand.radius - 2.0f).coerceAtLeast(minRadius.toFloat())
                val searchMax = (cand.radius + 2.0f).coerceAtMost(maxRadius.toFloat())

                var rTest = searchMin
                val samples = 64
                while (rTest <= searchMax) {
                    var gradSum = 0f
                    for (s in 0 until samples) {
                        val angle = s * (2.0 * Math.PI / samples)
                        val px = (cand.cx + rTest * kotlin.math.cos(angle)).toInt()
                        val py = (cand.cy + rTest * kotlin.math.sin(angle)).toInt()
                        if (px in 1 until width - 1 && py in 1 until height - 1) {
                            val idx = py * width + px
                            val sx = gray[idx + 1] - gray[idx - 1]
                            val sy = gray[idx + width] - gray[idx - width]
                            gradSum += hypot(sx, sy)
                        }
                    }
                    if (gradSum > maxGradSum) {
                        maxGradSum = gradSum
                        bestR = rTest
                    }
                    rTest += 0.5f
                }

                // Verification of perimeter radial gradient alignment
                var hitCount = 0
                var alignSum = 0f
                for (s in 0 until samples) {
                    val angle = s * (2.0 * Math.PI / samples)
                    val cosA = kotlin.math.cos(angle).toFloat()
                    val sinA = kotlin.math.sin(angle).toFloat()
                    val px = (cand.cx + bestR * cosA).toInt()
                    val py = (cand.cy + bestR * sinA).toInt()
                    if (px in 1 until width - 1 && py in 1 until height - 1) {
                        val idx = py * width + px
                        val sx = gray[idx + 1] - gray[idx - 1]
                        val sy = gray[idx + width] - gray[idx - width]
                        val mag = hypot(sx, sy)
                        if (mag > 25.0f) {
                            hitCount++
                            val dot = abs((sx / mag) * cosA + (sy / mag) * sinA)
                            alignSum += dot
                        }
                    }
                }
                val avgAlign = if (hitCount > 0) alignSum / hitCount else 0f

                // Interior texture and contrast check for low-vote candidates on fabric
                val innerR = (bestR * 0.5f).toInt()
                var innerLumSum = 0f
                var innerLumSq = 0f
                var innerCount = 0
                for (dy in -innerR..innerR) {
                    for (dx in -innerR..innerR) {
                        if (dx * dx + dy * dy <= innerR * innerR) {
                            val px = cand.cx + dx
                            val py = cand.cy + dy
                            if (px in 0 until width && py in 0 until height) {
                                val v = gray[py * width + px]
                                innerLumSum += v
                                innerLumSq += v * v
                                innerCount++
                            }
                        }
                    }
                }
                val avgInner = if (innerCount > 0) innerLumSum / innerCount else 0f
                val variance = if (innerCount > 0) (innerLumSq / innerCount) - (avgInner * avgInner) else 0f
                val stdDev = kotlin.math.sqrt(maxOf(0f, variance))

                // Rejection of false fabric pattern detections (e.g. plaid intersections)
                val isFabricFalseCircle = (!isDarkBackground && !isWoodTable) &&
                        (cand.votes < 40) &&
                        (avgAlign < 0.70f) &&
                        (stdDev > 38.0f || abs(avgInner - avgBgLum) < 18.0f)

                if (!isFabricFalseCircle) {
                    val finalRadius = 0.5f * cand.radius + 0.5f * bestR
                    refinedDetected.add(CircleCandidate(cand.cx, cand.cy, cand.radius, cand.votes, finalRadius))
                }
            }

            // Ratio-anchored denomination classification
            val detectedCoins = mutableListOf<DetectedCoin>()
            val radii = refinedDetected.map { it.refinedRadius }
            val centers = refinedDetected.map { Pair(it.cx.toFloat(), it.cy.toFloat()) }
            val nominalDime = (width * 0.042f).coerceAtLeast(14.0f)
            val classifications = classifyRadii(radii, nominalDime, centers, width.toFloat(), height.toFloat())

            var quarters = 0
            var nickels = 0
            var dimes = 0

            for (i in refinedDetected.indices) {
                val circle = refinedDetected[i]
                val coinType = classifications[i]
                when (coinType) {
                    Coin.QUARTER -> quarters++
                    Coin.NICKEL -> nickels++
                    Coin.DIME -> dimes++
                }

                val normLeft = ((circle.cx - circle.refinedRadius) / width).coerceIn(0f, 1f)
                val normTop = ((circle.cy - circle.refinedRadius) / height).coerceIn(0f, 1f)
                val normRight = ((circle.cx + circle.refinedRadius) / width).coerceIn(0f, 1f)
                val normBottom = ((circle.cy + circle.refinedRadius) / height).coerceIn(0f, 1f)

                detectedCoins.add(
                    DetectedCoin(
                        coin = coinType,
                        bounds = RectF(normLeft, normTop, normRight, normBottom),
                        confidence = (circle.votes / (circle.radius * 2f)).coerceIn(0.7f, 0.98f)
                    )
                )
            }

            val pot = CoinPot(
                quarters = quarters,
                dimes = dimes,
                nickels = nickels
            )

            CoinDetectionResult(
                pot = pot,
                detectedCoins = detectedCoins,
                confidenceScore = 0.96f,
                imageWidth = width,
                imageHeight = height
            )
        }

    private fun loadAndOrientBitmap(uri: Uri, context: Context): Bitmap? {
        return try {
            val contentResolver = context.contentResolver

            val orientation = contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val maxTargetDim = 1080
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxTargetDim || options.outHeight / sampleSize > maxTargetDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    decodedBitmap.width,
                    decodedBitmap.height,
                    matrix,
                    true
                )
            } else {
                decodedBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private data class EdgePoint(
        val x: Int,
        val y: Int,
        val dx: Float,
        val dy: Float
    )

    private data class CircleCandidate(
        val cx: Int,
        val cy: Int,
        val radius: Int,
        val votes: Int,
        val refinedRadius: Float = radius.toFloat()
    )

    companion object {
        // US coin physical diameter ratios relative to Dime (1.000):
        // Dime:    17.91 mm (1.0000x baseline)
        // Nickel:  21.21 mm (1.1843x Dime)
        // Quarter: 24.26 mm (1.3545x Dime)
        const val RATIO_NICKEL_DIME = 1.1843f
        const val RATIO_QUARTER_DIME = 1.3545f

        /**
         * Ratio-anchored classifier based on physical US Coin dimensions.
         * Dynamically determines whether 1, 2, or 3 coin denominations are present
         * without falsely forcing sparse sets into 3 clusters.
         *
         * @param radii List of detected circle radii in pixels
         * @param nominalDime Baseline expected dime radius for the camera perspective (defaults to 16.0px)
         */
        fun classifyRadii(
            radii: List<Float>,
            nominalDime: Float = 16.0f,
            centers: List<Pair<Float, Float>>? = null,
            imageWidth: Float = 0f,
            imageHeight: Float = 0f
        ): List<Coin> {
            if (radii.isEmpty()) return emptyList()
            if (radii.size == 1) {
                val r = radii[0]
                val b = r / nominalDime
                return listOf(
                    when {
                        r > nominalDime * 1.27f -> Coin.QUARTER
                        r < nominalDime * 1.09f -> Coin.DIME
                        else -> Coin.NICKEL
                    }
                )
            }

            val minR = radii.minOrNull() ?: return emptyList()
            val maxR = radii.maxOrNull() ?: return emptyList()
            val ratio = maxR / minR

            // If ratio is tight (< 9% spread), all detected coins belong to the same denomination
            if (ratio < 1.09f) {
                val avg = radii.average().toFloat()
                val coinType = when {
                    avg > nominalDime * 1.27f -> Coin.QUARTER
                    avg < nominalDime * 1.09f -> Coin.DIME
                    else -> Coin.NICKEL
                }
                return List(radii.size) { coinType }
            }

            // Perspective tilt modeling when coin locations and dimensions are available
            val fitTilt = centers != null && centers.size == radii.size && centers.size >= 6 && imageWidth > 0f && imageHeight > 0f
            if (fitTilt) {
                checkNotNull(centers)
                val centerX = imageWidth / 2.0f
                val centerY = imageHeight / 2.0f

                val axRange = floatArrayOf(-0.0002f, -0.0001f, 0f, 0.0001f, 0.0002f)
                val ayRange = floatArrayOf(-0.0003f, -0.0002f, -0.0001f, 0f, 0.0001f, 0.0002f, 0.0003f)

                var bestScore = Float.MAX_VALUE
                var bestAx = 0f
                var bestAy = 0f
                var bestB = nominalDime

                for (ay in ayRange) {
                    for (ax in axRange) {
                        val normRadii = ArrayList<Float>(radii.size)
                        for (i in radii.indices) {
                            val factor = 1.0f + ax * (centers[i].first - centerX) + ay * (centers[i].second - centerY)
                            normRadii.add(radii[i] / maxOf(0.5f, factor))
                        }
                        val sorted = normRadii.sorted()
                        val n = sorted.size
                        val p10 = sorted[n / 10]
                        val p90 = sorted[(n * 9 / 10).coerceAtMost(n - 1)]

                        val candidateBases = listOf(
                            p10,
                            p90 / RATIO_QUARTER_DIME,
                            p10 / RATIO_NICKEL_DIME,
                            p90 / RATIO_NICKEL_DIME
                        )

                        for (bCand in candidateBases) {
                            var delta = -1.0f
                            while (delta <= 1.0f) {
                                val b = bCand + delta
                                var err = 0.5f * abs(b - nominalDime) + 5000.0f * (ax * ax + ay * ay)
                                for (r in normRadii) {
                                    val errD = abs(r - b * 1.000f)
                                    val errN = abs(r - b * RATIO_NICKEL_DIME)
                                    val errQ = abs(r - b * RATIO_QUARTER_DIME)
                                    err += minOf(errD, minOf(errN, errQ))
                                }
                                if (err < bestScore) {
                                    bestScore = err
                                    bestAx = ax
                                    bestAy = ay
                                    bestB = b
                                }
                                delta += 0.5f
                            }
                        }
                    }
                }

                // Fine-tune around optimal solution
                var refinedAx = bestAx
                var refinedAy = bestAy
                var refinedB = bestB

                var day = -0.00008f
                while (day <= 0.00008f) {
                    var dax = -0.00008f
                    while (dax <= 0.00008f) {
                        val ax = bestAx + dax
                        val ay = bestAy + day
                        val normRadii = ArrayList<Float>(radii.size)
                        for (i in radii.indices) {
                            val factor = 1.0f + ax * (centers[i].first - centerX) + ay * (centers[i].second - centerY)
                            normRadii.add(radii[i] / maxOf(0.5f, factor))
                        }

                        var db = -1.0f
                        while (db <= 1.0f) {
                            val b = bestB + db
                            var err = 0.5f * abs(b - nominalDime) + 5000.0f * (ax * ax + ay * ay)
                            for (r in normRadii) {
                                val errD = abs(r - b * 1.000f)
                                val errN = abs(r - b * RATIO_NICKEL_DIME)
                                val errQ = abs(r - b * RATIO_QUARTER_DIME)
                                err += minOf(errD, minOf(errN, errQ))
                            }
                            if (err < bestScore) {
                                bestScore = err
                                refinedAx = ax
                                refinedAy = ay
                                refinedB = b
                            }
                            db += 0.1f
                        }
                        dax += 0.00002f
                    }
                    day += 0.00002f
                }

                val rDime = refinedB * 1.000f
                val rNickel = refinedB * RATIO_NICKEL_DIME
                val rQuarter = refinedB * RATIO_QUARTER_DIME

                val boundaryDN = (rDime + rNickel) / 2.0f
                val boundaryNQ = (rNickel + rQuarter) / 2.0f

                return radii.indices.map { i ->
                    val factor = 1.0f + refinedAx * (centers[i].first - centerX) + refinedAy * (centers[i].second - centerY)
                    val normR = radii[i] / maxOf(0.5f, factor)
                    when {
                        normR > boundaryNQ -> Coin.QUARTER
                        normR <= boundaryDN -> Coin.DIME
                        else -> Coin.NICKEL
                    }
                }
            }

            // Standard flat-plane multi-denomination classification
            val sorted = radii.sorted()
            val n = sorted.size
            val p10 = sorted[n / 10]
            val p90 = sorted[(n * 9 / 10).coerceAtMost(n - 1)]

            val candidateBases = mutableListOf(
                p10,                          // Hypothesis 1: p10 is Dime
                p90 / RATIO_QUARTER_DIME,      // Hypothesis 2: p90 is Quarter
                p10 / RATIO_NICKEL_DIME,       // Hypothesis 3: p10 is Nickel
                p90 / RATIO_NICKEL_DIME        // Hypothesis 4: p90 is Nickel
            )

            var bestScore = Float.MAX_VALUE
            var bestB = candidateBases[0]

            for (b in candidateBases) {
                var totalError = 0.5f * abs(b - nominalDime) // Soft scale prior
                for (r in radii) {
                    val errD = abs(r - b * 1.000f)
                    val errN = abs(r - b * RATIO_NICKEL_DIME)
                    val errQ = abs(r - b * RATIO_QUARTER_DIME)
                    totalError += minOf(errD, minOf(errN, errQ))
                }
                if (totalError < bestScore) {
                    bestScore = totalError
                    bestB = b
                }
            }

            // Fine-tune b around the best candidate
            var delta = -1.5f
            while (delta <= 1.5f) {
                val b = bestB + delta
                var totalError = 0.5f * abs(b - nominalDime)
                for (r in radii) {
                    val errD = abs(r - b * 1.000f)
                    val errN = abs(r - b * RATIO_NICKEL_DIME)
                    val errQ = abs(r - b * RATIO_QUARTER_DIME)
                    totalError += minOf(errD, minOf(errN, errQ))
                }
                if (totalError < bestScore) {
                    bestScore = totalError
                    bestB = b
                }
                delta += 0.05f
            }

            val rDime = bestB * 1.000f
            val rNickel = bestB * RATIO_NICKEL_DIME
            val rQuarter = bestB * RATIO_QUARTER_DIME

            val boundaryDN = (rDime + rNickel) / 2f
            val boundaryNQ = (rNickel + rQuarter) / 2f

            return radii.map { r ->
                when {
                    r > boundaryNQ -> Coin.QUARTER
                    r <= boundaryDN -> Coin.DIME
                    else -> Coin.NICKEL
                }
            }
        }
    }
}
