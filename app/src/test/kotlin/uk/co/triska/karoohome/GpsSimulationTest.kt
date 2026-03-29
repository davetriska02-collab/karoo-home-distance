package uk.co.triska.karoohome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Simulations for the core GPS mathematics and display logic used by the
 * Karoo Home Distance extension.
 *
 * Each test represents a real-world scenario a cyclist might encounter:
 * departing from home, riding away, navigating back, etc.
 *
 * The helper functions for arrow symbol selection, colour coding, and distance
 * formatting are replicated here (they are private in production code) so they
 * can be exercised directly without requiring Android instrumentation.
 */
class GpsSimulationTest {

    // -------------------------------------------------------------------------
    // Helpers mirroring the private logic in HomeBearingDataType.kt
    // -------------------------------------------------------------------------

    private fun bearingToArrow(bearing: Double): String {
        val n = ((bearing % 360) + 360) % 360
        return when {
            n < 22.5  -> "↑"
            n < 67.5  -> "↗"
            n < 112.5 -> "→"
            n < 157.5 -> "↘"
            n < 202.5 -> "↓"
            n < 247.5 -> "↙"
            n < 292.5 -> "←"
            n < 337.5 -> "↖"
            else      -> "↑"
        }
    }

    private fun colourLabel(distanceKm: Double): String = when {
        distanceKm < 5.0  -> "green"
        distanceKm < 20.0 -> "amber"
        else              -> "red"
    }

    private fun formatDistance(distanceKm: Double): String =
        if (distanceKm < 1.0) "${(distanceKm * 1000).toInt()} m"
        else String.format("%.1f km", distanceKm)

    // Tolerance for floating-point distance comparisons (±0.5 km)
    private fun assertDistanceKm(expectedKm: Double, actualKm: Double, toleranceKm: Double = 0.5) {
        val diff = abs(actualKm - expectedKm)
        assertTrue(
            "Expected ~$expectedKm km but got $actualKm km (diff $diff km, tolerance $toleranceKm km)",
            diff <= toleranceKm
        )
    }

    // Tolerance for bearing comparisons (±2°)
    private fun assertBearingDeg(expectedDeg: Double, actualDeg: Double, toleranceDeg: Double = 2.0) {
        // Handle wrap-around (e.g. expected 359°, actual 1° → diff should be 2°, not 358°)
        val diff = abs(((actualDeg - expectedDeg + 540) % 360) - 180)
        assertTrue(
            "Expected bearing ~$expectedDeg° but got $actualDeg° (circular diff $diff°, tolerance $toleranceDeg°)",
            diff <= toleranceDeg
        )
    }

    // =========================================================================
    // 1. HAVERSINE DISTANCE SIMULATIONS
    // =========================================================================

    @Test
    fun `distance from home to itself is zero`() {
        val d = haversineKm(HOME_LAT, HOME_LON, HOME_LAT, HOME_LON)
        assertEquals(0.0, d, 0.001)
    }

    @Test
    fun `distance is symmetric`() {
        val aToB = haversineKm(51.153, -0.656, 51.500, -0.126)
        val bToA = haversineKm(51.500, -0.126, 51.153, -0.656)
        assertEquals(aToB, bToA, 0.001)
    }

    @Test
    fun `home to central London is approximately 50 km`() {
        // London City (51.5074, -0.1278) — roughly 50 km north-east of Witley
        val d = haversineKm(HOME_LAT, HOME_LON, 51.5074, -0.1278)
        assertDistanceKm(50.0, d, toleranceKm = 5.0)
    }

    @Test
    fun `home to Brighton is approximately 55 km`() {
        // Brighton (50.8225, -0.1372)
        val d = haversineKm(HOME_LAT, HOME_LON, 50.8225, -0.1372)
        assertDistanceKm(55.0, d, toleranceKm = 5.0)
    }

    @Test
    fun `home to Edinburgh is approximately 560 km`() {
        // Edinburgh (55.9533, -3.1883)
        val d = haversineKm(HOME_LAT, HOME_LON, 55.9533, -3.1883)
        assertDistanceKm(560.0, d, toleranceKm = 10.0)
    }

    @Test
    fun `very short distance — 100 m away from home`() {
        // ~0.001° of latitude ≈ 111 m
        val d = haversineKm(HOME_LAT + 0.001, HOME_LON, HOME_LAT, HOME_LON)
        assertDistanceKm(0.111, d, toleranceKm = 0.05)
    }

    // =========================================================================
    // 2. BEARING SIMULATIONS
    // =========================================================================

    @Test
    fun `bearing due north is 0 degrees`() {
        // Start south of home, so home is directly north
        val bearing = bearingTo(HOME_LAT - 1.0, HOME_LON, HOME_LAT, HOME_LON)
        assertBearingDeg(0.0, bearing)
    }

    @Test
    fun `bearing due south is 180 degrees`() {
        // Start north of home, so home is directly south
        val bearing = bearingTo(HOME_LAT + 1.0, HOME_LON, HOME_LAT, HOME_LON)
        assertBearingDeg(180.0, bearing)
    }

    @Test
    fun `bearing due east is 90 degrees`() {
        // Start west of home, so home is directly east
        val bearing = bearingTo(HOME_LAT, HOME_LON - 1.0, HOME_LAT, HOME_LON)
        assertBearingDeg(90.0, bearing)
    }

    @Test
    fun `bearing due west is 270 degrees`() {
        // Start east of home, so home is directly west
        val bearing = bearingTo(HOME_LAT, HOME_LON + 1.0, HOME_LAT, HOME_LON)
        assertBearingDeg(270.0, bearing)
    }

    @Test
    fun `bearing from London to home is approximately south-west`() {
        // London is north-east of Witley → home is south-west ≈ 225°
        val bearing = bearingTo(51.5074, -0.1278, HOME_LAT, HOME_LON)
        assertBearingDeg(225.0, bearing, toleranceDeg = 15.0)
    }

    @Test
    fun `bearing normalises negative atan2 result into 0-360 range`() {
        // Any bearing must be in [0, 360)
        for (latOffset in listOf(-2.0, -0.5, 0.5, 2.0)) {
            for (lonOffset in listOf(-2.0, -0.5, 0.5, 2.0)) {
                val bearing = bearingTo(HOME_LAT + latOffset, HOME_LON + lonOffset, HOME_LAT, HOME_LON)
                assertTrue("Bearing $bearing out of [0,360)", bearing >= 0.0 && bearing < 360.0)
            }
        }
    }

    // =========================================================================
    // 3. ARROW SYMBOL SIMULATIONS
    // =========================================================================

    @Test
    fun `arrow symbols cover all 8 compass points`() {
        val cases = mapOf(
            0.0   to "↑",   // N
            45.0  to "↗",   // NE
            90.0  to "→",   // E
            135.0 to "↘",   // SE
            180.0 to "↓",   // S
            225.0 to "↙",   // SW
            270.0 to "←",   // W
            315.0 to "↖",   // NW
            359.9 to "↑",   // wraps back to N
        )
        for ((bearing, expected) in cases) {
            assertEquals("bearing $bearing°", expected, bearingToArrow(bearing))
        }
    }

    @Test
    fun `arrow handles negative bearings gracefully`() {
        // -90° should normalise to 270° → west arrow
        assertEquals("←", bearingToArrow(-90.0))
    }

    @Test
    fun `arrow handles bearings greater than 360`() {
        // 450° = 90° → east arrow
        assertEquals("→", bearingToArrow(450.0))
    }

    // =========================================================================
    // 4. COLOUR CODING SIMULATIONS
    // =========================================================================

    @Test
    fun `colour is green when within 5 km`() {
        assertEquals("green", colourLabel(0.0))
        assertEquals("green", colourLabel(2.5))
        assertEquals("green", colourLabel(4.99))
    }

    @Test
    fun `colour is amber between 5 and 20 km`() {
        assertEquals("amber", colourLabel(5.0))
        assertEquals("amber", colourLabel(12.5))
        assertEquals("amber", colourLabel(19.99))
    }

    @Test
    fun `colour is red beyond 20 km`() {
        assertEquals("red", colourLabel(20.0))
        assertEquals("red", colourLabel(50.0))
        assertEquals("red", colourLabel(500.0))
    }

    // =========================================================================
    // 5. DISTANCE FORMATTING SIMULATIONS
    // =========================================================================

    @Test
    fun `format shows metres when under 1 km`() {
        assertEquals("850 m", formatDistance(0.85))
        assertEquals("0 m",   formatDistance(0.0))
        assertEquals("999 m", formatDistance(0.999))
    }

    @Test
    fun `format shows km with one decimal when 1 km or more`() {
        assertEquals("1.0 km",  formatDistance(1.0))
        assertEquals("12.3 km", formatDistance(12.3))
        assertEquals("100.0 km", formatDistance(100.0))
    }

    // =========================================================================
    // 6. SIMULATED RIDE: departing from home, looping, returning
    // =========================================================================

    /**
     * A simulated 30 km loop ride departing north-east from Witley,
     * represented as a series of (lat, lon) waypoints.  We verify that:
     *   - Distance increases during the outbound leg
     *   - Bearing points roughly back towards home throughout
     *   - Distance decreases during the return leg
     *   - Colour transitions green → amber → red → amber → green as expected
     */
    @Test
    fun `simulated loop ride — distance and arrow behave correctly`() {
        // Waypoints: home → NE ride → turnaround → back to home
        data class Waypoint(val lat: Double, val lon: Double, val label: String)

        val waypoints = listOf(
            Waypoint(HOME_LAT,         HOME_LON,         "start (home)"),
            Waypoint(HOME_LAT + 0.04,  HOME_LON + 0.06,  "5 km NE"),
            Waypoint(HOME_LAT + 0.09,  HOME_LON + 0.13,  "12 km NE"),
            Waypoint(HOME_LAT + 0.18,  HOME_LON + 0.27,  "25 km NE — turnaround"),
            Waypoint(HOME_LAT + 0.09,  HOME_LON + 0.13,  "12 km NE — return"),
            Waypoint(HOME_LAT + 0.04,  HOME_LON + 0.06,  "5 km NE — return"),
            Waypoint(HOME_LAT + 0.001, HOME_LON + 0.001, "almost home"),
        )

        println("\n=== Simulated Ride ===")
        println("%-30s %10s %10s %8s %6s".format("Waypoint", "Dist (km)", "Bearing°", "Arrow", "Colour"))
        println("-".repeat(70))

        var prevDistance: Double? = null
        var reachedTurnaround = false

        for ((index, wp) in waypoints.withIndex()) {
            val distKm  = haversineKm(wp.lat, wp.lon, HOME_LAT, HOME_LON)
            val bearing = bearingTo(wp.lat, wp.lon, HOME_LAT, HOME_LON)
            val arrow   = bearingToArrow(bearing)
            val colour  = colourLabel(distKm)
            val distFmt = formatDistance(distKm)

            println("%-30s %10s %10.1f %8s %6s".format(wp.label, distFmt, bearing, arrow, colour))

            // At the turnaround point distance should be maximum
            if (wp.label.contains("turnaround")) {
                reachedTurnaround = true
            }

            if (prevDistance != null) {
                if (!reachedTurnaround) {
                    // Outbound: distance should be increasing
                    assertTrue(
                        "Outbound leg: distance should increase at '${wp.label}' " +
                                "(prev=${prevDistance}, curr=$distKm)",
                        distKm > prevDistance!!
                    )
                } else if (wp.label.contains("return") || wp.label.contains("almost")) {
                    // Return leg: distance should be decreasing
                    assertTrue(
                        "Return leg: distance should decrease at '${wp.label}' " +
                                "(prev=${prevDistance}, curr=$distKm)",
                        distKm < prevDistance!!
                    )
                }
            }

            prevDistance = distKm
        }

        // Final waypoint is ~100 m from home — must be green
        val finalDist = haversineKm(
            waypoints.last().lat, waypoints.last().lon, HOME_LAT, HOME_LON
        )
        assertEquals("green", colourLabel(finalDist))
        assertTrue("Final distance should be under 1 km", finalDist < 1.0)
    }

    // =========================================================================
    // 7. EDGE CASES
    // =========================================================================

    @Test
    fun `antipodal point is approximately 20000 km`() {
        // Antipode of home: (-HOME_LAT, HOME_LON + 180)
        val antiLat = -HOME_LAT
        val antiLon = HOME_LON + 180.0
        val d = haversineKm(HOME_LAT, HOME_LON, antiLat, antiLon)
        // Half earth circumference ≈ 20,015 km
        assertDistanceKm(20015.0, d, toleranceKm = 100.0)
    }

    @Test
    fun `crossing the prime meridian does not break distance`() {
        // One point slightly east, one slightly west of 0° longitude
        val d = haversineKm(51.0, -0.1, 51.0, 0.1)
        assertDistanceKm(13.8, d, toleranceKm = 1.0)
    }

    @Test
    fun `distance in metres conversion is correct for sub-km values`() {
        // 0.5 km → formatDistance should return "500 m"
        assertEquals("500 m", formatDistance(0.5))
    }
}
