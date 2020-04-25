package com.uu_uce

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.uu_uce.ui.createTopbar
import kotlinx.android.synthetic.main.settings_activity.*

class Settings : AppCompatActivity() {
    // private variables
    private val minPinSize = 10
    private val maxPinSize = 100

    private lateinit var sharedPref     : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        createTopbar(this, "Settings")

        val curSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", 60)
        pinsize_seekbar.max = maxPinSize - minPinSize
        pinsize_seekbar.progress = curSize - minPinSize
        pinsize_numberview.text = (curSize + minPinSize).toString()

        pinsize_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the pinSize
                pinsize_numberview.text = (seekBar.progress + minPinSize).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.PIN_SIZE", seekBar.progress + minPinSize)
                    apply()
                }
            }
        })
    }
}