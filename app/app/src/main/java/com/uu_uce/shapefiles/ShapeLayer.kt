package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.util.Log
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import diewald_shapeFile.files.shp.SHP_File
import kotlin.math.log
import kotlin.math.pow

class ShapeLayer(shapeFile: SHP_File, private val nrOfLODs: Int){
    private var allShapes: List<ShapeZ>
    private lateinit var zoomShapes: List<List<ShapeZ>>
    var bmin = p3Zero
        private set
    var bmax = p3Zero
        private set
    val zDens = hashMapOf<Int,Int>()
    private val zDensSorted: List<Int>

    init{
        allShapes = shapeFile.shpShapes.map{ s -> ShapeZ(s)}
        allShapes = allShapes.sortedBy{ it.meanZ() }

        allShapes.map{
                s ->
            val mz = s.meanZ()
            val old = zDens[mz] ?: 0
            val new = old + 1
            zDens.put(mz, new)
        }
        zDensSorted = zDens.keys.sorted()

        createBB()
        createZoomLevels()
    }

    private fun createBB(){
        val bminmax = mergeBBs(
            allShapes.map{ s -> s.bmin},
            allShapes.map{ s -> s.bmax})
        bmin = bminmax.first
        bmax = bminmax.second
    }

    private fun createZoomLevels(){
        val indices: MutableList<Int> = mutableListOf()
        var nrHeights = 0
        var curPow = log(zDensSorted.size.toDouble(), 2.0).toInt() + 1
        var curStep = 0
        var stepSize: Int = 1 shl curPow
        zoomShapes = List(nrOfLODs){ i->
            val level = (i+1).toDouble()/nrOfLODs
            val factor = maxOf(level.pow(3), 0.1)
            val totalHeights =
                if(i == nrOfLODs-1) zDensSorted.size
                else (factor*zDensSorted.size).toInt()

            while(nrHeights < totalHeights){
                val index: Int = curStep * stepSize
                if (index >= zDensSorted.size) {
                    curPow--
                    stepSize = 1 shl curPow
                    curStep = 1
                    continue
                }
                if(indices.contains(index))
                    throw Exception("uh oh")

                indices.add(index)
                nrHeights++
                curStep+=2
            }

            val shapes: MutableList<ShapeZ> = mutableListOf()
            indices.sort()
            if(indices.isNotEmpty()){
                var a = 0
                var b = 0
                while(a < indices.size && b < allShapes.size){
                    val shape = allShapes[b]
                    val z = zDensSorted[indices[a]]
                    when {
                        shape.meanZ() == z -> {
                            //shapes.add(ShapeZ((i+1).toDouble()/zs, allShapes[b]))
                            val factor =(level + level.pow(3))/2
                            shapes.add(ShapeZ((factor), allShapes[b]))
                            b++
                        }
                        shape.meanZ() < z -> b++
                        else -> a++
                    }
                }
            }
            shapes
        }
        zoomShapes.map{
                ss -> ss.map{
                s -> s.points.size
        }
        }
        val npoints = zoomShapes.fold(0){
                r0, ss -> r0 + ss.fold(0){
                r1, s -> r1 + s.points.size
        }
        }
    }

    fun draw(canvas: Canvas, type: LayerType, topleft: p3, botright: p3, width: Int, height: Int, zoomLevel: Int){
        if(allShapes.isEmpty()) return

        Logger.log(LogType.Continues, "zoom", zoomLevel.toString())

        var shapeCount = 0
        for(i in zoomShapes[zoomLevel].indices){
            val shape = zoomShapes[zoomLevel][i]
            if(aabbIntersect(shape.bmin,shape.bmax,topleft,botright)) {
                shape.draw(canvas, type, topleft, botright, width, height)
                shapeCount++
            }
        }
        Logger.log(LogType.Continues, "ShapeMap", "Shapes drawn: $shapeCount / ${allShapes.size}")
    }
}