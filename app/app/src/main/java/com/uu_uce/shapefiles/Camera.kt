package com.uu_uce.shapefiles

enum class AnimType{
    NONE, TRANS
}

class Camera(
    private var x: Double,
    private var y: Double,
    private var zoom: Double,
    private val viewMin: p3,
    private val viewMax: p3){

    private val mx = (viewMin.first + viewMax.first) / 2.0
    private val my = (viewMin.second + viewMax.second) / 2.0

    var maxZoom = 1.0
    var minZoom = 0.0000000001

    private var lastWoff = 0.0
    private var lastHoff = 0.0

    private var animType: AnimType = AnimType.NONE
    private var animBegin = p3Zero
    private var animTarget = p3Zero
    private var animDuration = 0.0
    private var animStartT = 0.0

    fun getViewport(waspect: Double): Pair<p3,p3>{
        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val woff = w * waspect / 2.0 * zoom
        val hoff = h / 2.0 * zoom
        lastWoff = woff
        lastHoff = hoff
        val nmin = Triple(x - woff, y - hoff, Double.MIN_VALUE)
        val nmax = Triple(x + woff, y + hoff, Double.MAX_VALUE)
        return Pair(nmin, nmax)
    }

    private fun isBusy(): Boolean{
        return animType != AnimType.NONE
    }

    fun setPosCenter(){
        if(isBusy()) return
        x = mx
        y = my
    }

    fun setPos(newX: Double, newY: Double){
        if(isBusy()) return
        val minx = viewMin.first + lastWoff
        val maxx = viewMax.first - lastWoff
        val miny = viewMin.second + lastHoff
        val maxy = viewMax.second - lastHoff
        if(minx >= maxx || miny >= maxy){
            setPosCenter()
        }else{
            x = newX.coerceIn(minx, maxx)
            y = newY.coerceIn(miny, maxy)
        }
    }

    fun moveView(dx: Double, dy: Double){
        if(isBusy()) return
        setPos(x + (dx * lastWoff), y + (dy * lastHoff))
    }

    fun getZoom(): Double{
        return zoom
    }

    fun setZoom(newZoom: Double){
        if(isBusy()) return
        zoom = newZoom.coerceIn(minZoom, maxZoom)
    }

    fun zoomIn(factor: Double){
        if(isBusy()) return
        zoom *= factor
        zoom = zoom.coerceIn(minZoom, maxZoom)
    }

    fun zoomOutMax(){
        if(isBusy()) return
        startAnimation(Triple(mx,my,maxZoom), 500.0)
    }

    fun startAnimation(target: p3, durationMs: Double){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = target
        animStartT = System.currentTimeMillis().toDouble()
        animDuration = durationMs
        animType = AnimType.TRANS
    }

    fun update(){
        if(!isBusy()) return
        val ct = System.currentTimeMillis().toDouble()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0, 1.0)
        x = animBegin.first + (animTarget.first - animBegin.first) * t
        y = animBegin.second + (animTarget.second - animBegin.second) * t
        zoom = animBegin.third + (animTarget.third - animBegin.third) * t
        if(ct > animStartT + animDuration){
            animType = AnimType.NONE
            return
        }
    }
}