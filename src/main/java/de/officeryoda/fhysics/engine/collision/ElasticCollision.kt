package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.FhysicsObject

object ElasticCollision : CollisionSolver() {

    override fun solveCollision(points: CollisionInfo) {
        // separate the objects to prevent tunneling and other anomalies
        separateOverlappingObjects(points)

        val objA: FhysicsObject = points.objA!!
        val objB: FhysicsObject = points.objB!!

        // Calculate relative velocity before collision; circleB doesn't move relatively speaking
        val relativeVelocity: Vector2 = objB.velocity - objA.velocity

        // Calculate relative velocity along the normal direction
        val relativeVelocityAlongNormal: Double = relativeVelocity.dot(points.normal)

        // Calculate impulse (change in momentum)
        val impulse: Double = (2.0 * relativeVelocityAlongNormal) / (objA.mass + objB.mass)

        // Apply impulse to update velocities
        val restitution = 1.0
        val impulseMultiplier = impulse * restitution * points.normal
        objA.velocity += impulseMultiplier * objB.mass
        objB.velocity -= impulseMultiplier * objA.mass
    }
}
