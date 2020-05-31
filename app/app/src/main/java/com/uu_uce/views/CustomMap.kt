package com.uu_uce.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
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
import com.uu_uce.fieldbook.FullRoute
import com.uu_uce.fieldbook.Route
import com.uu_uce.gestureDetection.*
import com.uu_uce.mapOverlay.Location
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.pointDistance
import com.uu_uce.mapOverlay.pointInAABoundingBox
import com.uu_uce.misc.ListenableBoolean
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.Pin
import com.uu_uce.services.*
import com.uu_uce.shapefiles.*
import kotlinx.android.synthetic.main.activity_geo_map.*
import org.jetbrains.annotations.TestOnly
import java.time.LocalDate
import kotlin.math.abs
import kotlin.system.measureTimeMillis

/*
the view displayed in the app that holds the map
 */
var pinsUpdated = ListenableBoolean()
class CustomMap : ViewTouchParent {

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    //gl renderer
    private val renderer: CustomMapGLRenderer

    // Location
    private val locationServices                            = LocationServices()
    private val locationDeadZone    : Float                 = 5f // How much does the location have to change on the screen to warrant a redraw
    private var loc                 : Location              = Location(UTMCoordinate(31, 'N', 0.0, 0.0), context)
    private var lastDrawnLoc        : Pair<Float, Float>    = Pair(0f, 0f)
    var locationAvailable           : Boolean               = false
    private var locAtCenterPress    : UTMCoordinate         = UTMCoordinate(31, 'N', 0.0, 0.0)

    // Paints
    private val deviceLocPaint      : Paint = Paint()
    private val deviceLocEdgePaint  : Paint = Paint()

    private lateinit var activity           : Activity
    private lateinit var pinViewModel       : PinViewModel
    private lateinit var fieldbookViewModel : FieldbookViewModel
    private lateinit var lfOwner            : LifecycleOwner

    private var pins                        : MutableMap<String, Pin>   = mutableMapOf()
    private var fieldbook                   : List<FieldbookEntry>      = listOf()
    private var sortedPins                  : List<Pin>                 = listOf()
    private var pinStatuses                 : MutableMap<String, Int>   = mutableMapOf()
    var activePopup                         : PopupWindow?              = null

    var pinSize: Int
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

        // Init paints
        deviceLocPaint.color = Color.BLUE
        deviceLocEdgePaint.color = Color.WHITE

        pinsUpdated.setListener(object : ListenableBoolean.ChangeListener {
            override fun onChange() {
                if(pinsUpdated.getValue()){
                    updatePins()
                }
            }
        })
        //width and height are not set in the init{} yet
        //we delay calculations that use them by using post
        post{
            setCameraWAspect()
        }
    }

    //to be called after all addLayer calls
    fun initializeCamera(){
        camera = smap.initialize()
    }

    // Add a new layer to the map, and generate a button to toggle it
    fun addLayer(lt: LayerType, chunkGetter: ChunkGetter, scrollLayout: LinearLayout?, hasInfo: Boolean, buttonSize: Int = 0){
        smap.addLayer(lt, chunkGetter, hasInfo)
        val curLayers = nrLayers
        nrLayers++

        if (buttonSize > 0) {
            val btn = ImageButton(context, null, R.attr.buttonBarButtonStyle).apply {
                setImageResource(R.drawable.ic_sprite_toggle_layer)
                setOnClickListener {
                    toggleLayer(curLayers)
                }
                layoutParams = ViewGroup.LayoutParams(buttonSize, buttonSize)
            }

            scrollLayout!!.addView(btn)
        }

        mods = smap.getMods()
    }

    // Remove all layers from map to avoid duplicates
    fun removeLayers(scrollLayout: LinearLayout){
        smap.removeLayers()
        nrLayers = 0
        scrollLayout.removeAllViewsInLayout()
    }

    fun onDrawFrame(standardProgram: Int, pinProgram: Int, locProgram: Int){
        //if both the camera and the map have no updates, don't redraw
        val res = camera.update()
        val chunkRes = smap.updateChunks()

        if(res == UpdateResult.NOOP && chunkRes == ChunkUpdateResult.NOTHING){
            //bufferframes to make sure everything is redrawn when returning from a different activity
            curBufferFrame++
            if(curBufferFrame >= bufferFrames)
                return
        }
        else curBufferFrame = 0

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val viewport = camera.getViewport()
        val (scale,trans) = camera.getScaleTrans()

        if(viewport == p2ZeroPair){
            Logger.error("CustomMap", "Camera could not be initialized")
            return
        }

        val timeDraw = measureTimeMillis {
            // Draw map
            smap.draw(standardProgram, scale, trans)

            if (context is GeoMap) {
                (context as GeoMap).runOnUiThread {
                    val zoomLevel = smap.getZoomLevel()
                    if (zoomLevel >= 0 && mods.count() > 0) {
                        (context as GeoMap).heightline_diff_text.text =
                            (context as Activity).getString(
                                R.string.geomap_heightline_diff_text,
                                mods[zoomLevel]
                            )

                    } else {
                        val standardValue = 0
                        (context as GeoMap).heightline_diff_text.text =
                            (context as Activity).getString(
                                R.string.geomap_heightline_diff_text,
                                standardValue
                            )
                    }
                }
            }

            Logger.log(LogType.Event, "DrawOverlay", "east: ${loc.utm.east}, north: ${loc.utm.north}")

            // Draw device location
            val deviceScreenLoc = coordToScreen(loc.utm, viewport, width, height)
            val locInScreen =
                deviceScreenLoc.first > 0 && deviceScreenLoc.first < width &&
                        deviceScreenLoc.second > 0 && deviceScreenLoc.second < height
            if(locationAvailable && locInScreen){
                loc.draw(locProgram, scale, trans, pinSize * locSizeFactor, width, height)
                lastDrawnLoc = deviceScreenLoc
            }

            // Draw pin
            synchronized(sortedPins) {
                for(pin in sortedPins) {
                    //pins are drawn at increasing height, lowest at 0, highest at (almost) 1
                    pin.draw(pinProgram, scale, trans, viewport, width, height, this)
                }
            }

            // TODO: drawing route
            //val route = setRoute()
            //route.draw(viewport,this,canvas)
        }
        Logger.log(LogType.Continuous, "CustomMap", "Draw MS: $timeDraw")

        //invalidate so onDraw is called again next frame if necessary
        if(res == UpdateResult.ANIM || chunkRes == ChunkUpdateResult.LOADING)
            requestRender()
    }

    fun initPinsGL(){
        synchronized(pins) {
            for ((_, pin) in pins)
                pin.initGL()
        }
    }

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
        }
        Logger.log(LogType.Event,"CustomMap", "No redraw needed")
        Logger.log(LogType.Event,"CustomMap", "${loc.utm.east}, ${loc.utm.north}")
    }

    fun startLocServices(){
        locationServices.startPollThread(context, 5000, locationDeadZone, ::updateLoc)
    }

    fun tryStartLocServices(activity: Activity){
        val missingPermissions = checkPermissions(activity, LocationServices.permissionsNeeded)
        if(missingPermissions.count() > 0){
            getPermissions(activity, missingPermissions, LOCATION_REQUEST)
        }
        else{
            startLocServices()
            locationAvailable = true
        }
    }

    //used to zoom the camera in and out
    private fun zoomMap(zoom: Float){
        val deltaOne = 1.0 - zoom.toDouble().coerceIn(0.5, 1.5)
        camera.zoomIn(1.0 + deltaOne)
        if(camera.needsInvalidate())
            requestRender()
    }

    //used to scroll the camera
    private fun moveMap(dxpxf: Float, dypxf: Float){
        val dxpx = dxpxf.toDouble()
        val dypx = dypxf.toDouble()
        val dx = dxpx / width * 2
        val dy = dypx / height * 2
        camera.moveCamera(dx, -dy)
        requestRender()
    }

    private fun flingMap(){
        camera.flingCamera()
        requestRender()
    }

    //zoomout until the whole map is visible
    private fun zoomOutMax(){
        camera.zoomOutMax(500.0)
        if(camera.needsInvalidate())
            requestRender()
    }

    //zoom in to the blue dot, at some arbitrary height
    fun zoomToDevice(){
        camera.startAnimation(Triple(loc.utm.east, loc.utm.north, 0.02), 1500.0)
        if(camera.needsInvalidate())
            requestRender()
    }

    //to be called when the map needs to be redrawn
    fun redrawMap(){
        camera.forceChanged()
        requestRender()
    }

    fun setPins(table: LiveData<List<PinData>>){
        // Set observer on pin database
        table.removeObservers(lfOwner)
        table.observe(lfOwner, Observer { pins ->
            // Update the cached copy of the words in the adapter.
            pins?.let { newData -> updatePinStatuses(newData) }
            renderer.pinsChanged = true
        })
    }

    private fun updatePinStatuses(newPinData: List<PinData>) {
        // Update pins from new data
        for(pin in newPinData) {
            if(pinStatuses[pin.pinId] == pin.status){
                // Pin is present and unchanged
                pins[pin.pinId]!!.resize(pinSize)
                continue
            }
            when {
                pinStatuses[pin.pinId] == null -> {
                    // Pin was not yet present
                    val newPin = PinConversion(activity).pinDataToPin(pin, pinViewModel)
                    newPin.tryUnlock {
                        Logger.log(LogType.Info, "CustomMap", "Adding pin")
                        pins[pin.pinId] = newPin
                        pinStatuses[newPin.id] = pin.status
                    }
                    newPin.tapAction = {activity: Activity -> (newPin::openContent)(this,activity) {activePopup = null}}
                    newPin.resize(pinSize)
                    renderer.pinsChanged = true
                }
                pinStatuses[pin.pinId] == 0 -> {
                    // Pin was present and locked (status = 0)
                    val changedPin = pins[pin.pinId]

                    changedPin?.tryUnlock {
                        changedPin.setStatus(1)
                        pinStatuses[changedPin.id] = 1
                    }
                }
                else -> {
                    // Pin was present and unlocked (status >= 1)
                    val changedPin = pins[pin.pinId]

                    if (changedPin != null) {
                        changedPin.setStatus(pin.status)
                        pinStatuses[changedPin.id] = pin.status
                    }
                }
            }
        }
        synchronized(sortedPins) {
            sortedPins = pins.values.sortedByDescending { pin -> pin.coordinate.north }
        }
        redrawMap()
    }

    private fun updatePins(){
        pins = mutableMapOf()
        pinStatuses = mutableMapOf()
        pinViewModel.reloadPins { newPinData -> updatePinStatuses(newPinData) }
        pinsUpdated.setValue(false)
    }

    fun resizePins(){
        for(pin in pins.values){
            pin.resize(pinSize)
        }
    }

    fun setFieldbook (fieldbook: List<FieldbookEntry>) {
        for (entry in fieldbook) {
            val pin = PinConversion(activity).fieldbookEntryToPin(entry,fieldbookViewModel)
            pins[pin.id] = pin.apply{
                resize(pinSize)
                tapAction = {activity: Activity ->  (::openFieldbookPopup)(activity,rootView,entry,pin.getContent().contentBlocks) }
            }
        }
        synchronized(sortedPins) {
            sortedPins = pins.values.sortedByDescending { pin -> pin.coordinate.north }
        }

        redrawMap()
    }

    //called when the screen is tapped at tapLocation
    private fun tapPin(tapLocation : p2, activity : Activity){
        if(activePopup != null) return
        for(pin in sortedPins.reversed()){
            if(!pin.inScreen || pin.getStatus() < 1) continue
            if(pointInAABoundingBox(pin.boundingBox.first, pin.boundingBox.second, tapLocation, 0)) {
                pin.run{
                    tapAction(activity)
                }
                activePopup = pin.popupWindow
                Logger.log(LogType.Info, "CustomMap", "${pin.getTitle()}: I have been tapped.")
                return
            }
        }
    }

    fun setRoute() : Route {
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
    }

    fun setCameraWAspect(){
        camera.wAspect = width.toDouble()/height

        val z = 1.0 / (camera.wAspect)
        camera.maxZoom = maxOf(1.0,z)
        camera.setZoom(z)
        smap.setzooms(camera.minZoom, camera.maxZoom)
    }

    // Turn a layer on or off
    private fun toggleLayer(l: Int){
        smap.toggleLayer(l)
    }

    fun setPinViewModel(vm: PinViewModel){
        pinViewModel = vm
    }

    fun setFieldbookViewModel(vm: FieldbookViewModel){
        fieldbookViewModel = vm
    }

    fun setLifeCycleOwner(lifecycleOwner: LifecycleOwner){
        lfOwner = lifecycleOwner
    }

    fun setActivity(activity: Activity){
        this.activity = activity
    }

    //open the all pins activity
    fun startAllPins() {
        val i = Intent(context, AllPins::class.java)
        startActivity(context, i, null)
    }

    //open fieldbook activity
    fun startFieldBook() {
        val i = Intent(context, Fieldbook::class.java)
        startActivity(context, i,null)
    }

    //open settings activity
    fun startSettings() {
        val i = Intent(context, Settings::class.java)
        startActivity(context, i,null)
    }

    //open profile activity
    fun startProfile() {
        val i = Intent(context, Profile::class.java)
        startActivity(context, i,null)
    }

    fun startLogin(){
        val i = Intent(context, Login::class.java)
        startActivity(context, i,null)
    }

    fun getLayerCount() : Int{
        return nrLayers
    }

    private val eps = 0.001
    //functions used for testing
    @TestOnly
    fun getPinLocation() : Pair<Float, Float>{
        return pins[pins.keys.first()]!!.getScreenLocation(camera.getViewport(), width, height)
    }

    @TestOnly
    fun checkLayerVisibility(layer : Int) : Boolean {
        return smap.checkLayerVisibility(layer)
    }

    @TestOnly
    fun userLocCentral() : Boolean {
        val screenLoc = coordToScreen(locAtCenterPress, camera.getViewport(), width, height)
        return (abs(screenLoc.first.toInt() - width / 2) < eps && abs(screenLoc.second.toInt() - height / 2) < eps)
    }

    @TestOnly
    fun cameraZoomedOut() : Boolean {
        return abs(camera.getZoom() - camera.maxZoom) < eps
    }

    @TestOnly
    fun zoomIn(){
        camera.setZoom(camera.minZoom)
    }

    @TestOnly
    fun setCenterPos(){
        locAtCenterPress = loc.utm
    }
}