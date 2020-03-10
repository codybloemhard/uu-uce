package com.uu_uce

import android.os.Bundle

import android.view.Gravity
import android.view.ViewGroup

import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinTextContent
import com.uu_uce.pins.PinType
import com.uu_uce.services.UTMCoordinate

class PinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_pins)

        val btnShowContent = findViewById<Button>(R.id.btnShowContent)
        val parentLayout = findViewById<ConstraintLayout>(R.id.testPins)
        val title = "Dit is de titel"
        val ptc = PinTextContent()
        ptc.text = getString(R.string.sample_text)
        val pin =
            getDrawable(R.drawable.ic_show_infographic)?.let {
                Pin(UTMCoordinate(31, 'N', 0.0, 0.0),0, PinType.TEXT, title, ptc, 60,
                    it
                )
            }

        btnShowContent.setOnClickListener {
            openPopupWindow(parentLayout, pin)
        }
    }

    private fun openPopupWindow(parentLayout: ConstraintLayout, pin: Pin?) {
        // make sure we can access the Pin in the fragment
        ContentFragment.pin = pin

        val layoutInflater = layoutInflater

        // build an custom view (to be inflated on top of our current view & build it's popup window
        val customView = layoutInflater.inflate(R.layout.popup_window, null)
        val popupWindow = PopupWindow(customView,ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // add the title for the popup window
        val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
        windowTitle.text = pin?.title

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
