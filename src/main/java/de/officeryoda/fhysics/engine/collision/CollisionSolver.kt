package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.FhysicsObject

abstract class CollisionSolver {

    abstract fun solveCollision(info: CollisionInfo)

    protected fun separateOverlappingObjects(info: CollisionInfo) {
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        // Early return if both objects are static
        if (objA.static && objB.static) {
            return
        }

        // Calculate total mass if at least one object is not static
        val totalMass: Float = when {
            !objA.static && !objB.static -> objA.mass + objB.mass
            objA.static -> objB.mass
            else -> objA.mass
        }

        // Calculate the overlap
        val overlap: Vector2 = info.overlap * info.normal

        // Calculate and apply movement amounts based on mass ratio
        if (!objA.static) {
            val moveAmountA: Vector2 =
                if (!objB.static) (objB.mass / totalMass) * overlap
                else overlap
            objA.position -= moveAmountA
        }
        if (!objB.static) {
            val moveAmountB: Vector2 =
                if (!objA.static) (objA.mass / totalMass) * overlap
                else overlap
            objB.position += moveAmountB
        }
    }
}
