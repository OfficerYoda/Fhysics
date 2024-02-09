package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.ToString

@EqualsAndHashCode(callSuper = true)
@ToString
class Circle(position: Vector2, val radius: Double) : FhysicsObject(position, Math.PI * radius * radius)
