package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.datastructures.spatial.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.*
import de.officeryoda.fhysics.engine.objects.factories.PolygonFactory
import de.officeryoda.fhysics.engine.util.Stopwatch
import de.officeryoda.fhysics.rendering.RenderUtil.colorToPaint
import de.officeryoda.fhysics.rendering.RenderUtil.darkenColor
import de.officeryoda.fhysics.rendering.RenderUtil.lerp
import de.officeryoda.fhysics.rendering.RenderUtil.lerpV2
import de.officeryoda.fhysics.rendering.RenderUtil.setFillColor
import de.officeryoda.fhysics.rendering.RenderUtil.setStrokeColor
import de.officeryoda.fhysics.rendering.RenderUtil.worldToScreen
import de.officeryoda.fhysics.rendering.RenderUtil.worldToScreenX
import de.officeryoda.fhysics.rendering.RenderUtil.worldToScreenY
import de.officeryoda.fhysics.rendering.SceneListener.POLYGON_CLOSE_RADIUS
import de.officeryoda.fhysics.rendering.SceneListener.hoveredObject
import de.officeryoda.fhysics.rendering.SceneListener.mousePosWorld
import de.officeryoda.fhysics.rendering.SceneListener.selectedObject
import de.officeryoda.fhysics.rendering.SceneListener.spawnPreview
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Accordion
import javafx.stage.Stage
import java.awt.Color
import java.lang.Math.toDegrees
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess

// Can't be converted to a singleton because JavaFX won't allow it
class FhysicsObjectDrawer : Application() {

    // Rendering properties
    private lateinit var stage: Stage
    lateinit var gc: GraphicsContext
        private set

    // Zoom properties
    /** The targeted zoom level */
    var targetZoom: Double = -1.0

    /** The current zoom level */
    var zoom: Double = targetZoom

    /** The targeted zoom center */
    var targetZoomCenter: Vector2 = Vector2((BORDER.width / 2), (BORDER.height / 2))

    /** The current zoom center */
    var zoomCenter: Vector2 = targetZoomCenter


    /** The width of the scene */
    val width: Double get() = stage.scene.width

    /** The height of the scene */
    val height: Double get() = stage.scene.height // Use scene height to prevent including the window's title bar

    /** The stopwatch used to measure the time it takes to draw a frame */
    val drawStopwatch = Stopwatch(20)

    /// region =====Start functions=====
    fun launch() {
        launch(FhysicsObjectDrawer::class.java)
    }

    override fun start(stage: Stage) {
        // Set the drawer in the RenderUtil
        RenderUtil.drawer = this
        DebugDrawer.drawer = this

        this.stage = stage

        setWindowSize()

        val root = Group()
        val scene = Scene(root, stage.width, stage.height)
        stage.title = "Fhysics"
        stage.scene = scene
        stage.isResizable = false
        stage.scene.root.clip = null // To draw things which are partially outside the window

        val canvas = Canvas(width, height)
        val uiRoot = Accordion()
        root.children.add(canvas)
        root.children.add(uiRoot)

        loadUI(uiRoot)

        // Set the background color
        stage.scene.fill = colorToPaint(Color.decode("#010409"))

        addListeners(scene)

        gc = canvas.graphicsContext2D
        targetZoom = calculateZoom()
        zoom = targetZoom

        gc.lineWidth = 2.0

        startAnimationTimer()

        // Set the onCloseRequest event handler to exit the application
        stage.onCloseRequest = EventHandler { exitProcess(0) }

        stage.show()
    }

    private fun loadUI(uiRoot: Accordion) {
        // Load FXML file
        val loader = FXMLLoader(javaClass.getResource("ui.fxml"))
        loader.setRoot(uiRoot) // Set the root explicitly

        // Load FXML content and add it to the provided uiRoot
        loader.load<Any>()
    }

    private fun addListeners(scene: Scene) {
        scene.setOnScroll { SceneListener.onMouseWheel(it) }
        scene.setOnMousePressed { SceneListener.onMousePressed(it) }
        scene.setOnMouseReleased { SceneListener.onMouseReleased(it) }
        scene.setOnMouseMoved { SceneListener.onMouseMoved(it) }
        scene.setOnMouseDragged { SceneListener.onMouseDragged(it) }
        scene.setOnKeyPressed { SceneListener.onKeyPressed(it) }
    }

    private fun startAnimationTimer() {
        // Draw functions need to be run on the JavaFX Application Thread
        object : AnimationTimer() {
            override fun handle(now: Long) {
                // Don't update while rendering to prevent concurrentModificationExceptions
                FhysicsCore.RENDER_LOCK.lock()
                drawFrame()
                FhysicsCore.RENDER_LOCK.unlock()
            }
        }.start()
    }
    /// endregion

    /// region =====Draw functions=====
    fun drawFrame() {
        drawStopwatch.start()
        // Update the zoom
        lerpZoom()

        // Clear the stage
        gc.clearRect(0.0, 0.0, width, height)

        // Find the hovered object (if any)
        hoveredObject = checkForHoveredObject()

        // Draw the objects
        QuadTree.drawObjects(this)

        // Special draw cases
        if (hoveredObject != null) drawObjectPulsing(hoveredObject!!)
        if (selectedObject != null && selectedObject !== hoveredObject) drawObjectPulsing(selectedObject!!)

        drawSpawnPreview()
        drawBorder()

        DebugDrawer.drawDebug()
        drawStopwatch.stop()
    }

    private fun drawObjectPulsing(obj: FhysicsObject) {
        // Pulsing effect by changing the transparency value
        val alpha: Int = (191 + 64 * sin(PI * System.currentTimeMillis() / 500.0)).toInt()
        val c: Color = obj.color
        val color = Color(c.red, c.green, c.blue, alpha)

        setFillColor(color)
        when (obj) {
            is Circle -> drawCircleShape(obj)
            is Rectangle -> drawRectangleShape(obj)
            is Polygon -> drawPolygonShape(obj)
        }
    }

    /**
     * Sets the fill color and draws the [circle].
     */
    fun drawCircle(circle: Circle) {
        // Hovered and selected object will be drawn pulsing
        if (circle == hoveredObject || circle == selectedObject) return

        setFillColor(circle.color)
        drawCircleShape(circle)
    }

    /**
     * Sets the fill color and draws the [rectangle][rect].
     */
    fun drawRectangle(rect: Rectangle) {
        // Hovered and selected object will be drawn pulsing
        if (rect == hoveredObject || rect == selectedObject) return

        setFillColor(rect.color)
        drawRectangleShape(rect)
    }

    /**
     * Sets the fill color and draws the [polygon][poly].
     */
    fun drawPolygon(poly: Polygon) {
        // Hovered and selected object will be drawn pulsing
        if (poly == hoveredObject || poly == selectedObject) return

        setFillColor(poly.color)
        drawPolygonShape(poly)
    }

    /**
     * Draws the given [circle] with the current fill color.
     */
    private fun drawCircleShape(circle: Circle) {
        gc.lineWidth = 2.0 * zoom * 0.05

        val pos: Vector2 = worldToScreen(circle.position)
        val radius: Double = circle.radius * zoom

        gc.fillOval(
            pos.x - radius, pos.y - radius,
            2 * radius, 2 * radius
        )

        // Show rotation
        val end: Vector2 = circle.position + Vector2(cos(circle.angle), sin(circle.angle)) * circle.radius
        val endScreen: Vector2 = worldToScreen(end)

        // Darken the current fill color and use it as stroke color
        setStrokeColor(darkenColor(RenderUtil.paintToColor(gc.fill)))
        gc.strokeLine(pos.x.toDouble(), pos.y.toDouble(), endScreen.x.toDouble(), endScreen.y.toDouble())

        gc.lineWidth = 2.0
    }

    /**
     * Draws the given [rectangle][rect] with the current fill color.
     */
    private fun drawRectangleShape(rect: Rectangle) {
        val pos: Vector2 = worldToScreen(rect.position)

        // Save the current state of the graphics context
        gc.save()

        // Translate to the center of the rectangle
        gc.translate(pos.x.toDouble(), pos.y.toDouble())

        // Rotate around the center of the rectangle
        gc.rotate(-toDegrees(rect.angle.toDouble()))

        // Draw the rectangle
        gc.fillRect(
            -rect.width * zoom / 2,  // Adjust for the center
            -rect.height * zoom / 2, // Adjust for the center
            rect.width * zoom,
            rect.height * zoom
        )

        // Restore the original state of the graphics context due to the translation and rotation
        gc.restore()
    }

    /**
     * Draws the given [polygon][poly] with the current fill color.
     */
    private fun drawPolygonShape(poly: Polygon) {
        // Draw subPolygons if the option is enabled
        if (UIController.showSubPolygons && poly is ConcavePolygon) { // TODO Optimize to not use type checking
            for (subPoly: SubPolygon in poly.subPolygons) {
                setFillColor(subPoly.color)
                drawPolygon(subPoly)
            }
            return
        }

        val vertices: Array<Vector2> = poly.getTransformedVertices()

        val xPoints = DoubleArray(vertices.size)
        val yPoints = DoubleArray(vertices.size)

        for (i: Int in vertices.indices) {
            xPoints[i] = worldToScreenX(vertices[i].x)
            yPoints[i] = worldToScreenY(vertices[i].y)
        }

        gc.fillPolygon(xPoints, yPoints, vertices.size)
    }

    private fun drawSpawnPreview() {
        when (UIController.spawnObjectType) {
            SpawnObjectType.NOTHING -> return
            SpawnObjectType.POLYGON -> drawPolygonPreview()
            else -> spawnPreview!!.draw(this)
        }
    }

    private fun drawPolygonPreview() {
        val vertices: List<Vector2> = SceneListener.polyVertices.plus(mousePosWorld)
        if (vertices.isEmpty()) return

        // Draw lines between the vertices
        gc.beginPath()
        for (i: Int in vertices.indices) {
            val vertex: Vector2 = worldToScreen(vertices[i])
            gc.lineTo(vertex.x.toDouble(), vertex.y.toDouble())
        }

        if (PolygonFactory.isPolygonValid(vertices)) {
            // Show valid polygons with a white fill
            setFillColor(Color(255, 255, 255, 128))
        } else {
            // Show invalid polygons with a red fill
            setFillColor(Color(255, 0, 0, 128))
        }

        setStrokeColor(Color.WHITE)

        gc.stroke()
        gc.fill()

        // Draw a circle at the first vertex for easier closing
        setFillColor(Color(0, 255, 0, 128))
        val firstVertex: Vector2 = worldToScreen(vertices.first())
        val radius: Double = POLYGON_CLOSE_RADIUS.toDouble()
        gc.fillOval(
            firstVertex.x.toDouble() - radius,
            firstVertex.y.toDouble() - radius,
            2 * radius,
            2 * radius
        )
    }

    private fun drawBorder() {
        setStrokeColor(Color.GRAY)
        gc.strokeRect(worldToScreenX(0.0), worldToScreenY(BORDER.height), BORDER.width * zoom, BORDER.height * zoom)
    }
    /// endregion

    /// region =====Window size functions=====
    private fun setWindowSize() {
        // Calculate the window size
        val border: BoundingBox = BORDER
        val borderWidth: Float = border.width
        val borderHeight: Float = border.height

        // Calculate the aspect ratio based on world space
        val ratio: Float = borderHeight / borderWidth
        val maxWidth = 1440.0
        val maxHeight = 960.0

        // Calculate the window size
        var windowWidth: Double = maxWidth
        var windowHeight: Double = windowWidth * ratio

        // Stretch the window horizontally if the window is too tall
        if (windowHeight > maxHeight) {
            windowHeight = maxHeight
            windowWidth = windowHeight / ratio
        }

        stage.width = windowWidth + 16 // The 16 is a magic number to correct the width
        stage.height = windowHeight + TITLE_BAR_HEIGHT
    }

    private fun calculateZoom(): Double {
        // Normal zoom amount
        val borderHeight: Float = BORDER.height
        val windowHeight: Double = stage.height - TITLE_BAR_HEIGHT

        return windowHeight / borderHeight
    }
    /// endregion

    /// region =====Utility functions=====
    /**
     * Linearly interpolates the zoom level and zoom center.
     */
    private fun lerpZoom() {
        // A value I think looks good
        val interpolation = 0.12F

        // Lerp the zoom and zoomCenter
        zoom = lerp(zoom, targetZoom, interpolation.toDouble())
        zoomCenter = lerpV2(zoomCenter, targetZoomCenter, interpolation)
    }

    /**
     * Checks if the mouse is hovering over an object.
     *
     * @return The object the mouse is hovering over or `null` if no object is hovered.
     */
    private fun checkForHoveredObject(): FhysicsObject? {
        val pendingRemovals: MutableList<FhysicsObject> = QuadTree.pendingRemovals

        // Check if the mouse is still hovering over the object
        val obj: FhysicsObject? =
            if (hoveredObject != null &&
                hoveredObject!!.contains(mousePosWorld) &&
                !pendingRemovals.contains(hoveredObject)
            ) {
                hoveredObject
            } else {
                QuadTree.query(mousePosWorld)
            }

        // If the object is in the remove queue, don't return it
        return obj.takeUnless { pendingRemovals.contains(it) }
    }

    /**
     * Resets the zoom level and center.
     */
    fun resetZoom() {
        targetZoom = calculateZoom()
        zoom = targetZoom
        targetZoomCenter = Vector2((BORDER.width / 2), (BORDER.height / 2))
        zoomCenter = targetZoomCenter
    }
    /// endregion

    companion object {
        /** The height of the window's title bar */
        const val TITLE_BAR_HEIGHT: Double = 39.0
    }
}
