package com.uu_uce.shapefiles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import java.io.File
import kotlin.system.measureTimeMillis

typealias p2 = Pair<Double, Double>
typealias p3 = Triple<Double,Double,Double>

val p2Zero = Pair(0.0,0.0)
val p3Zero = Triple(0.0,0.0,0.0)
val p3Max = Triple(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)

fun mergeBBs(mins: List<p3>,maxs: List<p3>): Pair<p3,p3>{
    var bmin = mutableListOf(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE)
    var bmax = mutableListOf(Double.MIN_VALUE,Double.MIN_VALUE,Double.MIN_VALUE)

    bmin = mins.fold(bmin, {bb, shapeZ ->
        bb[0] = minOf(shapeZ.first, bb[0])
        bb[1] = minOf(shapeZ.second, bb[1])
        bb[2] = minOf(shapeZ.third, bb[2])
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

class ShapeMap(private val nrOfLODs: Int,
               private val view: View
){
    private var layerMask = mutableListOf<Boolean>()

    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()
    private var bMin = p3Zero
    private var bMax = p3Zero
    private var zoomLevel = 5

    private val layerPaints : List<Paint>

    private lateinit var camera: Camera

    init{
        layerPaints = List(LayerType.values().size){ i ->
            val p = Paint()
            when(i){
                LayerType.Water.value -> {
                    p.color = Color.rgb(33,11,203)
                    p.color = Color.BLACK
                }
                LayerType.Height.value -> {
                    p.color = Color.rgb(0,0,0)
                }
                LayerType.Vegetation.value -> {
                    p.color = Color.rgb(0,133,31)
                    p.color = Color.BLACK
                }
            }
            p
        }
    }

    fun addLayer(type: LayerType, path: File){
        val timeSave = measureTimeMillis {
            layers.add(Pair(type,ShapeLayer(path, nrOfLODs)))
        }

        Logger.log(LogType.Info,"ShapeMap", "Save: $timeSave")
        Logger.log(LogType.Info, "ShapeMap", "bb: ($bMin),($bMax)")

        layerMask.add(true)
    }

    fun initialize(): Camera{
        val bminmax = mergeBBs(
            layers.map{l -> l.second.bmin},
            layers.map{l -> l.second.bmax})
        bMin = bminmax.first
        bMax = bminmax.second
        val mx = (bMin.first + bMax.first) / 2.0
        val my = (bMin.second + bMax.second) / 2.0
        camera = Camera(mx, my, 1.0, bMin, bMax)
        camera.onAnimationEnd = ::onTouchRelease
        return camera
    }

    fun toggleLayer(l: Int){
        layerMask[l] = !layerMask[l]
        invalidate()
    }

    fun invalidate(){
        camera.forceChanged()
        view.invalidate()
    }

    fun onTouchRelease(viewport: Pair<p2,p2>){
        for((_,layer) in layers){
            layer.onTouchRelease(viewport, zoomLevel, this)
        }
    }

    fun draw(canvas: Canvas, width: Int, height: Int){
        val waspect = width.toDouble() / height
        zoomLevel = maxOf(0,minOf(nrOfLODs-1, nrOfLODs - 1 - ((camera.getZoom()-0.01)/(1.0/waspect-0.01) * nrOfLODs).toInt()))
        val viewport = camera.getViewport()

        for(i in layers.indices) {
            if(layerMask[i]) {
                val (t,l) = layers[i]
                l.draw(
                    canvas,
                    layerPaints[t.value],
                    this,
                    viewport,
                    width,
                    height,
                    zoomLevel
                )
            }
        }
    }
}

enum class ShapeType{
    Polygon, Line, Point
}

enum class LayerType(val value: Int){
    Vegetation(0), Height(1), Water(2)
}