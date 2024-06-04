package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.engine.objects.FhysicsObject

object ElasticCollision : CollisionSolver() {

    override fun solveCollision(info: CollisionInfo) {
        // separate the objects to prevent tunneling and other anomalies
        separateOverlappingObjects(info)

        // Get the objects
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        // Calculate relative velocity before collision; circleB doesn't move relatively speaking
        val relativeVelocity: Vector2 = objB.velocity - objA.velocity

        // Return if the objects are already moving away from each other
        if (relativeVelocity.dot(info.normal) >= 0) return

        val impulseMagnitude: Float = -2f * relativeVelocity.dot(info.normal) / (objA.invMass + objB.invMass)
        val impulse: Vector2 = impulseMagnitude * info.normal

        objA.velocity -= impulse * objA.invMass
        objB.velocity += impulse * objB.invMass
    }
}
