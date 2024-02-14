package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.FhysicsObject

object ElasticCollision : CollisionSolver() {

    override fun solveCollision(objA: FhysicsObject, objB: FhysicsObject, points: CollisionPoints) {
        separateOverlappingObjects(objA, objB, points.depth, points.normal)

        // Calculate relative velocity before collision; circleB doesn't move relatively speaking
        val relativeVelocity: Vector2 = objB.velocity - objA.velocity

        // Calculate the normal vector along the line of collision
        // a vector from circleA in direction of circleB, normalized
        val collisionNormal: Vector2 = (objB.position - objA.position).normalized()

        // Calculate relative velocity along the normal direction
        val relativeVelocityAlongNormal: Double = relativeVelocity.dot(collisionNormal)

        // Calculate impulse (change in momentum)
        val impulse: Double = (2.0 * relativeVelocityAlongNormal) / (objA.mass + objB.mass)

        // Apply impulse to update velocities
        val restitution = 1.0
        val impulseMultiplier = impulse * restitution * collisionNormal
        objA.velocity += impulseMultiplier * objB.mass
        objB.velocity -= impulseMultiplier * objA.mass
    }

    private fun separateOverlappingObjects(objA: FhysicsObject, objB: FhysicsObject, overlap: Double, normal: Vector2) {
        val moveAmount: Vector2 = normal * (overlap * 0.5)

        // Move circles apart along the collision normal
        objA.position -= moveAmount
        objB.position += moveAmount
    }
}
