package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent

object BetterSceneListener {

    /**
     * The minimum distance the mouse has to be moved in world space to be registered as a drag
     */
    private const val MIN_DRAG_DISTANCE: Float = 2.0F

    /**
     * The position of the mouse in world space
     */
    val mouseWorldPos: Vector2 = Vector2.ZERO

    // The state of the mouse buttons
    private var leftPressed: Boolean = false
    private var rightPressed: Boolean = false

    /// region =====Custom event handlers=====
    private fun onLeftClick() {

    }

    private fun onRightClick() {

    }

    private fun onLeftDrag() {

    }

    private fun onRightDrag() {

    }

    /// endregion

    /// region =====Vanilla event handlers=====
    fun onMouseWheel(e: ScrollEvent) {
//        TODO("Not yet implemented")
    }

    fun onMousePressed(e: MouseEvent) {
        updateMouseButtonState(e)
    }

    fun onMouseReleased(e: MouseEvent) {
        updateMouseButtonState(e)
    }

    fun onMouseMoved(e: MouseEvent) {
        updateMouseWorldPos(e)
    }

    fun onMouseDragged(e: MouseEvent) {
        updateMouseWorldPos(e)
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

            KeyCode.Z -> RenderUtil.drawer.resetZoom()
            KeyCode.J -> QuadTree.capacity -= 5
            KeyCode.K -> QuadTree.capacity += 5
            KeyCode.G -> CapacityDiagram(FhysicsCore.qtCapacity)
            KeyCode.Q -> println(QuadTree.root.objects.forEach { println(it) })
            KeyCode.S -> println(SceneListener.selectedObject)
            else -> {}
        }
    }


    /**
     * Sets the mouse position in world space
     * @param e the mouse event
     * @return the mouse position in world space
     */
    private fun updateMouseWorldPos(e: MouseEvent) {
        mouseWorldPos.x = RenderUtil.screenToWorldX(e.x.toFloat()).toFloat()
        mouseWorldPos.y = RenderUtil.screenToWorldY(e.y.toFloat()).toFloat()
    }

    private fun updateMouseButtonState(e: MouseEvent) {
        leftPressed = e.isPrimaryButtonDown
        rightPressed = e.isSecondaryButtonDown
    }

    /// endregion
}