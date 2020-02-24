package com.uu_uce.shapefiles

import android.util.Log
import diewald_shapeFile.files.shp.SHP_File
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.files.shp.shapeTypes.ShpShape

class ShapeMap() {
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

    init{
        when(shape.shapeType){
            ShpShape.Type.PolygonZ -> {
                type = ShapeType.Polygon
                points = (shape as ShpPolygon).points.map { point -> Triple(point[0], point[1], point[2])}
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
                Log.d("ShapZ", "${shape.shapeType}")
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