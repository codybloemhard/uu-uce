package com.uu_uce

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger
import com.uu_uce.ui.TouchParent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : TouchParent() {
    /*lateinit var locationManager: LocationManager
    private var hasGps = false
    private var hasNetwork = false
    private var locationGps: Location? = null*/
    private var locationNetwork: Location? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permission
        getPermissions(listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))

        startApp()

        var button : Button = findViewById(R.id.gpsButton)
        button.setOnClickListener {getLoc()}
    }

    private fun action(dir: FlingDir, delta: Float) {
        val intent = Intent(this, GeoMap::class.java)
        startActivity(intent)
        if (dir == FlingDir.VER) return
        if (delta > 0.0f)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        else
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // Respond to permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()

                startApp()
            } else {
                Toast.makeText(this, "Permissions were not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startApp() {
        setContentView(R.layout.activity_main)
        addChild(Flinger(this, ::action))
    }

    /*Takes a list of strings describing permissions of the form of
    Manifest.permission.ACCESS_COARSE_LOCATION and asks for permission to use those permissions*/
    private fun getPermissions(permissions: List<String>) {
        var neededPermissions: MutableList<String> = mutableListOf()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (i in permissions) {
                if (checkSelfPermission(i) != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(i)) {
                        // Somehow inform the user that location permission is required
                    }
                    neededPermissions.add(i)
                }
            }
            if(neededPermissions.size > 0){
                requestPermissions(neededPermissions.toTypedArray(), 1)
            }
        }
    }

    private fun getLoc() {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        var hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (hasGps) {
            Log.d("CodeAndroidLocation", "gpsEnabled")
            var result = PackageManager.PERMISSION_DENIED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                result = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                Log.d("getLoc", "permissions: $result")
            }
            if (result == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Starting gps", Toast.LENGTH_SHORT).show()
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0F,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                locationNetwork = location
                                gpstext.text = ""
                                gpstext.append("\nNetwork ")
                                gpstext.append("\nLatitude : " + locationNetwork!!.latitude)
                                gpstext.append("\nLongitude : " + locationNetwork!!.longitude)
                                Log.d(
                                    "getLoc",
                                    " Network Latitude : " + locationNetwork!!.latitude
                                )
                                Log.d(
                                    "getLoc",
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
                    })
            }
            else{
                Toast.makeText(this, "No permission to use location", Toast.LENGTH_LONG).show()
                getPermissions(listOf(Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }
        else {
            Toast.makeText(this, "GPS not available", Toast.LENGTH_LONG).show()
        }
    }
}
