package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow

enum class ChunkUpdateResult{NOTHING, REDRAW, LOADING}

/*
the chunk manager makes sure chunks are loaded in and out of memory properly
chunks: reference to the array where the chunks are stored in the layer
chunkGetter: the means of actually retrieving chunks that tne manager needs from storage
bmin,bmax: bounding box of whole layer
nrCuts: there are nrCuts.size different zoomlevels, where level i has nrCuts[i] by nrCuts[i] chunks
 */
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

    var xmin = 0
    var xmax = 0
    var ymin = 0
    var ymax = 0

    var maxzoom = 0.0

    var zoom = nrOfLODs-1

    init{
        debugPaint.color = Color.RED
        debugPaint.strokeWidth = 5f

        loadedChunkPaint.color = Color.GREEN
        loadedChunkPaint.alpha = 128
    }

    fun setZooms(minzoom: Double, maxzoom: Double){
        factor = (minzoom/maxzoom).pow(1.0/chunkGetter.nrCuts.size)
        this.maxzoom = maxzoom
    }

    fun getZoomLevel() : Int{
        return lastZoom
    }

    //cancel all threads that are currently trying to load new chunks
    private fun cancelCurrentLoading(){
        synchronized(chunks) {
            chunksLoadedListener?.cancel()

            for ((_, job) in chunkLoaders) {
                job.cancel()
            }
            loading = false
        }
    }

    /*
    main function, updates the currently loaded chunks
    viewport: current viewport of the camera
    camzoom: current zoom of the camera
     */
    fun update(viewport: Pair<p2, p2>, camzoom: Double): ChunkUpdateResult {
        zoom = ceil(log((camzoom/maxzoom), factor)).toInt()
        if(zoom < 0){
            Logger.log(LogType.Info, "ChunkManager", "zoom below zero")
            zoom = 0
        }else if(zoom > nrOfLODs-1){
            Logger.log(LogType.Info, "ChunkManager", "zoom greater than nr of LODs")
            zoom = nrOfLODs-1
        }

        //calculate which indices should be loaded
        //xmin..xmax through ymin..ymax are in the viewport
        xmin = maxOf(0,((viewport.first.first - bmin.first)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        xmax = minOf(nrCuts[zoom]-1, ((viewport.second.first - bmin.first)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        ymin = maxOf(0, ((viewport.first.second - bmin.second)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())
        ymax = minOf(nrCuts[zoom]-1, ((viewport.second.second - bmin.second)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())

        //only update chunks if camera has been still for a while
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
            addChunks(activeChunks, zoom)

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

    /*
    load all chunks associated with given indices asynchronously
    chunkIndices: the chunks to load
    zoom: the current zoom level
     */
    private fun addChunks(chunkIndices: List<ChunkIndex>, zoom: Int){
        loading = true

        //make a thread for every chunk to be loaded
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
            //wait until all chunks are loaded
            for((_,job) in chunkLoaders) {
                job.join()
            }

            synchronized(chunks) {
                //remove outdated chunks
                chunks.keys.removeAll{index ->
                    val res = !shouldGetLoaded(index, zoom)
                    if(res)Logger.log(LogType.Continuous, "ChunkManager", "chunk $index should not be loaded")
                    else Logger.log(LogType.Continuous, "ChunkManager", "chunk $index stays loaded")
                    res
                }
                //add new chunks
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

    //all chunks that should currently be active
    private fun getActiveChunks(viewport: Pair<p2,p2>, zoom: Int): List<ChunkIndex>{
        val res:MutableList<ChunkIndex> = mutableListOf()
        for(x in xmin..xmax) for(y in ymin..ymax){
            res.add(ChunkIndex(x,y,zoom))
        }
        return res
    }

    //whether a chunk should be loaded witht he current viewport and zoom
    private fun shouldGetLoaded(chunkIndex: ChunkIndex, zoom: Int): Boolean{
        val (x,y,z) = chunkIndex
        return z == zoom && x >= xmin && y >= ymin && x <= xmax && y <= ymax
    }

    //whether the chunks have changed since last upate call
    private fun chunksChanged(viewport: Pair<p2,p2>, zoom: Int): Boolean{
        val lastxmin = maxOf(0,((lastViewport.first.first - bmin.first)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        val lastxmax = minOf(nrCuts[zoom]-1, ((lastViewport.second.first - bmin.first)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        val lastymin = maxOf(0, ((lastViewport.first.second - bmin.second)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())
        val lastymax = minOf(nrCuts[zoom]-1, ((lastViewport.second.second - bmin.second)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())

        return zoom != lastZoom || xmin != lastxmin || xmax != lastxmax || ymin != lastymin || ymax != lastymax
    }

    //debug function for showing chunks explicitly
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