package com.uu_uce.ui

import android.app.Activity
import android.widget.ImageButton
import android.widget.TextView
import com.uu_uce.R

fun onCreateToolbar(activity : Activity, title: String)
{
    activity.findViewById<TextView>(R.id.toolbar_title).text = title

    activity.findViewById<ImageButton>(R.id.toolbar_back_button).setOnClickListener{
        activity.finish()
    }
}