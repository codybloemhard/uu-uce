package com.uu_uce

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.fieldbook.FieldbookViewModel
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.*
import com.uu_uce.services.*
import com.uu_uce.ui.createTopbar

class FieldbookEditor: AppCompatActivity() {

    companion object {
        const val REQUEST_IMAGE_UPLOAD  = 0
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_VIDEO_UPLOAD  = 2
        const val REQUEST_VIDEO_CAPTURE = 3

        var currentUri: Uri = Uri.EMPTY
    }

    private lateinit var viewModel      : FieldbookViewModel

    private lateinit var mediaServices  : MediaServices

    private lateinit var content        :  MutableList<ContentBlockInterface>

    private lateinit var rootView       : View
    private lateinit var scrollView     : ScrollView
    private lateinit var layout         : LinearLayout
    private lateinit var title          : EditText

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

        createTopbar(this,getString(R.string.editor_topbar_title)){
            if(fieldbookIndex >= 0){
                finish()
            }
            else{
                deleteTemps()
                finish()
            }
        }

        mediaServices = MediaServices(this)

        viewModel = this.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        }

        resetVariables()
        content = mutableListOf()


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

    override fun onBackPressed() {
        if(fieldbookIndex < 0){
            deleteTemps()
        }
        super.onBackPressed()
    }

    private fun deleteTemps(){
        for(block in content){
            block.removeContent(layout)
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
                            imageSelectionIntent()
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
                        imageCaptureIntent()
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
                        videoSelectionIntent()
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
                        videoCaptureIntent()
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

                        currentUri  = mediaServices.getImagePathFromURI(uri)
                        currentName = mediaServices.getImageFileNameFromURI(currentUri)

                        addImage(
                            image       = currentUri,
                            thumbnail   = mediaServices.makeImageThumbnail(currentUri)
                        )
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    addImage(
                        image       = currentUri,
                        thumbnail   = mediaServices.makeImageThumbnail(currentUri)
                    )

                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        mediaServices.addImageToGallery(currentUri.path!!)
                    }
                }
                REQUEST_VIDEO_UPLOAD -> {
                    if (intent != null) {
                        val uri = intent.data

                        currentUri  = mediaServices.getVideoPathFromURI(uri)
                        currentName = mediaServices.getVideoFileNameFromURI(currentUri)

                        addVideo(
                            video       = currentUri,
                            thumbnail   = mediaServices.makeVideoThumbnail(currentUri)
                        )
                    }
                }
                REQUEST_VIDEO_CAPTURE -> {
                    addVideo(
                        video       = currentUri,
                        thumbnail   = mediaServices.makeVideoThumbnail(currentUri)
                    )
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        mediaServices.addVideoToGallery(currentUri.path!!)
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, getString(R.string.fieldbook_mediaselection_failed), Toast.LENGTH_SHORT).show()
        }
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
                getString(R.string.editor_delete_block) -> deleteBlock(cbi)
                getString(R.string.editor_edit_block)   -> editBlock(cbi)
                getString(R.string.editor_moveup)       -> moveBlockUp(cbi)
                getString(R.string.editor_movedown)     -> moveBlockDown(cbi)
                getString(R.string.editor_cancel_edit)  -> dialogInterface.dismiss()
            }
        }.show()

        return true
    }

    private fun deleteBlock(cbi: ContentBlockInterface) {
        cbi.removeContent(layout)
        content.remove(cbi)
        latestBlockIndex--
        if (currentBlockIndex > 0)
            scrollToView(currentBlockIndex - 1)
    }

    private fun editBlock(cbi: ContentBlockInterface) {
        editing = true
        when (cbi) {
            is ImageContentBlock -> selectImage()
            is VideoContentBlock -> selectVideo()
        }
    }

    private fun moveBlockUp(cbi: ContentBlockInterface) {
        val newIndex = currentBlockIndex - 1
        moveView(newIndex, cbi)
    }

    private fun moveBlockDown(cbi: ContentBlockInterface) {
        val newIndex = currentBlockIndex + 1
        moveView(newIndex, cbi)
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

    private fun saveFieldbookEntry(
        title: String,
        content: List<ContentBlockInterface>,
        currentDate: String,
        location: Location?
    ) {
        val utm = if(location == null){
            UTMCoordinate(0, 'N', 0.0f, 0.0f).toString()
        }
        else{
            degreeToUTM(Pair(location.latitude.toFloat(),location.longitude.toFloat())).toString()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PHOTOSTORAGE_REQUEST -> {
                if(grantResults.all { x -> x == 0 }) {
                    imageCaptureIntent()
                }
            }
            VIDEOSTORAGE_REQUEST -> {
                if(grantResults.all { x -> x == 0 }) {
                    videoCaptureIntent()
                }
            }

            EXTERNAL_PHOTO_REQUEST -> {
                if(grantResults.all { x -> x == 0 }){
                    imageSelectionIntent()
                }
            }

            EXTERNAL_VIDEO_REQUEST -> {
                if(grantResults.all { x -> x == 0 }){
                    videoSelectionIntent()
                }
            }
        }
    }

    private fun imageCaptureIntent() {
        currentUri = mediaServices.imageLocation()

        startActivityForResult(
            Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
            ).apply {
                resolveActivity(packageManager)
                putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    currentUri
                )
            },
            REQUEST_IMAGE_CAPTURE
        )
    }

    private fun imageSelectionIntent() {
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

    private fun videoCaptureIntent() {
        currentUri = mediaServices.videoLocation()

        startActivityForResult(
            Intent(
                MediaStore.ACTION_VIDEO_CAPTURE
            ).apply {
                resolveActivity(packageManager)
                putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    currentUri
                )
                putExtra(
                    MediaStore.EXTRA_VIDEO_QUALITY,
                    1
                )
            },
            REQUEST_VIDEO_CAPTURE
        )
    }


    private fun videoSelectionIntent() {
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
