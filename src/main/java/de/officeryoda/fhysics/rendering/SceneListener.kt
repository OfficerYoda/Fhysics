package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.engine.objects.PolygonCreator
import de.officeryoda.fhysics.engine.objects.PolygonCreator.isPolygonValid
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
     * The minimum distance the mouse has to be dragged to spawn a rectangle
     */
    private const val MIN_DRAG_DISTANCE: Float = 2.0F

    /**
     * The position of the mouse in world space
     */
    val mouseWorldPos: Vector2 = Vector2.ZERO

    /**
     * Whether the right mouse button is pressed
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
    private var polyVertices: MutableList<Vector2> = ArrayList()

    /**
     * Whether the polygon is valid
     */
    private var validPolygon = true

    /**
     * The preview object to spawn
     */
    private var spawnPreview: FhysicsObject? = null

    /**
     * The object that is currently hovered by the mouse
     */
    var hoveredObject: FhysicsObject? = null

    /**
     * The object that is currently selected
     */
    var selectedObject: FhysicsObject? = null

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
                if (pos.sqrDistanceTo(startPos) < POLYGON_CLOSE_RADIUS * POLYGON_CLOSE_RADIUS) {
                    createAndSpawnPolygon()
                    return
                }
            }

            // Add the vertex to the polygon
            polyVertices.add(pos)
            validPolygon = isPolygonValid(polyVertices)

            return
        }

        // Select object if hovered, otherwise spawn object
        if (hoveredObject != null) {
            selectedObject = hoveredObject
            UIController.instance.expandObjectPropertiesPane()
        } else {
            selectedObject = null
        }
    }

    /**
     * Creates and spawns a polygon from the current vertices
     */
    private fun createAndSpawnPolygon() {
        // Map the vertices relative to the center
        val polygon: Polygon = PolygonCreator.createPolygon(polyVertices.toTypedArray())
        FhysicsCore.spawn(polygon)

        polyVertices.clear()
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
                    FhysicsCore.spawn(spawnPreview!!.clone())
//                    UIController.instance.updateSpawnPreview()
                }
            }

            hoveredObject == null && !canceledDragSpawn -> {
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
//            UIController.instance.updateSpawnPreview()
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

        spawnPreview?.position?.set(mouseWorldPos)
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
            KeyCode.SPACE -> {
                DebugDrawer.clearDebug()
                FhysicsCore.update()
            }

            KeyCode.ENTER -> {
                DebugDrawer.clearDebug()
                FhysicsCore.update()
            }
            KeyCode.Z -> drawer.resetZoom()
            KeyCode.J -> QuadTree.capacity -= 5
            KeyCode.K -> QuadTree.capacity += 5
            KeyCode.G -> CapacityDiagram(FhysicsCore.qtCapacity)
            KeyCode.Q -> println(QuadTree.root.objects.forEach { println(it) })
            KeyCode.S -> println(selectedObject)
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

        spawnPreview = rect
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

        FhysicsCore.spawn(spawnPreview!!.clone())
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
}