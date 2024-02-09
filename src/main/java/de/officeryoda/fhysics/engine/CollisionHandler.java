package de.officeryoda.fhysics.engine;

import de.officeryoda.fhysics.objects.Circle;

public class CollisionHandler {

    public static void handleCollision(Circle circle1, Circle circle2) {
        double sqrDst = circle1.getPosition().sqrDistance(circle2.getPosition());
        double sqrRadii = Math.pow(circle1.getRadius() + circle2.getRadius(), 2);

        if (sqrDst < sqrRadii) {
            // Handle the collision logic here
            // For example, you can update the positions or velocities of the circles

            // Sample code to swap velocities (assuming elastic collision)
            Vector2 tempVelocity = circle1.getVelocity();
            circle1.setVelocity(circle2.getVelocity());
            circle2.setVelocity(tempVelocity);
        }
    }
}
