package de.officeryoda.fhysics.engine.collisionhandler

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import kotlin.math.pow
import kotlin.math.sqrt

object MinimizeOverlap : CollisionHandler() {

    override fun handleCollision(obj1: Circle, obj2: Circle) {
        val sqrDst: Double = obj1.position.sqrDistance(obj2.position)
        val sqrRadii: Double = (obj1.radius + obj2.radius).pow(2.0)

        if (sqrDst < sqrRadii) {
            // Calculate overlap distance
            val overlap: Double = sqrt(sqrRadii) - sqrt(sqrDst)

            val collisionNormal: Vector2 = (obj2.position - obj1.position).normalized()
            val moveAmount: Vector2 = collisionNormal * (overlap * 0.5)

            // Move circles apart along the collision normal and give them appropriate velocity
            obj1.position -= moveAmount
            obj1.velocity -= moveAmount
            obj2.position += moveAmount
            obj2.velocity += moveAmount
        }
    }

    override fun handleCollision(obj1: Circle, obj2: Box) {
        TODO("Not yet implemented")
    }

    override fun handleCollision(obj1: Box, obj2: Box) {
        TODO("Not yet implemented")
    }
}
