package de.officeryoda.fhysics

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.datastructures.QuadTree

// needs to be run with special VM options
// see https://www.youtube.com/watch?v=hS_6ek9rTco&ab_channel=BoostMyTool
fun main() {
    println("Fhysics")

    FhysicsCore.startEverything()

    // Better safe than sorry
    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            QuadTree.shutdownThreadPool()
        } catch (_: Exception) {
        }
    })
}