package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import kotlin.math.sqrt

object CollisionFinder {

    fun testCollision(circleA: Circle, circleB: Circle): CollisionInfo {
        // Calculate squared distance between circle centers
        val sqrDst: Double = circleA.position.sqrDistance(circleB.position)

        val radii: Double = circleA.radius + circleB.radius
        val sqrRadii: Double = radii * radii

        // Check if the circles don't overlap
        if (sqrDst >= sqrRadii) return CollisionInfo()

        // Calculate collision normal and overlap
        val collisionNormal: Vector2 = (circleB.position - circleA.position).normalized()
        val overlap: Double = radii - sqrt(sqrDst)

        return CollisionInfo(circleA, circleB, collisionNormal, overlap)
    }

    fun testCollision(circle: Circle, box: Box): CollisionInfo {
        // Get the closest point on the box to the circle's center
        val closestPoint: Vector2 = getClosestPoint(box, circle.position)

        // Calculate the vector offset from the closest point on the box to the circle's center
        val offset: Vector2 = closestPoint - circle.position

        // Check if circle doesn't overlap with the box
        if (offset.sqrMagnitude() >= circle.radius * circle.radius) return CollisionInfo()

        // Calculate collision normal and overlap
        val collisionNormal: Vector2 = (closestPoint - circle.position).normalized()
        val overlap: Double = circle.radius - offset.magnitude()

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

}