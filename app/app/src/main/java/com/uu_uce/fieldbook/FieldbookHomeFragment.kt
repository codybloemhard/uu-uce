package com.uu_uce.fieldbook

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.R
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.BlockTag
import com.uu_uce.pins.ContentBlockInterface
import com.uu_uce.pins.ImageContentBlock
import com.uu_uce.services.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

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

        lateinit var fieldbookDir : File
    }

    enum class RequestCode(val num: Int) {
        REQUEST_IMAGE_UPLOAD    (0),
        REQUEST_IMAGE_CAPTURE   (1),
        REQUEST_VIDEO_CAPTURE   (2)
    }

    private lateinit var viewModel: FieldbookViewModel
    private lateinit var fragmentActivity: FragmentActivity

    private lateinit var layout: LinearLayout
    private lateinit var title: EditText

    private var imageUri = ""

    private var content: MutableList<ContentBlockInterface> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fieldbookDir = File(requireContext().filesDir,"fieldbook").also {
            it.mkdirs()
        }

        fragmentActivity = requireActivity()

        viewModel = fragmentActivity.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        }
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
        imageUri = ""

        val customView = layoutInflater.inflate(R.layout.add_fieldbook_popup, null, false)
        val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        popupWindow.showAtLocation(fragmentActivity.findViewById(R.id.fieldbook_layout), Gravity.CENTER, 0, 0)

        // makes sure the keyboard appears whenever we want to add text
        popupWindow.isFocusable = true
        popupWindow.update()

        title = customView.findViewById(R.id.add_title)

        layout = customView.findViewById(R.id.fieldbook_content_blocks)

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
            Logger.log(LogType.Event, "Fielbook", "No last known location")
        }

        //TODO
        val savePinButton = customView.findViewById<Button>(R.id.add_fieldbook_pin)
        savePinButton.setOnClickListener{
            saveFieldbookEntry(
                title.text.toString(),
                imageUri,
                getCurrentDateTime(DateTimeFormat.FIELDBOOK_ENTRY),
                location
            )
            popupWindow.dismiss()
        }
    }

    private fun selectImage() {

        val options = arrayOf("Choose from gallery", "Take Photo", " Cancel")

        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Upload an image")

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> { // Choose from gallery
                    getPermissions(fragmentActivity, listOf(Manifest.permission.READ_EXTERNAL_STORAGE), CAMERA_REQUEST)
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragmentActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        startActivityForResult(
                            Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            ),
                            RequestCode.REQUEST_IMAGE_UPLOAD.num
                        )
                    }
                }
                1 -> { // Take photo
                    getPermissions(fragmentActivity, listOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragmentActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startActivityForResult(
                            Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE
                            ),
                            RequestCode.REQUEST_IMAGE_CAPTURE.num
                        )
                    }
                }
                2 -> dialogInterface.dismiss()
            }
        }
        dialog.show()
    }

    private fun selectVideo() {
        val options = arrayOf("Record Video", " Cancel")

        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Upload a video")

        dialog.setItems(options) { dialogInterface, which ->

            when (which) { //TODO: add check for permissions
                0 -> {
                    startActivityForResult(
                        Intent(
                            MediaStore.ACTION_VIDEO_CAPTURE).also {
                            it.resolveActivity(fragmentActivity.packageManager)
                        },
                        RequestCode.REQUEST_VIDEO_CAPTURE.num
                    )
                }
                else -> dialogInterface.dismiss()
            }
        }
        dialog.show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 0 || requestCode == 1) {
                saveBitmapToLocation(
                    when (requestCode) {
                        0 -> { // Choose from gallery
                            val uri = data.data
                            uri?.let { getImageFromGallery(it) }!!
                        }
                        else -> {// Take photo
                            //TODO: this is just a thumbnail... get full size picture
                            data.extras?.get("data") as Bitmap
                        }
                    }
                ).also {
                    addImage(it)
                }
            } else { // Captured video; requestCode = 2
                val uri = data.data
                println(uri!!.path)
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            TODO()
        }
    }

    private fun addText() {
        //TODO
        val text = EditText(requireContext())
        layout.addView(text, layoutParams)
    }

    private fun addImage(uri: Uri) {
        val imageView = ImageView(requireContext())
        layout.addView(imageView,layoutParams)
        imageView.setImageURI(uri)
        content.add(ImageContentBlock(uri))
    }

    private fun addVideo() {
        //TODO
    }


    private fun saveFieldbookEntry(
        title: String,
        image: String,
        currentDate: String,
        location: Location?
    ) {
        val content = listOf(
            Pair(
                BlockTag.TEXT,
                title
            ),
            Pair(
                BlockTag.IMAGE,
                image
            )
        )

        val utm = if(location == null){
            UTMCoordinate(0, 'N', 0.0, 0.0).toString()
        }
        else{
            degreeToUTM(Pair(location.latitude,location.longitude)).toString()
        }

        FieldbookEntry(
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

    private fun saveBitmapToLocation(image: Bitmap): Uri {
        val file = imageLocation()

        FileOutputStream(imageLocation()).also{
            image.compress(Bitmap.CompressFormat.PNG,100,it)
        }.apply{
            flush()
            close()
        }

        return file.toUri()
    }

    private fun getImageFromGallery(currentLocation: Uri): Bitmap {
        return BitmapFactory.decodeStream(
            fragmentActivity.contentResolver.openInputStream(
                currentLocation
            )
        )
    }

    private fun imageLocation(): File {
        val myDir: File = File(fieldbookDir,"Pictures").also{
            it.mkdirs()
        }
        val fileName = "IMG_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}.png"
        return File(myDir,fileName)
    }

    private fun saveVideoToLocation(uri: Uri) {

    }

    private fun videoLocation() : File {
        val myDir: File = File(fieldbookDir,"Videos").also{
            it.mkdirs()
        }
        val fileName = "VID_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}.mp4"
        return File(myDir,fileName)
    }

    private fun buildJSONContent(contentList: List<Pair<BlockTag,String>>): String {
        return  "[" +
                "{" +
                "\"tag\":\"${contentList.first().first}\"," +
                "\"text\":\"${contentList.first().second}\"" +
                "}," +
                "{" +
                "\"tag\":\"${contentList.last().first}\"," +
                "\"file_path\":\"${contentList.last().second}\"" +
                "}" +
                "]"
    }

    enum class DateTimeFormat{
        FILE_PATH,
        FIELDBOOK_ENTRY
    }

    private fun getCurrentDateTime(dtf: DateTimeFormat): String {
        val pattern: String = when(dtf) {
            DateTimeFormat.FILE_PATH -> "yyyMMdd_HHmmss"
            DateTimeFormat.FIELDBOOK_ENTRY -> "dd-MM-yyyy HH:mm"
        }

        return SimpleDateFormat(
            pattern,
            Locale("nl_NL")
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
}