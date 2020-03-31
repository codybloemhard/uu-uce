package com.uu_uce.views

import android.Manifest
import android.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.uu_uce.AllPins
import com.uu_uce.database.*
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.drawDeviceLocation
import com.uu_uce.mapOverlay.pointDistance
import com.uu_uce.mapOverlay.pointInAABoundingBox
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.Pin
import com.uu_uce.services.LocationServices
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.services.checkPermissions
import com.uu_uce.services.degreeToUTM
import com.uu_uce.shapefiles.*
import com.uu_uce.ui.*
import com.uu_uce.ui.Scroller
import diewald_shapeFile.files.shp.SHP_File
import kotlinx.android.synthetic.main.activity_geo_map.view.*
import java.io.File
import kotlin.system.measureTimeMillis

class CustomMap : ViewTouchParent {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var permissionsNeeded = listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var smap : ShapeMap
    private var first = true

    private var loc             : UTMCoordinate = UTMCoordinate(31, 'N', 0.0, 0.0)
    private var lastDrawnLoc    : Pair<Float, Float> = Pair(0f, 0f)

    // How much does the location have to change on the screen to warrant a redraw
    private val locationAccuracy : Float = 5f

    private val locationServices = LocationServices()

    private val pinTapBufferSize : Int = 10

    private val deviceLocPaint      : Paint = Paint()
    private val deviceLocEdgePaint  : Paint = Paint()

    private var arraysReady = false
    private lateinit var pins           : Array<Pin?>
    private lateinit var pinStatuses    : Array<Int?>
    private lateinit var viewModel      : PinViewModel
    private lateinit var lfOwner        : LifecycleOwner

    private var nrLayers = 0
    private lateinit var camera: Camera

    init{
        SHP_File.LOG_INFO           = false
        SHP_File.LOG_ONLOAD_HEADER  = false
        SHP_File.LOG_ONLOAD_CONTENT = false

        // Logger mask settings
        //Logger.setTagEnabled("CustomMap", false)
        Logger.setTagEnabled("zoom", false)
        //setup touch events
        addChild(Zoomer(context, ::zoomMap))
        addChild(Scroller(context, ::moveMap))
        addChild(DoubleTapper(context, ::zoomOutMax))
        addChild(SingleTapper(context as AppCompatActivity, ::tapPin))

        // Parse shapes
        smap = ShapeMap(5, this)

        deviceLocPaint.color = Color.BLUE
        deviceLocEdgePaint.color = Color.WHITE
    }

    fun addLayer(lt: LayerType, path: File, scrollLayout: LinearLayout, buttonSize: Int){
        smap.addLayer(lt, path, context)
        camera = smap.initialize()

        val btn = ImageButton(context, null, R.attr.buttonBarButtonStyle)
        btn.setImageResource(com.uu_uce.R.drawable.logotp)
        val curLayers = nrLayers
        btn.setOnClickListener{
            toggleLayer(curLayers)
        }
        nrLayers++

        btn.layoutParams = ViewGroup.LayoutParams(buttonSize, buttonSize)
        scrollLayout.addView(btn)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val waspect = width.toDouble() / height
        if(first){
            val z = 1.0 / (waspect)
            camera.maxZoom = maxOf(1.0,z)
            camera.setZoom(z)
            first = false
        }
        val res = camera.update()
        if(res == UpdateResult.NOOP){
            return
        }

        val viewport = camera.getViewport(waspect)
        val timeDraw = measureTimeMillis {
            canvas.drawColor(Color.rgb(234, 243, 245))
            smap.draw(canvas, width, height)

            Logger.log(LogType.Event, "DrawOverlay", "east: ${loc.east}, north: ${loc.north}")

            val screenLoc = coordToScreen(loc, viewport, width, height)
            drawDeviceLocation(
                screenLoc,
                canvas,
                deviceLocPaint,
                deviceLocEdgePaint,
                15F,
                4F)
            lastDrawnLoc = screenLoc

            if(!arraysReady) return
            pins.forEach{ pin ->
                if(pin == null) return@forEach
                pin.draw(viewport, this, canvas)
            }
        }
        Logger.log(LogType.Continuous, "CustomMap", "Draw MS: $timeDraw")
        if(res == UpdateResult.ANIM)
            invalidate()
    }

    private fun updateLoc(newLoc : p2) {
        // TODO: move location drawing to an overlaying transparent canvas to avoid unnecessary map drawing
        loc = degreeToUTM(newLoc)

        val waspect = width.toDouble() / height
        val viewport = camera.getViewport(waspect)
        val screenLoc = coordToScreen(loc, viewport, width, height)

        val distance = pointDistance(screenLoc, lastDrawnLoc)
        if(distance > locationAccuracy){
            camera.needsInvalidate()
            Logger.log(LogType.Event,"CustomMap", "Redrawing, distance: $distance")
        }
        Logger.log(LogType.Event,"CustomMap", "No redraw needed")
        Logger.log(LogType.Event,"CustomMap", "${loc.east}, ${loc.north}")
    }

    fun initPinArrays(){
        viewModel.createArrays{ pinCount ->
            pins = Array(pinCount){null}
            pinStatuses = Array(pinCount){null}
            arraysReady = true
            updatePins()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_UP){
            smap.onTouchRelease(camera.getViewport(width.toDouble()/height))
        }
        return super.onTouchEvent(event)
    }

    fun updatePins(){
        viewModel.allPinData.observe(lfOwner, Observer { pins ->
            // Update the cached copy of the words in the adapter.
            viewModel.allPinData
            pins?.let {
                updatePinStatuses(it)
            }
        })
    }

    private fun updatePinStatuses(newPinData: List<PinData>) {
        for(pin in newPinData) {
            if(pinStatuses[pin.pinId] == pin.status){
                // Pin is present and unchanged
                continue
            }
            when {
                pinStatuses[pin.pinId] == null -> {
                    // Pin was not yet present
                    val newPin = PinConversion(context).pinDataToPin(pin, viewModel)
                    newPin.tryUnlock {
                        Logger.log(LogType.Info, "CustomMap", "Adding pin")
                        pins[pin.pinId] = newPin
                        pinStatuses[newPin.id] = pin.status
                        camera.forceChanged()
                        invalidate()
                    }
                }
                pinStatuses[pin.pinId] == 0 -> {
                    // Pin was locked (status = 0)
                    val changedPin = pins[pin.pinId]

                    changedPin?.tryUnlock {
                        changedPin.setStatus(1)
                        pinStatuses[changedPin.id] = 1
                        camera.forceChanged()
                        invalidate()
                    }
                }
                else -> {
                    // Pin was unlocked (status = 1)
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

    private fun tapPin(tapLocation : p2, activity : Activity){
        val canvasTapLocation : p2 = Pair(tapLocation.first, tapLocation.second)
        pins.forEach{ p ->
            if(p == null || !p.inScreen) return@forEach
            if(pointInAABoundingBox(p.boundingBox.first, p.boundingBox.second, canvasTapLocation, pinTapBufferSize)){
                p.openPinPopupWindow(this, activity)
                Logger.log(LogType.Info, "CustomMap", "${p.getTitle()}: I have been tapped.")
                return
            }
        }
    }

    fun toggleLayer(l: Int){
        smap.toggleLayer(l)
    }

    fun setViewModel(vm: PinViewModel){
        viewModel = vm
    }

    fun setLifeCycleOwner(lifecycleOwner: LifecycleOwner){
        lfOwner = lifecycleOwner
    }

    fun allPins(){
        val i = Intent(context, AllPins::class.java)
        startActivity(context, i, null)
    }

    fun startLocServices(){
        locationServices.startPollThread(context, 5000, 0F, ::updateLoc)
    }

    fun redrawMap(){
        camera.forceChanged()
        invalidate()
    }
}