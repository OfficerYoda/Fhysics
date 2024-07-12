package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.ProjectionResult
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.*
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min

object CollisionFinder {

    private const val EPSILON: Float = 0.0001f

    /// region =====Collision Detection=====
    /**
     * Tests for collision between two circles
     *
     * @param circleA The first circle
     * @param circleB The second circle
     * @return A CollisionInfo object containing information about the collision
     */
    fun testCollision(circleA: Circle, circleB: Circle): CollisionInfo {
        // Calculate squared distance between circle centers
        val sqrDst: Float = circleA.position.sqrDistanceTo(circleB.position)

        val radii: Float = circleA.radius + circleB.radius

        // Check if the circles don't overlap
        if (sqrDst >= radii * radii) return CollisionInfo()

        // Calculate collision normal and overlap
        val collisionNormal: Vector2 = (circleB.position - circleA.position).normalized()
        val projA: Projection = circleA.project(collisionNormal)
        val projB: Projection = circleB.project(collisionNormal)
        val overlap: Float = min(
            projA.max - projB.min,
            projB.max - projA.min
        )

        return CollisionInfo(circleA, circleB, collisionNormal, overlap)
    }

    /**
     * Tests for collision between a polygon and a circle
     *
     * @param poly The polygon
     * @param circle The circle
     * @return A CollisionInfo object containing information about the collision
     */
    fun testCollision(poly: Polygon, circle: Circle): CollisionInfo {
        if (!poly.boundingBox.overlaps(circle.boundingBox)) return CollisionInfo()

        if (poly is ConcavePolygon) {
            return testConcavePolygonCollision(poly, circle)
        }

        val axes: Set<Vector2> = poly.getAxes()

        axes.forEach { axis: Vector2 ->
            val projResult: ProjectionResult = testProjectionOverlap(axis, poly, circle)

            if (!projResult.hasOverlap) return CollisionInfo()
        }

        val closestPoint: Vector2 = getClosestPoint(poly, circle.position)

        // Do a final check onto the axis from the circle to the closest point
        val finalAxis: Vector2 = (closestPoint - circle.position).normalized()
        val projResult: ProjectionResult = testProjectionOverlap(finalAxis, poly, circle)
        if (!projResult.hasOverlap) return CollisionInfo()

        // Make sure the normal points in the right direction
        if (finalAxis.dot(poly.position - circle.position) < 0) {
            finalAxis.negate()
        }

        val targetPoly: Polygon = if (poly is SubPolygon) poly.parent else poly
        return CollisionInfo(circle, targetPoly, finalAxis, projResult.getOverlap())
    }

    /**
     * Tests for collision between two polygons
     *
     * @param polyA The first polygon
     * @param polyB The second polygon
     * @return A CollisionInfo object containing information about the collision
     */
    fun testCollision(polyA: Polygon, polyB: Polygon): CollisionInfo {
        if (!polyA.boundingBox.overlaps(polyB.boundingBox)) return CollisionInfo()

        if (polyA is ConcavePolygon || polyB is ConcavePolygon) {
            return testConcavePolygonCollision(polyA, polyB)
        }

        val axes: Set<Vector2> = polyA.getAxes() + polyB.getAxes()
        var normal: Vector2 = Vector2.ZERO
        var depth: Float = Float.MAX_VALUE

        axes.forEach { axis: Vector2 ->
            val projResult: ProjectionResult = testProjectionOverlap(axis, polyA, polyB)

            if (!projResult.hasOverlap) return CollisionInfo()

            val overlap: Float = projResult.getOverlap()

            // Check if the overlap is the smallest so far
            if (overlap < depth) {
                depth = overlap
                normal = axis
            }
        }

        // Make sure the normal points in the right direction
        if (normal.dot(polyB.position - polyA.position) < 0) {
            normal.negate()
        }

        return CollisionInfo(polyA, polyB, normal, depth)
    }

    /**
     * Tests for collision between a concave polygon and a circle
     *
     * @param poly The concave polygon
     * @param circle The circle
     * @return A CollisionInfo object containing information about the collision
     */
    private fun testConcavePolygonCollision(poly: ConcavePolygon, circle: Circle): CollisionInfo {
        var deepestCollision = CollisionInfo()

        // Check for collision between the circle and every sub-polygon
        poly.subPolygons.forEach { subPoly: Polygon ->
            val collisionInfo: CollisionInfo = testCollision(subPoly, circle)
            if (!collisionInfo.hasCollision) return@forEach
            if (abs(deepestCollision.depth) < abs(collisionInfo.depth) || deepestCollision.depth == Float.NEGATIVE_INFINITY) {
                deepestCollision = collisionInfo
            }
        }

        return deepestCollision
    }

    /**
     * Tests for collision between one or two concave polygons
     * This method is called when at least one of the polygons is a concave polygon
     *
     * @param polyA The first polygon
     * @param polyB The second polygon
     * @return A CollisionInfo object containing information about the collision
     */
    private fun testConcavePolygonCollision(polyA: Polygon, polyB: Polygon): CollisionInfo {
        var deepestCollision = CollisionInfo()

        val polygonsA: List<Polygon> = if (polyA is ConcavePolygon) polyA.subPolygons else listOf(polyA)
        val polygonsB: List<Polygon> = if (polyB is ConcavePolygon) polyB.subPolygons else listOf(polyB)

        // Check for collision between every sub-polygon pair
        for (subPolyA: Polygon in polygonsA) {
            for (subPolyB: Polygon in polygonsB) {
                val info: CollisionInfo = testCollision(subPolyA, subPolyB)

                if (!info.hasCollision) continue

                if (abs(deepestCollision.depth) < abs(info.depth) || deepestCollision.depth == Float.NEGATIVE_INFINITY) {
                    deepestCollision = info
                }
            }
        }

        if (deepestCollision.hasCollision) {

            // Make sure the normal points in the right direction
            val normal: Vector2 = deepestCollision.normal
            if (normal.dot(deepestCollision.objB!!.position - deepestCollision.objA!!.position) < 0) {
                normal.negate()
            }

            deepestCollision = CollisionInfo(polyA, polyB, normal, deepestCollision.depth)
        }

        return deepestCollision
    }

    /**
     * Tests for overlap between the projections of two objects onto an axis
     *
     * @param axis The axis
     * @param objA The first object
     * @param objB The second object
     * @return A boolean indicating if the projections overlap
     */
    private fun testProjectionOverlap(axis: Vector2, objA: FhysicsObject, objB: FhysicsObject): ProjectionResult {
        val projectionA: Projection = objA.project(axis)
        val projectionB: Projection = objB.project(axis)

        return ProjectionResult(projectionA, projectionB)
    }

    /**
     * Gets the closest point on the polygon to the external point
     *
     * @param poly The polygon
     * @param point The external point
     */
    private fun getClosestPoint(poly: Polygon, point: Vector2): Vector2 {
        var closestPoint: Vector2 = Vector2.ZERO
        var minDistance: Float = Float.MAX_VALUE

        val vertices: Array<Vector2> = poly.getTransformedVertices()

        for (i: Int in vertices.indices) {
            val start: Vector2 = vertices[i]
            val end: Vector2 = vertices[(i + 1) % vertices.size]

            // Get the closest point on the current edge to the external point
            val closestPointOnEdge: Vector2 = getClosestPointOnEdge(start, end, point)
            val distance: Float = closestPointOnEdge.sqrDistanceTo(point)

            if (distance < minDistance) {
                closestPoint = closestPointOnEdge
                minDistance = distance
            }
        }

        return closestPoint
    }

    /**
     * Gets the closest point on an edge to an external point
     *
     * @param start The start point of the edge
     * @param end The end point of the edge
     * @param point The external point
     * @return The closest point on the edge to the external point
     */
    private fun getClosestPointOnEdge(start: Vector2, end: Vector2, point: Vector2): Vector2 {
        // Calculate the closest point on the current edge to the external point
        val edge: Vector2 = end - start
        val t: Float = ((point - start).dot(edge)) / edge.sqrMagnitude()

        // Get the closest point on the edge
        val closestPointOnEdge: Vector2 = when {
            t < 0.0 -> start
            t > 1.0 -> end
            else -> start + edge * t
        }

        return closestPointOnEdge
    }
    /// endregion

    /// region =====Contact Points=====
    /// region =====Object-Object=====
    /**
     * Finds the contact points between two circles
     *
     * @param circleA The first circle
     * @param circleB The second circle
     * @param info The CollisionInfo object containing information about the collision
     * @return An array containing the contact points
     */
    fun findContactPoints(circleA: Circle, circleB: Circle, info: CollisionInfo): Array<Vector2> {
        val contactPoint: Vector2 = circleA.position + info.normal * circleA.radius
        return arrayOf(contactPoint)
    }

    /**
     * Finds the contact points between a polygon and a circle
     *
     * @param poly The polygon
     * @param circle The circle
     * @param info The CollisionInfo object containing information about the collision
     * @return An array containing the contact points
     */
    fun findContactPoints(poly: Polygon, circle: Circle, info: CollisionInfo): Array<Vector2> {
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

                val closestPoint: Vector2 = getClosestPointOnEdge(va, vb, vertex)
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

                val closestPoint: Vector2 = getClosestPointOnEdge(va, vb, vertex)
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
    /// endregion
}