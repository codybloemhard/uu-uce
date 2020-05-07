package com.uu_uce.ui

import android.app.Activity
import android.widget.ImageButton
import android.widget.TextView
import com.uu_uce.R

/*
create a generic bar at the top of the screen
activity: the activity where the bar needs to be
title: the title visible in the bar
 */
fun createTopbar(activity : Activity, title: String)
{
    activity.findViewById<TextView>(R.id.toolbar_title).text = title

    activity.findViewById<ImageButton>(R.id.toolbar_back_button).setOnClickListener{
        activity.finish()
    }
}