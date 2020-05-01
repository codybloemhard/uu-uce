package com.uu_uce.services

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission

// Request codes
const val EXTERNAL_FILES_REQUEST    = 1
const val LOCATION_REQUEST          = 2
const val PHOTOCAMERA_REQUEST       = 3
const val VIDEOCAMERA_REQUEST       = 4

/*
Takes: a context and activity and a list of strings describing permissions.
Usually, context = activity = this.
Permissions of the form of Manifest.permission.ACCESS_COARSE_LOCATION.
Does:
Asks for permission to use those permissions.
*/
fun getPermissions(activity: Activity, permissions: List<String>, requestCode : Int) {
    val neededPermissions: MutableList<String> = mutableListOf()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    for (i in permissions) {
        if (checkSelfPermission(activity, i) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(i)
        }
    }
    if(neededPermissions.size > 0){
        requestPermissions(activity, neededPermissions.toTypedArray(), requestCode)
    }
}

/*
Takes: a context and activity and a list of strings describing permissions.
Usually, context = this.
Permissions of the form of Manifest.permission.ACCESS_COARSE_LOCATION.
Does:
Returns a list of missing permissions.
*/
fun checkPermissions(context: Context, permissions: List<String>) : List<String>{
    val neededPermissions: MutableList<String> = mutableListOf()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
    for (i in permissions) {
        if (checkSelfPermission(context, i) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(i)
        }
    }
    return neededPermissions
}