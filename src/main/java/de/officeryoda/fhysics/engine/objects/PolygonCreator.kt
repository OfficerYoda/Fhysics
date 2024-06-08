package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.rendering.RenderUtil
import java.awt.Color
import de.officeryoda.fhysics.engine.objects.`Hertel-Mehlhorn-Convex-Decomposition` as HMCD

/**
 * Converts polygons to convex polygons.
 */
object PolygonCreator {

    fun createPolygon(inputVertices: Array<Vector2>): de.officeryoda.fhysics.engine.objects.Polygon {
        ensureCCW(inputVertices)
        if (!isConcave(inputVertices))
            return ConvexPolygon(inputVertices)

        // Create concave polygon
//        HMCDDecomposing(inputVertices)

        val triangles = triangulate(inputVertices.toList())
        // draw triangles
        triangles.forEach { triangle ->
            RenderUtil.drawer.addDebugLine(triangle.p1, triangle.p2, Color.RED, 2000000)
            RenderUtil.drawer.addDebugLine(triangle.p2, triangle.p3, Color.RED, 2000000)
            RenderUtil.drawer.addDebugLine(triangle.p3, triangle.p1, Color.RED, 2000000)
        }

        return ConvexPolygon(inputVertices)
    }

    data class Triangle(val p1: Vector2, val p2: Vector2, val p3: Vector2)

    fun triangulate(polygon: List<Vector2>): List<Triangle> {
        val triangles = mutableListOf<Triangle>()
        val vertices = polygon.toMutableList()

        while (vertices.size > 3) {
            for (i in vertices.indices) {
                val prev = vertices[(i - 1 + vertices.size) % vertices.size]
                val curr = vertices[i]
                val next = vertices[(i + 1) % vertices.size]

                if (isEar(prev, curr, next, vertices)) {
                    triangles.add(Triangle(prev, curr, next))
                    vertices.removeAt(i)
                    break
                }
            }
        }

        triangles.add(Triangle(vertices[0], vertices[1], vertices[2]))
        return triangles
    }

    fun isEar(p1: Vector2, p2: Vector2, p3: Vector2, polygon: List<Vector2>): Boolean {
        // Check if the triangle (p1, p2, p3) is an ear
        if (!isConvex(p1, p2, p3)) return false
        for (vertex in polygon) {
            if (vertex != p1 && vertex != p2 && vertex != p3 && isPointInTriangle(vertex, p1, p2, p3)) {
                return false
            }
        }
        return true
    }

    fun isConvex(p1: Vector2, p2: Vector2, p3: Vector2): Boolean {
        return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x) > 0
    }

    fun isPointInTriangle(p: Vector2, p1: Vector2, p2: Vector2, p3: Vector2): Boolean {
        val area = 0.5 * (-p2.y * p3.x + p1.y * (-p2.x + p3.x) + p1.x * (p2.y - p3.y) + p2.x * p3.y)
        val s = 1 / (2 * area) * (p1.y * p3.x - p1.x * p3.y + (p3.y - p1.y) * p.x + (p1.x - p3.x) * p.y)
        val t = 1 / (2 * area) * (p1.x * p2.y - p1.y * p2.x + (p1.y - p2.y) * p.x + (p2.x - p1.x) * p.y)
        return s > 0 && t > 0 && (1 - s - t) > 0
    }


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

    private fun HMCDDecomposing(inputVertices: Array<Vector2>) {
        val (vertices: ArrayList<Vertex>, edges: ArrayList<Edge>) = HMCD.triangulate(inputVertices)
        HMCD.decompose(vertices, edges)
        HMCD.addOuterEdges(vertices, edges)

        val edgeSet: Set<Edge> = edges.toSet()

        edgeSet.forEach { edge ->
            val color = HMCD.generateRandomColor()
            RenderUtil.drawer.addDebugLine(
                HMCD.vertexToVector2(edge.start),
                HMCD.vertexToVector2(edge.end),
                color,
                2000000
            )
        }
        vertices.forEach {
            RenderUtil.drawer.addDebugPoint(HMCD.vertexToVector2(it), Color.PINK, 2000000)
        }
    }
}