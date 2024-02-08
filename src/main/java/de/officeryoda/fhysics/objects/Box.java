package de.officeryoda.fhysics.objects;

import de.officeryoda.fhysics.engine.Vector2;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class Box extends FhysicsObject{

    private final double width;
    private final double height;

    public Box(Vector2 position, double width, double height) {
        super(position);
        this.width = width;
        this.height = height;
    }
}
