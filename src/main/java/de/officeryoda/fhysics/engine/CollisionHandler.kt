package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.objects.Circle
import kotlin.math.sqrt

abstract class CollisionHandler {
    abstract fun handleCollision(obj1: Circle, obj2: Circle)


    protected fun separateOverlappingCircles(obj1: Circle, obj2: Circle, sqrRadii: Double, sqrDst: Double) {
        // Calculate overlap distance
        val overlap: Double = sqrt(sqrRadii) - sqrt(sqrDst)

        val collisionNormal: Vector2 = (obj2.position - obj1.position).normalized()
        val moveAmount: Vector2 = collisionNormal * (overlap * 0.5)

        // Move circles apart along the collision normal
        obj1.position -= moveAmount
        obj2.position += moveAmount
    }
}
