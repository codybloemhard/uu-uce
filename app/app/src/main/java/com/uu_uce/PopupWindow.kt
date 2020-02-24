package com.uu_uce

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_popup_window.*

class PopupWindow : AppCompatActivity() {
    private var popupTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_popup_window)

        val bundle = intent.extras
        popupTitle = bundle?.getString("popuptitle", "Title") ?: ""

        popup_window_title.text = popupTitle
    }
}
