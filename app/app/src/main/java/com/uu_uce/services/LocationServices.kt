package com.uu_uce.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat.checkSelfPermission
import kotlin.math.*

enum class LocationPollStartResult{
    ALREAD_LIVE,
    LOCATION_UNAVAILABLE,
    PERMISSIONS_DENIED,
    GPS_ONLY,
    NETWORK_ONLY,
    HYBRID;

    override fun toString(): String {
        return when(this){
            ALREAD_LIVE -> "Location already started!"
            LOCATION_UNAVAILABLE -> "Location not available!"
            PERMISSIONS_DENIED -> "Permissions denied!"
            GPS_ONLY -> "Location started using GPS!"
            NETWORK_ONLY -> "Location started using network!"
            HYBRID -> "Location using GPS/network hybrid!"
        }
    }
}

data class UTMCoordinate(val zone : Int, val letter : Char, val east : Double, val north : Double)

/*
    Will convert latitude, longitude coordinate to UTM.
    degPos: a pair of doubles of the form (latitude, longitude).
      It will provide you with a triple of UTM coordinates of the form (letter, easting, northing).
     */
fun degreeToUTM(degPos : Pair<Double, Double>) : UTMCoordinate{
    val letter : Char
    var easting : Double
    var northing : Double


    val lat = degPos.first
    val lon = degPos.second

    val zone = floor(lon / 6 + 31).toInt()

    when {
        lat < -72 -> letter = 'C'
        lat < -64 -> letter = 'D'
        lat < -56 -> letter = 'E'
        lat < -48 -> letter = 'F'
        lat < -40 -> letter = 'G'
        lat < -32 -> letter = 'H'
        lat < -24 -> letter = 'J'
        lat < -16 -> letter = 'K'
        lat < -8 -> letter = 'L'
        lat < 0 -> letter = 'M'
        lat < 8 -> letter = 'N'
        lat < 16 -> letter = 'P'
        lat < 24 -> letter = 'Q'
        lat < 32 -> letter = 'R'
        lat < 40 -> letter = 'S'
        lat < 48 -> letter = 'T'
        lat < 56 -> letter = 'U'
        lat < 64 -> letter = 'V'
        lat < 72 -> letter = 'W'
        else -> letter = 'X'
    }

    easting = 0.5 * ln(
        (1 + cos(lat * Math.PI / 180) * sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180)) / (1 - cos(
            lat * Math.PI / 180
        ) * sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180))
    ) * 0.9996 * 6399593.62 / (1 + 0.0820944379.pow(2.0) * cos(lat * Math.PI / 180).pow(2.0)).pow(0.5) * (1 + 0.0820944379.pow(
        2.0
    ) / 2 * (0.5 * ln(
        (1 + cos(lat * Math.PI / 180) * sin(
            lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180
        )) / (1 - cos(lat * Math.PI / 180) * sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180))
    )).pow(2.0) * cos(lat * Math.PI / 180).pow(2.0) / 3) + 500000
    easting = (easting * 100).roundToInt() * 0.01
    northing = (atan(
        tan(lat * Math.PI / 180) / cos(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180)
    ) - lat * Math.PI / 180) * 0.9996 * 6399593.625 / sqrt(
        1 + 0.006739496742 * cos(lat * Math.PI / 180).pow(2.0)
    ) * (1 + 0.006739496742 / 2 * (0.5 * ln(
        (1 + cos(
            lat * Math.PI / 180
        ) * sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180)) / (1 - cos(
            lat * Math.PI / 180
        ) * sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180))
    )).pow(2.0) * cos(lat * Math.PI / 180).pow(2.0)) + 0.9996 * 6399593.625 * (lat * Math.PI / 180 - 0.005054622556 * (lat * Math.PI / 180 + sin(
        2 * lat * Math.PI / 180
    ) / 2) + 4.258201531e-05 * (3 * (lat * Math.PI / 180 + sin(2 * lat * Math.PI / 180) / 2) + sin(
        2 * lat * Math.PI / 180
    ) * cos(lat * Math.PI / 180).pow(2.0)) / 4 - 1.674057895e-07 * (5 * (3 * (lat * Math.PI / 180 + sin(2 * lat * Math.PI / 180) / 2) + sin(
        2 * lat * Math.PI / 180
    ) * cos(lat * Math.PI / 180).pow(2.0)) / 4 + sin(2 * lat * Math.PI / 180) * cos(
        lat * Math.PI / 180
    ).pow(2.0) * cos(lat * Math.PI / 180).pow(2.0)) / 3)
    if (letter < 'M') northing += 10000000
    northing = (northing * 100).roundToInt() * 0.01

    return UTMCoordinate(zone, letter, easting, northing)
}

/*
Will poll the location for you.
 */
class LocationServices{
    companion object {
        val permissionsNeeded = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private var networkRunning = false
    /*
    Will start polling the location.
    context: the activity that uses this.
    pollTimeMs: how long to wait to poll the location again.
    minDist: minimum distance parameter of android.location.LocationManager.requestLocationUpdates.
    action: a lambda function that will be called when a location is received.
      It will provide you with the location as a tuple of Double.
     */
    fun startPollThread(
        context: Context,
        pollTimeMs: Long,
        minDist: Float,
        action: (Pair<Double, Double>) -> Unit)
        : LocationPollStartResult
    {
        //Check if the network is running, might not be the best way to do this.
        if(networkRunning) {
            Log.d("LocationServices", "LocationNetwork already running")
            return LocationPollStartResult.ALREAD_LIVE
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!hasGps && !hasNetwork)
            return LocationPollStartResult.LOCATION_UNAVAILABLE

        Log.d(
            "LocationServices",
            "gpsEnabled: $hasGps, networkEnabled: $hasNetwork"
        )

        var result = PackageManager.PERMISSION_DENIED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            result = checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            Log.d("LocationServices", "permissions: $result")
        }

        if (result != PackageManager.PERMISSION_GRANTED)
            return LocationPollStartResult.PERMISSIONS_DENIED

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                if (location != null) {

                    action(Pair(location.latitude, location.longitude))
                    Log.d(
                        "LocationServices",
                        " Network Latitude : " + location.latitude
                    )
                    Log.d(
                        "LocationServices",
                        " Network Longitude : " + location.longitude
                    )
                }
            }

            override fun onStatusChanged(
                provider: String?,
                status: Int,
                extras: Bundle?
            ) {
                Log.d("LocationService", "locationListener: onStatusChanged: No handling logic!")
            }

            override fun onProviderEnabled(provider: String?) {
                Log.d("LocationService", "locationListener: onProviderEnabled: No handling logic!")
            }

            override fun onProviderDisabled(provider: String?) {
                Log.d("LocationService", "locationListener: onProviderDisabled: No handling logic!")
            }
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, pollTimeMs, minDist, locationListener)

        networkRunning = true
        Log.d("LocationServices", "Started network")
        if(hasGps && hasNetwork)
            return LocationPollStartResult.HYBRID
        if(hasGps)
            return LocationPollStartResult.GPS_ONLY
        return LocationPollStartResult.NETWORK_ONLY
    }
}