package com.uu_uce

import android.app.Activity
import android.content.Intent
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_viewer)

        val videoTitleText = findViewById<TextView>(R.id.video_title_text)
        videoTitleText.text = intent.getStringExtra("title")

        val videoPlayer = findViewById<VideoView>(R.id.video_player)
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

        videoPlayer.start()

        val closeVideoButton = findViewById<Button>(R.id.close_video_player)

        closeVideoButton.setOnClickListener {
            videoPlayer.stopPlayback()
            val intent = Intent(this, GeoMap::class.java)
            startActivity(intent)
        }
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