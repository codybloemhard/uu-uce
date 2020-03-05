package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.uu_uce.misc.Logger
import diewald_shapeFile.files.shp.SHP_File
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

class ShapeMap(private val nrOfLODs: Int){
    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()
    private var bmin = p3Zero
    private var bmax = p3Zero
    private val zDens = hashMapOf<Int,Int>()
    private var zoomLevel = 5

    private val deviceLocPaint : Paint = Paint()
    private val deviceLocEdgePaint : Paint = Paint()

    private lateinit var camera: Camera
    private var first = true

    init{
        deviceLocPaint.color = Color.BLUE
        deviceLocEdgePaint.color = Color.WHITE
    }

    fun addLayer(type: LayerType, shpFile: SHP_File){
        val timeSave = measureTimeMillis {
            layers.add(Pair(type,ShapeLayer(shpFile, nrOfLODs)))
        }
        Logger.log("ShapeMap", "Save: $timeSave")
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
        Logger.log("ShapeMap", "Calc z density: $timeDens")
        Logger.log("ShapeMap", "bb: ($bmin),($bmax)")
        zDens.keys.sorted().map{
            key -> Log.i("ShapeMap", "($key,${zDens[key]})")
        }
    }

    fun initialize(): Camera{
        val bminmax = mergeBBs(
            layers.map{l -> l.second.bmin},
            layers.map{l -> l.second.bmax})
        bmin = bminmax.first
        bmax = bminmax.second
        val mx = (bmin.first + bmax.first) / 2.0
        val my = (bmin.second + bmax.second) / 2.0
        camera = Camera(mx, my, 1.0, bmin, bmax)
        return camera
    }

    fun draw(canvas: Canvas, width: Int, height: Int){
        val waspect = width.toDouble() / height
        if(first){
            val z = 1.0 / waspect
            camera.maxZoom = z
            camera.setZoom(z)
            first = false
        }
        zoomLevel = maxOf(0,minOf(nrOfLODs-1, nrOfLODs - 1 - ((camera.getZoom()-0.01)/(1.0/waspect-0.01) * nrOfLODs).toInt()))
        val viewport = camera.getViewport(waspect)
        for(layer in layers) layer.second.draw(canvas, layer.first, viewport.first, viewport.second, width, height, zoomLevel)
    }
}

enum class ShapeType{
    Polygon, Line, Point
}

enum class LayerType{
    Vegetation, Height, Water
}