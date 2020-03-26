package com.uu_uce.services

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import java.io.File


val permissionsNeeded = listOf(Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE)

fun updateFiles(requiredFilePaths : List<String>, activity : Activity, onCompleteAction : (() -> Unit)){
    val missingFiles = findMissingFilePaths(requiredFilePaths)
    if(missingFiles.count() > 0){
        getPermissions(activity, permissionsNeeded)
        getFiles(missingFiles, activity, onCompleteAction)
    }
    else{
        onCompleteAction()
    }
}

fun findMissingFilePaths(requiredFilePaths : List<String>) : List<String>{
    val missingFilePaths : MutableList<String> = mutableListOf()
    val adding : MutableMap<String, Boolean> = mutableMapOf()
    for(filePath in requiredFilePaths){
        if(!File(filePath).exists() && !adding.containsKey(filePath)){
            missingFilePaths.add(filePath)
            adding[filePath] = true
        }
    }
    return missingFilePaths
}

fun getFiles(requiredFilePaths : List<String>, activity: Activity, onCompleteAction : (() -> Unit)){
    val paths = listOf("https://cdn.filesend.jp/private/gzyfYl_REWa7bS4B7m0gZqucwaSr6ysvxHdmxnqBgnnTmx4SgCaytFtS5cJ8LTRK/test.png", "https://cdn.filesend.jp/private/Ghxa0g5o3Cbe0I4kB6xoCBeLh6S0KhadAMsnuvd7ivVOBLjU59YHG0nLn5U8JwYj/zoo.mp4", "https://cdn.filesend.jp/private/FOqUTH6cTImIIVkvazod_oJK8pmFZaWJmwW98c9F377fIXJpsyB5tqomsWuKjx00/zoothumbnail.png")
    val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    for(filePath in paths /*requiredFilePaths*/){ //TODO implement our own server
        val fileName = filePath.split('/').last()
        val request = DownloadManager.Request(Uri.parse(filePath))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)

        //request.setDestinationUri(Uri.parse(filePath))
        request.setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        manager.enqueue(request)
    }

    val onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            onCompleteAction()
        }
    }
    activity.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
}