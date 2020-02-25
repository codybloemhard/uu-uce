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
    private var bmin = mutableListOf(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE)
    private var bmax = mutableListOf(0.0,0.0,0.0)

    init{
        shapes = shapeFile.shpShapes.map{s -> ShapeZ(s)}
        bmin = shapes.fold(bmin, {bb, shapez ->
            bb[0] = minOf(shapez.bmin[0], bb[0])
            bb[1] = minOf(shapez.bmin[1], bb[1])
            bb[2] = minOf(shapez.bmin[2], bb[2])
            bb
        })
        bmax = shapes.fold(bmax, {bb, shapez ->
            bb[0] = maxOf(shapez.bmax[0], bb[0])
            bb[1] = maxOf(shapez.bmax[1], bb[1])
            bb[2] = maxOf(shapez.bmax[2], bb[2])
            bb
        })
        Log.d("ShapeLayer", "($bmin),($bmax)")
    }
}

class ShapeZ(shape: ShpShape){
    private var type: ShapeType
    private var points: List<Triple<Double,Double,Double>>
    var bmin = mutableListOf(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE)
    var bmax = mutableListOf(0.0,0.0,0.0)

    init{
        when(shape.shapeType){
            ShpShape.Type.PolygonZ -> {
                type = ShapeType.Polygon
                val poly = (shape as ShpPolygon)
                points = poly.points.map { point -> Triple(point[0], point[1], point[2])}
                updateBB(poly.boundingBox)
            }
            ShpShape.Type.PolyLineZ -> {
                type = ShapeType.Polygon
                val poly = (shape as ShpPolyLine)
                points = poly.points.map { point -> Triple(point[0], point[1], point[2])}
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

    private fun updateBB(bb: Array<DoubleArray>){
        bmin[0] = bb[0][0]
        bmin[1] = bb[1][0]
        bmin[2] = bb[2][0]
        bmax[0] = bb[0][1]
        bmax[1] = bb[1][1]
        bmax[2] = bb[2][1]
    }
}

enum class ShapeType{
    Polygon, Line, Point
}

enum class LayerType{
    Vegetation, Height, Water
}