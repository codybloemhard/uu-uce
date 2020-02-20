package com.uu_uce

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

class LocationServices (context: Context){
    private val mContext = context

    private var hasGps = false
    private var hasNetwork = false

    var locationNetwork: Location? = null
    var networkRunning = false

    fun startLocNet(action: (Pair<Double, Double>) -> Unit) {
        //Check if the network is running, might not be the best way to do this.
        if(networkRunning) {
            Log.d("LocationServices", "LocationNetwork already running")
            return
        }

        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!hasGps && !hasNetwork) return

        Log.d(
            "LocationServices",
            "gpsEnabled: $hasGps, networkEnabled: $hasNetwork"
        )

        var result = PackageManager.PERMISSION_DENIED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            result = checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
            Log.d("LocationServices.getLoc", "permissions: $result")
        }
        if (result != PackageManager.PERMISSION_GRANTED) return

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                if (location != null) {
                    locationNetwork = location

                    action(Pair(locationNetwork!!.latitude, locationNetwork!!.longitude))
                    Log.d(
                        "LocationServices",
                        " Network Latitude : " + locationNetwork!!.latitude
                    )
                    Log.d(
                        "LocationServices",
                        " Network Longitude : " + locationNetwork!!.longitude
                    )
                }
            }

            override fun onStatusChanged(
                provider: String?,
                status: Int,
                extras: Bundle?
            ) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onProviderEnabled(provider: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onProviderDisabled(provider: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0F, locationListener)

        networkRunning = true
        Log.d("LocationServices", "Started network")
    }
}