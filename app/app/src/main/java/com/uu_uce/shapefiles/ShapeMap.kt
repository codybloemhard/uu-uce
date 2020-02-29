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
import kotlin.system.measureTimeMillis

typealias p3 = Triple<Double,Double,Double>
val p3Zero = Triple(0.0,0.0,0.0)
val p3Max = Triple(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE)

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

class ShapeMap{
    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()
    private var bmin = p3Zero
    private var bmax = p3Zero
    val zDens = hashMapOf<Int,Int>()
    val zs = 10
    var zoomLevel = 5

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
            layers.add(Pair(type,ShapeLayer(shpFile, zs)))
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
        zoomLevel = maxOf(0,minOf(zs-1, zs - ((zoom-0.1)/(1.0/waspect-0.1) * zs).toInt()))
    }
}

class ShapeLayer(shapeFile: SHP_File, private val zs: Int){
    private var allShapes: List<ShapeZ>
    private var zoomShapes: List<List<ShapeZ>>
    var bmin = p3Zero
        private set
    var bmax = p3Zero
        private set
    val zDens = hashMapOf<Int,Int>()
    private val zDensSorted: List<Int>

    init{
        allShapes = shapeFile.shpShapes.map{ s -> ShapeZ(s)}
        val bminmax = mergeBBs(
            allShapes.map{ s -> s.bmin},
            allShapes.map{ s -> s.bmax})
        bmin = bminmax.first
        bmax = bminmax.second
        Log.i("ShapeLayer", "($bmin),($bmax)")
        allShapes.map{
            s ->
            val mz = s.meanZ()
            val old = zDens[mz] ?: 0
            var new = old + 1
            zDens.put(mz, new)
        }
        zDensSorted = zDens.keys.sorted()
        allShapes = allShapes.sortedBy{ it.meanZ() }

        //create zoom levels for shapes
        var indices: MutableList<Int> = mutableListOf()
        var nrHeights = 0
        var curPow = log(zDensSorted.size.toDouble(), 2.0).toInt() + 1
        var curStep = 0
        var stepSize: Int = 1 shl curPow
        zoomShapes = List(zs){i->
            var totalHeights =
                if(i == zs-1) zDensSorted.size
                else ((i+1) * zDensSorted.size.toFloat() / zs).toInt()

            while(nrHeights < totalHeights){
                var index: Int = curStep * stepSize
                if (index >= zDensSorted.size) {
                    curPow--
                    stepSize = 1 shl curPow
                    curStep = 1
                    continue
                }
                if(indices.contains(index))
                    throw Exception("uh oh")

                indices.add(index)
                nrHeights++
                curStep+=2
            }

            var shapes: MutableList<ShapeZ> = mutableListOf()
            indices.sort()
            if(indices.isNotEmpty()){
                var a = 0
                var b = 0
                while(a < indices.size && b < allShapes.size){
                    var shape = allShapes[b]
                    var z = zDensSorted[indices[a]]
                    when {
                        shape.meanZ() == z -> {
                            //shapes.add(ShapeZ((i+1).toDouble()/zs, allShapes[b]))
                            shapes.add(ShapeZ((i+1).toDouble()/zs, allShapes[b]))
                            b++
                        }
                        shape.meanZ() < z -> b++
                        else -> a++
                    }
                }
            }
            shapes
        }
    }

    fun draw(canvas: Canvas, type: LayerType, topleft: p3, botright: p3, width: Int, height: Int, zoomLevel: Int){
        if(allShapes.isEmpty()) return

        var shapeCount = 0
        for(i in zoomShapes[zoomLevel].indices){
            val shape = zoomShapes[zoomLevel][i]
            if(aabbIntersect(shape.bmin,shape.bmax,topleft,botright)) {
                shape.draw(canvas, type, topleft, botright, width, height)
                shapeCount++
            }
        }
        Log.d("ShapeMap", "Shapes drawn: $shapeCount / ${allShapes.size}")
    }
}

class ShapeZ {
    private var type: ShapeType
    private var points: List<Triple<Double, Double, Double>>
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var bmin = p3Zero
        private set
    var bmax = p3Zero
        private set

    constructor(shape: ShpShape) {
        when (shape.shapeType) {
            ShpShape.Type.PolygonZ -> {
                type = ShapeType.Polygon
                val poly = (shape as ShpPolygon)
                points = poly.points.map { point -> Triple(point[0], point[1], point[2]) }
                updateBB(poly.boundingBox)
            }
            ShpShape.Type.PolyLineZ -> {
                type = ShapeType.Polygon
                val poly = (shape as ShpPolyLine)
                points = poly.points.map { point -> Triple(point[0], point[1], point[2]) }
                updateBB(poly.boundingBox)
            }
            ShpShape.Type.PointZ -> {
                type = ShapeType.Point
                val point = (shape as ShpPoint).point
                points = listOf(Triple(point[0], point[1], point[2]))
                bmin = Triple(point[0],point[1],point[2])
                bmax = bmin.copy()
            }
            else -> {
                Log.d("ShapeZ", "${shape.shapeType}")
                type = ShapeType.Point
                points = listOf()
            }
        }
    }

    constructor(zoomPercentage: Double, baseShape: ShapeZ){
        type = baseShape.type
        var mutablePoints = mutableListOf(baseShape.points.first())
        for(i in 1 until (baseShape.points.size * zoomPercentage).toInt()){
            mutablePoints.add(baseShape.points[(i/zoomPercentage).toInt()])
        }
        mutablePoints.add(baseShape.points.last())
        points = mutablePoints
        if(points.size > 2)
            Log.d("","")

        var minx = points.minBy{it.first}!!.first
        var miny = points.minBy{it.second}!!.second
        var minz = points.minBy{it.third}!!.third
        var maxx = points.maxBy{it.first}!!.first
        var maxy = points.maxBy{it.second}!!.second
        var maxz = points.maxBy{it.third}!!.third
        bmin = p3(minx,miny,minz)
        bmax = p3(maxx,maxy,maxz)
    }

    fun draw(
        canvas: Canvas,
        type: LayerType,
        topleft: Triple<Double, Double, Double>,
        botright: Triple<Double, Double, Double>,
        width: Int,
        height: Int
    ) {
        if (points.size < 2) return
        var drawPoints = FloatArray(4 * (points.size - 1))

        var lineIndex = 0
        for (i in 0..points.size - 2) {
            drawPoints[lineIndex++] =
                ((points[i].first - topleft.first) / (botright.first - topleft.first) * width).toFloat()
            drawPoints[lineIndex++] =
                (height - (points[i].second - topleft.second) / (botright.second - topleft.second) * height).toFloat()
            drawPoints[lineIndex++] =
                ((points[i + 1].first - topleft.first) / (botright.first - topleft.first) * width).toFloat()
            drawPoints[lineIndex++] =
                (height - (points[i + 1].second - topleft.second) / (botright.second - topleft.second) * height).toFloat()
        }

        canvas.drawLines(drawPoints, paint)
    }

    private fun updateBB(bb: Array<DoubleArray>) {
        bmin = Triple(bb[0][0], bb[1][0], bb[2][0])
        bmax = Triple(bb[0][1], bb[1][1], bb[2][1])
    }

    fun meanZ(): Int{
        return ((bmin.third + bmax.third) / 2).toInt()
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