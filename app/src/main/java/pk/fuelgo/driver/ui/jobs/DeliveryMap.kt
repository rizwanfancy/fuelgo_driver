package pk.fuelgo.driver.ui.jobs

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// Karachi, same fallback the web portal uses when an order has no coordinates.
private const val DEFAULT_LAT = 24.8607
private const val DEFAULT_LNG = 67.0011
private const val MAP_ZOOM = 15.0

/**
 * OpenStreetMap view (same tile source as the web portal's Leaflet map, no API key) with
 * the delivery destination pinned, a "re-centre" button and an area search (Nominatim).
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun DeliveryMap(latitude: Double, longitude: Double, title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val lat = if (latitude != 0.0 || longitude != 0.0) latitude else DEFAULT_LAT
    val lng = if (latitude != 0.0 || longitude != 0.0) longitude else DEFAULT_LNG

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    // Last coordinates drawn on the map, so polling/typing recompositions don't reset the user's pan.
    val applied = remember { doubleArrayOf(Double.NaN, Double.NaN) }

    val mapView = remember {
        // Tiles cache in app cache: no storage permission needed. Nominatim/OSM require a UA.
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("osm-tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(MAP_ZOOM)
            // Let the map pan/zoom inside the scrolling job screen.
            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search area or landmark…") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                enabled = !searching && query.isNotBlank(),
                onClick = {
                    scope.launch {
                        searching = true
                        searchPlace(query)?.let { (la, lo) -> mapView.controller.animateTo(GeoPoint(la, lo)) }
                        searching = false
                    }
                },
            ) { Icon(Icons.Filled.Search, contentDescription = "Search") }
            IconButton(onClick = { mapView.controller.animateTo(GeoPoint(lat, lng)) }) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Re-centre on delivery")
            }
        }

        AndroidView(
            factory = { mapView },
            update = { map ->
                if (applied[0] != lat || applied[1] != lng) {
                    applied[0] = lat
                    applied[1] = lng
                    val point = GeoPoint(lat, lng)
                    map.overlays.clear()
                    map.overlays.add(
                        Marker(map).apply {
                            position = point
                            this.title = title
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        },
                    )
                    map.controller.setCenter(point)
                    map.invalidate()
                }
            },
            modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(14.dp)),
        )
    }
}

/** Nominatim lookup limited to Pakistan (same query the web portal uses). */
private suspend fun searchPlace(query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    runCatching {
        val url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=pk&q=" +
            java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val request = Request.Builder().url(url).header("User-Agent", "FuelGoDriver/1.0").build()
        OkHttpClient().newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val first = JSONArray(body).optJSONObject(0) ?: return@use null
            first.getString("lat").toDouble() to first.getString("lon").toDouble()
        }
    }.getOrNull()
}
