package com.uu_uce

import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.uu_uce.ui.createTopbar
import kotlinx.android.synthetic.main.activity_settings.*

class Settings : AppCompatActivity() {
    // private variables
    private val minPinSize = 50
    private val maxPinSize = 100

    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)//this.getPreferences(Context.MODE_PRIVATE)

        createTopbar(this, "Settings")

        // PinSize
        val curSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", 60)
        pinsize_seekbar.max = maxPinSize - minPinSize
        pinsize_seekbar.progress = curSize - minPinSize
        pinsize_numberview.text = curSize.toString()

        pinsize_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the pinSize
                pinsize_numberview.text = (seekBar.progress + minPinSize).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val pinSize = seekBar.progress + minPinSize
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.PIN_SIZE", pinSize)
                    apply()
                }
            }
        })
    }
}