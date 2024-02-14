package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.objects.FhysicsObject

abstract class CollisionSolver {

    abstract fun solveCollision(objA: FhysicsObject, objB: FhysicsObject, points: CollisionPoints)

}
