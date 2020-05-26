package com.uu_uce

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
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
import androidx.preference.PreferenceManager
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
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val darkMode = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        // Set desired theme
        if(darkMode) setTheme(R.style.DarkTheme)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !darkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else if(!darkMode){
            window.statusBarColor = Color.BLACK// set status background white
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fieldbook_editor)

        val bundle = intent.extras

        if (bundle != null)
            fieldbookIndex = bundle.getInt("fieldbook_index")

        createTopbar(this,getString(R.string.pineditor_topbar_title))

        viewModel = this.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
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
                content = PinContent(it.content,this, true).contentBlocks
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
                viewModel.update(title.text.toString(), buildJSONContent(content),fieldbookIndex)
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

        val options = arrayOf(
            getString(R.string.editor_imageselection_gallery),
            getString(R.string.editor_imageselection_camera),
            getString(R.string.cancel_button))

        val dialog = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        dialog.setTitle(getString(R.string.editor_imageselection_popup_title))

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> { // Choose (picture) from gallery
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                            (
                                    this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                                    this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            )
                        ) {
                            val intent = Intent(
                                Intent.ACTION_GET_CONTENT,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            intent.type = "image/*"
                            startActivityForResult(
                                intent,
                                REQUEST_IMAGE_UPLOAD
                            )
                        }
                        else{
                            getPermissions(
                                this,
                                listOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
                                EXTERNAL_PHOTO_REQUEST
                            )
                        }
                    }
                1 -> { // Take photo
                    getPermissions(
                        this,
                        listOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        PHOTOSTORAGE_REQUEST
                    )
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        (
                            this.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                            this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        )) {
                        fieldbookDir = File(
                            Environment.getExternalStorageDirectory(),
                            "UU-UCE/Fieldbook").also { it.mkdirs() }

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

        val options = arrayOf(
            getString(R.string.editor_videoselection_gallery),
            getString(R.string.editor_videoselection_camera),
            getString(R.string.cancel_button))

        val dialog = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        dialog.setTitle(getString(R.string.editor_videoselection_popup_title))

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> { // Choose (video) from gallery
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        (
                                this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                                this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        )
                    ) {
                        val intent = Intent(
                            Intent.ACTION_GET_CONTENT,
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        )
                        intent.type = "video/*"
                        startActivityForResult(
                            intent,
                            REQUEST_VIDEO_UPLOAD
                        )
                    }
                    else{
                        getPermissions(
                            this,
                            listOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ),
                            EXTERNAL_VIDEO_REQUEST
                        )
                    }
                }

                1 -> { // Record video
                    getPermissions(
                        this,
                        listOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        VIDEOSTORAGE_REQUEST
                    )
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        (this.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                            this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))
                    {
                        fieldbookDir = File(
                            Environment.getExternalStorageDirectory(),
                            "UU-UCE/Fieldbook").also { it.mkdirs() }

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
            if (!editing)
                currentBlockIndex = latestBlockIndex++
            when (requestCode) {
                REQUEST_IMAGE_UPLOAD -> {
                    if (intent != null) {
                        val uri =  intent.data
                        val path = getImagePathFromURI(this, uri)
                        val cr = contentResolver
                        cr.query(path, null, null, null, null)?.use {
                            val nameIndex =
                                it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                            it.moveToFirst()
                            currentName = File(it.getString(nameIndex)).nameWithoutExtension
                        }
                        currentUri = path
                        addImage(
                            image = currentUri,
                            thumbnail = makeImageThumbnail(uri)
                        )
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
                        val path = getVideoPathFromURI(this, uri)
                        val cr = contentResolver
                        cr.query(path, null, null, null, null)?.use {
                            val nameIndex =
                                it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                            it.moveToFirst()
                            currentName = File(it.getString(nameIndex)).nameWithoutExtension
                        }
                        currentUri = path
                        addVideo(
                            currentUri,
                            makeVideoThumbnail(uri)
                        )
                    }
                }
                REQUEST_VIDEO_CAPTURE -> {
                    addVideo(
                        currentUri,
                        makeVideoThumbnail(currentUri)
                    )
                    addToGallery(currentPath)
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this,"Failed to select media",Toast.LENGTH_SHORT).show()
        }
    }

    private fun getImagePathFromURI(context: Context, uri: Uri?): Uri {
        var filePath = ""
        val wholeID: String = DocumentsContract.getDocumentId(uri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":").toTypedArray()[1]
        val column = arrayOf(MediaStore.Images.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"
        val cursor: Cursor? = context.contentResolver.query(
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

    private fun getVideoPathFromURI(context: Context, uri: Uri?): Uri {
        var filePath = ""
        val wholeID: String = DocumentsContract.getDocumentId(uri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":").toTypedArray()[1]
        val column = arrayOf(MediaStore.Video.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Video.Media._ID + "=?"
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
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

    private fun addText() {
        title.clearFocus()
        TextBlock(
            this
        ).also {
            it.makeEditable(currentBlockIndex,layout,rootView,::onLongClick)
            content.add(it)
        }
        scrollToView(currentBlockIndex)
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
        scrollToView(currentBlockIndex)
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
        scrollToView(currentBlockIndex)
    }

    private fun onLongClick(cbi: ContentBlockInterface) : Boolean {
        currentBlockIndex = content.indexOf(cbi)

        // Set options for this block
        val list = mutableListOf<String>().apply {
            if (cbi !is TextBlock)
                add(getString(R.string.editor_edit_block))
            add(getString(R.string.editor_delete_block))
            if (currentBlockIndex > 0)
                add(getString(R.string.editor_moveup))
            if (currentBlockIndex < latestBlockIndex - 1)
                add(getString(R.string.editor_movedown))
            add(getString(R.string.editor_cancel_edit))
        }

        val options = list.toTypedArray()

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.editor_edit_popup_title))
            .setItems(options) { dialogInterface, which ->
            when(options[which]) {
                getString(R.string.editor_delete_block) -> {
                    cbi.removeContent(layout)
                    content.remove(cbi)
                    latestBlockIndex--
                    if (currentBlockIndex > 0)
                        scrollToView(currentBlockIndex-1)
                }
                getString(R.string.editor_edit_block)   -> {
                    editing = true
                    when (cbi) {
                        is ImageContentBlock -> selectImage()
                        is VideoContentBlock -> selectVideo()
                    }
                }
                getString(R.string.editor_moveup)       -> {
                    val newIndex = currentBlockIndex - 1
                    moveView(newIndex, cbi)
                }
                getString(R.string.editor_movedown)     -> {
                    val newIndex = currentBlockIndex + 1
                    moveView(newIndex, cbi)
                }
                getString(R.string.editor_cancel_edit)  -> dialogInterface.dismiss()
            }
        }.show()
        return true
    }

    private fun moveView (newIndex: Int, cbi: ContentBlockInterface) {
        layout.apply {
            removeViewAt(currentBlockIndex)
            addView(cbi.content, newIndex)
        }
        content.apply {
            removeAt(currentBlockIndex)
            add(newIndex, cbi)
        }
        scrollToView(newIndex)
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

    private fun makeImageThumbnail(uri: Uri?) : Uri {
        return try {
            saveThumbnail(
                contentResolver.openInputStream(uri!!).let {
                    BitmapFactory.decodeStream(it)
                }
            )
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }

    private fun makeVideoThumbnail(uri: Uri?) : Uri {
        val retriever = MediaMetadataRetriever().apply {
            try{
                setDataSource(this@FieldbookEditor,uri)
            }
            catch(e : java.lang.Exception){
                return Uri.EMPTY
            }
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

        val file = File(dir,"thumbnail_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}.jpg")

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
            buildJSONContent(content)
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
            PHOTOSTORAGE_REQUEST -> {
                if(grantResults.all { x -> x == 0 }) {
                    fieldbookDir = File(
                        Environment.getExternalStorageDirectory(),
                        "UU-UCE/Fieldbook").also { it.mkdirs() }

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
            VIDEOSTORAGE_REQUEST -> {
                if(grantResults.all { x -> x == 0 }) {
                    fieldbookDir = File(
                        Environment.getExternalStorageDirectory(),
                        "UU-UCE/Fieldbook").also { it.mkdirs() }

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

            EXTERNAL_PHOTO_REQUEST -> {
                if(grantResults.all { x -> x == 0 }){
                    val intent = Intent(
                        Intent.ACTION_GET_CONTENT,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    intent.type = "image/*"
                    startActivityForResult(
                        intent,
                        REQUEST_IMAGE_UPLOAD
                    )
                }
            }

            EXTERNAL_VIDEO_REQUEST -> {
                if(grantResults.all { x -> x == 0 }){
                    val intent = Intent(
                        Intent.ACTION_GET_CONTENT,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    intent.type = "video/*"
                    startActivityForResult(
                        intent,
                        REQUEST_VIDEO_UPLOAD
                    )
                }
            }
        }
    }

    private fun scrollToView(index: Int) {
        scrollView.post {
            scrollView.smoothScrollTo(0,layout.getChildAt(index).top)
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
