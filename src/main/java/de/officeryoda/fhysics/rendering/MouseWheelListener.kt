package de.officeryoda.fhysics.rendering;

import java.awt.event.MouseWheelEvent;

public class MouseWheelListener implements java.awt.event.MouseWheelListener {

    private final FhysicsPanel fhysicsPanel;

    MouseWheelListener(FhysicsPanel fhysicsPanel) {
        this.fhysicsPanel = fhysicsPanel;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        fhysicsPanel.onMouseWheel(e.getWheelRotation());
    }
}
