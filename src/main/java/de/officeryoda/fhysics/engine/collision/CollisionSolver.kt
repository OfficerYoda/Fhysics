package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.engine.objects.FhysicsObject

abstract class CollisionSolver {

    abstract fun solveCollision(info: CollisionInfo)

    protected fun separateOverlappingObjects(info: CollisionInfo) {
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        if (objA.static && objB.static) return

        val totalMass: Float =
            when {
                !objA.static && !objB.static -> objA.mass + objB.mass
                objA.static -> objB.mass
                else -> objA.mass
            }

        val overlap: Vector2 = info.depth * info.normal

        // if both objects are non-static, separate them by their mass ratio else move the non-static object by the overlap
        if (!objA.static) objA.position -= if (!objB.static) (objB.mass / totalMass) * overlap else overlap
        if (!objB.static) objB.position += if (!objA.static) (objA.mass / totalMass) * overlap else overlap
    }
}