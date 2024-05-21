package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.FhysicsObject

object ElasticCollision : CollisionSolver() {

    override fun solveCollision(info: CollisionInfo) {
        // separate the objects to prevent tunneling and other anomalies
        separateOverlappingObjects(info)

        // Get the objects
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        // Calculate relative velocity before collision; circleB doesn't move relatively speaking
        val relativeVelocity: Vector2 = objB.velocity - objA.velocity

        // Calculate relative velocity along the collision normal direction
        val relativeVelocityAlongNormal: Float = relativeVelocity.dot(info.normal)

        // Calculate impulse (change in momentum)
        val impulse: Float = (2.0F * relativeVelocityAlongNormal) / (objA.mass + objB.mass)

        // Apply impulse to update velocities
        val restitution = 1.0F
        val impulseMultiplier: Vector2 = impulse * restitution * info.normal

        // Apply impulse to the objects
        if (!objA.static)
            objA.velocity += impulseMultiplier * objB.mass
        if (!objB.static)
            objB.velocity -= impulseMultiplier * objA.mass
    }
}
