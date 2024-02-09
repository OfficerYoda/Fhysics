package de.officeryoda.fhysics.rendering;

import de.officeryoda.fhysics.engine.Border;
import de.officeryoda.fhysics.engine.FhysicsCore;
import de.officeryoda.fhysics.engine.Vector2;
import de.officeryoda.fhysics.engine.Vector2Int;
import de.officeryoda.fhysics.objects.Box;
import de.officeryoda.fhysics.objects.Circle;
import de.officeryoda.fhysics.objects.FhysicsObject;

import javax.swing.*;
import java.awt.*;

public class FhysicsObjectDrawer extends JFrame {

    private final FhysicsPanel fhysicsPanel;

    public FhysicsObjectDrawer(FhysicsCore fhysics) {
        setTitle("Fhysics");

        setWindowSize();
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        double zoom = calculateZoom();
        fhysicsPanel = new FhysicsPanel(fhysics, zoom);
        add(fhysicsPanel);

        addMouseWheelListener(new MouseWheelListener(fhysicsPanel));

        setLocationRelativeTo(null); // center the frame on the screen
        setVisible(true);
    }

    private void setWindowSize() {
        Insets insets = new Insets(31, 8, 8, 8); // these will be the values
        Border border = FhysicsCore.BORDER;
        double borderWidth = border.rightBorder() - border.leftBorder();
        double borderHeight = border.topBorder() - border.bottomBorder();

        double ratio = borderHeight / borderWidth;
        final int widowWidth = 1440;
        setSize(widowWidth, (int) (widowWidth * ratio + insets.top));
    }

    private double calculateZoom() {
        Border border = FhysicsCore.BORDER;
        double borderWidth = border.rightBorder() - border.leftBorder();
        int windowWidth = getWidth() - (8 + 8); // -(insets.left[8] + insets.right[8])
        return windowWidth / borderWidth;
    }

    public void repaintObjects() {
        fhysicsPanel.repaint();
    }
}

class FhysicsPanel extends JPanel {

    private final Color objectColor = Color.decode("#2f2f30");
    private final FhysicsCore fhysics;

    private double zoom;

    public FhysicsPanel(FhysicsCore fhysics, double zoom) {
        this.fhysics = fhysics;
        Color backgroundColor = Color.decode("#010409");
        this.zoom = zoom;
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
        drawBorder(g);
    }

    private void drawBorder(Graphics g) {
        Border border = FhysicsCore.BORDER;
        Insets insets = getInsets();

        int x = transformX(border.leftBorder(), insets);
        int y = transformY(border.topBorder(), insets);
        int width = (int) ((border.rightBorder() - border.leftBorder()) * zoom);
        int height = (int) ((border.topBorder() - border.bottomBorder()) * zoom);

        g.setColor(Color.white);
        g.drawRect(x, y, width, height);
    }

    private void drawObject(FhysicsObject object, Graphics g) {
        if(object instanceof de.officeryoda.fhysics.objects.Box) {
            drawBox((de.officeryoda.fhysics.objects.Box) object, g);
        } else if(object instanceof Circle) {
            drawCircle((Circle) object, g);
        }
    }

    private void drawBox(Box box, Graphics g) {
        Vector2Int pos = transformPosition(box.getPosition());
        g.fillRect(pos.getX(), pos.getY(),
                (int) box.getWidth(), (int) box.getHeight());
    }

    private void drawCircle(Circle circle, Graphics g) {
        Vector2Int pos = transformPosition(circle.getPosition());
        int radius = (int) (circle.getRadius() * zoom);
        int diameter = 2 * radius;
        g.fillOval(pos.getX() - radius, pos.getY() - radius,
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
    private Vector2Int transformPosition(Vector2 pos) {
        Insets insets = getInsets();
        int newX = transformX(pos.getX(), insets);
        int newY = transformY(pos.getY(), insets);
        return new Vector2Int(newX, newY);
    }

    private int transformX(double x, Insets insets) {
        return (int) (x * zoom + insets.left);
    }

    private int transformY(double y, Insets insets) {
        return (int) (getHeight() - insets.bottom - (y * zoom));
    }

    public void onMouseWheel(int dir) {
        zoom += dir * -0.01;
    }
}