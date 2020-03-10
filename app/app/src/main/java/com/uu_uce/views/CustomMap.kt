package com.uu_uce.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.drawDeviceLocation
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinTextContent
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
import java.io.File
import kotlin.system.measureTimeMillis

class CustomMap : ViewTouchParent {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var smap : ShapeMap

    private var loc : UTMCoordinate = UTMCoordinate(31, 'N', 0.0, 0.0)

    private val locationServices = LocationServices()

    private val deviceLocPaint : Paint = Paint()
    private val deviceLocEdgePaint : Paint = Paint()

    private val pin : Pin =
        Pin(
            UTMCoordinate(31, 'N', 314968.0, 4677733.6),
            1,
            PinType.TEXT,
            "Test",
            PinTextContent(),
            60,
            ResourcesCompat.getDrawable(context.resources, R.drawable.pin, null) ?: error ("Image not found")
        )

    private var camera: Camera

    init{
        //setup touch events
        addChild(Zoomer(context, ::zoomMap))
        addChild(Scroller(context, ::moveMap))
        addChild(DoubleTapper(context, ::zoomOutMax))


        Log.d("CustomMap", "Init")
        val dir = File(context.filesDir, "mydir")
        val path1 = File(dir, "bt25mv10sh0f6422al1r020.shp")
        //val path2 = File(dir, "bt25mv10sh0f6422hp1r020.shp")
        SHP_File.LOG_INFO = false
        SHP_File.LOG_ONLOAD_HEADER = false
        SHP_File.LOG_ONLOAD_CONTENT = false
        val file1 = SHP_File(null, path1)
        //val file2 = SHP_File(null, path2)
        val timeRead = measureTimeMillis {
            file1.read()
            //file2.read()
        }
        Log.i("CustomMap", "Read file: $timeRead")
        smap = ShapeMap(10)
        val timeParse = measureTimeMillis {
            smap.addLayer(LayerType.Height, file1, context)
            //smap.addLayer(LayerType.Water, file2, context)
        }

        camera = smap.initialize()
        Log.i("CustomMap", "Parse file: $timeParse")

        deviceLocPaint.color = Color.BLUE
        deviceLocEdgePaint.color = Color.WHITE

        locationServices.startPollThread(context, 5000, 0F, ::updateLoc)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        camera.update()
        val viewport = camera.getViewport(width.toDouble() / height)
        val timeDraw = measureTimeMillis {
            canvas.drawColor(Color.rgb(234, 243, 245))
            smap.draw(canvas, width, height)
            drawDeviceLocation(
                coordToScreen(loc, viewport, this),
                canvas,
                deviceLocPaint,
                deviceLocEdgePaint,
                15F,
                4F)
            pin.draw(viewport, this, canvas)
            
        }
        Log.i("CustomMap", "Draw: $timeDraw")
        invalidate()
    }

    private fun updateLoc(newLoc : Pair<Double, Double>) {
        loc = degreeToUTM(newLoc)
        Log.d("CustomMap", "${loc.east}, ${loc.north}")
    }

    fun zoomMap(zoomf: Float){
        val zoom = zoomf.toDouble()
        val deltaOne = 1.0 - zoom.coerceIn(0.5, 1.5)
        camera.zoomIn(1.0 + deltaOne)
    }

    fun moveMap(dxpxf: Float, dypxf: Float){
        val dxpx = dxpxf.toDouble()
        val dypx = dypxf.toDouble()
        val dx = dxpx / width
        val dy = dypx / height
        camera.moveView(dx * 2, dy * -2)
    }

    fun zoomOutMax(){
        camera.zoomOutMax(1500.0)
    }

    fun zoomToDevice(){
        camera.startAnimation(Triple(loc.east, loc.north, 0.02), 1500.0)
    }

    fun toggleLayer(l: Int){
        smap.layerMask[l] = !smap.layerMask[l]
    }
}