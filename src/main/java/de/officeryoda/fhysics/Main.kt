package de.officeryoda.fhysics

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.datastructures.QuadTree

// If it doesn't run you might need to change some VM options
// see https://www.youtube.com/watch?v=hS_6ek9rTco&ab_channel=BoostMyTool
fun main() {
    println("Fhysics")

    // Better safe than sorry
    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            QuadTree.shutdownThreadPool()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    })

    FhysicsCore.startEverything()
}