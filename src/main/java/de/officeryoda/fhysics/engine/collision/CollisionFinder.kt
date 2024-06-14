package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.ProjectionResult
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.ConcavePolygon
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import kotlin.math.absoluteValue
import kotlin.math.min

object CollisionFinder {

    /// =====Collision Detection=====

    /**
     * Tests for collision between two circles
     *
     * @param circleA The first circle
     * @param circleB The second circle
     * @return A CollisionInfo object containing information about the collision
     */
    fun testCollision(circleA: Circle, circleB: Circle): CollisionInfo {
        // Calculate squared distance between circle centers
        val sqrDst: Float = circleA.position.sqrDistance(circleB.position)

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

        return CollisionInfo(circle, poly, finalAxis, projResult.getOverlap())
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
        // Check for collision between the circle and every sub-polygon
        poly.subPolygons.forEach { subPoly: Polygon ->
            val collisionInfo: CollisionInfo = testCollision(subPoly, circle)
            if (collisionInfo.hasCollision) {
                return CollisionInfo(circle, poly, collisionInfo.normal, collisionInfo.depth)
            }
        }

        return CollisionInfo()
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
        val polygonsA: List<Polygon> = if (polyA is ConcavePolygon) polyA.subPolygons else listOf(polyA)
        val polygonsB: List<Polygon> = if (polyB is ConcavePolygon) polyB.subPolygons else listOf(polyB)

        // Check for collision between every sub-polygon pair
        for (subPolyA: Polygon in polygonsA) {
            for (subPolyB: Polygon in polygonsB) {
                val collisionInfo: CollisionInfo = testCollision(subPolyA, subPolyB)

                if (!collisionInfo.hasCollision) continue

                val normal: Vector2 = collisionInfo.normal
                if (normal.dot(subPolyB.position - subPolyA.position) < 0) {
                    normal.negate()
                }

                return CollisionInfo(polyA, polyB, normal, collisionInfo.depth)
            }
        }

        return CollisionInfo()
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
        var closestPoint: Pair<Vector2, Float> = Pair(Vector2.ZERO, Float.MAX_VALUE)

        val vertices: Array<Vector2> = poly.getTransformedVertices()

        for (i: Int in vertices.indices) {
            val start: Vector2 = vertices[i]
            val end: Vector2 = vertices[(i + 1) % vertices.size]

            // Get the closest point on the current edge to the external point
            val pointEdgeDistance: Pair<Vector2, Float> = getPointEdgeDistance(start, end, point)

            if (pointEdgeDistance.second < closestPoint.second) {
                closestPoint = pointEdgeDistance
            }
        }

        return closestPoint.first
    }

    /**
     * Gets the closest point on an edge to an external point
     *
     * @param start The start point of the edge
     * @param end The end point of the edge
     * @param point The external point
     * @return A pair containing the closest point on the edge and the square distance to the external point
     */
    private fun getPointEdgeDistance(start: Vector2, end: Vector2, point: Vector2): Pair<Vector2, Float> {
        // Calculate the closest point on the current edge to the external point
        val edge: Vector2 = end - start
        val t: Float = ((point - start).dot(edge)) / edge.sqrMagnitude()

        // Get the closest point on the edge
        val closestPointOnEdge: Vector2 = when {
            t < 0.0 -> start
            t > 1.0 -> end
            else -> start + edge * t
        }

        return Pair(closestPointOnEdge, closestPointOnEdge.sqrDistance(point))
    }

    /// =====Contact Points=====

    // TODO remove redundant parameters

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
        if (info.objA == circle) {
            val contactPoint: Vector2 = circle.position + info.normal * circle.radius
            return arrayOf(contactPoint)
        } else {
            val contactPoint: Vector2 = circle.position - info.normal * circle.radius
            return arrayOf(contactPoint)
        }
    }

    /**
     * Finds the contact points between two polygons
     *
     * @param polyA The first polygon
     * @param polyB The second polygon
     * @param info The CollisionInfo object containing information about the collision
     * @return An array containing the contact points
     */
    fun findContactPoints(polyA: Polygon, polyB: Polygon, info: CollisionInfo): Array<Vector2> {
        var contactA: Vector2 = Vector2.ZERO
        var contactB: Vector2 = Vector2.ZERO
        var contactCount = 0
        var minDistance: Float = Float.MAX_VALUE

        val verticesA: Array<Vector2> = polyA.getTransformedVertices()
        val verticesB: Array<Vector2> = polyB.getTransformedVertices()

        // Check for contact points between the vertices of polyA and the edges of polyB
        for (i: Int in verticesA.indices) {
            val vertex: Vector2 = verticesA[i]
            for (j: Int in verticesB.indices) {
                val va: Vector2 = verticesB[j]
                val vb: Vector2 = verticesB[(j + 1) % verticesB.size]

                val (closestPoint: Vector2, distance: Float) = getPointEdgeDistance(va, vb, vertex)

                if (nearlyEquals(distance, minDistance)) {
                    if (!nearlyEquals(closestPoint, contactA)) {
                        contactB = closestPoint
                        contactCount = 2
                    }
                } else if (distance < minDistance) {
                    minDistance = distance
                    contactA = closestPoint
                    contactCount = 1
                }
            }
        }

        // Check for contact points between the vertices of polyB and the edges of polyA
        for (i: Int in verticesB.indices) {
            val vertex: Vector2 = verticesB[i]
            for (j: Int in verticesA.indices) {
                val va: Vector2 = verticesA[j]
                val vb: Vector2 = verticesA[(j + 1) % verticesA.size]

                val (closestPoint: Vector2, distance: Float) = getPointEdgeDistance(va, vb, vertex)

                if (nearlyEquals(distance, minDistance)) {
                    if (!nearlyEquals(closestPoint, contactA)) {
                        contactB = closestPoint
                        contactCount = 2
                    }
                } else if (distance < minDistance) {
                    minDistance = distance
                    contactA = closestPoint
                    contactCount = 1
                }
            }
        }

        return if (contactCount == 2) arrayOf(contactA, contactB) else arrayOf(contactA)
    }

    /**
     * Checks if two floats are nearly equal
     *
     * @param a The first float
     * @param b The second float
     * @return A boolean indicating if the floats are nearly equal
     */
    private fun nearlyEquals(a: Float, b: Float): Boolean {
        val epsilon = 0.0001f
        return (a - b).absoluteValue < epsilon
    }

    /**
     * Checks if two vectors are nearly equal
     *
     * @param a The first vector
     * @param b The second vector
     * @return A boolean indicating if the vectors are nearly equal
     */
    private fun nearlyEquals(a: Vector2, b: Vector2): Boolean {
        return nearlyEquals(a.x, b.x) && nearlyEquals(a.y, b.y)
    }
}