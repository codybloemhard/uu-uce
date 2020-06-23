package com.uu_uce.ui

import android.app.Activity
import android.widget.ImageButton
import android.widget.TextView
import com.uu_uce.R

/**
 * Create a generic bar at the top of the screen.
 * @param[activity] the activity where the bar needs to be.
 * @param[title] the title visible in the bar.
 * @param[backPressAction] the action executed when the backbutton is pressed.
 */
fun createTopbar(activity : Activity, title: String, backPressAction : (() -> Unit) = { activity.finish() })
{
    activity.findViewById<TextView>(R.id.toolbar_title).text = title

    activity.findViewById<ImageButton>(R.id.toolbar_back_button).setOnClickListener{
        backPressAction()
    }
}