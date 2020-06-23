package com.uu_uce.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.opengl.GLES20
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.uu_uce.*
import com.uu_uce.Fieldbook
import com.uu_uce.OpenGL.CustomMapGLRenderer
import com.uu_uce.allpins.PinConversion
import com.uu_uce.allpins.PinData
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.fieldbook.FieldbookViewModel
import com.uu_uce.gestureDetection.*
import com.uu_uce.mapOverlay.Location
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.pointDistance
import com.uu_uce.misc.ListenableBoolean
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.MergedPin
import com.uu_uce.pins.Pin
import com.uu_uce.pins.SinglePin
import com.uu_uce.services.*
import com.uu_uce.shapefiles.*
import kotlinx.android.synthetic.main.activity_geo_map.*
import org.jetbrains.annotations.TestOnly
import kotlin.math.abs
import kotlin.math.pow
import kotlin.system.measureTimeMillis

var pinsUpdated = ListenableBoolean()

/**
 * The view displayed in the app that holds the map.
 * @property[renderer] TODO
 * @property[locationServices] service used to get location updates.
 * @property[locationDeadZone] how much does the location have to change on the screen to warrant a redraw.
 * @property[loc] the most recent known location of the device.
 * @property[lastDrawnLoc] the last location that was drawn on the map.
 * @property[locationAvailable] whether or not the location of the user can be used.
 * @property[locAtCenterPress] user location at the time of pressing the center button, used for testing.
 * @property[activity] the activity the CustomMap is in.
 * @property[pinViewModel] the ViewModel throught which the pin database is accessed.
 * @property[fieldbookViewModel] the ViewModel throught which the fieldbook database is accessed.
 * @property[lfOwner] the LifecycleOwner of the ViewModels.
 * @property[pins] a map from pinId to the corresponding pin object.
 * @property[pinSize] the size of pins on the map, this is changed from settings.
 * @property[pinStatuses] the statuses of all pins in in pins.
 * @property[mergedPins] TODO
 * @property[mergedPinsLock] TODO
 * @property[locSizeFactor] the size of the location marker compared to the pin size.
 * @property[smap] TODO
 * @property[nrLayers] TODO
 * @property[mods] TODO
 * @property[camera] TODO
 * @property[bufferFrames] TODO
 * @property[curBufferFrame] TODO
 * @constructor creates a CustomMap view.
  */
class CustomMap : ViewTouchParent {

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    //gl renderer
    private val renderer: CustomMapGLRenderer

    // Location
    private val locationServices                            = LocationServices()
    private val locationDeadZone    : Float                 = 5f
    private var loc                 : Location              = Location(UTMCoordinate(31, 'N', 0.0f, 0.0f), context)
    private var lastDrawnLoc        : Pair<Float, Float>    = Pair(0f, 0f)
    var locationAvailable           : Boolean               = false
    private var locAtCenterPress    : UTMCoordinate         = UTMCoordinate(31, 'N', 0.0f, 0.0f)

    private lateinit var activity           : Activity
    private var pinViewModel                : PinViewModel? = null
    private var fieldbookViewModel          : FieldbookViewModel? = null
    private lateinit var lfOwner            : LifecycleOwner

    // Pins
    private var pins: MutableMap<String, SinglePin> = mutableMapOf()
    var pinSize: Int
    private var pinStatuses: MutableMap<String, Int> = mutableMapOf()
    private var mergedPins: Pin? = null
    private var mergedPinsLock: Any = Object()

    private var locSizeFactor = 0.5f

    // Map
    private var smap = ShapeMap(this)
    private var nrLayers = 0
    private lateinit var mods : List<Int>
    private lateinit var camera : Camera
    private var bufferFrames = 5
    private var curBufferFrame = 0

    init{
        setEGLContextClientVersion(2)
        debugFlags = DEBUG_CHECK_GL_ERROR // enable log
        preserveEGLContextOnPause = true // default is false

        pinSize = defaultPinSize

        renderer = CustomMapGLRenderer(this)
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY

        // Logger mask settings
        Logger.setTagEnabled("CustomMap", false)
        Logger.setTagEnabled("zoom", false)

        //setup touch events
        addChild(Zoomer(context, ::zoomMap))
        addChild(Scroller(context, ::moveMap, ::flingMap))
        addChild(DoubleTapper(context, ::zoomOutMax))
        addChild(SingleTapper(context as AppCompatActivity, ::tapPin))

        pinsUpdated.setListener(object : ListenableBoolean.ChangeListener {
            override fun onChange() {
                if(pinsUpdated.getValue()){
                    reloadPins()
                }
            }
        })
        // Width and height are not set in the init{} yet
        // We delay calculations that use them by using post
        post{
            setCameraWAspect()
        }
    }

    /**
     * Creates a new camera, to be called after all addLayer calls.
     */
    fun initializeCamera(){
        camera = smap.createCamera()
    }

    /**
     * Add a new layer to the map, and generate a button to toggle it.
     * @param[lt] the type of layer that is being added.
     * @param[chunkGetter] TODO
     * @param[scrollLayout] the layout to which the toggle layer button should be added.
     * @param[zoomCutoff] TODO
     * @param[buttonSize] the size of the toggle layer button that will be added.
     * @param[layerName] the name of the layer taht is being added.
      */
    fun addLayer(lt: LayerType, chunkGetter: ChunkGetter, scrollLayout: LinearLayout?, zoomCutoff: Float = Float.MAX_VALUE, buttonSize: Int = 0, layerName: String){
        smap.addLayer(lt, chunkGetter, zoomCutoff)
        val curLayers = nrLayers
        nrLayers++

        if (buttonSize > 0) {
            val buttonLayout = LinearLayout(context, null).apply {
                layoutParams = ViewGroup.LayoutParams(buttonSize, LinearLayout.LayoutParams.WRAP_CONTENT)
                orientation = LinearLayout.VERTICAL
            }
            val buttonFrame = FrameLayout(context, null).apply{
                layoutParams = ViewGroup.LayoutParams(buttonSize, buttonSize)
            }

            val btnBackground = CardView(context, null).apply{
                val params = FrameLayout.LayoutParams(
                    (buttonSize * 0.9).toInt(),
                    (buttonSize * 0.9).toInt()
                )
                params.gravity = Gravity.CENTER
                layoutParams = params
                setCardBackgroundColor(ResourcesCompat.getColor(context.resources, R.color.BeniukonBronze, null))
                radius = 10f
            }

            val btn = ImageButton(context, null, R.attr.buttonBarButtonStyle).apply {
                setImageResource(R.drawable.ic_sprite_toggle_layer)
                setOnClickListener {
                    toggleLayer(curLayers)
                    if(layerVisible(curLayers)){
                        btnBackground.setCardBackgroundColor(ResourcesCompat.getColor(context.resources, R.color.BeniukonBronze, null))
                    }
                    else{
                        btnBackground.setCardBackgroundColor(ResourcesCompat.getColor(context.resources, R.color.TextGrey, null))
                    }
                }
                elevation = 6.5f
            }

            buttonFrame.addView(btnBackground)
            buttonFrame.addView(btn)

            val layerTitle = TextView(context, null).apply{
                text = layerName
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams =
                    ViewGroup.LayoutParams(buttonSize, LinearLayout.LayoutParams.WRAP_CONTENT)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }

            buttonLayout.addView(buttonFrame)
            buttonLayout.addView(layerTitle)

            scrollLayout!!.addView(buttonLayout)
        }

        mods = smap.getMods()
    }

    /**
     * Remove all layers from map to avoid duplicates, to be called before reloading all layers.
     * @param[scrollLayout] the layout from which all toggle buttons should be removed.
      */
    fun removeLayers(scrollLayout: LinearLayout){
        smap.removeLayers()
        nrLayers = 0
        scrollLayout.removeAllViewsInLayout()
    }

    /**
     * The draw function which draws the map, pins and location.
     * @param[lineProgram] TODO
     * @param[varyingColorProgram] TODO
     * @param[pinProgram] TODO
     * @param[locProgram] TODO
     */
    fun onDrawFrame(lineProgram: Int, varyingColorProgram: Int, pinProgram: Int, locProgram: Int){
        //if both the camera and the map have no updates, don't redraw
        val res = camera.update()
        val viewport = camera.getViewport()
        if(viewport == p2ZeroPair){
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            Logger.error("CustomMap", "Camera could not be initialized")
            return
        }

        val chunkRes = smap.updateChunks(viewport)

        if(res == UpdateResult.NOOP && chunkRes == ChunkUpdateResult.NOTHING){
            //bufferframes to make sure everything is redrawn when returning from a different activity
            curBufferFrame++
            if(curBufferFrame >= bufferFrames) {
                Logger.log(LogType.Event, "CustomMap", "All updates done, no longer redrawing")
                return
            }
        }
        else curBufferFrame = 0

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val (scale,trans) = camera.getScaleTrans()

        val timeDraw = measureTimeMillis {
            // Draw map
            smap.draw(lineProgram, varyingColorProgram, scale, trans)

            if (context is GeoMap) {
                val gm = context as GeoMap
                gm.runOnUiThread {
                    val zoomLevel = smap.getZoomLevel()
                    if (zoomLevel >= 0 && mods.count() > 0) {
                        gm.heightline_diff_text.text =
                            (context as Activity).getString(
                                R.string.geomap_heightline_diff_text,
                                mods[zoomLevel]
                            )

                    } else {
                        val standardValue = 0
                        gm.heightline_diff_text.text =
                            (context as Activity).getString(
                                R.string.geomap_heightline_diff_text,
                                standardValue
                            )
                    }
                }
                gm.scaleWidget.update(viewport, gm)
            }

            // Draw device location
            val deviceScreenLoc = coordToScreen(loc.utm, viewport, width, height)
            val locInScreen =
                deviceScreenLoc.first > 0 && deviceScreenLoc.first < width &&
                        deviceScreenLoc.second > 0 && deviceScreenLoc.second < height
            if(locationAvailable && locInScreen){
                loc.draw(locProgram, scale, trans, pinSize * locSizeFactor, width, height)
                lastDrawnLoc = deviceScreenLoc
            }

            synchronized(mergedPinsLock){
                val disPerPixel = (viewport.second.first - viewport.first.first)/width
                if(mergedPins?.draw(pinProgram, scale, trans, viewport, this, disPerPixel) == true){
                    renderer.pinsChanged = true
                }
            }

            // TODO: drawing route
            //val route = setRoute()
            //route.draw(viewport,this,canvas)
        }
        Logger.log(LogType.Continuous, "CustomMap", "Draw MS: $timeDraw")

        //invalidate so onDraw is called again next frame if necessary
        //if(res == UpdateResult.ANIM || chunkRes == ChunkUpdateResult.LOADING)
            requestRender()
    }

    /**
     * TODO
     */
    fun initPinsGL(){
        synchronized(mergedPinsLock){
            mergedPins?.initGL()
        }
    }

    /**
     * Takes a new loction and calculates new information with it.
     * @param[newLoc] the new loction that was observed.
     */
    private fun updateLoc(newLoc : p2) {
        // Update called by locationManager
        // TODO: move location drawing to an overlaying transparent canvas to avoid unnecessary map drawing
        loc.utm = degreeToUTM(newLoc)

        val viewport = camera.getViewport()
        if(viewport == p2ZeroPair){
            Logger.error("CustomMap", "Camera could not be initialized")
            return
        }

        val screenLoc = coordToScreen(loc.utm, viewport, width, height)

        // Check if redraw is necessary
        val distance = pointDistance(screenLoc, lastDrawnLoc)

        if(distance > locationDeadZone){
            redrawMap()
            Logger.log(LogType.Event,"CustomMap", "Redrawing, distance: $distance")
            return
        }
        Logger.log(LogType.Event,"CustomMap", "No redraw needed, current loc ${loc.utm.east}, ${loc.utm.north}")
    }

    /**
     * Starts polling device location, each location update will call updateLoc.
     */
    fun startLocServices(){
        locationServices.startPollThread(context, 5000, locationDeadZone, ::updateLoc)
    }

    /**
     * Calls startLocServices when all permissions in LocationServices.permissionsNeeded have been granted.
     * @param[activity] the current activity.
     */
    fun tryStartLocServices(activity: Activity){
        val missingPermissions = missingPermissions(activity, LocationServices.permissionsNeeded)
        if(missingPermissions.count() > 0){
            getPermissions(activity, missingPermissions, LOCATION_REQUEST)
        }
        else{
            startLocServices()
            locationAvailable = true
        }
    }

    /**
     * Used to zoom the camera in and out.
     * @param[zoom] the desired zoom level.
     */
    private fun zoomMap(zoom: Float){
        val deltaOne = 1.0f - zoom.coerceIn(0.5f, 1.5f)
        camera.zoomIn(1.0f + deltaOne)
        if(camera.needsInvalidate())
            requestRender()
    }

    /**
     * Used to scroll the camera.
     * @param[dxpx] the amount of pixels the camera should move in the x direction.
     * @param[dypx] the amount of pixels the camera should move in the y direction.
     */
    private fun moveMap(dxpx: Float, dypx: Float){
        val dx = dxpx / width * 2
        val dy = dypx / height * 2
        camera.moveCamera(dx, -dy)
        requestRender()
    }

    /**
     * TODO
     */
    private fun flingMap(){
        camera.flingCamera()
        requestRender()
    }

    /**
     * Zoomout until the maximum zoom level is reached.
     */
    private fun zoomOutMax(){
        camera.zoomOutMax(500.0f)
        if(camera.needsInvalidate())
            requestRender()
    }

    /**
     * Zoom in to the users location (blue dot), at some arbitrary height.
     */
    fun zoomToDevice(){
        camera.startAnimation(Triple(loc.utm.east, loc.utm.north, 0.02f), 1500.0f)
        if(camera.needsInvalidate())
            requestRender()
    }

    /**
     * To be called when the map needs to be redrawn
     */
    fun redrawMap(){
        camera.forceChanged()
        requestRender()
    }

    /**
     * Sets the current database to get the pins from.
     * @param[table] the table to get the pins from.
     */
    fun setPins(table: LiveData<List<PinData>>){
        table.removeObservers(lfOwner)
        table.observe(lfOwner, Observer { pins ->
            // Update the cached copy of the words in the adapter.
            pins?.let { newData -> updatePinStatuses(newData) }
            renderer.pinsChanged = true
        })
    }

    /**
     * Updates pins in memory to a list of new PinData.
     * @param[newPinData] the PinData which the pins in memory should match.
     */
    private fun updatePinStatuses(newPinData: List<PinData>) {
        for(pin in newPinData) {
            if(pinStatuses[pin.pinId] == pin.status){
                // Pin is present and unchanged
                pins[pin.pinId]!!.resize(pinSize)
                continue
            }
            when {
                pinStatuses[pin.pinId] == null -> {
                    // Pin was not yet present
                    val newPin = PinConversion(activity).pinDataToPin(
                        pin,
                        pinViewModel!!
                    )
                    newPin.tryUnlock {
                        Logger.log(LogType.Info, "CustomMap", "Adding pin")
                        synchronized(pins){
                            pins[pin.pinId] = newPin
                        }
                        pinStatuses[newPin.id] = pin.status
                    }
                    newPin.tapAction = {activity: Activity ->
                        (newPin::openContent)(this,activity)
                    }
                    newPin.resize(pinSize)
                    renderer.pinsChanged = true
                }
                pinStatuses[pin.pinId] == 0 -> {
                    // Pin was present and locked (status = 0)
                    val changedPin = pins[pin.pinId]

                    changedPin?.tryUnlock {
                        changedPin.status =1
                        pinStatuses[changedPin.id] = 1
                    }
                }
                else -> {
                    // Pin was present and unlocked (status >= 1)
                    val changedPin = pins[pin.pinId]

                    if (changedPin != null) {
                        changedPin.status = pin.status
                        pinStatuses[changedPin.id] = pin.status
                    }
                }
            }
        }
        synchronized(mergedPinsLock){
            mergedPins = mergePins()
        }
        redrawMap()
    }

    /**
     * Clears pins from memory and does a reload from the database.
     */
    fun reloadPins(){
        synchronized(pins) {
            pins = mutableMapOf()
        }
        pinStatuses = mutableMapOf()
        pinViewModel!!.reloadPins { newPinData -> updatePinStatuses(newPinData) }
        pinsUpdated.setValue(false)
        synchronized(mergedPinsLock){
            mergedPins = mergePins()
        }
    }

    /**
     * Resizes all pins to the value set in settings.
     */
    fun resizePins(){
        mergedPins?.resize(pinSize)
        synchronized(mergedPinsLock){
            mergedPins = mergePins()
        }
    }

    /**
     * TODO
     */
    fun setFieldbook (fieldbook: List<FieldbookEntry>) {
        for (entry in fieldbook) {
            val pin = PinConversion(activity).fieldbookEntryToPin(
                entry,
                fieldbookViewModel!!
            )
            pins[pin.id] = pin.apply{
                resize(pinSize)
                tapAction = { activity: Activity ->
                    (::openFieldbookPopup)(activity,rootView,entry,pin.content.contentBlocks)
                }
            }
        }
        synchronized(mergedPinsLock){
            mergedPins = mergePins()
        }

        redrawMap()
    }

    /**
     * Called when the screen is tapped at tapLocation.
     */
    private fun tapPin(tapLocation : p2, activity : Activity){
        val viewport = camera.getViewport()
        synchronized(mergedPinsLock){
            val disPerPixel = (viewport.second.first - viewport.first.first)/width
            mergedPins?.tap(tapLocation, activity, this, disPerPixel)
        }
    }

    /**
     * Pre-calculate all distances between all pins, and merge them optimally.
     */
    private fun mergePins(): Pin?{
        val finalpins: MutableList<Pin> = pins.values.filter{pin -> pin.status > 0}.toMutableList()
        
        while(finalpins.size > 1) {
            //find two closest pins
            var mini = -1
            var minj = -1
            var mindis2 = Float.MAX_VALUE
            for (i in finalpins.indices) for (j in i+1 until finalpins.size) {
                val dis2 = (finalpins[i].coordinate.east - finalpins[j].coordinate.east).pow(2) + (finalpins[i].coordinate.north - finalpins[j].coordinate.north).pow(2)
                if (dis2 < mindis2) {
                    mini = i
                    minj = j
                    mindis2 = dis2
                }
            }

            //calculate minimum distance in pixels between the two pins
            //depending on if they hit each other horizontally or vertically
            val xdisabs = abs(finalpins[minj].coordinate.east - finalpins[mini].coordinate.east)
            val ydis = finalpins[minj].coordinate.north - finalpins[mini].coordinate.north

            val (top,bot) = if(ydis < 0){
                Pair(finalpins[mini],finalpins[minj])
            }else{
                Pair(finalpins[minj],finalpins[mini])
            }

            val slope = abs(ydis)/xdisabs
            val width = (bot.pinWidth + top.pinWidth)/2
            val slopeSwitch = bot.pinHeight/width
            val pixeldis = if(slope > slopeSwitch) bot.pinHeight else width
            val actualDis = abs(
                if(slope > slopeSwitch) bot.coordinate.north - top.coordinate.north
                else bot.coordinate.east - top.coordinate.east
            )

            //coordinate is average of two pins
            val coordinate = UTMCoordinate(
                bot.coordinate.zone,
                bot.coordinate.letter,
                (bot.coordinate.east + top.coordinate.east)/2,
                (bot.coordinate.north + top.coordinate.north)/2
            )

            val background =
                PinConversion.difficultyToBackground(mergedPinBackground, (context as Activity))
            val icon = PinConversion.typeToIcon(mergedPinIcon, context.resources)

            val newMergedPin = MergedPin(
                finalpins[mini],
                finalpins[minj],
                actualDis,
                pixeldis,
                pinViewModel,
                fieldbookViewModel,
                coordinate,
                background,
                icon,
                pinSize.toFloat()
            )

            finalpins.removeAt(minj)
            finalpins.removeAt(mini)
            finalpins.add(newMergedPin)
        }

        return finalpins.getOrNull(0)
    }

    // TODO: Implement route in fieldbook
    /*fun setRoute() : Route {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Route(
                0,
                FullRoute(
                    "[\n" +
                            "    {\n" +
                            "        \"coordinate\": \"31N46758336N3133680E\",\n" +
                            "        \"localtime\": \"10:19:16\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "        \"coordinate\": \"31N46670000N3130000E\",\n" +
                            "        \"localtime\": \"15:13:42\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "        \"coordinate\": \"31N46655335N3134680E\",\n" +
                            "        \"localtime\": \"18:00:57\"\n" +
                            "    }\n" +
                            "]"
                ),
                LocalDate.now()
            )
        } else {
            TODO("VERSION.SDK_INT < O")
        }
    }*/

    /**
     * Calculates and sets the wAspect for the camera.
     */
    fun setCameraWAspect(){
        camera.wAspect = width.toFloat()/height

        val z = 1.0f / (camera.wAspect)
        camera.maxZoom = maxOf(1.0f,z)
        camera.setZoom(z)
        smap.setzooms(camera.minZoom, camera.maxZoom)
    }

    /**
     * Turn a layer on or off.
     * @param[l] the layer that is to be toggled.
      */
    private fun toggleLayer(l: Int){
        smap.toggleLayer(l)
    }

    /**
     * Gets the current visibility of a layer.
     * @param[l] the layer whose visibility will be returned.
     * @return the visibility of a layer.
     */
    private fun layerVisible(l: Int): Boolean {
        return smap.layerVisible(l)
    }

    /**
     * Sets the PinViewModel.
     * @param[vm] the ViewModel through which the pin database can be accessed.
     */
    fun setPinViewModel(vm: PinViewModel){
        pinViewModel = vm
    }

    /**
     * Sets the FieldbookViewModel.
     * @param[vm] the ViewModel through which the fieldbook database can be accessed.
     */
    fun setFieldbookViewModel(vm: FieldbookViewModel){
        fieldbookViewModel = vm
    }

    /**
     * Sets the LifecycleOwner.
     * @param[lifecycleOwner] the LifecycleOwner of the ViewModel.
     */
    fun setLifeCycleOwner(lifecycleOwner: LifecycleOwner){
        lfOwner = lifecycleOwner
    }

    /**
     * Set the activity that the CustomMap is in.
     * @param[activity] the current activity.
     */
    fun setActivity(activity: Activity){
        this.activity = activity
    }

    /**
     * Open the AllPins activity.
     */
    fun startAllPins() {
        val i = Intent(context, AllPins::class.java)
        startActivity(context, i, null)
    }

    /**
     * Open the FieldBook activity.
     */
    fun startFieldBook() {
        val i = Intent(context, Fieldbook::class.java)
        startActivity(context, i,null)
    }

    /**
     * Open the Settings activity.
     */
    fun startSettings() {
        val i = Intent(context, Settings::class.java)
        startActivity(context, i,null)
    }

    /**
     * Open the Profile activity.
     */
    fun startProfile() {
        val i = Intent(context, Profile::class.java)
        startActivity(context, i,null)
    }

    /**
     * Open the Login activity.
     */
    fun startLogin(){
        val i = Intent(context, Login::class.java)
        startActivity(context, i,null)
    }

    /**
     * Get the current amount of layers.
     * @return the amount of layers.
     */
    fun getLayerCount() : Int{
        return nrLayers
    }

    //functions used for testing
    private val eps = 0.001

    @TestOnly
    /**
     * Gets the location of the first pin in the pins map.
     * @return the location of the first pin.
     */
    fun getPinLocation() : Pair<Float, Float>{
        return pins[pins.keys.first()]!!.getScreenLocation(camera.getViewport(), width, height)
    }

    @TestOnly
    /**
     * A public version of layerVisible.
     * @param[layer] the layer whose visibility will be returned.
     * @return the visibility of the requested layer.
     */
    fun checkLayerVisibility(layer : Int) : Boolean {
        return smap.layerVisible(layer)
    }

    @TestOnly
    /**
     * Checks whether the user location is at the center of the screen.
     * @return whether the user location is (nearly) at the center of the screen.
     */
    fun userLocCentral() : Boolean {
        val screenLoc = coordToScreen(locAtCenterPress, camera.getViewport(), width, height)
        return (abs(screenLoc.first.toInt() - width / 2) < eps && abs(screenLoc.second.toInt() - height / 2) < eps)
    }

    @TestOnly
    /**
     * Checks whether the camera is all the way zoomed out.
     * @return whether the camera is (nearly) all the way zoomed out.
     */
    fun cameraZoomedOut() : Boolean {
        return abs(camera.getZoom() - camera.maxZoom) < eps
    }

    @TestOnly
            /**
             * Zooms the camera in to as far as possible.
             */
    fun zoomIn(){
        camera.setZoom(camera.minZoom)
    }

    @TestOnly
    /**
     * Sets the locAtCenterPress to the current location.
     */
    fun setCenterPos(){
        locAtCenterPress = loc.utm
    }
}