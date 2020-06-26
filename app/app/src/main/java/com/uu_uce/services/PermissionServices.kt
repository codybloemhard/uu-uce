package com.uu_uce.services

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission

// Request codes
const val EXTERNAL_FILES_REQUEST    = 0
const val EXTERNAL_PHOTO_REQUEST    = 1
const val EXTERNAL_VIDEO_REQUEST    = 2
const val LOCATION_REQUEST          = 3
const val PHOTOSTORAGE_REQUEST      = 4
const val VIDEOSTORAGE_REQUEST      = 5

/**
 * Asks the user for missing permissions.
 * @param[activity] the current activity.
 * @param[permissions] a list of strings describing permissions. (e.g. Manifest.permission.ACCESS_COARSE_LOCATION)
 * @param[requestCode] a code by which the onRequestPermissionsResult listener knows what to do with the response.
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

/**
 * Finds which permissions have not been granted.
 * @param[context] the current context.
 * @param[permissions] a list of required permissions.
 * @return a list of all missing permissions.
 */
fun missingPermissions(context: Context, permissions: List<String>): List<String> {
    val neededPermissions: MutableList<String> = mutableListOf()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
    for (i in permissions) {
        if (checkSelfPermission(context, i) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(i)
        }
    }
    return neededPermissions
}


