package de.officeryoda.fhysics.engine.objects

import java.awt.Color
import java.awt.Graphics
import java.util.*
import kotlin.math.hypot

class Edge(var start: Vertex, var end: Vertex) {
    val center: FloatArray = floatArrayOf((start.x + end.x) / 2, (start.y + end.y) / 2)

    /**
     * Checks if an edge has a vertex as its start or end
     * @param vertex the vertex in question
     * @return true if an edge has a vertex as its start or end, false otherwise
     */
    fun contains(vertex: Vertex?): Boolean {
        return start.equals(vertex) || end.equals(vertex)
    }

    /**
     * Inverts which is the start and end vertices
     */
    fun invert() {
        val temp = start
        start = end
        end = temp
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val edge = o as Edge
        return start == edge.start && end == edge.end
    }

    override fun hashCode(): Int {
        return Objects.hash(start, end)
    }

    /**
     * Paints the edge in the GPanel
     * @param g the Graphics Object for GPanel
     * @param i index of the edge
     */
    fun paint(g: Graphics, i: Int) {
        g.color = Color.BLACK
        g.drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
        g.drawString("" + i, center[0].toInt() + 1, center[1].toInt())
    }

    /**
     * Gets the opposite vertex from the one given between the start or end vertex
     * @param vertex the vertex in considertation
     * @return the opposite vertex from the given
     */
    fun getOther(vertex: Vertex): Vertex {
        if (vertex.equals(start)) {
            return end
        }
        return start
    }

    /**
     * Finds the Euclidean length of the Edge
     * @return the length of the Edge
     */
    fun length(): Double {
        return hypot((start.x - end.x).toDouble(), (start.y - end.y).toDouble())
    }

    /**
     * Adds this edge to the edgeLists of the vertices it is composed from
     */
    fun incorporate() {
        start.addEdge(this)
        end.addEdge(this)
    }

    override fun toString(): String {
        return "Edge{" +
                "start=" + start +
                ", end=" + end +
                '}'
    }

    companion object {
        /**
         * Constructs an Edge for use in a Simple polygon, basically sets them as next to each other
         * @param start start vertex
         * @param end end vertex
         * @return the edge from start to end
         */
        fun polygonalEdge(start: Vertex, end: Vertex): Edge {
            val edge = Edge(start, end)
            start.next = end
            end.prev = start
            return edge
        }

        /**
         * Constructs an Edge for use in a Simple polygon, basically sets them as next to each other
         * @param start start vertex
         * @param end end vertex
         */
        fun orderVertex(start: Vertex, end: Vertex) {
            start.next = end
            end.prev = start
        }
    }
}
