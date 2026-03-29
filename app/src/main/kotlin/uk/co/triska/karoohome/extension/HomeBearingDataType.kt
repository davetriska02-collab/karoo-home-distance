package uk.co.triska.karoohome.extension

import android.content.Context
import android.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.wrapContentSize
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.DataPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import uk.co.triska.karoohome.HOME_LAT
import uk.co.triska.karoohome.HOME_LON
import uk.co.triska.karoohome.bearingTo
import uk.co.triska.karoohome.haversineKm
import java.util.Locale

/**
 * Graphical data field that renders a compass arrow pointing towards home plus
 * a distance label below it.
 *
 * Extension id:  "karoo-home-distance"
 * Data type id:  "home-bearing"
 *
 * Arrow colour coding:
 *   Green  — within 5 km of home (nearly there)
 *   Amber  — 5–20 km (on your way)
 *   Red    — more than 20 km away (long way to go)
 *
 * The view is rendered via GlanceRemoteViews so it can be pushed to Karoo's
 * RemoteViews-based widget system.  The bearing is applied as a Unicode arrow
 * rotated by the bearing angle, which is the simplest approach compatible with
 * Glance's layout model.
 *
 * Note: True arrow rotation via Glance requires an ImageView with a rotation
 * modifier.  Glance does not currently expose a direct rotation modifier for
 * arbitrary composables, so we render a directional Unicode arrow character and
 * label the bearing in degrees as a fallback that is always legible on-device.
 * Swap to a proper rotated vector drawable if Glance rotation support lands in
 * a later SDK version.
 */
class HomeBearingDataType(
    private val karooSystem: KarooSystemService,
) : DataTypeImpl("karoo-home-distance", "home-bearing") {

    // Data class bundling what the view needs each update cycle
    private data class HomeState(val distanceKm: Double, val bearingDeg: Double)

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.streamDataFlow(DataType.Type.LOCATION)
                .mapNotNull { it as? StreamState.Streaming }
                .mapNotNull { streaming ->
                    val lat = streaming.dataPoint.values[DataType.Field.LOC_LATITUDE]
                    val lng = streaming.dataPoint.values[DataType.Field.LOC_LONGITUDE]
                    val accuracy = streaming.dataPoint.values[DataType.Field.LOC_ACCURACY]
                    if (lat != null && lng != null && accuracy != null && accuracy < 500) {
                        HomeState(
                            distanceKm = haversineKm(lat, lng, HOME_LAT, HOME_LON),
                            bearingDeg = bearingTo(lat, lng, HOME_LAT, HOME_LON),
                        )
                    } else {
                        null
                    }
                }
                .throttle(5_000L)
                .collect { state ->
                    // Emit bearing in degrees as the stream value so it can be
                    // inspected by other data consumers if needed
                    emitter.onNext(
                        StreamState.Streaming(
                            DataPoint(
                                dataTypeId,
                                mapOf(DataType.Field.SINGLE to state.bearingDeg)
                            )
                        )
                    )
                }
        }
        emitter.setCancellable { job.cancel() }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val glance = GlanceRemoteViews()

        val job = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.streamDataFlow(DataType.Type.LOCATION)
                .mapNotNull { it as? StreamState.Streaming }
                .mapNotNull { streaming ->
                    val lat = streaming.dataPoint.values[DataType.Field.LOC_LATITUDE]
                    val lng = streaming.dataPoint.values[DataType.Field.LOC_LONGITUDE]
                    val accuracy = streaming.dataPoint.values[DataType.Field.LOC_ACCURACY]
                    if (lat != null && lng != null && accuracy != null && accuracy < 500) {
                        HomeState(
                            distanceKm = haversineKm(lat, lng, HOME_LAT, HOME_LON),
                            bearingDeg = bearingTo(lat, lng, HOME_LAT, HOME_LON),
                        )
                    } else {
                        null
                    }
                }
                .throttle(5_000L)
                .collect { state ->
                    val result = glance.compose(context, DpSize.Unspecified) {
                        HomeBearingView(state.distanceKm, state.bearingDeg)
                    }
                    emitter.updateView(result.remoteViews)
                }
        }
        emitter.setCancellable { job.cancel() }
    }
}

/**
 * Arrow character look-up — returns the Unicode arrow that best approximates
 * the 8-point compass direction for the given bearing.
 *
 * Directions: N ↑, NE ↗, E →, SE ↘, S ↓, SW ↙, W ←, NW ↖
 */
private fun bearingToArrow(bearing: Double): String {
    val normalised = ((bearing % 360) + 360) % 360
    return when {
        normalised < 22.5  -> "↑"
        normalised < 67.5  -> "↗"
        normalised < 112.5 -> "→"
        normalised < 157.5 -> "↘"
        normalised < 202.5 -> "↓"
        normalised < 247.5 -> "↙"
        normalised < 292.5 -> "←"
        normalised < 337.5 -> "↖"
        else               -> "↑"
    }
}

/**
 * Colour coding for the arrow based on distance from home.
 *   < 5 km  — green  (nearly home)
 *   5–20 km — amber  (on the way)
 *   > 20 km — red    (long ride ahead)
 */
private fun arrowColour(distanceKm: Double): ComposeColor = when {
    distanceKm < 5.0  -> ComposeColor(0xFF4CAF50.toInt())  // Green
    distanceKm < 20.0 -> ComposeColor(0xFFFFC107.toInt())  // Amber
    else              -> ComposeColor(0xFFF44336.toInt())  // Red
}

/**
 * Formats the distance for display.  Values < 1 km show as metres ("850 m"),
 * values ≥ 1 km show with one decimal place ("12.3 km").
 */
private fun formatDistance(distanceKm: Double): String {
    return if (distanceKm < 1.0) {
        "${(distanceKm * 1000).toInt()} m"
    } else {
        String.format(Locale.UK, "%.1f km", distanceKm)
    }
}

/**
 * Glance composable that renders the bearing arrow and distance label.
 * This is called inside GlanceRemoteViews.compose() and converted to RemoteViews
 * for delivery to the Karoo widget system.
 */
@androidx.glance.GlanceComposable
@androidx.compose.runtime.Composable
private fun HomeBearingView(distanceKm: Double, bearingDeg: Double) {
    val arrow = bearingToArrow(bearingDeg)
    val colour = arrowColour(distanceKm)
    val distanceText = formatDistance(distanceKm)
    val bearingInt = bearingDeg.toInt()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(ComposeColor.Black)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Large directional arrow
            Text(
                text = arrow,
                style = TextStyle(
                    color = ColorProvider(colour),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            // Distance label
            Text(
                text = distanceText,
                style = TextStyle(
                    color = ColorProvider(ComposeColor.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.padding(top = 4.dp),
            )
            // Bearing in degrees as a secondary label
            Text(
                text = "${bearingInt}°",
                style = TextStyle(
                    color = ColorProvider(ComposeColor(0xFFAAAAAA.toInt())),
                    fontSize = 12.sp,
                ),
                modifier = GlanceModifier.padding(top = 2.dp),
            )
        }
    }
}
