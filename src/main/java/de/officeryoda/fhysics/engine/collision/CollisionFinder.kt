package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.rendering.FhysicsPanel
import kotlin.math.abs
import kotlin.math.sqrt

object CollisionFinder {

    fun testCollision(circleA: Circle, circleB: Circle): CollisionInfo {
        // Calculate squared distance between circle centers
        val sqrDst: Double = circleA.position.sqrDistance(circleB.position)

        val radii: Double = circleA.radius + circleB.radius
        val sqrRadii: Double = radii * radii

        // Check if the circles don't overlap
        if (sqrDst >= sqrRadii)
            return CollisionInfo()

        // Calculate collision normal and overlap
        val collisionNormal: Vector2 = (circleB.position - circleA.position).normalized()
        val overlap: Double = radii - sqrt(sqrDst)

        return CollisionInfo(circleA, circleB, collisionNormal, overlap)
    }

    fun testCollision(circle: Circle, box: Box): CollisionInfo {
        // Get the closest point on the box to the circle's center
        val closestPoint: Vector2 = getClosestPoint(box, circle.position)

        // Calculate the vector offset from the circles center to the closest point
        var offset: Vector2 = closestPoint - circle.position

        // Check if circle doesn't overlap with the box
        if (offset.sqrMagnitude() >= circle.radius * circle.radius)
            return CollisionInfo()

        val closestPointOnEdge: Vector2 = getClosestPointOnEdge(box, closestPoint)
        offset = closestPointOnEdge - circle.position

        FhysicsPanel.INSTANCE.drawPoint(closestPointOnEdge)

        // Calculate collision normal and overlap
        val collisionNormal: Vector2 = (closestPointOnEdge - circle.position).normalized()
        val overlap: Double = circle.radius + offset.magnitude()

        // Calculate collision normal and overlap
//        val collisionNormal: Vector2 = (closestPoint - circle.position).normalized()
//        val overlap: Double = circle.radius - offset.magnitude()

        return CollisionInfo(circle, box, collisionNormal, overlap)
    }

    fun testCollision(box1: Box, box2: Box): CollisionInfo {
//        TODO("Not yet implemented")
        return CollisionInfo()
    }

    private fun getClosestPoint(box: Box, externalPoint: Vector2): Vector2 {
        // Coerce external point coordinates to be within box boundaries
        val closestX = externalPoint.x.coerceIn(box.minX, box.maxX)
        val closestY = externalPoint.y.coerceIn(box.minY, box.maxY)

        return Vector2(closestX, closestY)
    }

    private fun getClosestPointOnEdge(box: Box, closestPoint: Vector2): Vector2 {

        val dx1 = closestPoint.x - box.minX
        val dx2 = closestPoint.x - box.maxX
        val dy1 = closestPoint.y - box.minY
        val dy2 = closestPoint.y - box.maxY

        // check if the external point is inside the box
        val insideBox = dx1 > 0 && dx2 < 0 && dy1 > 0 && dy2 < 0

        val dx = if (abs(dx1) < abs(dx2)) dx1 else dx2
        val dy = if (abs(dy1) < abs(dy2)) dy1 else dy2

        if(!insideBox) return closestPoint

        return if (abs(dx) < abs(dy)) {
            Vector2(closestPoint.x - dx, closestPoint.y)
        } else {
            Vector2(closestPoint.x, closestPoint.y - dy)
        }
    }
}