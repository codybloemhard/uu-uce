package com.uu_uce

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.android.synthetic.main.video_viewer.*


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

        val act = this

        val titleBar = findViewById<ConstraintLayout>(R.id.video_title)

        mediaController = object : MediaController(this) {
            override fun show() {
                super.show(0)
                titleBar.visibility = View.VISIBLE
            }
            override fun show(timeout: Int) {
                super.show(0)
                titleBar.visibility = View.VISIBLE
            }
            override fun hide() {
                super.hide()
                titleBar.visibility = View.GONE
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

    private fun setUIVisibility(visible : Boolean): Boolean{ //returns whether visibility was changed
        if(visible == uiVisible) return false
        if(visible){
            mediaController.show()
        }
        else{
            mediaController.hide()
        }
        uiVisible = !uiVisible
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event?.action == MotionEvent.ACTION_DOWN){
            setUIVisibility(!uiVisible)
            return true
        }
        return false
    }

    override fun onBackPressed() {
        Logger.log(LogType.Continuous, "VideoViewer", "test3")
        finish()
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