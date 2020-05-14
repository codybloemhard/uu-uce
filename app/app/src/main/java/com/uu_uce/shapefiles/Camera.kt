package com.uu_uce.shapefiles

import kotlin.math.pow
import kotlin.math.sqrt

fun distXy(p: p3, q: p3): Double{
    return sqrt((p.first - q.first).pow(2) +
            (p.second - q.second).pow(2))
}

fun lerp(a: Double, b: Double, t: Double): Double{
    return a + (b - a) * t
}

enum class AnimType{
    NONE, TRANS, OUT, SLIDE
}

enum class UpdateResult{
    NOOP, REDRAW, ANIM
    }

/*
basic camera used to track which part of the world we are viewing
x,y: position of the middle point of the camera
zoom: current zoom/height
viewMin/viewMax: bounds of the currently loaded layers, which the camera can't leave
 */
class Camera(
    private var x: Double,
    private var y: Double,
    private var zoom: Double,
    private val viewMin: p3,
    val viewMax: p3){

    private val mx = (viewMin.first + viewMax.first) / 2.0
    private val my = (viewMin.second + viewMax.second) / 2.0
    private val maxDistXy = distXy(viewMin, viewMax)
    private var velo = p2Zero

    var maxZoom = 1.0
        set(value) {minZoom = value/500; field = value}
    var minZoom = 0.01

    private var lastWoff = 0.0
    private var lastHoff = 0.0
    private var changed = true

    private var animType: AnimType = AnimType.NONE
    private set(value){
        when(value){
            AnimType.SLIDE -> {}
            else -> { velo = p2Zero}
        }
        field = value
    }

    private var animBegin = p3Zero
    private var animTarget = p3Zero
    private var animDuration = 0.0
    private var animStartT = 0.0
    private var animT = 0.0

    //variables for sliding camera
    private var decline = p2(1.0,1.0)
    private var declineLength = 40.0

    var wAspect = 0.0

    //get matrix for drawing lines
    fun getScaleTrans(): Pair<FloatArray,FloatArray>{
        val trans = floatArrayOf(-x.toFloat(), -y.toFloat())

        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val width = (w * wAspect / 2 * zoom).toFloat()
        val height = (h / 2 * zoom).toFloat()
        val scale = floatArrayOf(1f/width, 1f/height)
        return Pair(scale, trans)
    }

    //retrieve the topleft and bottomright coordinates that are visible in the camera
    fun getViewport(): Pair<p2,p2>{
        //if camera is not initialized properly, return dummy value
        if (viewMax.first < viewMin.first || viewMax.second < viewMin.second || viewMax.third < viewMin.third) {
            return p2ZeroPair
        }

        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val woff = w * wAspect / 2.0 * zoom
        val hoff = h / 2.0 * zoom
        lastWoff = woff
        lastHoff = hoff
        val nmin = p2(x - woff, y - hoff)
        val nmax = p2(x + woff, y + hoff)
        return Pair(nmin, nmax)
    }

    //whether the camera is currently animating
    private fun isBusy(): Boolean{
        return !(animType == AnimType.NONE || animType == AnimType.SLIDE)
    }

    //whether the camera needs the screen to be redrawn
    fun needsInvalidate(): Boolean{
        return changed || isBusy()
    }

    fun forceChanged(){
        changed = true
    }

    private fun setXy(xx: Double, yy: Double){
        changed = changed || xx != x || yy != y
        x = xx
        y = yy
    }

    //set the x and y to new values, while not going out of bounds
    private fun setPos(newX: Double, newY: Double){
        if(isBusy() || viewMin.first > viewMax.first || viewMin.second > viewMax.second) return
        val xvalue = newX.coerceIn(viewMin.first,viewMax.first)
        val yvalue = newY.coerceIn(viewMin.second,viewMax.second)
        setXy(xvalue,yvalue)
    }

    fun moveCamera(dx: Double, dy: Double){
        if(isBusy()) return
        setPos(x + (dx * lastWoff), y + (dy * lastHoff))

        animType = AnimType.NONE
        velo = p2((dx * lastWoff), (dy * lastHoff))
        changed = true
        decline = p2(velo.first/declineLength,velo.second/declineLength)
    }

    fun flingCamera(){
        animType = AnimType.SLIDE
    }

    fun getZoom(): Double{
        return zoom
    }

    private fun setZ(zz: Double){
        changed = changed || zoom != zz
        zoom = zz
    }

    fun setZoom(newZoom: Double){
        if(isBusy()) return
        setZ(newZoom.coerceIn(minZoom, maxZoom))
    }

    fun zoomIn(factor: Double){
        if(isBusy()) return
        val z = zoom * factor
        setZ(z.coerceIn(minZoom, maxZoom))
    }

    //fully zoom out
    fun zoomOutMax(duration: Double){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = Triple(mx,my,maxZoom)
        animStartT = System.currentTimeMillis().toDouble()
        animDuration = duration
        animType = AnimType.OUT
        animT = 0.0
    }

    //initialize the animation from current position to target in durationMs milisecs
    fun startAnimation(target: p3, durationMs: Double){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = target
        animStartT = System.currentTimeMillis().toDouble()
        animDuration = durationMs
        animType = AnimType.TRANS
        animT = 0.0
    }

    private fun smooth(t: Double): Double{
        return -(t - 1.0).pow(2.0) + 1.0
    }

    //Updates and returns true if viewport has changed
    fun update(): UpdateResult{
        if(!changed && !isBusy()){
            return UpdateResult.NOOP
        }
        changed = false
        when(animType){
            AnimType.TRANS -> updateTrans()
            AnimType.OUT -> updateOut()
            AnimType.SLIDE -> updateSlide()
            AnimType.NONE -> {}
        }
        return if(isBusy() || changed)
            UpdateResult.ANIM
        else
            UpdateResult.REDRAW
    }

    //animate a full zoomout
    private fun updateOut(){
        val ct = System.currentTimeMillis().toDouble()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0, 1.0)
        x = animBegin.first + (animTarget.first - animBegin.first) * t
        y = animBegin.second + (animTarget.second - animBegin.second) * t
        zoom = animBegin.third + (animTarget.third - animBegin.third) * t
        if(ct > animStartT + animDuration){
            animType = AnimType.NONE
            zoom = maxZoom
            return
        }
    }

    //animate the movement to animTarget
    private fun updateTrans(){
        val ct = System.currentTimeMillis().toDouble()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0, 1.0)
        val distFraction = distXy(animBegin, animTarget) / maxDistXy
        val zoomAvg = (animBegin.third + animTarget.third) / 2.0
        val midZoom = (maxZoom - zoomAvg)*distFraction + zoomAvg
        val zt = 1.0/3.0
        when {
            t < zt -> {
                zoom = lerp(animBegin.third, midZoom, smooth(t / zt))
            }
            t < 1-zt -> {
                val tt = (t - zt) / (1 - 2*zt)
                x = animBegin.first + (animTarget.first - animBegin.first) * tt
                y = animBegin.second + (animTarget.second - animBegin.second) * tt
            }
            else -> {
                x = animTarget.first
                y = animTarget.second
                zoom = lerp(animTarget.third,midZoom, 1 - smooth((t - (1 - zt)) / zt))
            }
        }

        if(ct > animStartT + animDuration){
            animType = AnimType.NONE
            return
        }
    }

    private fun updateSlide(){
        changed = true
        setPos(x + velo.first, y + velo.second)
        val newXVel =
            if(decline.first > 0) maxOf(0.0,velo.first - decline.first)
            else minOf(0.0,velo.first - decline.first)
        val newYVel =
            if(decline.second > 0) maxOf(0.0,velo.second - decline.second)
            else minOf(0.0,velo.second - decline.second)
        velo = p2(newXVel,newYVel)
        if(velo == p2Zero) {
            animType = AnimType.NONE
        }
    }
}