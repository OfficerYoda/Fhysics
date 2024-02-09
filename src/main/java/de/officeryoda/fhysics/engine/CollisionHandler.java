package de.officeryoda.fhysics.engine;

import de.officeryoda.fhysics.objects.Circle;

public class CollisionHandler {

    public static void handleElasticCollision(Circle obj1, Circle obj2) {
        double sqrDst = obj1.getPosition().sqrDistance(obj2.getPosition());
        double sqrRadii = Math.pow(obj1.getRadius() + obj2.getRadius(), 2);

        if(sqrDst < sqrRadii) {
            // Calculate relative velocity before collision; obj2 doesn't move relatively speaking
            Vector2 relativeVelocity = obj2.getVelocity().subtractNew(obj1.getVelocity());

            // Calculate the normal vector along the line of collision
            // a vector from obj1 in direction of obj2, normalized
            Vector2 collisionNormal = obj2.getPosition().subtractNew(obj1.getPosition()).normalize();

            // Calculate relative velocity along the normal direction
            double relativeVelocityAlongNormal = relativeVelocity.dot(collisionNormal);

            // Calculate impulse (change in momentum)
//            double impulse = (2.0 * relativeVelocityAlongNormal) / (obj1.getMass() + obj2.getMass());
            double impulse = (2.0 * relativeVelocityAlongNormal) / (1 + 1);

            // Apply impulse to update velocities
//            obj1.getVelocity().add(collisionNormal.multiplyNew(impulse * obj2.getMass() * restitution));
//            obj2.getVelocity().subtract(collisionNormal.multiplyNew(impulse * obj1.getMass() * restitution));
            obj1.getVelocity().add(collisionNormal.multiplyNew(impulse));
            obj2.getVelocity().subtract(collisionNormal.multiplyNew(impulse));

        }
    }
}
