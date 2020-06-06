package com.uu_uce

import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.allpins.parsePins
import com.uu_uce.pins.PinContent
import com.uu_uce.services.dirSize
import com.uu_uce.services.unpackZip
import com.uu_uce.services.updateFiles
import com.uu_uce.services.writableSize
import com.uu_uce.ui.createTopbar
import com.uu_uce.views.pinsUpdated
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.File
import kotlin.math.max
import kotlin.math.min

// Default settings
const val defaultPinSize = 60
const val defaultUnlockRange = 100
var needsRestart = false
const val mapsName = "2e9e5736-a18f-402a-8843-8124d3b6248d.zip"
const val mapsFolderName = "Maps"
const val contentFolderName = "PinContent"
const val pinDatabaseFile = "4da6aae3-5287-45f2-985f-01fdc27a3fbf.json"

class Settings : AppCompatActivity() {
    // private variables
    private val minPinSize = 10
    private val maxPinSize = 200
    private val minRange = 10
    private val maxRange = 200

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
        pinsize_numberview.setText(curSize.toString())

        pinsize_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the pinSize
                pinsize_numberview.setText((seekBar.progress + minPinSize).toString())
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

        pinsize_numberview.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                val progress = min(max(pinsize_numberview.text.toString().toInt(), minPinSize), maxPinSize)
                pinsize_seekbar.progress = progress - minPinSize
                pinsize_numberview.setText(progress.toString())
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.PIN_SIZE", progress)
                    apply()
                }
                return@OnKeyListener true
            }
            false
        })

        // Unlock range
        val curRange = sharedPref.getInt("com.uu_uce.UNLOCKRANGE", defaultUnlockRange)
        unlockrange_seekbar.max = maxRange - minRange
        unlockrange_seekbar.progress = curRange - minRange
        unlockrange_numberview.setText(curRange.toString())

        unlockrange_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the pinSize
                unlockrange_numberview.setText((seekBar.progress + minRange).toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val range = seekBar.progress + minRange
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.UNLOCKRANGE", range)
                    apply()
                }
            }
        })

        unlockrange_numberview.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                val progress = min(max(unlockrange_numberview.text.toString().toInt(), minRange), maxRange)
                unlockrange_seekbar.progress = progress - minRange
                unlockrange_numberview.setText(progress.toString())
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.UNLOCKRANGE", progress)
                    apply()
                }
                return@OnKeyListener true
            }
            false
        })

        // Network downloading
        val curNetworkDownloading = sharedPref.getBoolean("com.uu_uce.NETWORK_DOWNLOADING", false)
        networkdownload_switch.isChecked = curNetworkDownloading
        networkdownload_switch.setOnClickListener{
            if(networkdownload_switch.isChecked){
                AlertDialog.Builder(this, R.style.AlertDialogStyle)
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
            if (!File(getExternalFilesDir(null)?.path + File.separator + mapsFolderName).exists()) {
                downloadMaps()
            }
            else{
                AlertDialog.Builder(this, R.style.AlertDialogStyle)
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
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
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
                    for (block in PinContent(data, this, false).contentBlocks){
                        for (path in block.getFilePath()){
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
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
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

        // Download pins
        download_pins_button.setOnClickListener{
            pins_downloading_progress.visibility = View.VISIBLE

            updateFiles(
                listOf(getExternalFilesDir(null)?.path + File.separator + pinDatabaseFile),
                this,
                {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.settings_download_complete), Toast.LENGTH_LONG).show()
                        pins_downloading_progress.visibility = View.INVISIBLE
                        pinViewModel.updatePins(parsePins(File(getExternalFilesDir(null)?.path + File.separator + pinDatabaseFile))){
                            pinsUpdated.setValue(true)
                        }
                    }
                },
                {
                    progress -> runOnUiThread { pins_downloading_progress.progress = progress }
                }
            )
        }

        /*databasetest.setOnClickListener{
            pinViewModel.updatePins(parsePins(File(getExternalFilesDir(null)?.path + File.separator + "database.json"))){
                pinsUpdated.setValue(true)
            }
        }

        databasetest2.setOnClickListener{
            pinViewModel.updatePins(parsePins(File(getExternalFilesDir(null)?.path + File.separator + "database (1).json"))){
                pinsUpdated.setValue(true)
            }
        }*/
    }
}