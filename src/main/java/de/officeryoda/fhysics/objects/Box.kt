package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2

class Box(position: Vector2, val width: Double, val height: Double) :
    FhysicsObject(position, width * height) {

    override fun toString(): String {
        return "Box(position=$position, width=$width, height=$height) - ${super.toString()}"
    }
}
