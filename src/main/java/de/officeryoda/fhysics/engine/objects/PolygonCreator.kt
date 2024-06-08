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
//        if (!isConcave(inputVertices)) return ConvexPolygon(inputVertices)

        // Create concave polygon
//        HMCDDecomposing(inputVertices)

        var verts = arrayOf(
            Vector2(40f, 40f),
            Vector2(60f, 40f),
            Vector2(60f, 60f),
            Vector2(50f, 70f),
            Vector2(40f, 60f)
        )
        ensureCCW(verts)
        fixCollinearPoints(verts)

        verts = inputVertices

        val triangles = triangulate(verts.toList())
        // draw triangles
        triangles.forEach { triangle ->
            RenderUtil.drawer.addDebugLine(triangle[0], triangle[1], Color.RED, 2000000)
            RenderUtil.drawer.addDebugLine(triangle[1], triangle[2], Color.RED, 2000000)
            RenderUtil.drawer.addDebugLine(triangle[2], triangle[0], Color.RED, 2000000)
        }

        val convexPolygons = mergeTriangles(triangles)
        // draw merged triangles
        convexPolygons.forEach { poly ->
            for (i in poly.indices) {
                RenderUtil.drawer.addDebugLine(poly[i], poly[(i + 1) % poly.size], Color.GREEN, 2000000)
            }
        }

        return ConvexPolygon(verts)
    }

    //    data class Triangle(val p1: Vector2, val p2: Vector2, val p3: Vector2)
    private fun fixCollinearPoints(points: Array<Vector2>) {
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                for (k in j + 1 until points.size) {
                    val p1 = points[i]
                    val p2 = points[j]
                    val p3 = points[k]

                    if (isCollinear(p1, p2, p3)) {
                        // Perturb the middle point slightly to break collinearity
                        val epsilon = 1e-5f
                        points[i] = p1 + Vector2(epsilon, 0f)
                        points[j] = p2 + Vector2(-epsilon, 0f)
                    }
                }
            }
        }
    }

    private fun isCollinear(p1: Vector2, p2: Vector2, p3: Vector2): Boolean {
        val crossProduct = (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x)
        return crossProduct == 0f // Points are collinear if cross product is zero
    }

    private fun triangulate(polygon: List<Vector2>): List<Array<Vector2>> {
        val triangles = mutableListOf<Array<Vector2>>()
        val vertices = polygon.toMutableList()

        while (vertices.size > 3) {
            for (i in vertices.indices) {
                val prev = vertices[(i - 1 + vertices.size) % vertices.size]
                val curr = vertices[i]
                val next = vertices[(i + 1) % vertices.size]

                if (isEar(prev, curr, next, vertices)) {
                    triangles.add(arrayOf(prev, curr, next))
                    vertices.removeAt(i)
                    break
                }
            }
        }

        triangles.add(arrayOf(vertices[0], vertices[1], vertices[2]))
        return triangles
    }

    private fun isEar(p1: Vector2, p2: Vector2, p3: Vector2, polygon: List<Vector2>): Boolean {
        // Check if the triangle (p1, p2, p3) is an ear
        if (!isConvex(p1, p2, p3)) return false
        for (vertex in polygon) {
            if (vertex != p1 && vertex != p2 && vertex != p3 && isPointInTriangle(vertex, p1, p2, p3)) {
                return false
            }
        }
        return true
    }

    private fun mergeTriangles(triangles: List<Array<Vector2>>): MutableList<Array<Vector2>> {
        val mergedPolygons = mutableListOf<Array<Vector2>>()
        val mergedIndices = mutableSetOf<Int>()
        val loopingList = triangles.toMutableList()
        var mergedSomething = true

        while (mergedSomething) {
            mergedSomething = false
            mergedPolygons.clear()
            mergedIndices.clear()

            for (i in loopingList.indices) {
                if (mergedIndices.contains(i)) continue

                val objA: Array<Vector2> = loopingList[i]
                var merged = false
                for (j in i + 1..<loopingList.size) {
                    if (mergedIndices.contains(j)) continue

                    val objB: Array<Vector2> = loopingList[j]

                    val newPoly: Array<Vector2?> = arrayOfNulls(objA.size + objB.size - 2)
                    val sharedEdge = shareEdge(objA, objB) ?: continue

                    // Add the first polygon up to the shared edge
                    var k = 0
                    while (k <= sharedEdge.first) {
                        newPoly[k] = objA[k]
                        k++
                    }
                    // Add the second polygon up to the shared edge
                    var l = sharedEdge.second + 2
                    repeat(objB.size - 2) {
                        newPoly[k] = objB[l % objB.size]
                        k++
                        l++
                    }
                    // Add the rest of the first polygon
                    repeat(objA.size - sharedEdge.first - 1) {
                        newPoly[k] = objA[(sharedEdge.first + 1 + it) % objA.size]
                        k++
                    }

                    if (newPoly.size != newPoly.filterNotNull().size) {
                        throw Exception("Null in newPoly") // TODO remove; just here for testing
                    }

                    if (isConcave(newPoly.filterNotNull().toTypedArray())) break


                    mergedPolygons.add(newPoly as Array<Vector2>)
                    mergedIndices.add(j)
                    merged = true
                    mergedSomething = true

                    break
                }

                if (!merged && !mergedIndices.contains(i)) {
                    mergedPolygons.add(objA)
                }
            }

            loopingList.clear()
            loopingList.addAll(mergedPolygons)
        }

        return mergedPolygons
    }

    private fun shareEdge(polyA: Array<Vector2>, polyB: Array<Vector2>): Pair<Int, Int>? {
        for (i in polyA.indices) {
            val p1 = polyA[i]
            val p2 = polyA[(i + 1) % polyA.size]
            for (j in polyB.indices) {
                val p3 = polyB[j]
                val p4 = polyB[(j + 1) % polyB.size]
                if (p1 == p4 && p2 == p3) return Pair(i, j)
            }
        }
        return null
    }

    private fun isConvex(p1: Vector2, p2: Vector2, p3: Vector2): Boolean {
        return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x) > 0
    }

    private fun isPointInTriangle(p: Vector2, p1: Vector2, p2: Vector2, p3: Vector2): Boolean {
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
                HMCD.vertexToVector2(edge.start), HMCD.vertexToVector2(edge.end), color, 2000000
            )
        }
        vertices.forEach {
            RenderUtil.drawer.addDebugPoint(HMCD.vertexToVector2(it), Color.PINK, 2000000)
        }
    }
}