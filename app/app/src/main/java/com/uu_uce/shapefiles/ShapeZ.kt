package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.uu_uce.misc.LinkedList
import com.uu_uce.misc.Node


abstract class ShapeZ(var bmin: p3, var bmax: p3){
    abstract fun draw(canvas: Canvas, paint: Paint, viewport: Pair<p2,p2>, width: Int, height: Int)
}

class HeightShapeZ(private var points: List<p2>, bmi: p3, bma: p3): ShapeZ(bmi,bma) {

    override fun draw(
        canvas: Canvas,
        paint: Paint,
        viewport : Pair<p2, p2>,
        width: Int,
        height: Int
    ) {
        if (points.size < 2) return
        val drawPoints = FloatArray(4 * (points.size - 1))

        var lineIndex = 0
        for (i in 0..points.size - 2) {
            drawPoints[lineIndex++] =
                ((points[i].first - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat()
            drawPoints[lineIndex++] =
                (height - (points[i].second - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat()
            drawPoints[lineIndex++] =
                ((points[i + 1].first - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat()
            drawPoints[lineIndex++] =
                (height - (points[i + 1].second - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat()
        }

        canvas.drawLines(drawPoints, paint)
    }

    fun meanZ(): Int{
        return ((bmin.third + bmax.third) / 2).toInt()
    }
}

class PolyPoint(val point: p3, var reflex: Boolean, var ear: Boolean, val index: Int){
    var convex: Boolean
    get() { return !reflex}
        set(value) {reflex = !value}
}

class PolygonZ(outerRings: List<List<p3>>, private var innerRings: List<List<p3>>, bmi: p3, bma:p3): ShapeZ(bmi,bma){
    private lateinit var triangles: MutableList<Int>
    private var vertices: List<p3>

    init{
        //todo in proprocessing: make sure there is only 1 outer ring, with matching inner rings
        //using isInsidePolygon
        vertices = outerRings[0]
        mergeInner()
        removeDoubles()
        triangulate(vertices)
    }

    override fun draw(
        canvas: Canvas,
        paint: Paint,
        viewport: Pair<p2, p2>,
        width: Int,
        height: Int
    ) {
        val verts = FloatArray(vertices.size * 2){i ->
            if(i%2 == 0)
                ((vertices[i/2].first - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat()
            else
                (height - (vertices[i/2].second - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat()
        }
        val indices = triangles.map{i -> i.toShort()}.toShortArray()

        val colors = IntArray(verts.size){ Color.CYAN}
        //val colors = null

        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, verts.size, verts, 0, null, 0, colors, 0, indices, 0, indices.size, paint)
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

            //rm1:(310127.85714285716, 4671878.142857143, 0.0)
            //is1:(310138.63081861957, 4671878.142857143, 0.0)

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
        val l = List(originalPolygon.size){i -> PolyPoint(originalPolygon[i],false,false,i)}
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
        triangles = mutableListOf()

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
            triangles.add(cur.prev!!.value.index)
            triangles.add(cur.value.index)
            triangles.add(cur.next!!.value.index)

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
            triangles.add(point.value.index)
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

    private fun isInsidePolygon(p:p3, polygon: List<p3>): Boolean{
        //shoot a ray to the right and count how many times it intersects the polygon
        //even means outside, odd means inside
        var intersects = 0
        for(i in polygon.indices){
            val x1 = polygon[i].first
            val y1 = polygon[i].second
            val x2 = polygon[(i + 1) % polygon.size].first
            val y2 = polygon[(i + 1) % polygon.size].second

            val t = (p.second - y1) / (y2 - y1)
            if (t < 0 || t > 1) continue
            val x = x1 + t * (x2 - x1)
            if(x<p.first) continue

            //there was an intersection
            intersects++
        }
        return intersects % 2 == 1
    }
}