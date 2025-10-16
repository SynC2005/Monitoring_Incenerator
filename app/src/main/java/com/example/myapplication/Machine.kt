package com.example.myapplication

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class Machine(
    val name: String = "",
    val status: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val credential: String = "",
    val mapLink: String = ""
) {
    companion object {
        private const val TAG = "Machine"

        // Extract koordinat dari berbagai format Google Maps link
        fun extractCoordinatesFromMapLink(mapLink: String): Pair<Double, Double>? {
            if (mapLink.isEmpty()) return null

            try {
                Log.d(TAG, "Trying to extract from: $mapLink")

                // Format 1: @lat,lng,zoom (contoh: @-6.2088,106.8456,15z)
                val regex1 = "@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
                regex1.find(mapLink)?.let {
                    val lat = it.groupValues[1].toDouble()
                    val lng = it.groupValues[2].toDouble()
                    Log.d(TAG, "Extracted (format 1): $lat, $lng")
                    return Pair(lat, lng)
                }

                // Format 2: /maps?q=lat,lng
                val regex2 = "[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
                regex2.find(mapLink)?.let {
                    val lat = it.groupValues[1].toDouble()
                    val lng = it.groupValues[2].toDouble()
                    Log.d(TAG, "Extracted (format 2): $lat, $lng")
                    return Pair(lat, lng)
                }

                // Format 3: /place/NAME/@lat,lng
                val regex3 = "/place/[^/]+/@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
                regex3.find(mapLink)?.let {
                    val lat = it.groupValues[1].toDouble()
                    val lng = it.groupValues[2].toDouble()
                    Log.d(TAG, "Extracted (format 3): $lat, $lng")
                    return Pair(lat, lng)
                }

                // Format 4: ll=lat,lng
                val regex4 = "ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
                regex4.find(mapLink)?.let {
                    val lat = it.groupValues[1].toDouble()
                    val lng = it.groupValues[2].toDouble()
                    Log.d(TAG, "Extracted (format 4): $lat, $lng")
                    return Pair(lat, lng)
                }

                // Format 5: /maps/place/lat,lng
                val regex5 = "/maps/place/(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
                regex5.find(mapLink)?.let {
                    val lat = it.groupValues[1].toDouble()
                    val lng = it.groupValues[2].toDouble()
                    Log.d(TAG, "Extracted (format 5): $lat, $lng")
                    return Pair(lat, lng)
                }

                Log.d(TAG, "No coordinates found in link")

            } catch (e: Exception) {
                Log.e(TAG, "Error extracting coordinates: ${e.message}")
                e.printStackTrace()
            }
            return null
        }

        // Untuk shortened link, kita bisa follow redirect (optional, butuh network)
        suspend fun resolveShortLink(shortLink: String): String? = withContext(Dispatchers.IO) {
            try {
                val url = URL(shortLink)
                val connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connect()

                val location = connection.getHeaderField("Location")
                connection.disconnect()

                location
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving short link: ${e.message}")
                null
            }
        }
    }
}