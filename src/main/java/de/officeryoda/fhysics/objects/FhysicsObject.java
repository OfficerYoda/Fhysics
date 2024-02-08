package de.officeryoda.fhysics.objects;

import de.officeryoda.fhysics.engine.Vector2;
import lombok.Data;

@Data
public abstract class FhysicsObject {

    private Vector2 position;

    protected FhysicsObject(Vector2 position) {
        this.position = position;
    }

    protected FhysicsObject() {
        this(Vector2.ZERO);
    }
}
