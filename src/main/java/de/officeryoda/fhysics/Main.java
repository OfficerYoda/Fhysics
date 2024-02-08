package de.officeryoda.fhysics;

import de.officeryoda.fhysics.engine.Fhysics;
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        Fhysics fhysics = new Fhysics();

        SwingUtilities.invokeLater(() -> {
//            FhysicsObjectDrawer drawer = new FhysicsObjectDrawer(fhysics);
            FhysicsObjectDrawer drawer = new FhysicsObjectDrawer(fhysics);
            fhysics.setDrawer(drawer);
            fhysics.startUpdateLoop();
        });
    }
}