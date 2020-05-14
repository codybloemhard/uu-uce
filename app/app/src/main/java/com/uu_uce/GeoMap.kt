package com.uu_uce

import android.app.AlertDialog
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.uu_uce.allpins.PinData
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.misc.ListenableBoolean
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.*
import com.uu_uce.shapefiles.HeightLineReader
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.PolygonReader
import com.uu_uce.views.DragStatus
import kotlinx.android.synthetic.main.activity_geo_map.*
import org.jetbrains.annotations.TestOnly
import java.io.File

var needsReload = ListenableBoolean()

//main activity in which the map and menu are displayed
class GeoMap : AppCompatActivity() {
    private lateinit var pinViewModel: PinViewModel
    private var screenDim = Point(0,0)
    private var statusBarHeight = 0
    private var resourceId = 0
    private var started = false

    private lateinit var sharedPref : SharedPreferences

    // Popup for showing download progress
    private var popupWindow: PopupWindow? = null
    private lateinit var progressBar : ProgressBar
    private var downloadResult = false

    // TODO: Remove temporary hardcoded map information
    private val mapsName = "maps.zip"
    private lateinit var maps : List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.setTagEnabled("CustomMap", false)
        Logger.setTagEnabled("LocationServices", false)
        Logger.setTagEnabled("Pin", false)
        Logger.setTagEnabled("DrawOverlay", false)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else {
            window.statusBarColor = Color.BLACK// set status background white
        }

        super.onCreate(savedInstanceState)

        maps = listOf(getExternalFilesDir(null)?.path + File.separator + mapsName)

        // TODO: remove when streaming is implemented
        if(!File(getExternalFilesDir(null)?.path + File.separator + "Maps").exists()){
            AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_sprite_question)
                .setTitle(getString(R.string.geomap_download_warning_head))
                .setMessage(getString(R.string.geomap_download_warning_body))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    openProgressPopup(window.decorView.rootView)
                    downloadResult = updateFiles(
                        maps,
                        this,
                        {
                            if(downloadResult){
                                runOnUiThread{
                                    Toast.makeText(this, "Download completed, unpacking", Toast.LENGTH_LONG).show()
                                }
                                val unzipResult = unpackZip(maps.first()) { progress -> runOnUiThread { progressBar.progress = progress } }
                                runOnUiThread{
                                    if(unzipResult) Toast.makeText(this, "Unpacking completed", Toast.LENGTH_LONG).show()
                                    else Toast.makeText(this, "Unpacking failed", Toast.LENGTH_LONG).show()
                                    popupWindow?.dismiss()
                                    start()
                                }
                            }
                            else{
                                runOnUiThread{
                                    Toast.makeText(this, "Download failed", Toast.LENGTH_LONG).show()
                                    popupWindow?.dismiss()
                                    start()
                                }
                            }
                        },
                        { progress -> runOnUiThread { progressBar.progress = progress } }
                    )
                }
                .setNegativeButton(getString(R.string.negative_button_text)) { _, _ ->
                    start()
                    Toast.makeText(this, getString(R.string.geomap_maps_download_instructions), Toast.LENGTH_LONG).show()
                }
                .show()
        }
        else{
            start()
        }
    }

    private fun start(){
        setContentView(R.layout.activity_geo_map)

        // Get preferences
        sharedPref = getDefaultSharedPreferences(this)

        // Set settings
        customMap.debug = sharedPref.getBoolean("com.uu_uce.DEBUG", false)
        customMap.pinSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)
        customMap.hardwareAccelerated = sharedPref.getBoolean("com.uu_uce.HARDWARE", false)

        // TODO: Remove when releasing
        with(sharedPref.edit()) {
            putInt("com.uu_uce.USER_POINTS", 0)
            apply()
        }

        // Start database and get pins from database
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        this.customMap.setPinViewModel(pinViewModel)
        this.customMap.setLifeCycleOwner(this)
        this.customMap.setPins(pinViewModel.allPinData)

        // Get statusbar height
        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        // Initialize menu
        allpins_button.setOnClickListener   { customMap.startAllPins() }
        fieldbook_button.setOnClickListener { customMap.startFieldBook() }
        settings_button.setOnClickListener  { customMap.startSettings() }
        profile_button.setOnClickListener   { customMap.startProfile() }
        logout_button.setOnClickListener    {
            with(sharedPref.edit()) {
                putString("com.uu_uce.USERNAME", "")
                putString("com.uu_uce.PASSWORD", "")
                apply()
            }
            customMap.startLogin()
        }

        dragBar.clickAction      = {menu.dragButtonTap()}
        dragBar.dragAction       = { dx, dy -> menu.drag(dx,dy)}
        dragBar.dragEndAction    = { dx, dy -> menu.snap(dx, dy)}

        //add layers to map
        loadMap()

        customMap.tryStartLocServices(this)

        // Set center on location button functionality
        center_button.setOnClickListener{
            if(customMap.locationAvailable){
                customMap.zoomToDevice()
                customMap.setCenterPos()
            }
            else{
                Toast.makeText(this, "Location not avaiable", Toast.LENGTH_LONG).show()
                getPermissions(this, LocationServices.permissionsNeeded, LOCATION_REQUEST)
            }
        }

        needsReload.setListener(object : ListenableBoolean.ChangeListener {
            override fun onChange() {
                if(needsReload.getValue()){
                    loadMap()
                }
            }
        })

        started = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        //move the menu down when the map is tapped
        //this needs to be done in dispatch so the touch can't be consumed by other views
        if(menu.dragStatus != DragStatus.Down &&
            ev.action == MotionEvent.ACTION_DOWN &&
            !(ev.x > menu.x && ev.x < menu.x + menu.width && ev.y-statusBarHeight > menu.y && ev.y-statusBarHeight < menu.y + menu.height)){
            menu.down()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        //move the menu down when it's up, otherwise close the current popup
        if(menu.dragStatus != DragStatus.Down){
            menu.down()
            return
        }
        if(customMap.activePopup != null){
            customMap.activePopup!!.dismiss()
        }
        else{
            moveTaskToBack(true)
        }
    }

    override fun onResume() {
        if(needsReload.getValue()) loadMap()
        if(started){
            super.onResume()
            customMap.debug = sharedPref.getBoolean("com.uu_uce.DEBUG", false)
            customMap.pinSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)
            customMap.hardwareAccelerated = sharedPref.getBoolean("com.uu_uce.HARDWARE", false)
            customMap.setPins(pinViewModel.allPinData)
            customMap.redrawMap()
        }
        super.onResume()
    }

    private fun initMenu(){
        if(customMap.getLayerCount() > 0){
            menu.setScreenHeight(customMap.height, dragBar.height, toggle_layer_scroll.height, lower_menu_layout.height)
        }
        else{
            menu.setScreenHeight(customMap.height, dragBar.height, 0, lower_menu_layout.height)
        }
    }

    // Respond to permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Logger.log(LogType.Info,"GeoMap", "Permissions granted")
                    customMap.locationAvailable = true
                    customMap.startLocServices()
                }
                else{
                    Logger.log(LogType.Info,"GeoMap", "Permissions were not granted")
                    customMap.locationAvailable = false
                }
            }
        }
    }

    private fun loadMap(){
        (Display::getSize)(windowManager.defaultDisplay, screenDim)
        val longest = maxOf(screenDim.x, screenDim.y)
        val size = (longest*menu.buttonPercent).toInt()

        customMap.removeLayers(toggle_layer_layout)

        val mydir = File(getExternalFilesDir(null)?.path + "/Maps/")
        try {
            val heightlines = File(mydir, "Heightlines")
            customMap.addLayer(
                LayerType.Height,
                HeightLineReader(heightlines),
                toggle_layer_layout,
                size,
                true
            )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $heightlines")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        }
        try {
            val polygons = File(mydir, "Polygons")
            customMap.addLayer(
                LayerType.Water,
                PolygonReader(polygons),
                toggle_layer_layout,
                size,
                false
            )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $mydir")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        }

        //create camera based on layers
        customMap.initializeCamera()

        //more menu initialization which needs its width/height
        menu.post{
            initMenu()
        }

        customMap.setCameraWAspect()
        needsReload.setValue(false)
        customMap.redrawMap()
    }

    private fun openProgressPopup(currentView: View){
        val layoutInflater = layoutInflater

        // Build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.progress_popup, geoMapLayout, false)

        popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        progressBar = customView.findViewById(R.id.progress_popup_progressBar)

        // Open popup
        popupWindow?.showAtLocation(currentView, Gravity.CENTER, 0, 0)
    }

    @TestOnly
    fun setPinData(newPinData : List<PinData>) {
        pinViewModel.setPins(newPinData)
    }

    @TestOnly
    fun getPinLocation() : Pair<Float, Float> {
        return customMap.getPinLocation()
    }
}
