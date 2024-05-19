package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.intersects
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.Rectangle
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.sqrt

object CollisionFinder {

    private const val EPSILON: Float = 0.0001F

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
        if (!Rectangle2D.Float(rect.minX, rect.minY, rect.maxX - rect.minX, rect.maxY - rect.minY).intersects(circle)) {
            return CollisionInfo()
        }

        // Get the rectangle's axes (normals of its sides)
        val axes: List<Vector2> = rect.getAxes()

        // For each axis...
        for (axis: Vector2 in axes) {
            // Project the rectangle and the circle onto the axis
            val projection1: Projection = rect.project(axis)
            val projection2: Projection = circle.project(axis)

            // If the projections do not overlap, then the rectangle and the circle do not collide
            if (!projection1.overlaps(projection2)) {
                return CollisionInfo()
            }
        }

        // Get the closest point on the rectangle to the circle's center
        val closestPoint: Vector2 = getClosestPoint(rect, circle.position)

        // Get the closest point on the rect's edge to the circle's center
        val edgePair: Pair<Vector2, Int> = getClosestPointOnEdge(rect, closestPoint)
        val offset: Vector2 = circle.position - edgePair.first

        // Calculate the collision normal and overlap based on the closest point
        val collisionNormal: Vector2 = offset.normalized()
        val overlap: Float = offset.magnitude() - circle.radius * edgePair.second

        return CollisionInfo(circle, rect, collisionNormal, overlap)
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
        val localPoint: Vector2 = externalPoint.rotateAround(rect.position, -rect.rotation)

        val halfWidth: Float = rect.width / 2
        val halfHeight: Float = rect.height / 2

        // Coerce local point coordinates to be within rect boundaries
        val localClosestX: Float =
            localPoint.x.coerceIn(rect.position.x - halfWidth, rect.position.x + halfWidth)
        val localClosestY: Float =
            localPoint.y.coerceIn(rect.position.y - halfHeight, rect.position.y + halfHeight)


        // Transform the local closest point back to the global coordinate system
        val globalClosestPoint: Vector2 =
            Vector2(localClosestX, localClosestY).rotateAround(rect.position, rect.rotation)

        return globalClosestPoint
    }

    /**
     * Gets the closest point on the rectangle's edge to the external point
     * and an integer that represents if the external point is inside (-1) or outside (1) the rectangle
     *
     * @param rect The rectangle
     * @param closestPoint The closest point on the rectangle to the external point
     * @return A pair containing the closest point on the rectangle's edge and an integer that represents if the external point is outside the rectangle
     */
    private fun getClosestPointOnEdge(rect: Rectangle, closestPoint: Vector2): Pair<Vector2, Int> {
        val closestRotatedPoint: Vector2 = closestPoint.rotateAround(rect.position, -rect.rotation)

        val halfWidth: Float = rect.width / 2
        val halfHeight: Float = rect.height / 2

        // Calculate the distance from the closest point to the rect's edges
        val dx1: Float = closestRotatedPoint.x - (rect.position.x - halfWidth)
        val dx2: Float = closestRotatedPoint.x - (rect.position.x + halfWidth)
        val dy1: Float = closestRotatedPoint.y - (rect.position.y - halfHeight)
        val dy2: Float = closestRotatedPoint.y - (rect.position.y + halfHeight)

        val dx: Float = if (abs(dx1) < abs(dx2)) dx1 else dx2
        val dy: Float = if (abs(dy1) < abs(dy2)) dy1 else dy2

        // check if the external point is inside the rect
        val insideBox: Boolean = dx1 > EPSILON && dx2 < -EPSILON && dy1 > EPSILON && dy2 < -EPSILON
        val radiusSign: Int = if (insideBox) -1 else 1 // the sign used for the radius further down in the calculation

        // return the closest point on the rect's edge
        return if (abs(dx) < abs(dy)) {
            Pair(
                Vector2(closestRotatedPoint.x - dx, closestRotatedPoint.y).rotateAround(rect.position, rect.rotation),
                radiusSign
            )
        } else {
            Pair(
                Vector2(closestRotatedPoint.x, closestRotatedPoint.y - dy).rotateAround(rect.position, rect.rotation),
                radiusSign
            )
        }
    }
}