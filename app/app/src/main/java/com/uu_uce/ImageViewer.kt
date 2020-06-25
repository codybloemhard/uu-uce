package com.uu_uce

import android.content.SharedPreferences
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher

/**
 * An activity in which a single image with title can be viewed and zoomed in on.
 * @property[sharedPref] the shared preferences where the settings are stored.
 * @constructor an ImageViewer activity.
 */
class ImageViewer : AppCompatActivity() {

    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val darkMode = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        // Set desired theme
        if(darkMode) setTheme(R.style.DarkTheme)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !darkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else if(!darkMode){
            window.statusBarColor = Color.BLACK// set status background white
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        // Set title in bar
        val imageTitleText = findViewById<TextView>(R.id.image_title_text)
        imageTitleText.text = intent.getStringExtra("title")

        // Load image
        val imageViewer = findViewById<PhotoView>(R.id.image_photoview)
        imageViewer.setImageURI(intent.getParcelableExtra("uri"))
        val attacher = PhotoViewAttacher(imageViewer)
        attacher.maximumScale = 5f
        attacher.maximumScale = 20f

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

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

