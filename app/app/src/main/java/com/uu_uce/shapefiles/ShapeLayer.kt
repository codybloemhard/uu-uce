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
    private lateinit var zoomShapes: List<List<ShapeZ>>
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()

    private val chunkLoaders: MutableList<Pair<ChunkIndex,Job>> = mutableListOf()
    @ExperimentalUnsignedTypes
    private val binShapeReader: BinShapeReader = BinShapeReader(path, nrOfLODs)

    var bmin: p3
        private set
    var bmax: p3
        private set


    init{
        val index = ChunkIndex(0,0,nrOfLODs-1)
        val chunk = binShapeReader.getChunk(index)
        chunks[index] = chunk
        bmin = chunk.bmin
        bmax = chunk.bmax
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

        val routines: MutableList<Job> = mutableListOf()
        for (chunkIndex in newChunks) {
            val routine = GlobalScope.launch {
                val c: Chunk = binShapeReader.getChunk(chunkIndex)
                synchronized(chunks) {
                    chunks[chunkIndex] = c
                }
                map.invalidate()
            }
            chunkLoaders.add(Pair(chunkIndex, routine))
            routines.add(routine)
        }

        GlobalScope.launch {
            var ok = routines.isNotEmpty()
            for(routine in routines){
                routine.join()
                if(routine.isCancelled) {
                    ok = false
                    break
                }
            }
            if(ok) {
                synchronized(chunks) {
                    chunks.keys.removeAll{index ->
                        !shouldGetLoaded(index,viewport,zoom)
                    }
                    var test = chunks
                }

            }
        }

        lastViewport = viewport
        lastZoom = zoom
        Logger.log(LogType.Continuous, "ShapeLayer", "loaded chunks: ${chunks.size}")
    }

    fun draw(canvas: Canvas, paint: Paint, map: ShapeMap, viewport : Pair<p2,p2>, width: Int, height: Int, zoomLevel: Int){
        updateChunks(viewport, zoomLevel, map)

        Logger.log(LogType.Continuous, "zoom", zoomLevel.toString())

        synchronized(chunks) {
            for(chunk in chunks.values) {
                chunk.draw(canvas, paint, viewport, width, height)
            }
        }
    }
}