package com.uu_uce

import android.os.Bundle

import android.view.Gravity
import android.view.ViewGroup

import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import androidx.constraintlayout.widget.ConstraintLayout

class PinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_pins)

        val btnShowContent = findViewById<Button>(R.id.btnShowContent)
        val parentLayout = findViewById<ConstraintLayout>(R.id.testPins)
        var title = "Dit is de titel"

        btnShowContent.setOnClickListener {
            openPopupWindow(parentLayout, title)
        }
    }

    private fun openPopupWindow(parentLayout: ConstraintLayout, title: String) {

        val layoutInflater = layoutInflater

        // build an custom view (to be inflated on top of our current view & build it's popup window
        val customView = layoutInflater.inflate(R.layout.popup_window, null)
        val popupWindow = PopupWindow(customView,ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // add the title for the popup window
        val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
        windowTitle.text = title

        popupWindow.showAtLocation(parentLayout, Gravity.CENTER, 0, 0)

        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        val cf = fm.findFragmentById(R.id.ContentFragment)

        val btnClosePopupWindow = customView.findViewById<Button>(R.id.popup_window_close_button)

        btnClosePopupWindow.setOnClickListener {
            popupWindow.dismiss()
            if (cf != null) {
                ft.remove(cf).commit()
            }
        }
    }
}
