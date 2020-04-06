package com.uu_uce.shapefiles

import android.R.bool
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.uu_uce.misc.LinkedList
import com.uu_uce.misc.Node


abstract class ShapeZ(var bmin: p3, var bmax: p3){
    abstract fun draw(canvas: Canvas, paint: Paint, viewport: Pair<p2,p2>, width: Int, height: Int)
}

class HeightShapeZ(private var type: ShapeType, private var points: List<p2>, bmi: p3, bma: p3): ShapeZ(bmi,bma) {

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

class PolygonZ(private var outerRings: List<List<p3>>, private var innerRings: List<List<p3>>, bmi: p3, bma:p3): ShapeZ(bmi,bma){
    private lateinit var triangles: MutableList<Int>
    private var vertices: List<p3>

    init{
        vertices = outerRings[0]
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

        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, verts.size, verts, 0, null, 0, colors, 0, indices, 0, indices.size, paint)
    }

    private fun removeDoubles(){
        val hashset = LinkedHashSet(vertices)
        vertices = hashset.toList()
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

        var cur = remainingPolygon.first!!
        while(remainingPolygon.size > 3){
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
        //todo: make sure rotation is clockwise
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
            if(isInside(node.value.point, p.prev!!.value.point,p.value.point,p.next!!.value.point)) {
                return false
            }
        }
        return true
    }

    private fun isInside(p:p3, p0:p3 ,p1:p3 ,p2:p3): Boolean{
        //check if p is inside the triangle (p0,p1,p2)
        val d1 = sign(p, p0, p1)
        val d2 = sign(p, p1, p2)
        val d3 = sign(p, p2, p0)

        val hasneg = d1 < 0 || d2 < 0 || d3 < 0
        val haspos = d1 > 0 || d2 > 0 || d3 > 0

        return !(hasneg && haspos)
    }

    private fun sign(p0: p3, p1: p3, p2: p3): Float {
        return ((p0.first - p2.first) * (p1.second - p2.second) - (p1.first - p2.first) * (p0.second - p2.second)).toFloat()
    }
}