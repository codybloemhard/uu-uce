package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import java.io.File

class ShapeLayer(chunkGetter: ChunkGetter, map: ShapeMap, onLoadedAction: (sl: ShapeLayer) -> Unit){
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()

    private val chunkManager: ChunkManager = StopLoader(chunks, chunkGetter, map)

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

    fun updateChunks(viewport: Pair<p2,p2>, zoom: Int): ChunkUpdateResult{
        return chunkManager.update(viewport, zoom)
    }

    fun draw(canvas: Canvas, paint: Paint, viewport : Pair<p2,p2>, width: Int, height: Int, zoomLevel: Int){
        Logger.log(LogType.Continuous, "zoom", zoomLevel.toString())

        synchronized(chunks) {
            for(chunk in chunks.values) {
                chunk.draw(canvas, paint, viewport, width, height)
            }
        }
    }
}