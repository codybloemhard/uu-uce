package com.uu_uce.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import com.uu_uce.AllPins
import com.uu_uce.R
import com.uu_uce.allpins.PinConversion
import com.uu_uce.allpins.PinData
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.FieldBook
import com.uu_uce.fieldbook.FullRoute
import com.uu_uce.fieldbook.Route
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.drawLocation
import com.uu_uce.mapOverlay.pointDistance
import com.uu_uce.mapOverlay.pointInAABoundingBox
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.Pin
import com.uu_uce.services.*
import com.uu_uce.shapefiles.*
import com.uu_uce.gestureDetection.*
import java.io.File
import java.time.LocalDate
import kotlin.system.measureTimeMillis

class CustomMap : ViewTouchParent {

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var smap : ShapeMap = ShapeMap(5, this)

    // Location
    private val locationServices                            = LocationServices()
    private val locationDeadZone    : Float                 = 5f // How much does the location have to change on the screen to warrant a redraw
    private val locSize             : Int                   = 20
    private var loc                 : UTMCoordinate         = UTMCoordinate(31, 'N', 0.0, 0.0)
    private var lastDrawnLoc        : Pair<Float, Float>    = Pair(0f, 0f)
    var locationAvailable           : Boolean               = false

    // Paints
    private val deviceLocPaint      : Paint = Paint()
    private val deviceLocEdgePaint  : Paint = Paint()

    // Pins
    private val pinTapBufferSize        : Int                   = 10
    private var pins                    : MutableMap<Int, Pin>  = mutableMapOf()
    private var pinStatuses             : MutableMap<Int, Int>  = mutableMapOf()
    private lateinit var pinViewModel   : PinViewModel
    private lateinit var lfOwner        : LifecycleOwner
    var activePopup: PopupWindow? = null

    // Map
    private var nrLayers = 0
    private lateinit var camera : Camera

    init{
        //disable hardware acceleration for canvas.drawVertices
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        smap = ShapeMap(5, this)

        // Logger mask settings
        Logger.setTagEnabled("CustomMap", false)
        Logger.setTagEnabled("zoom", false)

        //setup touch events
        addChild(Zoomer(context, ::zoomMap))
        addChild(Scroller(context, ::moveMap))
        addChild(DoubleTapper(context, ::zoomOutMax))
        addChild(SingleTapper(context as AppCompatActivity, ::tapPin))



        // Init paints
        deviceLocPaint.color = Color.BLUE
        deviceLocEdgePaint.color = Color.WHITE

        post{
            camera.wAspect = width.toDouble()/height

            val z = 1.0 / (camera.wAspect)
            camera.maxZoom = maxOf(1.0,z)
            camera.setZoom(z)
        }
    }

    //to be called after all addLayer calls
    fun initializeCamera(){
        camera = smap.initialize()
    }

    fun addLayer(lt: LayerType, path: File, chunkGetter: ChunkGetter, scrollLayout: LinearLayout, buttonSize: Int){
        smap.addLayer(lt, path, chunkGetter)

        val btn = ImageButton(context, null, R.attr.buttonBarButtonStyle)
        btn.setImageResource(R.drawable.logotp)
        val curLayers = nrLayers
        btn.setOnClickListener{
            toggleLayer(curLayers)
        }
        nrLayers++

        btn.layoutParams = ViewGroup.LayoutParams(buttonSize, buttonSize)
        scrollLayout.addView(btn)
    }

    override fun onDraw(canvas: Canvas) {
        val res = camera.update()
        val chunkRes = smap.updateChunks()
        if(res == UpdateResult.NOOP && chunkRes == ChunkUpdateResult.NOTHING){
                return
        }


        val viewport = camera.getViewport()
        val timeDraw = measureTimeMillis {
            // Set canvas background color
            canvas.drawColor(Color.rgb(234, 243, 245))

            // Draw map
            smap.draw(canvas, width, height)

            Logger.log(LogType.Event, "DrawOverlay", "east: ${loc.east}, north: ${loc.north}")

            // Draw device location
            val deviceScreenLoc = coordToScreen(loc, viewport, width, height)
            val locInScreen =
                deviceScreenLoc.first > 0 && deviceScreenLoc.first < width &&
                deviceScreenLoc.second > 0 && deviceScreenLoc.second < height
            if(locationAvailable && locInScreen){
                drawLocation(
                    deviceScreenLoc,
                    canvas,
                    deviceLocPaint,
                    deviceLocEdgePaint,
                    locSize * 0.57f,
                    locSize * 0.25f)
                lastDrawnLoc = deviceScreenLoc
            }

            // Draw pins
            pins.forEach{ entry ->
                entry.value.draw(viewport, width, height,this, canvas)
            }

            // TODO: drawing route
            //val route = setRoute()
            //route.draw(viewport,this,canvas)
        }
        Logger.log(LogType.Continuous, "CustomMap", "Draw MS: $timeDraw")
        if(res == UpdateResult.ANIM || chunkRes == ChunkUpdateResult.LOADING)
            invalidate()
    }

    private fun updateLoc(newLoc : p2) {
        // Update called by locationManager
        // TODO: move location drawing to an overlaying transparent canvas to avoid unnecessary map drawing
        loc = degreeToUTM(newLoc)

        val viewport = camera.getViewport()
        val screenLoc = coordToScreen(loc, viewport, width, height)

        // Check if redraw is necessary
        val distance = pointDistance(screenLoc, lastDrawnLoc)

        if(distance > locationDeadZone){
            camera.needsInvalidate()
            Logger.log(LogType.Event,"CustomMap", "Redrawing, distance: $distance")
        }
        Logger.log(LogType.Event,"CustomMap", "No redraw needed")
        Logger.log(LogType.Event,"CustomMap", "${loc.east}, ${loc.north}")
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

    private fun zoomMap(zoom: Float){
        val deltaOne = 1.0 - zoom.toDouble().coerceIn(0.5, 1.5)
        camera.zoomIn(1.0 + deltaOne)
        if(camera.needsInvalidate())
            invalidate()
    }

    private fun moveMap(dxpxf: Float, dypxf: Float){
        Logger.log(LogType.Continuous, "CustomMap", "$dypxf")
        val dxpx = dxpxf.toDouble()
        val dypx = dypxf.toDouble()
        val dx = dxpx / width
        val dy = dypx / height
        camera.moveView(dx * 2, dy * -2)
        if(camera.needsInvalidate())
            invalidate()
    }

    private fun zoomOutMax(){
        camera.zoomOutMax(500.0)
        if(camera.needsInvalidate())
            invalidate()
    }

    fun zoomToDevice(){
        camera.startAnimation(Triple(loc.east, loc.north, 0.02), 1500.0)
        if(camera.needsInvalidate())
            invalidate()
    }

    fun redrawMap(){
        camera.forceChanged()
        invalidate()
    }

    fun setPins(table: LiveData<List<PinData>>){
        // Set observer on pin database
        table.observe(lfOwner, Observer { pins ->
            // Update the cached copy of the words in the adapter.
            pins?.let { newData ->
                updatePinStatuses(newData)
            }
        })
    }

    private fun updatePinStatuses(newPinData: List<PinData>) {
        // Update pins from new data
        for(pin in newPinData) {
            if(pinStatuses[pin.pinId] == pin.status){
                // Pin is present and unchanged
                continue
            }
            when {
                pinStatuses[pin.pinId] == null -> {
                    // Pin was not yet present
                    val newPin = PinConversion(context)
                        .pinDataToPin(pin, pinViewModel)
                    newPin.tryUnlock {
                        Logger.log(LogType.Info, "CustomMap", "Adding pin")
                        pins[pin.pinId] = newPin
                        pinStatuses[newPin.id] = pin.status
                        camera.forceChanged()
                        invalidate()
                    }
                }
                pinStatuses[pin.pinId] == 0 -> {
                    // Pin was present and locked (status = 0)
                    val changedPin = pins[pin.pinId]

                    changedPin?.tryUnlock {
                        changedPin.setStatus(1)
                        pinStatuses[changedPin.id] = 1
                        camera.forceChanged()
                        invalidate()
                    }
                }
                else -> {
                    // Pin was present and unlocked (status = 1)
                    val changedPin = pins[pin.pinId]

                    if (changedPin != null) {
                        changedPin.setStatus(pin.status)
                        pinStatuses[changedPin.id] = pin.status
                        camera.forceChanged()
                        invalidate()
                    }
                }
            }
        }
    }

    private fun tapPin(tapLocation : p2, activity : Activity){
        for(entry in pins.toList().asReversed()){
            val pin = entry.second
            if(!pin.inScreen || pin.getStatus() < 1) continue
            if(pointInAABoundingBox(pin.boundingBox.first, pin.boundingBox.second, tapLocation, pinTapBufferSize)){
                pin.openPinPopupWindow(this, activity) {activePopup = null}
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
                            "        \"coordinate\": \"31N3149680N46777336E\",\n" +
                            "        \"localtime\": \"10:19:16\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "        \"coordinate\": \"31N3133680N46718336E\",\n" +
                            "        \"localtime\": \"15:13:42\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "        \"coordinate\": \"31N3130000N46710000E\",\n" +
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

    private fun toggleLayer(l: Int){
        smap.toggleLayer(l)
    }

    fun setPinViewModel(vm: PinViewModel){
        pinViewModel = vm
    }

    fun setLifeCycleOwner(lifecycleOwner: LifecycleOwner){
        lfOwner = lifecycleOwner
    }

    fun startAllPins(){
        val i = Intent(context, AllPins::class.java)
        startActivity(context, i, null)
    }

    fun startFieldBook() {
        val i = Intent(context, FieldBook::class.java)
        startActivity(context,i,null)
    }
}