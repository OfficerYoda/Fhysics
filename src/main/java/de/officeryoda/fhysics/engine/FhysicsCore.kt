package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.collision.ElasticCollision
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.FhysicsObjectFactory
import de.officeryoda.fhysics.objects.Rectangle
import java.awt.geom.Rectangle2D
import java.util.*

object FhysicsCore {

    /// =====constants=====
    // x and y must be 0.0
    val BORDER: Rectangle2D = Rectangle2D.Float(0.0F, 0.0F, 250.0F, 250.0F)
    private val COLLISION_SOLVER: CollisionSolver = ElasticCollision
    const val QUAD_TREE_CAPACITY: Int = 32
    const val UPDATES_PER_SECOND: Int = 200
    /// =====variables=====
    var quadTree: QuadTree = QuadTree(BORDER, QUAD_TREE_CAPACITY, null)

    var objectCount: Int = 0
    val fhysicsObjects: MutableList<FhysicsObject> = ArrayList()
    private val borderRects: List<Rectangle>

    var updateCount = 0
    private val updateDurations: MutableList<Long> = mutableListOf()

    var dt: Float = 1.0F / UPDATES_PER_SECOND
    var running: Boolean = true

    init {
        borderRects = createBorderBoxes()
        quadTree.subdivide()

//        for (i in 1..3000) {
//            val circle = FhysicsObjectFactory.randomCircle()
////            circle.radius *= 2
//            spawn(circle)
//        }

//        for (i in 1..14) {
//            val rect = FhysicsObjectFactory.randomRectangle()
//            spawn(rect)
//        }

        // create 20 circles in the middle of the width with different heights and no velocity
//        for (i in 1 until 1000) {
//            val pos = Vector2(i.toFloat() / 10, 90.0)
//            val vel = Vector2(0.0, 0.0)
//            val circle = FhysicsObjectFactory.customCircle(pos, 0.1, vel)
//            spawn(circle)
//        }

        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Int = (1f / UPDATES_PER_SECOND * 1000).toInt()
        Timer(true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (running) {
                    update()
                }
            }
        }, 0, updateIntervalMillis.toLong())
    }

    fun update() {
        val startTime: Long = System.nanoTime()

//        spawnObject()

        quadTree.updateObjectsAndRebuild()

        checkObjectCollision(quadTree)

        updateCount++
        addUpdateDuration(System.nanoTime() - startTime)
    }

    // This method must be called when trying to spawn an object
    fun spawn(obj: FhysicsObject) {
        fhysicsObjects.add(obj)
        quadTree.insert(obj)
    }

    private fun spawnObject() {
        for (i in 1..100) {
            if (objectCount < 60000) {
                spawn(FhysicsObjectFactory.randomCircle())
            }
        }
    }

    private fun checkObjectCollision(quadTree: QuadTree) {
        if (!quadTree.divided) {
            val objects = quadTree.objects
            val numObjects = objects.size

            for (i in 0 until numObjects - 1) {
                val objA = objects[i]

                for (j in i + 1 until numObjects) {
                    val objB = objects[j]
                    handleCollision(objA, objB)
                }
            }
        } else {
            checkObjectCollision(quadTree.topLeft!!)
            checkObjectCollision(quadTree.topRight!!)
            checkObjectCollision(quadTree.botLeft!!)
            checkObjectCollision(quadTree.botRight!!)
        }
    }

    private fun handleCollision(objA: FhysicsObject, objB: FhysicsObject) {
        val points: CollisionInfo = objA.testCollision(objB)

        if (points.overlap == -1.0F) return

        COLLISION_SOLVER.solveCollision(points)
    }

    fun checkBorderCollision(obj: FhysicsObject) {
        borderRects.forEach { handleCollision(obj, it) }
    }

    private fun createBorderBoxes(): List<Rectangle> {
        val width: Float = BORDER.width.toFloat()
        val height: Float = BORDER.height.toFloat()

        val left: Rectangle = FhysicsObjectFactory.customRectangle(Vector2(-width, 0.0F), width, height, Vector2.ZERO)
        val right: Rectangle = FhysicsObjectFactory.customRectangle(Vector2(width, 0.0F), width, height, Vector2.ZERO)
        val top: Rectangle = FhysicsObjectFactory.customRectangle(Vector2(-width, height), 3 * width, height, Vector2.ZERO)
        val bottom: Rectangle = FhysicsObjectFactory.customRectangle(Vector2(-width, -height), 3 * width, height, Vector2.ZERO)

        return listOf(left, right, top, bottom)
    }

    private fun addUpdateDuration(updateTime: Long) {
        val updatesToAverage = 100

        updateDurations.add(updateTime)

        if (updateDurations.size > updatesToAverage) {
            updateDurations.removeAt(0) // Remove the oldest time if more than specified updates have been recorded
        }
    }

    fun getAverageUpdateDuration(): Float {
        return updateDurations
            .toList()
            .filterNotNull() // this is not redundant even if IntelliJ says so
            .map { it / 1E6 } // convert nano to milliseconds
            .average().toFloat()
    }

    fun nextId(): Int {
        return objectCount++
    }
}
