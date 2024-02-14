package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import kotlin.math.sqrt

object CollisionFinder {

    fun testCollision(circleA: Circle, circleB: Circle): CollisionInfo {
        val sqrDst: Double = circleA.position.sqrDistance(circleB.position)
        val radii: Double = circleA.radius + circleB.radius
        val sqrRadii: Double = radii * radii

        if (sqrDst >= sqrRadii) return CollisionInfo()

        // Calculate the normal vector along the line of collision
        // a vector from circleA in direction of circleB, normalized
        val collisionNormal: Vector2 = (circleB.position - circleA.position).normalized()

        // calculate the overlap
        val overlap: Double = radii - sqrt(sqrDst)

        return CollisionInfo(circleA, circleB, collisionNormal, overlap)
    }

    fun testCollision(circle: Circle, box: Box): CollisionInfo {
        // calculate the closes point on the box to the circles center
        val closestPoint: Vector2 = getClosestPoint(box, circle.position)

        // Calculate the vector from the circle's center to the closest point on the box
        val offset: Vector2 = closestPoint - circle.position

        // Check if the distance between the circle's center and the closest point is less than the circle's radius
        if (offset.sqrMagnitude() < circle.radius * circle.radius) {
            separateCircleFromBox(circle, closestPoint, offset)

            // Calculate relative velocity before collision; obx doesn't move relatively speaking
            val relativeVelocity: Vector2 = box.velocity - circle.velocity

            // Calculate the normal vector along the line of collision
            // a vector from circle in direction of closestPoint, normalized
            val collisionNormal: Vector2 = (closestPoint - circle.position).normalized()

            // Calculate relative velocity along the normal direction
            val relativeVelocityAlongNormal: Double = relativeVelocity.dot(collisionNormal)

            // Calculate impulse (change in momentum)
            val impulse: Double = (2.0 * relativeVelocityAlongNormal) / (circle.mass + box.mass)

            // Apply impulse to update velocities
            val restitution = 1.0
            val impulseMultiplier = impulse * restitution * collisionNormal
            circle.velocity += impulseMultiplier * box.mass
        }

        return CollisionInfo()
    }

    fun testCollision(box1: Box, box2: Box): CollisionInfo {
//        TODO("Not yet implemented")
        return CollisionInfo()
    }

    private fun separateCircleFromBox(circle: Circle, closestPoint: Vector2, offset: Vector2) {
        // from closestPoint to circle.position
        val collisionNormal: Vector2 = (circle.position - closestPoint).normalized()

        // Calculate overlap distance
        val overlap: Double = circle.radius - offset.magnitude()

        // update the position based on the overlap
        // *1.01 because the balls sometimes got stuck to the box and this fixes it
        circle.position += 1.01 * overlap * collisionNormal
    }

    private fun getClosestPoint(box: Box, externalPoint: Vector2): Vector2 {
        val closestX = externalPoint.x.coerceIn(box.minX, box.maxX)
        val closestY = externalPoint.y.coerceIn(box.minY, box.maxY)

        return Vector2(closestX, closestY)
    }
}