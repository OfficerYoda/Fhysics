package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.FhysicsObject

abstract class CollisionSolver {

    abstract fun solveCollision(info: CollisionInfo)

    protected fun separateOverlappingObjects(info: CollisionInfo) {
        val moveAmount: Vector2 = 0.5 * info.overlap * info.normal

        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        // other object needs to move double the distance
        if(objA.static || objB.static)
            moveAmount *= 2.0

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
