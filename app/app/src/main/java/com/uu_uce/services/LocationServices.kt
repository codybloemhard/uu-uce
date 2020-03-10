package com.uu_uce.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat.checkSelfPermission
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.shapefiles.p2
import kotlin.math.*

enum class LocationPollStartResult{
    ALREADY_LIVE,
    LOCATION_UNAVAILABLE,
    PERMISSIONS_DENIED,
    GPS_ONLY,
    NETWORK_ONLY,
    HYBRID;

    override fun toString(): String {
        return when(this){
            ALREADY_LIVE -> "Location already started!"
            LOCATION_UNAVAILABLE -> "Location not available!"
            PERMISSIONS_DENIED -> "Permissions denied!"
            GPS_ONLY -> "Location started using GPS!"
            NETWORK_ONLY -> "Location started using network!"
            HYBRID -> "Location using GPS/network hybrid!"
        }
    }
}

fun latToUTMLetter(lat: Double): Char{
    val letters = listOf('C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M',
        'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W')
    var counter = -72
    for(l in letters){
        if(lat < counter)
            return l
        counter += 8
    }
    return 'X'
}

data class UTMCoordinate(val zone : Int, val letter : Char, val east : Double, val north : Double)

/*
Will convert latitude, longitude coordinate to UTM.
degPos: a pair of doubles of the form (latitude, longitude).
It will provide you with a triple of UTM coordinates of the form (letter, easting, northing).
 */
fun degreeToUTM(degPos : p2) : UTMCoordinate{
    var easting : Double
    var northing : Double

    val lat = degPos.first
    val lon = degPos.second

    val zone = floor(lon / 6 + 31).toInt()
    val letter = latToUTMLetter(lat)

    val deg = Math.PI / 180

    easting = 0.5 * ln(
        (1 + cos(lat * deg) * sin(lon * deg - (6 * zone - 183) * deg)) / (1 - cos(
            lat * deg
        ) * sin(lon * deg - (6 * zone - 183) * deg))
    ) * 0.9996 * 6399593.62 / (1 + 0.0820944379.pow(2.0) * cos(lat * deg).pow(2.0)).pow(0.5) * (1 + 0.0820944379.pow(
        2.0
    ) / 2 * (0.5 * ln(
        (1 + cos(lat * deg) * sin(
            lon * deg - (6 * zone - 183) * deg
        )) / (1 - cos(lat * deg) * sin(lon * deg - (6 * zone - 183) * deg))
    )).pow(2.0) * cos(lat * deg).pow(2.0) / 3) + 500000
    easting = (easting * 100).roundToInt() * 0.01
    northing = (atan(
        tan(lat * deg) / cos(lon * deg - (6 * zone - 183) * deg)
    ) - lat * deg) * 0.9996 * 6399593.625 / sqrt(
        1 + 0.006739496742 * cos(lat * deg).pow(2.0)
    ) * (1 + 0.006739496742 / 2 * (0.5 * ln(
        (1 + cos(
            lat * deg
        ) * sin(lon * deg - (6 * zone - 183) * deg)) / (1 - cos(
            lat * deg
        ) * sin(lon * deg - (6 * zone - 183) * deg))
    )).pow(2.0) * cos(lat * deg).pow(2.0)) + 0.9996 * 6399593.625 * (lat * deg - 0.005054622556 * (lat * deg + sin(
        2 * lat * deg
    ) / 2) + 4.258201531e-05 * (3 * (lat * deg + sin(2 * lat * deg) / 2) + sin(
        2 * lat * deg
    ) * cos(lat * deg).pow(2.0)) / 4 - 1.674057895e-07 * (5 * (3 * (lat * deg + sin(2 * lat * deg) / 2) + sin(
        2 * lat * deg
    ) * cos(lat * deg).pow(2.0)) / 4 + sin(2 * lat * deg) * cos(
        lat * deg
    ).pow(2.0) * cos(lat * deg).pow(2.0)) / 3)
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
    private var networkKilled = false
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
        action: (p2) -> Unit)
        : LocationPollStartResult
    {
        //Check if the network is running, might not be the best way to do this.
        if(networkRunning) {
            Logger.log(LogType.Info,"LocationServices", "LocationNetwork already running")
            return LocationPollStartResult.ALREADY_LIVE
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        var networkProvider : String? = null

        if (!hasGps && !hasNetwork)
            return LocationPollStartResult.LOCATION_UNAVAILABLE

        Logger.log( LogType.Info,
            "LocationServices",
            "gpsEnabled: $hasGps, networkEnabled: $hasNetwork"
        )

        var result = PackageManager.PERMISSION_DENIED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            result = checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            Logger.log(LogType.Info,"LocationServices", "permissions: $result")
        }

        if (result != PackageManager.PERMISSION_GRANTED)
            return LocationPollStartResult.PERMISSIONS_DENIED

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                if (location != null) {
                    action(Pair(location.latitude, location.longitude))
                    Logger.log( LogType.Event,
                        "LocationServices",
                        "Latitude : " + location.latitude
                    )
                    Logger.log( LogType.Event,
                        "LocationServices",
                        "Longitude : " + location.longitude
                    )
                }
            }

            override fun onStatusChanged(
                provider: String?,
                status: Int,
                extras: Bundle?
            ) {
                Logger.log(LogType.Event,"LocationServices", "$provider new status : $status")
            }

            override fun onProviderEnabled(provider: String?) {
                Logger.log(LogType.Event,"LocationServices", "$provider now enabled")
                if(!networkRunning && !networkKilled){
                    Logger.log(LogType.Info,"LocationServices", "Restarting network")
                    startPollThread(context, pollTimeMs, minDist, action)
                }
            }

            override fun onProviderDisabled(provider: String?) {
                Logger.log(LogType.Event,"LocationServices", "$provider now disabled")
                if(networkProvider == provider){
                    Logger.log(LogType.Info,"LocationServices", "Active provider disabled, restarting network")
                    networkRunning = false
                    startPollThread(context, pollTimeMs, minDist, action)
                }
            }
        }

        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, Looper.myLooper())
        val locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, Looper.myLooper())
        val locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        /*
        Will call requestLocationUpdates with specified provider and start by entering the result of
        lastKnownLocation into action.
        provider: the type of location you want to use.
         */
        fun startLocUpdates(provider : String){
            locationManager.requestLocationUpdates(provider, pollTimeMs, minDist, locationListener)
            if(locationManager.getLastKnownLocation(provider) != null)
                action(Pair(locationManager.getLastKnownLocation(provider).latitude, locationManager.getLastKnownLocation(provider).longitude))
            networkRunning = true
            networkProvider = provider
        }

        if(locationGps != null && locationNetwork != null && hasGps && hasNetwork){
            if(locationGps.accuracy > locationNetwork.accuracy){
                Logger.log(LogType.Info,"LocationServices", "Using network location")
                startLocUpdates(LocationManager.NETWORK_PROVIDER)
            }else{
                Logger.log(LogType.Info,"LocationServices", "Using gps location")
                startLocUpdates(LocationManager.GPS_PROVIDER)
            }
            return LocationPollStartResult.HYBRID
        }
        else if(hasGps){
            Logger.log(LogType.Info,"LocationServices", "Defaulting to gps location")
            startLocUpdates(LocationManager.GPS_PROVIDER)
            return LocationPollStartResult.GPS_ONLY
        }
        else if(hasNetwork){
            Logger.log(LogType.Info,"LocationServices", "Gps unavailable, using network location")
            startLocUpdates(LocationManager.GPS_PROVIDER)
            return LocationPollStartResult.NETWORK_ONLY
        }
        else{
            return LocationPollStartResult.LOCATION_UNAVAILABLE
        }
    }
}