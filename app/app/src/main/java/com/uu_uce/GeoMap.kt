package com.uu_uce

import android.app.AlertDialog
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.uu_uce.allpins.PinData
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.allpins.parsePins
import com.uu_uce.misc.ListenableBoolean
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.openImageView
import com.uu_uce.services.*
import com.uu_uce.shapefiles.*
import com.uu_uce.views.DragStatus
import com.uu_uce.views.pinsUpdated
import kotlinx.android.synthetic.main.activity_geo_map.*
import org.jetbrains.annotations.TestOnly
import java.io.File

var needsReload = ListenableBoolean()
var testing = false

/**
 * Main activity in which the map and menu are displayed.
 * @property[pinViewModel] the ViewModel through which the pin database can be accessed.
 * @property[screenDim] the dimensions of the screen.
 * @property[statusBarHeight] the height of the statusbar.
 * @property[resourceId] statusbar id.
 * @property[started] variable representing if the GeoMap was successfully started.
 * @property[offline] variable representing if there is a server known to the app.
 * @property[sharedPref] shared preferences where app settings are saved.
 * @property[popupWindow] the current popup window.
 * @property[progressBar] the progressBar for showing the map downloading progress.
 * @property[polyStyles] the styles for drawing polygons
 * @property[lineStyles] the styles for drawing lines
 * @constructor the GeoMap activity.
 */
class GeoMap : AppCompatActivity() {
    private lateinit var pinViewModel: PinViewModel
    private var screenDim = Point(0,0)
    private var statusBarHeight = 0
    private var resourceId = 0
    private var started = false
    private var offline = false

    private lateinit var sharedPref : SharedPreferences

    // Popup for showing download progress
    private var popupWindow: PopupWindow? = null
    private lateinit var progressBar : ProgressBar

    private var polyStyles: List<PolyStyle> = listOf()
    private var lineStyles: List<LineStyle> = listOf()

    /**
     * When this activity is created set some settings, and check if maps are present.
     */
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

        // Check whether a server is known
        offline = sharedPref.getString("com.uu_uce.SERVER_IP", "") == ""

        // Check whether maps are present
        val mapsPresent = File(getExternalFilesDir(null)?.path + File.separator + mapsFolderName).exists()

        // Alert that notifies the user that maps need to be downloaded TODO: remove when streaming is implemented
        if(!mapsPresent && !offline){
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setIcon(R.drawable.ic_sprite_question)
                .setTitle(getString(R.string.geomap_download_warning_head))
                .setMessage(getString(R.string.geomap_download_warning_body))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    queryServer("map", this){ mapid -> downloadMaps(mapid) }
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

    /**
     * Initialize everything to do with the GeoMap.
     */
    private fun start(){
        setContentView(R.layout.activity_geo_map)
        customMap.setActivity(this)

        // Set pin size
        customMap.pinSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)
        customMap.resizePins()

        // Start database and get pins from database
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        this.customMap.setPinViewModel(pinViewModel)
        this.customMap.setLifeCycleOwner(this)
        this.customMap.setPins(pinViewModel.allPinData)

        // Get newest database from server and update pins (when not testing)
        if(!testing && !offline){
            queryServer("pin", this) { s -> updateDatabase(s) }
        }

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
                putString("com.uu_uce.WEBTOKEN", "")
                apply()
            }
            customMap.startLogin()
            finish()
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

        // Set legend button
        legend_button.setOnClickListener{
            openLegend()
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

    /**
     * Intercept touch to move down the menu.
     */
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

    /**
     * Intercept backpress to move down the menu.
     */
    override fun onBackPressed() {
        //move the menu down when it's up, otherwise close the current popup
        if (menu.dragStatus != DragStatus.Down) {
            menu.down()
            return
        }
        else {
            moveTaskToBack(true)
        }
    }

    /**
     * Restart activity if necessary, or apply changes.
     */
    override fun onResume() {
        // Get desired theme
        if(needsRestart){
            // Restart activity
            val intent = intent
            finish()
            startActivity(intent)
            needsRestart = false
        }
        if(started) {
            val newSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)
            customMap.reloadPins()
            if (newSize != customMap.pinSize) {
                customMap.pinSize = newSize
                customMap.resizePins()
            }

            needsReload.setListener(object : ListenableBoolean.ChangeListener {
                override fun onChange() {
                    if (needsReload.getValue()) {
                        loadMap()
                    }
                }
            })

            customMap.redrawMap()
        }
        super.onResume()
    }

    /**
     * Initialize everything to do with the screens/menus height.
     */
    private fun initMenu(){
        if(customMap.getLayerCount() > 0){
            menu.setScreenHeight(customMap.height, dragBar.height, toggle_layer_scroll.height, lower_menu_layout.height)
        }
        else{
            menu.setScreenHeight(customMap.height, dragBar.height, 0, lower_menu_layout.height)
        }
        val heightlineY = menu.downY - heightline_diff_text.height - heightline_diff_text.marginBottom - heightline_diff_text.paddingBottom
        val centerY = menu.downY - center_button.height - center_button.marginBottom - center_button.paddingBottom
        val scaleY = heightlineY - scaleWidget.height - scaleWidget.marginBottom - scaleWidget.paddingBottom
        val legendY = centerY - legend_button.height - legend_button.marginBottom - legend_button.paddingBottom
        heightline_diff_text.y = heightlineY
        center_button.y = centerY
        scaleWidget.y = scaleY
        legend_button.y = legendY
    }

    /**
     * Respond to permission request result
     * @param[requestCode] type of request as integer code (see PermissionServices.kt).
     * @param[permissions] the permissions that were requested.
     * @param[grantResults] the results for each permission.
     */
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

    /**
     * Loads all present layers of the map from storage.
     */
    private fun loadMap(){
        (Display::getSize)(windowManager.defaultDisplay, screenDim)
        val longest = maxOf(screenDim.x, screenDim.y)
        val size = (longest*menu.buttonPercent).toInt()

        customMap.removeLayers(toggle_layer_layout)

        Logger.log(LogType.Event, "GeoMap", "loading maps")

        val mydir = File(getExternalFilesDir(null)?.path + "/Maps/")

        try{readPolyStyles(mydir)}
        catch(e: Exception){Logger.error("GeoMap", "no polystyle file available: "+ e.message)}
        try{readLineStyles(mydir)}
        catch(e: Exception){Logger.error("GeoMap", "no linestyle file available: "+ e.message)}
        var layerName = "Polygons"
        val polygons = File(mydir, layerName)
        try {
            val layerType = LayerType.Water
            customMap.addLayer(
                layerType,
                PolygonReader(polygons, layerType, true, polyStyles),
                toggle_layer_layout,
                0.5f,
                size,
                layerName
            )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $polygons")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $polygons.\nError: " + e.message)
        }
        layerName = "Heightlines"
        val heightlines = File(mydir, layerName)
        try {
            val layerType = LayerType.Height
            customMap.addLayer(
                LayerType.Height,
                HeightLineReader(heightlines,layerType),
                toggle_layer_layout,
                Float.MAX_VALUE,
                size,
                layerName
            )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $heightlines")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $heightlines.\nError: " + e.message)
        }
        layerName = "Coloredlines"
        val coloredLines = File(mydir, layerName)
        try {
            val layerType = LayerType.Lines
            customMap.addLayer(
                LayerType.Lines,
                ColoredLineReader(coloredLines,lineStyles,layerType),
                toggle_layer_layout,
                0.5f,
                size,
                layerName
            )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $coloredLines")
        } catch (e: Exception) {
            Logger.error("GeoMap", "Could not load layer at $coloredLines.\nError: " + e.message)
        }

        //create camera based on layers
        customMap.initializeCamera()
        customMap.post {
            customMap.setCameraWAspect()
            customMap.redrawMap()
        }

        //more menu initialization which needs its width/height
        scaleWidget.post {
            menu.post {
                initMenu()
            }
        }

        //customMap.setCameraWAspect()
        needsReload.setValue(false)
        customMap.redrawMap()
    }

    /**
     * Starts a download for the maps.
     * @param[mapid] string containing the map id.
     */
    private fun downloadMaps(mapid : String) {
        if(mapid == "") {
            runOnUiThread{
                popupWindow?.dismiss()
                Toast.makeText(
                    this,
                    getString(R.string.settings_queryfail),
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        openProgressPopup(window.decorView.rootView)
        val maps = listOf(getExternalFilesDir(null)?.path + File.separator + mapid)
        updateFiles(
            maps,
            this,
            { success ->
                if (success) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.zip_download_completed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    val unzipResult = unpackZip(maps.first()) { progress ->
                        runOnUiThread {
                            progressBar.progress = progress
                        }
                    }
                    runOnUiThread {
                        if (unzipResult) Toast.makeText(
                            this,
                            getString(R.string.zip_unpack_completed),
                            Toast.LENGTH_LONG
                        ).show()
                        else Toast.makeText(
                            this,
                            getString(R.string.zip_unpacking_failed),
                            Toast.LENGTH_LONG
                        ).show()
                        popupWindow?.dismiss()
                        start()
                    }
                }
                else {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_LONG)
                            .show()
                        popupWindow?.dismiss()
                        start()
                    }
                }
            },
            { progress -> runOnUiThread { progressBar.progress = progress } }
        )
    }

    /**
     * Starts a download for the database json file.
     * @param[pinDatabaseFile] the id of a database file.
     */
    private fun updateDatabase(pinDatabaseFile: String) {
        if(pinDatabaseFile == ""){
            runOnUiThread {
                Toast.makeText(
                    this,
                    getString(R.string.settings_queryfail),
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        updateFiles(
            listOf(getExternalFilesDir(null)?.path + File.separator + pinDatabaseFile),
            this,
            { success ->
                if (success) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.settings_pins_downloaded),
                            Toast.LENGTH_LONG
                        ).show()
                        pinViewModel.updatePins(parsePins(File(getExternalFilesDir(null)?.path + File.separator + pinDatabaseFile))) {
                            pinsUpdated.setValue(true)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.geomap_pins_download_instructions),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    /**
     * read all polygon styles
     */
    private fun readPolyStyles(dir: File) {
        val file = File(dir, "styles")
        val reader = FileReader(file)

        val nrStyles = reader.readULong()
        polyStyles = List(nrStyles.toInt()) {
            //a style consists of a color in bgr format (for some reason)
            //there is also an outline, which should be removed from the preprocessor eventually
            reader.readUByte() //outline
            val b = reader.readUByte()
            val g = reader.readUByte()
            val r = reader.readUByte()

            PolyStyle(floatArrayOf(
                r.toFloat()/255,
                g.toFloat()/255,
                b.toFloat()/255
            ))
        }
    }

    /**
     * read all line styles
     */
    private fun readLineStyles(dir: File){
        val file = File(dir, "linestyles")
        val reader = FileReader(file)

        val nrStyles = reader.readULong()
        lineStyles = List(nrStyles.toInt()) {
            //a linestyle has a linewidth and a color
            //line width is currently not used
            val width = reader.readUByte()
            val b = reader.readUByte()
            val g = reader.readUByte()
            val r = reader.readUByte()

            LineStyle(width.toFloat(), floatArrayOf(
                r.toFloat()/255,
                g.toFloat()/255,
                b.toFloat()/255
            ))
        }
    }

    /**
     * Opens a popup window to show a progressbar in.
     * @param[currentView] the view that the popup should be opened in.
     */
    private fun openProgressPopup(currentView: View){
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

    /**
     * Opens an ImageViewer with the legend of the map in it.
     */
    private fun openLegend(){
        val uri = getExternalFilesDir(null)?.path + File.separator + mapsFolderName + File.separator + legendName

        openImageView(this, Uri.parse(uri), "Legend")
    }

    @TestOnly
    /**
     * Sets the pin database to the supplied data, used to make the database tests run on constant data.
     * @param[newPinData] the new data that the current database should be replaced with.
     */
    fun setPinData(newPinData : List<PinData>) {
        pinViewModel.setPins(newPinData)
    }

    @TestOnly
    /**
     * Gets the location of the first pin in the pins map.
     * @return the location of the first pin.
     */
    fun getPinLocation() : Pair<Float, Float> {
        return customMap.getPinLocation()
    }
}
