package com.uu_uce

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger
import com.uu_uce.ui.TouchParent

class MainActivity : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permission
        startApp()

        getPermissions(listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
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
            requestPermissions(neededPermissions.toTypedArray(), 1)
        }
    }
}
