package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import java.awt.Color
import kotlin.math.abs
import kotlin.random.Random

object `Hertel-Mehlhorn-Convex-Decomposition` {

    fun addOuterEdges(vertices: ArrayList<Vertex>, edges: ArrayList<Edge>) {
        val size = vertices.size
        for (i in 0 until size) {
            val edge = Edge.polygonalEdge(vertices[i], vertices[(i + 1) % size])
            edges.add(edge)
        }
    }

    fun vertexToVector2(vertex: Vertex): Vector2 {
        return Vector2(vertex.x, vertex.y)
    }

    fun generateRandomColor(): Color {
        val red = Random.nextInt(64) + 64
        val green = Random.nextInt(192) + 64
        val blue = Random.nextInt(192) + 64
        return Color(red, green, blue)
    }

    fun triangulate(v2Vertices: Array<Vector2>): Pair<ArrayList<Vertex>, ArrayList<Edge>> {
        val vertices: ArrayList<Vertex> = v2Vertices.map { Vertex(it.x, it.y) } as ArrayList<Vertex>
        val edges: ArrayList<Edge> = vertices.indices.map {
            Edge.polygonalEdge(
                vertices[it],
                vertices[(it + 1) % vertices.size]
            )
        } as ArrayList<Edge>

        var i = 0
        var pointer: Vertex = vertices[0]
        while (pointer.next !== pointer.prev && i < 200) {
            val edge: Edge? = clipEar(pointer, vertices.toTypedArray(), edges)
            pointer = pointer.prev!!
            if (edge != null) {
                edges.add(edge)
            }
            i++
        }
        edges.removeAt(edges.size - 1)
        i = 0
        while (i < vertices.size) {
            Edge.orderVertex(vertices[i], vertices[(i + 1) % vertices.size])
            i++
        }
        for (edge in edges) {
            edge.incorporate()
        }

        return Pair(vertices, edges)
    }

    /**
     * Given the triangulation, finds a convex decomposition
     */
    fun decompose(vertices: ArrayList<Vertex>, edges: ArrayList<Edge>) {
        for (vertex in vertices) {
            vertex.edges.sortWith<Edge> { o1, o2 ->
                if ((o1.center[1] - vertex.y) * (o2.center[1] - vertex.y) < 0) return@sortWith (o1.center[1] - o2.center[1]).toInt()
                val crossproduct: Int =
                    crossProduct(
                        vertex.coordsArr,
                        o1.center,
                        vertex.coordsArr,
                        o2.center
                    ).toInt()
                crossproduct.compareTo(0)
            }
        }
        var i = 0
        while (i < edges.size && edges.size != 0) {
            val edge: Edge = edges[i]
            if (vertexClearance(edge, edge.end) && vertexClearance(edge, edge.start)) {
                removeEdge(edge, edges)
                i--
            }
            i++
        }
    }

    /**
     * Clips off a vertex if it's an ear vertex
     * @param v0 the vertex in question
     * @return the edge formed by clipping the vertex
     */
    private fun clipEar(v0: Vertex, vertices: Array<Vertex>, edges: ArrayList<Edge>): Edge? {
        if (!diagonal(v0.prev!!, v0.next!!, vertices, edges)) return null

        return Edge.polygonalEdge(v0.prev!!, v0.next!!)
    }

    /**
     * Determines if two vertices have a diagonal
     * @param v0 first vertex
     * @param v1 second vertex
     * @return true if they have a diagonal
     */
    private fun diagonal(v0: Vertex, v1: Vertex, vertices: Array<Vertex>, edges: ArrayList<Edge>): Boolean {
        return inCone(v0, v1, vertices) && inCone(v1, v0, vertices) && diagonalie(v0, v1, edges)
    }

    /**
     * Determines whether a vertex is in the "open cone" of another vertex.
     * @param v0 vertex who's in the cone is analyzed
     * @param v1 vertex who may or may not be in the cone
     * @return true if vertex is in the cone, false otherwise
     */
    private fun inCone(v0: Vertex, v1: Vertex, vertices: Array<Vertex>): Boolean {
        val next: Vertex = vertices[(vertices.indexOf(v0) + 1) % vertices.size]
        val prev: Vertex = vertices[(vertices.indexOf(v0) - 1 + vertices.size) % vertices.size]
        if (leftOn(v0.coordsArr, next.coordsArr, prev.coordsArr)) {
            return left(v0.coordsArr, v1.coordsArr, prev.coordsArr) && left(
                v1.coordsArr,
                v0.coordsArr,
                next.coordsArr
            )
        }
        return !(leftOn(
            v0.coordsArr,
            v1.coordsArr,
            next.coordsArr
        ) && leftOn(v1.coordsArr, v0.coordsArr, prev.coordsArr))
    }

    /**
     * Determines if two vertices can see each other
     * @param v0 first vertex
     * @param v1 second vertex
     * @return true if no edges overlap with line between vertices, false otherwise.
     */
    private fun diagonalie(v0: Vertex, v1: Vertex, edges: ArrayList<Edge>): Boolean {
        for (edge in edges) {
            if (edge.contains(v0) || edge.contains(v1)) {
                continue
            }
            if (intersectsProp(
                    edge.start.coordsArr,
                    edge.end.coordsArr,
                    v0.coordsArr,
                    v1.coordsArr
                )
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Determines if a point is to the left of or inline with two other points
     * @param a coordinates of a point
     * @param b coordinates of another point
     * @param c coordinates of the point that may or may not be to the left
     * @return true if c is to the left of or collinear with a and b, false elsewise.
     */
    private fun leftOn(a: FloatArray, b: FloatArray, c: FloatArray): Boolean {
        return crossProduct(a, b, a, c) >= 0
    }

    /**
     * Determines if a point is to the left of two other points
     * @param a coordinates of a point
     * @param b coordinates of another point
     * @param c coordinates of the point that may or may not be to the left
     * @return true if c is to the left of a and b, false elsewise.
     */
    private fun left(a: FloatArray, b: FloatArray, c: FloatArray): Boolean {
        return crossProduct(a, b, a, c) > 0
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

    /**
     * Determines if two line segments intersect
     * @param a first endpoint of first line segment
     * @param b second endpoint of first line segment
     * @param c first endpoint of second line segment
     * @param d first endpoint of second line segment
     * @return true if they intersect, false otherwise
     */
    private fun intersectsProp(a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray): Boolean {
        if (collinear(a, b, c) && collinear(a, b, d)) return (between(a, b, c) || between(a, b, d))
        if ((between(a, b, c) || between(a, b, d) || between(c, d, a) || between(c, d, b))) return true
        if (collinear(a, b, c) || collinear(a, b, d) || collinear(c, d, a) || collinear(c, d, b)) return false
        return (left(a, b, c) != left(a, b, d)) && (left(c, d, a) != left(c, d, b))
    }

    /**
     * Determines if a point is between two other points (all collinear)
     * @param a coordinates of a point
     * @param b coordinates of another point
     * @param c coordinates of the point between two other points
     * @return true if c is between a and b, false elsewise.
     */
    private fun between(a: FloatArray, b: FloatArray, c: FloatArray): Boolean {
        if (!collinear(a, b, c)) return false
        if (a[0] != b[0]) return ((a[0] <= c[0]) && (c[0] <= b[0])) || ((a[0] >= c[0]) && (c[0] >= b[0]))
        return ((a[1] <= c[1]) && (c[1] <= b[1])) || ((a[1] >= c[1]) && (c[1] >= b[1]))
    }

    /**
     * Determines if a point is to the collinear with two other points
     * @param a coordinates of a point
     * @param b coordinates of another point
     * @param c coordinates of the point that may or may not be collinear.
     * @return true if c is to the left of a and b, false elsewise.
     */
    private fun collinear(a: FloatArray, b: FloatArray, c: FloatArray): Boolean {
        return abs(crossProduct(a, b, a, c)) < 0.01f
    }

    /**
     * Checks if a diagonal can be removed based off info at given vertex
     * @param edge the diagonal potentially to be removed
     * @param vertex the vertex being checked
     * @return true if the diagonal would keep all angles of the vertex are convex, false otherwise
     */
    private fun vertexClearance(edge: Edge, vertex: Vertex): Boolean {
        val edges: ArrayList<Edge> = vertex.edges
        if (edges.size == 3) {
            return vertex.sinAngle(vertex.prev!!, vertex.next!!) <= 0
        }
        val prev = edges[(edges.indexOf(edge) - 1 + edges.size) % edges.size]
        val next = edges[(edges.indexOf(edge) + 1) % edges.size]
        return vertex.sinAngle(prev, next) <= 0
    }

    /**
     * Removes an edge from the program
     * @param edge the edge being removed
     */
    private fun removeEdge(edge: Edge, edges: ArrayList<Edge>) {
        edge.start.removeEdge(edge)
        edge.end.removeEdge(edge)
        edges.remove(edge)
    }
}