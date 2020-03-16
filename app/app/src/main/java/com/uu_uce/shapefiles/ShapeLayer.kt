package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.uu_uce.mapOverlay.aaBoundingBoxIntersect
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import diewald_shapeFile.files.shp.SHP_File
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.log
import kotlin.math.pow

class ShapeLayer(path: File, private val nrOfLODs: Int){
    private var lastViewport: Pair<p2,p2> = Pair(p2Zero,p2Zero)
    private var lastZoom: Int = -1
    private var allShapes: List<ShapeZ>
    private lateinit var zoomShapes: List<List<ShapeZ>>
    private var chunksBackup: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()

    private val chunkLoaders: MutableList<Pair<ChunkIndex,Job>> = mutableListOf()
    private var loadedChunks = 1

    var bmin = p3Zero
        private set
    var bmax = p3Zero
        private set
    val zDens = hashMapOf<Int,Int>()
    private val zDensSorted: List<Int>

    init{
        val shapefile = SHP_File(null, path)
        shapefile.read()

        allShapes = shapefile.shpShapes.map{ s -> ShapeZ(s) }
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

        for(i in 0 until nrOfLODs){
            chunksBackup[Triple(0,0,i)] = Chunk(Triple(0,0,i),zoomShapes[i])
        }
    }

    private fun createBB(){
        val bminmax = mergeBBs(
            allShapes.map{ s -> s.bMin},
            allShapes.map{ s -> s.bMax})
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
                            shapes.add(allShapes[b])
                            //val factor =(level + level.pow(3))/2
                            //shapes.add(ShapeZ((factor), allShapes[b]))
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

    private fun getNewOldChunks(viewport: Pair<p2,p2>, zoomLevel: Int) : Pair<List<ChunkIndex>,List<ChunkIndex>>{
        val new: MutableList<ChunkIndex> = mutableListOf()
        if(zoomLevel != lastZoom) new.add(Triple(0,0,zoomLevel))

        val old: MutableList<Triple<Int,Int,Int>> = mutableListOf()
        if(zoomLevel!= lastZoom)old.add(Triple(0,0,lastZoom))

        return Pair(new.toList(),old.toList())
    }

    private fun shouldGetLoaded(chunkIndex: ChunkIndex, viewport: Pair<p2,p2>, zoom: Int): Boolean{
        return chunkIndex.third == zoom
    }

    private fun updateChunks(viewport: Pair<p2,p2>, zoom: Int, map: ShapeMap){
        for(i in chunkLoaders.size-1 downTo 0){
            val (index,routine) = chunkLoaders[i]
            if(!shouldGetLoaded(index, viewport, zoom))
                routine.cancel()

            if(routine.isCancelled || routine.isCompleted) {
                chunkLoaders.removeAt(i)
                continue
            }
        }

        val (newChunks,oldChunks) = getNewOldChunks(viewport, zoom)

        for (chunkIndex in newChunks) {
            val c: Chunk = chunksBackup[chunkIndex] ?: continue
            val routine = GlobalScope.launch {
                delay(500)
                chunks[chunkIndex] = c
                loadedChunks++
                map.invalidate()
            }
            chunkLoaders.add(Pair(chunkIndex, routine))
        }

        for (chunk in oldChunks) {
            if(chunks.remove(chunk)!=null)
                loadedChunks--
        }

        lastViewport = viewport
        lastZoom = zoom
        Logger.log(LogType.Continuous, "ShapeLayer", "loaded chunks: $loadedChunks")
    }

    fun draw(canvas: Canvas, paint: Paint, map: ShapeMap, viewport : Pair<p2,p2>, width: Int, height: Int, zoomLevel: Int){
        if(allShapes.isEmpty()) return

        updateChunks(viewport, zoomLevel, map)

        Logger.log(LogType.Continuous, "zoom", zoomLevel.toString())

        for(chunk in chunks.values){
            chunk.draw(canvas, paint, viewport, width, height)
        }
    }
}