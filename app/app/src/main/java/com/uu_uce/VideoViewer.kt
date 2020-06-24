package com.uu_uce

import android.app.Activity
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

/**
 * An activity in which a single video with title can be viewed.
 * @property[uiVisible] represents whether or not the title bar is visible.
 * @property[mediaController] the MediaController used to control the video playback.
 * @property[videoPlayer] the view that the video is displayed in.
 * @property[sharedPref] the shared preferences where the settings are stored.
 * @property[prevVideoPos] the last position the video was on before closing the activity.
 * @constructor an VideoViewer activity.
 */
class VideoViewer : Activity() {
    private var uiVisible                   : Boolean = true
    private lateinit var mediaController    : MediaController
    private lateinit var videoPlayer        : VideoView
    private lateinit var sharedPref         : SharedPreferences
    private var prevVideoPos                : Int = 0

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
        setContentView(R.layout.activity_video_viewer)

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

    /**
     * Hide the media controller when the activity is closed to avoid leaking.
     */
    override fun finish() {
        mediaController.hide()
        videoPlayer.stopPlayback()
        super.finish()
    }

    /**
     * Save the current video position to resume here when the screen is rotated.
     */
    override fun onPause() {
        prevVideoPos = videoPlayer.currentPosition
        super.onPause()
    }

    /**
     * Save the current position to the outstate to resume from here after restart.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("prevVideoPos", prevVideoPos)
        super.onSaveInstanceState(outState)
    }
}