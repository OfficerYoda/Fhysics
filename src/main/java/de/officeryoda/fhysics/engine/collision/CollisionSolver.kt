package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.FhysicsObject

abstract class CollisionSolver {

    abstract fun solveCollision(info: CollisionInfo)

    protected fun separateOverlappingObjects(info: CollisionInfo) {
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        // non-static object needs to move the full distance
        // instead of both objects moving half the distance
        val moveAmount: Vector2 =
            if (objA.static || objB.static)
                info.overlap * info.normal
            else
                0.5 * info.overlap * info.normal

        // Move circles apart along the collision normal
        moveIfNotStatic(objA, -moveAmount)
        moveIfNotStatic(objB, moveAmount)
    }

    private fun moveIfNotStatic(obj: FhysicsObject, moveAmount: Vector2) {
        if (!obj.static) {
            obj.position += moveAmount
        }
    }
}
