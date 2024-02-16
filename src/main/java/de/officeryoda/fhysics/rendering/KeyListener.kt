package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class KeyListener(val fhysics: FhysicsCore) : KeyListener {
    override fun keyTyped(e: KeyEvent) {

    }

    override fun keyPressed(e: KeyEvent) {
        val pressedChar: Char = e.keyChar.lowercaseChar()

        // if pressed char is p toggle isRunning in FhysicsCore
        // if it is Enter or space call the update function
        when (pressedChar) {
            'p' -> fhysics.isRunning = !fhysics.isRunning
            ' ' -> fhysics.update() // space
            '\n' -> fhysics.update() // enter
        }
    }

    override fun keyReleased(e: KeyEvent) {
    }
}
