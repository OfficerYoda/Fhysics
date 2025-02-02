package de.officeryoda.fhysics.visual

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Settings
import de.officeryoda.fhysics.engine.Settings.EPSILON
import de.officeryoda.fhysics.engine.SpawnObjectType
import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.engine.objects.Rectangle
import de.officeryoda.fhysics.engine.objects.factories.PolygonFactory
import de.officeryoda.fhysics.visual.RenderUtil.render
import de.officeryoda.fhysics.visual.RenderUtil.toScreenSpace
import de.officeryoda.fhysics.visual.RenderUtil.toWorldSpace
import javafx.scene.input.*
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

/**
 * The listener for the scene that handles input events.
 */
object SceneListener {

    /** The minimum distance the mouse has to be moved in screen space to be registered as a drag. */
    private const val MIN_DRAG_DISTANCE_SQR: Float = 4f // 2px

    /** The position of the mouse in screen space. */
    private val mousePosScreen: Vector2 = Vector2.ZERO

    /** The position of the mouse in world space. */
    val mousePosWorld: Vector2 = Vector2.ZERO

    /** Whether the left mouse button is pressed. */
    private var leftPressed: Boolean = false

    /** Whether the right mouse button is pressed. */
    private var rightPressed: Boolean = false

    /** Whether the left mouse button is being dragged. */
    private var leftDragging: Boolean = false

    /** The position where the left mouse button was pressed in screen space. */
    private val leftPressedPosScreen: Vector2 = Vector2.ZERO

    /** The position where the left mouse button was pressed in world space. */
    private val leftPressedPosWorld: Vector2 = Vector2.ZERO

    /** The position where the right mouse button was pressed in screen space. */
    private val rightPressedPosScreen: Vector2 = Vector2.ZERO

    /** The position where the right mouse button was pressed in world space. */
    private val rightPressedPosWorld: Vector2 = Vector2.ZERO

    /** The preview object to spawn. */
    var spawnPreview: FhysicsObject? = null

    /** The object that is currently selected. */
    var selectedObject: FhysicsObject? = null

    /** The object that is currently hovered. */
    var hoveredObject: FhysicsObject? = null

    /** The radius around the first polygon vertex where the polygon closes when clicked inside. */
    const val POLYGON_CLOSE_RADIUS = 10f

    /** The vertices of the polygon being created. */
    var polyVertices: MutableList<Vector2> = ArrayList()

    /** The object that is being pulled by the mouse. */
    private var pullObject: FhysicsObject? = null

    /** The relative position of the object to the mouse when pulling started. */
    private var pulledRelativePos: Vector2 = Vector2.ZERO

    /** The angle of the object when pulling started. */
    private var pulledAtAngle = 0.0f

    /// region =====Custom event handlers=====
    private fun onLeftClick() {
        // Don't select object if a polygon is being created
        if (polyVertices.isEmpty()) {
            // Select object if hovered, otherwise spawn object
            selectedObject = hoveredObject
            if (selectedObject != null) {
                UIController.expandObjectPropertiesPane()
                return
            }
        }

        // Handle spawning
        when (Settings.spawnObjectType) {
            SpawnObjectType.CIRCLE, SpawnObjectType.RECTANGLE -> spawnPreview()
            SpawnObjectType.POLYGON -> handlePolygonCreation()
            else -> {}
        }
    }

    private fun onRightClick() {
        if (Settings.spawnObjectType == SpawnObjectType.POLYGON) {
            polyVertices.clear()
        }
    }

    private fun onLeftDragStart() {
        // Only pull when no spawn type is selected
        if (Settings.spawnObjectType != SpawnObjectType.NOTHING) return

        // Find the object under the mouse
        pullObject = QuadTree.query(mousePosWorld) ?: return
        if (pullObject!!.static) return

        // Save the relative position and angle of the object
        pulledRelativePos = mousePosWorld - pullObject!!.position
        pulledAtAngle = pullObject!!.angle
    }

    private fun onLeftDrag() {
        // Don't create a drag preview if the spawn object type is not a rectangle
        if (Settings.spawnObjectType != SpawnObjectType.RECTANGLE) return

        // Calculate the corners of the rectangle
        val minX: Float = min(leftPressedPosWorld.x, mousePosWorld.x)
        val maxX: Float = max(leftPressedPosWorld.x, mousePosWorld.x)
        val minY: Float = min(leftPressedPosWorld.y, mousePosWorld.y)
        val maxY: Float = max(leftPressedPosWorld.y, mousePosWorld.y)

        // Create the preview rectangle
        val size = Vector2(maxX - minX, maxY - minY)
        val pos: Vector2 = Vector2(minX, minY) + size / 2f

        val rect = Rectangle(pos, size.x, size.y)
        val color: Color = spawnPreview!!.color
        rect.color = Color(color.red, color.green, color.blue, 128)

        spawnPreview = rect
    }

    private fun onLeftDragEnd() {
        when (Settings.spawnObjectType) {
            SpawnObjectType.CIRCLE, SpawnObjectType.RECTANGLE -> spawnPreview()
            SpawnObjectType.POLYGON -> handlePolygonCreation()
            else -> {}
        }

        pullObject = null
    }

    private fun onRightDrag() {
        // Move the camera by the amount the mouse is away from the position where the right mouse button was pressed
        val deltaMousePos: Vector2 = rightPressedPosWorld - mousePosWorld
        render.targetZoomCenter = render.targetZoomCenter + deltaMousePos
        render.zoomCenter = render.targetZoomCenter
    }

    private fun onScroll(direction: Double) {
        // Zoom in or out based on the scroll direction
        val zoomFactor: Float = if (direction > 0) 1.1f else 1 / 1.1f
        val newTargetZoom: Double = render.targetZoom * zoomFactor

        val minZoom: Double = 200.0 / max(FhysicsCore.border.width, FhysicsCore.border.height)
        val maxZoom: Double = max(FhysicsCore.border.width, FhysicsCore.border.height) * 2.0

        // If the zoom is already at min/max return to not change the zoom center
        if (newTargetZoom !in minZoom..maxZoom // New zoom is not in bounds
            && (render.zoom - minZoom < EPSILON || maxZoom - render.zoom < EPSILON)
        ) // Zoom is already at min/max
            return

        // Clamp the zoom level
        render.targetZoom = newTargetZoom.coerceIn(minZoom, maxZoom)

        // Calculate the difference between the mouse position and the current zoom center
        val deltaMousePos: Vector2 = mousePosWorld - render.targetZoomCenter

        // Adjust the target zoom center to zoom towards the mouse position
        render.targetZoomCenter = render.targetZoomCenter + deltaMousePos * (1 - 1 / zoomFactor)
    }
    /// endregion

    /// region =====Other methods=====
    /**
     * Spawns the preview object at the mouse position.
     */
    private fun spawnPreview() {
        // Check if spawn pos is outside the border
        if (!FhysicsCore.border.contains(mousePosWorld)) return

        val validParams: Boolean = when (Settings.spawnObjectType) {
            SpawnObjectType.CIRCLE -> Settings.spawnRadius > 0.0F
            SpawnObjectType.RECTANGLE -> Settings.spawnWidth > 0.0F && Settings.spawnHeight > 0.0F
            else -> false
        }

        if (!validParams) return

        FhysicsCore.spawn(spawnPreview!!.apply {
            color = asOpaqueColor(spawnPreview!!.color)
            static = Settings.spawnStatic
        })
        updateSpawnPreview()
    }

    /**
     * Handles the placement of the polygon vertices and creates the polygon if it's complete.
     */
    private fun handlePolygonCreation() {
        // Create the polygon if the polygon is complete
        if (polyVertices.size > 2 && PolygonFactory.isPolygonValid(polyVertices)) {
            val startPosScreen: Vector2 = polyVertices.first().toScreenSpace()

            if (mousePosScreen.sqrDistanceTo(startPosScreen) < POLYGON_CLOSE_RADIUS * POLYGON_CLOSE_RADIUS) {
                createAndSpawnPolygon()
                return
            }
        }

        // Add the vertex to the polygon
        polyVertices.add(mousePosWorld.copy())
    }

    private fun createAndSpawnPolygon() {
        // Create and spawn the polygon
        val polygon: Polygon = PolygonFactory.createPolygon(polyVertices.toTypedArray())
        FhysicsCore.spawn(polygon.apply {
            color = asOpaqueColor(polygon.color)
            static = Settings.spawnStatic
        })

        // Clear the polygon vertices
        polyVertices.clear()
    }

    /**
     * Updates the spawn preview object based on the current spawn object type and the input fields.
     */
    fun updateSpawnPreview() {
        if (Settings.spawnObjectType == SpawnObjectType.NOTHING) {
            spawnPreview = null
            return
        }

        val obj: FhysicsObject = when (Settings.spawnObjectType) {
            SpawnObjectType.CIRCLE -> Circle(mousePosWorld.copy(), Settings.spawnRadius)
            SpawnObjectType.RECTANGLE -> Rectangle(
                mousePosWorld.copy(),
                Settings.spawnWidth,
                Settings.spawnHeight
            )

            SpawnObjectType.POLYGON -> {
                val circle = Circle(mousePosWorld, Settings.spawnRadius)
                circle.color = Color.PINK
                circle
            }

            else -> throw IllegalArgumentException("Invalid spawn object type")
        }

        val color: Color = if (Settings.useCustomColor) Settings.spawnColor else obj.color
        obj.color = Color(color.red, color.green, color.blue, 128)
        spawnPreview = obj
    }

    /**
     * Returns an opaque version of the given [color].
     */
    private fun asOpaqueColor(color: Color): Color {
        return Color(color.red, color.green, color.blue)
    }

    /**
     * Pulls the object that is being pulled by the mouse.
     */
    fun pullObject() {
        val obj: FhysicsObject = pullObject ?: return

        // Calculate the pull force
        val pullPoint: Vector2 = obj.position + pulledRelativePos.rotated(obj.angle - pulledAtAngle)
        val pullForce: Vector2 = mousePosWorld - pullPoint
        pullForce *= obj.mass
        pullForce /= 50f

        // Apply the pull force
        obj.velocity += pullForce * obj.invMass
        obj.angularVelocity += pullForce.cross(obj.position - pullPoint) * obj.invInertia
    }

    /**
     * Sets the mouse position in world space and updates the spawn preview position based on the [InputEvent][e].
     */
    private fun updateMousePos(e: InputEvent) {
        when (e) {
            is MouseEvent -> mousePosScreen.set(getMouseScreenPos(e))
            is ScrollEvent -> mousePosScreen.set(Vector2(e.x.toFloat(), e.y.toFloat()))
        }
        mousePosWorld.set(mousePosScreen.toWorldSpace())

        // Update the spawn preview position if it's not set due to dragging a rectangle
        if (!(leftDragging && Settings.spawnObjectType == SpawnObjectType.RECTANGLE)) {
            spawnPreview?.position?.set(mousePosWorld)
        }
    }

    /**
     * Updates the state of the mouse buttons based on the [MouseEvent][e].
     */
    private fun updateMouseButtonState(e: MouseEvent) {
        leftPressed = e.isPrimaryButtonDown
        rightPressed = e.isSecondaryButtonDown
    }

    /**
     * Clears the selection of the hovered and selected object.
     */
    fun clearSelection() {
        hoveredObject = null
        selectedObject = null
    }
    /// endregion

    /// region =====Vanilla event handlers=====
    fun onMousePressed(e: MouseEvent) {
        // Set the pressed position if the specific button was pressed
        if (!leftPressed && e.isPrimaryButtonDown) {
            leftPressedPosScreen.set(getMouseScreenPos(e))
            leftPressedPosWorld.set(mousePosWorld.copy())
        }
        if (!rightPressed && e.isSecondaryButtonDown) {
            rightPressedPosScreen.set(getMouseScreenPos(e))
            rightPressedPosWorld.set(mousePosWorld.copy())
        }

        updateMouseButtonState(e)
    }

    fun onMouseReleased(e: MouseEvent) {
        if (leftPressed && !e.isPrimaryButtonDown) {
            // If the mouse wasn't moved too much, it's a click
            if ((mousePosScreen - leftPressedPosScreen).sqrMagnitude() <= MIN_DRAG_DISTANCE_SQR) {
                onLeftClick()
            } else {
                onLeftDragEnd()
                leftDragging = false
            }
        }
        if (rightPressed && !e.isSecondaryButtonDown) {
            // If the mouse wasn't moved too much, it's a click
            if ((mousePosScreen - rightPressedPosScreen).sqrMagnitude() <= MIN_DRAG_DISTANCE_SQR) {
                onRightClick()
            }
        }

        updateMouseButtonState(e)
    }

    fun onMouseMoved(e: MouseEvent) {
        updateMousePos(e)
    }

    fun onMouseDragged(e: MouseEvent) {
        if (leftPressed) {
            // If the mouse was moved enough, it's a drag
            if ((mousePosScreen - leftPressedPosScreen).sqrMagnitude() > MIN_DRAG_DISTANCE_SQR) {
                if (!leftDragging) {
                    onLeftDragStart()
                }

                leftDragging = true
                onLeftDrag()
            }
        }
        if (rightPressed) {
            // If the mouse was moved enough, it's a drag
            if ((mousePosScreen - rightPressedPosScreen).sqrMagnitude() > MIN_DRAG_DISTANCE_SQR) {
                onRightDrag()
            }
        }

        updateMousePos(e)
    }

    fun onMouseWheel(e: ScrollEvent) {
        updateMousePos(e)
        onScroll(e.deltaY)
    }

    fun onKeyPressed(event: KeyEvent) {
        when (event.code) {
            KeyCode.P -> FhysicsCore.running = !FhysicsCore.running
            KeyCode.ENTER ->
                if (!FhysicsCore.running) {
                    DebugRenderer.clearDebug()
                    FhysicsCore.update()
                }

            KeyCode.Q -> QuadTree.QTDebugHelper.printTree()
            KeyCode.Z -> render.resetZoom()
            KeyCode.H -> QuadTree.capacity -= 1
            KeyCode.J -> QuadTree.capacity -= 5
            KeyCode.K -> QuadTree.capacity += 5
            KeyCode.L -> QuadTree.capacity += 1
            KeyCode.S -> println(selectedObject)
            KeyCode.DELETE -> UIController.instance.onPropertyRemoveClicked()
            else -> {}
        }
    }

    /**
     * Returns the mouse position in screen space from the [MouseEvent][e].
     */
    private fun getMouseScreenPos(e: MouseEvent): Vector2 {
        return Vector2(e.x.toFloat(), e.y.toFloat())
    }
    /// endregion
}