package com.uu_uce

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.uu_uce.ui.createTopbar
import kotlinx.android.synthetic.main.activity_settings.*

class Settings : AppCompatActivity() {
    // private variables
    private val minPinSize = 10
    private val maxPinSize = 100

    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPref = getDefaultSharedPreferences(this)

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

        // Debug
        val curDebug = sharedPref.getBoolean("com.uu_uce.DEBUG", false)
        debug_switch.isChecked = curDebug
        debug_switch.setOnClickListener{
            with(sharedPref.edit()) {
                putBoolean("com.uu_uce.DEBUG", debug_switch.isChecked)
                apply()
            }
        }

        // hardware acceleration
        val curHardware = sharedPref.getBoolean("com.uu_uce.HARDWARE", false)
        hardware_switch.isChecked = curHardware
        hardware_switch.setOnClickListener{
            with(sharedPref.edit()) {
                putBoolean("com.uu_uce.HARDWARE", hardware_switch.isChecked)
                apply()
            }
        }
    }
}