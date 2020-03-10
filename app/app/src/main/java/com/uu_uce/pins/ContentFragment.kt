package com.uu_uce.pins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

                val windowContent = contentView?.findViewById<TextView>(R.id.text_content)
                windowContent?.text = getString(R.string.sample_text)
            }
            PinType.IMAGE -> {
                contentView = inflater.inflate(R.layout.popup_window_image_content, container)
            }
            PinType.VIDEO -> {

            }
        }
        return contentView
    }
}