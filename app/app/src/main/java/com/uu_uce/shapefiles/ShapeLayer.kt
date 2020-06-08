package com.uu_uce.shapefiles

/*
a layer to be displayed in the map, consisting of multiple shapes
chunkGetter: means of getting chunks associated with this layer
map: the map this layer is part of
hasInfo: temporary indicator whether this layer has an info file associated with it
 */
class ShapeLayer(private val chunkGetter: ChunkGetter, zoomCutoff: Float){
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()
    private val chunkManager: ChunkManager

    var bmin: Triple<Float,Float,Float>
        private set
    var bmax: Triple<Float,Float,Float>
        private set

    //setup the chunk and bounding box information
    init{
        val info = chunkGetter.readInfo()
        bmin = info.first
        bmax = info.second

        chunkManager = ChunkManager(chunks, chunkGetter, bmin, bmax, chunkGetter.nrCuts, zoomCutoff)
    }

    fun setzooms(minzoom: Float, maxzoom: Float){
        chunkManager.setZooms(minzoom, maxzoom)
    }

    fun updateChunks(viewport: Pair<p2,p2>, zoom: Float): ChunkUpdateResult {
        return chunkManager.update(viewport, zoom)
    }

    fun getZoomLevel() : Int {
        return chunkManager.zoomLevel
    }

    fun getMods() : List<Int> {
        return chunkGetter.mods
    }

    //draw all chunks associated with this layer
    fun draw(lineProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray){

        synchronized(chunks) {
            for(chunk in chunks.values) {
                chunk.draw(lineProgram, varyingColorProgram, scale, trans, color)
            }
        }
    }
}