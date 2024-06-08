package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2

/**
 * Converts polygons to convex polygons.
 */
object PolygonCreator {

    fun createPolygon(vertices: Array<Vector2>): Polygon {
        ensureCCW(vertices)
        if (!isConcave(vertices)) {
            // create the polygon
            return ConvexPolygon(vertices)
        } else {
            val polygonsVertices: List<List<Vector2>> = concavePolygonExample(vertices.toList())
            for (polygonVertices: List<Vector2> in polygonsVertices) {
                val polygon = ConvexPolygon(polygonVertices.toTypedArray())
                polygon.static = true
                FhysicsCore.spawn(polygon)
            }
            // return dummy polygon for now TODO: implement concave polygon class
            return ConvexPolygon(arrayOf(Vector2(), Vector2(), Vector2()))
        }
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

    /**
     * Validates the polygon vertices
     * Checks if the polygon is valid by checking if the lines intersect
     * and if the polygon is convex
     *
     * @return true if the polygon is valid
     */
    fun validatePolyVertices(vertices: MutableList<Vector2>): Boolean {
        val size: Int = vertices.size
        if (size < 3) return false

        return !areLinesIntersecting(vertices)
//                && !isConcave(vertices)
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
     * @return true if the polygon is concave
     */
    private fun isConcave(vertices: Array<Vector2>): Boolean {
        ensureCCW(vertices)
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
     * Returns the vertices in counter-clockwise order
     *
     * @param vertices the vertices of the polygon
     */
    private fun ensureCCW(vertices: Array<Vector2>) {
        // Calculate the signed area of the polygon
        var signedArea = 0f
        for (i: Int in vertices.indices) {
            val j: Int = (i + 1) % vertices.size
            signedArea += vertices[i].x * vertices[j].y - vertices[j].x * vertices[i].y
        }
        signedArea /= 2

        // Reverse the vertices if the polygon is CW
        if (signedArea < 0) vertices.reverse()
    }
}