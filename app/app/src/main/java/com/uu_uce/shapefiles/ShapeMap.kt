package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import diewald_shapeFile.files.shp.SHP_File
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.files.shp.shapeTypes.ShpShape
import kotlin.system.measureTimeMillis

fun mergeBBs(
        mins: List<MutableList<Double>>,
        maxs: List<MutableList<Double>>)
        : Pair<MutableList<Double>,MutableList<Double>>{
    var bmin = mutableListOf(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE)
    var bmax = mutableListOf(0.0,0.0,0.0)

    bmin = mins.fold(bmin, {bb, shapez ->
        bb[0] = minOf(shapez[0], bb[0])
        bb[1] = minOf(shapez[1], bb[1])
        bb[2] = minOf(shapez[2], bb[2])
        bb
    })
    bmax = maxs.fold(bmax, {bb, shapez ->
        bb[0] = maxOf(shapez[0], bb[0])
        bb[1] = maxOf(shapez[1], bb[1])
        bb[2] = maxOf(shapez[2], bb[2])
        bb
    })
    return Pair(bmin,bmax)
}

class ShapeMap{
    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()
    private var bmin = mutableListOf(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE)
    private var bmax = mutableListOf(0.0,0.0,0.0)
    @ExperimentalUnsignedTypes
    var zDens = hashMapOf<Int,UInt>()
        private set

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
        val bmin = Triple(bmin[0],bmin[1],bmin[2])
        val bmax = Triple(bmax[0],bmax[1],bmax[2])
        val size = minOf(width,height)
        for(layer in layers) layer.second.draw(canvas,layer.first, bmin, bmax, size, size)
    }
}

class ShapeLayer(shapeFile: SHP_File){
    private var shapes: List<ShapeZ>
    var bmin = mutableListOf(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE)
        private set
    var bmax = mutableListOf(0.0,0.0,0.0)
        private set
    @ExperimentalUnsignedTypes
    var zDens = hashMapOf<Int,UInt>()
        private set

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
    }

    fun draw(canvas: Canvas, type: LayerType, topleft: Triple<Double,Double,Double>, botright: Triple<Double,Double,Double>, width: Int, height: Int){
        for(shape in shapes) shape.draw(canvas, type, topleft, botright, width, height)
    }
}

class ShapeZ(shape: ShpShape) {
    private var type: ShapeType
    private var points: List<Triple<Double, Double, Double>>
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var bmin = mutableListOf(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
        private set
    var bmax = mutableListOf(0.0, 0.0, 0.0)
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
                bmin[0] = point[0]
                bmin[1] = point[1]
                bmin[2] = point[2]
                bmax[0] = point[0]
                bmax[1] = point[1]
                bmax[2] = point[2]
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
        bmin[0] = bb[0][0]
        bmin[1] = bb[1][0]
        bmin[2] = bb[2][0]
        bmax[0] = bb[0][1]
        bmax[1] = bb[1][1]
        bmax[2] = bb[2][1]
    }

    fun meanZ(): Int{
        return ((bmin[2] + bmax[2]) / 2).toInt()
    }
}

enum class ShapeType{
    Polygon, Line, Point
}

enum class LayerType{
    Vegetation, Height, Water
}