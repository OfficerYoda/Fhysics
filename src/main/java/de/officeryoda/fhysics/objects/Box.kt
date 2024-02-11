package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import lombok.EqualsAndHashCode
import lombok.ToString

@EqualsAndHashCode(callSuper = true)
@ToString
class Box(position: Vector2, val width: Double, val height: Double) :
    FhysicsObject(position, width * height)
