package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2

abstract class CollisionSolver {

    abstract fun solveCollision(points: CollisionInfo)

    protected fun separateOverlappingObjects(points: CollisionInfo) {
        val moveAmount: Vector2 = points.normal * (points.overlap * 0.5)

        // Move circles apart along the collision normal
        points.objA!!.position -= moveAmount
        points.objB!!.position += moveAmount
    }
}
