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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin

object FhysicsCore {

    /// =====constants=====
    val BORDER: Rectangle2D = Rectangle2D.Float(0.0F, 0.0F, 100.0F, 100.0F) // x and y must be 0.0
    private val COLLISION_SOLVER: CollisionSolver = ElasticCollision
    const val UPDATES_PER_SECOND: Int = 120
    private const val MAX_FRAMES_AT_CAPACITY: Int = 100
    private const val QTC_START_STEP_SIZE = 10.0

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
    private var stepSize: Double = QTC_START_STEP_SIZE
    private var lastSample: Double = 0.0
    private var lastCapacity: Int = QuadTree.capacity
    private var objectsAtStepSizeIncrease: Int = 0

    init {
        quadTree.subdivide()

//        for (i in 1..200) {
//            val circle: Circle = FhysicsObjectFactory.randomCircle()
//            circle.velocity.set(Vector2.ZERO)
//            spawn(circle)
//        }

        // spawn objects in a circle around the center
        val center = Vector2((BORDER.width / 2).toFloat(), (BORDER.height / 2).toFloat())
        val radius = 10.0f
        val numObjects = 10
        val speed = 2.0f // Desired speed for objects

        for (l in 1..4) {
            // Loop to spawn objects
            for (i in 0 until numObjects * l) {
                // Calculate angle and position
                val angle: Double = 2.0 * Math.PI * (i.toFloat() / (numObjects * l))
                val x: Double = center.x + radius * l * cos(angle)
                val y: Double = center.y + radius * l * sin(angle)

                // Calculate unit direction vector
                val cosAngle: Float = cos(angle).toFloat()
                val sinAngle: Float = sin(angle).toFloat()

                // Rotate direction by 90 degrees for tangent
                val velocity: Vector2 = Vector2(sinAngle, -cosAngle) * speed

                val circle: Circle =
                    FhysicsObjectFactory.customCircle(Vector2(x.toFloat(), y.toFloat()), 0.5f, velocity)
                spawn(circle)
            }
        }

        objectsAtStepSizeIncrease = objectCount
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

        spawnObject()

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
        if (updateCount % 10 != 0) return
        for (i: Int in 1..50) {
            if (objectCount < 20000) {
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
        if (obj !is Circle) return
        if (obj.position.x - obj.radius < 0.0F) {
            obj.velocity.x = -obj.velocity.x * UIController.wallElasticity
            obj.position.x = obj.radius
        } else if (obj.position.x + obj.radius > BORDER.width) {
            obj.velocity.x = -obj.velocity.x * UIController.wallElasticity
            obj.position.x = (BORDER.width - obj.radius).toFloat()
        }

        if (obj.position.y - obj.radius < 0.0F) {
            obj.velocity.y = -obj.velocity.y * UIController.wallElasticity
            obj.position.y = obj.radius
        } else if (obj.position.y + obj.radius > BORDER.height) {
            obj.velocity.y = -obj.velocity.y * UIController.wallElasticity
            obj.position.y = (BORDER.height - obj.radius).toFloat()
        }
    }

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
        // if the capacity is increasing or decreasing
        val valueDir: Int = -(crntSample - lastSample).sign.toInt()
        // if the mspu is increasing or decreasing
        val capacityDir: Int = (QuadTree.capacity - lastCapacity).sign
        // the direction the capacity should change to get closer to the optimal capacity
        val totalDir: Int = valueDir * capacityDir

        // calculate the new capacity
        val newCapacity: Int = QuadTree.capacity + stepSize.toInt() * totalDir

        // if a lot of objects got spawned since the last time the stepSize was increased then increase the stepSize again
        if (objectCount - objectsAtStepSizeIncrease > 100) {
            stepSize = max(
                stepSize,
                QTC_START_STEP_SIZE * 0.75
            ) // increase stepSize to increase the speed of the capacity optimization (max to not decrease when it's higher at the start of the program)
            objectsAtStepSizeIncrease = objectCount
        } else {
            stepSize =
                max(1.0, stepSize - 0.5) // reduces stepSize by one every 2 updates
        }

        return newCapacity
    }

    fun gravityAt(pos: Vector2): Vector2 {
        if (UIController.gravityType == GravityType.DIRECTIONAL) {
            return Vector2(0.0F, -9.81F)
        } else {
            val minDst = 0.05F
            val sqrDst: Float = UIController.gravityPoint.sqrDistance(pos)
            if (sqrDst < minDst * minDst) {
                // to not fling objects away and to avoid objects getting stuck in the
                // gravity point causing it to vibrate and hitting other objects away
                return Vector2.ZERO
            }

            val direction: Vector2 = UIController.gravityPoint - pos
            return UIController.gravityPointStrength / sqrDst * direction.normalized()
        }
    }

    fun nextId(): Int {
        return objectCount++
    }
}
