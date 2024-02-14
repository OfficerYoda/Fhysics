package de.officeryoda.fhysics.engine.collisionhandler

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import kotlin.math.pow
import kotlin.math.sqrt

object ElasticCollision : CollisionHandler() {

    override fun handleCollision(circle1: Circle, circle2: Circle) {
        val sqrDst: Double = circle1.position.sqrDistance(circle2.position)
        val sqrRadii: Double = (circle1.radius + circle2.radius).pow(2.0)

        if (sqrDst < sqrRadii) {
            separateOverlappingCircles(circle1, circle2, circle1.radius + circle2.radius, sqrDst)

            // Calculate relative velocity before collision; obj2 doesn't move relatively speaking
            val relativeVelocity: Vector2 = circle2.velocity - circle1.velocity

            // Calculate the normal vector along the line of collision
            // a vector from obj1 in direction of obj2, normalized
            val collisionNormal: Vector2 = (circle2.position - circle1.position).normalized()

            // Calculate relative velocity along the normal direction
            val relativeVelocityAlongNormal: Double = relativeVelocity.dot(collisionNormal)

            // Calculate impulse (change in momentum)
            val impulse: Double = (2.0 * relativeVelocityAlongNormal) / (circle1.mass + circle2.mass)

            // Apply impulse to update velocities
            val restitution = 1.0
            val impulseMultiplier = impulse * restitution * collisionNormal
            circle1.velocity += impulseMultiplier * circle2.mass
            circle2.velocity -= impulseMultiplier * circle1.mass
        }
    }

    override fun handleCollision(circle: Circle, box: Box) {
        val closestPoint: Vector2 = getClosestPoint(box, circle.position)

        // Calculate the vector from the circle's center to the closest point on the box
        val offset: Vector2 = closestPoint - circle.position

        // Check if the distance between the circle's center and the closest point is less than the circle's radius
        if (offset.sqrMagnitude() < circle.radius * circle.radius) {
            separateCircleFromBox(circle, closestPoint, offset)

            val temp = circle.velocity.copy()
            circle.velocity -= 2 * temp
        }
    }

    private fun separateCircleFromBox(circle: Circle, closestPoint: Vector2, offset: Vector2) {
        // from closestPoint to circle.position
        val collisionNormal: Vector2 = (circle.position - closestPoint).normalized()

        // Calculate overlap distance
        val overlap: Double = circle.radius - offset.magnitude()

        circle.position += overlap * collisionNormal
    }

    override fun handleCollision(box1: Box, box2: Box) {
        TODO("Not yet implemented")
    }

    private fun separateOverlappingCircles(obj1: Circle, obj2: Circle, radii: Double, sqrDst: Double) {
        // Calculate overlap distance
        val overlap: Double = radii - sqrt(sqrDst)

        val collisionNormal: Vector2 = (obj2.position - obj1.position).normalized()
        val moveAmount: Vector2 = collisionNormal * (overlap * 0.5)

        // Move circles apart along the collision normal
        obj1.position -= moveAmount
        obj2.position += moveAmount
    }

    private fun getClosestPoint(box: Box, externalPoint: Vector2): Vector2 {
        val closestX = externalPoint.x.coerceIn(box.minX, box.maxX)
        val closestY = externalPoint.y.coerceIn(box.minY, box.maxY)

        return Vector2(closestX, closestY)
    }
}
