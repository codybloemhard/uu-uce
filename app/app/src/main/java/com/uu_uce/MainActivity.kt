package com.uu_uce

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger
import com.uu_uce.ui.TouchParent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : TouchParent() {
    private var loc : Pair<Double, Double>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val locationServices = LocationServices(this)
        val permissionServices = PermissionServices(this, this)
        Log.d("MainActivity", "Successfully created services")

        // Request location permission
        permissionServices.getPermissions(listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))

        startApp()

        val button : Button = findViewById(R.id.gpsButton)

        button.setOnClickListener {
            locationServices.startLocNet(::updateLoc)
        }

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

    private fun updateLoc(newLoc : Pair<Double, Double>){
        loc = newLoc
        gpstext.text = "Latitude: ${loc?.first} \nlongitude: ${loc?.second}"
    }

    private fun startApp() {
        setContentView(R.layout.activity_main)
        addChild(Flinger(this, ::action))
    }

    // Respond to permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permissions granted")
            } else {
                Log.d("MainActivity", "Permissions were not granted")
            }
        }
    }
}
