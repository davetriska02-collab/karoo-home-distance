package uk.co.triska.karoohome.extension

import android.content.Context
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.internal.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import uk.co.triska.karoohome.HOME_LAT
import uk.co.triska.karoohome.HOME_LON
import uk.co.triska.karoohome.haversineKm

/**
 * Numeric data field that emits the rider's distance from home in metres.
 *
 * Extension id:  "karoo-home-distance"  (matches KarooExtension constructor + extension_info.xml)
 * Data type id:  "home-distance"        (matches typeId in extension_info.xml)
 *
 * Karoo receives the value in metres (its internal base unit for distance) and handles
 * the km/mi conversion and display formatting for us, as instructed by UpdateGraphicConfig.
 *
 * The stream is throttled to update at most every 5 seconds to avoid unnecessary
 * battery drain and UI churn.
 */
class HomeDistanceDataType(
    private val karooSystem: KarooSystemService,
) : DataTypeImpl("karoo-home-distance", "home-distance") {

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.streamDataFlow(DataType.Type.LOCATION)
                .mapNotNull { it as? StreamState.Streaming }
                .mapNotNull { streaming ->
                    val lat = streaming.dataPoint.values[DataType.Field.LOC_LATITUDE]
                    val lng = streaming.dataPoint.values[DataType.Field.LOC_LONGITUDE]
                    val accuracy = streaming.dataPoint.values[DataType.Field.LOC_ACCURACY]

                    // Only emit if we have a fix accurate to within 500 metres
                    if (lat != null && lng != null && accuracy != null && accuracy < 500) {
                        haversineKm(lat, lng, HOME_LAT, HOME_LON)
                    } else {
                        null
                    }
                }
                .throttle(5_000L)
                .collect { distanceKm ->
                    // Convert km → metres (Karoo's base unit for distance fields)
                    val distanceMetres = distanceKm * 1000.0
                    emitter.onNext(
                        StreamState.Streaming(
                            DataPoint(
                                dataTypeId,
                                mapOf(DataType.Field.SINGLE to distanceMetres)
                            )
                        )
                    )
                }
        }

        // Cancel the coroutine when Karoo stops the stream
        emitter.setCancellable { job.cancel() }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        // Ask Karoo to render this field using the same formatting as the built-in
        // DISTANCE data type (handles km/mi preference, decimal places, units label).
        emitter.onNext(UpdateGraphicConfig(formatDataTypeId = DataType.Type.DISTANCE))
    }
}
