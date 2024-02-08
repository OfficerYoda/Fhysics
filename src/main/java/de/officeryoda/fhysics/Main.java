package de.officeryoda.fhysics;

import de.officeryoda.fhysics.engine.Vector2;
import de.officeryoda.fhysics.objects.Box;
import de.officeryoda.fhysics.objects.Circle;
import de.officeryoda.fhysics.objects.FhysicsObject;
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static List<FhysicsObject> objects = new ArrayList<>();

    public static void main(String[] args) {
        objects.add(new Circle(new Vector2(50,50), 50));
        objects.add(new Circle(new Vector2(50, 50), 25));
        objects.add(new Circle(new Vector2(100, 20), 15));
        objects.add(new Box(new Vector2(300, 400), 50, 100));
        objects.add(new Box(new Vector2(300, 31), 50, 100));

        SwingUtilities.invokeLater(FhysicsObjectDrawer::new);
    }
}