package com.uu_uce.shapefiles

import com.uu_uce.misc.Logger
import com.uu_uce.views.CustomMap
import org.jetbrains.annotations.TestOnly

//some useful types
typealias p2 = Pair<Float, Float>
typealias p3 = Triple<Float,Float,Float>
val p2Zero = Pair(0.0f,0.0f)
val p2ZeroPair = Pair(p2Zero,p2Zero)
val p3Zero = Triple(0.0f,0.0f,0.0f)
val p3NaN = Triple(Float.NaN, Float.NaN, Float.NaN)

/**
 * merge a list of bounding boxes in one big one containing them all
 * @param[mins] bottom lefts of all bounding boxes
 * @param[maxs] top rights of all bounding boxes
 * @return bounding box containing all other bounding boxes
 */
fun mergeBBs(mins: List<p3>,maxs: List<p3>): Pair<p3,p3>{
    var bmin = mutableListOf(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE)
    var bmax = mutableListOf(Float.MIN_VALUE,Float.MIN_VALUE,Float.MIN_VALUE)

    bmin = mins.fold(bmin, {bb, shapeZ ->
        bb[0] = minOf(shapeZ.first, bb[0])
        bb[1] = minOf(shapeZ.second, bb[1])
        bb[2] = minOf(shapeZ.third, bb[2])
        bb
    })
    bmax = maxs.fold(bmax, {bb, shapez ->
        bb[0] = maxOf(shapez.first, bb[0])
        bb[1] = maxOf(shapez.second, bb[1])
        bb[2] = maxOf(shapez.third, bb[2])
        bb
    })
    return Pair(p3(bmin[0],bmin[1],bmin[2]),p3(bmax[0],bmax[1],bmax[2]))
}

/**
 * a map to be displayed in the app, consisting of multiple layers
 * @param[customMap]: the customMap this is displayed in
 * @constructor creates a ShapeMap
 */
class ShapeMap(private val customMap: CustomMap){
    private var layerMask = mutableListOf<Boolean>()

    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()
    private var bMin = p3Zero
    private var bMax = p3Zero

    private lateinit var camera: Camera

    /**
     * give the min/max zoom from the camera to the layers
     * @param[minzoom] minimum zoom level of the camera
     * @param[maxzoom] maximum zoom level of the camera
     */
    fun setzooms(minzoom: Float, maxzoom: Float){
        for((_,layer) in layers){
            layer.setzooms(minzoom,maxzoom)
        }
    }

    fun getZoomLevel() : Int {
        var i = 0
        while(i < layers.count() && layers[i].first != LayerType.Height) i++
        return if(i == layers.count()) {
            Logger.error("ShapeMap", "No heightlines found")
            -1
        } else layers[i].second.getZoomLevel()
    }

    /**
     * @return the modulos of a heightline layer, if present
     */
    fun getMods() : List<Int> {
        var i = 0
        while(i < layers.count() && layers[i].first != LayerType.Height) i++
        return if(i == layers.count()) {
            Logger.error("ShapeMap", "No heightlines found")
            listOf()
        } else layers[i].second.getMods()
    }

    /**
     * add a new layer to this shape map
     * @param[type] type of the new layer
     * @param[chunkGetter] the chunkGetter to use for this layer
     * @param[zoomCutoff] the zoom level above which this layer should not be drawn
     */
    fun addLayer(type: LayerType, chunkGetter: ChunkGetter, zoomCutoff: Float){
        layers.add(Pair(type,ShapeLayer(chunkGetter, zoomCutoff)))
        layerMask.add(true)
    }

    /**
     * remove all layers
     */
    fun removeLayers(){
        layers = mutableListOf()
        layerMask = mutableListOf()
    }

    /**
     * @return a new camera, based on the layers currently present
     */
    fun createCamera(): Camera{
        val first: List<Triple<Float,Float,Float>> = layers.map{l -> l.second.bmin}
        val second:List<Triple<Float,Float,Float>> = layers.map{l -> l.second.bmax}
        val bminmax: Pair<p3,p3> = mergeBBs(
            first,
            second)
        bMin = bminmax.first
        bMax = bminmax.second
        val mx = (bMin.first + bMax.first) / 2.0f
        val my = (bMin.second + bMax.second) / 2.0f
        camera = Camera(mx, my, 1.0f, bMin, bMax)
        return camera
    }

    /**
     * toggle the visibility of a layer
     * @param[l] the layer to toggle
     */
    fun toggleLayer(l: Int){
        layerMask[l] = !layerMask[l]
        invalidate()
    }

    /**
     * check if a layer is visible
     * @param[l] the layer to check
     * @return true if the layer is visible, false otherwise
     */
    fun layerVisible(l : Int) : Boolean {
        return layerMask[l]
    }

    /**
     * ensures the map will be rendered again
     */
    private fun invalidate(){
        camera.forceChanged()
        customMap.requestRender()
    }

    /**
     * update the chunks of all layers
     * @param[viewport] the current viewport of the camera
     * @return the combined result of updating all chunks
     */
    fun updateChunks(viewport: Pair<p2,p2>): ChunkUpdateResult{
        var res = ChunkUpdateResult.NOTHING
        for(i in layers.indices){
            if(!layerMask[i]) continue
            val (_,layer) = layers[i]

            val zoom = camera.getZoom()
            val cur = layer.updateChunks(viewport, zoom)
            if(cur != ChunkUpdateResult.NOTHING)
                res = cur
            if(res == ChunkUpdateResult.LOADING) break
        }
        return res
    }

    /**
     * draw all layers
     * @param[uniColorProgram] the GL program to draw unicolor shapes with
     * @param[varyingColorProgram] the GL program to draw different colored shapes with
     * @param[scale] scale vector used to draw everything at the right size
     * @param[trans] translation vector to draw everything in the right place
     */
    fun draw(uniColorProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray){
        for(i in layers.indices) {
            if(!layerMask[i]) continue
            val (t,layer) = layers[i]

            layer.draw(uniColorProgram, varyingColorProgram, scale, trans)
        }
    }
}

/**
 * used to distinguish between the different types of layers
 */
enum class LayerType{
    Vegetation, Height, Water,Lines
}

