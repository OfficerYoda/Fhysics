package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.ProjectionResult
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.ConcavePolygon
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import kotlin.math.sqrt

object CollisionFinder {

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
        val overlap: Float = radii - sqrt(sqrDst)

        return CollisionInfo(circleA, circleB, collisionNormal, overlap)
    }

    fun testCollision(poly: Polygon, circle: Circle): CollisionInfo {
        if (!poly.boundingBox.overlaps(circle.boundingBox)) return CollisionInfo()

        if (poly is ConcavePolygon) {
            poly.subPolygons.forEach { subPoly: Polygon ->
                val collisionInfo: CollisionInfo = testCollision(subPoly, circle)
                if (collisionInfo.hasCollision) {
                    return CollisionInfo(circle, poly, collisionInfo.normal, collisionInfo.depth)
                }
            }
            return CollisionInfo()
        }

        val axes: Set<Vector2> = poly.getAxes()

        axes.forEach { axis: Vector2 ->
            // Project the objects onto the axis
            val projResult: ProjectionResult = testProjectionOverlap(axis, poly, circle)

            // Check for no overlap
            if (!projResult.hasOverlap) return CollisionInfo()
        }

        // Get the closest point on the polygons to the circle's center
        val closestPoint: Vector2 = getClosestPoint(poly, circle.position)

        // Do a final check onto the axis from the circle to the closest point
        val finalAxis: Vector2 = (closestPoint - circle.position).normalized()
        val projResult: ProjectionResult = testProjectionOverlap(finalAxis, poly, circle)
        if (!projResult.hasOverlap) return CollisionInfo()

        if (finalAxis.dot(poly.center - circle.position) < 0) {
            finalAxis.negate()
        }

        return CollisionInfo(circle, poly, finalAxis, projResult.getOverlap())
    }

    fun testCollision(polyA: Polygon, polyB: Polygon): CollisionInfo {
        if (!polyA.boundingBox.overlaps(polyB.boundingBox)) return CollisionInfo()

        if (polyA is ConcavePolygon || polyB is ConcavePolygon) {
            return testConcavePolygonCollision(polyA, polyB)
        }

        val axes: Set<Vector2> = polyA.getAxes() + polyB.getAxes()
        var normal: Vector2 = Vector2.ZERO
        var depth: Float = Float.MAX_VALUE

        axes.forEach { axis: Vector2 ->
            // Project the objects onto the axis
            val projResult: ProjectionResult = testProjectionOverlap(axis, polyA, polyB)

            // Check for no overlap
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

        for (subPolyA: Polygon in polygonsA) {
            for (subPolyB: Polygon in polygonsB) {
                val collisionInfo: CollisionInfo = testCollision(subPolyA, subPolyB)
                if (collisionInfo.hasCollision) {
                    val normal: Vector2 = collisionInfo.normal
                    if (normal.dot(subPolyB.center - subPolyA.center) < 0) {
                        normal.negate()
                    }
                    return CollisionInfo(polyA, polyB, normal, collisionInfo.depth)
                }
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
        // Project the objects onto the axis
        val projectionA: Projection = objA.project(axis)
        val projectionB: Projection = objB.project(axis)

        return ProjectionResult(projectionA, projectionB)
    }

    /**
     * Gets the closest point on the polygon to the external point
     *
     * @param poly The polygon
     * @param externalPoint The external point
     */
    private fun getClosestPoint(poly: Polygon, externalPoint: Vector2): Vector2 {
        var closestPoint: Vector2 = Vector2.ZERO
        var minDistance: Float = Float.MAX_VALUE

        val vertices: List<Vector2> = poly.getTransformedVertices()

        for (i: Int in vertices.indices) {
            val start: Vector2 = vertices[i]
            val end: Vector2 = vertices[(i + 1) % vertices.size] // Wrap around to the first vertex for the last edge

            // Calculate the closest point on the current edge to the external point
            val edge: Vector2 = end - start
            val t: Float = ((externalPoint - start).dot(edge)) / edge.sqrMagnitude()

            // If the closest point on the line defined by the edge is not on the edge itself, ignore it
            val closestPointOnEdge: Vector2 = if (t in 0.0..1.0) start + edge * t else continue

            val distance: Float = closestPointOnEdge.sqrDistance(externalPoint)
            if (distance < minDistance) {
                minDistance = distance
                closestPoint = closestPointOnEdge
            }
        }

        return closestPoint
    }
}