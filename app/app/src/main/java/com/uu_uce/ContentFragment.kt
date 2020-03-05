package com.uu_uce

import android.os.Bundle
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinType

class ContentFragment : Fragment() {

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
                windowContent?.text = pin?.content?.getTextContent()
            }
            PinType.IMAGE -> {

            }
            PinType.VIDEO -> {

            }
        }

        return contentView
    }
}