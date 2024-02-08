package de.officeryoda.fhysics.objects;

import de.officeryoda.fhysics.engine.Vector2;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class Circle extends FhysicsObject {

    private final double radius;

    public Circle(Vector2 position, double radius) {
        super(position);
        this.radius = radius;
    }
}
