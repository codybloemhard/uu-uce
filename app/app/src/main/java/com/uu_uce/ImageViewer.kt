package com.uu_uce

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher

class ImageViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else{
            window.statusBarColor = Color.BLACK// set status background white
        }

        // Set title in bar
        val imageTitleText = findViewById<TextView>(R.id.image_title_text)
        imageTitleText.text = intent.getStringExtra("title")

        // Load image
        val imageViewer = findViewById<PhotoView>(R.id.image_photoview)
        imageViewer.setImageURI(intent.getParcelableExtra("uri"))
        PhotoViewAttacher(imageViewer)

        //set close button
        val closeVideoButton = findViewById<Button>(R.id.close_image_viewer)
        closeVideoButton.setOnClickListener {
            this.finish()
        }
    }
}
