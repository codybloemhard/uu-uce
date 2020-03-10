package com.uu_uce.exampleScenes

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.uu_uce.GeoMap
import com.uu_uce.R
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.LocationServices
import com.uu_uce.services.getPermissions
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger
import com.uu_uce.ui.TouchParent
import kotlinx.android.synthetic.main.activity_main.*

class GpsExample : TouchParent() {
    private var loc : Pair<Double, Double>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val locationServices = LocationServices()
        Logger.log(LogType.Info, "GPS", "Successfully created services")
        // Request location permission
        getPermissions(this, this, LocationServices.permissionsNeeded)

        setContentView(R.layout.activity_main)
        addChild(Flinger(this, ::action))

        val button : Button = findViewById(R.id.gpsButton)

        button.setOnClickListener {
            val result = locationServices.startPollThread(this, 5000, 0F, ::updateLoc)
            Toast.makeText(this, result.toString(), Toast.LENGTH_SHORT).show()
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

    // Respond to permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Logger.log(LogType.Info,"GPS", "Permissions granted")
            else
                Logger.log(LogType.Info,"GPS", "Permissions were not granted")
        }
    }
}