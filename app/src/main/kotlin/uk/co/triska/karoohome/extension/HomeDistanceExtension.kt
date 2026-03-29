package uk.co.triska.karoohome.extension

import io.hammerhead.karooext.KarooExtension
import io.hammerhead.karooext.KarooSystemService

/**
 * Main extension service. Karoo discovers this via the AndroidManifest intent filter
 * (io.hammerhead.karooext.KAROO_EXTENSION) and the extension_info.xml meta-data.
 *
 * The extension id "karoo-home-distance" must match the id in extension_info.xml.
 * The version string is displayed in the Karoo companion app.
 */
class HomeDistanceExtension : KarooExtension("karoo-home-distance", "1.0") {

    lateinit var karooSystem: KarooSystemService

    /**
     * Lazily instantiated list of data types provided by this extension.
     * Both types receive a reference to karooSystem so they can subscribe to GPS streams.
     */
    override val types by lazy {
        listOf(
            HomeDistanceDataType(karooSystem),
            HomeBearingDataType(karooSystem),
        )
    }

    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(applicationContext)
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}
