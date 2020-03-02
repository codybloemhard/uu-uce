package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import diewald_shapeFile.files.shp.SHP_File
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.files.shp.shapeTypes.ShpShape
import kotlin.math.log
import kotlin.math.pow
import kotlin.system.measureTimeMillis

typealias p3 = Triple<Double,Double,Double>
val p3Zero = Triple(0.0,0.0,0.0)

fun mergeBBs(mins: List<p3>,maxs: List<p3>): Pair<p3,p3>{
    var bmin = mutableListOf(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE)
    var bmax = mutableListOf(Double.MIN_VALUE,Double.MIN_VALUE,Double.MIN_VALUE)

    bmin = mins.fold(bmin, {bb, shapez ->
        bb[0] = minOf(shapez.first, bb[0])
        bb[1] = minOf(shapez.second, bb[1])
        bb[2] = minOf(shapez.third, bb[2])
        bb
    })
    bmax = maxs.fold(bmax, {bb, shapez ->
        bb[0] = maxOf(shapez.first, bb[0])
        bb[1] = maxOf(shapez.second, bb[1])
        bb[2] = maxOf(shapez.third, bb[2])
        bb
    })
    return Pair(Triple(bmin[0],bmin[1],bmin[2]),Triple(bmax[0],bmax[1],bmax[2]))
}

fun aabbIntersect(amin: p3, amax: p3, bmin: p3, bmax: p3) : Boolean{
    return !(
            amin.first > bmax.first ||
            amax.first < bmin.first ||
            amin.second > bmax.second ||
            amax.second < bmin.second
            )
}

fun zoomXyMidAabb(bmin: p3, bmax: p3, zoom: Double, waspect: Double): Pair<p3,p3>{
    val w = bmax.first - bmin.first
    val h = bmax.second - bmin.second
    val xm = bmin.first + w / 2.0
    val ym = bmin.second + h / 2.0
    val woff = w * waspect / 2.0 * zoom
    val hoff = h / 2.0 * zoom
    val nmin = Triple(xm - woff, ym - hoff, Double.MIN_VALUE)
    val nmax = Triple(xm + woff, ym + hoff, Double.MAX_VALUE)
    return Pair(nmin, nmax)
}

class ShapeMap(private val nrOfLODs: Int){
    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()
    private var bmin = p3Zero
    private var bmax = p3Zero
    private val zDens = hashMapOf<Int,Int>()
    private var zoomLevel = 5

    private var zoom = 999.0
    private var zoomVel = 0.01
    private var zoomDir = 1.0

    private val deviceLocPaint : Paint = Paint()
    private val deviceLocEdgePaint : Paint = Paint()

    init{
        deviceLocPaint.color = Color.BLUE
        deviceLocEdgePaint.color = Color.WHITE
    }

    fun addLayer(type: LayerType, shpFile: SHP_File){
        val timeSave = measureTimeMillis {
            layers.add(Pair(type,ShapeLayer(shpFile, nrOfLODs)))
        }
        Log.i("ShapeMap", "Save: $timeSave")
        val timeBB = measureTimeMillis {
            val bminmax = mergeBBs(
                layers.map{l -> l.second.bmin},
                layers.map{l -> l.second.bmax})
            bmin = bminmax.first
            bmax = bminmax.second
        }
        val timeDens = measureTimeMillis {
            layers.map{
                l ->
                val lDens = l.second.zDens
                lDens.keys.map {
                    key ->
                    val old = zDens[key] ?: 0
                    val local = lDens[key] ?: 0
                    zDens.put(key, old + local)
                }
            }
        }
        Log.i("ShapeMap", "Calc boundingbox: $timeBB")
        Log.i("ShapeMap", "Calc z density: $timeDens")
        Log.i("ShapeMap", "bb: ($bmin),($bmax)")
        zDens.keys.sorted().map{
            key -> Log.i("ShapeMap", "($key,${zDens[key]})")
        }
    }

    fun draw(canvas: Canvas, width: Int, height: Int){
        val waspect = width.toDouble() / height.toDouble()
        setZoom(waspect)
        val viewport = zoomXyMidAabb(bmin,bmax,zoom, waspect)
        for(layer in layers) layer.second.draw(canvas, layer.first, viewport.first, viewport.second, width, height, zoomLevel)
        drawDeviceLocation(coordToScreen(Pair(314000.0, 4675000.0), viewport.first, viewport.second, width, height), canvas, deviceLocPaint, deviceLocEdgePaint, 15F, 4F)
    }

    private fun setZoom(waspect: Double){
        if(zoom == 999.0) {
            zoom = 1.0 / waspect
        }
        if(zoom < 0.01 && zoomDir > 0.0)
            zoomDir = -1.0
        if(zoom > (1.0 / waspect) && zoomDir < 0.0)
            zoomDir = 1.0

        zoom *= (1.0 - (zoomVel * zoomDir))
        zoomLevel = maxOf(0,minOf(nrOfLODs-1, nrOfLODs - 1 - ((zoom-0.01)/(1.0/waspect-0.01) * nrOfLODs).toInt()))
    }
}

/*
Calculates where on the screen a coordinate is.
coordinate: the coordinate to be mapped onto the screen.
topleft: the coordinate that the top left point of the viewport is on.
botright: the coordinate that the bottom right point of the viewport is on.
width: the amount of pixels the phone screen is wide.
height: the amount of pixels the phone screen is high.
It will provide you with the screen location of a certain coordinate.
 */
private fun coordToScreen(
    coordinate  : Pair<Double, Double>,
    topleft     : Triple<Double, Double, Double>,
    botright    : Triple<Double, Double, Double>,
    width       : Int,
    height      : Int) : Pair<Float, Float>{
    return Pair(((coordinate.first - topleft.first) / (botright.first - topleft.first) * width).toFloat(), (height - (coordinate.second - topleft.second) / (botright.second - topleft.second) * height).toFloat())
}

enum class ShapeType{
    Polygon, Line, Point
}

enum class LayerType{
    Vegetation, Height, Water
}

/*
Calculates where on the screen a coordinate is.
screenLoc: the coordinate on the screen where the device location should be drawn.
canvas: the canvas that the location should be drawn on.
It will draw a circle on the screen at the desired location.
 */
private fun drawDeviceLocation(screenLoc : Pair<Float, Float>, canvas : Canvas, paint : Paint, edgePaint : Paint, size : Float, edgeSize : Float){
    canvas.drawCircle(screenLoc.first, screenLoc.second, size + edgeSize, edgePaint)
    canvas.drawCircle(screenLoc.first, screenLoc.second, size, paint)
}