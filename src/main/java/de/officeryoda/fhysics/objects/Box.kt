package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import lombok.Data
import lombok.EqualsAndHashCode

@EqualsAndHashCode(callSuper = true)
@Data
class Box(position: Vector2, val width: Double, val height: Double) :
    FhysicsObject(position, width * height)
