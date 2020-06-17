package com.uu_uce.shapefiles

import com.uu_uce.misc.Logger
import com.uu_uce.views.CustomMap
import org.jetbrains.annotations.TestOnly

typealias p2 = Pair<Float, Float>
typealias p3 = Triple<Float,Float,Float>

val p2Zero = Pair(0.0f,0.0f)
val p2ZeroPair = Pair(p2Zero,p2Zero)
val p3Min = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
val p3Zero = Triple(0.0f,0.0f,0.0f)
val p3NaN = Triple(Float.NaN, Float.NaN, Float.NaN)

//merge a list of bounding boxes in one big one containing them all
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

/*
a map to be displayed in the app, consisting of multiple layers
view: the view this is displayed in
 */
class ShapeMap(
               private val view: CustomMap
){
    private var layerMask = mutableListOf<Boolean>()

    private var layers = mutableListOf<Pair<LayerType,ShapeLayer>>()
    private var bMin = p3Zero
    private var bMax = p3Zero

    private val layerColors : List<FloatArray>

    private lateinit var camera: Camera

    init{
        layerColors = List(LayerType.values().size){ i ->
            when(i){
                LayerType.Water.value -> {
                    floatArrayOf(0.1f, 0.2f, 0.8f, 1.0f)
                }
                LayerType.Height.value -> {
                    floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
                }
                LayerType.Vegetation.value -> {
                    floatArrayOf(0.2f, 0.8f, 0.2f, 1.0f)
                }
                else ->{
                    floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
                }
            }
        }
    }

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

    fun getMods() : List<Int> {
        var i = 0
        while(i < layers.count() && layers[i].first != LayerType.Height) i++
        return if(i == layers.count()) {
            Logger.error("ShapeMap", "No heightlines found")
            listOf()
        } else layers[i].second.getMods()
    }

    fun addLayer(type: LayerType, chunkGetter: ChunkGetter, zoomCutoff: Float){
        layers.add(Pair(type,ShapeLayer(chunkGetter, zoomCutoff)))
        layerMask.add(true)
    }

    fun removeLayers(){
        layers = mutableListOf()
        layerMask = mutableListOf()
    }

    //create a camera with the correct bounding box
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

    fun toggleLayer(l: Int){
        layerMask[l] = !layerMask[l]
        invalidate()
    }

    fun layerVisible(l : Int) : Boolean {
        return layerMask[l]
    }

    private fun invalidate(){
        camera.forceChanged()
        view.requestRender()
    }

    //update chunks of all layers
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

    //draw all layers
    fun draw(lineProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray){
        for(i in layers.indices) {
            if(!layerMask[i]) continue
            val (t,layer) = layers[i]

            layer.draw(lineProgram, varyingColorProgram, scale, trans, layerColors[t.value])
        }
    }

    @TestOnly
    fun checkLayerVisibility(layer : Int) : Boolean{
        return layerMask[layer]
    }
}

enum class LayerType(val value: Int){
    Vegetation(0), Height(1), Water(2),Lines(3)
}

