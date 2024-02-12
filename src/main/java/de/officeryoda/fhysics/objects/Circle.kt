package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2

class Circle(position: Vector2, val radius: Double) :
    FhysicsObject(position, Math.PI * radius * radius) {

    override fun toString(): String {
        return "Circle(position=$position, radius=$radius) - ${super.toString()}"
    }
}
