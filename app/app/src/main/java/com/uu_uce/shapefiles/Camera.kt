package com.uu_uce.shapefiles

class Camera(
    private var x: Double,
    private var y: Double,
    private var zoom: Double,
    private val viewMin: p3,
    private val viewMax: p3){

    var maxZoom = 1.0
    var minZoom = 0.0000000001

    fun getViewport(waspect: Double): Pair<p3,p3>{
        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val woff = w * waspect / 2.0 * zoom
        val hoff = h / 2.0 * zoom
        val nmin = Triple(x - woff, y - hoff, Double.MIN_VALUE)
        val nmax = Triple(x + woff, y + hoff, Double.MAX_VALUE)
        return Pair(nmin, nmax)
    }

    fun setPos(newX: Double, newY: Double){
        x = newX.coerceIn(viewMin.first, viewMax.first)
        y = newY.coerceIn(viewMin.second, viewMax.second)
    }

    fun getZoom(): Double{
        return zoom
    }

    fun setZoom(newZoom: Double){
        zoom = newZoom.coerceIn(minZoom, maxZoom)
    }

    fun zoomIn(factor: Double){
        zoom *= factor
        zoom = zoom.coerceIn(minZoom, maxZoom)
    }
}