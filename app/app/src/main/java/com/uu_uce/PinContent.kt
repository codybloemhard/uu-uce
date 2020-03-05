package com.uu_uce

import android.os.Bundle
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment


class PinContent : Fragment() {

    private var textContentView : View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (textContentView == null) {
            println("textContentView == null")
        } else {
            println("textContentView != null")
        }

        textContentView = inflater.inflate(R.layout.popup_window_text_content, container)

        val windowContent = textContentView?.findViewById<TextView>(R.id.text_content)
        windowContent?.text = getString(R.string.sample_text)

        return textContentView
    }
}