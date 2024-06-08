package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.ConvexPolygon
import de.officeryoda.fhysics.engine.objects.Rectangle
import de.officeryoda.fhysics.rendering.RenderUtil.drawer
import de.officeryoda.fhysics.rendering.RenderUtil.zoomCenter
import javafx.scene.input.*
import java.awt.Color
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

object SceneListener {

    /**
     * The position of the mouse in world space
     */
    val mouseWorldPos: Vector2 = Vector2.ZERO

    /**
     * The position of the mouse when the right mouse button was pressed
     */
    private var rightPressed: Boolean = false

    /**
     * The position of the mouse when the right mouse button was pressed
     */
    private var rightPressedPos: Vector2 = Vector2.ZERO

    /**
     * The target zoom factor of the drawer (used for smooth zooming)
     */
    private var targetZoom: Double
        get() = drawer.targetZoom
        set(value) {
            drawer.targetZoom = value
        }

    /**
     * The target zoom center of the drawer (used for smooth zooming)
     */
    private var targetZoomCenter: Vector2
        get() = drawer.targetZoomCenter
        set(value) {
            drawer.targetZoomCenter = value
        }

    /**
     * The minimum distance the mouse has to be dragged to spawn a rectangle
     */
    private const val MIN_DRAG_DISTANCE: Float = 2.0F

    /**
     * The position of the mouse when the dragging started
     */
    private var dragStartWorldPos: Vector2? = null

    /**
     * Whether the drag spawn was canceled by right-clicking
     */
    private var canceledDragSpawn: Boolean = false

    /**
     * The radius around the first polygon vertex where the polygon closes when clicked inside
     */
    const val POLYGON_CLOSE_RADIUS = 1.0f

    /**
     * The vertices of the polygon being created
     */
    var polyVertices: MutableList<Vector2> = ArrayList()

    /**
     * Whether the polygon is valid
     */
    var validPolygon = true

    /**
     * Handles mouse pressed events
     *
     * @param e the mouse event
     */
    fun onMousePressed(e: MouseEvent) {
        when (e.button) {
            MouseButton.PRIMARY -> handlePrimaryButtonPressed()
            MouseButton.SECONDARY -> handleSecondaryButtonPressed()
            else -> {}
        }

        UIController.instance.updateObjectPropertiesValues()
    }

    /**
     * Handles the press of the primary mouse button
     */
    private fun handlePrimaryButtonPressed() {
        // Add a vertex to the polygon
        if (UIController.spawnObjectType == SpawnObjectType.POLYGON) {
            val pos: Vector2 = mouseWorldPos.copy()

            // Create the polygon if the polygon is complete
            if (polyVertices.size > 2 && validPolygon) {
                val startPos: Vector2 = polyVertices.first()
                if (pos.sqrDistance(startPos) < POLYGON_CLOSE_RADIUS * POLYGON_CLOSE_RADIUS) {
                    // Map the vertices relative to the center
                    val vertices: List<Vector2> = ensureCCW(polyVertices.map { it })
                    // create the polygon
                    val polygon = ConvexPolygon(vertices.toTypedArray())

                    FhysicsCore.spawn(polygon)

                    polyVertices.clear()
                    return
                }
            }

            polyVertices.add(pos)
            validPolygon = validatePolyVertices(polyVertices)

            return
        }

        // Select object if hovered, otherwise spawn object
        if (drawer.hoveredObject != null) {
            drawer.selectedObject = drawer.hoveredObject
            UIController.instance.expandObjectPropertiesPane()
        } else {
            drawer.selectedObject = null
        }
    }

    /**
     * Handles the press of the secondary mouse button
     */
    private fun handleSecondaryButtonPressed() {
        rightPressed = true
        rightPressedPos = mouseWorldPos.copy()
    }

    /**
     * Handles mouse released events
     *
     * @param e the mouse event
     */
    fun onMouseReleased(e: MouseEvent) {
        when (e.button) {
            MouseButton.PRIMARY -> handlePrimaryButtonReleased()
            MouseButton.SECONDARY -> handleSecondaryButtonReleased()
            else -> {}
        }
    }

    /**
     * Handles the release of the primary mouse button
     */
    private fun handlePrimaryButtonReleased() {
        when {
            hasDraggedMinDistance() -> {
                if (!canceledDragSpawn) {
                    FhysicsCore.spawn(drawer.spawnPreview!!.clone())
                    UIController.instance.updateSpawnPreview()
                }
            }

            drawer.hoveredObject == null && !canceledDragSpawn -> {
                spawnObject()
            }
        }

        // Reset the drag state
        canceledDragSpawn = false
        dragStartWorldPos = null
    }

    /**
     * Handles the release of the secondary mouse button
     */
    private fun handleSecondaryButtonReleased() {
        // Cancel drag spawning
        if (dragStartWorldPos != null) {
            canceledDragSpawn = true
            dragStartWorldPos = null
            UIController.instance.updateSpawnPreview()
        }

        // Clear the polygon vertices for a new polygon
        polyVertices.clear()

        rightPressed = false
    }

    /**
     * Handles mouse wheel events
     *
     * @param e the scroll event
     */
    fun onMouseWheel(e: ScrollEvent) {
        // Don't zoom if moving with the mouse
        if (rightPressed) return

        val zoomBefore: Double = RenderUtil.zoom
        val deltaZoom: Double = exp(targetZoom * 0.035)

        // Record the mouse position before zooming
        var mousePosBeforeZoom = Vector2(e.x.toFloat(), e.y.toFloat())
        mousePosBeforeZoom = RenderUtil.screenToWorld(mousePosBeforeZoom)

        // Adjust the zoom amount
        RenderUtil.zoom += deltaZoom * e.deltaY.sign
        RenderUtil.zoom = RenderUtil.zoom.coerceIn(2.0, 120.0)

        // Record the mouse position after zooming
        var mousePosAfterZoom = Vector2(e.x.toFloat(), e.y.toFloat())
        mousePosAfterZoom = RenderUtil.screenToWorld(mousePosAfterZoom)

        // Calculate the difference in mouse position caused by zooming
        val deltaMousePos: Vector2 = mousePosBeforeZoom - mousePosAfterZoom

        // Update the zoom center to keep the mouse at the same world position
        targetZoomCenter = zoomCenter + deltaMousePos

        // Update the target zoom
        targetZoom = RenderUtil.zoom
        RenderUtil.zoom = zoomBefore
    }

    /**
     * Handles mouse moved events
     *
     * @param e the mouse event
     */
    fun onMouseMoved(e: MouseEvent) {
        // Update the mouse position
        mouseWorldPos.x = RenderUtil.screenToWorldX(e.x.toFloat()).toFloat()
        mouseWorldPos.y = RenderUtil.screenToWorldY(e.y.toFloat()).toFloat()
    }

    /**
     * Handles mouse dragged events
     *
     * @param e the mouse event
     */
    fun onMouseDragged(e: MouseEvent) {
        // to update the mouse position
        onMouseMoved(e)

        when (e.button) {
            MouseButton.PRIMARY -> dragRectanglePreview()
            MouseButton.SECONDARY -> dragCamera(e)
            else -> {}
        }
    }

    /**
     * Handles key pressed events
     *
     * @param event the key event
     */
    fun onKeyPressed(event: KeyEvent) {
        when (event.code) {
            KeyCode.P -> FhysicsCore.running = !FhysicsCore.running
            KeyCode.SPACE -> FhysicsCore.update()
            KeyCode.ENTER -> FhysicsCore.update()
            KeyCode.Z -> drawer.resetZoom()
            KeyCode.J -> QuadTree.capacity -= 5
            KeyCode.K -> QuadTree.capacity += 5
            KeyCode.G -> CapacityDiagram(FhysicsCore.qtCapacity)
            KeyCode.Q -> println(QuadTree.root)
            KeyCode.S -> println(drawer.selectedObject)
            else -> {}
        }
    }

    /**
     * Creates a preview of a rectangle with the current mouse position
     * and the position where the dragging started
     *
     * @return the preview rectangle
     */
    private fun dragRectanglePreview() {
        // Don't create a drag preview if the spawn object type is not a rectangle
        if (UIController.spawnObjectType != SpawnObjectType.RECTANGLE) return

        // Set the drag start position if it is not set yet
        if (dragStartWorldPos == null) {
            dragStartWorldPos = mouseWorldPos.copy()
        }

        // Don't create a drag preview if the drag distance is too small
        if (!hasDraggedMinDistance()) return

        // Calculate the corners of the rectangle
        val minX: Float = min(dragStartWorldPos!!.x, mouseWorldPos.x)
        val maxX: Float = max(dragStartWorldPos!!.x, mouseWorldPos.x)
        val minY: Float = min(dragStartWorldPos!!.y, mouseWorldPos.y)
        val maxY: Float = max(dragStartWorldPos!!.y, mouseWorldPos.y)

        // Create the preview rectangle
        val size = Vector2(maxX - minX, maxY - minY)
        val pos: Vector2 = Vector2(minX, minY) + size / 2f

        val rect = Rectangle(pos, size.x, size.y)
        rect.color = Color(rect.color.red, rect.color.green, rect.color.blue, 128)

        drawer.spawnPreview = rect
    }

    /**
     * Drags the camera by moving the zoom center
     *
     * @param e the mouse event
     */
    private fun dragCamera(e: MouseEvent) {
        if (rightPressed) {
            val mousePos: Vector2 = RenderUtil.screenToWorld(Vector2(e.x.toFloat(), e.y.toFloat()))
            val deltaMousePos: Vector2 = rightPressedPos - mousePos
            targetZoomCenter = targetZoomCenter + deltaMousePos
            zoomCenter = targetZoomCenter
        }
    }

    /**
     * Spawns an object at the mouse position
     */
    private fun spawnObject() {
        // Check if spawn pos is outside the border
        if (!FhysicsCore.BORDER.contains(mouseWorldPos)) return

        val validParams: Boolean = when (UIController.spawnObjectType) {
            SpawnObjectType.CIRCLE -> UIController.spawnRadius > 0.0F
            SpawnObjectType.RECTANGLE -> UIController.spawnWidth > 0.0F && UIController.spawnHeight > 0.0F && !hasDraggedMinDistance()
            else -> false
        }

        if (!validParams) return

        FhysicsCore.spawn(drawer.spawnPreview!!.clone())
    }

    /**
     * Checks if the mouse has been dragged over or the minimum distance
     *
     * @return true if the mouse has been dragged over or the minimum distance
     */
    private fun hasDraggedMinDistance(): Boolean {
        if (dragStartWorldPos == null) return false
        return (mouseWorldPos - dragStartWorldPos!!).sqrMagnitude() >= MIN_DRAG_DISTANCE * MIN_DRAG_DISTANCE
    }

    /**
     * Validates the polygon vertices
     * Checks if the polygon is valid by checking if the lines intersect
     * and if the polygon is convex
     *
     * @return true if the polygon is valid
     */
    fun validatePolyVertices(vertices: MutableList<Vector2>): Boolean {
        val size: Int = vertices.size
        if (size < 3) return false

        return !areLinesIntersecting(vertices) && !isConcave(vertices)
    }

    /**
     * Checks if the lines of the polygon are intersecting
     *
     * @return true if the lines are intersecting
     */
    private fun areLinesIntersecting(vertices: MutableList<Vector2>): Boolean {
        val size: Int = vertices.size
        for (i: Int in 0 until size) {
            for (j: Int in i + 1 until size) {
                val line1: Pair<Vector2, Vector2> = Pair(vertices[i], vertices[(i + 1) % size])
                val line2: Pair<Vector2, Vector2> = Pair(vertices[j], vertices[(j + 1) % size])
                if (doLinesIntersect(line1, line2)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if two lines intersect
     *
     * @param lineA the first line
     * @param lineB the second line
     */
    private fun doLinesIntersect(lineA: Pair<Vector2, Vector2>, lineB: Pair<Vector2, Vector2>): Boolean {
        val a: Vector2 = lineA.first
        val b: Vector2 = lineA.second
        val c: Vector2 = lineB.first
        val d: Vector2 = lineB.second

        val denominator: Float = ((b.x - a.x) * (d.y - c.y)) - ((b.y - a.y) * (d.x - c.x))

        // If the denominator is zero, lines are parallel and do not intersect
        if (denominator == 0.0f) {
            return false
        }

        // Calculate the numerators of the line intersection formula
        val numeratorA: Float = ((a.y - c.y) * (d.x - c.x)) - ((a.x - c.x) * (d.y - c.y))
        val numeratorB: Float = ((a.y - c.y) * (b.x - a.x)) - ((a.x - c.x) * (b.y - a.y))

        // Calculate r and s parameters
        val r: Float = numeratorA / denominator
        val s: Float = numeratorB / denominator

        // If r and s are both between 0 and 1, lines intersect (excluding endpoints)
        return (0f < r && r < 1f) && (0f < s && s < 1f)
    }

    /**
     * Checks if the polygon is concave
     *
     * @return true if the polygon is concave
     */
    private fun isConcave(vertices: MutableList<Vector2>): Boolean {
        val ccwVertices: List<Vector2> = ensureCCW(vertices)
        val size: Int = ccwVertices.size
        for (i: Int in 0 until size) {
            val a: Vector2 = ccwVertices[i]
            val b: Vector2 = ccwVertices[(i + 1) % size]
            val c: Vector2 = ccwVertices[(i + 2) % size]

            val crossProduct: Float = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
            if (crossProduct < 0) {
                return true
            }
        }
        return false
    }

    /**
     * Returns the vertices in counter-clockwise order
     *
     * @param vertices the vertices of the polygon
     */
    private fun ensureCCW(vertices: List<Vector2>): List<Vector2> {
        // Calculate the signed area of the polygon
        var signedArea = 0f
        for (i: Int in vertices.indices) {
            val j: Int = (i + 1) % vertices.size
            signedArea += vertices[i].x * vertices[j].y - vertices[j].x * vertices[i].y
        }
        signedArea /= 2

        // Reverse the vertices if the polygon is CW
        return if (signedArea < 0) vertices.reversed() else vertices
    }
}