package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.objects.Circle
import kotlin.math.pow

object MinimizeOverlap : CollisionHandler() {
    override fun handleCollision(obj1: Circle, obj2: Circle) {
        val sqrDst: Double = obj1.position.sqrDistance(obj2.position)
        val sqrRadii: Double = (obj1.radius + obj2.radius).pow(2.0)

        if (sqrDst < sqrRadii) {
            separateOverlappingCircles(obj1, obj2, sqrRadii, sqrDst)
        }
    }
}
