package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import lombok.ToString

@ToString
abstract class FhysicsObject protected constructor(
    val position: Vector2 = Vector2.ZERO,
    val mass: Double = 1.0,
    val velocity: Vector2 = Vector2.ZERO,
    private val acceleration: Vector2 = Vector2.ZERO
) {
    val id = FhysicsCore.nextId()


    fun applyGravity(dt: Double, gravity: Vector2) {
        acceleration += gravity
        velocity += acceleration * dt
        position += velocity * dt

        acceleration.set(Vector2.ZERO)
    }
}
