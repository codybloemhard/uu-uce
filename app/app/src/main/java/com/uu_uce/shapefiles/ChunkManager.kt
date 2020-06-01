package com.uu_uce.shapefiles

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
    private val bmin: p3,
    private val bmax: p3,
    private val nrCuts: List<Int>)
{
    //render a little extra around the camera for smoothness
    private val extraRenderFac = -0.2f

    private var lastViewport: Pair<p2,p2> = Pair(p2Zero,p2Zero)
    private var lastZoom: Int = -1
    private val nrOfLODs = nrCuts.size

    private var chunkLoader: Job? = null
    private var chunkloaderName = ""

    private var loading = false
    private var upToDate = false
    private var changed = false
    var factor = 0.0f

    private var xmin = 0
    private var xmax = 0
    private var ymin = 0
    private var ymax = 0

    private var maxzoom = 0.0f

    private var zoom = nrOfLODs-1

    fun setZooms(minzoom: Float, maxzoom: Float){
        factor = (minzoom/maxzoom).pow(1.0f/chunkGetter.nrCuts.size)
        this.maxzoom = maxzoom
    }

    fun getZoomLevel() : Int{
        return lastZoom
    }

    //cancel all threads that are currently trying to load new chunks
    private fun cancelCurrentLoading(){
        chunkLoader?.cancel()

        loading = false
    }

    /*
    main function, updates the currently loaded chunks
    viewport: current viewport of the camera
    camzoom: current zoom of the camera
     */
    fun update(viewport: Pair<p2, p2>, camzoom: Float): ChunkUpdateResult {
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
        val width = viewport.second.first - viewport.first.first
        val height = viewport.second.second - viewport.first.second
        xmin = maxOf(0,((viewport.first.first - bmin.first - extraRenderFac * width)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        xmax = minOf(nrCuts[zoom]-1, ((viewport.second.first - bmin.first + extraRenderFac * width)/(bmax.first - bmin.first)*nrCuts[zoom]).toInt())
        ymin = maxOf(0, ((viewport.first.second - bmin.second - extraRenderFac * height)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())
        ymax = minOf(nrCuts[zoom]-1, ((viewport.second.second - bmin.second + extraRenderFac * height)/(bmax.second - bmin.second)*nrCuts[zoom]).toInt())

        //only update chunks if camera has been still for a while
        if(chunksChanged(zoom)) {
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
            val activeChunks = getActiveChunks(zoom)
            addChunks(activeChunks, zoom)
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

        chunkLoader = GlobalScope.launch{
            chunkloaderName = Thread.currentThread().name
            //wait until all chunks are loaded

            for(i in chunkIndices.indices){
                val chunkIndex = chunkIndices[i]
                if(!chunks.containsKey(chunkIndex)) {
                    if(shouldGetLoaded(chunkIndex, zoom)) {
                        val c: Chunk = chunkGetter.getChunk(chunkIndex)
                        if (shouldGetLoaded(chunkIndex, zoom))
                            synchronized(chunks) {
                                chunks[chunkIndex] = c
                            }
                    }
                    Logger.log(LogType.Info, "ChunkManager", "loaded chunk $chunkIndex")
                }
            }

            if(Thread.currentThread().name != chunkloaderName) {
                Logger.error("ChunkManager", "${Thread.currentThread().name} is not main loader $chunkloaderName")
                return@launch
            }

            synchronized(chunks) {
                clearUnusedChunks()
            }

            changed = true
            upToDate = true
            loading = false
        }
    }

    private fun clearUnusedChunks(){
        chunks.keys.removeAll{index ->
            !shouldGetLoaded(index, zoom)
        }
    }

    //all chunks that should currently be active
    private fun getActiveChunks(zoom: Int): List<ChunkIndex>{
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
    private fun chunksChanged( zoom: Int): Boolean {
        val width = lastViewport.second.first - lastViewport.first.first
        val height = lastViewport.second.second - lastViewport.first.second
        val lastxmin = maxOf(0, ((lastViewport.first.first - bmin.first - extraRenderFac * width) / (bmax.first - bmin.first) * nrCuts[zoom]).toInt())
        val lastxmax = minOf(nrCuts[zoom] - 1, ((lastViewport.second.first - bmin.first + extraRenderFac * width) / (bmax.first - bmin.first) * nrCuts[zoom]).toInt())
        val lastymin = maxOf(0, ((lastViewport.first.second - bmin.second - extraRenderFac * height) / (bmax.second - bmin.second) * nrCuts[zoom]).toInt())
        val lastymax = minOf(nrCuts[zoom] - 1, ((lastViewport.second.second - bmin.second + extraRenderFac * height) / (bmax.second - bmin.second) * nrCuts[zoom]).toInt())

        return zoom != lastZoom || xmin != lastxmin || xmax != lastxmax || ymin != lastymin || ymax != lastymax
    }
}