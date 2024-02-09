package de.officeryoda.fhysics.objects;

import de.officeryoda.fhysics.engine.Border;
import de.officeryoda.fhysics.engine.FhysicsCore;
import de.officeryoda.fhysics.engine.Vector2;

import java.util.Random;

public class FhysicsObjectFactory {

    private static final Random RANDOM = new Random();

    public static Circle randomCircle() {
        double radius = RANDOM.nextDouble(5, 35);
        Vector2 pos = randomPosInsideBounds(radius);
        Circle circle = new Circle(pos, radius);

        circle.setVelocity(randomVector2(-200, 200));

        return circle;
    }

    private static Vector2 randomPosInsideBounds(double buffer) {
        Border border = FhysicsCore.BORDER;
        double minX = border.leftBorder() + buffer;
        double maxX = border.rightBorder() - minX - buffer;
        double x = RANDOM.nextDouble(minX, maxX);

        double minY = border.bottomBorder() + buffer;
        double maxY = border.topBorder() - minY - buffer;
        double y = RANDOM.nextDouble(minY, maxY);

        return new Vector2(x, y);
    }

    private static Vector2 randomVector2(double min, double max) {
        double x = RANDOM.nextDouble(min, max);
        double y = RANDOM.nextDouble(min, max);

        return new Vector2(x, y);
    }
}
