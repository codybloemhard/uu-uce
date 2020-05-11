package com.uu_uce

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.uu_uce.services.unpackZip
import com.uu_uce.services.updateFiles
import com.uu_uce.ui.createTopbar
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.*

// Default settings
const val defaultPinSize = 60

class Settings : AppCompatActivity() {
    // private variables
    private val minPinSize = 10
    private val maxPinSize = 100

    // TODO: Remove temporary hardcoded map information
    private val mapsName = "maps.zip"
    private lateinit var maps : List<String>

    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        maps = listOf(getExternalFilesDir(null)?.path + File.separator + mapsName)

        sharedPref = getDefaultSharedPreferences(this)

        createTopbar(this, "Settings")

        // PinSize
        val curSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)
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

        // Network downloading
        val curNetworkDownloading = sharedPref.getBoolean("com.uu_uce.NETWORK_DOWNLOADING", false)
        networkdownload_switch.isChecked = curNetworkDownloading
        networkdownload_switch.setOnClickListener{
            if(networkdownload_switch.isChecked){
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_sprite_warning)
                    .setTitle("Enabling mobile data")
                    .setMessage("Are you sure you want to enable downloading over mobile data? This may lead to significant amounts of data being used.")
                    .setPositiveButton("Yes") { _, _ ->
                        with(sharedPref.edit()) {
                            putBoolean(
                                "com.uu_uce.NETWORK_DOWNLOADING",
                                true
                            )
                            apply()
                        }
                    }
                    .setNegativeButton("No") { _, _ -> networkdownload_switch.isChecked = false }
                    .show()
            }
            else{
                with(sharedPref.edit()) {
                    putBoolean(
                        "com.uu_uce.NETWORK_DOWNLOADING",
                        false
                    )
                    apply()
                }
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

        // Download maps
        fun downloadMaps(){
            maps_downloading_progress.visibility = View.VISIBLE
            updateFiles(
                maps,
                this,
                {
                    runOnUiThread {
                        Toast.makeText(this, "Download completed, unpacking", Toast.LENGTH_LONG)
                            .show()
                    }
                    unpackZip(maps.first()) { progress ->
                        runOnUiThread {
                            maps_downloading_progress.progress = progress
                        }
                    }
                    runOnUiThread {
                        Toast.makeText(this, "Unpacking completed", Toast.LENGTH_LONG).show()
                        maps_downloading_progress.visibility = View.INVISIBLE
                        needsReload = true // TODO: Do this in a neater way
                        delete_maps_button.visibility = View.VISIBLE
                    }
                },
                { progress -> runOnUiThread { maps_downloading_progress.progress = progress } }
            )
        }

        download_maps_button.setOnClickListener{
            if(!File(getExternalFilesDir(null)?.path + File.separator + "Maps").exists()) {
                downloadMaps()
            }
            else{
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_sprite_question)
                    .setTitle("Maps already downloaded")
                    .setMessage("Are you sure you want to download the maps again?")
                    .setPositiveButton("Yes") { _, _ ->
                        downloadMaps()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

        // Delete maps
        delete_maps_button.visibility =
            if(File(getExternalFilesDir(null)?.path + File.separator + "Maps").exists()){
            View.VISIBLE
        }
        else{
            View.INVISIBLE
        }
        delete_maps_button.setOnClickListener {
            AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_sprite_warning)
                .setTitle("Deleting maps")
                .setMessage("Are you sure you want to delete the maps.")
                .setPositiveButton("Yes") { _, _ ->
                    File(getExternalFilesDir(null)?.path + File.separator + "Maps").deleteRecursively()
                    needsReload = true
                    delete_maps_button.visibility = View.INVISIBLE
                    Toast.makeText(this, "Maps deleted", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}