package de.officeryoda.fhysics.engine.collisionhandler

import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle

abstract class CollisionHandler {
    abstract fun handleCollision(circle1: Circle, circle2: Circle)
    abstract fun handleCollision(circle: Circle, box: Box)
    abstract fun handleCollision(box1: Box, box2:Box)
}
