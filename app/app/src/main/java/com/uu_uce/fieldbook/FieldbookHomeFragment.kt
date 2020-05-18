package com.uu_uce.fieldbook

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
import android.view.*
import android.view.View.FOCUS_DOWN
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.R
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.*
import com.uu_uce.services.*
import com.uu_uce.ui.createTopbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FieldbookHomeFragment(view: View) : Fragment() {

    companion object {
        fun newInstance(view: View) =
            FieldbookHomeFragment(view)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        //TODO: add checks on availability of storage et cetera
        lateinit var fieldbookDir : File

        const val REQUEST_IMAGE_UPLOAD  = 0
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_VIDEO_UPLOAD  = 2
        const val REQUEST_VIDEO_CAPTURE = 3

        var currentUri: Uri = Uri.EMPTY
    }

    private lateinit var viewModel          : FieldbookViewModel
    private lateinit var viewAdapter        : FieldbookAdapter
    private lateinit var fragmentActivity   : FragmentActivity

    private lateinit var content: MutableList<ContentBlockInterface>

    private val parentView              = view
    private lateinit var customView     : View
    private lateinit var scrollView     : ScrollView
    private lateinit var layout         : LinearLayout
    private lateinit var title          : EditText

    private var currentName = ""
    private var currentPath = ""

    private var latestBlockIndex = 0
    private var blockID = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentActivity = requireActivity()

        viewModel = fragmentActivity.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        }
    }

    private fun resetVariables () {
        currentPath = ""
        currentUri = Uri.EMPTY
        currentName = ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fieldbook_fragment_home, container, false).also { view ->
            val recyclerView = view.findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
            val addButton = view.findViewById<FloatingActionButton>(R.id.fieldbook_fab)

            viewAdapter = FieldbookAdapter(fragmentActivity, viewModel, parentView)

            viewModel.allFieldbookEntries.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                viewAdapter.setFieldbook(it)
            })

            recyclerView.layoutManager = LinearLayoutManager(fragmentActivity)
            recyclerView.adapter = viewAdapter

            val searchBar = view.findViewById<EditText>(R.id.fieldbook_searchbar)

            searchBar.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    //Perform Code
                    val search = searchBar.text.toString()
                    searchPins(search)
                    return@OnKeyListener true
                }
                false
            })

            addButton.setOnClickListener {
                openFieldbookAdderPopup()
            }
        }
    }

    private fun searchPins(search : String){
        viewModel.search(search){ fieldbook ->
            fieldbook?.let {
                viewAdapter.setFieldbook(fieldbook)
            }
            hideKeyboard(fragmentActivity)
        }
    }


    // Opens a popup, in which we can make new entries to the fieldbook
    private fun openFieldbookAdderPopup() {

        /**
         * TODO: used function is deprecated from API 29 and onwards. Can still be used, because android:requestLegacyExternalStorage="true" in the manifest
         * Eventually switch to the MediaStore API. Doesn't need READ/WRITE permissions anymore -> only to be imported for API 28 and lower
         */
        getPermissions(fragmentActivity, listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE), PHOTOCAMERA_REQUEST)

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            fragmentActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                    fragmentActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                ) {
            fieldbookDir =
                File(
                    Environment.getExternalStorageDirectory(),
                    "UU-UCE/Fieldbook"
                ).also {
                    it.mkdirs()
                }
        }

        blockID = 0
        resetVariables()
        content = mutableListOf()

        customView = layoutInflater.inflate(R.layout.fieldbook_addpin_popup, requireView() as ViewGroup, false)
        val popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            showAtLocation(fragmentActivity.findViewById(R.id.fieldbook_layout), Gravity.CENTER, 0, 0)

            // makes sure the keyboard appears whenever we want to add text
            isFocusable = true
            update()
        }

        customView.findViewById<TextView>(R.id.toolbar_title).text = getString(R.string.fieldbook_add_pin_title)

        customView.findViewById<ImageButton>(R.id.toolbar_back_button).setOnClickListener{
            popupWindow.dismiss()
        }

        layout      = customView.findViewById(R.id.fieldbook_content_container)
        scrollView  = customView.findViewById(R.id.fieldbook_scroll_view)

        title       = customView.findViewById<EditText>(R.id.add_title).apply{
            setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    //Perform Code
                    hideKeyboard(fragmentActivity, customView)
                    return@OnKeyListener true
                }
                false
            })
            inputType = TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        customView.findViewById<ConstraintLayout>(R.id.add_text_block).apply{
            setOnClickListener {
                addText()
            }
        }

        customView.findViewById<ConstraintLayout>(R.id.add_image_block).apply{
            setOnClickListener {
                selectImage()
            }
        }

        customView.findViewById<ConstraintLayout>(R.id.add_video_block).apply{
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

        val savePinButton = customView.findViewById<Button>(R.id.add_fieldbook_pin)
        savePinButton.setOnClickListener{
            saveFieldbookEntry(
                title.text.toString(),
                content,
                getCurrentDateTime(DateTimeFormat.FIELDBOOK_ENTRY),
                location
            )
            popupWindow.dismiss()
        }
    }

    private fun selectImage() {
        resetVariables()

        val options = arrayOf("Choose from gallery", "Take Photo", " Cancel")

        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Upload an image")

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> { // Choose (picture) from gallery
                    getPermissions(fragmentActivity, listOf(Manifest.permission.READ_EXTERNAL_STORAGE), PHOTOCAMERA_REQUEST)
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragmentActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
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
                    getPermissions(fragmentActivity, listOf(Manifest.permission.CAMERA), PHOTOCAMERA_REQUEST)
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragmentActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startActivityForResult(
                            Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE
                            ).apply {
                                resolveActivity(requireContext().packageManager)
                                putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    FileProvider.getUriForFile(
                                        requireContext(),
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

        val dialog = AlertDialog.Builder(requireContext())
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
                        fragmentActivity,
                        listOf(Manifest.permission.CAMERA),
                        VIDEOCAMERA_REQUEST
                    )
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragmentActivity.checkSelfPermission(
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startActivityForResult(
                            Intent(
                                MediaStore.ACTION_VIDEO_CAPTURE
                            ).apply {
                                resolveActivity(requireContext().packageManager)
                                putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    FileProvider.getUriForFile(
                                        requireContext(),
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
            when (requestCode) {
                REQUEST_IMAGE_UPLOAD -> {
                    if (intent != null) {
                        val uri = intent.data
                        if (uri != null) {
                            val cr = requireContext().contentResolver
                            cr.query(uri, null, null, null, null)?.use {
                                val nameIndex =
                                    it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                                it.moveToFirst()
                                currentName = File(it.getString(nameIndex)).nameWithoutExtension
                            }
                            currentUri = uri
                            addImage(currentUri)
                        }
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    addImage(currentUri)
                    addToGallery(currentPath)
                }
                REQUEST_VIDEO_UPLOAD -> {
                    if (intent != null) {
                        val uri = intent.data
                        if (uri != null) {
                            currentUri = uri
                            addVideo(makeVideoThumbnail(currentUri))
                        }
                    }
                }
                REQUEST_VIDEO_CAPTURE -> {
                    addVideo(currentUri)
                    addToGallery(currentPath)
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(requireContext(),"Failed to select media",Toast.LENGTH_SHORT).show()
        }
    }

    private fun addText() {
        latestBlockIndex++
        title.clearFocus()
        EditTextBlock(
            fragmentActivity
        ).also {
            it.generateContent(blockID++,layout,customView,null)
            content.add(it)
        }
        scrollToEnd()

        //hideKeyboard(fragmentActivity, it)

    }

    private fun addImage(image: Uri) {
        latestBlockIndex++
        title.clearFocus()
        ImageContentBlock(
            image,
            makeImageThumbnail(image),
            fragmentActivity
        ).also{
            it.generateContent(blockID++, layout, customView, null)
            content.add(it)
        }
        scrollToEnd()
    }

    private fun addVideo(video: Uri) {
        latestBlockIndex++
        title.clearFocus()
        VideoContentBlock(
            video,
            makeVideoThumbnail(video),
            fragmentActivity
        ).also {
            it.generateContent(blockID++, layout, customView, null)
            content.add(it)
        }
        scrollToEnd()
    }

    private fun scrollToEnd() {
        scrollView.post {
            scrollView.fullScroll(FOCUS_DOWN)
        }
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
                requireContext().contentResolver.openInputStream(uri).let {
                    BitmapFactory.decodeStream(it)
                }
            )
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }

    private fun makeVideoThumbnail(uri: Uri) : Uri {
        val retriever = MediaMetadataRetriever().apply {
            setDataSource(requireContext(),uri)
        }

        return saveThumbnail (
            retriever.getFrameAtTime(1000,0)
        )
    }

    private fun saveThumbnail(bitmap: Bitmap) : Uri {
        val dir = File(requireContext().
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
                requireContext().sendBroadcast(mediaScanIntent)
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
            buildJSONContent(content, requireContext()).also{ jsonString ->
                // added for debugging purposes
                val myDir: File = File(requireContext().filesDir,"Content").also{
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
                            resolveActivity(requireContext().packageManager)
                            putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(
                                    requireContext(),
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
                            resolveActivity(requireContext().packageManager)
                            putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(
                                    requireContext(),
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

    private fun hideKeyboard(activity: Activity, currentView : View? = null) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        var view = currentView
        if(view == null){
            //Find the currently focused view, so we can grab the correct window token from it.
            view = activity.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
        }

        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}