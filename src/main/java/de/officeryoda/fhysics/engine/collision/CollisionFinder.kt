package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.math.Projection
import de.officeryoda.fhysics.engine.math.ProjectionResult
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.*
import kotlin.math.abs
import kotlin.math.min

object CollisionFinder {

    /**
     * Returns the [CollisionInfo] of the collision between [circleA] and [circleB].
     */
    fun testCollision(circleA: Circle, circleB: Circle): CollisionInfo {
        // Calculate squared distance between circle centers
        val sqrDst: Float = circleA.position.distanceToSqr(circleB.position)

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
     * Returns the [CollisionInfo] of the collision between [poly] and [circle].
     */
    fun testCollision(poly: Polygon, circle: Circle): CollisionInfo {
        if (!poly.boundingBox.overlaps(circle.boundingBox)) return CollisionInfo()

        if (poly is ConcavePolygon) { // TODO Optimize to not use type checking
            return testConcavePolygonCollision(poly, circle)
        }

        val axes: Set<Vector2> = poly.getAxes()

        for (axis: Vector2 in axes) {
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

        val targetPoly: Polygon =
            if (poly is SubPolygon) poly.parent else poly // TODO Optimize to not use type checking
        return CollisionInfo(circle, targetPoly, finalAxis, projResult.getOverlap())
    }

    /**
     * Returns the [CollisionInfo] of the collision between [polyA] and [polyB].
     */
    fun testCollision(polyA: Polygon, polyB: Polygon): CollisionInfo {
        if (!polyA.boundingBox.overlaps(polyB.boundingBox)) return CollisionInfo()

        if (polyA is ConcavePolygon || polyB is ConcavePolygon) { // TODO Optimize to not use type checking
            return testConcavePolygonCollision(polyA, polyB)
        }

        val axes: Set<Vector2> = polyA.getAxes() + polyB.getAxes()
        var normal: Vector2 = Vector2.ZERO
        var depth: Float = Float.MAX_VALUE

        for (axis: Vector2 in axes) {
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
     * Returns the [CollisionInfo] of the collision between [poly] (Concave) and [circle].
     */
    private fun testConcavePolygonCollision(poly: ConcavePolygon, circle: Circle): CollisionInfo {
        var deepestCollision = CollisionInfo()

        // Check for collision between the circle and every sub-polygon
        for (subPoly: Polygon in poly.subPolygons) {
            val collisionInfo: CollisionInfo = testCollision(subPoly, circle)
            if (!collisionInfo.hasCollision) continue
            if (abs(deepestCollision.depth) < abs(collisionInfo.depth) || deepestCollision.depth == Float.NEGATIVE_INFINITY) {
                deepestCollision = collisionInfo
            }
        }

        return deepestCollision
    }

    /**
     * Returns the [CollisionInfo] of the collision between [polyA] and [polyB], where at least one of them is a [ConcavePolygon].
     */
    private fun testConcavePolygonCollision(polyA: Polygon, polyB: Polygon): CollisionInfo {
        var deepestCollision = CollisionInfo()

        val polygonsA: List<Polygon> =
            if (polyA is ConcavePolygon) polyA.subPolygons else listOf(polyA) // TODO Optimize to not use type checking
        val polygonsB: List<Polygon> =
            if (polyB is ConcavePolygon) polyB.subPolygons else listOf(polyB) // TODO Optimize to not use type checking

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
     * Returns the [ProjectionResult] of the projection overlap test between [objA] and [objB] on the given [axis].
     */
    private fun testProjectionOverlap(axis: Vector2, objA: FhysicsObject, objB: FhysicsObject): ProjectionResult {
        val projectionA: Projection = objA.project(axis)
        val projectionB: Projection = objB.project(axis)

        return ProjectionResult(projectionA, projectionB)
    }

    /**
     * Gets the closest point on the [polygon][poly] to the [external point][point].
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
            val distance: Float = closestPointOnEdge.distanceToSqr(point)

            if (distance < minDistance) {
                closestPoint = closestPointOnEdge
                minDistance = distance
            }
        }

        return closestPoint
    }

    /**
     * Returns the closest point on the edge defined by [start] and [end] to the [external point][point].
     */
    fun getClosestPointOnEdge(start: Vector2, end: Vector2, point: Vector2): Vector2 {
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
}