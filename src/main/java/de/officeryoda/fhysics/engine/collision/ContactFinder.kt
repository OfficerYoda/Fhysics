package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.FhysicsCore.EPSILON
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.ConcavePolygon
import de.officeryoda.fhysics.engine.objects.Polygon
import kotlin.math.absoluteValue

object ContactFinder {

    /// region =====Object-Object=====
    /**
     * Finds the contact points in a collision with a circle
     *
     * @param circle The circle
     * @param info The CollisionInfo object containing information about the collision
     * @return An array containing the contact points
     */
    fun findContactPoints(circle: Circle, info: CollisionInfo): Array<Vector2> {
        val offset: Vector2 = info.normal * circle.radius
        if (info.objB == circle) offset.negate()

        return arrayOf(circle.position + offset)
    }

    /**
     * Finds the contact points between two polygons
     *
     * @param polyA The first polygon
     * @param polyB The second polygon
     * @return An array containing the contact points
     */
    fun findContactPoints(polyA: Polygon, polyB: Polygon): Array<Vector2> {
        if (polyA is ConcavePolygon || polyB is ConcavePolygon) {
            return findConcavePolygonContactPoints(polyA, polyB)
        }

        val contactPoints: Array<Vector2> = arrayOf(Vector2.ZERO, Vector2.ZERO)
        var contactCount = 0
        var minDistance: Float = Float.MAX_VALUE

        val verticesA: Array<Vector2> = polyA.getTransformedVertices()
        val verticesB: Array<Vector2> = polyB.getTransformedVertices()

        // Check for contact points between the edges of polyA and the vertices of polyB
        for (i: Int in verticesA.indices) {
            val va: Vector2 = verticesA[i]
            val vb: Vector2 = verticesA[(i + 1) % verticesA.size]
            for (j: Int in verticesB.indices) {
                val vertex: Vector2 = verticesB[j]

                val closestPoint: Vector2 = CollisionFinder.getClosestPointOnEdge(va, vb, vertex)
                val sqrDistance: Float = vertex.sqrDistanceTo(closestPoint)

                if (nearlyEquals(sqrDistance, minDistance) && !nearlyEquals(closestPoint, contactPoints[0])) {
                    contactPoints[1] = closestPoint
                    contactCount = 2
                } else if (sqrDistance < minDistance) {
                    minDistance = sqrDistance
                    contactPoints[0] = closestPoint
                    contactCount = 1
                }
            }
        }

        // Check for contact points between the edges of polyB and the vertices of polyA
        for (i: Int in verticesB.indices) {
            val va: Vector2 = verticesB[i]
            val vb: Vector2 = verticesB[(i + 1) % verticesB.size]
            for (j: Int in verticesA.indices) {
                val vertex: Vector2 = verticesA[j]

                val closestPoint: Vector2 = CollisionFinder.getClosestPointOnEdge(va, vb, vertex)
                val sqrDistance: Float = vertex.sqrDistanceTo(closestPoint)

                if (nearlyEquals(sqrDistance, minDistance) && !nearlyEquals(closestPoint, contactPoints[0])) {
                    contactPoints[1] = closestPoint
                    contactCount = 2
                } else if (sqrDistance < minDistance) {
                    minDistance = sqrDistance
                    contactPoints[0] = closestPoint
                    contactCount = 1
                }
            }
        }

        // To ensure that the contact point is really a contact
        return if (minDistance > EPSILON) {
            arrayOf()
        } else {
            if (contactCount == 2) contactPoints else arrayOf(contactPoints[0])
        }
    }

    /**
     * Finds the contact points between two polygons when at least one of them is a concave polygon
     *
     * @param polyA The first polygon
     * @param polyB The second polygon
     */
    private fun findConcavePolygonContactPoints(polyA: Polygon, polyB: Polygon): Array<Vector2> {
        val contactPoints: MutableList<Vector2> = mutableListOf()

        val polygonsA: List<Polygon> = if (polyA is ConcavePolygon) polyA.subPolygons else listOf(polyA)
        val polygonsB: List<Polygon> = if (polyB is ConcavePolygon) polyB.subPolygons else listOf(polyB)

        // Check for contact points between every sub-polygon pair
        for (subPolyA: Polygon in polygonsA) {
            for (subPolyB: Polygon in polygonsB) {
                if (!subPolyA.boundingBox.overlaps(subPolyB.boundingBox)) continue

                val subContactPoints: Array<Vector2> = findContactPoints(subPolyA, subPolyB)

                subContactPoints.forEach {
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
     * Finds the contact points between a border and a circle
     *
     * @param border The border
     * @param circle The circle
     * @return An array containing the contact points
     */
    fun findContactPoints(border: BorderEdge, circle: Circle): Array<Vector2> {
        val contactPoint: Vector2 = circle.position + border.normal * circle.radius
        return arrayOf(contactPoint)
    }

    /**
     * Finds the contact points between a border and a polygon
     *
     * @param border The border
     * @param poly The polygon
     * @return An array containing the contact points
     */
    fun findContactPoints(border: BorderEdge, poly: Polygon): Array<Vector2> {
        if (poly is ConcavePolygon) {
            return findConcavePolygonContactPoints(border, poly)
        }

        val contactPoints: Array<Vector2> = arrayOf(Vector2.ZERO, Vector2.ZERO)
        var contactCount = 0
        val tangent = Vector2(-border.normal.y, border.normal.x)
        val vertices: Array<Vector2> = poly.getTransformedVertices()
        var minDistance: Float = Float.MAX_VALUE

        for (i: Int in vertices.indices) {
            val vertex: Vector2 = vertices[i]
            val closestPoint: Vector2 = border.edgeCorner + tangent * (vertex - border.edgeCorner).dot(tangent)

            val sqrDistance: Float = vertex.sqrDistanceTo(closestPoint)
            if (sqrDistance > EPSILON) continue

            if (nearlyEquals(sqrDistance, minDistance) && !nearlyEquals(closestPoint, contactPoints[0])) {
                contactPoints[1] = closestPoint
                contactCount = 2
            } else if (sqrDistance < minDistance) {
                minDistance = sqrDistance
                contactPoints[0] = closestPoint
                contactCount = 1
            }
        }

        return when (contactCount) {
            2 -> contactPoints
            1 -> arrayOf(contactPoints[0])
            else -> arrayOf()
        }
    }

    /**
     * Finds the contact points between a border and a concave polygon
     *
     * @param border The border
     * @param concavePolygon The concave polygon
     * @return An array containing the contact points
     */
    private fun findConcavePolygonContactPoints(border: BorderEdge, concavePolygon: ConcavePolygon): Array<Vector2> {
        val contactPoints: MutableList<Vector2> = mutableListOf()

        for (subPoly: Polygon in concavePolygon.subPolygons) {
            val subContactPoints: Array<Vector2> = findContactPoints(border, subPoly)
            contactPoints.addAll(subContactPoints)
        }

        return contactPoints.toTypedArray()
    }
    /// endregion

    /// region =====Helper Methods=====
    /**
     * Checks if a contact point is near any existing contact points
     *
     * @param contactPoint The contact point to check
     * @param existingContactPoints The existing contact points
     * @return A boolean indicating if the contact point is near any existing contact points
     */
    private fun isNearExisting(contactPoint: Vector2, existingContactPoints: List<Vector2>): Boolean {
        return existingContactPoints.firstOrNull { nearlyEquals(contactPoint, it) } != null
    }

    /**
     * Checks if two floats are nearly equal
     *
     * @param a The first float
     * @param b The second float
     * @return A boolean indicating if the floats are nearly equal
     */
    private fun nearlyEquals(a: Float, b: Float): Boolean {
        return (a - b).absoluteValue < EPSILON
    }

    /**
     * Checks if two vectors are nearly equal
     *
     * @param a The first vector
     * @param b The second vector
     * @return A boolean indicating if the vectors are nearly equal
     */
    fun nearlyEquals(a: Vector2, b: Vector2): Boolean {
        return a.sqrDistanceTo(b) < EPSILON
    }
    /// endregion
}