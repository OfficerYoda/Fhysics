package de.officeryoda.fhysics.rendering;

import de.officeryoda.fhysics.Main;
import de.officeryoda.fhysics.engine.Vector2;
import de.officeryoda.fhysics.objects.Box;
import de.officeryoda.fhysics.objects.Circle;
import de.officeryoda.fhysics.objects.FhysicsObject;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FhysicsObjectDrawer extends JFrame {

    private final List<FhysicsObject> physicsObjects;

    public FhysicsObjectDrawer() {
        this.physicsObjects = Main.objects;
        initUI();
    }

    private void initUI() {
        setTitle("Fhysics Object Drawer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        drawAllObjects(g);
    }

    private void drawAllObjects(Graphics g) {
        for(FhysicsObject object : physicsObjects) {
            drawObject(object, g);
        }
    }

    private void drawObject(FhysicsObject object, Graphics g) {
        if(object instanceof Box) {
            drawBox((Box) object, g);
        } else if(object instanceof Circle) {
            drawCircle((Circle) object, g);
        } // Add more cases for other FhysicsObject types if needed
    }

    private void drawBox(Box box, Graphics g) {
        Vector2 pos = transformPosition(box.getPosition());
        g.fillRect((int) pos.getX(), (int) pos.getY(),
                (int) box.getWidth(), (int) box.getHeight());
    }

    private void drawCircle(Circle circle, Graphics g) {
        Vector2 pos = transformPosition(circle.getPosition());
        double radius = circle.getRadius();
        double diameter = 2 * radius;
        g.fillOval((int) (pos.getX() - radius), (int) (pos.getY() - radius),
                (int) diameter, (int) diameter);
    }

    /**
     * Transforms the position to make objects with y-pos 0
     * appear at the bottom of the window instead of at the top.
     * Takes into account the window's height, top insets, and left insets.
     *
     * @param pos the original position
     * @return the transformed position
     */
    private Vector2 transformPosition(Vector2 pos) {
        Insets insets = getInsets();
        double newX = pos.getX() + insets.left;
        double newY = getHeight() - insets.bottom - pos.getY();
        return new Vector2(newX, newY);
    }
}
