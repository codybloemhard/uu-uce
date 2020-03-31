package com.uu_uce

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger


class VideoViewer : Activity() {
    private var uiVisible : Boolean = true
    private lateinit var mediaController : MediaController
    private lateinit var videoPlayer : VideoView

    private var prevVideoPos : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_viewer)

        val videoTitleText = findViewById<TextView>(R.id.video_title_text)
        videoTitleText.text = intent.getStringExtra("title")

        videoPlayer = findViewById(R.id.video_player)
        videoPlayer.setVideoURI(intent.getParcelableExtra("uri"))
        val titleBar = findViewById<ConstraintLayout>(R.id.video_title)

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

        videoPlayer.setMediaController(mediaController)
        mediaController.setAnchorView(findViewById(R.id.video_player))

        videoPlayer.setOnPreparedListener {
            if(savedInstanceState != null){
                val videoPos = savedInstanceState.getInt("prevVideoPos")
                videoPlayer.seekTo(videoPos)
            }
            videoPlayer.start()
            mediaController.show(0)
        }

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

    override fun onBackPressed() {
        Logger.log(LogType.Continuous, "VideoViewer", "test3")
        finish()
    }
}