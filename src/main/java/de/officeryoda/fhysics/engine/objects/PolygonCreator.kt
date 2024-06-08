package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.rendering.RenderUtil
import java.awt.Color
import kotlin.math.abs
import kotlin.random.Random

/**
 * Converts polygons to convex polygons.
 */
object PolygonCreator {

    fun createPolygon(vertices: Array<Vector2>): Polygon {
        ensureCCW(vertices)
        if (!isConcave(vertices))
            return ConvexPolygon(vertices)

        // Create concave polygon
        val edges = triangulate(vertices)

        edges.forEach { edge ->
            println(edge)
            val color = generateRandomColor()
//            RenderUtil.drawer.addDebugPoint(vertexToVector2(edge.start), color, 2000000)
//            RenderUtil.drawer.addDebugPoint(vertexToVector2(edge.end), color, 200000)
            RenderUtil.drawer.addDebugLine(vertexToVector2(edge.start), vertexToVector2(edge.end), color, 2000000)
        }

        return ConvexPolygon(vertices)

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

    fun triangulate(v2Vertices: Array<Vector2>): ArrayList<Edge> {
        val vertices: ArrayList<Vertex> = toVertexArray(v2Vertices)
        val edges: ArrayList<Edge> = toEdgeArray(vertices.toTypedArray())
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
        return edges
    }

    private fun toVertexArray(v2Vertices: Array<Vector2>): ArrayList<Vertex> {
        val vertices: ArrayList<Vertex> = ArrayList<Vertex>()
        for ((i, v2Vertex: Vector2) in v2Vertices.withIndex()) {
            val vertex = Vertex(v2Vertex.x, v2Vertex.y)
            vertices.add(vertex)
        }

        return vertices
    }

    private fun toEdgeArray(vertices: Array<Vertex>): ArrayList<Edge> {
        val edges = ArrayList<Edge>()
        for (i in vertices.indices) {
            edges.add(Edge.polygonalEdge(vertices[i], vertices[(i + 1) % vertices.size]))
        }
        var lowest: Vertex = vertices.get(0)
        for (vertex in vertices) {
            if (vertex.x < lowest.x) lowest = vertex
        }

        return edges
    }

    /**
     * Clips off a vertex if it's an ear vertex
     * @param v0 the vertex in question
     * @return the edge formed by clipping the vertex
     */
    fun clipEar(v0: Vertex, vertices: Array<Vertex>, edges: ArrayList<Edge>): Edge? {
        if (!diagonal(v0.prev!!, v0.next!!, vertices, edges)) {
            return null
        }
        return Edge.polygonalEdge(v0.prev!!, v0.next!!)
    }

    /**
     * Determines if two vertices have a diagonal
     * @param v0 first vertex
     * @param v1 second vertex
     * @return true if they have a diagonal
     */
    fun diagonal(v0: Vertex, v1: Vertex, vertices: Array<Vertex>, edges: ArrayList<Edge>): Boolean {
        return inCone(v0, v1, vertices) && inCone(v1, v0, vertices) && diagonalie(v0, v1, edges)
    }

    /**
     * Determines whether a vertex is in the "open cone" of another vertex.
     * @param v0 vertex who's in the cone is analyzed
     * @param v1 vertex who may or may not be in the cone
     * @return true if vertex is in the cone, false otherwise
     */
    fun inCone(v0: Vertex, v1: Vertex, vertices: Array<Vertex>): Boolean {
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
    fun leftOn(a: FloatArray, b: FloatArray, c: FloatArray): Boolean {
        return crossProduct(a, b, a, c) >= 0
    }

    /**
     * Determines if a point is to the left of two other points
     * @param a coordinates of a point
     * @param b coordinates of another point
     * @param c coordinates of the point that may or may not be to the left
     * @return true if c is to the left of a and b, false elsewise.
     */
    fun left(a: FloatArray, b: FloatArray, c: FloatArray): Boolean {
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
    fun intersectsProp(a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray): Boolean {
        if (collinear(a, b, c) && collinear(a, b, d)) {
            return (between(a, b, c) || between(a, b, d))
        }
        if ((between(a, b, c) || between(a, b, d) || between(
                c,
                d,
                a
            ) || between(c, d, b))
        ) {
            return true
        }
        if (collinear(a, b, c) || collinear(a, b, d) || collinear(
                c,
                d,
                a
            ) || collinear(c, d, b)
        ) {
            return false
        }
        return (left(a, b, c) != left(a, b, d)) && (left(
            c,
            d,
            a
        ) != left(c, d, b))
    }

    /**
     * Determines if a point is between two other points (all collinear)
     * @param a coordinates of a point
     * @param b coordinates of another point
     * @param c coordinates of the point between two other points
     * @return true if c is between a and b, false elsewise.
     */
    fun between(a: FloatArray, b: FloatArray, c: FloatArray): Boolean {
        if (!collinear(a, b, c)) return false
        if (a[0] != b[0]) return ((a[0] <= c[0]) && (c[0] <= b[0])) ||
                ((a[0] >= c[0]) && (c[0] >= b[0]))
        return ((a[1] <= c[1]) && (c[1] <= b[1])) ||
                ((a[1] >= c[1]) && (c[1] >= b[1]))
    }

    /**
     * Determines if a point is to the collinear with two other points
     * @param a coordinates of a point
     * @param b coordinates of another point
     * @param c coordinates of the point that may or may not be collinear.
     * @return true if c is to the left of a and b, false elsewise.
     */
    fun collinear(a: FloatArray, b: FloatArray, c: FloatArray): Boolean {
        return abs(crossProduct(a, b, a, c)) < 0.01f
    }

    //<editor-fold desc="Non Copy Code">
    /**
     * Validates the polygon vertices
     *
     * Checks if the polygon is valid by checking if any
     * of the edges between two neighboring vertices intersect
     *
     * @return true if the polygon is valid
     */
    fun validatePolyVertices(vertices: MutableList<Vector2>): Boolean {
        val size: Int = vertices.size
        if (size < 3) return false

        return !areLinesIntersecting(vertices)
    }

    /**
     * Checks if the lines of the polygon are intersecting
     *
     * @return true if the lines are intersecting
     */
    private fun areLinesIntersecting(vertices: MutableList<Vector2>): Boolean {
        val size: Int = vertices.size
        for (i: Int in 0 until size) {
            for (j: Int in i + 1 until size) {
                val line1: Pair<Vector2, Vector2> = Pair(vertices[i], vertices[(i + 1) % size])
                val line2: Pair<Vector2, Vector2> = Pair(vertices[j], vertices[(j + 1) % size])
                if (doLinesIntersect(line1, line2)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if two lines intersect
     *
     * @param lineA the first line
     * @param lineB the second line
     */
    private fun doLinesIntersect(lineA: Pair<Vector2, Vector2>, lineB: Pair<Vector2, Vector2>): Boolean {
        val a: Vector2 = lineA.first
        val b: Vector2 = lineA.second
        val c: Vector2 = lineB.first
        val d: Vector2 = lineB.second

        val denominator: Float = ((b.x - a.x) * (d.y - c.y)) - ((b.y - a.y) * (d.x - c.x))

        // If the denominator is zero, lines are parallel and do not intersect
        if (denominator == 0.0f) return false

        // Calculate the numerators of the line intersection formula
        val numeratorA: Float = ((a.y - c.y) * (d.x - c.x)) - ((a.x - c.x) * (d.y - c.y))
        val numeratorB: Float = ((a.y - c.y) * (b.x - a.x)) - ((a.x - c.x) * (b.y - a.y))

        // Calculate r and s parameters
        val r: Float = numeratorA / denominator
        val s: Float = numeratorB / denominator

        // If r and s are both between 0 and 1, lines intersect (excluding endpoints)
        return (0f < r && r < 1f) && (0f < s && s < 1f)
    }

    /**
     * Checks if the polygon is concave
     *
     * @param vertices the vertices of the polygon (must be CCW)
     * @return true if the polygon is concave
     */
    private fun isConcave(vertices: Array<Vector2>): Boolean {
        val size: Int = vertices.size
        for (i: Int in 0 until size) {
            val a: Vector2 = vertices[i]
            val b: Vector2 = vertices[(i + 1) % size]
            val c: Vector2 = vertices[(i + 2) % size]

            val crossProduct: Float = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
            if (crossProduct < 0) {
                return true
            }
        }
        return false
    }

    /**
     * Ensures that the vertices of the polygon are in counter-clockwise order
     *
     * @param vertices the vertices of the polygon
     */
    private fun ensureCCW(vertices: Array<Vector2>) {
        // Calculate the signed area of the polygon
        val signedArea: Float = calculateSignedArea(vertices)

        // Reverse the vertices if the polygon is CW
        if (signedArea < 0) vertices.reverse()
    }

    /**
     * Calculates the signed area of a polygon
     *
     * The area will be positive if the polygon is in counter-clockwise order
     * and negative if the polygon is in clockwise order
     *
     * @param vertices the vertices of the polygon
     * @return the signed area of the polygon
     */
    private fun calculateSignedArea(vertices: Array<Vector2>): Float {
        var signedArea = 0f
        for (i: Int in vertices.indices) {
            val j: Int = (i + 1) % vertices.size
            signedArea += vertices[i].x * vertices[j].y - vertices[j].x * vertices[i].y
        }

        return signedArea / 2
    }

    private fun concavePolygonExample(ignore: List<Vector2>): List<List<Vector2>> {
        val vertices: List<List<Vector2>> = listOf(
            listOf(Vector2(2.07f, 5.35f), Vector2(5.7f, 4.1f), Vector2(3.75f, 2.15f)),
            listOf(Vector2(2.07f, 5.35f), Vector2(6.85f, 7.50f), Vector2(5.7f, 4.1f)),
            listOf(Vector2(6.85f, 7.5f), Vector2(7.0f, 3.65f), Vector2(5.7f, 4.1f))
        )

        vertices.forEach { polygon ->
            polygon.forEach { vertex ->
                vertex *= 3f
                vertex += Vector2(40f, 40f)
            }
        }

        return vertices
    }
    //</editor-fold>
}