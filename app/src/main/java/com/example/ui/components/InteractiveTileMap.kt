package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.util.GeoapifyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Tile cache manager holding in-memory bitmaps and persisting to disk cache.
 * Ensures tiles stay continuously visible during drag gestures (never white frames).
 */
object TileMemoryCache {
    val memoryCache = ConcurrentHashMap<String, Bitmap>(256)
    val inFlightRequests = ConcurrentHashMap.newKeySet<String>()
    private val httpClient = OkHttpClient.Builder().build()

    suspend fun getOrFetchTile(
        context: android.content.Context,
        zoom: Int,
        x: Int,
        y: Int,
        onTileLoaded: () -> Unit
    ): Bitmap? {
        val key = "$zoom/$x/$y"
        memoryCache[key]?.let { return it }

        if (!inFlightRequests.add(key)) {
            return null // Request already in flight
        }

        withContext(Dispatchers.IO) {
            try {
                // 1. Check disk cache
                val cacheDir = File(context.cacheDir, "map_tiles")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val tileFile = File(cacheDir, "${zoom}_${x}_${y}.png")

                if (tileFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(tileFile.absolutePath)
                    if (bitmap != null) {
                        memoryCache[key] = bitmap
                        withContext(Dispatchers.Main) { onTileLoaded() }
                        return@withContext
                    }
                }

                // 2. Fetch from Geoapify raster tile API
                val url = GeoapifyService.buildTileUrl(zoom, x, y)
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        tileFile.writeBytes(bytes)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            memoryCache[key] = bitmap
                            withContext(Dispatchers.Main) { onTileLoaded() }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore network errors - keep existing frame intact
            } finally {
                inFlightRequests.remove(key)
            }
        }

        return memoryCache[key]
    }
}

/**
 * Continuous, smooth interactive slippy map canvas powered by standard Web Mercator tiles.
 * Preserves all loaded tiles during dragging, pan gestures, and zoom operations without blank frames.
 */
@Composable
fun InteractiveTileMap(
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Int,
    isDragging: Boolean,
    onDragStateChanged: (Boolean) -> Unit,
    onLocationChanged: (lat: Double, lng: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Display tile size in screen pixels (256 dp scaled for crisp mobile rendering)
    val tileSizePx = with(density) { 256.dp.toPx() }.toDouble()
    val worldSizePx = tileSizePx * (1 shl zoomLevel)

    // Convert lat/lng to global Mercator screen-pixel coordinates
    fun latLngToPixel(lat: Double, lng: Double): Pair<Double, Double> {
        val x = (lng + 180.0) / 360.0 * worldSizePx
        val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        val y = (1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * worldSizePx
        return Pair(x, y)
    }

    // Convert global Mercator screen-pixel coordinates back to lat/lng
    fun pixelToLatLng(px: Double, py: Double): Pair<Double, Double> {
        val lng = (px / worldSizePx) * 360.0 - 180.0
        val yNorm = 1.0 - (2.0 * py / worldSizePx)
        val latRad = atan(sinh(yNorm * Math.PI))
        val lat = Math.toDegrees(latRad)
        return Pair(lat, lng)
    }

    // Current center pixel coordinates
    var currentPixelX by remember(zoomLevel) {
        val (px, _) = latLngToPixel(centerLat, centerLng)
        mutableDoubleStateOf(px)
    }
    var currentPixelY by remember(zoomLevel) {
        val (_, py) = latLngToPixel(centerLat, centerLng)
        mutableDoubleStateOf(py)
    }

    // When idle (not dragging), sync center pixels with incoming props (e.g. GPS button click or place selection)
    LaunchedEffect(centerLat, centerLng, zoomLevel) {
        if (!isDragging) {
            val (px, py) = latLngToPixel(centerLat, centerLng)
            currentPixelX = px
            currentPixelY = py
        }
    }

    // Trigger state to notify canvas whenever a background tile finishes loading
    var tileRefreshTrigger by remember { mutableIntStateOf(0) }

    val currentOnLocationChanged by rememberUpdatedState(onLocationChanged)
    val currentOnDragStateChanged by rememberUpdatedState(onDragStateChanged)

    BoxWithConstraints(modifier = modifier) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val halfW = viewWidth / 2f
        val halfH = viewHeight / 2f

        val maxTileIndex = (1 shl zoomLevel) - 1

        // Calculate tile range to cover the viewport + 1 extra buffer tile in each direction
        val minXPixel = currentPixelX - halfW - tileSizePx
        val maxXPixel = currentPixelX + halfW + tileSizePx
        val minYPixel = currentPixelY - halfH - tileSizePx
        val maxYPixel = currentPixelY + halfH + tileSizePx

        val minTileX = max(0, floor(minXPixel / tileSizePx).toInt())
        val maxTileX = min(maxTileIndex, ceil(maxXPixel / tileSizePx).toInt())
        val minTileY = max(0, floor(minYPixel / tileSizePx).toInt())
        val maxTileY = min(maxTileIndex, ceil(maxYPixel / tileSizePx).toInt())

        // Request any tiles in the visible grid that are not yet cached
        LaunchedEffect(minTileX, maxTileX, minTileY, maxTileY, zoomLevel) {
            for (tx in minTileX..maxTileX) {
                for (ty in minTileY..maxTileY) {
                    val key = "$zoomLevel/$tx/$ty"
                    if (TileMemoryCache.memoryCache[key] == null) {
                        TileMemoryCache.getOrFetchTile(context, zoomLevel, tx, ty) {
                            tileRefreshTrigger++
                        }
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomLevel) {
                    detectDragGestures(
                        onDragStart = {
                            currentOnDragStateChanged(true)
                        },
                        onDragEnd = {
                            currentOnDragStateChanged(false)
                            val (finalLat, finalLng) = pixelToLatLng(currentPixelX, currentPixelY)
                            currentOnLocationChanged(finalLat, finalLng)
                        },
                        onDragCancel = {
                            currentOnDragStateChanged(false)
                            val (finalLat, finalLng) = pixelToLatLng(currentPixelX, currentPixelY)
                            currentOnLocationChanged(finalLat, finalLng)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Pan viewport continuously
                            currentPixelX -= dragAmount.x
                            currentPixelY -= dragAmount.y
                            val (newLat, newLng) = pixelToLatLng(currentPixelX, currentPixelY)
                            currentOnLocationChanged(newLat, newLng)
                        }
                    )
                }
        ) {
            // Read refresh trigger so newly loaded tiles trigger repaint seamlessly
            val _trigger = tileRefreshTrigger

            // Neutral map canvas background (clean map grey, never blank white)
            drawRect(Color(0xFFE8ECEF))

            // Paint all visible tiles at exact viewport offsets
            for (tx in minTileX..maxTileX) {
                for (ty in minTileY..maxTileY) {
                    val tileOriginX = (tx * tileSizePx) - currentPixelX + halfW
                    val tileOriginY = (ty * tileSizePx) - currentPixelY + halfH

                    val key = "$zoomLevel/$tx/$ty"
                    val bitmap = TileMemoryCache.memoryCache[key]
                    if (bitmap != null && !bitmap.isRecycled) {
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawBitmap(
                                bitmap,
                                null,
                                android.graphics.RectF(
                                    tileOriginX.toFloat(),
                                    tileOriginY.toFloat(),
                                    (tileOriginX + tileSizePx).toFloat(),
                                    (tileOriginY + tileSizePx).toFloat()
                                ),
                                null
                            )
                        }
                    }
                }
            }
        }
    }
}
