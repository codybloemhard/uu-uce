package com.uu_uce.shapefiles

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
}

class ShapeLayer(shapeFile: SHP_File){
    private var shapes: List<ShapeZ>

    init{
        shapes = shapeFile.shpShapes.map{s -> ShapeZ(s)}
    }
}

class ShapeZ(shape: ShpShape){
    private var type: ShapeType
    private var points: List<Triple<Double,Double,Double>>
    private var bmin = mutableListOf(0.0,0.0,0.0)
    private var bmax = mutableListOf(0.0,0.0,0.0)

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
}

enum class ShapeType{
    Polygon, Line, Point
}

enum class LayerType{
    Vegetation, Height, Water
}