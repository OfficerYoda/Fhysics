package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.datastructures.spatial.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.util.Stopwatch
import de.officeryoda.fhysics.engine.util.times
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.GravityType
import de.officeryoda.fhysics.rendering.SceneListener
import de.officeryoda.fhysics.rendering.UIController
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max

object FhysicsCore {

    /** The Bounds of the simulation */
    val BORDER: BoundingBox = BoundingBox(0f, 0f, 100f, 100f) // x and y must be 0.0

    /** The amount of updates the simulation should perform per second */
    const val UPDATES_PER_SECOND: Int = 60

    /** The amount of sub steps the simulation performs per update */
    const val SUB_STEPS: Int = 4

    /** A Thread lock to prevent simulating and rendering at the same time */
    val RENDER_LOCK = ReentrantLock()

    /** A small value used for floating point comparisons */
    const val EPSILON: Float = 1E-4f

    var updateCount = 0 // Includes all sub steps

    var dt: Float = 1.0f / (UPDATES_PER_SECOND * SUB_STEPS)
    var running: Boolean = true
    val updateStopwatch = Stopwatch(20)

    init {
//        val objects: List<FhysicsObject> = List(5) { FhysicsObjectFactory.randomCircle() }
//        for (it: FhysicsObject in objects) {
//            it.restitution = 1f
//            it.frictionDynamic = 0f
//            it.frictionStatic = 0f
//        }
//        UIController.setBorderProperties(1f, 1f, 1f)
//        spawn(objects)

//        repeat(100) {
//            spawn(FhysicsObjectFactory.randomRectangle())
//        }
//
//        repeat(10) {
//            spawn(FhysicsObjectFactory.randomConcavePolygon())
//        }
    }


    fun startEverything() {
        // Start the rendering thread
        Thread { FhysicsObjectDrawer().launch() }.start()
        // Start the simulation, running on this thread
        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Long = (1f / UPDATES_PER_SECOND * 1000).toLong()

        Timer("Fhysics-Core", true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (running) {
                    // Don't render while updating to prevent concurrentModificationExceptions
                    RENDER_LOCK.lock()
                    update()
                    RENDER_LOCK.unlock()
                }
            }
        }, 0, updateIntervalMillis)
    }

    /**
     * Updates the simulation by one step.
     */
    fun update() {
        updateStopwatch.start()

        // Rebuild once per update
        QuadTree.processPendingOperations()
        QuadTree.rebuild()

        // Update objects multiple times per update
        repeat(SUB_STEPS) {
            QuadTree.update()

            SceneListener.pullObject()

            updateCount++
        }

        updateStopwatch.stop()
    }

    fun spawn(vararg objects: FhysicsObject) {
        spawn(objects.toList())
    }

    private fun spawn(objects: List<FhysicsObject>) {
        for (obj: FhysicsObject in objects) {
            obj.updateBoundingBox()
            QuadTree.insert(obj)
        }
    }

    fun clear() {
        QuadTree.clear()
    }

    /**
     * Calculates the gravity at the given [position][pos].
     */
    fun gravityAt(pos: Vector2): Vector2 {
        if (UIController.gravityType == GravityType.DIRECTIONAL) {
            return UIController.gravityDirection
        } else {
            val direction: Vector2 = UIController.gravityPoint - pos
            val sqrDistance: Float =
                max(1f, direction.sqrMagnitude()) // Prevent high forces when the object is close to the gravity point
            return UIController.gravityPointStrength / sqrDistance * direction.normalized()
        }
    }
}
