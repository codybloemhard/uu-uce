@file:Suppress("BlockingMethodInNonBlockingContext") // Downloads can not be suspended

package com.uu_uce.services

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.util.JsonReader
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.uu_uce.R
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

val permissionsNeeded = listOf(Manifest.permission.INTERNET)

private const val downloadURL = "/api/files/download/"

private lateinit var sharedPref : SharedPreferences

/**
 * Gets missing files by calling missingFiles and gets them by calling getFiles.
 * @param[requiredFilePaths] a list of file paths to all files that are required to run onCompleteAction.
 * @param[activity] the activity from which this function is called.
 * @param[onCompleteAction] a function to be executed when all files are present.
 */
fun updateFiles(requiredFilePaths : List<String>, activity : Activity, onCompleteAction : ((success : Boolean) -> Unit)= {}, progressAction : (Int) -> Unit = {}) {
    val missingFiles = findMissingFilePaths(requiredFilePaths)
    if (missingFiles.count() > 0) {
        getPermissions(activity, permissionsNeeded, EXTERNAL_FILES_REQUEST)
        getFiles(missingFiles, activity, onCompleteAction, progressAction)
    }
    else {
        GlobalScope.launch { onCompleteAction(true) }
    }
}

/**
 * Gets a list of the filepaths of missing files.
 * @param[requestedFilePaths] a list of file paths to all files that are requested.
 * @return a list of the file paths of all missing files in String format.
 */
fun findMissingFilePaths(requestedFilePaths : List<String>) : List<Pair<String, String>>{
    val missingFilePaths : MutableList<Pair<String, String>> = mutableListOf()
    val adding : MutableMap<String, Boolean> = mutableMapOf()
    for(filePath in requestedFilePaths){
        val file = File(filePath)
        if((!file.exists() || !file.canRead()) && !adding.containsKey(filePath)){
            val fileName =
                getFileName(filePath) // Because we use UUID4 names there can never be a / or . in the file name
            missingFilePaths.add(Pair(filePath, fileName))
            adding[fileName] = true
        }
    }
    return missingFilePaths
}

/**
 * Gets the file name from a file path.
 * @param[filePath] the file path the file name will be extracted from.
 * @return the file name.
 */
fun getFileName(filePath: String) = filePath.split('/').last().split('.').first()

/**
 * Downloads specified files and executes action when all requested files are present.
 * @param[requiredFilePaths] a list of file paths to all files that are to be downloaded.
 * @param[activity] the activity from which this function is called.
 * @param[onCompleteAction] a function to be executed when all files are present.
 */
fun getFiles (
    requiredFilePaths : List<Pair<String, String>>,
    activity: Activity,
    onCompleteAction : ((success : Boolean) -> Unit) = {},
    progressAction : (Int) -> Unit = {}
) {
    var allSucceed = true
    val jobList : MutableList<Job> = mutableListOf()
    val wifiManager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
    val serverURL = sharedPref.getString("com.uu_uce.SERVER_IP", "").toString()

    if(serverURL == ""){
        Toast.makeText(activity, activity.getString(R.string.contentservices_noserver), Toast.LENGTH_LONG).show()
        onCompleteAction(false)
        return
    }

    val networkDownloadAllowed = sharedPref.getBoolean("com.uu_uce.NETWORK_DOWNLOADING", false)
    if (!wifiManager!!.isWifiEnabled && !networkDownloadAllowed) {
        Toast.makeText(activity, activity.getString(R.string.contentservices_nowifi_downloadblock), Toast.LENGTH_LONG).show()
        onCompleteAction(false)
        return
    }
    activity.runOnUiThread {
        Toast.makeText(activity, activity.getString(R.string.contentservices_downloading), Toast.LENGTH_SHORT).show()
    }
    for (filePath in requiredFilePaths) {
        jobList.add(GlobalScope.launch {
            val success = downloadFile(
                URL(serverURL + downloadURL + filePath.second), filePath.first, progressAction)
            if (!success) allSucceed = false
        })
    }

    GlobalScope.launch {
        for (job in jobList) {
            job.join()
        }
        onCompleteAction(allSucceed)
    }
}

/**
 * Downloads specified file from URL.
 * @param[targetUrl] the URL from which a file needs to be downloaded.
 * @param[fileDestination] the filepath to which the downloaded file will be downloaded.
 * @return a boolean representing if downloading was successful.
 */
fun downloadFile(targetUrl : URL, fileDestination : String, progressAction : (Int) -> Unit = {}) : Boolean {
    try{
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
                    val length = contentLength
                    var total = 0.0
                    val buffer = ByteArray(4 * 1024) // or other buffer size
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        if(length > 0){
                            total += read
                            progressAction((total / length * 100).toInt())
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                    return true
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

/**
 * Gets id of first file that matches query from server.
 * @param[type] the type of content that is queried. [pin, map, content]
 * @param[activity] the current activity, used for getting preferences.
 * @param[onCompleteAction] function that uses takes the found id.
 */
fun queryServer(type: String, activity: Activity, onCompleteAction: ((result: String) -> Unit)) {
    sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
    val serverURL = sharedPref.getString("com.uu_uce.SERVER_IP", "").toString()

    if(serverURL == "") {
        onCompleteAction("")
        return
    }

    val queryApi = URL("$serverURL/api/files/query")
    val reqParam =
        "{\"" +
                URLEncoder.encode("type", "UTF-8") +
                "\":\"" +
                URLEncoder.encode(type, "UTF-8") +
                "\"}"

    GlobalScope.launch {
        try {
            with(queryApi.openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "POST"

                val wr = OutputStreamWriter(outputStream)
                wr.write(reqParam)
                wr.flush()

                println("URL : $url")
                println("Response Code : $responseCode")

                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val reader = JsonReader(InputStreamReader(this.inputStream))
                        var idRead = false
                        reader.beginArray()
                        try {
                            while (reader.hasNext()) {
                                if (idRead) {
                                    reader.skipValue()
                                    continue
                                }
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    if (idRead) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (reader.nextName()) {
                                        "id" -> {
                                            onCompleteAction(reader.nextString())
                                            idRead = true
                                        }
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            }
                            reader.endArray()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    else -> {
                        Logger.error("ContentServices", "Query failed, response: $responseCode")
                    }
                }
            }
        }
        catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Unzips specified file in place.
 * @param[zipPath] the file path to the file that will be unzipped
 * @param[progressAction] the action that takes the progress of unzipping.
 * @return a boolean representing if unzipping was successful.
 */
fun unpackZip(zipPath: String, progressAction: (Int) -> Unit = {}): Boolean {
    val splitPath = zipPath.split('/')
    val destinationPath = splitPath.take(splitPath.count() - 1)
        .fold(splitPath.first()) { s1, s2 -> "$s1${File.separator}$s2" }.drop(1)

    try{
        val zipSize =  ZipFile(zipPath).size()
        val `is`: InputStream
        val zis: ZipInputStream
        try {
            var filename: String
            `is` = FileInputStream(zipPath)
            zis = ZipInputStream(BufferedInputStream(`is`))
            var ze: ZipEntry?
            val buffer = ByteArray(1024)
            var count: Int
            var i = 0.0
            while (zis.nextEntry.also { ze = it } != null) {
                if(ze != null){
                    filename = ze!!.name

                    // Need to create directories if not exists, or
                    // it will generate an Exception...
                    if (ze!!.isDirectory) {
                        val fmd = File(destinationPath + File.separator + filename)
                        fmd.mkdirs()
                        continue
                    }
                    val fout = FileOutputStream(destinationPath + File.separator + filename)
                    while (zis.read(buffer).also { count = it } != -1) {
                        fout.write(buffer, 0, count)
                    }

                    i++
                    progressAction((i / zipSize * 100).toInt())

                    fout.close()
                    zis.closeEntry()
                }
            }
            zis.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        File(zipPath).delete()
        return true
    }
    catch (e: ZipException) {
        File(zipPath).delete()
        return false
    }
}

/**
 * Recursively calculates the size of all files in a directory.
 * @param[dir] the filepath to the directory whichs size will be calculated.
 * @return the directory size in bytes.
 */
fun dirSize(dir: File): Long {
    if (dir.exists()) {
        var result: Long = 0
        val fileList = dir.listFiles()
        if (fileList != null) {
            for (i in fileList.indices) {
                // Recursive call if it's a directory
                result += if (fileList[i].isDirectory) {
                    dirSize(fileList[i])
                }
                else {
                    // Sum the file size in bytes
                    fileList[i].length()
                }
            }
            return result // return the file size
        }
    }
    return 0
}

/**
 * Converts a number of bytes to a the largest appropriate unit with the unit attached.
 * @param[bytes] the amount of bytes that should be displayed.
 * @return a human readable size in string format.
 */
fun writableSize(bytes : Long) : String {
    val units = listOf("B", "KB", "MB", "GB")

    var curSize = bytes.toDouble()
    var curUnit = 0
    while (curSize > 512) {
        curUnit++
        curSize /= 1024
    }

    return String.format("%.2f", curSize) + " " + units[curUnit]
}