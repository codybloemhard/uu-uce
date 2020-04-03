package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import java.io.File

class ShapeLayer(path: File, nrOfLODs: Int){
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()

    private val chunkGetter= HeightLineReader(path, nrOfLODs)
    private val chunkManager: ChunkManager = ScrollingLoader(chunks, chunkGetter)

    var bmin: p3
        private set
    var bmax: p3
        private set


    init{
        val index = ChunkIndex(0,0,0)
        val chunk = chunkGetter.getChunk(index)
        chunks[index] = chunk
        bmin = chunk.bmin
        bmax = chunk.bmax
    }

    fun onTouchRelease(viewport: Pair<p2, p2>, zoom: Int, map: ShapeMap){
        chunkManager.updateOnStop(viewport, zoom, map)
    }

    fun draw(canvas: Canvas, paint: Paint, map: ShapeMap, viewport : Pair<p2,p2>, width: Int, height: Int, zoomLevel: Int){
        chunkManager.updateOnMove(viewport, zoomLevel, map)

        Logger.log(LogType.Continuous, "zoom", zoomLevel.toString())

        synchronized(chunks) {
            for(chunk in chunks.values) {
                chunk.draw(canvas, paint, viewport, width, height)
            }
        }
    }
}