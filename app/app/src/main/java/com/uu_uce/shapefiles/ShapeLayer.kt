package com.uu_uce.shapefiles

/*
a layer to be displayed in the map, consisting of multiple shapes
chunkGetter: means of getting chunks associated with this layer
map: the map this layer is part of
hasInfo: temporary indicator whether this layer has an info file associated with it
 */
class ShapeLayer(private val chunkGetter: ChunkGetter, hasInfo: Boolean){
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()
    private val chunkManager: ChunkManager

    var bmin: p3
        private set
    var bmax: p3
        private set

    //setup the chunk and bounding box information
    init{
        val info = chunkGetter.readInfo()
        bmin = info.first
        bmax = info.second

        chunkManager = ChunkManager(chunks, chunkGetter, bmin, bmax, chunkGetter.nrCuts)
    }

    fun setzooms(minzoom: Double, maxzoom: Double){
        chunkManager.setZooms(minzoom, maxzoom)
    }

    fun updateChunks(viewport: Pair<p2,p2>, zoom: Double): ChunkUpdateResult {
        return chunkManager.update(viewport, zoom)
    }

    fun getZoomLevel() : Int {
        return chunkManager.getZoomLevel()
    }

    fun getMods() : List<Int> {
        return chunkGetter.mods
    }

    //draw all chunks associated with this layer
    fun draw(lineProgram: Int, polygonProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray){

        synchronized(chunks) {
            var nrShapes = 0
            var nrLines = 0
            for(chunk in chunks.values) {
                chunk.draw(lineProgram, polygonProgram, scale, trans, color)

                nrShapes += chunk.shapes.size
                for(shape in chunk.shapes){
                    nrLines+=shape.nrPoints-1
                }
            }
        }
    }
}