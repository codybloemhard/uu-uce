package com.uu_uce

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.uu_uce.ui.createTopbar

class Profile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        createTopbar(this, "Profile")
    }
}
