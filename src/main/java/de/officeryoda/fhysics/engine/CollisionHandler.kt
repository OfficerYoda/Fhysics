package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.objects.Circle
import kotlin.math.pow
import kotlin.math.sqrt

object CollisionHandler {
    fun handleElasticCollision(obj1: Circle, obj2: Circle) {
        val sqrDst = obj1.position.sqrDistance(obj2.position)
        val sqrRadii: Double = (obj1.radius + obj2.radius).pow(2.0)

        if (sqrDst < sqrRadii) {
            separateOverlappingCircles(obj1, obj2, sqrRadii, sqrDst)

            // Calculate relative velocity before collision; obj2 doesn't move relatively speaking
            val relativeVelocity = obj2.velocity.subtractNew(obj1.velocity)

            // Calculate the normal vector along the line of collision
            // a vector from obj1 in direction of obj2, normalized
            val collisionNormal = obj2.position.subtractNew(obj1.position).normalize()

            // Calculate relative velocity along the normal direction
            val relativeVelocityAlongNormal = relativeVelocity.dot(collisionNormal)

            // Calculate impulse (change in momentum)
            val impulse = (2.0 * relativeVelocityAlongNormal) / (obj1.mass + obj2.mass)

            // Apply impulse to update velocities
            val restitution = 1.0
            obj1.velocity.add(collisionNormal.multiplyNew(impulse * obj2.mass * restitution))
            obj2.velocity.subtract(collisionNormal.multiplyNew(impulse * obj1.mass * restitution))
        }
    }

    private fun separateOverlappingCircles(obj1: Circle, obj2: Circle, sqrRadii: Double, sqrDst: Double) {
        // Calculate overlap distance
        val overlap = sqrt(sqrRadii) - sqrt(sqrDst)

        val collisionNormal = obj2.position.subtractNew(obj1.position).normalize()
        val moveAmount = collisionNormal.multiplyNew(overlap * 0.5)

        // Move circles apart along the collision normal
        obj1.position.subtract(moveAmount)
        obj2.position.add(moveAmount)
    }
}
