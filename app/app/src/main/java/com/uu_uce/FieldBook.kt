package com.uu_uce

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
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
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.database.UceRoomDatabase
import com.uu_uce.fieldbook.FieldbookAdapter
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.services.LocationServices
import com.uu_uce.services.checkPermissions
import com.uu_uce.ui.createTopbar
import kotlinx.coroutines.MainScope
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class FieldBook : AppCompatActivity() {

    var permissionsNeeded = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    lateinit var image: ImageView
    lateinit var text: EditText
    lateinit var imageUri: Uri
    lateinit var textInput: String
    lateinit var bitmap: Bitmap

    lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        createTopbar(this, "my fieldbook")

        val fieldbook = getFieldbookData()

        val recyclerView = findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FieldbookAdapter(this, fieldbook)

        val parent = findViewById<View>(R.id.fieldbook_layout)

        val addButton = findViewById<FloatingActionButton>(R.id.fieldbook_fab)

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

        val missingPermissions = checkPermissions(this,permissionsNeeded + LocationServices.permissionsNeeded)
        if(missingPermissions.count() == 0) {
            //TODO: use existing LocationServices
            //TODO: or copy all cases and exceptions
            val locationManager: LocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            val locationListener: LocationListener? = null
            locationManager.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                locationListener,
                Looper.myLooper()
            )
            val locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val location = Pair(locationGps.latitude, locationGps.longitude)
        }

        text = customView.findViewById(R.id.addText)

        image = customView.findViewById(R.id.addImage)

        image.setOnClickListener {
            selectImage(this)
        }

        val closePopup = customView.findViewById<Button>(R.id.close_fieldbook_popup)
        closePopup.setOnClickListener{
            val sdf = DateFormat.getDateTimeInstance()
            val currentDate = sdf.format(Date())


            saveFieldbookEntry(textInput, bitmap, currentDate)
            popupWindow.dismiss()
        }
    }

    private fun selectImage(context: Context) {
        val options = arrayOf("Take Photo", "Choose from gallery", " Cancel")
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Upload an image")

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> {
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    /*
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        null
                    }
                    if (photoFile != null) {
                        val photoUri: Uri = FileProvider.getUriForFile(
                            this,
                            "com.uu-uce.fileprovider",
                            photoFile
                        )
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    }
                     */
                    startActivityForResult(takePictureIntent, 0)
                }
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

            //TODO: this is just a thumbnail... get full size picture
            when (requestCode) {
                0 -> {
                    bitmap = data.extras?.get("data") as Bitmap
                    image.setImageBitmap(bitmap) }
                1 -> {
                    imageUri = data.data!!
                    image.setImageURI(imageUri) }
            }
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("nl_NL")).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).also {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = it.absolutePath
        }
    }

    private fun saveFieldbookEntry(
        text: String,
        image: Bitmap,
        currentDate: String
    ) {

    }

    fun getFieldbookData(): MutableList<FieldbookEntry> {
        val fieldbookDao = UceRoomDatabase.getDatabase(this, MainScope()).fieldbookDao()
        return fieldbookDao.getAllFieldbookEntries()
    }
}
