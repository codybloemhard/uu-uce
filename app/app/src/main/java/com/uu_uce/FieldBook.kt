package com.uu_uce

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
import com.uu_uce.pins.BlockTag
import com.uu_uce.services.*
import com.uu_uce.ui.createTopbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FieldBook : AppCompatActivity() {
    private var permissionsNeeded = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private var cameraAccess = false
    private var filesAccess = false

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

        val fieldbookAdapter = FieldbookAdapter(this, fieldbookViewModel)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fieldbookAdapter


        fieldbookViewModel.allFieldbookEntries.observe(this, androidx.lifecycle.Observer {
            fieldbookAdapter.setFieldbook(it)
        })

        addButton.setOnClickListener {
            openFieldbookAdderPopup(parent)
        }
    }

    private fun openFieldbookAdderPopup(parent: View) {
        val customView = layoutInflater.inflate(R.layout.add_fieldbook_popup, null, false)
        val popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0)

        // makes sure the keyboard appears whenever we want to add text
        popupWindow.isFocusable = true
        popupWindow.update()

        getPermissions(this, permissionsNeeded, CAMERA_REQUEST)

        text = customView.findViewById(R.id.addText)

        imageView = customView.findViewById(R.id.addImage)

        imageView.setOnClickListener {
            selectImage(this)
        }

        val savePinButton = customView.findViewById<Button>(R.id.close_fieldbook_popup)
        savePinButton.setOnClickListener {
            //val sdf = DateFormat.getDateTimeInstance()

            saveFieldbookEntry(
                text.text.toString(),
                imageUri,
                getCurrentDateTime(DateTimeFormat.FIELDBOOK_ENTRY),
                LocationServices.lastKnownLocation
            )
            popupWindow.dismiss()
        }
    }

    private fun selectImage(context: Context) {
        // Check how an image may be selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            filesAccess = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            cameraAccess = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }

        val options = mutableListOf<String>()
        if (cameraAccess) options.add("Take Photo")
        if (filesAccess) options.add("Choose from gallery")
        options.add("Cancel")
        val optionsArray = options.toTypedArray()

        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Upload an image")

        dialog.setItems(optionsArray) { dialogInterface, which ->
            var index = which
            if (!cameraAccess && !filesAccess) index += 2
            else if (!cameraAccess) index += 1
            else if (!filesAccess) index *= 2

            when (index) {
                0 -> startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0)
                1 -> startActivityForResult(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    ), 1
                )
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
            degreeToUTM(Pair(location.latitude, location.longitude)).toString(),
            currentDate,
            buildJSONContent(content).also { jsonString ->
                // added for debugging purposes
                val root = "data/data/com.uu_uce/files/fieldbook"
                val myDir: File = File("$root/Content").also {
                    it.mkdirs()
                }
                val fileName = "TestContent.txt"
                val file = File(myDir, fileName)
                file.writeText(jsonString)
            }
        ).also {
            fieldbookViewModel.insert(it)
        }
    }

    private fun saveBitmapToLocation(image: Bitmap): String {
        val file = imageLocation()

        FileOutputStream(imageLocation()).also {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.apply {
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
        val myDir: File = File("$root/Pictures").also {
            it.mkdirs()
        }
        val fileName = "IMG_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}.png"
        return File(myDir, fileName)
    }

    private fun buildJSONContent(contentList: List<Pair<BlockTag, String>>): String {
        return "[" +
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

    enum class DateTimeFormat {
        FILE_PATH,
        FIELDBOOK_ENTRY
    }

    private fun getCurrentDateTime(dtf: DateTimeFormat): String {
        val pattern: String = when (dtf) {
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
