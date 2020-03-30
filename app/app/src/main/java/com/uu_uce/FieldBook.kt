package com.uu_uce

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.database.UceRoomDatabase
import com.uu_uce.fieldbook.FieldbookAdapter
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.ui.createTopbar
import kotlinx.coroutines.MainScope


class FieldBook : AppCompatActivity() {

    lateinit var image: ImageView
    lateinit var text: EditText
    lateinit var imageUri: Uri
    lateinit var textInput: String

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

        text = customView.findViewById(R.id.addText)

        //TODO: should show keyboard, doesn't work as of yet
        text.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                popupWindow.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            }
        }

        image = customView.findViewById(R.id.addImage)
        val closePopup = customView.findViewById<Button>(R.id.close_fieldbook_popup)

        image.setOnClickListener {
            selectImage(this)
        }

        closePopup.setOnClickListener{
            saveFieldbookEntry(textInput, imageUri)
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

            //TODO: this is just a thumbnail... get full size picture
            when (resultCode) {
                0 -> {
                    val imageBitmap = data.extras?.get("data") as Bitmap
                    image.setImageBitmap(imageBitmap) }
                1 -> {
                    val selectedImage: Uri? = data.data
                    image.setImageURI(selectedImage) }
            }
        }
    }

    private fun saveFieldbookEntry(text: String, image: Uri) {

    }

    fun getFieldbookData(): MutableList<FieldbookEntry> {
        val fieldbookDao = UceRoomDatabase.getDatabase(this, MainScope()).fieldbookDao()
        return fieldbookDao.getAllFieldbookEntries()
    }
}
