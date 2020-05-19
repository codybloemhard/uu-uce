package com.uu_uce

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import kotlinx.android.synthetic.main.activity_image_viewer.*
import kotlinx.android.synthetic.main.activity_login_screen.view.*

class ImageViewer : AppCompatActivity() {

    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // Get desired theme
        if(sharedPref.getBoolean("com.uu_uce.DARKMODE", false)) setTheme(R.style.DarkTheme)

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

        val color =
            if(sharedPref.getBoolean("com.uu_uce.DARKMODE", false))
                ResourcesCompat.getColor(resources, R.color.BestWhite, null)
            else
                ResourcesCompat.getColor(resources, R.color.TextDarkGrey, null)

        val closeVideoButton = findViewById<Button>(R.id.close_image_viewer)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            closeVideoButton.background.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        }
        else{
            // Older versions will use depricated function
            @Suppress("DEPRECATION")
            closeVideoButton.background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }

        //set close button
        closeVideoButton.setOnClickListener {
            this.finish()
        }
    }
}
