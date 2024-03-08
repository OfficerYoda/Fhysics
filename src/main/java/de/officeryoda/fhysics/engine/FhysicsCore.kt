package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.collision.ElasticCollision
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.FhysicsObjectFactory
import de.officeryoda.fhysics.rendering.GravityType
import de.officeryoda.fhysics.rendering.UIController
import java.awt.geom.Rectangle2D
import java.util.*
import java.util.Timer
import kotlin.math.max
import kotlin.math.sign

object FhysicsCore {

    /// =====constants=====
    // x and y must be 0.0
    val BORDER: Rectangle2D = Rectangle2D.Float(0.0F, 0.0F, 100.0F, 100.0F)
    private val COLLISION_SOLVER: CollisionSolver = ElasticCollision
    const val UPDATES_PER_SECOND: Int = 200
    private const val MAX_FRAMES_AT_CAPACITY: Int = 100

    /// =====variables=====
    var quadTree: QuadTree = QuadTree(BORDER, null)

    var objectCount: Int = 0
    val fhysicsObjects: MutableList<FhysicsObject> = ArrayList()

    var updateCount = 0

    var dt: Float = 1.0F / UPDATES_PER_SECOND
    var running: Boolean = true
    val updateTimer = Timer(50)

    // quad tree capacity optimization
    val qtCapacity: MutableMap<Int, Double> = mutableMapOf()
    private var framesAtCapacity: Int = 0

    init {
        quadTree.subdivide()

        for (i in 1..200) {
            val circle: Circle = FhysicsObjectFactory.randomCircle()
            circle.velocity.set(Vector2.ZERO)
            spawn(circle)
        }

        // spawn objects on the x-axis
//        for (i in 1..30) {
//            val circle: Circle = FhysicsObjectFactory.randomCircle()
//            circle.velocity.set(Vector2(0.0F, 0.0F))
//            circle.position.set(Vector2(i.toFloat(), 50.0F))
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
        updateTimer.start()

//        spawnObject()

        quadTree.updateObjectsAndRebuild()

        checkObjectCollision(quadTree)

        optimizeQuadTreeCapacity()

        updateCount++
        updateTimer.stop()
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
        if(obj !is Circle) return
        if (obj.position.x - obj.radius < 0.0F) {
            obj.velocity.x = -obj.velocity.x
            obj.position.x = obj.radius
        } else if (obj.position.x + obj.radius > BORDER.width) {
            obj.velocity.x = -obj.velocity.x
            obj.position.x = (BORDER.width - obj.radius).toFloat()
        }

        if (obj.position.y - obj.radius < 0.0F) {
            obj.velocity.y = -obj.velocity.y
            obj.position.y = obj.radius
        } else if (obj.position.y + obj.radius > BORDER.height) {
            obj.velocity.y = -obj.velocity.y
            obj.position.y = (BORDER.height - obj.radius).toFloat()
        }
    }

    private var stepSize: Double = 10.0
    private var lastSample: Double = 0.0
    private var lastCapacity: Int = QuadTree.capacity

    private fun optimizeQuadTreeCapacity() {
        framesAtCapacity++
        if (framesAtCapacity > MAX_FRAMES_AT_CAPACITY) { // > and not >= to exclude the first frame which where the rebuild will take the longest
            val average: Double = updateTimer.average()

            qtCapacity[QuadTree.capacity] = average
            val newCapacity: Int = calculateNextQTCapacity()

            lastCapacity = QuadTree.capacity
            QuadTree.capacity = newCapacity
            lastSample = average
            framesAtCapacity = 0
        }
    }

    private fun calculateNextQTCapacity(): Int {
        // two samples are needed to calculate the next capacity
        if (qtCapacity.size < 2)
            return QuadTree.capacity + 5

        val crntSample: Double = qtCapacity[QuadTree.capacity]!!

        val valueDir: Int = -(crntSample - lastSample).sign.toInt()
        val capacityDir: Int = (QuadTree.capacity - lastCapacity).sign
        val totalDir: Int = valueDir * capacityDir

        val newCapacity: Int = QuadTree.capacity + stepSize.toInt() * totalDir

        stepSize = max(1.0, stepSize - 0.5) // reduces stepSize by one every 2 updates because it's rounded to an int

        return newCapacity
    }

    fun gravityAt(pos: Vector2): Vector2 {
        if (UIController.gravityType == GravityType.DIRECTIONAL) {
            return Vector2(0.0F, -9.81F)
        } else {
            val minDst = 0.05F
            val sqrDst: Float = UIController.gravityPoint.sqrDistance(pos)
            if(sqrDst < minDst) {
                // to not fling objects away and to avoid objects getting stuck in the
                // gravity point causing it to vibrate and hitting other objects away
                return Vector2.ZERO
            }

            val direction: Vector2 = UIController.gravityPoint - pos
            return UIController.gravityPointStrength/sqrDst * direction.normalized()
        }
    }

    fun nextId(): Int {
        return objectCount++
    }
}
