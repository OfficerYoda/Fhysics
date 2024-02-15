package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.FhysicsObject

abstract class CollisionSolver {

    abstract fun solveCollision(info: CollisionInfo)

    protected fun separateOverlappingObjects(info: CollisionInfo) {
        val moveAmount: Vector2 = info.overlap * 0.5 * info.normal

        // Move circles apart along the collision normal
        moveIfNotStatic(info.objA!!, -moveAmount)
        moveIfNotStatic(info.objB!!, moveAmount)
    }

    private fun moveIfNotStatic(obj: FhysicsObject, moveAmount: Vector2) {
        if (!obj.static) {
            obj.position += moveAmount
        }
    }
}
