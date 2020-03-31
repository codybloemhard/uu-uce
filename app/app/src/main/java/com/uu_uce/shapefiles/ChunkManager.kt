package com.uu_uce.shapefiles

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.launch

abstract class ChunkManager(protected val chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, protected val chunkGetter: ChunkGetter){
    open fun updateOnMove(viewport: Pair<p2,p2>, zoom: Int, map: ShapeMap){}
    open fun updateOnTouchRelease(viewport: Pair<p2,p2>, zoom: Int, map: ShapeMap){}
}

@InternalCoroutinesApi
class ScrollingLoader(chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, chunkGetter: ChunkGetter): ChunkManager(chunks, chunkGetter){
    private val toRemove: HashSet<ChunkIndex> = hashSetOf()
    private val chunkLoaders: MutableList<Pair<ChunkIndex,Job>> = mutableListOf()
    private var lastViewport: Pair<p2,p2> = Pair(p2Zero,p2Zero)
    private var lastZoom: Int = -1

    private fun getNewOldChunks(viewport: Pair<p2,p2>, zoomLevel: Int) : Pair<List<ChunkIndex>,List<ChunkIndex>>{
        val new: MutableList<ChunkIndex> = mutableListOf()
        if(zoomLevel != lastZoom) new.add(Triple(0,0,zoomLevel))

        val old: MutableList<Triple<Int,Int,Int>> = mutableListOf()
        if(zoomLevel!= lastZoom)old.add(Triple(0,0,lastZoom))

        return Pair(new.toList(),old.toList())
    }

    override fun updateOnMove(viewport: Pair<p2,p2>, zoom: Int, map: ShapeMap){
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

        lastViewport = viewport
        lastZoom = zoom
    }

    private fun shouldGetLoaded(chunkIndex: ChunkIndex, viewport: Pair<p2,p2>, zoom: Int): Boolean{
        return chunkIndex.third == zoom
    }
}

@InternalCoroutinesApi
class StopLoader(chunks: MutableMap<Triple<Int, Int, Int>, Chunk>, chunkGetter: ChunkGetter): ChunkManager(chunks, chunkGetter){
    private val chunkLoaders: MutableList<Pair<ChunkIndex,Job>> = mutableListOf()

    override fun updateOnMove(viewport: Pair<p2, p2>, zoom: Int, map: ShapeMap) {
        for((_,job) in chunkLoaders){
            job.cancel()
        }
        chunkLoaders.clear()
    }

    override fun updateOnTouchRelease(viewport: Pair<p2, p2>, zoom: Int, map: ShapeMap){
        for((_,job) in chunkLoaders){
            job.cancel()
        }
        chunkLoaders.clear()


        val activeChunks = getActiveChunks(viewport, zoom)
        val loadedChunks: MutableList<Chunk?> = MutableList(activeChunks.size){null}

        val currentLoaders = List(activeChunks.size) {i ->
            val chunkIndex = activeChunks[i]
            val job = GlobalScope.launch {
                val c: Chunk = chunkGetter.getChunk(chunkIndex)
                loadedChunks[i] = c
            }
            val pair = Pair(chunkIndex,job)
            chunkLoaders.add(pair)
            pair
        }
        GlobalScope.launch{
            for((_,job) in currentLoaders) {
                job.join()
                if(job.isCancelled) {
                    return@launch
                }
            }

            //screen has been held still for long enough, jobs are all finished
            synchronized(chunks) {
                chunks.clear()

                for (i in currentLoaders.indices) {
                    val index = activeChunks[i]
                    val chunk = loadedChunks[i] ?: continue
                    chunks[index] = chunk

                }
            }
        }
    }

    fun getActiveChunks(viewport: Pair<p2,p2>, zoom: Int): List<ChunkIndex>{
        return listOf(ChunkIndex(0,0,zoom))
    }
}
