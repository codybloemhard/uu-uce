package com.uu_uce

import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.pins.PinContent
import com.uu_uce.services.dirSize
import com.uu_uce.services.unpackZip
import com.uu_uce.services.updateFiles
import com.uu_uce.services.writableSize
import com.uu_uce.ui.createTopbar
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.File


// Default settings
const val defaultPinSize = 60
var needsRestart = false

class Settings : AppCompatActivity() {
    // private variables
    private val minPinSize = 10
    private val maxPinSize = 200

    // TODO: Remove temporary hardcoded map information
    private val mapsName = "maps.zip"
    private val mapsFolderName = "Maps"
    private val contentFolderName = "PinContent"
    private lateinit var maps : List<String>
    private lateinit var mapsDir : String
    private lateinit var contentDir : String

    private lateinit var sharedPref : SharedPreferences
    private lateinit var pinViewModel: PinViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getDefaultSharedPreferences(this)
        val darkMode = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        // Set desired theme
        if(darkMode) setTheme(R.style.DarkTheme)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !darkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else if(!darkMode){
            window.statusBarColor = Color.BLACK// set status background white
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        mapsDir = getExternalFilesDir(null)?.path + File.separator + mapsFolderName
        contentDir = getExternalFilesDir(null)?.path + File.separator + contentFolderName

        maps = listOf(getExternalFilesDir(null)?.path + File.separator + mapsName)

        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)

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

        // Network downloading
        val curNetworkDownloading = sharedPref.getBoolean("com.uu_uce.NETWORK_DOWNLOADING", false)
        networkdownload_switch.isChecked = curNetworkDownloading
        networkdownload_switch.setOnClickListener{
            if(networkdownload_switch.isChecked){
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_sprite_warning)
                    .setTitle(getString(R.string.settings_mobiledata_warning_head))
                    .setMessage(getString(R.string.settings_mobiledata_warning_body))
                    .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                        with(sharedPref.edit()) {
                            putBoolean(
                                "com.uu_uce.NETWORK_DOWNLOADING",
                                true
                            )
                            apply()
                        }
                    }
                    .setNegativeButton(getString(R.string.negative_button_text)) { _, _ -> networkdownload_switch.isChecked = false }
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

        // Darkmode switch
        darktheme_switch.isChecked = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        darktheme_switch.setOnClickListener {
            with(sharedPref.edit()) {
                putBoolean(
                    "com.uu_uce.DARKMODE",
                    darktheme_switch.isChecked
                )
                apply()
            }

            needsRestart = !needsRestart

            // Restart activity
            val intent = intent
            finish()
            startActivity(intent)
        }

        // Download maps
        fun downloadMaps(){
            maps_downloading_progress.visibility = View.VISIBLE
            updateFiles(
                maps,
                this,
                {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.settings_zip_download_complete), Toast.LENGTH_LONG)
                            .show()
                    }
                    val result = unpackZip(maps.first()) { progress ->
                        runOnUiThread {
                            maps_downloading_progress.progress = progress
                        }
                    }
                    runOnUiThread {
                        if(result) Toast.makeText(this, getString(R.string.settings_zip_unpacked), Toast.LENGTH_LONG).show()
                        else Toast.makeText(this, getString(R.string.settings_zip_not_unpacked), Toast.LENGTH_LONG).show()
                        maps_downloading_progress.visibility = View.INVISIBLE
                        needsReload.setValue(true)
                        delete_maps_button.visibility = View.VISIBLE
                        maps_storage_size.text = writableSize(dirSize(File(mapsDir)))
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
                    .setTitle(getString(R.string.settings_redownload_map_head))
                    .setMessage(getString(R.string.settings_redownload_map_body))
                    .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                        downloadMaps()
                    }
                    .setNegativeButton(getString(R.string.negative_button_text), null)
                    .show()
            }
        }

        // Maps storage
        delete_maps_button.visibility =
            if(File(mapsDir).exists()){
            View.VISIBLE
        }
        else{
            View.INVISIBLE
        }

        maps_storage_size.text = writableSize(dirSize(File(mapsDir)))

        delete_maps_button.setOnClickListener {
            AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_sprite_warning)
                .setTitle(getString(R.string.settings_delete_maps_warning_head))
                .setMessage(getString(R.string.settings_delete_maps_warning_body))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    File(mapsDir).deleteRecursively()
                    needsReload.setValue(true)
                    delete_maps_button.visibility = View.INVISIBLE
                    maps_storage_size.text = writableSize(dirSize(File(mapsDir)))
                    Toast.makeText(this, getString(R.string.settings_map_deleted_text), Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(getString(R.string.negative_button_text), null)
                .show()
        }

        // Download pin content
        download_content_button.setOnClickListener{
            val list = mutableListOf<String>()

            pinViewModel.getContent(list){
                val pathList = mutableListOf<String>()

                for (data in list){
                    for (block in PinContent(data).contentBlocks){
                        for (path in block.getFilePaths()){
                            pathList.add(path)
                        }
                    }
                }

                content_downloading_progress.visibility = View.VISIBLE

                updateFiles(
                    pathList,
                    this,
                    {
                        runOnUiThread {
                            Toast.makeText(this, getString(R.string.settings_download_complete), Toast.LENGTH_LONG).show()
                            content_downloading_progress.visibility = View.INVISIBLE
                            content_storage_size.text = writableSize(dirSize(File(contentDir)))
                            delete_content_button.visibility = View.VISIBLE
                        }
                    },
                    {
                            progress -> runOnUiThread { content_downloading_progress.progress = progress }
                    }
                )
            }
        }

        // Content storage
        delete_content_button.visibility =
            if(File(contentDir).exists()){
                View.VISIBLE
            }
            else{
                View.INVISIBLE
            }

        content_storage_size.text = writableSize(dirSize(File(contentDir)))

        delete_content_button.setOnClickListener {
            AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_sprite_warning)
                .setTitle(getString(R.string.settings_delete_content_warning_head))
                .setMessage(getString(R.string.settings_delete_content_warning_body))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    File(contentDir).deleteRecursively()
                    needsReload.setValue(true)
                    delete_content_button.visibility = View.INVISIBLE
                    content_storage_size.text = writableSize(dirSize(File(contentDir)))
                    Toast.makeText(this, getString(R.string.settings_content_deleted_text), Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(getString(R.string.negative_button_text), null)
                .show()
        }
    }
}