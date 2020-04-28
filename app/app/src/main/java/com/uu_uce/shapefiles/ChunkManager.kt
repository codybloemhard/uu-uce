package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow

enum class ChunkUpdateResult{NOTHING, REDRAW, LOADING}

class ChunkManager(
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk>,
    private val chunkGetter: ChunkGetter,
    private val map: ShapeMap,
    val bmin: p3,
    val bmax: p3,
    val nrCuts: List<Int>)
{

    private var lastViewport: Pair<p2,p2> = Pair(p2Zero,p2Zero)
    private var lastZoom: Int = -1
    private val nrOfLODs = nrCuts.size

    private var chunkLoaders: List<Pair<ChunkIndex,Job>> = listOf()
    private var chunksLoadedListener: Job? = null
    private var loading = false
    private var upToDate = false
    private var changed = false
    var factor = 0.0

    private val debugPaint = Paint()
    private val loadedChunkPaint = Paint()
    private val loadingChunkPaint = Paint()
    private val unloadedChunkPaint = Paint()

    var xmin = 0
    var xmax = 0
    var ymin = 0
    var ymax = 0

    var maxzoom = 0.0

    var zoom = nrOfLODs

    init{
        debugPaint.color = Color.RED
        debugPaint.strokeWidth = 5f

        loadedChunkPaint.color = Color.GREEN
        loadedChunkPaint.alpha = 128
        loadingChunkPaint.color = Color.GREEN
        loadingChunkPaint.alpha = 128
        unloadedChunkPaint.color = Color.GREEN
        unloadedChunkPaint.alpha = 128
    }

    fun setZooms(minzoom: Double, maxzoom: Double){
        factor = (minzoom/maxzoom).pow(1.0/chunkGetter.nrCuts.size)
        this.maxzoom = maxzoom
    }

    private fun cancelCurrentLoading(){
        synchronized(chunks) {
            chunksLoadedListener?.cancel()

            for ((_, job) in chunkLoaders) {
                job.cancel()
            }
            loading = false
        }
    }

    fun update(viewport: Pair<p2, p2>, camzoom: Double, waspect: Double): ChunkUpdateResult {
        zoom = ceil(log((camzoom/maxzoom), factor)).toInt()
        if(zoom < 0){
            Logger.log(LogType.Info, "ChunkManager", "zoom below zero")
            zoom = 0
        }else if(zoom > nrOfLODs-1){
            Logger.log(LogType.Info, "ChunkManager", "zoom greater than nr of LODs")
            zoom = nrOfLODs-1
        }

        xmin = maxOf(0,((viewport.first.first - bmin.first)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        xmax = minOf(nrCuts[zoom]-1, ((viewport.second.first - bmin.first)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        ymin = maxOf(0, ((viewport.first.second - bmin.second)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())
        ymax = minOf(nrCuts[zoom]-1, ((viewport.second.second - bmin.second)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())

        if(chunksChanged(viewport,zoom)) {
            Logger.log(LogType.Event, "ChunkManager", "camera moved, not updating chunks")
            cancelCurrentLoading()
            upToDate = false
            lastViewport = viewport
            lastZoom = zoom
            return ChunkUpdateResult.LOADING
        }
        lastViewport = viewport
        lastZoom = zoom

        if(loading){
            return ChunkUpdateResult.LOADING
        }

        if(!upToDate) {
            val activeChunks = getActiveChunks(viewport, zoom)
            addChunks(activeChunks, viewport, zoom)

            for (index in activeChunks)
                if (!chunks.containsKey(index))
                    Logger.log(LogType.Event, "ChunkManager", "loading $index")
            return ChunkUpdateResult.LOADING
        }

        if(changed) {
            changed = false
            return ChunkUpdateResult.REDRAW
        }

        return ChunkUpdateResult.NOTHING
    }

    private fun addChunks(chunkIndices: List<ChunkIndex>, viewport: Pair<p2,p2>, zoom: Int){
        loading = true
        val loadedChunks: MutableList<Chunk?> = MutableList(chunkIndices.size){null}
        chunkLoaders = List(chunkIndices.size) {i ->
            val chunkIndex = chunkIndices[i]
            val job = GlobalScope.launch {
                if(!chunks.containsKey(chunkIndex)) {
                    val c: Chunk = chunkGetter.getChunk(chunkIndex)
                    loadedChunks[i] = c
                }
            }
            val pair = Pair(chunkIndex,job)
            pair
        }


        chunksLoadedListener = GlobalScope.launch{
            for((_,job) in chunkLoaders) {
                job.join()
            }
            //screen has not moved for long enough, jobs are all finished
            synchronized(chunks) {
                chunks.keys.removeAll{index ->
                    val res = !shouldGetLoaded(index, zoom)
                    if(res)Logger.log(LogType.Continuous, "ChunkManager", "chunk $index should not be loaded")
                    else Logger.log(LogType.Continuous, "ChunkManager", "chunk $index stays loaded")
                    res
                }
                for (i in chunkLoaders.indices) {
                    val index = chunkIndices[i]
                    val chunk = loadedChunks[i] ?: continue
                    chunks[index] = chunk
                    Logger.log(LogType.Event, "ChunkManager", "loaded chunk $index")
                }
                changed = true
                upToDate = true
                loading = false
            }
        }
    }

    private fun getActiveChunks(viewport: Pair<p2,p2>, zoom: Int): List<ChunkIndex>{
        val res:MutableList<ChunkIndex> = mutableListOf()
        for(x in xmin..xmax) for(y in ymin..ymax){
            res.add(ChunkIndex(x,y,zoom))
        }
        return res
    }

    private fun shouldGetLoaded(chunkIndex: ChunkIndex, zoom: Int): Boolean{
        val (x,y,z) = chunkIndex
        return z == zoom && x >= xmin && y >= ymin && x <= xmax && y <= ymax
    }

    private fun chunksChanged(viewport: Pair<p2,p2>, zoom: Int): Boolean{
        val lastxmin = maxOf(0,((lastViewport.first.first - bmin.first)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        val lastxmax = minOf(nrCuts[zoom]-1, ((lastViewport.second.first - bmin.first)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        val lastymin = maxOf(0, ((lastViewport.first.second - bmin.second)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())
        val lastymax = minOf(nrCuts[zoom]-1, ((lastViewport.second.second - bmin.second)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())

        return zoom != lastZoom || xmin != lastxmin || xmax != lastxmax || ymin != lastymin || ymax != lastymax
    }

    fun debug(canvas: Canvas, viewport: Pair<p2,p2>, width: Int, height: Int){
        synchronized(chunks) {
            for ((index, chunk) in chunks) {
                val (x, y, z) = index
                val xstep = (bmax.first-bmin.first)/nrCuts[z]
                val ystep = (bmax.second-bmin.second)/nrCuts[z]
                canvas.drawRect(
                    (((bmin.first + x * xstep) - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat(),
                    (height - ((bmin.second + y * ystep) - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat(),
                    (((bmin.first + (x + 1) * xstep) - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat(),
                    (height - ((bmin.second + (y + 1) * ystep) - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat(),
                    loadedChunkPaint
                )
            }
        }

        val xstep = (bmax.first-bmin.first)/nrCuts[zoom]
        val ystep = (bmax.second-bmin.second)/nrCuts[zoom]
        for(x in 0..nrCuts[zoom])for(y in 0..nrCuts[zoom]){
            canvas.drawLine(
                (((bmin.first + x*xstep) - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat(),
                (height - ((bmin.second) - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat(),
                (((bmin.first + x*xstep) - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat(),
                (height - ((bmax.second) - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat(),
                debugPaint)
            canvas.drawLine(
                (((bmin.first) - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat(),
                (height - ((bmin.second + y*ystep) - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat(),
                (((bmax.first) - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat(),
                (height - ((bmin.second + y*ystep) - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat(),
                debugPaint)
        }
    }
}