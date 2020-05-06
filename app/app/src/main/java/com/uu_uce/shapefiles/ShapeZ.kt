package com.uu_uce.shapefiles

import android.graphics.Paint
import android.opengl.GLES20
import com.uu_uce.OpenGL.coordsPerVertex
import com.uu_uce.misc.LinkedList
import com.uu_uce.misc.Logger
import com.uu_uce.misc.Node
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

//abstract class for drawing shapes
abstract class DrawInfo{
    abstract fun draw(program: Int, scale: FloatArray, trans: FloatArray, paint: Paint)
    abstract fun finalize()
}

//information for drawing a line
class LineDrawInfo(nrPoints: Int, nrLines: Int): DrawInfo(){
    private var vertices: FloatArray = FloatArray(nrPoints*4)
    var i = 0
    private var indices: ShortArray = ShortArray(nrLines*2)
    var j = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer
    var curIndex = 0.toShort()

    fun shapeLength(length: Int){

        for(k in 0 until length){
            indices[j++] = curIndex
            indices[j++] = ++curIndex
        }
        curIndex++
    }

    fun addVertex(item: Float){vertices[i++] = item}

    override fun draw(program: Int, scale: FloatArray, trans: FloatArray, paint: Paint) {
        val color = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

        GLES20.glUseProgram(program)

        // get handle to vertex shader's vPosition member
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex*4,
            vertexBuffer
        )

        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val scaleHandle = GLES20.glGetUniformLocation(program, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val transHandle = GLES20.glGetUniformLocation(program, "trans")
        GLES20.glUniform2fv(transHandle, 1, trans, 0)

        // Draw the triangle
        //GLES20.glDrawElements(GLES20.GL_LINES, lines.size, GLES20.GL_UNSIGNED_SHORT, 0)
        GLES20.glDrawElements(GLES20.GL_LINES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    override fun finalize() {
        vertexBuffer =
            // (# of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertices)
                    position(0)
                }
            }
        indexBuffer=
            // (# of coordinate values * 2 bytes per short)
            ByteBuffer.allocateDirect(indices.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(indices)
                    position(0)
                }
            }

    }
}

//information for drawing a polygon
class PolygonDrawInfo(nrVertices: Int, nrIndices: Int): DrawInfo(){
    private var vertices = FloatArray(nrVertices*4)
    private var v = 0
    private var indices = ShortArray(nrIndices)
    private var i = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    fun addVertex(item: Float) {vertices[v++]=item}
    fun addIndices(idcs: MutableList<Short>){
        for(index in idcs) indices[i++]=index
    }

    override fun draw(program: Int, scale: FloatArray, trans: FloatArray, paint: Paint) {
        //canvas.drawVertices(Canvas.VertexMode.TRIANGLES, vertices.size, vertices, 0, null, 0, colors, 0, indices, 0,indices.size, paint)
        val color = floatArrayOf(0.1f, 0.2f, 0.8f, 1.0f)

        GLES20.glUseProgram(program)

        // get handle to vertex shader's vPosition member
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex*4,
            vertexBuffer
        )

        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val scaleHandle = GLES20.glGetUniformLocation(program, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val transHandle = GLES20.glGetUniformLocation(program, "trans")
        GLES20.glUniform2fv(transHandle, 1, trans, 0)

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_LINES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    override fun finalize() {
        vertexBuffer =
                // (# of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertices)
                    position(0)
                }
            }
        val newIndices:MutableList<Short> = mutableListOf()
        for(i in indices.indices){
            if(i%3 != 0)continue
            newIndices.add(indices[i])
            newIndices.add(indices[i+1])
            newIndices.add(indices[i+1])
            newIndices.add(indices[i+2])
            newIndices.add(indices[i+2])
            newIndices.add(indices[i])
        }
        indexBuffer=
                // (# of coordinate values * 2 bytes per short)
            ByteBuffer.allocateDirect(newIndices.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(newIndices.toShortArray())
                    position(0)
                }
            }
    }
}

//generic shape
abstract class ShapeZ(var bmin: p3, var bmax: p3){
    abstract fun initDrawInfo(drawInfo: DrawInfo)
    abstract val nrPoints: Int
}

//shape consisting of just lines on the same height
class HeightShapeZ(private var points: List<p2>, bmi: p3, bma: p3): ShapeZ(bmi,bma) {
    override val nrPoints = points.size

    override fun initDrawInfo(
        drawInfo: DrawInfo
    ) {
        if (points.size < 2) return

        if(drawInfo is LineDrawInfo) {
            drawInfo.shapeLength(points.size-1)
            for (i in points.indices) {
                drawInfo.addVertex(points[i].first.toFloat())
                drawInfo.addVertex(points[i].second.toFloat())
            }
        }
        else Logger.error("ShapeZ", "wrong draw information for heightshape")
    }
}

//type used in triangulation
class PolyPoint(val point: p3, var reflex: Boolean, var ear: Boolean, val index: Int){
    var convex: Boolean
    get() { return !reflex}
        set(value) {reflex = !value}
}

//shape consisting of polygons that need to be colorized
class PolygonZ(outerRings: List<List<p3>>, private var innerRings: List<List<p3>>, bmi: p3, bma:p3): ShapeZ(bmi,bma){
    lateinit var indices: MutableList<Short>
    private var vertices: List<p3>
    override val nrPoints: Int

    init{
        vertices = outerRings[0]
        mergeInner()
        removeDoubles()
        triangulate(vertices)

        nrPoints = vertices.size
    }

    override fun initDrawInfo(
        drawInfo: DrawInfo
    ) {
        if(drawInfo is PolygonDrawInfo) {
            for (i in 0 until vertices.size) {
                drawInfo.addVertex(vertices[i / 2].first.toFloat())
                drawInfo.addVertex(vertices[i / 2].second.toFloat())
            }
            drawInfo.addIndices(indices)
        }
        else Logger.error("ShapeZ", "wrong draw information for polygon shape")

    }

    private fun mergeInner(){
        //merge inner ring with highest x coordinate first (this one can defneitely to see the outer ring)
        innerRings = innerRings.sortedByDescending{ring ->
            var rightmost = Double.MIN_VALUE
            for(point in ring){
                rightmost = maxOf(rightmost, point.first)
            }
            rightmost
        }

        //merge rings one by one
        for(innerRing in innerRings){
            //get rightmost point in inner ring
            var rightmost = p3Min
            var rightmostIndex = -1
            for(i in innerRing.indices) {
                val point = innerRing[i]
                if (point.first > rightmost.first) {
                    rightmost = point
                    rightmostIndex = i
                }
            }

            //calculate closest intersection with outer ring when going to the right
            var intersectIndex = -1 //second point on line of intersection
            var intersect = p3NaN //actual intersection point
            var bestDis = Double.MAX_VALUE
            val x3 = rightmost.first
            val y3 = rightmost.second
            //intersect with every line of the outer ring
            for(i in vertices.indices) {
                val x1 = vertices[i].first
                val y1 = vertices[i].second
                val x2 = vertices[(i + 1) % vertices.size].first
                val y2 = vertices[(i + 1) % vertices.size].second

                val t = (y3 - y1) / (y2 - y1)
                if (t < 0 || t > 1) continue

                val x = x1 + t * (x2 - x1)
                val curDis = x - x3
                if(curDis<0 || curDis >= bestDis) continue

                bestDis = curDis
                val z1 = vertices[i].third
                val z2 = vertices[(i + 1) % vertices.size].third
                val z = z1 + t * (z2 - z1)
                intersectIndex = (i + 1) % vertices.size
                intersect = p3(x,y3,z)
            }
            //intersection point is known: now add the original outer ring up to that point,
            //then the inner ring, then continue with the outer ring
            val newVertices: MutableList<p3> = mutableListOf()
            for(i in vertices.indices){
                val point = vertices[i]
                if(i == intersectIndex){
                    newVertices.add(intersect)

                    var k = rightmostIndex
                    var step = 0
                    while(step < innerRing.size){
                        newVertices.add(innerRing[k])
                        k = (k+1)%innerRing.size
                        step++
                    }

                    newVertices.add(innerRing[k])
                    newVertices.add(intersect)
                }
                newVertices.add(point)
            }
            vertices = newVertices
        }
    }

    private fun removeDoubles(){
        //shapefile specification allows duplicate points to occur right after each other..
        //useless information, we throw it away
        vertices = vertices.filterIndexed{i, p -> vertices[(i+1)%vertices.size] != p}
    }

    private fun triangulate(originalPolygon: List<p3>){
        if(originalPolygon.size < 3) throw Exception("Polygons can't have less than three sides")
        val l = List(originalPolygon.size){i -> PolyPoint(originalPolygon[i],
            reflex = false,
            ear = false,
            index = i
        )}
        val remainingPolygon = LinkedList(l,true)

        for(node in remainingPolygon){
            val res = isReflex(node)
            node.value.reflex = res
        }
        for(point in remainingPolygon){
            val res = isEar(remainingPolygon, point)
            point.value.ear = res
        }

        //indices into originalPolygon
        //three consecutive indices form a triangle
        indices = mutableListOf()

        var step = 0
        var cur = remainingPolygon.first!!
        while(remainingPolygon.size > 3){
            step++
            /*if(step>100001){
                var hasEar = false
                for(point in remainingPolygon){
                    if(point.value.ear != isEar(remainingPolygon,point))
                        throw Exception("weird0")
                    if(point.value.ear) hasEar = true
                }
                if(!hasEar) throw Exception("weird1")
            }*/

            if(!cur.value.ear) {
                cur = cur.next!!
                continue
            }

            //add new ear to found triangles
            indices.add(cur.prev!!.value.index.toShort())
            indices.add(cur.value.index.toShort())
            indices.add(cur.next!!.value.index.toShort())

            val prev = cur.prev!!
            remainingPolygon.remove(cur)

            update(remainingPolygon, prev)
            update(remainingPolygon, prev.next!!)

            if(step>3300) {
                for(point in remainingPolygon){
                    if(point.value.reflex != isReflex(point)) {
                        throw Exception("weird3")
                    }
                }
                for (point in remainingPolygon) {
                    if (point.value.ear != isEar(remainingPolygon, point)) {
                        throw Exception("weird4")
                    }
                }
            }

            cur = prev
        }
        for(point in remainingPolygon)
            indices.add(point.value.index.toShort())
    }

    private fun update(polygon: LinkedList<PolyPoint>, p: Node<PolyPoint>){
        if(p.value.convex) {
            p.value.ear = isEar(polygon, p)
            //convex points will stay convex
        }
        else{
            //reflex points might become convex
            p.value.reflex = isReflex(p)
            //..and might become an ear
            if(p.value.convex){
                p.value.ear = isEar(polygon, p)
            }
        }
    }

    private fun isReflex(p: Node<PolyPoint>): Boolean{
        //rotation of outer ring is always clockwise, inner always counter clockwise
        val ax = p.prev!!.value.point.first
        val ay = p.prev!!.value.point.second
        val bx = p.value.point.first
        val by = p.value.point.second
        val cx = p.next!!.value.point.first
        val cy = p.next!!.value.point.second
        return (bx - ax) * (cy - by) - (cx - bx) * (by - ay) > 0
    }

    private fun isEar(polygon: LinkedList<PolyPoint>, p: Node<PolyPoint>): Boolean{
        //an ear is a point of a triangle with no other points inside
        if(p.value.reflex) return false
        for(node in polygon){
            if(node == p.prev || node == p || node == p.next) continue
            if(isInsideTriangle(node.value.point, p.prev!!.value.point,p.value.point,p.next!!.value.point)) {
                return false
            }
        }
        return true
    }

    private fun isInsideTriangle(p:p3, p0:p3, p1:p3, p2:p3):Boolean
    {
        val x = p.first
        val y = p.second
        val x1 = p0.first
        val y1 = p0.second
        val x2 = p1.first
        val y2 = p1.second
        val x3 = p2.first
        val y3 = p2.second

        val denominator = ((y2 - y3)*(x1 - x3) + (x3 - x2)*(y1 - y3))
        val a = ((y2 - y3)*(x - x3) + (x3 - x2)*(y - y3)) / denominator
        val b = ((y3 - y1)*(x - x3) + (x1 - x3)*(y - y3)) / denominator
        val c = 1 - a - b

        //epsilon is needed because of how inner and outer polygons are merged because
        //there will be two exactly equal lines in the polygon, only in reversed order
        val e = 0.0001
        return (a in 0.0+e..1.0-e) && (b in 0.0+e..1.0-e) && (c in 0.0+e..1.0-e)
    }
}