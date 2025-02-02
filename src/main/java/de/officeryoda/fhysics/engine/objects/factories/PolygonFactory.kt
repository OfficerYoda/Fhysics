package de.officeryoda.fhysics.engine.objects.factories

import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.ConcavePolygon
import de.officeryoda.fhysics.engine.objects.ConvexPolygon
import de.officeryoda.fhysics.engine.objects.Polygon

/**
 * Factory class to convert a list of vertices into a polygon object.
 */
object PolygonFactory {

    /**
     * Creates a polygon from the given [vertices].
     */
    fun createPolygon(vertices: Array<Vector2>, angle: Float = 0f): Polygon {
        ensureCCW(vertices)
        if (!isConcave(vertices)) return ConvexPolygon(vertices, angle)

        // Triangulate the polygon
        val triangles: MutableList<Array<Vector2>> = triangulate(vertices.toMutableList())
        // Merge the triangles to create convex polygons
        val polygonIndices: Array<IntArray> = mergePolygons(vertices, triangles)

        return ConcavePolygon(vertices, polygonIndices, angle)
    }

    /**
     * Returns a list of triangles that form the polygon with the given [vertices].
     *
     * This method has problems with collinear points, but the appearance of those is unlikely when creating polygons by hand.
     */
    private fun triangulate(vertices: MutableList<Vector2>): MutableList<Array<Vector2>> {
        val triangles: MutableList<Array<Vector2>> = mutableListOf()

        while (vertices.size > 3) {
            for (i: Int in vertices.indices) {
                val prev: Vector2 = vertices[(i - 1 + vertices.size) % vertices.size]
                val curr: Vector2 = vertices[i]
                val next: Vector2 = vertices[(i + 1) % vertices.size]

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

    /**
     * Returns a boolean indicating if the triangle ([v1], [v2], [v3]) is an ear of the [polygon].
     *
     * An ear is a triangle that does not contain any other vertices of the polygon inside it.
     */
    private fun isEar(v1: Vector2, v2: Vector2, v3: Vector2, polygon: List<Vector2>): Boolean {
        // Check if the triangle (p1, p2, p3) is an ear
        if (!isConvex(v1, v2, v3)) return false
        for (vertex: Vector2 in polygon) {
            if (vertex != v1 && vertex != v2 && vertex != v3 && isPointInTriangle(vertex, v1, v2, v3)) {
                return false
            }
        }
        return true
    }

    /**
     * Returns a boolean indicating if the [point][p] is inside the triangle ([v1], [v2], [v3]).
     */
    private fun isPointInTriangle(p: Vector2, v1: Vector2, v2: Vector2, v3: Vector2): Boolean {
        // WTF is going on here but it works
        val area: Float = 0.5f * (-v2.y * v3.x + v1.y * (-v2.x + v3.x) + v1.x * (v2.y - v3.y) + v2.x * v3.y)
        val s: Float = 1 / (2 * area) * (v1.y * v3.x - v1.x * v3.y + (v3.y - v1.y) * p.x + (v1.x - v3.x) * p.y)
        val t: Float = 1 / (2 * area) * (v1.x * v2.y - v1.y * v2.x + (v1.y - v2.y) * p.x + (v2.x - v1.x) * p.y)
        val u: Float = 1 - s - t
        return s >= 0 && t >= 0 && u >= 0
    }

    /**
     * Returns a list of indices that form the convex polygons by merging the [triangles].
     * @param vertices the vertices of the polygon containing the triangles
     */
    private fun mergePolygons(vertices: Array<Vector2>, triangles: MutableList<Array<Vector2>>): Array<IntArray> {
        val polygons: MutableList<Array<Vector2>> = triangles
        var merged = true

        while (merged) {
            merged = false
            for (i: Int in polygons.indices) {
                for (j: Int in i + 1 until polygons.size) {
                    val sharedEdge: Pair<Int, Int> = sharedEdge(polygons[i], polygons[j]) ?: continue
                    val mergedPolygon: Array<Vector2> =
                        mergePolygons(polygons[i], polygons[j], sharedEdge)

                    if (!isConcave(mergedPolygon)) {
                        polygons[i] = mergedPolygon
                        polygons.removeAt(j)
                        merged = true
                        break
                    }
                }

                if (merged) break
            }
        }

        val verticesIndicesMap: Map<Vector2, Int> = vertices.withIndex().associate { it.value to it.index }
        val polygonIndices: Array<IntArray> =
            polygons.map { polygon ->
                polygon.map { vertex ->
                    verticesIndicesMap[vertex]!!
                }.toIntArray()
            }.toTypedArray()

        return polygonIndices
    }

    /**
     * Returns a merged polygon of [polyA] and [polyB] by removing the [shared edge][sharedEdge] between them.
     */
    private fun mergePolygons(
        polyA: Array<Vector2>,
        polyB: Array<Vector2>,
        sharedEdge: Pair<Int, Int>,
    ): Array<Vector2> {
        val newPoly: MutableList<Vector2> = mutableListOf()

        newPoly.addAll(polyA.take(sharedEdge.first + 1)) // Add all points of A before the shared edge
        newPoly.addAll((sharedEdge.second + 2 until sharedEdge.second + polyB.size).map { polyB[it % polyB.size] }) // Add all points of B except the shared edge
        newPoly.addAll((sharedEdge.first + 1 until polyA.size).map { polyA[it % polyA.size] }) // Add the rest of the points of A

        return newPoly.toTypedArray()
    }

    /**
     * Returns the shared edge between [polyA] and [polyB] if it exists.
     */
    private fun sharedEdge(polyA: Array<Vector2>, polyB: Array<Vector2>): Pair<Int, Int>? {
        for (i: Int in polyA.indices) {
            val p1: Vector2 = polyA[i]
            val p2: Vector2 = polyA[(i + 1) % polyA.size]
            for (j: Int in polyB.indices) {
                val p3: Vector2 = polyB[j]
                val p4: Vector2 = polyB[(j + 1) % polyB.size]
                if (p1 == p4 && p2 == p3) return Pair(i, j)
            }
        }
        return null
    }

    /**
     * Returns a boolean indicating if the polygon's [vertices] are valid.
     */
    fun isPolygonValid(vertices: List<Vector2>): Boolean {
        val size: Int = vertices.size
        if (size < 3) return false

        return !areEdgesIntersecting(vertices)
    }

    /**
     * Returns a boolean indicating if the [vertices] are a valid polygon.
     */
    private fun areEdgesIntersecting(vertices: List<Vector2>): Boolean {
        // Works by checking if the line between two neighbouring vertices
        // intersects with any other line between two neighbouring vertices
        val size: Int = vertices.size
        for (i: Int in 0 until size) {
            for (j: Int in i + 1 until size) {
                val line1: Pair<Vector2, Vector2> = Pair(vertices[i], vertices[(i + 1) % size])
                val line2: Pair<Vector2, Vector2> = Pair(vertices[j], vertices[(j + 1) % size])
                if (areLinesIntersecting(line1, line2)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Returns a boolean indicating if [lineA] intersects with line [lineB].
     */
    private fun areLinesIntersecting(lineA: Pair<Vector2, Vector2>, lineB: Pair<Vector2, Vector2>): Boolean {
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
     * Returns a boolean indicating if the polygon with the given [vertices] is concave.
     */
    private fun isConcave(vertices: Array<Vector2>): Boolean {
        val size: Int = vertices.size
        for (i: Int in 0 until size) {
            val a: Vector2 = vertices[i]
            val b: Vector2 = vertices[(i + 1) % size]
            val c: Vector2 = vertices[(i + 2) % size]

            if (!isConvex(a, b, c)) return true
        }
        return false
    }

    /**
     * Returns a boolean indicating if the angle created by the points [p1], [p2], and [p3] is convex.
     */
    private fun isConvex(p1: Vector2, p2: Vector2, p3: Vector2): Boolean {
        return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x) > 0
    }

    /**
     * Ensures that the [vertices] of the polygon are in counter-clockwise order
     */
    private fun ensureCCW(vertices: Array<Vector2>) {
        // Calculate the signed area of the polygon
        val signedArea: Float = calculateSignedArea(vertices)

        // Reverse the vertices if the polygon is CW
        if (signedArea < 0) vertices.reverse()
    }

    /**
     * Returns the signed area of a polygon defined by the given [vertices].
     *
     * The area will be positive if the polygon is in counter-clockwise order
     * and negative if the polygon is in clockwise order.
     */
    private fun calculateSignedArea(vertices: Array<Vector2>): Float {
        var signedArea = 0f
        for (i: Int in vertices.indices) {
            val j: Int = (i + 1) % vertices.size
            signedArea += vertices[i].x * vertices[j].y - vertices[j].x * vertices[i].y
        }

        return signedArea / 2
    }
}