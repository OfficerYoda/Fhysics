package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.intersects
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.Rectangle
import de.officeryoda.fhysics.rendering.RenderUtil
import java.awt.geom.Rectangle2D
import kotlin.math.abs
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

        // Calculate the collision normal and overlap based on the closest point
        val collisionNormal: Vector2 = (circle.position - closestPoint).normalized()
        val overlap: Float = closestPoint.distance(circle.position) - circle.radius

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

        // Coerce local point coordinates to be within rect boundaries
        val localClosestX: Float = localPoint.x.coerceIn(rect.position.x - rect.width / 2, rect.position.x + rect.width / 2)
        val localClosestY: Float = localPoint.y.coerceIn(rect.position.y - rect.height / 2, rect.position.y + rect.height / 2)

        // Transform the local closest point back to the global coordinate system
        val globalClosestPoint: Vector2 =
            Vector2(localClosestX, localClosestY).rotateAround(rect.position, rect.rotation)

        RenderUtil.drawer.addDebugPoint(globalClosestPoint)

        return globalClosestPoint
    }

    /**
     * Gets the closest point on the rectangle's edge to the external point
     * and an integer that represents if the external point is outside the rectangle
     *
     * @param rect The rectangle
     * @param closestPoint The closest point on the rectangle to the external point
     * @return A pair containing the closest point on the rectangle's edge and an integer that represents if the external point is outside the rectangle
     */
    private fun getClosestPointOnEdge(rect: Rectangle, closestPoint: Vector2): Pair<Vector2, Int> {
        // Calculate the distance from the closest point to the rect's edges
        val dx1: Float = closestPoint.x - rect.minX
        val dx2: Float = closestPoint.x - rect.maxX
        val dy1: Float = closestPoint.y - rect.minY
        val dy2: Float = closestPoint.y - rect.maxY

        val dx: Float = if (abs(dx1) < abs(dx2)) dx1 else dx2
        val dy: Float = if (abs(dy1) < abs(dy2)) dy1 else dy2

        // check if the external point is inside the rect
        val insideBox: Boolean = dx1 > 0 && dx2 < 0 && dy1 > 0 && dy2 < 0
        val radiusSign: Int = if (insideBox) -1 else 1 // the sign used for the radius further down in the calculation

        // return the closest point on the rect's edge
        return if (abs(dx) < abs(dy)) {
            Pair(Vector2(closestPoint.x - dx, closestPoint.y), radiusSign)
        } else {
            Pair(Vector2(closestPoint.x, closestPoint.y - dy), radiusSign)
        }
    }
}