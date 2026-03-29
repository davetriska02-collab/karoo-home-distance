package uk.co.triska.karoohome

import kotlin.math.*

// Home coordinates — Witley, Surrey GU8 5SX
const val HOME_LAT = 51.153
const val HOME_LON = -0.656

/**
 * Calculate the great-circle distance between two GPS coordinates using the Haversine formula.
 *
 * @param lat1 Latitude of point 1 in decimal degrees
 * @param lon1 Longitude of point 1 in decimal degrees
 * @param lat2 Latitude of point 2 in decimal degrees
 * @param lon2 Longitude of point 2 in decimal degrees
 * @return Distance in kilometres
 */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadiusKm * c
}

/**
 * Calculate the initial bearing from point 1 to point 2.
 *
 * @param lat1 Latitude of the current position in decimal degrees
 * @param lon1 Longitude of the current position in decimal degrees
 * @param lat2 Latitude of the target (home) in decimal degrees
 * @param lon2 Longitude of the target (home) in decimal degrees
 * @return Bearing in degrees, 0–360, where 0/360 = North, 90 = East, etc.
 */
fun bearingTo(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val dLonRad = Math.toRadians(lon2 - lon1)

    val x = sin(dLonRad) * cos(lat2Rad)
    val y = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLonRad)

    val bearing = Math.toDegrees(atan2(x, y))
    // Normalise to 0–360
    return (bearing + 360) % 360
}
