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
import com.uu_uce.shapefiles.*
import com.uu_uce.views.DragStatus
import kotlinx.android.synthetic.main.activity_geo_map.*
import org.jetbrains.annotations.TestOnly
import java.io.File

var needsReload = ListenableBoolean()

const val linewidth = 1f
const val outlinewidth = 5f

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

    private lateinit var maps : List<String>

    private var styles: List<Style> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set logger settings
        Logger.setTagEnabled("CustomMap", false)
        Logger.setTagEnabled("LocationServices", false)
        Logger.setTagEnabled("Pin", false)
        Logger.setTagEnabled("DrawOverlay", false)

        sharedPref = getDefaultSharedPreferences(this)

        // Set desired theme
        val darkMode = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        if (darkMode) setTheme(R.style.DarkTheme)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !darkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else if(!darkMode){
            window.statusBarColor = Color.BLACK// set status background white
        }

        super.onCreate(savedInstanceState)

        maps = listOf(getExternalFilesDir(null)?.path + File.separator + mapsName)

        // Alert that notifies the user that maps need to be downloaded TODO: remove when streaming is implemented
        if(!File(getExternalFilesDir(null)?.path + File.separator + "Maps").exists()){
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
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
                                    Toast.makeText(this, getString(R.string.zip_download_completed), Toast.LENGTH_LONG).show()
                                }
                                val unzipResult = unpackZip(maps.first()) { progress -> runOnUiThread { progressBar.progress = progress } }
                                runOnUiThread{
                                    if(unzipResult) Toast.makeText(this, getString(R.string.zip_unpack_completed), Toast.LENGTH_LONG).show()
                                    else Toast.makeText(this, getString(R.string.zip_unpacking_failed), Toast.LENGTH_LONG).show()
                                    popupWindow?.dismiss()
                                    start()
                                }
                            }
                            else{
                                runOnUiThread{
                                    Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_LONG).show()
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
        customMap.setActivity(this)

        // Set settings
        customMap.pinSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)
        customMap.resizePins()

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

        // Set menu controls
        dragBar.clickAction      = {menu.dragButtonTap()}
        dragBar.dragAction       = { dx, dy -> menu.drag(dx,dy)}
        dragBar.dragEndAction    = { dx, dy -> menu.snap(dx, dy)}

        // Add layers to map
        loadMap()

        customMap.tryStartLocServices(this)

        // Set center on location button functionality
        center_button.setOnClickListener{
            if(customMap.locationAvailable){
                customMap.zoomToDevice()
                customMap.setCenterPos()
            }
            else{
                Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_LONG).show()
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
        
        customMap.post {
            scaleWidget.post {
                scaleWidget.setScreenWidth(customMap.width)
            }
        }

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
        if (menu.dragStatus != DragStatus.Down) {
            menu.down()
            return
        }
        if (customMap.activePopup != null) {
            customMap.activePopup!!.dismiss()
        }
        else {
            moveTaskToBack(true)
        }
    }

    override fun onResume() {
        // Get desired theme
        if(needsRestart){
            // Restart activity
            val intent = intent
            finish()
            startActivity(intent)
            needsRestart = false
        }
        if(needsReload.getValue()) loadMap()
        if(started){
            customMap.pinSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)
            customMap.resizePins()
            customMap.redrawMap()
        }
        super.onResume()
    }

    private fun initMenuAndScales(){
        if(customMap.getLayerCount() > 0){
            menu.setScreenHeight(customMap.height, dragBar.height, toggle_layer_scroll.height, lower_menu_layout.height)
        }
        else{
            menu.setScreenHeight(customMap.height, dragBar.height, 0, lower_menu_layout.height)
        }
        val heightlineY = menu.downY - heightline_diff_text.height - heightline_diff_text.paddingBottom - heightline_diff_text.paddingBottom
        val scaleY = heightlineY - scaleWidget.height - scaleWidget.paddingBottom - scaleWidget.paddingBottom
        heightline_diff_text.y = heightlineY
        scaleWidget.y = scaleY
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

        try{readStyles(mydir)}
        catch(e: Exception){Logger.error("GeoMap", "no style file available: "+ e.message)}
        //try {
            val polygons = File(mydir, "Polygons")
            customMap.addLayer(
                LayerType.Water,
                PolygonReader(polygons, true, styles),
                toggle_layer_layout,
                0.5f,
                size
            )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $mydir")
        //}catch(e: Exception){
        //    Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        //}
        //try {
        val heightlines = File(mydir, "Heightlines")
        customMap.addLayer(
            LayerType.Height,
            HeightLineReader(heightlines),
            toggle_layer_layout,
            Float.MAX_VALUE,
            size
        )
        Logger.log(LogType.Info, "GeoMap", "Loaded layer at $heightlines")

        //}catch(e: Exception){
        //    Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        //}

        //create camera based on layers
        customMap.initializeCamera()

        //more menu initialization which needs its width/height
        scaleWidget.post {
            menu.post {
                initMenuAndScales()
            }
        }

        //customMap.setCameraWAspect()
        needsReload.setValue(false)
        customMap.redrawMap()
    }
    private fun readStyles(dir: File){
        val file = File(dir, "styles")
        val reader = FileReader(file)

        val nrStyles = reader.readULong()
        styles = List(nrStyles.toInt()) {
            val outline = reader.readUByte()
            val b = reader.readUByte()
            val g = reader.readUByte()
            val r = reader.readUByte()

            Style(outline.toInt() == 1, floatArrayOf(
                r.toFloat()/255,
                g.toFloat()/255,
                b.toFloat()/255
            ))
        }
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
