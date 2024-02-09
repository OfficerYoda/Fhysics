package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.Vector2.Companion.zero
import lombok.Data

@Data
abstract class FhysicsObject protected constructor(
    val position: Vector2 = zero(),
    val mass: Double = 1.0
) {
    val id = FhysicsCore.nextId()

    var velocity = zero()
    private val acceleration = zero()

    fun applyGravity(dt: Double, gravity: Vector2?) {
        acceleration.add(gravity!!)
        velocity.add(acceleration.multiplyNew(dt))
        position.add(velocity.multiplyNew(dt))

        acceleration.set(zero())
    }
}
