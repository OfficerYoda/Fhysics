package de.officeryoda.fhysics.engine.collisionhandler

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import kotlin.math.pow
import kotlin.math.sqrt

object ElasticCollision : CollisionHandler() {

    override fun handleCollision(obj1: Circle, obj2: Circle) {
        val sqrDst: Double = obj1.position.sqrDistance(obj2.position)
        val sqrRadii: Double = (obj1.radius + obj2.radius).pow(2.0)

        if (sqrDst < sqrRadii) {
            separateOverlappingCircles(obj1, obj2, sqrRadii, sqrDst)

            // Calculate relative velocity before collision; obj2 doesn't move relatively speaking
            val relativeVelocity: Vector2 = obj2.velocity - obj1.velocity

            // Calculate the normal vector along the line of collision
            // a vector from obj1 in direction of obj2, normalized
            val collisionNormal: Vector2 = (obj2.position - obj1.position).normalized()

            // Calculate relative velocity along the normal direction
            val relativeVelocityAlongNormal: Double = relativeVelocity.dot(collisionNormal)

            // Calculate impulse (change in momentum)
            val impulse: Double = (2.0 * relativeVelocityAlongNormal) / (obj1.mass + obj2.mass)

            // Apply impulse to update velocities
            val restitution = 1.0
            obj1.velocity += collisionNormal * (impulse * obj2.mass * restitution)
            obj2.velocity -= collisionNormal * (impulse * obj1.mass * restitution)
        }
    }

    override fun handleCollision(obj1: Circle, obj2: Box) {

    }

    override fun handleCollision(obj1: Box, obj2: Box) {
        TODO("Not yet implemented")
    }

    private fun separateOverlappingCircles(obj1: Circle, obj2: Circle, sqrRadii: Double, sqrDst: Double) {
        // Calculate overlap distance
        val overlap: Double = sqrt(sqrRadii) - sqrt(sqrDst)

        val collisionNormal: Vector2 = (obj2.position - obj1.position).normalized()
        val moveAmount: Vector2 = collisionNormal * (overlap * 0.5)

        // Move circles apart along the collision normal
        obj1.position -= moveAmount
        obj2.position += moveAmount
    }
}
