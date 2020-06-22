package com.uu_uce.services

import android.app.Activity
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MediaServices(private val activity: Activity) {

    private fun fieldbookDir() : File {
        @Suppress("DEPRECATION")
        return File(
            Environment.getExternalStorageDirectory(),
            "UU-UCE/Fieldbook"
        ).also { it.mkdirs() }
    }

    fun fieldbookImageLocation(): Uri {

        val outputUri: Uri
        val fileName = "IMG_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}_UCE"

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Fieldbook")
                }
            }

            outputUri = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)!!

        } else {

            val myDir: File = File(fieldbookDir(),"Pictures").also{
                it.mkdirs()
            }

            outputUri = FileProvider.getUriForFile(
                activity,
                "com.uu-uce.fileprovider",
                createTempFile(
                    fileName,
                    ".jpg",
                    myDir
                )
            )
        }

        return outputUri
    }

    fun fieldbookVideoLocation() : Uri {
        val outputUri: Uri
        val fileName = "VID_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}_UCE"

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                    put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Fieldbook")
                }
            }

            outputUri = activity.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values)!!

        } else {

            val myDir: File = File(fieldbookDir(),"Videos").also{
                it.mkdirs()
            }

            outputUri = FileProvider.getUriForFile(
                activity,
                "com.uu-uce.fileprovider",
                createTempFile(
                    fileName,
                    ".mp4",
                    myDir
                )
            )
        }

        return outputUri
    }

    fun addImageToGallery(path: String) {

        //todo
        MediaScannerConnection.scanFile(
            activity,
            arrayOf(path),
            arrayOf("image/*"),
            null
        )

        /*
        try {
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                val f = File(path)
                mediaScanIntent.data = Uri.fromFile(f)
                activity.sendBroadcast(mediaScanIntent)
            }
        } catch (e: Exception) {

        }
         */
    }
    fun addVideoToGallery(path: String) {

        MediaScannerConnection.scanFile(
            activity,
            arrayOf(path),
            arrayOf("video/*"),
            null
        )

        /*
        try {
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                val f = File(path)
                mediaScanIntent.data = Uri.fromFile(f)
                activity.sendBroadcast(mediaScanIntent)
            }
        } catch (e: Exception) {

        }
         */
    }

    fun getImageFileNameFromURI(path: Uri) : String {
        var fileName = ""
        activity.contentResolver.query(path, null, null, null, null)?.use {
            val nameIndex =
                it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            it.moveToFirst()
            fileName = File(it.getString(nameIndex)).nameWithoutExtension
        }
        return fileName
    }

    fun getVideoFileNameFromURI(path: Uri) : String {
        var fileName = ""
        activity.contentResolver.query(path, null, null, null, null)?.use {
            val nameIndex =
                it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            it.moveToFirst()
            fileName = File(it.getString(nameIndex)).nameWithoutExtension
        }
        return fileName
    }

    fun getImagePathFromURI(uri: Uri?): Uri {
        var filePath = ""
        val wholeID : String

        try{
            wholeID = DocumentsContract.getDocumentId(uri)
        }
        catch(e : java.lang.Exception){
            return Uri.EMPTY
        }

        // Split at colon, use second item in the array
        val id = wholeID.split(":").toTypedArray()[1]
        //todo
        val column = arrayOf(MediaStore.Images.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"
        val cursor: Cursor? = activity.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            column, sel, arrayOf(id), null
        )
        if(cursor != null){
            val columnIndex: Int = cursor.getColumnIndex(column[0])
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return Uri.parse(filePath)
    }

    fun getVideoPathFromURI(uri: Uri?): Uri {
        var filePath = ""
        val wholeID: String = DocumentsContract.getDocumentId(uri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":").toTypedArray()[1]
        val column = arrayOf(MediaStore.Video.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Video.Media._ID + "=?"
        val cursor: Cursor? = activity.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            column, sel, arrayOf(id), null
        )
        if (cursor != null) {
            val columnIndex: Int = cursor.getColumnIndex(column[0])
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return Uri.parse(filePath)
    }

    fun generateMissingVideoThumbnail(videoUri: Uri): Uri {
        val fileName = getFileName(videoUri.path.toString())

        val filePath =
            activity.getExternalFilesDir(null).toString() +
                    "/PinContent/Videos/Thumbnails/thumbnail_" +
                    fileName +
                    ".jpeg"

        val file = File(filePath)

        return if (!file.exists() || !file.canRead()) {
            makeVideoThumbnail(
                videoUri,
                "PinContent/Videos/Thumbnails",
                fileName
            )
        } else {
            Uri.parse(filePath)
        }
    }

    fun makeImageThumbnail(uri: Uri?, directory: String, fileName: String? = null): Uri {
        return try {
            saveThumbnail(
                activity.contentResolver.openInputStream(uri!!).let {
                    BitmapFactory.decodeStream(it)
                },
                directory,
                fileName
            )
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }

    fun makeVideoThumbnail(uri: Uri?, directory: String, fileName: String? = null): Uri {
        return try {
            saveThumbnail(
                MediaMetadataRetriever().apply {
                    setDataSource(activity, uri)
                }.getFrameAtTime(1000, 0),
                directory,
                fileName
            )
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }

    private fun saveThumbnail(bitmap: Bitmap, directory: String, fileName: String?): Uri {
        val dir = File(
            activity.getExternalFilesDir(null),
            directory
        ).apply {
            mkdirs()
        }

        val file = if (fileName != null) {
            File(
                dir,
                "thumbnail_$fileName.jpeg"
            )
        } else {
            File(
                dir,
                "thumbnail_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}.jpeg"
            ).also {
                println(it.toString())
            }
        }

        FileOutputStream(file).also {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, it)
        }.apply {
            flush()
            close()
        }

        return file.toUri()
    }
}

enum class DateTimeFormat{
    FILE_PATH,
    FIELDBOOK_ENTRY
}

fun getCurrentDateTime(dtf: DateTimeFormat): String {
    val pattern: String = when(dtf) {
        DateTimeFormat.FILE_PATH        -> "yyyyMMdd_HHmmss"
        DateTimeFormat.FIELDBOOK_ENTRY  -> "dd-MM-yyyy HH:mm"
    }

    return SimpleDateFormat(
        pattern,
        Locale("nl-NL")
    ).format(
        Date()
    )
}