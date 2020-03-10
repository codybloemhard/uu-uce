package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.files.shp.shapeTypes.ShpShape

class ShapeZ {
    private var type: ShapeType
    var points: List<Triple<Double, Double, Double>>
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var bMin = p3Zero
        private set
    var bMax = p3Zero
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
                bMin = Triple(point[0],point[1],point[2])
                bMax = bMin.copy()
            }
            else -> {
                Logger.log(LogType.NotImplemented,"ShapeZ", "Non supported type: ${shape.shapeType}")
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

        val minX = points.minBy{it.first}!!.first
        val minY = points.minBy{it.second}!!.second
        val minZ = points.minBy{it.third}!!.third
        val maxX = points.maxBy{it.first}!!.first
        val maxY = points.maxBy{it.second}!!.second
        val maxZ = points.maxBy{it.third}!!.third

        bMin = p3(minX,minY,minZ)
        bMax = p3(maxX,maxY,maxZ)
    }

    fun draw(
        canvas: Canvas,
        type: LayerType,
        viewport : Pair<p2, p2>,
        width: Int,
        height: Int
    ) {
        if (points.size < 2) return
        val drawPoints = FloatArray(4 * (points.size - 1))

        var lineIndex = 0
        for (i in 0..points.size - 2) {
            drawPoints[lineIndex++] =
                ((points[i].first - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat()
            drawPoints[lineIndex++] =
                (height - (points[i].second - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat()
            drawPoints[lineIndex++] =
                ((points[i + 1].first - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat()
            drawPoints[lineIndex++] =
                (height - (points[i + 1].second - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat()
        }

        canvas.drawLines(drawPoints, paint)
    }

    private fun updateBB(bb: Array<DoubleArray>) {
        bMin = Triple(bb[0][0], bb[1][0], bb[2][0])
        bMax = Triple(bb[0][1], bb[1][1], bb[2][1])
    }

    fun meanZ(): Int{
        return ((bMin.third + bMax.third) / 2).toInt()
    }
}