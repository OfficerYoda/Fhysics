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

    // Constants
    val BORDER: BoundingBox = BoundingBox(0f, 0f, 100f, 100f) // x and y must be 0.0
    const val UPDATES_PER_SECOND: Int = 60 * 4
    const val SUB_STEPS: Int = 1
    val RENDER_LOCK = ReentrantLock()
    const val EPSILON: Float = 1E-4f

    // Variables
    private var objectCount: Int = 0
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
        Thread { FhysicsObjectDrawer().launch() }.start()
        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Long = (1f / UPDATES_PER_SECOND * 1000).toLong()

        Timer("Fhysics-Core", true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (running) {
                    RENDER_LOCK.lock()
                    update()
                    RENDER_LOCK.unlock()
                }
            }
        }, 0, updateIntervalMillis)
    }

    fun update() {
        updateStopwatch.start()

        QuadTree.processPendingOperations()

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
        objectCount = 0
        QuadTree.clear()
    }

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

    fun nextId(): Int {
        return objectCount++
    }
}
