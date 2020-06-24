package com.uu_uce.services

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.uu_uce.FieldbookEditor.Companion.currentPath
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles all functions for images and videos
 *
 * @property[activity] the associated activity
 */
class MediaServices(private val activity: Activity) {

    /**
     * Gives the location for storing Fieldbook content for API's before N (24)
     *
     * @return the path to the fieldbook directory, as a File
     */
    @Suppress("DEPRECATION")
    private fun fieldbookDir(): File {
        return File(
            Environment.getExternalStorageDirectory(),
            "UU-UCE/Fieldbook"
        ).also { it.mkdirs() }
    }

    /**
     * Gives the file, filename and file location to store newly captured images
     *
     * @return the uri for the to-be-stored image
     */
    fun fieldbookImageLocation(): Uri {
        val outputUri: Uri
        val fileName = "IMG_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}_UCE"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Fieldbook")
                }
            }

            outputUri = activity.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!

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
                ).apply {
                    currentPath = absolutePath
                }
            )
        }

        return outputUri
    }

    /**
     * Gives the file, filename and file location to store newly captured videos
     *
     * @return the uri for the to-be-stored video
     */
    fun fieldbookVideoLocation(): Uri {
        val outputUri: Uri
        val fileName = "VID_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}_UCE"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                    put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Fieldbook")
                }
            }

            outputUri = activity.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            )!!

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
                ).apply {
                    currentPath = absolutePath
                }
            )
        }

        return outputUri
    }

    /**
     * Makes an image/video visible to the mediascanner, so it can be shown in the gallery
     * Used for API's < 24
     *
     * @param[path] the path to the image that we want to be made visible to the gallery
     */
    @Suppress("DEPRECATION")
    fun addMediaToGallery(path: String) {
        try {
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                val f = File(path)
                mediaScanIntent.data = Uri.fromFile(f)
                activity.sendBroadcast(mediaScanIntent)
            }
        } catch (e: Exception) {

        }
    }

    /**
     * Gets the filename of an image from the MediaStore MediaColumns
     *
     * @param[uri] the Uri that refers to the image file
     * @return the filename of the image
     */
    fun getImageFileNameFromURI(uri: Uri): String {
        var fileName = ""
        activity.contentResolver.query(uri, null, null, null, null)?.use {
            val nameIndex =
                it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            it.moveToFirst()
            fileName = File(it.getString(nameIndex)).nameWithoutExtension
        }
        return fileName
    }

    /**
     * Gets the filename of an video from the MediaStore MediaColumns
     *
     * @param[uri] the Uri that refers to the video file
     * @return the filename of the video
     */
    fun getVideoFileNameFromURI(uri: Uri): String {
        var fileName = ""
        activity.contentResolver.query(uri, null, null, null, null)?.use {
            val nameIndex =
                it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            it.moveToFirst()
            fileName = File(it.getString(nameIndex)).nameWithoutExtension
        }
        return fileName
    }

    /**
     * Finds the full path to an image, using the path provided by the File Provider
     *
     * @param[uri] the (FileProvider) Uri that refers to the image file
     * @return the entire path to the image, contained in a Uri
     */
    fun getImagePathFromURI(uri: Uri?): Uri {
        var filePath = ""
        val wholeID: String

        try {
            wholeID = DocumentsContract.getDocumentId(uri)
        } catch (e: java.lang.Exception) {
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
        if (cursor != null) {
            val columnIndex: Int = cursor.getColumnIndex(column[0])
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return Uri.parse(filePath)
    }

    /**
     * Finds the full path to an video, using the path provided by the File Provider
     *
     * @param[uri] the (FileProvider) Uri that refers to the video file
     * @return the entire path to the video, contained in a Uri
     */
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

    /**
     * Called whenever a thumbnail is not present in the PinContent
     * Checks whether a thumbnail has been generated earlier
     * If it doesn't exist yet, it generates a new thumbnail
     *
     * @param[videoUri] the uri of the video that's missing a thumbnail
     * @param[thumbnailDirectory] the directory to store new thumbnails to
     * @return the Uri for the generated thumbnail
     */
    fun generateMissingVideoThumbnail(videoUri: Uri, thumbnailDirectory: String): Uri {
        val fileName = getFileName(videoUri.path.toString())

        val filePath =
            activity.getExternalFilesDir(null).toString() +
                    File.separator +
                    thumbnailDirectory +
                    File.separator +
                    "thumbnail_$fileName.jpeg"

        val file = File(filePath)

        return if (!file.exists() || !file.canRead()) {
            makeVideoThumbnail(
                videoUri,
                thumbnailDirectory,
                fileName
            )
        } else {
            Uri.parse(filePath)
        }
    }

    /**
     * Loads the image as a bitmap and saves it to the dedicated directory
     *
     * @param[uri] the uri of the image that's missing a thumbnail
     * @param[directory] the directory to store new thumbnails to
     * @param[fileName] the name of the image that's missing a thumbnail. For newly taken images, a new name is generated (so null is passed)
     * @return the Uri that refers to the newly made thumbnail
     */
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

    /**
     * Gets a frame in the video as a bitmap and saves it to the dedicated directory
     *
     * @param[uri] the uri of the video that's missing a thumbnail
     * @param[directory] the directory to store new thumbnails to
     * @param[fileName] the name of the video that's missing a thumbnail. For newly taken videos, a new name is generated (so null is passed)
     * @return the Uri that refers to the newly made thumbnail
     */
    fun makeVideoThumbnail(uri: Uri?, directory: String, fileName: String? = null): Uri {
        return try {
            saveThumbnail(
                MediaMetadataRetriever().apply {
                    setDataSource(activity, uri)
                }.getFrameAtTime(20000000, 0),
                directory,
                fileName
            )
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }

    /**
     * Finds the directory and builds the name for the newly made thumbnail
     * Compresses the passed bitmap and saves it using afore mentioned name and directory
     *
     * @param[bitmap] the bitmap we will use as our thumbnail
     * @param[directory] the directory to store new thumbnails to
     * @param[fileName] the name for the thumbnail. For newly taken images/videos, a new name will be generated (so null is passed)
     * @return the Uri that refers to the newly made thumbnail
     */
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
            )
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

/**
 * An enum that indicates the format for the DateTime we will use
 *
 * @param[format] the format we want to use
 */
enum class DateTimeFormat(val format: String) {
    FILE_PATH("yyyyMMdd_HHmmss"),
    FIELDBOOK_ENTRY("dd-MM-yyyy HH:mm")
}

/**
 * Gives the DateTime in the requested format
 *
 * @param[dtf] the DataTimeFormat we will use
 * @return a string containing the Date and Time in the requested format
 */
fun getCurrentDateTime(dtf: DateTimeFormat): String {
    return SimpleDateFormat(
        dtf.format,
        Locale("nl-NL")
    ).format(
        Date()
    )
}