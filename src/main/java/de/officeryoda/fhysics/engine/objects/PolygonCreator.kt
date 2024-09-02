package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2

object PolygonCreator {

    /**
     * Creates a polygon from the given vertices
     *
     * @param vertices the vertices of the polygon
     * @return the polygon
     */
    fun createPolygon(vertices: Array<Vector2>, angle: Float = 0f): Polygon {
        ensureCCW(vertices)
        if (!isConcave(vertices)) return ConvexPolygon(vertices, angle)

        val triangles: MutableList<Array<Vector2>> = triangulate(vertices.toMutableList())
        // draw triangles
//        triangles.forEach { triangle ->
//            DebugDrawer.addDebugLine(triangle[0].copy(), triangle[1].copy(), Color.RED, 2400)
//            DebugDrawer.addDebugLine(triangle[1].copy(), triangle[2].copy(), Color.RED, 2400)
//            DebugDrawer.addDebugLine(triangle[2].copy(), triangle[0].copy(), Color.RED, 2400)
//        }

        val polygonIndices: Array<Array<Int>> = mergePolygons(vertices, triangles)
        // draw merged triangles
//        polygonIndices.forEach { polygon ->
//            polygon.indices.forEach { i ->
//                val j: Int = (i + 1) % polygon.size
//                DebugDrawer.addDebugLine(vertices[polygon[i]].copy(), vertices[polygon[j]].copy(), Color.GREEN, 2400)
//            }
//        }

        return ConcavePolygon(vertices, polygonIndices, angle)
    }

    /**
     * Triangulates a polygon
     *
     * It has problems with collinear points, but the appearance of those is unlikely when creating polygons by hand
     *
     * @param vertices the polygon to triangulate
     * @return a list of triangles
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
     * Checks if a triangle is an ear
     *
     * An ear is a triangle that does not contain any other vertices of the polygon inside it
     *
     * @param p1 the first vertex of the triangle
     * @param p2 the second vertex of the triangle
     * @param p3 the third vertex of the triangle
     * @param polygon the polygon containing the triangle
     */
    private fun isEar(p1: Vector2, p2: Vector2, p3: Vector2, polygon: List<Vector2>): Boolean {
        // Check if the triangle (p1, p2, p3) is an ear
        if (!isConvex(p1, p2, p3)) return false
        for (vertex: Vector2 in polygon) {
            if (vertex != p1 && vertex != p2 && vertex != p3 && isPointInTriangle(vertex, p1, p2, p3)) {
                return false
            }
        }
        return true
    }

    /**
     * Checks if a point is inside a triangle
     *
     * @param p the point
     * @param p1 the first vertex of the triangle
     * @param p2 the second vertex of the triangle
     * @param p3 the third vertex of the triangle
     */
    private fun isPointInTriangle(p: Vector2, p1: Vector2, p2: Vector2, p3: Vector2): Boolean {
        // WTF is going on here but it works
        val area: Float = 0.5f * (-p2.y * p3.x + p1.y * (-p2.x + p3.x) + p1.x * (p2.y - p3.y) + p2.x * p3.y)
        val s: Float = 1 / (2 * area) * (p1.y * p3.x - p1.x * p3.y + (p3.y - p1.y) * p.x + (p1.x - p3.x) * p.y)
        val t: Float = 1 / (2 * area) * (p1.x * p2.y - p1.y * p2.x + (p1.y - p2.y) * p.x + (p2.x - p1.x) * p.y)
        val u: Float = 1 - s - t
        return s >= 0 && t >= 0 && u >= 0
    }

    /**
     * Merges triangles to create convex polygons
     *
     * @param vertices all vertices of the polygon
     * @param triangles the indexed triangles of the polygon
     * @return a list of convex polygons
     */
    private fun mergePolygons(vertices: Array<Vector2>, triangles: MutableList<Array<Vector2>>): Array<Array<Int>> {
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
        val polygonIndices: Array<Array<Int>> =
            polygons.map { polygon ->
                polygon.map { vertex ->
                    verticesIndicesMap[vertex]!!
                }.toTypedArray()
            }.toTypedArray()

        return polygonIndices
    }

    /**
     * Merges two polygons by removing the shared edge
     *
     * @param polyA the first polygon
     * @param polyB the second polygon
     * @param sharedEdge the shared edge between the polygons
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
     * Finds the shared edge between two polygons
     *
     * @param polyA the first polygon
     * @param polyB the second polygon
     * @return the indices of the shared edge in the polygons or null if no edge is shared
     */
    private fun sharedEdge(polyA: Array<Vector2>, polyB: Array<Vector2>): Pair<Int, Int>? {
        for (i: Int in polyA.indices) {
            val p1: Vector2 = polyA[i]
            val p2: Vector2 = polyA[(i + 1) % polyA.size]
            polyB.indices.forEach { j ->
                val p3: Vector2 = polyB[j]
                val p4: Vector2 = polyB[(j + 1) % polyB.size]
                if (p1 == p4 && p2 == p3) return Pair(i, j)
            }
        }
        return null
    }

    /**
     * Validates the polygon vertices
     *
     * Checks if the polygon is valid by checking if any
     * of the edges between two neighboring vertices intersect
     *
     * @return true if the polygon is valid
     */
    fun isPolygonValid(vertices: MutableList<Vector2>): Boolean {
        val size: Int = vertices.size
        if (size < 3) return false

        return !areEdgesIntersecting(vertices)
    }

    /**
     * Checks if the lines of the polygon are intersecting
     *
     * @return true if the lines are intersecting
     */
    private fun areEdgesIntersecting(vertices: MutableList<Vector2>): Boolean {
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
     * Checks if two lines intersect
     *
     * @param lineA the first line
     * @param lineB the second line
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

            if (!isConvex(a, b, c)) return true
        }
        return false
    }

    /**
     * Checks if the angle between three points is convex
     *
     * @param p1 the first point
     * @param p2 the second point
     * @param p3 the third point
     * @return true if the angle is convex
     */
    private fun isConvex(p1: Vector2, p2: Vector2, p3: Vector2): Boolean {
        return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x) > 0
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
}