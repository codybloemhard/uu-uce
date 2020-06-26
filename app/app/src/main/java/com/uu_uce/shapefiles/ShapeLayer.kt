package com.uu_uce.shapefiles

/**
 * a layer to be displayed in the map, consisting of multiple shapes
 * @param[chunkGetter] means of getting chunks associated with this layer
 * @param[zoomCutoff] the zoom level above which this layer should not be drawn
 * @constructor create a ShapeLayer, and setup chunk and boundingbox information
 *
 * @property[chunks] all chunks currently loaded
 * @property[chunkManager] makes sure the right chunks are loaded
 * @property[bmin] bottom left of bounding box]
 * @property[bmax] top right of bounding box]
 */
class ShapeLayer(private val chunkGetter: ChunkGetter, zoomCutoff: Float){
    private val chunks: MutableMap<Triple<Int, Int, Int>, Chunk> = mutableMapOf()
    private val chunkManager: ChunkManager

    var bmin: Triple<Float,Float,Float>
        private set
    var bmax: Triple<Float,Float,Float>
        private set

    init {
        val info = chunkGetter.readInfo()
        bmin = info.first
        bmax = info.second

        chunkManager = ChunkManager(chunks, chunkGetter, bmin, bmax, chunkGetter.nrCuts, zoomCutoff)
    }

    /**
     * give the min/max zoom from the camera to the chunk manager
     * @param[minzoom] minimum zoom level of the camera
     * @param[maxzoom] maximum zoom level of the camera
     */
    fun setzooms(minzoom: Float, maxzoom: Float) {
        chunkManager.setZooms(minzoom, maxzoom)
    }

    /**
     * update the chunk manager
     * @param[viewport] current viewport of the camera
     * @param[zoom] current zoom of the camera
     */
    fun updateChunks(viewport: Pair<p2, p2>, zoom: Float): ChunkUpdateResult {
        return chunkManager.update(viewport, zoom)
    }

    /**
     * @return the current zoomlevel of the chunkManager
     */
    fun getZoomLevel(): Int {
        return chunkManager.zoomLevel
    }

    /**
     * @return this layers modulos (only relevant for heightlines)
     */
    fun getMods(): List<Int> {
        return chunkGetter.mods
    }

    /**
     * draw all chunks in this layers
     * @param[uniColorProgram] the GL program to draw unicolor shapes with
     * @param[varyingColorProgram] the GL program to draw different colored shapes with
     * @param[scale] scale vector used to draw everything at the right size
     * @param[trans] translation vector to draw everything in the right place
     */
    fun draw(uniColorProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray) {
        synchronized(chunks) {
            for (chunk in chunks.values) {
                chunk.draw(uniColorProgram, varyingColorProgram, scale, trans)
            }
        }
    }
}


