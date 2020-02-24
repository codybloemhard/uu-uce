package com.uu_uce

import android.content.Intent
import android.os.Bundle
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_test_pins.*

class TestPins : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_pins)

        btnShowContent.setOnClickListener {
            val intent = Intent(this, PopupWindow :: class.java)
            intent.putExtra("popuptitle", "Title")
            startActivity(intent)
        }

    }

}
