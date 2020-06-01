package com.uu_uce.shapefiles

import kotlin.math.pow
import kotlin.math.sqrt

fun distXy(p: p3, q: p3): Float{
    return sqrt((p.first - q.first).pow(2) +
            (p.second - q.second).pow(2))
}

fun lerp(a: Float, b: Float, t: Float): Float{
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
    private var x: Float,
    private var y: Float,
    private var zoom: Float,
    private val viewMin: p3,
    private val viewMax: p3){

    private val mx = (viewMin.first + viewMax.first) / 2.0f
    private val my = (viewMin.second + viewMax.second) / 2.0f
    private val maxDistXy = distXy(viewMin, viewMax)
    private var velo = p2Zero

    var maxZoom = 1.0f
        set(value) {minZoom = value/500; field = value}
    var minZoom = 0.01f

    private var lastWoff = 0.0f
    private var lastHoff = 0.0f
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
    private var animDuration = 0.0f
    private var animStartT = 0.0f
    private var animT = 0.0f

    //variables for sliding camera
    private var decline = p2(1.0f,1.0f)
    private var declineLength = 40.0f

    var wAspect = 0.0f

    //get matrix for drawing lines
    fun getScaleTrans(): Pair<FloatArray,FloatArray>{
        val trans = floatArrayOf(-x.toFloat(), -y.toFloat())

        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val maxwh = maxOf(w,h)
        val width = (maxwh * wAspect / 2.0 * zoom).toFloat()
        val height = (maxwh / 2.0 * zoom).toFloat()
        val scale = floatArrayOf(1f/width, 1f/height)
        return Pair(scale, trans)
    }

    //retrieve the topleft and bottomright coordinates that are visible in the camera
    fun getViewport(): Pair<p2,p2>{
        //if camera is not initialized properly, return dummy value
        if (viewMax.first < viewMin.first || viewMax.second < viewMin.second) {
            return p2ZeroPair
        }

        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val maxwh = maxOf(w,h)
        val woff = maxwh * wAspect / 2.0f * zoom
        val hoff = maxwh / 2.0f * zoom
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

    private fun setXy(xx: Float, yy: Float){
        changed = changed || xx != x || yy != y
        x = xx
        y = yy
    }

    //set the x and y to new values, while not going out of bounds
    private fun setPos(newX: Float, newY: Float){
        if(isBusy() || viewMin.first > viewMax.first || viewMin.second > viewMax.second) return
        val xvalue = newX.coerceIn(viewMin.first,viewMax.first)
        val yvalue = newY.coerceIn(viewMin.second,viewMax.second)
        setXy(xvalue,yvalue)
    }

    fun moveCamera(dx: Float, dy: Float){
        if(isBusy()) return
        setPos(x + (dx * lastWoff), y + (dy * lastHoff))

        animType = AnimType.NONE
        velo = p2((dx * lastWoff), (dy * lastHoff))
        changed = true
        decline = p2(velo.first/declineLength,velo.second/declineLength)
    }

    fun flingCamera(){
        if(isBusy()) return
        animType = AnimType.SLIDE
    }

    fun getZoom(): Float{
        return zoom
    }

    private fun setZ(zz: Float){
        changed = changed || zoom != zz
        zoom = zz
    }

    fun setZoom(newZoom: Float){
        if(isBusy()) return
        setZ(newZoom.coerceIn(minZoom, maxZoom))
    }

    fun zoomIn(factor: Float){
        if(isBusy()) return
        val z = zoom * factor
        setZ(z.coerceIn(minZoom, maxZoom))
    }

    //fully zoom out
    fun zoomOutMax(duration: Float){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = Triple(mx,my,maxZoom)
        animStartT = System.currentTimeMillis().toFloat()
        animDuration = duration
        animType = AnimType.OUT
        animT = 0.0f
    }

    //initialize the animation from current position to target in durationMs milisecs
    fun startAnimation(target: p3, durationMs: Float){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = target
        animStartT = System.currentTimeMillis().toFloat()
        animDuration = durationMs
        animType = AnimType.TRANS
        animT = 0.0f
    }

    private fun smooth(t: Float): Float{
        return -(t - 1.0f).pow(2.0f) + 1.0f
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
        val ct = System.currentTimeMillis().toFloat()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0f, 1.0f)
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
        val ct = System.currentTimeMillis().toFloat()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0f, 1.0f)
        val distFraction = distXy(animBegin, animTarget) / maxDistXy
        val zoomAvg = (animBegin.third + animTarget.third) / 2.0f
        val midZoom = (maxZoom - zoomAvg)*distFraction + zoomAvg
        val zt = 1.0f/3.0f
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
            if(decline.first > 0) maxOf(0.0f,velo.first - decline.first)
            else minOf(0.0f,velo.first - decline.first)
        val newYVel =
            if(decline.second > 0) maxOf(0.0f,velo.second - decline.second)
            else minOf(0.0f,velo.second - decline.second)
        velo = p2(newXVel,newYVel)
        if(velo == p2Zero) {
            animType = AnimType.NONE
        }
    }
}