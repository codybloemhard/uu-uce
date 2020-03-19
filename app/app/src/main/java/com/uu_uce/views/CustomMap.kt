package com.uu_uce.views

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.uu_uce.AllPins
import com.uu_uce.R
import com.uu_uce.database.PinData
import com.uu_uce.database.PinViewModel
import com.uu_uce.database.PinConversion
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.drawDeviceLocation

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.mapOverlay.pointInAABoundingBox
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.PinType
import com.uu_uce.services.LocationServices
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.services.degreeToUTM
import com.uu_uce.shapefiles.Camera
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.ShapeMap
import com.uu_uce.ui.DoubleTapper
import com.uu_uce.ui.Scroller
import com.uu_uce.ui.ViewTouchParent
import com.uu_uce.ui.Zoomer
import diewald_shapeFile.files.shp.SHP_File
import diewald_shapeFile.files.shp.shapeTypes.ShpShape
import kotlinx.android.synthetic.main.activity_geo_map.view.*
import com.uu_uce.shapefiles.UpdateResult
import com.uu_uce.shapefiles.p2
import java.io.File
import kotlin.system.measureTimeMillis

class CustomMap : ViewTouchParent {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var smap : ShapeMap
    private var first = true

    private var loc : UTMCoordinate = UTMCoordinate(31, 'N', 0.0, 0.0)

    private val locationServices = LocationServices()

    private val pinTapBufferSize : Int = 10

    private val deviceLocPaint : Paint = Paint()
    private val deviceLocEdgePaint : Paint = Paint()
    private var pins : List<Pin> = emptyList<Pin>()
    private lateinit var viewModel : PinViewModel
    private lateinit var lfOwner : LifecycleOwner

    private var statusBarHeight = 0
    private val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")

    private var camera: Camera

    init{
        //setup touch events
        addChild(Zoomer(context, ::zoomMap))
        addChild(Scroller(context, ::moveMap))
        addChild(DoubleTapper(context, ::zoomOutMax))
        Logger.log(LogType.Info,"CustomMap", "Init")

        val dir = File(context.filesDir, "mydir")
        val path1 = File(dir, "bt25mv10sh0f6422al1r020.shp")
        val path2 = File(dir, "bt25mv10sh0f6422hp1r020.shp")
        SHP_File.LOG_INFO = false
        SHP_File.LOG_ONLOAD_HEADER = false
        SHP_File.LOG_ONLOAD_CONTENT = false
        val file1 = SHP_File(null, path1)
        val file2 = SHP_File(null, path2)
        val timeRead = measureTimeMillis {
            file1.read()
            file2.read()
        }
        Log.i("CustomMap", "Read file: $timeRead")
        smap = ShapeMap(10)
        val timeParse = measureTimeMillis {
            smap.addLayer(LayerType.Height, file1, context)
            smap.addLayer(LayerType.Water, file2, context)
        }

        camera = smap.initialize()
        //Log.i("CustomMap", "Parse file: $timeParse")

        deviceLocPaint.color = Color.BLUE
        deviceLocEdgePaint.color = Color.WHITE
        locationServices.startPollThread(context, 5000, 0F, ::updateLoc)

        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val waspect = width.toDouble() / height
        if(first){
            val z = 1.0 / (waspect)
            camera.maxZoom = z
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

            drawDeviceLocation(
                coordToScreen(loc, viewport, width, height),
                canvas,
                deviceLocPaint,
                deviceLocEdgePaint,
                15F,
                4F)

            pins.map{ pin -> pin.draw(viewport, this, canvas) }

        }
        Logger.log(LogType.Continuous, "CustomMap", "Draw MS: $timeDraw")
        if(res == UpdateResult.ANIM)
            invalidate()
    }

    private fun updateLoc(newLoc : p2) {
        loc = degreeToUTM(newLoc)
        Logger.log(LogType.Event,"CustomMap", "${loc.east}, ${loc.north}")
    }

    fun updatePins(){
        viewModel.allPinData.observe(lfOwner, Observer { pins ->
            // Update the cached copy of the words in the adapter.
            pins?.let { setPins(it) }
        })
    }

    private fun setPins(pins: List<PinData>) {
        var processedPins = mutableListOf<Pin>()
        for(pin in pins) {
            processedPins.add(PinConversion(context).pinDataToPin(pin))
        }
        this.pins = processedPins
        camera.forceChanged()
        invalidate()
    }

    fun zoomMap(zoom: Float){
        val deltaOne = 1.0 - zoom.toDouble().coerceIn(0.5, 1.5)
        camera.zoomIn(1.0 + deltaOne)
        if(camera.needsInvalidate())
            invalidate()
    }

    fun moveMap(dxpxf: Float, dypxf: Float){
        Logger.log(LogType.Continuous, "CustomMap", "$dypxf")
        val dxpx = dxpxf.toDouble()
        val dypx = dypxf.toDouble()
        val dx = dxpx / width
        val dy = dypx / height
        camera.moveView(dx * 2, dy * -2)
        if(camera.needsInvalidate())
            invalidate()
    }

    fun zoomOutMax(){
        camera.zoomOutMax(500.0)
        if(camera.needsInvalidate())
            invalidate()
    }

    fun zoomToDevice(){
        camera.startAnimation(Triple(loc.east, loc.north, 0.02), 1500.0)
        if(camera.needsInvalidate())
            invalidate()
    }

    fun tapPin(tapLocation : p2){
        val canvasTapLocation : p2 = Pair(tapLocation.first, tapLocation.second - statusBarHeight)
        pins.forEach{ p ->
            if(!p.inScreen) return@forEach
            if(pointInAABoundingBox(p.boundingBox.first, p.boundingBox.second, canvasTapLocation, pinTapBufferSize)){
                //TODO: implement popup function here
                Logger.log(LogType.Info, "CustomMap", "${p.title}: I have been tapped.")
                return
            }
        }
    }

    fun toggleLayer(l: Int) {
        smap.layerMask[l] = !smap.layerMask[l]
        camera.forceChanged()
        invalidate()
    }

    fun setViewModel(vm: PinViewModel) {
        viewModel = vm
    }

    fun setLifeCycleOwner(lifecycleOwner: LifecycleOwner) {
        lfOwner = lifecycleOwner
    }

    fun allPins() {
        Log.i("test", "test123")
        val i = Intent(context, AllPins::class.java)
        startActivity(context, i, null)
    }
}