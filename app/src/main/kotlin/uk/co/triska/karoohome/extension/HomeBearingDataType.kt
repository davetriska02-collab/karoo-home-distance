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
import uk.co.triska.karoohome.bearingTo

/**
 * Numeric data field that emits the bearing from the rider to home in degrees.
 *
 * Extension id:  "karoo-home-distance"
 * Data type id:  "home-bearing"
 *
 * The value is emitted as degrees (0-360) where 0/360 = North, 90 = East, etc.
 * Karoo renders it as a plain number using its standard numeric field widget.
 */
class HomeBearingDataType(
    private val karooSystem: KarooSystemService,
) : DataTypeImpl("karoo-home-distance", "home-bearing") {

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.streamDataFlow(DataType.Type.LOCATION)
                .mapNotNull { it as? StreamState.Streaming }
                .mapNotNull { streaming ->
                    val lat = streaming.dataPoint.values[DataType.Field.LOC_LATITUDE]
                    val lng = streaming.dataPoint.values[DataType.Field.LOC_LONGITUDE]
                    val accuracy = streaming.dataPoint.values[DataType.Field.LOC_ACCURACY]
                    if (lat != null && lng != null && accuracy != null && accuracy < 500) {
                        bearingTo(lat, lng, HOME_LAT, HOME_LON)
                    } else {
                        null
                    }
                }
                .throttle(5_000L)
                .collect { bearingDeg ->
                    emitter.onNext(
                        StreamState.Streaming(
                            DataPoint(
                                dataTypeId,
                                mapOf(DataType.Field.SINGLE to bearingDeg)
                            )
                        )
                    )
                }
        }
        emitter.setCancellable { job.cancel() }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        // Let Karoo render the bearing as a plain numeric field
        emitter.onNext(UpdateGraphicConfig(formatDataTypeId = null))
    }
}
