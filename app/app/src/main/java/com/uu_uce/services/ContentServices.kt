package com.uu_uce.services
/*
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

/*
Gets missing files by calling missingFiles and gets them by calling getFiles
requiredPaths: A list of file paths to all files that are required to run onCompleteAction.
activity: The activity from which this function is called.
onCompleteAction : A function to be executed when all files are present.
It will call getFiles for all missing files.
 */
fun updateFiles(requiredFilePaths : List<String>, activity : Activity, onCompleteAction : (() -> Unit)){
    val missingFiles = findMissingFilePaths(requiredFilePaths)
    if(missingFiles.count() > 0){
        getPermissions(activity, permissionsNeeded, EXTERNAL_FILES_REQUEST)
        getFiles(missingFiles, activity, onCompleteAction)
    }
    else{
        onCompleteAction()
    }
}

/*
Gets a list of the filepaths of missing files
requestedPaths: A list of file paths to all files that are requested.
It will return a list of the file paths of all missing files in String format.
 */
fun findMissingFilePaths(requestedFilePaths : List<String>) : List<String>{
    val missingFilePaths : MutableList<String> = mutableListOf()
    val adding : MutableMap<String, Boolean> = mutableMapOf()
    for(filePath in requestedFilePaths){
        if(!File(filePath).exists() && !adding.containsKey(filePath)){
            missingFilePaths.add(filePath)
            adding[filePath] = true
        }
    }
    return missingFilePaths
}

/*
Downloads specified files and executes action when all requested files are present.
requiredPaths: A list of file paths to all files that are to be downloaded.
activity: The activity from which this function is called.
onCompleteAction : A function to be executed when all files are present.
It will download all files and start onCompleteAction.
 */
fun getFiles(requiredFilePaths : List<String>, activity: Activity, onCompleteAction : (() -> Unit)){
    val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val downloadDone : MutableMap<Long, Boolean> = mutableMapOf()

    for(filePath in requiredFilePaths){ //TODO implement our own server
        val fileName = filePath.split('/').last()
        val request = DownloadManager.Request(Uri.parse(filePath))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)

        //request.setDestinationUri(Uri.parse(filePath))
        request.setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val id : Long = manager.enqueue(request)
        downloadDone[id] = false
        manager.enqueue(request)
    }

    val onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if(downloadDone[id] != null){
                downloadDone[id] = true
            }
            if(downloadDone.all{entry -> entry.value}){
                onCompleteAction()
            }
        }
    }
    activity.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
}*/