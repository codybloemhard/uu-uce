package com.uu_uce

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger

//activity in which videos from video pins are shown
class VideoViewer : Activity() {
    private var uiVisible : Boolean = true
    private lateinit var mediaController : MediaController
    private lateinit var videoPlayer : VideoView

    private var prevVideoPos : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
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

        //set close button
        val closeVideoButton = findViewById<Button>(R.id.close_video_player)
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