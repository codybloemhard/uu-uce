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


class VideoViewer : Activity() {
    private var uiVisible : Boolean = true
    private lateinit var mediaController : MediaController
    private lateinit var videoPlayer : VideoView

    private var prevVideoPos : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_viewer)

        // Set title in bar
        val videoTitleText = findViewById<TextView>(R.id.video_title_text)
        videoTitleText.text = intent.getStringExtra("title")

        // Load video
        videoPlayer = findViewById(R.id.video_player)
        videoPlayer.setVideoURI(intent.getParcelableExtra("uri"))

        mediaController = object : MediaController(this) {
            override fun show(timeout: Int) {
                super.show(0)
            }
            override fun hide(){
                super.show(0)
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
        }

        val closeVideoButton = findViewById<Button>(R.id.close_video_player)

        closeVideoButton.setOnClickListener {
            videoPlayer.stopPlayback()
            this.finish()
        }
    }

    override fun onPause(){
        prevVideoPos = videoPlayer.currentPosition
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("prevVideoPos", prevVideoPos)
        super.onSaveInstanceState(outState)
    }

    private fun setUIVisibility(visible : Boolean){
        val titleBar = findViewById<ConstraintLayout>(R.id.video_title)
        if(visible == uiVisible) return
        if(visible){
            titleBar.visibility = View.VISIBLE
            mediaController.show()
            mediaController.visibility = View.VISIBLE
        }
        else{
            titleBar.visibility = View.GONE
            mediaController.visibility = View.INVISIBLE
        }
        uiVisible = !uiVisible
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event?.action == MotionEvent.ACTION_DOWN){
            setUIVisibility(!uiVisible)
            return true
        }
        return  false
    }

    // VLC player code
    /*private fun initializeVideoPlayer(videoURI: Uri, view: View, activity: Activity) {
        val videoPlayer: VLCVideoLayout = view.findViewById(R.id.video_player)

        /* PLEASE KEEP COMMENTED: NEED THIS FOR FURTHER DEVELOPMENT
        val playerSurface: SurfaceView = activity.findViewById(R.id.player_surface)
        val surfaceHolder = playerSurface.holder
        val surface = surfaceHolder.surface
        val surfaceFrame: FrameLayout = activity.findViewById(R.id.player_surface_frame)
         */

        val libVLC: LibVLC = LibVLC(activity)
        val mediaPlayer: MediaPlayer = MediaPlayer(libVLC)

        mediaPlayer.attachViews(videoPlayer, null, false, false)
        val media: Media = Media(libVLC, videoURI)
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }*/
}