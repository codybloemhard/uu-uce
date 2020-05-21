package com.uu_uce

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.fieldbook.FieldbookViewModel
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.*
import com.uu_uce.services.*
import com.uu_uce.ui.createTopbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FieldbookEditor: AppCompatActivity() {

    companion object {
        //TODO: add checks on availability of storage et cetera
        lateinit var fieldbookDir : File

        const val REQUEST_IMAGE_UPLOAD  = 0
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_VIDEO_UPLOAD  = 2
        const val REQUEST_VIDEO_CAPTURE = 3

        var currentUri: Uri = Uri.EMPTY
    }

    private lateinit var viewModel   : FieldbookViewModel

    private lateinit var content     :  MutableList<ContentBlockInterface>

    private lateinit var rootView    : View
    private lateinit var scrollView  : ScrollView
    private lateinit var layout      : LinearLayout
    private lateinit var title       : EditText

    private var currentName = ""
    private var currentPath = ""

    private var latestBlockIndex    = 0
    private var currentBlockIndex   = 0
    private var fieldbookIndex      = -1

    private var editing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fieldbook_editor)

        val bundle = intent.extras

        if (bundle != null)
            fieldbookIndex = bundle.getInt("fieldbook_index")

        createTopbar(this,"Edit pins")

        viewModel = this.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        }

        /**
         * TODO: used function is deprecated from API 29 and onwards. Can still be used, because android:requestLegacyExternalStorage="true" in the manifest
         * Eventually switch to the MediaStore API. Doesn't need READ/WRITE permissions anymore -> only to be imported for API 28 and lower
         */
        getPermissions(this, listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE), PHOTOCAMERA_REQUEST
        )

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
            this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            fieldbookDir =
                File(
                    Environment.getExternalStorageDirectory(),
                    "UU-UCE/Fieldbook"
                ).also {
                    it.mkdirs()
                }
        }

        resetVariables()
        content = mutableListOf()

        // makes sure the keyboard appears whenever we want to add text
        //isFocusable = true
        //update()

        rootView    = findViewById(android.R.id.content)
        layout      = findViewById(R.id.fieldbook_content_container)
        scrollView  = findViewById(R.id.fieldbook_scroll_view)

        title       = findViewById<EditText>(R.id.add_title).apply{
            setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    //Perform Code
                    hideKeyboard(this@FieldbookEditor)
                    return@OnKeyListener true
                }
                false
            })
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        // Fill layout
        if (fieldbookIndex >= 0) {
            viewModel.getContent(fieldbookIndex) {
                title.setText(it.title)
                content = PinContent(it.content,this).contentBlocks
                for (c in content)
                    content[content.indexOf(c)] =
                        c.makeEditable(latestBlockIndex++,layout,rootView,::onLongClick)
            }
        }

        findViewById<ConstraintLayout>(R.id.add_text_block).apply{
            setOnClickListener {
                currentBlockIndex = latestBlockIndex++
                addText()
            }
        }

        findViewById<ConstraintLayout>(R.id.add_image_block).apply{
            setOnClickListener {
                selectImage()
            }
        }

        findViewById<ConstraintLayout>(R.id.add_video_block).apply{
            setOnClickListener {
                selectVideo()
            }
        }

        var location : Location? = null

        try{
            location = LocationServices.lastKnownLocation
        }
        catch(e : Exception) {
            Logger.log(LogType.Event, "Fieldbook", "No last known location")
        }

        val savePinButton = findViewById<Button>(R.id.add_fieldbook_pin)
        savePinButton.setOnClickListener{
            if (fieldbookIndex >= 0)
                viewModel.update(title.text.toString(), buildJSONContent(content, this),fieldbookIndex)
            else
                saveFieldbookEntry(
                    title.text.toString(),
                    content,
                    getCurrentDateTime(DateTimeFormat.FIELDBOOK_ENTRY),
                    location
                )
            finish()
        }

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else{
            window.statusBarColor = Color.BLACK// set status background white
        }
    }

    private fun resetVariables () {
        currentPath = ""
        currentUri = Uri.EMPTY
        currentName = ""
    }

    private fun selectImage() {
        resetVariables()

        val options = arrayOf("Choose from gallery", "Take Photo", " Cancel")

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Upload an image")

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> { // Choose (picture) from gallery
                    getPermissions(
                        this,
                        listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PHOTOCAMERA_REQUEST
                    )
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || this.checkSelfPermission(
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startActivityForResult(
                            Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            ,
                            REQUEST_IMAGE_UPLOAD
                        )
                    }
                }
                1 -> { // Take photo
                    getPermissions(
                        this,
                        listOf(Manifest.permission.CAMERA),
                        PHOTOCAMERA_REQUEST
                    )
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || this.checkSelfPermission(
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startActivityForResult(
                            Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE
                            ).apply {
                                resolveActivity(packageManager)
                                putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    FileProvider.getUriForFile(
                                        this@FieldbookEditor,
                                        "com.uu-uce.fileprovider",
                                        imageLocation()
                                    ).also{
                                        currentUri = it
                                    }
                                )
                            },
                            REQUEST_IMAGE_CAPTURE
                        )
                    }
                }
                2 -> dialogInterface.dismiss()
            }
        }
        dialog.show()
    }

    private fun selectVideo() {
        resetVariables()

        val options = arrayOf("Choose from gallery", "Record Video", " Cancel")

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Upload a video")

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                /*0 -> { // Choose (video) from gallery
                    getPermissions(
                        fragmentActivity,
                        listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        CAMERA_REQUEST
                    )
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragmentActivity.checkSelfPermission(
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startActivityForResult(
                            Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            ),
                            REQUEST_VIDEO_UPLOAD
                        )
                    }
                }
                 */
                1 -> { // Record video
                    getPermissions(
                        this,
                        listOf(Manifest.permission.CAMERA),
                        VIDEOCAMERA_REQUEST
                    )
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || this.checkSelfPermission(
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startActivityForResult(
                            Intent(
                                MediaStore.ACTION_VIDEO_CAPTURE
                            ).apply {
                                resolveActivity(packageManager)
                                putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    FileProvider.getUriForFile(
                                        this@FieldbookEditor,
                                        "com.uu-uce.fileprovider",
                                        videoLocation()
                                    ).also {
                                        currentUri = it
                                    }
                                )
                                putExtra(
                                    MediaStore.EXTRA_VIDEO_QUALITY,
                                    1
                                )
                            },
                            REQUEST_VIDEO_CAPTURE
                        )
                    }
                }
                else -> dialogInterface.dismiss()
            }
        }
        dialog.show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            currentBlockIndex = latestBlockIndex++
            when (requestCode) {
                REQUEST_IMAGE_UPLOAD -> {
                    if (intent != null) {
                        val uri = intent.data
                        if (uri != null) {
                            val cr = contentResolver
                            cr.query(uri, null, null, null, null)?.use {
                                val nameIndex =
                                    it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                                it.moveToFirst()
                                currentName = File(it.getString(nameIndex)).nameWithoutExtension
                            }
                            currentUri = uri
                            addImage(
                                image = currentUri,
                                thumbnail = makeImageThumbnail(currentUri)
                            )
                        }
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    addImage(
                        image = currentUri,
                        thumbnail = makeImageThumbnail(currentUri)
                    )
                    addToGallery(currentPath)
                }
                REQUEST_VIDEO_UPLOAD -> {
                    if (intent != null) {
                        val uri = intent.data
                        if (uri != null) {
                            currentUri = uri
                            addVideo(
                                video = currentUri,
                                thumbnail = makeVideoThumbnail(currentUri)
                            )
                        }
                    }
                }
                REQUEST_VIDEO_CAPTURE -> {
                    addVideo(
                        video = currentUri,
                        thumbnail = makeVideoThumbnail(currentUri)
                    )
                    addToGallery(currentPath)
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this,"Failed to select media",Toast.LENGTH_SHORT).show()
        }
    }

    private fun addText() {
        title.clearFocus()
        EditTextBlock(
            this
        ).also {
            it.makeEditable(currentBlockIndex,layout,rootView,::onLongClick)
            content.add(it)
        }
        scrollToEnd()
    }

    private fun addImage(image: Uri, thumbnail: Uri) {
        title.clearFocus()
        ImageContentBlock(
            image,
            thumbnail,
            this
        ).also{
            if (editing) {
                content[currentBlockIndex].removeContent(layout)
                content[currentBlockIndex] = it
            } else {
                content.add(it)
            }
            it.makeEditable(currentBlockIndex,layout,rootView,::onLongClick)
        }
        editing = false
        scrollToEnd()
    }

    private fun addVideo(video: Uri, thumbnail: Uri) {
        title.clearFocus()
        VideoContentBlock(
            video,
            thumbnail,
            this
        ).also {
            if (editing) {
                content[currentBlockIndex].removeContent(layout)
                content[currentBlockIndex] = it
            } else {
                content.add(it)
            }
            it.makeEditable(currentBlockIndex,layout,rootView,::onLongClick)
        }
        editing = false
        scrollToEnd()
    }

    private fun onLongClick(cbi: ContentBlockInterface) : Boolean {
        val options = when (cbi) {
            is EditTextBlock -> arrayOf("Delete", "Cancel")
            else -> arrayOf("Delete", "Edit", "Cancel")
        }

        currentBlockIndex = content.indexOf(cbi)

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Change content")

        dialog.setItems(options) { dialogInterface, which ->
            when(options[which]) {
                "Delete"    -> {
                    cbi.removeContent(layout)
                    content.remove(cbi)
                    latestBlockIndex--
                }
                "Edit"      -> {
                    editing = true
                    when (cbi) {
                        is ImageContentBlock -> selectImage()
                        is VideoContentBlock -> selectVideo()
                    }
                }
                "Move up"   -> TODO()
                "Move down" -> TODO()
                "Cancel"    -> dialogInterface.dismiss()
            }
        }
        dialog.show()
        return true
    }

    private fun imageLocation(): File {
        val myDir: File = File(fieldbookDir,"Pictures").also{
            it.mkdirs()
        }
        val fileName = "IMG_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}_UCE_"

        return createTempFile(
            fileName,
            ".jpg",
            myDir
        ).apply {
            currentName = nameWithoutExtension
            currentPath = absolutePath
        }
    }

    private fun videoLocation() : File {
        val myDir: File = File(fieldbookDir,"Videos").also{
            it.mkdirs()
        }
        val fileName = "VID_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}_UCE_"
        return createTempFile(
            fileName,
            ".mp4",
            myDir
        ).apply {
            currentName = nameWithoutExtension
            currentPath = absolutePath
        }
    }

    private fun makeImageThumbnail(uri: Uri) : Uri {
        return try {
            saveThumbnail(
                contentResolver.openInputStream(uri).let {
                    BitmapFactory.decodeStream(it)
                }
            )
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }

    private fun makeVideoThumbnail(uri: Uri) : Uri {
        val retriever = MediaMetadataRetriever().apply {
            setDataSource(this@FieldbookEditor,uri)
        }

        return saveThumbnail (
            retriever.getFrameAtTime(1000,0)
        )
    }

    private fun saveThumbnail(bitmap: Bitmap) : Uri {
        val dir = File(this.
            getExternalFilesDir(null),
            "Fieldbook/Thumbnails"
        ).apply{
            mkdirs()
        }

        val file = File(dir,"$currentName.jpg")

        FileOutputStream(file).also{
            bitmap.compress(Bitmap.CompressFormat.JPEG,10,it)
        }.apply{
            flush()
            close()
        }

        return file.toUri()
    }

    private fun addToGallery(path: String) {
        try {
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                val f = File(path)
                mediaScanIntent.data = Uri.fromFile(f)
                this.sendBroadcast(mediaScanIntent)
            }
        } catch (e: Exception) {

        }
    }

    private fun saveFieldbookEntry(
        title: String,
        content: List<ContentBlockInterface>,
        currentDate: String,
        location: Location?
    ) {
        val utm = if(location == null){
            UTMCoordinate(0, 'N', 0.0, 0.0).toString()
        }
        else{
            degreeToUTM(Pair(location.latitude,location.longitude)).toString()
        }

        FieldbookEntry(
            title,
            utm,
            currentDate,
            buildJSONContent(content, this).also{ jsonString ->
                // added for debugging purposes
                val myDir: File = File(this.filesDir,"Content").also{
                    it.mkdirs()
                }
                val fileName = "TestContent.txt"
                val file = File(myDir,fileName)
                file.writeText(jsonString)
            }
        ).also{
            viewModel.insert(it)
        }
    }

    enum class DateTimeFormat{
        FILE_PATH,
        FIELDBOOK_ENTRY
    }

    private fun getCurrentDateTime(dtf: DateTimeFormat): String {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PHOTOCAMERA_REQUEST -> {
                if(grantResults[0] == 0) {
                    startActivityForResult(
                        Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE
                        ).apply {
                            resolveActivity(packageManager)
                            putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(
                                    this@FieldbookEditor,
                                    "com.uu-uce.fileprovider",
                                    imageLocation()
                                ).also{
                                    currentUri = it
                                }
                            )
                        },
                        REQUEST_IMAGE_CAPTURE
                    )
                }
            }
            VIDEOCAMERA_REQUEST -> {
                if(grantResults[0] == 0) {
                    startActivityForResult(
                        Intent(
                            MediaStore.ACTION_VIDEO_CAPTURE
                        ).apply {
                            resolveActivity(packageManager)
                            putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(
                                    this@FieldbookEditor,
                                    "com.uu-uce.fileprovider",
                                    videoLocation()
                                ).also {
                                    currentUri = it
                                }
                            )
                            putExtra(
                                MediaStore.EXTRA_VIDEO_QUALITY,
                                1
                            )
                        },
                        REQUEST_VIDEO_CAPTURE
                    )
                }
            }
            EXTERNAL_FILES_REQUEST -> {
                if(grantResults[0] == 0){
                    startActivityForResult(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        ,
                        REQUEST_IMAGE_UPLOAD
                    )
                }
            }
        }
    }

    private fun scrollToEnd() {
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
