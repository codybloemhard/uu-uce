package com.uu_uce.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.drawDeviceLocation
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinTextContent
import com.uu_uce.pins.PinType
import com.uu_uce.services.LocationServices
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.services.degreeToUTM
import com.uu_uce.shapefiles.Camera
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.ShapeMap
import diewald_shapeFile.files.shp.SHP_File
import java.io.File
import kotlin.system.measureTimeMillis

class CustomMap : View {
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
        Logger.log(LogType.Info,"CustomMap", "Init")
        val dir = File(context.filesDir, "mydir")
        val path = File(dir, "bt25mv10sh0f6422al1r020.shp")
        SHP_File.LOG_INFO = false
        SHP_File.LOG_ONLOAD_HEADER = false
        SHP_File.LOG_ONLOAD_CONTENT = false
        val file = SHP_File(null, path)
        val timeRead = measureTimeMillis {
            file.read()
        }
        Log.i("CustomMap", "Read file: $timeRead")
        smap = ShapeMap(10)
        val timeParse = measureTimeMillis {
            smap.addLayer(LayerType.Height, file)
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
        Logger.log(LogType.Continues, "CustomMap", "Draw: $timeDraw")
        invalidate()
    }

    private fun updateLoc(newLoc : Pair<Double, Double>) {
        loc = degreeToUTM(newLoc)
        Logger.log(LogType.Event,"CustomMap", "${loc.east}, ${loc.north}")
    }

    fun zoomMap(zoom: Double){
        val deltaOne = 1.0 - zoom.coerceIn(0.5, 1.5)
        camera.zoomIn(1.0 + deltaOne)
    }

    fun moveMap(dxpx: Double, dypx: Double){
        val dx = dxpx / width
        val dy = dypx / height
        camera.moveView(dx * 2, dy * -2)
    }

    fun zoomOutMax(){
        camera.zoomOutMax(500.0)
    }

    fun zoomToDevice(){
        camera.startAnimation(Triple(loc.east, loc.north, 0.02), 1500.0)
    }
}