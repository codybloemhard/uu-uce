package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import diewald_shapeFile.files.shp.SHP_File
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.files.shp.shapeTypes.ShpShape
import kotlin.math.absoluteValue
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
    val woff = w * waspect / 2.0 * zoom;
    val hoff = h / 2.0 * zoom
    val nmin = Triple(xm - woff, ym - hoff, Double.MIN_VALUE)
    val nmax = Triple(xm + woff, ym + hoff, Double.MAX_VALUE)
    return Pair(nmin, nmax)
}

class ShapeMap{
    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()
    private var bmin = p3Zero
    private var bmax = p3Zero
    @ExperimentalUnsignedTypes
    val zDens = hashMapOf<Int,UInt>()

    private var zoom = 999.0

    fun addLayer(type: LayerType, shpFile: SHP_File){
        val timeSave = measureTimeMillis {
            layers.add(Pair(type,ShapeLayer(shpFile)))
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
                    val old = zDens[key] ?: 0u
                    val local = lDens[key] ?: 0u
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
        if(zoom == 999.0)
            zoom = 1.0 / waspect
        else
            zoom *= 0.99
        val viewport = zoomXyMidAabb(bmin,bmax,zoom, waspect)
        for(layer in layers) layer.second.draw(canvas,layer.first, viewport.first, viewport.second, width, height)
    }
}

class ShapeLayer(shapeFile: SHP_File){
    private var shapes: List<ShapeZ>
    var bmin = p3Zero
        private set
    var bmax = p3Zero
        private set
    @ExperimentalUnsignedTypes
    val zDens = hashMapOf<Int,UInt>()
    private val shapeMask = mutableListOf<Boolean>()
    private val shapeSet = mutableSetOf<Int>()

    init{
        shapes = shapeFile.shpShapes.map{s -> ShapeZ(s)}
        val bminmax = mergeBBs(
            shapes.map{s -> s.bmin},
            shapes.map{s -> s.bmax})
        bmin = bminmax.first
        bmax = bminmax.second
        Log.i("ShapeLayer", "($bmin),($bmax)")
        shapes.map{
            s ->
            val mz = s.meanZ()
            val old = zDens[mz] ?: 0u
            var new = old + 1u
            zDens.put(mz, new)
        }
        shapes.map{ shapeMask.add(false) }
    }

    fun draw(canvas: Canvas, type: LayerType, topleft: p3, botright: p3, width: Int, height: Int){
        if(shapes.isEmpty()) return
        var minz = Int.MAX_VALUE
        var maxz = Int.MIN_VALUE
        val zs = 14
        shapeSet.clear()
        shapes.forEachIndexed(){
            i, shape ->
            val ok = aabbIntersect(shape.bmin,shape.bmax,topleft,botright)
            if(ok){
                val mz = shape.meanZ()
                minz = minOf(minz, mz)
                maxz = maxOf(maxz, mz)
                shapeMask[i] = true
                if(!shapeSet.contains(mz)){
                    shapeSet.add(mz)
                }
            }else shapeMask[i] = false
        }
        val zdiff = maxz - minz
        val zIndices = mutableListOf<Int>()
        for(zi in 0..zs)
            zIndices.add(minz + (zdiff.toDouble() * (1.0/zi.toDouble())).toInt())
        val zLevels = zIndices.map{ Int.MAX_VALUE }.toMutableList()
        val zDeltas = zLevels.map{ Int.MAX_VALUE }.toMutableList()
        zIndices.forEachIndexed{
            i, zi ->
            for(shape in shapes){
                val mz = shape.meanZ()
                val delta = (mz - zi).absoluteValue
                if(zDeltas[i] > delta){
                    zDeltas[i] = delta
                    zLevels[i] = mz
                }
            }
        }
        shapes.forEachIndexed{
            i, shape ->
            if(!zLevels.contains(shape.meanZ()))
                shapeMask[i] = false
        }

        var shapeCount = 0
        shapes.forEachIndexed(){
            i, shape ->
            if(shapeMask[i]){
                shape.draw(canvas, type, topleft, botright, width, height)
                shapeCount++
            }
        }
        Log.d("ShapeMap", "Shapes drawn: $shapeCount / ${shapes.size}")
    }
}

class ShapeZ(shape: ShpShape) {
    private var type: ShapeType
    private var points: List<Triple<Double, Double, Double>>
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var bmin = p3Zero
        private set
    var bmax = p3Zero
        private set

    init {
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

enum class ShapeType{
    Polygon, Line, Point
}

enum class LayerType{
    Vegetation, Height, Water
}