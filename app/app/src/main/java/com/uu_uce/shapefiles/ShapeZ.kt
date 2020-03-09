package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.files.shp.shapeTypes.ShpShape

class ShapeZ {
    private var type: ShapeType
    var points: List<Triple<Double, Double, Double>>
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
        val mutablePoints = mutableListOf(baseShape.points.first())
        for(i in 1 until (baseShape.points.size * zoomPercentage).toInt()){
            mutablePoints.add(baseShape.points[(i/zoomPercentage).toInt()])
        }
        mutablePoints.add(baseShape.points.last())
        points = mutablePoints
        if(points.size > 2)
            Log.d("","")

        val minx = points.minBy{it.first}!!.first
        val miny = points.minBy{it.second}!!.second
        val minz = points.minBy{it.third}!!.third
        val maxx = points.maxBy{it.first}!!.first
        val maxy = points.maxBy{it.second}!!.second
        val maxz = points.maxBy{it.third}!!.third
        bmin = p3(minx,miny,minz)
        bmax = p3(maxx,maxy,maxz)
    }

    fun draw(
        canvas: Canvas,
        paint: Paint,
        topleft: Triple<Double, Double, Double>,
        botright: Triple<Double, Double, Double>,
        width: Int,
        height: Int
    ) {
        if (points.size < 2) return
        val drawPoints = FloatArray(4 * (points.size - 1))

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