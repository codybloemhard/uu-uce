package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import java.io.File
import kotlin.math.pow

class ShapeLayer(chunkGetter: ChunkGetter, map: ShapeMap, onLoadedAction: (sl: ShapeLayer) -> Unit, hasInfo: Boolean){
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()
    private val chunkManager: StopLoader

    var bmin: p3
        private set
    var bmax: p3
        private set


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
        }

        chunkManager = StopLoader(chunks, chunkGetter, map, bmin, bmax, chunkGetter.nrCuts)
    }

    fun setzooms(minzoom: Double, maxzoom: Double){
        chunkManager.setZooms(minzoom, maxzoom)
    }

    fun updateChunks(viewport: Pair<p2,p2>, zoom: Double, waspect: Double): ChunkUpdateResult{
        return chunkManager.update(viewport, zoom, waspect)
    }

    fun draw(canvas: Canvas, paint: Paint, viewport : Pair<p2,p2>, width: Int, height: Int){
        chunkManager.debug(canvas,viewport, width,height)

        synchronized(chunks) {
            for(chunk in chunks.values) {
                chunk.draw(canvas, paint, viewport, width, height)
            }
        }
    }
}