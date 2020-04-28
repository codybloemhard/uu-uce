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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.FOCUS_DOWN
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
import com.uu_uce.pins.ContentBlockInterface
import com.uu_uce.pins.ImageContentBlock
import com.uu_uce.pins.TextContentBlock
import com.uu_uce.pins.VideoContentBlock
import com.uu_uce.services.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [FieldbookHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FieldbookHomeFragment : Fragment() {

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        fun newInstance() =
            FieldbookHomeFragment()

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
    }

    private lateinit var viewModel: FieldbookViewModel
    private lateinit var fragmentActivity: FragmentActivity

    private lateinit var customView : View
    private lateinit var scrollView : ScrollView
    private lateinit var layout     : LinearLayout
    private lateinit var title      : EditText

    private var currentName = ""
    private var currentPath = ""
    private var currentUri: Uri = Uri.EMPTY

    private var blockID = 0
    private var content: MutableList<ContentBlockInterface> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * TODO: used function is deprecated from API 29 and onwards. Can still be used, because android:requestLegacyExternalStorage="true" in the manifest
         * Eventually switch to the MediaStore API. Doesn't need READ/WRITE permissions anymore -> only to be imported for API 28 and lower
         */
        fieldbookDir = File(Environment.getExternalStorageDirectory(),"UU-UCE/Fieldbook").also {
            it.mkdirs()
        }

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
        return inflater.inflate(R.layout.fragment_fieldbook_home, container, false).also {view ->
            val recyclerView = view.findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
            val addButton = view.findViewById<FloatingActionButton>(R.id.fieldbook_fab)

            val fieldbookAdapter = FieldbookAdapter(fragmentActivity, viewModel)

            viewModel.allFieldbookEntries.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                fieldbookAdapter.setFieldbook(it)
            })

            recyclerView.layoutManager = LinearLayoutManager(fragmentActivity)
            recyclerView.adapter = fieldbookAdapter

            addButton.setOnClickListener {
                openFieldbookAdderPopup()
            }
        }
    }

    /**
     * Opens a popup, in which we can make new entries to the fieldbook
     */
    private fun openFieldbookAdderPopup() {
        blockID = 0
        resetVariables()

        customView = layoutInflater.inflate(R.layout.add_fieldbook_popup, null, false)
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

        title = customView.findViewById(R.id.add_title)
        layout = customView.findViewById(R.id.fieldbook_content_container)
        scrollView = customView.findViewById(R.id.fieldbook_scroll_view)

        customView.findViewById<ImageButton>(R.id.add_text_block).also{
            it.setOnClickListener {
                addText()
            }
        }

        customView.findViewById<ImageButton>(R.id.add_image_block).also{
            it.setOnClickListener {
                selectImage()
            }
        }

        customView.findViewById<ImageButton>(R.id.add_video_block).also{
            it.setOnClickListener {
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
                    getPermissions(fragmentActivity, listOf(Manifest.permission.READ_EXTERNAL_STORAGE), CAMERA_REQUEST)
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragmentActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        startActivityForResult(
                            Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            ,
                            REQUEST_IMAGE_UPLOAD
                        )
                    }
                }
                1 -> { // Take photo
                    getPermissions(fragmentActivity, listOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
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
                        CAMERA_REQUEST
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
                                        println(it)
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
        if (resultCode == Activity.RESULT_OK && intent != null) {
            when (requestCode) {
                REQUEST_IMAGE_UPLOAD -> {
                    val uri = intent.data
                    if (uri != null) {
                        val cr = requireContext().contentResolver
                        cr.query(uri,null,null,null,null)?.use {
                            val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                            it.moveToFirst()
                            currentName = File(it.getString(nameIndex)).nameWithoutExtension
                        }
                        println(currentName)
                        currentUri = uri
                        addImage(currentUri)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    addImage(currentUri)
                    addToGallery(currentPath)
                }
                REQUEST_VIDEO_UPLOAD -> {
                    val uri = intent.data
                    if (uri != null) {
                        currentUri = uri
                        addVideo(makeVideoThumbnail(currentUri))
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
        title.clearFocus()
        val text = EditText(requireContext())
        layout.addView(text, layoutParams)
        scrollToEnd()

        //TODO: remove focus from editText when the user touches outside of it
        val button = Button(requireContext()).apply {
            setText(context.getString(R.string.done))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1.0f
                gravity = Gravity.END
            }
            gravity = Gravity.CENTER
            setOnClickListener {
                text.clearFocus()
                hideKeyboard(fragmentActivity, it)
            }
        }.also {
            layout.addView(it)
        }
        text.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                TextContentBlock(
                    text.text.toString()
                ).also {
                    it.generateContent(blockID++, layout, requireActivity(), customView, null)
                    content.add(it)
                }
                layout.apply {
                    removeView(text)
                    removeView(button)
                }
                scrollToEnd()
            }
        }
    }

    private fun addImage(image: Uri) {
        ImageContentBlock(
            image,
            makeImageThumbnail(image)
        ).also{
            it.generateContent(blockID++,layout,requireActivity(),customView,null)
            content.add(it)
        }
        scrollToEnd()
    }

    private fun addVideo(video: Uri) {
        title.clearFocus()
        VideoContentBlock(
            video,
            makeVideoThumbnail(video)
        ).also {
            it.generateContent(blockID++,layout, requireActivity(),customView,null)
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

        return saveThumbnail (
            requireContext().contentResolver.openInputStream(uri).let {
                BitmapFactory.decodeStream(it)
            }
        )
    }

    private fun makeVideoThumbnail(uri: Uri) : Uri {
        val retriever = MediaMetadataRetriever().apply {
            setDataSource(fragmentActivity,uri)
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
        //TODO: should we do this?
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(path)
            mediaScanIntent.data = Uri.fromFile(f)
            requireContext().sendBroadcast(mediaScanIntent)
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
            buildJSONContent(content).also{ jsonString ->
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

    private fun buildJSONContent(content: List<ContentBlockInterface>): String {
        return  content.joinToString(
            prefix      = "[",
            separator   = ",",
            postfix     = "]"
        ).also { jsonString ->
            // added for debugging purposes
            val myDir: File = File(requireContext().filesDir, "Content").also {
                it.mkdirs()
            }
            val fileName = "TestContent.txt"
            val file = File(myDir, fileName)
            file.writeText(jsonString)
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
            CAMERA_REQUEST -> {
                if(grantResults[0] == 0) {
                    startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0)
                }
            }
            EXTERNAL_FILES_REQUEST -> {
                if(grantResults[0] == 0){
                    startActivityForResult(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        ), 1
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