package com.uu_uce.pins

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.uu_uce.R

class ContentFragment() : Fragment() {

    companion object{
        var pin: Pin? = null
    }
    private var contentView : View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        when (pin?.type) {
            PinType.TEXT -> {
                contentView = inflater.inflate(R.layout.popup_window_text_content, container)
                val content = getString(R.string.sample_text) //TODO: use pincontent

                val textContent = contentView?.findViewById<TextView>(R.id.text_content)
                textContent?.text = content
            }
            PinType.IMAGE -> {
                contentView = inflater.inflate(R.layout.popup_window_image_content, container)
                val content = Uri.parse("app\\src\\main\\res\\drawable\\logo.png") //TODO: use filepath for image

                val imageContent = contentView?.findViewById<ImageView>(R.id.image_content)
                imageContent?.setImageURI(content)
                imageContent?.contentDescription = "description"
            }
            PinType.VIDEO -> {
                contentView = inflater.inflate(R.layout.popup_window_video_content, container)
                val content = Uri.parse("app\\src\\main\\res\\drawable\\logo.png") //TODO: use filepath for video

                val videoContent = contentView?.findViewById<VideoView>(R.id.video_content)
                videoContent?.setVideoURI(content)
                videoContent?.contentDescription = "description"
            }
        }

        return contentView
    }
}