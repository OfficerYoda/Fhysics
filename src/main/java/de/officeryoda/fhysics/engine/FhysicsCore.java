package de.officeryoda.fhysics.engine;

import de.officeryoda.fhysics.objects.Circle;
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FhysicsCore {

    public static final Border BORDER = new Border(0, 600, 0, 400);
    private static int objectCount;
    @Getter
    private final List<Circle> fhysicsObjects;
    private final Vector2 gravity = new Vector2(0, -9.81);
    private final int updatesPerSecond = 500;
    private final double updatesIntervalSeconds = 1.0 / updatesPerSecond;
    @Setter
    private FhysicsObjectDrawer drawer;

    public FhysicsCore() {
        this.fhysicsObjects = new ArrayList<>();

        fhysicsObjects.add(new Circle(new Vector2(50, 200), 20));
        fhysicsObjects.add(new Circle(new Vector2(400, 190), 20));

        fhysicsObjects.add(new Circle(new Vector2(50, 305), 5));
        fhysicsObjects.add(new Circle(new Vector2(250, 150), 12));
        fhysicsObjects.add(new Circle(new Vector2(100, 200), 15));
        fhysicsObjects.add(new Circle(new Vector2(500, 360), 10));
//        fhysicsObjects.add(new Box(new Vector2(300, 400), 50, 100));
//        fhysicsObjects.add(new Box(new Vector2(300, 31), 50, 100));

        fhysicsObjects.getFirst().setVelocity(new Vector2(200, 0));
    }

    public static int nextId() {
        return objectCount++;
    }

    public void startUpdateLoop() {
        Timer updateTimer = new Timer(true);
        // time in ms

        int updateIntervalMillis = (int) (1f / updatesPerSecond * 1000);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                update();
            }
        }, 0, updateIntervalMillis);
    }

    private void update() {
        fhysicsObjects.forEach(obj -> {
//            obj.applyGravity(updatesIntervalSeconds, gravity);
            obj.applyGravity(updatesIntervalSeconds, Vector2.zero());
            checkBorderCollision(obj);
            checkObjectCollision(obj);
        });
        drawer.repaintObjects();
    }

    private void checkObjectCollision(Circle obj1) {
        fhysicsObjects.forEach(obj2 -> {
            if(obj1.getId() == obj2.getId()) return;
            CollisionHandler.handleElasticCollision(obj1, obj2);
        });
    }

    private void checkBorderCollision(Circle circle) {
        Vector2 pos = circle.getPosition();
        Vector2 velocity = circle.getVelocity();
        double radius = circle.getRadius();

        // check top/bottom border
        if(pos.getY() + radius > BORDER.topBorder()) {
            velocity.setY(-velocity.getY());
//            velocity.set(Vector2.zero());
            pos.setY(BORDER.topBorder() - radius);
//            velocity.setX(Math.random() * 400 - 200);
        } else if(pos.getY() - radius < BORDER.bottomBorder()) {
            velocity.setY(-velocity.getY());
            pos.setY(BORDER.bottomBorder() + radius);
//            velocity.setX(Math.random() * 400 - 200);
        }

        // check left/right border
        if(pos.getX() - radius < BORDER.leftBorder()) {
            velocity.setX(-velocity.getX());
            pos.setX(BORDER.leftBorder() + radius);
        } else if(pos.getX() + radius > BORDER.rightBorder()) {
            velocity.setX(-velocity.getX());
            pos.setX(BORDER.rightBorder() - radius);
        }
    }
}
