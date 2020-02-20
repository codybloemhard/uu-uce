package com.uu_uce.services

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission

/*
Takes: a context and activity and a list of strings describing permissions.
Usually, context = activity = this.
Permissions of the form of Manifest.permission.ACCESS_COARSE_LOCATION.
Does:
Asks for permission to use those permissions.
*/

fun getPermissions(context: Context, activity: Activity, permissions: List<String>) {
    val neededPermissions: MutableList<String> = mutableListOf()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    for (i in permissions) {
        if (checkSelfPermission(context, i) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(i)
        }
    }
    if(neededPermissions.size > 0){
        requestPermissions(activity, neededPermissions.toTypedArray(),1)
    }
}