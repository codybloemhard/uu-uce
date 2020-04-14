package com.uu_uce.shapefiles

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class ChunkUpdateResult{NOTHING, REDRAW, LOADING}

//manages the loading and unloading of chunks
abstract class ChunkManager(
    protected val chunks: MutableMap<Triple<Int, Int, Int>, Chunk>,
    protected val chunkGetter: ChunkGetter,
    protected val map: ShapeMap)
{
    protected var lastViewport: Pair<p2,p2> = Pair(p2Zero,p2Zero)
    protected var lastZoom: Int = -1

    abstract fun update(viewport: Pair<p2,p2>, zoom: Int): ChunkUpdateResult

    open fun updateOnMove(viewport: Pair<p2,p2>, zoom: Int){}
    open fun updateOnStop(viewport: Pair<p2,p2>, zoom: Int){}

    protected fun shouldGetLoaded(chunkIndex: ChunkIndex, viewport: Pair<p2,p2>, zoom: Int): Boolean{
        return chunkIndex.third == zoom
    }

    protected fun chunksChanged(viewport: Pair<p2,p2>, zoom: Int): Boolean{
        return zoom != lastZoom
    }
}

class ScrollingLoader(chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, chunkGetter: ChunkGetter, map: ShapeMap): ChunkManager(chunks, chunkGetter, map){
    private val toRemove: HashSet<ChunkIndex> = hashSetOf()
    private val chunkLoaders: MutableList<Pair<ChunkIndex,Job>> = mutableListOf()

    private fun addChunks(chunkIndices: List<ChunkIndex>){
        val routines: MutableList<Job> = mutableListOf()
        for (chunkIndex in chunkIndices) {
            val routine = GlobalScope.launch {
                val time = System.currentTimeMillis()
                val c: Chunk = chunkGetter.getChunk(chunkIndex)
                synchronized(chunks) {
                    chunks[chunkIndex] = c
                }
                map.invalidate()
                Logger.log(
                    LogType.Event,
                    "ChunkManager",
                    "loaded chunk $chunkIndex in time: ${System.currentTimeMillis() - time}"
                )
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
                    chunks.keys.removeAll(toRemove)
                }
                synchronized(toRemove) {
                    toRemove.clear()
                }
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

    override fun update(viewport: Pair<p2, p2>, zoom: Int): ChunkUpdateResult {
        TODO("Not yet implemented")
    }

    override fun updateOnMove(viewport: Pair<p2,p2>, zoom: Int){
        if(!chunksChanged(viewport,zoom)) return
        chunkLoaders.filter{(index,routine) ->
            if(!shouldGetLoaded(index, viewport, zoom))
                routine.cancel()
            (routine.isCancelled || routine.isCompleted)
        }

        val (newChunks,oldChunks) = getNewOldChunks(viewport, zoom)

        synchronized(toRemove) {
            toRemove.addAll(oldChunks)
            toRemove.removeAll(newChunks)
        }

        addChunks(newChunks)
    }
}

//loads new chunks only when the user is not moving the camera
//less smooth, but also less resource intensive
class StopLoader(chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, chunkGetter: ChunkGetter, map: ShapeMap): ChunkManager(chunks, chunkGetter, map){
    private var chunkLoaders: List<Pair<ChunkIndex,Job>> = listOf()
    private var chunksLoadedListener: Job? = null
    private var loading = false
    private var upToDate = false
    private var changed = false

    private fun cancelCurrentLoading(){
        synchronized(chunks) {
            chunksLoadedListener?.cancel()

            for ((_, job) in chunkLoaders) {
                job.cancel()
            }
            loading = false
        }
    }

    override fun update(viewport: Pair<p2, p2>, zoom: Int): ChunkUpdateResult {
        if(viewport != lastViewport || zoom != lastZoom) {
            Logger.log(LogType.Event, "ChunkManager", "camera moved, not updating chunks")
            cancelCurrentLoading()
            upToDate = !chunksChanged(viewport,zoom) && upToDate
            lastViewport = viewport
            lastZoom = zoom
            return  if(upToDate) ChunkUpdateResult.NOTHING
                    else ChunkUpdateResult.LOADING
        }

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
                chunks.keys.removeAll{index -> !shouldGetLoaded(index, viewport, zoom) }
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
        return listOf(ChunkIndex(0,0,zoom))
    }
}