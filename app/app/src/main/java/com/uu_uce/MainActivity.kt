package com.uu_uce

import android.content.Intent
import android.os.Bundle
import com.uu_uce.ui.TouchParent

class MainActivity : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, PinActivity::class.java)
        startActivity(intent)
    }
}
