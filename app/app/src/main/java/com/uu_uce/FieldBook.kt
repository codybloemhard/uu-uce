package com.uu_uce

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.uu_uce.views.onCreateToolbar

class FieldBook : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        onCreateToolbar(this, "my fieldbook")
    }
}
