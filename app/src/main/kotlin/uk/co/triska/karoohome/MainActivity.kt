package uk.co.triska.karoohome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uk.co.triska.karoohome.screens.MainScreen

/**
 * Entry-point activity.
 *
 * Shown when the user taps the app icon on the Karoo device or the companion phone app.
 * It renders MainScreen, which displays the home location and instructions for adding
 * the data fields to a ride profile.
 *
 * There is no configuration UI — home coordinates are hardcoded in GpsUtils.kt.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }
}
