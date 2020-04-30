package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import com.uu_uce.debug
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger

/*
a layer to be displayed in the map, consisting of multiple shapes
chunkGetter: means of getting chunks associated with this layer
map: the map this layer is part of
hasInfo: temporary indicator whether this layer has an info file associated with it
 */
class ShapeLayer(private val chunkGetter: ChunkGetter, map: ShapeMap, hasInfo: Boolean){
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()
    private val chunkManager: ChunkManager

    var bmin: p3
        private set
    var bmax: p3
        private set


    //setup the chunk and bounding box information
    init{
        if(hasInfo) {
            val info = chunkGetter.readInfo()
            bmin = info.first
            bmax = info.second
        }
        else {
            val index = ChunkIndex(0, 0, 0)
            val chunk = chunkGetter.getChunk(index)
            chunks[index] = chunk
            bmin = chunk.bmin
            bmax = chunk.bmax
            chunkGetter.nrCuts = listOf(1)
        }

        chunkManager = ChunkManager(chunks, chunkGetter, map, bmin, bmax, chunkGetter.nrCuts)
    }

    fun setzooms(minzoom: Double, maxzoom: Double){
        chunkManager.setZooms(minzoom, maxzoom)
    }

    fun updateChunks(viewport: Pair<p2,p2>, zoom: Double): ChunkUpdateResult {
        return chunkManager.update(viewport, zoom)
    }

    fun getZoomLevel() : Int {
        return chunkManager.getZoomLevel()
    }

    fun getMods() : List<Int> {
        return chunkGetter.mods
    }

    //draw all chunks associated with this layer
    fun draw(canvas: Canvas, paint: Paint, viewport : Pair<p2,p2>, width: Int, height: Int){
        if(debug) chunkManager.debug(canvas,viewport, width,height)

        synchronized(chunks) {
            var nrShapes = 0
            var nrLines = 0
            for(chunk in chunks.values) {
                chunk.draw(canvas, paint, viewport, width, height)

                nrShapes += chunk.shapes.size
                for(shape in chunk.shapes){
                    nrLines+=shape.nrPoints-1
                }
            }

            Logger.log(LogType.Continuous, "ShapeLayer", "$nrShapes shapes with $nrLines lines, average ${if(nrShapes > 0) nrLines.toDouble()/nrShapes else 0} lines per shape")
        }
    }
}