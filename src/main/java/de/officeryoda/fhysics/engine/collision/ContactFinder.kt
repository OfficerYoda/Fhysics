package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.FhysicsCore.EPSILON
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.ConcavePolygon
import de.officeryoda.fhysics.engine.objects.FhysicsObjectType
import de.officeryoda.fhysics.engine.objects.Polygon

/**
 * A class containing methods to find contact points of objects and borders.
 */
object ContactFinder {

    /// region =====Object-Object=====
    /**
     * Returns an array containing the contact points in the [collision][info] with the given [circle].
     */
    fun findContactPoints(circle: Circle, info: CollisionInfo): Array<Vector2> {
        val offset: Vector2 = info.normal * circle.radius
        if (info.objB == circle) offset.negate()

        return arrayOf(circle.position + offset)
    }

    /**
     * Returns an array containing the contact points between [polyA] and [polyB].
     */
    fun findContactPoints(polyA: Polygon, polyB: Polygon): Array<Vector2> {
        if (polyA.type == FhysicsObjectType.CONCAVE_POLYGON || polyB.type == FhysicsObjectType.CONCAVE_POLYGON) {
            return findConcavePolygonContactPoints(polyA, polyB)
        }

        // More than two contact points are only possible with at least one concave polygon
        val contactPoints: Array<Vector2> = arrayOf(Vector2.ZERO, Vector2.ZERO)
        var contactCount = 0

        val verticesA: Array<Vector2> = polyA.getTransformedVertices()
        val verticesB: Array<Vector2> = polyB.getTransformedVertices()

        /**
         * Adds the contact points between the edges of the polygonA and the vertices of polygonB to the contactPoints array.
         *
         *
         * polygonA is made up of [verticesA] and polygonB is made up of [verticesB].
         */
        fun addContactPoints(verticesA: Array<Vector2>, verticesB: Array<Vector2>) {
            // Check if two contact points have already been found
            if (contactCount == 2) return

            for (i: Int in verticesA.indices) {
                val start: Vector2 = verticesA[i]
                val end: Vector2 = verticesA[(i + 1) % verticesA.size]
                for (vertex: Vector2 in verticesB) {
                    val closestPoint: Vector2 = CollisionFinder.getClosestPointOnLine(start, end, vertex)

                    if (!nearlyEquals(vertex, closestPoint)) {
                        // The vertex is not close enough to the edge
                        continue
                    }

                    if (contactCount == 0 || !nearlyEquals(closestPoint, contactPoints[0])) {
                        contactPoints[contactCount] = closestPoint
                        contactCount++

                        if (contactCount == 2) {
                            // Can't have more than two contact points
                            return
                        }
                    }
                }
            }
        }

        addContactPoints(verticesA, verticesB)
        addContactPoints(verticesB, verticesA)

        return when (contactCount) {
            2 -> contactPoints
            1 -> arrayOf(contactPoints[0])
            else -> arrayOf()
        }
    }

    /**
     * Returns an array containing the contact points between [polyA] and [polyB], where at least one of them is a [ConcavePolygon].
     */
    private fun findConcavePolygonContactPoints(polyA: Polygon, polyB: Polygon): Array<Vector2> {
        // Polygons checks must be done with convex polygons
        val (polygonsA: List<Polygon>, polygonsB: List<Polygon>) = getConvexPolygons(polyA, polyB)

        val contactPoints: Array<Vector2> = findContactPoints(polygonsA, polygonsB)

        return contactPoints
    }

    /**
     * Returns the convex polygons that make up the given polygons.
     *
     * For collision checks, the polygons must be convex. If one of the given polygons is concave, the method returns
     * the sub-polygons of the concave polygon. Otherwise, the method returns the given polygons.
     */
    fun getConvexPolygons(polyA: Polygon, polyB: Polygon): Pair<List<Polygon>, List<Polygon>> {
        val polygonsA: List<Polygon> =
            if (polyA.type == FhysicsObjectType.CONCAVE_POLYGON) (polyA as ConcavePolygon).subPolygons else listOf(polyA)
        val polygonsB: List<Polygon> =
            if (polyB.type == FhysicsObjectType.CONCAVE_POLYGON) (polyB as ConcavePolygon).subPolygons else listOf(polyB)
        return Pair(polygonsA, polygonsB)
    }

    /**
     * Returns an array containing the contact points between the given [polygonsA] and [polygonsB].
     */
    private fun findContactPoints(polygonsA: List<Polygon>, polygonsB: List<Polygon>): Array<Vector2> {
        val contactPoints: MutableList<Vector2> = mutableListOf()

        // Check for contact points between every polygon pair
        for (subPolyA: Polygon in polygonsA) {
            for (subPolyB: Polygon in polygonsB) {
                // Skip if the bounding boxes don't overlap
                if (!subPolyA.boundingBox.overlaps(subPolyB.boundingBox)) continue

                val subContactPoints: Array<Vector2> = findContactPoints(subPolyA, subPolyB)

                for (it: Vector2 in subContactPoints) {
                    if (!isNearExisting(it, contactPoints)) {
                        contactPoints.add(it)
                    }
                }
            }
        }

        return contactPoints.toTypedArray()
    }
    /// endregion

    /// region =====Border-Object=====
    /**
     * Returns an array containing the contact points between the [border] and the given [circle].
     */
    fun findContactPoints(border: BorderEdge, circle: Circle): Array<Vector2> {
        // Circle has been pushed inside the border at this point
        val contactPoint: Vector2 = circle.position + border.normal * circle.radius
        return arrayOf(contactPoint)
    }

    /**
     * Returns an array containing the contact points between the [border] and the given [polygon][poly].
     */
    fun findContactPoints(border: BorderEdge, poly: Polygon): Array<Vector2> {
        if (poly.type == FhysicsObjectType.CONCAVE_POLYGON) {
            return findConcavePolygonContactPoints(border, poly as ConcavePolygon)
        }

        val contactPoints: Array<Vector2> = arrayOf(Vector2.ZERO, Vector2.ZERO)
        var contactCount = 0
        val tangent = Vector2(-border.normal.y, border.normal.x)
        val vertices: Array<Vector2> = poly.getTransformedVertices()

        // Check for contact points between the vertices of the polygon and the border
        for (i: Int in vertices.indices) {
            val vertex: Vector2 = vertices[i]
            val closestPoint: Vector2 = border.edgeCorner + tangent * (vertex - border.edgeCorner).dot(tangent)

            if (!nearlyEquals(vertex, closestPoint)) {
                // The vertex is not close enough to the edge
                continue
            }

            // If no contact Point is found or the found yet
            // or the contact point is not the same as the first one add it to the list
            if (contactCount == 0 || !nearlyEquals(closestPoint, contactPoints[0])) {
                contactPoints[contactCount] = closestPoint
                contactCount++

                if (contactCount == 2) {
                    // Can't have more than two contact points
                    break
                }
            }
        }

        return when (contactCount) {
            2 -> contactPoints
            1 -> arrayOf(contactPoints[0])
            else -> arrayOf()
        }
    }

    /**
     * Returns an array containing the contact points between the [border] and the given [concave polygon][concavePolygon].
     */
    private fun findConcavePolygonContactPoints(border: BorderEdge, concavePolygon: ConcavePolygon): Array<Vector2> {
        val contactPoints: MutableList<Vector2> = mutableListOf()

        for (subPoly: Polygon in concavePolygon.subPolygons) {
            val subContactPoints: Array<Vector2> = findContactPoints(border, subPoly)

            // Add new contact points to the list if they are not near existing ones
            for (it: Vector2 in subContactPoints) {
                if (!isNearExisting(it, contactPoints)) {
                    contactPoints.add(it)
                }
            }
        }

        return contactPoints.toTypedArray()
    }
    /// endregion

    /// region =====Helper Methods=====
    /**
     * Returns a boolean indicating if the given [contactPoint] is near any of the [existingContactPoints].
     */
    private fun isNearExisting(contactPoint: Vector2, existingContactPoints: List<Vector2>): Boolean {
        return existingContactPoints.firstOrNull { nearlyEquals(contactPoint, it) } != null
    }

    /**
     * Returns a boolean indicating if the given Vectors [a] and [b] are nearly equal.
     */
    private fun nearlyEquals(a: Vector2, b: Vector2): Boolean {
        return a.sqrDistanceTo(b) < EPSILON
    }
    /// endregion
}