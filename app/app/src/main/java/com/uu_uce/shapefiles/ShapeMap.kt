package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import diewald_shapeFile.files.shp.SHP_File
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.files.shp.shapeTypes.ShpShape

class ShapeMap{
    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()

    fun addLayer(type: LayerType, shpFile: SHP_File){
        layers.add(Pair(type,ShapeLayer(shpFile)))
    }

    fun draw(canvas: Canvas, topleft: Triple<Double,Double,Double>, botright: Triple<Double,Double,Double>, width: Int, height: Int){
        for(layer in layers) layer.second.draw(canvas,layer.first, topleft, botright, width, height)
    }
}

class ShapeLayer(shapeFile: SHP_File){
    private var shapes: List<ShapeZ>

    init{
        shapes = shapeFile.shpShapes.map{s -> ShapeZ(s)}
    }

    fun draw(canvas: Canvas, type: LayerType, topleft: Triple<Double,Double,Double>, botright: Triple<Double,Double,Double>, width: Int, height: Int){
        for(shape in shapes) shape.draw(canvas, type, topleft, botright, width, height)
    }
}

class ShapeZ(shape: ShpShape){
    private var type: ShapeType
    private var points: List<Triple<Double,Double,Double>>
    private var bmin = mutableListOf(0.0,0.0,0.0)
    private var bmax = mutableListOf(0.0,0.0,0.0)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init{
        when(shape.shapeType){
            ShpShape.Type.PolygonZ -> {
                type = ShapeType.Polygon
                val poly = (shape as ShpPolygon)
                points = poly.points.map { point -> Triple(point[0], point[1], point[2])}
                poly.boundingBox
                bmin[0] = minOf(bmin[0], poly.boundingBox[0][0])
                //bmin[1] = minOf(bmin[1], poly.boundingBox[0])
            }
            ShpShape.Type.PolyLineZ -> {
                type = ShapeType.Polygon
                points = (shape as ShpPolyLine).points.map { point -> Triple(point[0], point[1], point[2])}
            }
            ShpShape.Type.PointZ -> {
                type = ShapeType.Point
                points = listOf()
            }
            else -> {
                Log.d("ShapeZ", "${shape.shapeType}")
                type = ShapeType.Point
                points = listOf()
            }
        }
    }

    fun draw(canvas: Canvas, type: LayerType, topleft: Triple<Double,Double,Double>, botright: Triple<Double,Double,Double>, width: Int, height: Int){
        if(points.size<1) return
        var drawPoints = FloatArray(4*(points.size-1))

        var lineIndex = 0
        for(i in 0..points.size-2){
            drawPoints[lineIndex++] = ((points[i].first - topleft.first)/(botright.first-topleft.first) * width).toFloat()
            drawPoints[lineIndex++] = ((points[i].second - topleft.second)/(botright.second-topleft.second) * height).toFloat()
            drawPoints[lineIndex++] = ((points[i+1].first - topleft.first)/(botright.first-topleft.first) * height).toFloat()
            drawPoints[lineIndex++] = ((points[i+1].first - topleft.second)/(botright.second-topleft.second) * width).toFloat()
        }

        canvas.drawLines(drawPoints, paint)
    }
}

enum class ShapeType{
    Polygon, Line, Point
}

enum class LayerType{
    Vegetation, Height, Water
}