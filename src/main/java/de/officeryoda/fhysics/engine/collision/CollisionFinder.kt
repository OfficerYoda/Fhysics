package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.ProjectionResult
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.Rectangle
import kotlin.math.min
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

    /**
     * Tests for collision between a circle and a rectangle
     *
     * @param circle The circle
     * @param rect The rectangle
     * @return A CollisionInfo object containing information about the collision
     */
    fun testCollision(circle: Circle, rect: Rectangle): CollisionInfo {
        if (!rect.boundingBox.overlaps(circle.boundingBox)) {
            return CollisionInfo()
        }

        // Get the rectangle's axes (normals of its sides)
        val axes: List<Vector2> = rect.getAxes()

        // For each axis...
        axes.forEach { axis: Vector2 ->
            if (!testProjectionOverlap(axis, rect, circle).hasOverlap) return CollisionInfo()
        }

        // Get the closest point on the rectangle to the circle's center
        val closestPoint: Vector2 = getClosestPoint(rect, circle.position)

        // Do a final check onto the axis from the circle to the closest point
        val finalAxis: Vector2 = (closestPoint - circle.position).normalized()
        val projResult: ProjectionResult = testProjectionOverlap(finalAxis, circle, rect)
        if (!projResult.hasOverlap) return CollisionInfo()

        // Calculate the collision normal and overlap with the final axis
        if(finalAxis.dot(rect.position - circle.position) < 0) {
            finalAxis.negate()
        }

        val overlap: Float = min(
            projResult.projectionA.max - projResult.projectionB.min,
            projResult.projectionB.max - projResult.projectionA.min
        )

        return CollisionInfo(circle, rect, finalAxis, overlap)
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
        // Project the rectangle and the circle onto the axis
        val projectionA: Projection = objA.project(axis)
        val projectionB: Projection = objB.project(axis)

        // If the projections do not overlap, then the rectangle and the circle do not collide
        val overlap: Boolean = projectionA.overlaps(projectionB)

        return ProjectionResult(overlap, projectionA, projectionB)
    }

    /**
     * Tests for collision between two rectangles
     *
     * @param rect1 The first rectangle
     * @param rect2 The second rectangle
     * @return A CollisionInfo object containing information about the collision
     */
    fun testCollision(rect1: Rectangle, rect2: Rectangle): CollisionInfo {
//        TODO("Not yet implemented")
        return CollisionInfo()
    }

    /**
     * Gets the closest point on the rect to the external point
     *
     * @param rect The rectangle
     * @param externalPoint The external point
     */
    private fun getClosestPoint(rect: Rectangle, externalPoint: Vector2): Vector2 {
        // Transform the external point to the rectangle's local coordinate system
        val localPoint: Vector2 = externalPoint.rotatedAround(rect.position, -rect.rotation)

        val halfWidth: Float = rect.width / 2
        val halfHeight: Float = rect.height / 2

        // Coerce local point coordinates to be within rect boundaries
        val localClosestX: Float =
            localPoint.x.coerceIn(rect.position.x - halfWidth, rect.position.x + halfWidth)
        val localClosestY: Float =
            localPoint.y.coerceIn(rect.position.y - halfHeight, rect.position.y + halfHeight)

        // Transform the local closest point back to the global coordinate system
        val globalClosestPoint: Vector2 =
            Vector2(localClosestX, localClosestY).rotatedAround(rect.position, rect.rotation)

        return globalClosestPoint
    }
}