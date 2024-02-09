package de.officeryoda.fhysics.rendering;

import de.officeryoda.fhysics.engine.Fhysics;
import de.officeryoda.fhysics.engine.Vector2;
import de.officeryoda.fhysics.objects.Box;
import de.officeryoda.fhysics.objects.Circle;
import de.officeryoda.fhysics.objects.FhysicsObject;

import javax.swing.*;
import java.awt.*;

public class FhysicsObjectDrawer extends JFrame {

    private final FhysicsPanel fhysicsPanel;

    public FhysicsObjectDrawer(Fhysics fhysics) {
        setTitle("Falling Sand");
        setSize(1440, 720);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        fhysicsPanel = new FhysicsPanel(fhysics);
        add(fhysicsPanel);

        addMouseWheelListener(new MouseWheelListener(fhysicsPanel));

        setLocationRelativeTo(null); // center the frame on the screen
        setVisible(true);
    }

    public void repaintObjects() {
        fhysicsPanel.repaint();
    }
}

class FhysicsPanel extends JPanel {

    private final Color objectColor = Color.decode("#2f2f30");
    private final Fhysics fhysics;

    private double zoom;

    public FhysicsPanel(Fhysics fhysics) {
        this.fhysics = fhysics;
        Color backgroundColor = Color.decode("#010409");
        this.zoom = 3;
        setBackground(backgroundColor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawAllObjects(g);
    }

    private void drawAllObjects(Graphics g) {
        g.setColor(objectColor);
        for(FhysicsObject object : fhysics.getFhysicsObjects()) {
            drawObject(object, g);
        }
    }

    private void drawObject(FhysicsObject object, Graphics g) {
        if(object instanceof de.officeryoda.fhysics.objects.Box) {
            drawBox((de.officeryoda.fhysics.objects.Box) object, g);
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
        double radius = circle.getRadius() * zoom;
        int diameter = (int) (2 * radius);
        g.fillOval((int) (pos.getX() - radius), (int) (pos.getY() - radius),
                diameter, diameter);
    }

    /**
     * Transforms the position to make objects with y-pos 0
     * appear at the bottom of the window instead of at the top.
     * Takes into account the window's height, top insets, left insets, and zoom factor.
     *
     * @param pos the original position
     * @return the transformed position
     */
    private Vector2 transformPosition(Vector2 pos) {
        Insets insets = getInsets();
        double newX = pos.getX() * zoom + insets.left;
        double newY = getHeight() - insets.bottom - (pos.getY() * zoom);
        return new Vector2(newX, newY);
    }


    public void onMouseWheel(int dir) {
        zoom += dir * -0.1;
    }
}