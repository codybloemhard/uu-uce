package com.uu_uce

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
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.fieldbook.FieldbookAdapter
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.fieldbook.FieldbookViewModel
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.BlockTag
import com.uu_uce.services.*
import com.uu_uce.ui.createTopbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Fieldbook : AppCompatActivity() {

    private var permissionsNeeded = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    private lateinit var imageView: ImageView
    lateinit var text: EditText

    private var imageUri: String = ""

    private lateinit var fieldbookViewModel: FieldbookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        val recyclerView = findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
        val parent = findViewById<View>(R.id.fieldbook_layout)
        val addButton = findViewById<FloatingActionButton>(R.id.fieldbook_fab)
        createTopbar(this, "my fieldbook")

        fieldbookViewModel = ViewModelProvider(this).get(FieldbookViewModel::class.java)

        val fieldbookAdapter = FieldbookAdapter(this,fieldbookViewModel)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fieldbookAdapter


        fieldbookViewModel.allFieldbookEntries.observe(this, androidx.lifecycle.Observer {
            fieldbookAdapter.setFieldbook(it)
        })

        addButton.setOnClickListener{
            openFieldbookAdderPopup(parent)
        }
    }

    private fun openFieldbookAdderPopup(parent: View) {
        val customView = layoutInflater.inflate(R.layout.add_fieldbook_popup, null, false)
        val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0)

        // makes sure the keyboard appears whenever we want to add text
        popupWindow.isFocusable = true
        popupWindow.update()

        checkPermissions(this,permissionsNeeded).let {
            for (item in it)
                when(item) {
                    Manifest.permission.READ_EXTERNAL_STORAGE ->
                        getPermissions(this,permissionsNeeded,1)
                    Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                        getPermissions(this,permissionsNeeded,1)
                    Manifest.permission.CAMERA ->
                        getPermissions(this,permissionsNeeded,3)
                }
        }

        text = customView.findViewById(R.id.addText)

        imageView = customView.findViewById(R.id.addImage)

        imageView.setOnClickListener {
            selectImage(this)
        }

        val closePopup = customView.findViewById<Button>(R.id.add_fieldbook_pin)
        closePopup.setOnClickListener{
            //val sdf = DateFormat.getDateTimeInstance()

            var location : Location? = null

            try{
                location = LocationServices.lastKnownLocation
            }
            catch(e : Exception){
                Logger.log(LogType.Event, "Fielbook", "No last known location")
            }


            saveFieldbookEntry(
                text.text.toString(),
                imageUri,
                getCurrentDateTime(DateTimeFormat.FIELDBOOK_ENTRY),
                location)
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
                1 -> startActivityForResult(Intent(Intent.ACTION_PICK,
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
                0 -> {//TODO: this is just a thumbnail... get full size picture
                    val bitmap = data.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(bitmap)
                    imageUri = saveBitmapToLocation(bitmap)
                }
                1 -> {
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
        location: Location?
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
                val root = "data/data/com.uu_uce/files/fieldbook"
                val myDir: File = File("$root/Content").also{
                    it.mkdirs()
                }
                val fileName = "TestContent.txt"
                val file = File(myDir,fileName)
                file.writeText(jsonString)
            }
        ).also{
            fieldbookViewModel.insert(it)
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
                contentResolver.openInputStream(
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
            Locale("nl_NL")).format(Date()
        )
    }
}
