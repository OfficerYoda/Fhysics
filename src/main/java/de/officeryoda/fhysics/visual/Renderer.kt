package de.officeryoda.fhysics.visual

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.border
import de.officeryoda.fhysics.engine.Settings
import de.officeryoda.fhysics.engine.SpawnObjectType
import de.officeryoda.fhysics.engine.datastructures.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.*
import de.officeryoda.fhysics.engine.objects.factories.PolygonFactory
import de.officeryoda.fhysics.engine.util.Stopwatch
import de.officeryoda.fhysics.visual.RenderUtil.colorToPaint
import de.officeryoda.fhysics.visual.RenderUtil.darkenColor
import de.officeryoda.fhysics.visual.RenderUtil.lerp
import de.officeryoda.fhysics.visual.RenderUtil.lerpV2
import de.officeryoda.fhysics.visual.RenderUtil.setFillColor
import de.officeryoda.fhysics.visual.RenderUtil.setStrokeColor
import de.officeryoda.fhysics.visual.RenderUtil.toScreenSpace
import de.officeryoda.fhysics.visual.RenderUtil.toScreenSpaceX
import de.officeryoda.fhysics.visual.RenderUtil.toScreenSpaceY
import de.officeryoda.fhysics.visual.RenderUtil.toWorldSpace
import de.officeryoda.fhysics.visual.SceneListener.POLYGON_CLOSE_RADIUS
import de.officeryoda.fhysics.visual.SceneListener.hoveredObject
import de.officeryoda.fhysics.visual.SceneListener.mousePosWorld
import de.officeryoda.fhysics.visual.SceneListener.selectedObject
import de.officeryoda.fhysics.visual.SceneListener.spawnPreview
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

/**
 * The main rendering class.
 */
// Can't be converted to a singleton because JavaFX won't allow it
class Renderer : Application() {

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
    var targetZoomCenter: Vector2 = Vector2((border.width / 2), (border.height / 2))

    /** The current zoom center */
    var zoomCenter: Vector2 = targetZoomCenter

    /**
     * The area in world space which is visible on the screen.
     */
    var viewingFrustum: BoundingBox = border

    /** The width of the scene */
    val width: Double get() = stage.scene.width

    /** The height of the scene */
    val height: Double get() = stage.scene.height // Use scene height to prevent including the window's title bar

    /** The stopwatch used to measure the time it takes to draw a frame */
    val drawStopwatch = Stopwatch()

    init {
        RenderUtil.render = this
    }

    /// region =====Start functions=====
    fun launch() {
        launch(Renderer::class.java)
    }

    override fun start(stage: Stage) {
        // Set the renderer in the helper objects
        RenderUtil.render = this
        DebugRenderer.renderer = this

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

        UIController.updateUi()

        // Update the camera view
        lerpZoom()
        updateCameraFrustum()

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

        DebugRenderer.drawDebug()
        drawStopwatch.stop()
    }


    private fun drawObjectPulsing(obj: FhysicsObject) {
        // Pulsing effect by changing the transparency value
        val alpha: Int = (191 + 64 * sin(PI * System.currentTimeMillis() / 500.0)).toInt()
        val c: Color = obj.color
        val color = Color(c.red, c.green, c.blue, alpha)

        setFillColor(color)
        when (obj.type) {
            FhysicsObjectType.CIRCLE -> drawCircleShape(obj as Circle)
            FhysicsObjectType.RECTANGLE -> drawRectangleShape(obj as Rectangle)
            FhysicsObjectType.CONVEX_POLYGON, FhysicsObjectType.CONCAVE_POLYGON -> drawPolygonShape(obj as Polygon)
            FhysicsObjectType.SUB_POLYGON -> throw IllegalArgumentException("SubPolygons draw should not be called")
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

        val pos: Vector2 = circle.position.toScreenSpace()
        val radius: Double = circle.radius * zoom

        gc.fillOval(
            pos.x - radius, pos.y - radius,
            2 * radius, 2 * radius
        )

        // Show rotation
        val end: Vector2 = circle.position + Vector2(cos(circle.angle), sin(circle.angle)) * circle.radius
        val endScreen: Vector2 = end.toScreenSpace()

        // Darken the current fill color and use it as stroke color
        setStrokeColor(darkenColor(RenderUtil.paintToColor(gc.fill)))
        gc.strokeLine(pos.x.toDouble(), pos.y.toDouble(), endScreen.x.toDouble(), endScreen.y.toDouble())

        gc.lineWidth = 2.0
    }

    /**
     * Draws the given [rectangle][rect] with the current fill color.
     */
    private fun drawRectangleShape(rect: Rectangle) {
        val pos: Vector2 = rect.position.toScreenSpace()

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
        if (Settings.showSubPolygons && poly.type == FhysicsObjectType.CONCAVE_POLYGON) {
            for (subPoly: SubPolygon in (poly as ConcavePolygon).subPolygons) {
                setFillColor(subPoly.color)
                drawPolygon(subPoly)
            }
            return
        }

        val vertices: Array<Vector2> = poly.getTransformedVertices()

        val xPoints = DoubleArray(vertices.size)
        val yPoints = DoubleArray(vertices.size)

        for (i: Int in vertices.indices) {
            val screenVertex: Vector2 = vertices[i].toScreenSpace()
            xPoints[i] = screenVertex.x.toDouble()
            yPoints[i] = screenVertex.y.toDouble()
        }

        gc.fillPolygon(xPoints, yPoints, vertices.size)
    }

    private fun drawSpawnPreview() {
        when (Settings.spawnObjectType) {
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
            val vertex: Vector2 = vertices[i].toScreenSpace()
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
        val firstVertex: Vector2 = vertices.first().toScreenSpace()
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
        gc.strokeRect(
            0f.toScreenSpaceX().toDouble(),
            border.height.toScreenSpaceY().toDouble(),
            border.width * zoom,
            border.height * zoom
        )
    }
    /// endregion

    /// region =====Window size functions=====
    private fun setWindowSize() {
        // Calculate the window size
        val border: BoundingBox = border
        val borderWidth: Float = border.width
        val borderHeight: Float = border.height

        // Calculate the aspect ratio based on world space
        val ratio: Float = borderHeight / borderWidth
        val maxWidth = 1080.0
        val maxHeight = 720.0

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
        val borderHeight: Float = border.height
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
     * Resets the zoom level and center.
     */
    fun resetZoom() {
        targetZoom = calculateZoom()
        zoom = targetZoom
        targetZoomCenter = Vector2((border.width / 2), (border.height / 2))
        zoomCenter = targetZoomCenter
    }

    private fun updateCameraFrustum() {
        val min: Vector2 = Vector2(0f, height.toFloat()).toWorldSpace() // Bottom left screen corner
        val max: Vector2 = Vector2(width.toFloat(), 0f).toWorldSpace() // Top right screen corner

        viewingFrustum = BoundingBox(min.x, min.y, max.x - min.x, max.y - min.y)
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
    /// endregion

    companion object {
        /** The height of the window's title bar */
        const val TITLE_BAR_HEIGHT: Double = 39.0
    }
}
