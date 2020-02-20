package com.uu_uce

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission

class PermissionServices(context : Context, activity: Activity) {
    private val mContext = context
    private val mActivity = activity

    /*Takes a list of strings describing permissions of the form of
    Manifest.permission.ACCESS_COARSE_LOCATION and asks for permission to use those permissions*/
    fun getPermissions(permissions: List<String>) {
        val neededPermissions: MutableList<String> = mutableListOf()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        for (i in permissions) {
            if (checkSelfPermission(mContext, i) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(i)
            }
        }
        if(neededPermissions.size > 0){
            requestPermissions(mActivity, neededPermissions.toTypedArray(),1)
        }
    }
}