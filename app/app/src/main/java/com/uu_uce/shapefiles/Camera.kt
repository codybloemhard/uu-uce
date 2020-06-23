package com.uu_uce.shapefiles

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * distance between to points
 * @param[p] first point
 * @param[q] second point
 * @return distance between p and q
 */
fun distXy(p: p3, q: p3): Float{
    return sqrt((p.first - q.first).pow(2) +
            (p.second - q.second).pow(2))
}

/**
 * linear interpolation between a and b
 * @param[a] interpolation start
 * @param[b] interpolation end
 * @param[t] progress between 0..1
 */
fun lerp(a: Double, b: Double, t: Double): Double{
    return a + (b - a) * t
}

/**
 * different types of camera animation
 */
enum class AnimType{
    NONE, TRANS, OUT, SLIDE
}

/**
 * possible results after updating the camera
 */
enum class UpdateResult{
    NOOP, REDRAW, ANIM
    }

/**
 * basic camera used to track which part of the world we are viewing
 * @param[x] position of the middle point of the camera
 * @param[y] position of the middle point of the camera
 * @param[zoom]: current zoom/height
 * @param[viewMin] lower left bound of the currently loaded layers, which the camera can't leave
 * @param[viewMax] upperright bound of the currently loaded layers, which the camera can't leave
 * @constructor creates a camera
 *
 * @property[mx] horizontal middle of the map
 * @property[my] vertical middle of the map
 * @property[maxDistXy] diagonal length of map
 * @property[velo] current sliding velocity
 * @property[maxZoom] farthest the camera can zoom out
 * @property[minZoom] farthest the camera can zoom in
 * @property[lastWoff] width of the camera in last update
 * @property[lastHoff] height of the camera in last update
 * @property[changed] whether the camera has changed
 * @property[animType] what animation is currently playing
 * @property[animBegin] start of the current animation
 * @property[animTarget] target of the curretn animation
 * @property[animDuration] duration of current animation
 * @property[animStartT] when the animation started
 * @property[animT] current time in the animation
 * @property[decline] speed at which velocity decreases
 * @property[declineLength] how many frames it should take to stop sliding
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
    private var animStartT = 0.0
    private var animT = 0.0

    //variables for sliding camera
    private var decline = p2(1.0f,1.0f)
    private var declineLength = 40.0f

    var wAspect = 0.0f

    /**
     * @return a pair of (scale,trans) vectors for drawing
     */
    fun getScaleTrans(): Pair<FloatArray,FloatArray>{
        val trans = floatArrayOf(-x, -y)

        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val maxwh = maxOf(w,h)
        val width = (maxwh * wAspect / 2.0 * zoom).toFloat()
        val height = (maxwh / 2.0 * zoom).toFloat()
        val scale = floatArrayOf(1f/width, 1f/height)
        return Pair(scale, trans)
    }

    /**
     * @return the bottomleft and topright coordinates that are visible in the camera
     */
    fun getViewport(): Pair<p2,p2>{
        //if camera is not initialized properly, return dummy value
        if (viewMax.first < viewMin.first || viewMax.second < viewMin.second || wAspect == 0.0f) {
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

    /**
     * @return whether the camera is currently animating
     */
    private fun isBusy(): Boolean{
        return !(animType == AnimType.NONE || animType == AnimType.SLIDE)
    }

    /**
     * @return whether the camera needs the screen to be redrawn
     */
    fun needsInvalidate(): Boolean{
        return changed || isBusy()
    }

    /**
     * make the camera realize something has changed
     */
    fun forceChanged(){
        changed = true
    }

    /**
     * set new x,y values
     * @param[xx] new x value
     * @param[yy] new y value
     */
    private fun setXy(xx: Float, yy: Float){
        changed = changed || xx != x || yy != y
        x = xx
        y = yy
    }

    /**
     * set the x and y to new values, while not going out of bounds
     * @param[newX] new x value
     * @param[newY] new y value
     */
    private fun setPos(newX: Float, newY: Float){
        if(isBusy() || viewMin.first > viewMax.first || viewMin.second > viewMax.second) return
        val xvalue = newX.coerceIn(viewMin.first,viewMax.first)
        val yvalue = newY.coerceIn(viewMin.second,viewMax.second)
        setXy(xvalue,yvalue)
    }

    /**
     * move the camera by (dy,dx)
     * @param[dx] amount to move horizontally
     * @param[dy] amount to move vertically
     */
    fun moveCamera(dx: Float, dy: Float){
        if(isBusy()) return
        setPos(x + (dx * lastWoff), y + (dy * lastHoff))

        animType = AnimType.NONE
        velo = p2((dx * lastWoff), (dy * lastHoff))
        changed = true
        decline = p2(velo.first/declineLength,velo.second/declineLength)
    }

    /**
     * fling the camera and set it to sliding animation
     */
    fun flingCamera(){
        if(isBusy()) return
        animType = AnimType.SLIDE
    }

    /**
     * @return current camera zoom
     */
    fun getZoom(): Float{
        return zoom
    }

    /**
     * set a new zoom value
     * @param[zz] new z value
     */
    private fun setZ(zz: Float){
        changed = changed || zoom != zz
        zoom = zz
    }

    /**
     * safely set a new zoom value
     * @param[newZoom] new zoom value
     */
    fun setZoom(newZoom: Float){
        if(isBusy()) return
        setZ(newZoom.coerceIn(minZoom, maxZoom))
    }

    /**
     * zoom in by a certain factor
     * @param[factor] factor to zoom in by
     */
    fun zoomIn(factor: Float){
        if(isBusy()) return
        val z = zoom * factor
        setZ(z.coerceIn(minZoom, maxZoom))
    }

    /**
     * fully zoom out
     * @param[duration] duration of zoomout animation
     */
    fun zoomOutMax(duration: Float){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = Triple(mx,my,maxZoom)
        animStartT = System.currentTimeMillis().toDouble()
        animDuration = duration
        animType = AnimType.OUT
        animT = 0.0
    }

    /**
     * initialize the animation from current position to target in durationMs milisecs
     * @param[target] the place to animate the camera to
     * @param[duration] duration of the animation
     */
    fun startAnimation(target: p3, duration: Float){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = target
        animStartT = System.currentTimeMillis().toDouble()
        animDuration = duration
        animType = AnimType.TRANS
        animT = 0.0
    }

    /**
     * make t go smoothly from 0..1 instead of linearly
     * @param[t] input number, should be between 0..1
     * @return t, but smooth
     */
    private fun smooth(t: Double): Double{
        return -(t - 1.0).pow(2.0) + 1.0
    }

    /**
     * updates the camera
     * @return whether or not the camera is still animating
     */
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

    /**
     * animates the zooming out of the camera
     */
    private fun updateOut(){
        val ct = System.currentTimeMillis().toDouble()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0, 1.0)
        x = (animBegin.first + (animTarget.first - animBegin.first) * t).toFloat()
        y = (animBegin.second + (animTarget.second - animBegin.second) * t).toFloat()
        zoom = (animBegin.third + (animTarget.third - animBegin.third) * t).toFloat()
        if(ct > animStartT + animDuration){
            animType = AnimType.NONE
            zoom = maxZoom
            return
        }
    }

    /**
     * animates moving the camera from animBegin to animTarget
     */
    private fun updateTrans(){
        val ct = System.currentTimeMillis().toDouble()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0, 1.0)
        val distFraction = distXy(animBegin, animTarget) / maxDistXy
        val zoomAvg = (animBegin.third + animTarget.third) / 2.0f
        val midZoom = (maxZoom - zoomAvg)*distFraction + zoomAvg
        val zt = 1.0f/3.0f
        when {
            t < zt -> {
                zoom = lerp(animBegin.third.toDouble(), midZoom.toDouble(), smooth(t / zt)).toFloat()
            }
            t < 1-zt -> {
                val tt = (t - zt) / (1 - 2*zt)
                x = (animBegin.first + (animTarget.first - animBegin.first) * tt).toFloat()
                y = (animBegin.second + (animTarget.second - animBegin.second) * tt).toFloat()
            }
            else -> {
                x = animTarget.first
                y = animTarget.second
                zoom = lerp(animTarget.third.toDouble(),midZoom.toDouble(), 1 - smooth((t - (1 - zt)) / zt)).toFloat()
            }
        }

        if(ct > animStartT + animDuration){
            animType = AnimType.NONE
            return
        }
    }

    /**
     * updates sliding the camera after being flung
     */
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