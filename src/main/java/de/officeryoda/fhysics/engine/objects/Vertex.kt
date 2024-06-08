package de.officeryoda.fhysics.engine.objects

import java.awt.Color
import java.util.*
import kotlin.math.hypot

class Vertex(@JvmField val x: Float, @JvmField val y: Float) {
    @JvmField
    var next: Vertex? = null

    @JvmField
    var prev: Vertex? = null
    var color: Color = Color(170, 170, 170)
    val edges: ArrayList<Edge> = ArrayList()

    val coordsArr: FloatArray
        /**
         * Makes an array with the coords of the vertex
         * @return integer array [x,y]
         */
        get() = floatArrayOf(x, y)

    /**
     * Swaps the previous and next vertices.
     */
    fun invert() {
        val temp = this.prev
        this.prev = this.next
        this.next = temp
    }

    fun addEdge(edge: Edge) {
        if (edges.contains(edge)) return
        edges.add(edge)
    }

    fun removeEdge(edge: Edge) {
        edges.remove(edge)
    }

    override fun toString(): String {
        return "Vertex{" +
                "x=" + x +
                ", y=" + y +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val vertex = o as Vertex
        return x == vertex.x &&
                y == vertex.y
    }

    override fun hashCode(): Int {
        return Objects.hash(x, y)
    }

    /**
     * Returns the sine of the angle between two edges that share this Vertex
     * @return the sine of the angle they make.
     */
    fun sinAngle(firstEdge: Edge, secondEdge: Edge): Double {
        val crossProduct: Float = crossProduct(
            this.coordsArr, firstEdge.getOther(this).coordsArr,
            coordsArr, secondEdge.getOther(this).coordsArr
        )
        return crossProduct / firstEdge.length() / secondEdge.length()
    }

    /**
     * Returns the sine of the angle between two edges that share this Vertex
     * @return the sine of the angle they make.
     */
    fun sinAngle(firstVertex: Vertex, secondVertex: Vertex): Double {
        val crossProduct: Float = crossProduct(
            this.coordsArr, firstVertex.coordsArr,
            coordsArr, secondVertex.coordsArr
        )
        return crossProduct / distance(firstVertex) / distance(secondVertex)
    }

    /**
     * Returns the distance between this instance and another Vertex
     * @param other the other vertex
     * @return the distance to the other vertex
     */
    fun distance(other: Vertex): Double {
        return hypot((this.x - other.x).toDouble(), (this.y - other.y).toDouble())
    }

    /**
     * Determines signed area of the parallelogram with points a,b,c,d
     * @param a coordinates of point a
     * @param b coordinates of point b
     * @param c coordinates of point c
     * @param d coordinates of point d
     * @return signed area of the parallelogram with points a,b,c,d
     */
    private fun crossProduct(a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray): Float {
        return (b[0] - a[0]) * (d[1] - c[1]) - (b[1] - a[1]) * (d[0] - c[0])
    }
}
