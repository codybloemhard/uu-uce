package com.uu_uce.shapefiles

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow
import kotlin.system.measureTimeMillis

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
    private val nrCuts: List<Int>,
    private val zoomCutoff: Float)
{
    //render a little extra around the camera for smoothness
    private val extraRenderFac = 0.0f

    private var lastViewport: Pair<p2,p2> = Pair(p2Zero,p2Zero)
    private var lastZoomLevel: Int = -1
    private val nrOfLODs = nrCuts.size
    private var camzoom = 0f

    private var chunkLoader: Job? = null
    private var chunkloaderName = ""

    private var loading = false
    private var changed = false

    var factor = 0.0f

    private var xmin = 0
    private var xmax = 0
    private var ymin = 0
    private var ymax = 0

    private var maxzoom = 0.0f

    var zoomLevel = nrOfLODs-1

    fun setZooms(minzoom: Float, maxzoom: Float){
        factor = (minzoom/maxzoom).pow(1.0f/chunkGetter.nrCuts.size)
        this.maxzoom = maxzoom
    }

    /*
    main function, updates the currently loaded chunks
    viewport: current viewport of the camera
    camzoom: current zoom of the camera
     */
    fun update(viewport: Pair<p2, p2>, cameraZoom: Float): ChunkUpdateResult {
        camzoom = cameraZoom
        if(camzoom > zoomCutoff) {
            synchronized(chunks){
                chunks.clear()
            }
            return ChunkUpdateResult.NOTHING
        }
        val newZoomLevel = ceil(log((camzoom/maxzoom), factor)).toInt()
        zoomLevel = maxOf(0,minOf(nrOfLODs-1,newZoomLevel))

        //calculate which indices should be loaded
        //xmin..xmax through ymin..ymax are in the viewport
        val width = viewport.second.first - viewport.first.first
        val height = viewport.second.second - viewport.first.second
        xmin = maxOf(0,((viewport.first.first - bmin.first - extraRenderFac * width)/(bmax.first - bmin.first)*nrCuts[zoomLevel]).toInt())
        xmax = minOf(nrCuts[zoomLevel]-1, ((viewport.second.first - bmin.first + extraRenderFac * width)/(bmax.first - bmin.first)*nrCuts[zoomLevel]).toInt())
        ymin = maxOf(0, ((viewport.first.second - bmin.second - extraRenderFac * height)/(bmax.second - bmin.second)*nrCuts[zoomLevel]).toInt())
        ymax = minOf(nrCuts[zoomLevel]-1, ((viewport.second.second - bmin.second + extraRenderFac * height)/(bmax.second - bmin.second)*nrCuts[zoomLevel]).toInt())

        val chunksChanged = chunksChanged()
        lastViewport = viewport
        lastZoomLevel = zoomLevel

        if(chunksChanged) {
            val activeChunks = getActiveChunks()
            addChunks(activeChunks)
            return ChunkUpdateResult.LOADING
        }

        if(loading){
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
    private fun addChunks(chunkIndices: List<ChunkIndex>){
        loading = true

        chunkLoader?.cancel()
        chunkLoader = GlobalScope.launch{
            chunkloaderName = Thread.currentThread().name
            synchronized(chunks) {
                clearUnusedChunks()
            }

            for(i in chunkIndices.indices){
                val chunkIndex = chunkIndices[i]
                if(!chunks.containsKey(chunkIndex)) {
                    if(shouldGetLoaded(chunkIndex)) {
                        val c: Chunk = chunkGetter.getChunk(chunkIndex)
                        synchronized(chunks) {
                            if(shouldGetLoaded(chunkIndex)) {
                                chunks[chunkIndex] = c
                                Logger.log(LogType.Info, "ChunkManager", "loaded chunk $chunkIndex")
                            }
                        }
                    }
                }
            }

            if(Thread.currentThread().name != chunkloaderName) {
                return@launch
            }
            synchronized(chunks) {
                clearUnusedChunks()
            }

            changed = true
            loading = false
        }
    }

    private fun clearUnusedChunks(){
        chunks.keys.removeAll { index ->
            !shouldGetLoaded(index)
        }
    }

    //all chunks that should currently be active, in a spiral pattern
    private fun getActiveChunks(): List<ChunkIndex>{
        val res:MutableList<ChunkIndex> = mutableListOf()
        val nrrings = maxOf((xmax+1-xmin)/2f + 1, (ymax+1-ymin)/2f + 1).toInt()
        val midx = (xmax+xmin)/2
        val midy = (ymax+ymin)/2
        res.add(ChunkIndex(midx,midy,zoomLevel))
        for(ring in 1 until nrrings){
            val left = maxOf(xmin,midx-ring)
            val leftok = left == midx-ring
            val right = minOf(xmax, midx+ring)
            val rightok = right == midx+ring
            val bot = maxOf(ymin,midy-ring)
            val botok = bot == midy-ring
            val top = minOf(ymax,midy+ring)
            val topok = top == midy+ring

            if(topok) {
                for (x in left + 1..right - 1) {
                    res.add(ChunkIndex(x, top, zoomLevel))
                }
            }
            if(rightok||topok)res.add(ChunkIndex(right, top, zoomLevel))

            if(rightok) {
                for (y in top - 1 downTo bot + 1) {
                    res.add(ChunkIndex(right, y, zoomLevel))
                }
            }
            if(rightok||botok)res.add(ChunkIndex(right, bot, zoomLevel))

            if(botok){
                for(x in right-1 downTo left+1){
                    res.add(ChunkIndex(x, bot, zoomLevel))
                }
            }
            if(leftok||botok)res.add(ChunkIndex(left, bot, zoomLevel))

            if(leftok){
                for(y in bot+1..top-1){
                    res.add(ChunkIndex(left, y, zoomLevel))
                }
            }
            if(leftok || topok) res.add(ChunkIndex(left, top, zoomLevel))
        }
        return res
    }

    //whether a chunk should be loaded witht he current viewport and zoom
    private fun shouldGetLoaded(chunkIndex: ChunkIndex): Boolean{
        val (x,y,z) = chunkIndex
        return camzoom < zoomCutoff && z == zoomLevel && x >= xmin && y >= ymin && x <= xmax && y <= ymax
    }

    //whether the chunks have changed since last upate call
    private fun chunksChanged(): Boolean {
        val width = lastViewport.second.first - lastViewport.first.first
        val height = lastViewport.second.second - lastViewport.first.second
        val lastxmin = maxOf(0, ((lastViewport.first.first - bmin.first - extraRenderFac * width) / (bmax.first - bmin.first) * nrCuts[zoomLevel]).toInt())
        val lastxmax = minOf(nrCuts[zoomLevel] - 1, ((lastViewport.second.first - bmin.first + extraRenderFac * width) / (bmax.first - bmin.first) * nrCuts[zoomLevel]).toInt())
        val lastymin = maxOf(0, ((lastViewport.first.second - bmin.second - extraRenderFac * height) / (bmax.second - bmin.second) * nrCuts[zoomLevel]).toInt())
        val lastymax = minOf(nrCuts[zoomLevel] - 1, ((lastViewport.second.second - bmin.second + extraRenderFac * height) / (bmax.second - bmin.second) * nrCuts[zoomLevel]).toInt())
        return zoomLevel != lastZoomLevel || xmin != lastxmin || xmax != lastxmax || ymin != lastymin || ymax != lastymax
    }
}