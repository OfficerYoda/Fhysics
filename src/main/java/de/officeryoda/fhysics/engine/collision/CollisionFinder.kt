package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
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
     * Tests for collision between a circle and a box
     *
     * @param circle The circle
     * @param box The box
     * @return A CollisionInfo object containing information about the collision
     */
    fun testCollision(circle: Circle, box: Box): CollisionInfo {
        // Get the closest point on the box to the circle's center
        val closestPoint: Vector2 = getClosestPoint(box, circle.position)

        // Calculate the vector offset from the circles center to the closest point
        var offset: Vector2 = closestPoint - circle.position

        // Check if circle doesn't overlap with the box
        if (offset.sqrMagnitude() >= circle.radius * circle.radius)
            return CollisionInfo()

        // Get the closest point on the box's edge to the circle's center
        val edgePair: Pair<Vector2, Int> = getClosestPointOnEdge(box, closestPoint)
        val closestPointOnEdge: Vector2 = edgePair.first

        offset = closestPointOnEdge - circle.position

        // Calculate collision normal and overlap
        val collisionNormal: Vector2 = (- circle.position + closestPointOnEdge).normalized()
        val overlap: Float = -offset.magnitude() + circle.radius * edgePair.second

        return CollisionInfo(circle, box, collisionNormal, overlap)
    }

    /**
     * Tests for collision between two boxes
     *
     * @param box1 The first box
     * @param box2 The second box
     * @return A CollisionInfo object containing information about the collision
     */
    fun testCollision(box1: Box, box2: Box): CollisionInfo {
//        TODO("Not yet implemented")
        return CollisionInfo()
    }

    /**
     * Gets the closest point on the box to the external point
     *
     * @param box The box
     * @param externalPoint The external point
     */
    private fun getClosestPoint(box: Box, externalPoint: Vector2): Vector2 {
        // Coerce external point coordinates to be within box boundaries
        val closestX = externalPoint.x.coerceIn(box.minX, box.maxX)
        val closestY = externalPoint.y.coerceIn(box.minY, box.maxY)

        return Vector2(closestX, closestY)
    }

    /**
     * Gets the closest point on the box's edge to the external point
     * and an integer that represents if the external point is outside the box
     *
     * @param box The box
     * @param closestPoint The closest point on the box to the external point
     * @return A pair containing the closest point on the box's edge and an integer that represents if the external point is outside the box
     */
    private fun getClosestPointOnEdge(box: Box, closestPoint: Vector2): Pair<Vector2, Int> {
        // Calculate the distance from the closest point to the box's edges
        val dx1 = closestPoint.x - box.minX
        val dx2 = closestPoint.x - box.maxX
        val dy1 = closestPoint.y - box.minY
        val dy2 = closestPoint.y - box.maxY

        val dx = if (abs(dx1) < abs(dx2)) dx1 else dx2
        val dy = if (abs(dy1) < abs(dy2)) dy1 else dy2

        // check if the external point is inside the box
        val insideBox = dx1 > 0 && dx2 < 0 && dy1 > 0 && dy2 < 0
        val radiusSign = if (insideBox) -1 else 1 // the sign used for the radius further down in the calculation

        // return the closest point on the box's edge
        return if (abs(dx) < abs(dy)) {
            Pair(Vector2(closestPoint.x - dx, closestPoint.y), radiusSign)
        } else {
            Pair(Vector2(closestPoint.x, closestPoint.y - dy), radiusSign)
        }
    }
}