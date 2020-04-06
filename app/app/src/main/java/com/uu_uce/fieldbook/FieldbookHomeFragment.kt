package com.uu_uce.fieldbook

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.FieldBook
import com.uu_uce.R
import com.uu_uce.pins.BlockTag
import com.uu_uce.services.LocationServices
import com.uu_uce.services.checkPermissions
import com.uu_uce.services.degreeToUTM
import com.uu_uce.services.getPermissions
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
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

    private lateinit var viewModel: FieldbookViewModel

    private lateinit var imageView: ImageView
    private lateinit var text: EditText

    private var imageUri = ""

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fieldbook_home, container, false).also {view ->
            val recyclerView = view.findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
            val addButton = view.findViewById<FloatingActionButton>(R.id.fieldbook_fab)

            val fieldbookAdapter = FieldbookAdapter(requireActivity(), viewModel)

            viewModel.allFieldbookEntries.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                fieldbookAdapter.setFieldbook(it)
            })

            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = fieldbookAdapter

            addButton.setOnClickListener {
                openFieldbookAdderPopup()
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FieldbookHomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String) =
            FieldbookHomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun openFieldbookAdderPopup() {
        imageUri = ""

        val customView = layoutInflater.inflate(R.layout.add_fieldbook_popup, null, false)
        val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        popupWindow.showAtLocation(activity!!.findViewById(R.id.fieldbook_layout), Gravity.CENTER, 0, 0)

        // makes sure the keyboard appears whenever we want to add text
        popupWindow.isFocusable = true
        popupWindow.update()

        checkPermissions(this.requireContext(), FieldBook.permissionsNeeded).let {
            for (item in it)
                when(item) {
                    Manifest.permission.READ_EXTERNAL_STORAGE ->
                        getPermissions(activity!!.parent,FieldBook.permissionsNeeded,1)
                    Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                        getPermissions(activity!!.parent,FieldBook.permissionsNeeded,1)
                    Manifest.permission.CAMERA ->
                        getPermissions(activity!!.parent,FieldBook.permissionsNeeded,3)
                }
        }

        text = customView.findViewById(R.id.addText)

        imageView = customView.findViewById(R.id.addImage)

        imageView.setOnClickListener {
            selectImage(activity!!)
        }

        val closePopup = customView.findViewById<Button>(R.id.close_fieldbook_popup)
        closePopup.setOnClickListener{
            val sdf = DateFormat.getDateTimeInstance()

            saveFieldbookEntry(
                text.text.toString(),
                imageUri,
                getCurrentDateTime(DateTimeFormat.FIELDBOOK_ENTRY),
                LocationServices.lastKnownLocation)
            popupWindow.dismiss()
        }
    }

    private fun selectImage(context: Context) {
        val options = arrayOf("Take Photo", "Choose from gallery", " Cancel")
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Upload an image")

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0)
                1 -> startActivityForResult(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 1)
                2 -> dialogInterface.dismiss()
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
            when (requestCode) {
                0 -> {
                    val bitmap = data.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(bitmap)
                    imageUri = saveBitmapToLocation(bitmap)
                }
                1 -> { //TODO: this is just a thumbnail... get full size picture
                    val uri = data.data
                    imageView.setImageURI(uri)
                    if (uri != null)
                        imageUri = moveImageFromGallery(uri)
                }
            }
        }
    }

    private fun saveFieldbookEntry(
        text: String,
        image: String,
        currentDate: String,
        location: Location
    ) {
        val content = listOf(
            Pair(
                BlockTag.TEXT,
                text
            ),
            Pair(
                BlockTag.IMAGE,
                image
            )
        )

        FieldbookEntry(
            degreeToUTM(Pair(location.latitude,location.longitude)).toString(),
            currentDate,
            buildJSONContent(content).also{ jsonString ->
                // added for debugging purposes
                val root = "data/data/com.uu_uce/files/fieldbook"
                val myDir: File = File("$root/Content").also{
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

    private fun saveBitmapToLocation(image: Bitmap): String {
        val file = imageLocation()

        FileOutputStream(imageLocation()).also{
            image.compress(Bitmap.CompressFormat.PNG,100,it)
        }.apply{
            flush()
            close()
        }

        return file.toUri().toString()
    }

    private fun moveImageFromGallery(currentLocation: Uri): String {
        return saveBitmapToLocation(
            BitmapFactory.decodeStream(
                activity!!.contentResolver.openInputStream(
                    currentLocation
                )
            )
        )
    }

    private fun imageLocation(): File {
        val root = "data/data/com.uu_uce/files/fieldbook"
        val myDir: File = File("$root/Pictures").also{
            it.mkdirs()
        }
        val fileName = "IMG_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}.png"
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
}