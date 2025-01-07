package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.collision.ContactFinder.getConvexPolygons
import de.officeryoda.fhysics.engine.math.Projection
import de.officeryoda.fhysics.engine.math.ProjectionResult
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.*
import kotlin.math.min

object CollisionFinder {

    /**
     * Returns the [CollisionInfo] of the collision between [circleA] and [circleB].
     */
    fun testCollision(circleA: Circle, circleB: Circle): CollisionInfo {
        val sqrDst: Float = circleA.position.sqrDistanceTo(circleB.position)
        val radii: Float = circleA.radius + circleB.radius

        if (sqrDst >= radii * radii) return CollisionInfo()

        val collisionNormal: Vector2 = (circleB.position - circleA.position).normalized()
        val overlap: Float = calculateOverlap(circleA, circleB, collisionNormal)

        return CollisionInfo(circleA, circleB, collisionNormal, overlap)
    }

    /**
     * Calculates the overlap between two circles along the given [collisionNormal].
     */
    private fun calculateOverlap(circleA: Circle, circleB: Circle, collisionNormal: Vector2): Float {
        val projA: Projection = circleA.project(collisionNormal)
        val projB: Projection = circleB.project(collisionNormal)
        return min(projA.max - projB.min, projB.max - projA.min)
    }

    /**
     * Returns the [CollisionInfo] of the collision between [poly] and [circle].
     */
    fun testCollision(poly: Polygon, circle: Circle): CollisionInfo {
        if (!poly.boundingBox.overlaps(circle.boundingBox)) return CollisionInfo()

        if (poly.type == FhysicsObjectType.CONCAVE_POLYGON) {
            return testConcavePolygonCollision(poly as ConcavePolygon, circle)
        }

        val axes: List<Vector2> = poly.getAxes()
        if (!checkAxesForOverlap(axes, poly, circle)) return CollisionInfo()

        val closestPoint: Vector2 = getClosestPoint(poly, circle.position)
        val finalAxis: Vector2 = (closestPoint - circle.position).normalized()
        val projResult: ProjectionResult = testProjectionOverlap(finalAxis, poly, circle)

        if (!projResult.hasOverlap) return CollisionInfo()

        // Make sure the normal points in the right direction
        if (finalAxis.dot(poly.position - circle.position) < 0) {
            finalAxis.negate()
        }

        val targetPoly: Polygon = if (poly.type == FhysicsObjectType.SUB_POLYGON) (poly as SubPolygon).parent else poly
        return CollisionInfo(circle, targetPoly, finalAxis, projResult.getOverlap())
    }

    /**
     * Returns whether the given [polygon][poly] and [circle] overlap on any of the given [axes].
     */
    private fun checkAxesForOverlap(axes: List<Vector2>, poly: Polygon, circle: Circle): Boolean {
        for (axis: Vector2 in axes) {
            val projResult: ProjectionResult = testProjectionOverlap(axis, poly, circle)
            if (!projResult.hasOverlap) return false
        }
        return true
    }

    /**
     * Returns the [CollisionInfo] of the collision between [polyA] and [polyB].
     */
    fun testCollision(polyA: Polygon, polyB: Polygon): CollisionInfo {
        if (!polyA.boundingBox.overlaps(polyB.boundingBox)) return CollisionInfo()

        if (polyA.type == FhysicsObjectType.CONCAVE_POLYGON || polyB.type == FhysicsObjectType.CONCAVE_POLYGON) {
            return testConcavePolygonCollision(polyA, polyB)
        }

        val axes: List<Vector2> = getUniqueAxes(polyA, polyB)
        return findCollisionInfo(polyA, polyB, axes)
    }

    /**
     * Returns the unique axes of the given [polygons][polyA] and [polyB].
     */
    private fun getUniqueAxes(polyA: Polygon, polyB: Polygon): List<Vector2> {
        val axes: MutableList<Vector2> = polyA.getAxes().toMutableList()
        val axesB: List<Vector2> = polyB.getAxes()

        // Add axes from polyB that are not already in axes
        for (axis: Vector2 in axesB) {
            if (axis !in axes) {
                axes.add(axis)
            }
        }

        return axes
    }

    /**
     * Returns the [CollisionInfo] of the collision between [polyA] and [polyB] on the given [axes].
     */
    private fun findCollisionInfo(polyA: Polygon, polyB: Polygon, axes: List<Vector2>): CollisionInfo {
        var normal: Vector2 = Vector2.ZERO
        var depth: Float = Float.MAX_VALUE

        // Check for overlap on every axis
        for (axis: Vector2 in axes) {
            val projResult: ProjectionResult = testProjectionOverlap(axis, polyA, polyB)
            if (!projResult.hasOverlap) return CollisionInfo()

            // Check if the overlap is the smallest so far
            val overlap: Float = projResult.getOverlap()
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
     * Returns the [CollisionInfo] of the collision between the given [concave polygon][poly] and [circle].
     */
    private fun testConcavePolygonCollision(poly: ConcavePolygon, circle: Circle): CollisionInfo {
        var deepestCollision = CollisionInfo()

        // Find the deepest collision
        for (subPoly: Polygon in poly.subPolygons) {
            val currentCollision: CollisionInfo = testCollision(subPoly, circle)
            if (!currentCollision.hasCollision) continue

            if (currentCollision.depth > deepestCollision.depth || deepestCollision.depth == Float.POSITIVE_INFINITY) {
                deepestCollision = currentCollision
            }
        }

        return deepestCollision
    }

    /**
     * Returns the [CollisionInfo] of the collision between [polyA] and [polyB].
     */
    private fun testConcavePolygonCollision(polyA: Polygon, polyB: Polygon): CollisionInfo {
        var deepestCollision: CollisionInfo = findDeepestCollision(polyA, polyB)

        if (deepestCollision.hasCollision) {
            // Make sure the normal points in the right direction
            val normal: Vector2 = deepestCollision.normal
            val bToA: Vector2 = deepestCollision.objB!!.position - deepestCollision.objA!!.position
            if (normal.dot(bToA) < 0) {
                normal.negate()
            }

            deepestCollision = CollisionInfo(polyA, polyB, normal, deepestCollision.depth)
        }

        return deepestCollision
    }

    /**
     * Returns the deepest collision between the given [polygons][polyA] and [polyB].
     */
    private fun findDeepestCollision(polyA: Polygon, polyB: Polygon): CollisionInfo {
        var deepestCollision = CollisionInfo()
        val (polygonsA: List<Polygon>, polygonsB: List<Polygon>) = getConvexPolygons(polyA, polyB)

        for (subPolyA: Polygon in polygonsA) {
            for (subPolyB: Polygon in polygonsB) {
                val currentCollision: CollisionInfo = testCollision(subPolyA, subPolyB)
                if (!currentCollision.hasCollision) continue

                if (currentCollision.depth > deepestCollision.depth || deepestCollision.depth == Float.POSITIVE_INFINITY) {
                    deepestCollision = currentCollision
                }
            }
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
     * Returns the closest point on the given [polygon][poly] to the specified [point].
     */
    private fun getClosestPoint(poly: Polygon, point: Vector2): Vector2 {
        var closestPoint: Vector2 = Vector2.ZERO
        var minDistance: Float = Float.MAX_VALUE

        val vertices: Array<Vector2> = poly.getTransformedVertices()
        for (i: Int in vertices.indices) {
            // Find the distance to the closest point on the edge
            val start: Vector2 = vertices[i]
            val end: Vector2 = vertices[(i + 1) % vertices.size]
            val closestPointOnEdge: Vector2 = getClosestPointOnLine(start, end, point)
            val distance: Float = closestPointOnEdge.sqrDistanceTo(point)

            // Update the closest point if necessary
            if (distance < minDistance) {
                closestPoint = closestPointOnEdge
                minDistance = distance
            }
        }

        return closestPoint
    }

    /**
     * Returns the closest point on the line defined by [start] and [end] to the [external point][point].
     */
    fun getClosestPointOnLine(start: Vector2, end: Vector2, point: Vector2): Vector2 {
        val edge: Vector2 = end - start
        val t: Float = (point - start).dot(edge) / edge.sqrMagnitude()

        return when {
            t < 0.0 -> start
            t > 1.0 -> end
            else -> start + edge * t
        }
    }
}