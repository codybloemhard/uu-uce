package com.uu_uce

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_image_viewer.*
import kotlinx.android.synthetic.main.activity_login_screen.view.*

//activity in which videos from video pins are shown
class VideoViewer : Activity() {
    private var uiVisible                   : Boolean = true
    private lateinit var mediaController    : MediaController
    private lateinit var videoPlayer        : VideoView
    private lateinit var sharedPref         : SharedPreferences
    private var prevVideoPos                : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // Get desired theme
        if(sharedPref.getBoolean("com.uu_uce.DARKMODE", false)) setTheme(R.style.DarkTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_viewer)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else{
            window.statusBarColor = Color.BLACK// set status background white
        }

        // Set title in bar
        val videoTitleText = findViewById<TextView>(R.id.video_title_text)
        videoTitleText.text = intent.getStringExtra("title")

        // Load video
        videoPlayer = findViewById(R.id.video_player)
        videoPlayer.setVideoURI(intent.getParcelableExtra("uri"))

        val titleBar = findViewById<ConstraintLayout>(R.id.video_title)

        //override show/hide functions to make the bar on top appear and disappear too
        mediaController = object : MediaController(this) {
            override fun show() {
                show(0)
            }
            override fun show(timeout: Int) {
                super.show(0)
                titleBar.visibility = View.VISIBLE
                uiVisible = true
            }
            override fun hide() {
                super.hide()
                titleBar.visibility = View.GONE
                uiVisible = false
            }
        }

        videoPlayer.setOnPreparedListener { mp ->
            mp.setOnVideoSizeChangedListener { _, _, _ ->
                videoPlayer.setMediaController(mediaController)
                mediaController.setAnchorView(videoPlayer)
            }
            if(savedInstanceState != null){
                val videoPos = savedInstanceState.getInt("prevVideoPos")
                videoPlayer.seekTo(videoPos)
            }
            videoPlayer.start()
        }

        val color =
            if(sharedPref.getBoolean("com.uu_uce.DARKMODE", false))
                ResourcesCompat.getColor(resources, R.color.BestWhite, null)
            else
                ResourcesCompat.getColor(resources, R.color.TextDarkGrey, null)

        val closeVideoButton = findViewById<Button>(R.id.close_video_player)

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

    override fun finish() {
        mediaController.hide()
        videoPlayer.stopPlayback()
        super.finish()
    }

    override fun onPause(){
        prevVideoPos = videoPlayer.currentPosition
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("prevVideoPos", prevVideoPos)
        super.onSaveInstanceState(outState)
    }
}