package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.Rectangle
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
        if (sqrDst >= radii * radii)
            return CollisionInfo()

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
        // Get the closest point on the rect to the circle's center
        val closestPoint: Vector2 = getClosestPoint(rect, circle.position)

        // Calculate the vector offset from the circles center to the closest point
        var offset: Vector2 = closestPoint - circle.position

        // Check if circle doesn't overlap with the rect
        if (offset.sqrMagnitude() >= circle.radius * circle.radius)
            return CollisionInfo()

        // Get the closest point on the rect's edge to the circle's center
        val edgePair: Pair<Vector2, Int> = getClosestPointOnEdge(rect, closestPoint)
        val closestPointOnEdge: Vector2 = edgePair.first

        offset = closestPointOnEdge - circle.position

        // Calculate collision normal and overlap
        val collisionNormal: Vector2 = (-circle.position + closestPointOnEdge).normalized()
        val overlap: Float = -offset.magnitude() + circle.radius * edgePair.second

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
        // Coerce external point coordinates to be within rect boundaries
        val closestX: Float = externalPoint.x.coerceIn(rect.minX, rect.maxX)
        val closestY: Float = externalPoint.y.coerceIn(rect.minY, rect.maxY)

        return Vector2(closestX, closestY)
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