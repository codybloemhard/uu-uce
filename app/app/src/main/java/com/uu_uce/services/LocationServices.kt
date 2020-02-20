package com.uu_uce.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat.checkSelfPermission

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

/*
Will poll the location for you.
 */
class LocationServices{
    companion object {
        val permissionsNeeded = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var networkRunning = false
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

                    action(Pair(location!!.latitude, location!!.longitude))
                    Log.d(
                        "LocationServices",
                        " Network Latitude : " + location!!.latitude
                    )
                    Log.d(
                        "LocationServices",
                        " Network Longitude : " + location!!.longitude
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