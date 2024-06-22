package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.BoundingBox
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.*
import de.officeryoda.fhysics.rendering.RenderUtil.colorToPaint
import de.officeryoda.fhysics.rendering.RenderUtil.darkenColor
import de.officeryoda.fhysics.rendering.RenderUtil.lerp
import de.officeryoda.fhysics.rendering.RenderUtil.lerpV2
import de.officeryoda.fhysics.rendering.RenderUtil.setFillColor
import de.officeryoda.fhysics.rendering.RenderUtil.setStrokeColor
import de.officeryoda.fhysics.rendering.RenderUtil.worldToScreen
import de.officeryoda.fhysics.rendering.RenderUtil.worldToScreenX
import de.officeryoda.fhysics.rendering.RenderUtil.worldToScreenY
import javafx.animation.AnimationTimer
import javafx.application.Application
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

// Can't be converted to object because it is a JavaFX Application
class FhysicsObjectDrawer : Application() {

    // Rendering properties
    private lateinit var stage: Stage
    lateinit var gc: GraphicsContext
        private set

    // Zoom properties
    var targetZoom: Double = -1.0
    var zoom: Double = targetZoom
    var targetZoomCenter: Vector2 = Vector2((BORDER.width / 2), (BORDER.height / 2))
    var zoomCenter: Vector2 = targetZoomCenter

    // Window size properties
    val width: Double get() = stage.scene.width
    val height: Double get() = stage.scene.height // Use scene height to prevent including the window's title bar

    // Object modification properties
    var spawnPreview: FhysicsObject? = null
    var hoveredObject: FhysicsObject? = null
    var selectedObject: FhysicsObject? = null

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
        // to prevent exceptions (just stopping to update the visuals)
        object : AnimationTimer() {
            override fun handle(now: Long) {
                FhysicsCore.RENDER_LOCK.lock()
                drawFrame()
                FhysicsCore.RENDER_LOCK.unlock()
            }
        }.start()
    }

    /// endregion

    /// region =====Draw functions=====
    fun drawFrame() {
        lerpZoom()

        // Clear the stage
        gc.clearRect(0.0, 0.0, width, height)

        // Find the hovered object (if any)
        hoveredObject = checkForHoveredObject()

        // Draw the objects
        QuadTree.root.drawObjects(this)

        if (hoveredObject != null) drawObjectPulsing(hoveredObject!!)
        if (selectedObject != null && selectedObject !== hoveredObject) drawObjectPulsing(selectedObject!!)
        if (UIController.drawSpawnPreview && hoveredObject == null) drawSpawnPreview()

        drawBorder()
        DebugDrawer.drawDebug()
    }

    fun drawObject(obj: FhysicsObject) {
        // Hovered and selected object will be drawn pulsing
        if (obj == hoveredObject || obj == selectedObject) return

        setFillColor(obj.color)

        // Draw Object
        when (obj) {
            is Circle -> drawCircle(obj)
            is Rectangle -> drawRectangle(obj)
            is Polygon -> drawPolygon(obj)
        }
    }

    private fun drawObjectPulsing(obj: FhysicsObject) {
        val alpha: Int = (191 + 64 * sin(PI * System.currentTimeMillis() / 500.0)).toInt()
        val c: Color = obj.color
        val color = Color(c.red, c.green, c.blue, alpha)
        setFillColor(color)

        when (obj) {
            is Circle -> drawCircle(obj)
            is Rectangle -> drawRectangle(obj)
            is Polygon -> drawPolygon(obj)
        }
    }

    private fun drawCircle(circle: Circle) {
        gc.lineWidth = 2.0 * zoom * 0.05

        val pos: Vector2 = worldToScreen(circle.position)
        val radius: Double = circle.radius * zoom

        gc.fillOval(
            pos.x - radius,
            pos.y - radius,
            2 * radius,
            2 * radius
        )

        // Show rotation
        val end: Vector2 = circle.position + Vector2(cos(circle.angle), sin(circle.angle)) * circle.radius
        val endScreen: Vector2 = worldToScreen(end)

        // Darken the current fill color and use it as stroke color
        setStrokeColor(darkenColor(RenderUtil.paintToColor(gc.fill)))
        gc.strokeLine(pos.x.toDouble(), pos.y.toDouble(), endScreen.x.toDouble(), endScreen.y.toDouble())

        gc.lineWidth = 2.0
    }

    private fun drawRectangle(rect: Rectangle) {
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

    private fun drawPolygon(poly: Polygon) {
        if (UIController.drawSubPolygons && poly is ConcavePolygon) {
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
        // Triangle temp for nothing selected to spawn
        when (UIController.spawnObjectType) {
            SpawnObjectType.NOTHING -> return
            SpawnObjectType.POLYGON -> drawPolygonPreview()
            else -> drawObject(spawnPreview!!)
        }
    }

    private fun drawPolygonPreview() {
        val vertices: List<Vector2> = SceneListener.polyVertices

        if (vertices.isEmpty()) return

        gc.beginPath()

        for (i: Int in vertices.indices) {
            val vertex: Vector2 = worldToScreen(vertices[i])
            gc.lineTo(vertex.x.toDouble(), vertex.y.toDouble())
        }

        val c: Color = Color.WHITE
        val transparentC = Color(c.red, c.green, c.blue, 128)

        setStrokeColor(c)
        setFillColor(transparentC)

        gc.stroke()
        if (!SceneListener.validPolygon)
            setFillColor(Color(255, 0, 0, 128))
        gc.fill()

        // Draw a circle at the first vertex for easier closing
        setFillColor(Color(0, 255, 0, 128))
        val firstVertex: Vector2 = worldToScreen(vertices.first())
        val radius: Double = SceneListener.POLYGON_CLOSE_RADIUS.toDouble() * zoom
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
    private fun lerpZoom() {
        // A value I think looks good
        val interpolation = 0.12F

        // Lerp the zoom and zoomCenter
        zoom = lerp(zoom, targetZoom, interpolation.toDouble())
        zoomCenter = lerpV2(zoomCenter, targetZoomCenter, interpolation)
    }

    private fun checkForHoveredObject(): FhysicsObject? {

        // Check if the mouse is still hovering over the object
        val obj: FhysicsObject? =
            this.hoveredObject?.takeIf { it.contains(SceneListener.mouseWorldPos) && !QuadTree.removeQueue.contains(it) }
                ?: QuadTree.root.query(SceneListener.mouseWorldPos)

        // If the object is in the remove queue, don't return it
        return obj.takeUnless { QuadTree.removeQueue.contains(it) }
    }

    fun resetZoom() {
        targetZoom = calculateZoom()
        zoom = targetZoom
        targetZoomCenter = Vector2((BORDER.width / 2), (BORDER.height / 2))
        zoomCenter = targetZoomCenter
    }

    /// endregion

    companion object {
        const val TITLE_BAR_HEIGHT: Double = 39.0 // That's the default height of the window's title bar
    }
}
