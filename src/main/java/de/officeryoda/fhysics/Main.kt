package de.officeryoda.fhysics

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.datastructures.QuadTree

/**
 * The size of the window
 */
var size = 960

// If it doesn't run you might need to change some VM options
// see https://www.youtube.com/watch?v=hS_6ek9rTco&ab_channel=BoostMyTool
fun main(args: Array<String>) {
    if (args.contains("-small")) {
        size = 720
    }

    println("Fhysics")
    println("Size: $size")

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