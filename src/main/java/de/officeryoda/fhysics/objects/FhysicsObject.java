package de.officeryoda.fhysics.objects;

import de.officeryoda.fhysics.engine.FhysicsCore;
import de.officeryoda.fhysics.engine.Vector2;
import lombok.Data;

@Data
public abstract class FhysicsObject {

    private final int id;

    private Vector2 position;
    private Vector2 velocity;
    private Vector2 acceleration;

    private double mass;

    protected FhysicsObject(Vector2 position, double mass) {
        this.id = FhysicsCore.nextId();
        this.position = position;
        this.velocity = Vector2.zero();
        this.acceleration = Vector2.zero();
        this.mass = mass;
    }

    protected FhysicsObject() {
        this(Vector2.zero(), 1);
    }

    public void applyGravity(double dt, Vector2 gravity) {
        acceleration.add(gravity);
        velocity.add(acceleration.multiplyNew(dt));
        position.add(velocity.multiplyNew(dt));

        acceleration.set(Vector2.zero());
    }
}
