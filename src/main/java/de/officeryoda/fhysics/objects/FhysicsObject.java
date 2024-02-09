package de.officeryoda.fhysics.objects;

import de.officeryoda.fhysics.engine.Vector2;
import lombok.Data;

@Data
public abstract class FhysicsObject {

    private Vector2 position;
    private Vector2 velocity;
    private Vector2 acceleration;

    protected FhysicsObject(Vector2 position) {
        this.position = position;
        this.velocity = Vector2.zero();
        this.acceleration = Vector2.zero();
    }

    protected FhysicsObject() {
        this(Vector2.zero());
    }

    public void applyGravity(double dt, Vector2 gravity) {
        acceleration.add(gravity);
        velocity.add(acceleration.multiplyNew(dt));
        position.add(velocity.multiplyNew(dt));

        acceleration.set(Vector2.zero());
    }
}
