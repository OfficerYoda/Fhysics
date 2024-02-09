package de.officeryoda.fhysics.engine;

import de.officeryoda.fhysics.objects.Circle;
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Fhysics {

    @Getter
    private final List<Circle> fhysicsObjects;
    private final Vector2 gravity = new Vector2(0, -9.81);
    private final int updatesPerSecond = 500;
    private final double updatesIntervalSeconds = 1.0 / updatesPerSecond;
    boolean collision = false;
    @Setter
    private FhysicsObjectDrawer drawer;

    public Fhysics() {
        this.fhysicsObjects = new ArrayList<>();

        fhysicsObjects.add(new Circle(new Vector2(50, 500), 50));
        fhysicsObjects.add(new Circle(new Vector2(50, 500), 25));
        fhysicsObjects.add(new Circle(new Vector2(100, 200), 15));
        fhysicsObjects.add(new Circle(new Vector2(720, 360), 10));
//        fhysicsObjects.add(new Box(new Vector2(300, 400), 50, 100));
//        fhysicsObjects.add(new Box(new Vector2(300, 31), 50, 100));
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
            if(collision) {
                obj.applyGravity(updatesIntervalSeconds, gravity);
            } else {
                obj.applyGravity(updatesIntervalSeconds, gravity);
            }
            checkBottomCollision(obj);
        });
        drawer.repaintObjects();
    }

    private void checkBottomCollision(Circle circle) {
        if(circle.getPosition().getY() - circle.getRadius() <= 0) {
            circle.getVelocity().multiply(-1);
            collision = true;
        }
    }
}
