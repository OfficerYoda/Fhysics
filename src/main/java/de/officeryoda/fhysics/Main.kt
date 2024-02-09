package de.officeryoda.fhysics

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import javax.swing.SwingUtilities

fun main() {
    println("Fhysics")

    val fhysics = FhysicsCore()

    SwingUtilities.invokeLater {
        val drawer = FhysicsObjectDrawer(fhysics)
        fhysics.drawer = drawer
        fhysics.startUpdateLoop()
    }
}
