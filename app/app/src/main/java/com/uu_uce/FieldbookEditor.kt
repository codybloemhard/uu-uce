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
        const val REQUEST_IMAGE_UPLOAD = 0
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_VIDEO_UPLOAD = 2
        const val REQUEST_VIDEO_CAPTURE = 3

        var currentUri: Uri = Uri.EMPTY
        var currentPath: String = ""

        private const val THUMBNAIL_DIRECTORY = "Fieldbook/Thumbnails"
    }

    private lateinit var viewModel      : FieldbookViewModel

    private lateinit var mediaServices  : MediaServices

    private lateinit var content: MutableList<ContentBlock>

    private lateinit var rootView       : View
    private lateinit var scrollView     : ScrollView
    private lateinit var layout         : LinearLayout
    private lateinit var title          : EditText

    private var currentName         = ""
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

        mediaServices = MediaServices(this)

        viewModel = this.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        }

        resetVariables()

        // Initiate the view and set OnClickListeners
        createTopbar(this, getString(R.string.editor_topbar_title)) {
            if (fieldbookIndex >= 0) {
                finish()
            } else {
                deleteTemps()
                finish()
            }
        }

        rootView = findViewById(android.R.id.content)
        layout = findViewById(R.id.fieldbook_content_container)
        scrollView = findViewById(R.id.fieldbook_scroll_view)

        title = findViewById<EditText>(R.id.add_title).apply {
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

        findViewById<Button>(R.id.add_fieldbook_pin).apply {
            setOnClickListener {
                if (fieldbookIndex >= 0)
                    viewModel.update(
                        title.text.toString(),
                        buildJSONContent(content),
                        fieldbookIndex
                    )
                else
                    insertIntoFieldbook(
                        title.text.toString(),
                        content,
                        getCurrentDateTime(DateTimeFormat.FIELDBOOK_ENTRY),
                        location()
                    )
                finish()
            }
        }

        // Fill layout
        content = mutableListOf()

        if (fieldbookIndex >= 0) {
            viewModel.getContent(fieldbookIndex) {
                title.setText(it.title)
                content = PinContent(it.content, this, true).contentBlocks
                for (c in content)
                    content[content.indexOf(c)] =
                        c.makeEditable(latestBlockIndex++, layout, rootView, ::changeBlock)
            }
        }
    }

    /**
     * Gives the last know location, or none
     *
     * @return the last known location (or null whenever it can't find any)
     */
    private fun location(): Location? {
        var location: Location? = null

        try {
            location = LocationServices.lastKnownLocation
        } catch (e: Exception) {
            Logger.log(LogType.Event, "Fieldbook", "No last known location")
        }
        return location
    }

    override fun onBackPressed() {
        if (fieldbookIndex < 0) {
            deleteTemps()
        }
        super.onBackPressed()
    }

    /**
     * Deletes already created blocks whenever the new fieldbook entry isn't saved
     */
    private fun deleteTemps() {
        for (block in content) {
            block.removeContent(layout)
        }
    }

    /**
     * Resets global variables when a new fieldbook entry is generated
     */
    private fun resetVariables() {
        currentUri = Uri.EMPTY
        currentName = ""
        currentPath = ""
    }

    /**
     * Gives the user the choice to take a photo, of upload an image from the gallery
     * Checks for the required permissions and asks to give permission when necessary
     * Starts the intents for selecting of taking pictures
     */
    private fun selectImage() {
        resetVariables()

        // Building the dialog
        val options = arrayOf(
            getString(R.string.editor_imageselection_gallery),
            getString(R.string.editor_imageselection_camera),
            getString(R.string.cancel_button)
        )

        val dialog = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        dialog.setTitle(getString(R.string.editor_imageselection_popup_title))

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> {
                    // Check for permissions
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        (
                                this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                                        this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                                )
                    ) {
                        // Start image selection intent
                        imageSelectionIntent()
                    } else {
                        // Ask for permissions when they aren't present
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
                1 -> {
                    // Ask for permissions when they aren't present
                    getPermissions(
                        this,
                        listOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        PHOTOSTORAGE_REQUEST
                    )
                    // Check for permissions
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        (
                                this.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                                        this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                                        this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                                )
                    ) {
                        // Start image capture intent
                        imageCaptureIntent()
                    }
                }
                // Close the dialog
                else -> dialogInterface.dismiss()
            }
        }
        dialog.show()
    }

    /**
     * Gives the user the choice to record a video, of upload a video from the gallery
     * Checks for the required permissions and asks to give permission when necessary
     * Starts the intents for selecting of taking videos
     */
    private fun selectVideo() {
        resetVariables()

        // Building the dialog
        val options = arrayOf(
            getString(R.string.editor_videoselection_gallery),
            getString(R.string.editor_videoselection_camera),
            getString(R.string.cancel_button)
        )

        val dialog = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        dialog.setTitle(getString(R.string.editor_videoselection_popup_title))

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> {
                    // Check for permissions
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        (
                                this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                                        this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                                )
                    ) {
                        // Start video selection intent
                        videoSelectionIntent()
                    } else {
                        // Ask for permissions when they aren't present
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

                1 -> {
                    // Ask for permissions when they aren't present
                    getPermissions(
                        this,
                        listOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        VIDEOSTORAGE_REQUEST
                    )
                    // Check for permissions
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        (
                                this.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                                        this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                                        this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                                )
                    ) {
                        // Start video capture intent
                        videoCaptureIntent()
                    }
                }
                // Close the dialog
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
                            thumbnail   = mediaServices.makeImageThumbnail(
                                uri,
                                THUMBNAIL_DIRECTORY
                            )
                        )
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    addImage(
                        image       = currentUri,
                        thumbnail   = mediaServices.makeImageThumbnail(
                            currentUri,
                            THUMBNAIL_DIRECTORY
                        )
                    )

                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        mediaServices.addMediaToGallery(currentPath)
                    }
                }
                REQUEST_VIDEO_UPLOAD -> {
                    if (intent != null) {
                        val uri = intent.data

                        currentUri  = mediaServices.getVideoPathFromURI(uri)
                        currentName = mediaServices.getVideoFileNameFromURI(currentUri)

                        addVideo(
                            video       = currentUri,
                            thumbnail   = mediaServices.makeVideoThumbnail(
                                uri,
                                THUMBNAIL_DIRECTORY
                            )
                        )
                    }
                }
                REQUEST_VIDEO_CAPTURE -> {
                    addVideo(
                        video       = currentUri,
                        thumbnail   = mediaServices.makeVideoThumbnail(
                            currentUri,
                            THUMBNAIL_DIRECTORY
                        )
                    )
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        mediaServices.addMediaToGallery(currentPath)
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(
                this,
                getString(R.string.fieldbook_mediaselection_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Adds an exitable textblock to the view and adds it to the list of ContentBlocks
     */
    private fun addText() {
        title.clearFocus()
        EditTextContentBlock(
            this
        ).also {
            it.makeEditable(currentBlockIndex, layout, rootView, ::changeBlock)
            content.add(it)
        }
        scrollToView(currentBlockIndex)
    }

    /**
     * Makes a new ImageContentBlock, which adds an ImageView to the view,
     * showing the image the provided Uri refers to
     *
     * @param[image] the Uri that refers to the image we want to add to this FieldbookEntry
     * @param[thumbnail] the thumbnail of the aforementioned image
     */
    private fun addImage(image: Uri, thumbnail: Uri) {
        title.clearFocus()
        ImageContentBlock(
            image,
            thumbnail,
            this
        ).also {
            if (editing) {
                content[currentBlockIndex].removeContent(layout)
                content[currentBlockIndex] = it
            } else {
                content.add(it)
            }
            it.makeEditable(currentBlockIndex, layout, rootView, ::changeBlock)
        }
        editing = false
        scrollToView(currentBlockIndex)
    }

    /**
     * Makes a new ImageContentBlock, which adds an VideoView to the view,
     * showing the video the provided Uri refers to
     *
     * @param[video] the Uri that refers to the video we want to add to this FieldbookEntry
     * @param[thumbnail] the thumbnail of the aforementioned video
     */
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
            it.makeEditable(currentBlockIndex, layout, rootView, ::changeBlock)
        }
        editing = false
        scrollToView(currentBlockIndex)
    }

    /**
     * Handles what happens when the user clicks one ContentBlock
     * Gives the user the option to edit, delete or move a block
     *
     * @param[cbi] the ContentBlock that has been long clicked
     */
    private fun changeBlock(cbi: ContentBlock): Boolean {
        currentBlockIndex = content.indexOf(cbi)

        // Set options for this block
        val list = mutableListOf<String>().apply {
            if (cbi !is EditTextContentBlock)
                add(getString(R.string.editor_edit_block))
            add(getString(R.string.editor_delete_block))
            if (currentBlockIndex > 0)
                add(getString(R.string.editor_moveup))
            if (currentBlockIndex < latestBlockIndex - 1)
                add(getString(R.string.editor_movedown))
            add(getString(R.string.editor_cancel_edit))
        }

        // Build the dialog
        val options = list.toTypedArray()

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.editor_edit_popup_title))
            .setItems(options) { dialogInterface, which ->
                when(options[which]) {
                    getString(R.string.editor_delete_block) -> deleteBlock(cbi)
                    getString(R.string.editor_edit_block) -> editBlock(cbi)
                    getString(R.string.editor_moveup) -> moveBlockUp(cbi)
                    getString(R.string.editor_movedown) -> moveBlockDown(cbi)
                    getString(R.string.editor_cancel_edit) -> dialogInterface.dismiss()
                }
            }.show()

        return true
    }

    /**
     * Deletes the long clicked block from the view and from the list of ContentBlocks
     *
     * @param[cbi] the ContentBlock that has been long clicked
     */
    private fun deleteBlock(cbi: ContentBlock) {
        cbi.removeContent(layout)
        content.remove(cbi)
        latestBlockIndex--
        if (currentBlockIndex > 0)
            scrollToView(currentBlockIndex - 1)
    }

    /**
     * Gives the user the option to edit the long clicked ContentBlock
     * Only works for Image- and VideoBlocks, as TextBlocks are already editable in the FieldbookEditor
     * On long clicking, the user can replace the block with a newly captured image/video or an image/video from the gallery
     *
     * @param[cbi] the ContentBlock that has been long clicked
     */
    private fun editBlock(cbi: ContentBlock) {
        editing = true
        when (cbi) {
            is ImageContentBlock -> selectImage()
            is VideoContentBlock -> selectVideo()
        }
    }

    /**
     * Moves the long clicked block one position up, switching with the block above
     *
     * @param[cbi] the ContentBlock that has been long clicked
     */
    private fun moveBlockUp(cbi: ContentBlock) {
        val newIndex = currentBlockIndex - 1
        moveBlock(newIndex, cbi)
    }

    /**
     * Moves the long clicked block one position down, switching with the block below
     *
     * @param[cbi] the ContentBlock that has been long clicked
     */
    private fun moveBlockDown(cbi: ContentBlock) {
        val newIndex = currentBlockIndex + 1
        moveBlock(newIndex, cbi)
    }

    /**
     * Moves the block one position
     *
     * @param[newIndex] the new index we want to move the block to
     * @param[cbi] the ContentBlock we want to move
     */
    private fun moveBlock(newIndex: Int, cbi: ContentBlock) {
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

    /**
     * Makes the collected data into a FieldbookEntry and inserts in the database
     *
     * @param[title] the title the user selected
     * @param[content] all ContentBlocks the user added
     * @param[currentDate] the current Date and Time
     * @param[location] the last know location
     */
    private fun insertIntoFieldbook(
        title: String,
        content: List<ContentBlock>,
        currentDate: String,
        location: Location?
    ) {
        val utm = if (location == null) {
            UTMCoordinate(0, 'N', 0.0f, 0.0f).toString()
        } else {
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
                if (grantResults.all { x -> x == 0 }) {
                    videoSelectionIntent()
                }
            }
        }
    }

    /**
     * Starts the image capture intent
     * Indicates the location to store the image to
     */
    private fun imageCaptureIntent() {
        currentUri = mediaServices.fieldbookImageLocation()

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

    /**
     * Starts the image selection intent
     * Opens a content picker, that shows only images
     */
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

    /**
     * Starts the video capture intent
     * Indicates the location to store the video to
     */
    private fun videoCaptureIntent() {
        currentUri = mediaServices.fieldbookVideoLocation()

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

    /**
     * Starts the video selection intent
     * Opens a content picker, that shows only videos
     */
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

    /**
     * Scrolls the scrollview to the requested index
     *
     * @param[index] the index of the ContentBlock that has to be centered
     */
    private fun scrollToView(index: Int) {
        scrollView.post {
            scrollView.smoothScrollTo(0, layout.getChildAt(index).top)
        }
    }

    /**
     * Hides the keyboard from the screen
     *
     * @param[activity] the associated activity
     */
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


