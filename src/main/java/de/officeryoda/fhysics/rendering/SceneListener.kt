package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Rectangle
import de.officeryoda.fhysics.rendering.RenderUtil.drawer
import de.officeryoda.fhysics.rendering.RenderUtil.zoomCenter
import javafx.scene.input.*
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
     * Handles mouse pressed events
     *
     * @param e the mouse event
     */
    fun onMousePressed(e: MouseEvent) {
        when (e.button) {
            MouseButton.PRIMARY -> {
                // Select object if hovered, otherwise spawn object
                if (drawer.hoveredObject != null) {
                    drawer.selectedObject = drawer.hoveredObject
                    UIController.instance.expandObjectPropertiesPane()
                } else {
                    drawer.selectedObject = null
                    spawnObject(e)
                }
            }

            MouseButton.SECONDARY -> {
                rightPressed = true
                rightPressedPos = RenderUtil.screenToWorld(Vector2(e.x.toFloat(), e.y.toFloat()))
            }

            else -> {}
        }

        UIController.instance.updateObjectPropertiesValues()
    }

    /**
     * Handles mouse released events
     *
     * @param e the mouse event
     */
    fun onMouseReleased(e: MouseEvent) {
        when (e.button) {
            MouseButton.PRIMARY -> {
                if (dragStartWorldPos != null) {
                    dragStartWorldPos = null
                    FhysicsCore.spawn(drawer.spawnPreview!!.clone())
                    UIController.instance.updateSpawnPreview()
                }
            }

            MouseButton.SECONDARY -> rightPressed = false
            else -> {}
        }
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
            MouseButton.PRIMARY -> dragSpawnPreview(e)
            MouseButton.SECONDARY -> dragCamera(e)

            else -> {}
        }
    }

    private var dragStartWorldPos: Vector2? = null

    private fun dragSpawnPreview(e: MouseEvent) {
        if (dragStartWorldPos == null) {
            dragStartWorldPos = mouseWorldPos.copy()
        }

        val minX: Float = min(dragStartWorldPos!!.x, mouseWorldPos.x)
        val maxX: Float = max(dragStartWorldPos!!.x, mouseWorldPos.x)
        val minY: Float = min(dragStartWorldPos!!.y, mouseWorldPos.y)
        val maxY: Float = max(dragStartWorldPos!!.y, mouseWorldPos.y)

        val size = Vector2(maxX - minX, maxY - minY)
        val pos: Vector2 = Vector2(minX, minY) + size / 2f
        drawer.spawnPreview = Rectangle(pos, size.x, size.y)
    }

    private fun dragCamera(e: MouseEvent) {
        if (rightPressed) {
            val mousePos: Vector2 = RenderUtil.screenToWorld(Vector2(e.x.toFloat(), e.y.toFloat()))
            val deltaMousePos: Vector2 = rightPressedPos - mousePos
            targetZoomCenter = targetZoomCenter + deltaMousePos
            zoomCenter = targetZoomCenter
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
            else -> {}
        }
    }

    /**
     * Spawns an object at the mouse position
     *
     * @param e the mouse event
     */
    private fun spawnObject(e: MouseEvent) {
        // Check if spawn pos is outside the border
        val transformedMousePos: Vector2 = RenderUtil.screenToWorld(Vector2(e.x.toFloat(), e.y.toFloat()))
        if (!FhysicsCore.BORDER.contains(transformedMousePos)) return

        val validParams: Boolean = when (UIController.spawnObjectType) {
            SpawnObjectType.CIRCLE -> UIController.spawnRadius > 0.0F
//            SpawnObjectType.RECTANGLE -> UIController.spawnWidth > 0.0F && UIController.spawnHeight > 0.0F
            else -> false
        }

        if (!validParams) return

        FhysicsCore.spawn(drawer.spawnPreview!!.clone())
    }
}