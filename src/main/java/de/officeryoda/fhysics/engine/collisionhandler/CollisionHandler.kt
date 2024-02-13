package de.officeryoda.fhysics.engine.collisionhandler

import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle

abstract class CollisionHandler {
    abstract fun handleCollision(obj1: Circle, obj2: Circle)
    abstract fun handleCollision(obj1: Circle, obj2: Box)
    abstract fun handleCollision(obj1: Box, obj2:Box)
}
