package de.officeryoda.fhysics

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer

class Main {

    private lateinit var fhysics: FhysicsCore

    companion object {
        // needs to be run with special VM options
        // see https://www.youtube.com/watch?v=hS_6ek9rTco&ab_channel=BoostMyTool
        @JvmStatic
        fun main(args: Array<String>) {
            FhysicsObjectDrawer().launch()
        }
    }

}