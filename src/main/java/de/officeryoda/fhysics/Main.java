package de.officeryoda.fhysics;

import de.officeryoda.fhysics.engine.FhysicsCore;
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("Fhysics");

        FhysicsCore fhysics = new FhysicsCore();

        SwingUtilities.invokeLater(() -> {
            FhysicsObjectDrawer drawer = new FhysicsObjectDrawer(fhysics);
            fhysics.setDrawer(drawer);
            fhysics.startUpdateLoop();
        });
    }
}