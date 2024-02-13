package de.officeryoda.fhysics.engine.collisionhandler

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import kotlin.math.pow
import kotlin.math.sqrt

object MinimizeOverlap : CollisionHandler() {

    override fun handleCollision(circle1: Circle, circle2: Circle) {
        val sqrDst: Double = circle1.position.sqrDistance(circle2.position)
        val sqrRadii: Double = (circle1.radius + circle2.radius).pow(2.0)

        if (sqrDst < sqrRadii) {
            // Calculate overlap distance
            val overlap: Double = sqrt(sqrRadii) - sqrt(sqrDst)

            val collisionNormal: Vector2 = (circle2.position - circle1.position).normalized()
            val moveAmount: Vector2 = collisionNormal * (overlap * 0.5)

            // Move circles apart along the collision normal and give them appropriate velocity
            circle1.position -= moveAmount
            circle1.velocity -= moveAmount
            circle2.position += moveAmount
            circle2.velocity += moveAmount
        }
    }

    override fun handleCollision(circle: Circle, box: Box) {
        TODO("Not yet implemented")
    }

    override fun handleCollision(box1: Box, box2: Box) {
        TODO("Not yet implemented")
    }
}
