package com.uu_uce.shapefiles

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

//manages the loading and unloading of chunks
abstract class ChunkManager(protected val chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, protected val chunkGetter: ChunkGetter){
    protected var lastViewport: Pair<p2,p2> = Pair(p2Zero,p2Zero)
    protected var lastZoom: Int = -1

    open fun updateOnMove(viewport: Pair<p2,p2>, zoom: Int, map: ShapeMap){
        lastViewport = viewport
        lastZoom = zoom
    }
    open fun updateOnTouchRelease(viewport: Pair<p2,p2>, zoom: Int, map: ShapeMap){}

    protected fun shouldGetLoaded(chunkIndex: ChunkIndex, viewport: Pair<p2,p2>, zoom: Int): Boolean{
        return chunkIndex.third == zoom
    }

    protected fun chunksChanged(viewport: Pair<p2,p2>, zoom: Int): Boolean{
        return zoom != lastZoom
    }
}

//attempts to load new chunks every time the camera moves (might not work properly with horizontal chunks)
class ScrollingLoader(chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, chunkGetter: ChunkGetter): ChunkManager(chunks, chunkGetter){
    private val toRemove: HashSet<ChunkIndex> = hashSetOf()
    private val chunkLoaders: MutableList<Pair<ChunkIndex,Job>> = mutableListOf()

    private fun getNewOldChunks(viewport: Pair<p2,p2>, zoomLevel: Int) : Pair<List<ChunkIndex>,List<ChunkIndex>>{
        val new: MutableList<ChunkIndex> = mutableListOf()
        if(zoomLevel != lastZoom) new.add(Triple(0,0,zoomLevel))

        val old: MutableList<Triple<Int,Int,Int>> = mutableListOf()
        if(zoomLevel!= lastZoom)old.add(Triple(0,0,lastZoom))

        return Pair(new.toList(),old.toList())
    }

    override fun updateOnMove(viewport: Pair<p2,p2>, zoom: Int, map: ShapeMap){
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

        val routines: MutableList<Job> = mutableListOf()
        for (chunkIndex in newChunks) {
            val routine = GlobalScope.launch {
                val time = System.currentTimeMillis()
                val c: Chunk = chunkGetter.getChunk(chunkIndex)
                synchronized(chunks) {
                    chunks[chunkIndex] = c
                }
                map.invalidate()
                Logger.log(LogType.Continuous, "ShapeLayer", "loadTime: ${System.currentTimeMillis() - time}")
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
        super.updateOnMove(viewport, zoom, map)
    }
}

//loads new chunks only when the user is not moving the camera
//less smooth, but much less resource intensive
class StopLoader(chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, chunkGetter: ChunkGetter): ChunkManager(chunks, chunkGetter){
    private var chunkLoaders: List<Pair<ChunkIndex,Job>> = listOf()
    private var chunksLoadedListener: Job? = null

    private fun cancelCurrentLoading(){
        synchronized(chunks) {
            if(chunksLoadedListener?.isActive == true)
                Logger.log(LogType.Continuous, "ChunkManager", "canceled listener")
            chunksLoadedListener?.cancel()

            for ((_, job) in chunkLoaders) {
                job.cancel()
            }
        }
    }

    override fun updateOnMove(viewport: Pair<p2, p2>, zoom: Int, map: ShapeMap) {
        if(!chunksChanged(viewport,zoom)) return
        cancelCurrentLoading()
        super.updateOnMove(viewport, zoom, map)
    }

    override fun updateOnTouchRelease(viewport: Pair<p2, p2>, zoom: Int, map: ShapeMap){
        cancelCurrentLoading()

        val activeChunks = getActiveChunks(viewport, zoom)
        val loadedChunks: MutableList<Chunk?> = MutableList(activeChunks.size){null}

        chunkLoaders = List(activeChunks.size) {i ->
            val chunkIndex = activeChunks[i]
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
                    val index = activeChunks[i]
                    val chunk = loadedChunks[i] ?: continue
                    chunks[index] = chunk
                }
            }
            map.invalidate()
        }
    }

    fun getActiveChunks(viewport: Pair<p2,p2>, zoom: Int): List<ChunkIndex>{
        return listOf(ChunkIndex(0,0,zoom))
    }
}
