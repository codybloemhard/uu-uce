package com.uu_uce.shapefiles

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

//manages the loading and unloading of chunks
abstract class ChunkManager(
    protected val chunks: MutableMap<Triple<Int, Int, Int>, Chunk>,
    protected val chunkGetter: ChunkGetter,
    protected val map: ShapeMap)
{
    protected var lastViewport: Pair<p2,p2> = Pair(p2Zero,p2Zero)
    protected var lastZoom: Int = -1

    open fun updateOnMove(viewport: Pair<p2,p2>, zoom: Int){
        lastViewport = viewport
        lastZoom = zoom
    }
    open fun updateOnStop(viewport: Pair<p2,p2>, zoom: Int){}

    protected fun shouldGetLoaded(chunkIndex: ChunkIndex, viewport: Pair<p2,p2>, zoom: Int): Boolean{
        return chunkIndex.third == zoom
    }

    protected fun chunksChanged(viewport: Pair<p2,p2>, zoom: Int): Boolean{
        return zoom != lastZoom
    }
}

//attempts to load new chunks every time the camera moves (might not work properly with horizontal chunks)
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

        super.updateOnMove(viewport, zoom)
    }
}

//loads new chunks only when the user is not moving the camera
//less smooth, but also less resource intensive
class StopLoader(chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, chunkGetter: ChunkGetter, map: ShapeMap): ChunkManager(chunks, chunkGetter, map){
    private var chunkLoaders: List<Pair<ChunkIndex,Job>> = listOf()
    private var chunksLoadedListener: Job? = null

    private fun cancelCurrentLoading(){
        synchronized(chunks) {
            if(chunksLoadedListener?.isActive == true)
                Logger.log(LogType.Continuous, "ChunkManager", "canceled listener ${chunkLoaders[0].first}")
            chunksLoadedListener?.cancel()

            for ((_, job) in chunkLoaders) {
                job.cancel()
            }
        }
    }

    override fun updateOnMove(viewport: Pair<p2, p2>, zoom: Int) {
        if(!chunksChanged(viewport,zoom)) return
        cancelCurrentLoading()
        super.updateOnMove(viewport, zoom)
    }

    fun addChunks(chunkIndices: List<ChunkIndex>, viewport: Pair<p2,p2>, zoom: Int){
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
            }
            map.invalidate()
        }
    }

    override fun updateOnStop(viewport: Pair<p2, p2>, zoom: Int){
        Logger.log(LogType.Event, "ChunkManager", "release")
        cancelCurrentLoading()

        val activeChunks = getActiveChunks(viewport, zoom)
        addChunks(activeChunks, viewport, zoom)

        Logger.log(LogType.Event, "ChunkManager", "loading ${activeChunks[0]}")
    }

    private fun getActiveChunks(viewport: Pair<p2,p2>, zoom: Int): List<ChunkIndex>{
        return listOf(ChunkIndex(0,0,zoom))
    }
}
