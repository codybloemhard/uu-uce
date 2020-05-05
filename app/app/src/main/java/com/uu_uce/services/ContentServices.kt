package com.uu_uce.services

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.preference.PreferenceManager
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


val permissionsNeeded = listOf(Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE)

private val universityName = "UU"
private val facultyName    = "GEO"
private val areaName       = "PYR"
private val className      = "1A"
private val serverURL      = "http://uce.edu"

private lateinit var task : AsyncTask<Void, Void, Int>

private lateinit var sharedPref : SharedPreferences

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
fun findMissingFilePaths(requestedFilePaths : List<String>) : List<Pair<String, String>>{
    val missingFilePaths : MutableList<Pair<String, String>> = mutableListOf()
    val adding : MutableMap<String, Boolean> = mutableMapOf()
    for(filePath in requestedFilePaths){
        if(!File(filePath).exists() && !adding.containsKey(filePath)){
            val fileName = filePath.split('/').last()
            missingFilePaths.add(Pair(filePath, fileName))
            adding[fileName] = true
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
fun getFiles(requiredFilePaths : List<Pair<String, String>>, activity: Activity, onCompleteAction : (() -> Unit)){
    val jobList : MutableList<Job> = mutableListOf()
    val wifiManager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
    val networkDownloadAllowed = sharedPref.getBoolean("com.uu_uce.NETWORK_DOWNLOADING", false)
    if (!wifiManager!!.isWifiEnabled && networkDownloadAllowed){
        Toast.makeText(activity, "Enable wifi or allow network downloading", Toast.LENGTH_LONG).show()
        return
    }
    for(filePath in requiredFilePaths){
        jobList.add(GlobalScope.launch{
            downloadFile(
                URL(serverURL + "/api/files/download/" + filePath.second), filePath.first)
        })
    }

    GlobalScope.launch{
        for(job in jobList){
            job.join()
        }
        onCompleteAction()
    }
}

/*
Downloads specified file from URL.
targetUrl: The URL from which a file needs to be downloaded
fileDestination: The filepath to which the downloaded file will be downloaded.
It will download the file.
 */
fun downloadFile(targetUrl : URL, fileDestination : String) {
    with(targetUrl.openConnection() as HttpURLConnection) {
        requestMethod = "GET"

        Logger.log(LogType.Event, "ContentServices", "\nSent 'GET' request to URL : $targetUrl; Response Code : $responseCode")

        inputStream.use { inputStream ->
            val splitPath = fileDestination.split('/')
            val folder = File(splitPath.take(splitPath.count() - 1).fold(""){s1, s2 -> "$s1${File.separator}$s2"})

            if (!folder.exists()) {
                folder.mkdirs()
            }
            val file = File(fileDestination)
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
    }
}

