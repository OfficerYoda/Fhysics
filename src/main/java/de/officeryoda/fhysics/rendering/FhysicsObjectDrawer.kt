package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.BoundingBox
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.engine.objects.Rectangle
import de.officeryoda.fhysics.rendering.RenderUtil.colorToPaint
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
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.lang.Math.toDegrees
import java.util.*
import kotlin.math.PI
import kotlin.math.min
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

    // Debug properties
    private val debugPoints: MutableList<Triple<Vector2, Color, Int>> = ArrayList()

    // Window size properties
    val width: Double get() = stage.scene.width
    val height: Double get() = stage.scene.height // Use scene height to prevent including the window's title bar
    private val titleBarHeight: Double = 39.0 // That's the default height of the window's title bar (in windows)

    // Object modification properties
    var spawnPreview: FhysicsObject? = null
    var hoveredObject: FhysicsObject? = null
    var selectedObject: FhysicsObject? = null

    /// =====Start functions=====
    fun launch() {
        launch(FhysicsObjectDrawer::class.java)
    }

    override fun start(stage: Stage) {
        // Set the drawer in the RenderUtil
        RenderUtil.drawer = this

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

    /// =====Draw functions=====
    fun drawFrame() {
        lerpZoom()

        // Clear the stage
        gc.clearRect(0.0, 0.0, width, height)

        // Find the hovered object (if any)
        this.hoveredObject = checkForHoveredObject()

        // Draw the objects
        QuadTree.root.drawObjects(this)

        if (hoveredObject != null) drawObjectPulsing(hoveredObject!!)
        if (selectedObject != null && selectedObject !== hoveredObject) drawObjectPulsing(selectedObject!!)
        if (UIController.drawSpawnPreview && hoveredObject == null) drawSpawnPreview()
        if (UIController.drawQuadTree) QuadTree.root.drawNode(this)

        drawBorder()
        drawDebugPoints()
        drawStats()
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

    fun drawObject(obj: FhysicsObject) {
        // Hovered and selected object will be drawn pulsing
        if (obj === this.hoveredObject || obj === this.selectedObject) {
            return
        }

        setFillColor(obj.color)

        // Draw Object
        when (obj) {
            is Circle -> drawCircle(obj)
            is Rectangle -> drawRectangle(obj)
            is Polygon -> drawPolygon(obj)
        }
    }

    private fun drawCircle(circle: Circle) {
        val pos: Vector2 = worldToScreen(circle.position)
        val radius: Double = circle.radius * zoom

        gc.fillOval(
            pos.x - radius,
            pos.y - radius,
            2 * radius,
            2 * radius
        )
    }

    private fun drawRectangle(rect: Rectangle) {
        val pos: Vector2 = worldToScreen(rect.position)

        // Save the current state of the graphics context
        gc.save()

        // Translate to the center of the rectangle
        gc.translate(pos.x.toDouble(), pos.y.toDouble())

        // Rotate around the center of the rectangle
        gc.rotate(-toDegrees(rect.rotation.toDouble()))

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
        val vertices: List<Vector2> = poly.getTranslatedVertices()

        val center: Vector2 = poly.position
        val xPoints = DoubleArray(vertices.size)
        val yPoints = DoubleArray(vertices.size)

        for (i: Int in vertices.indices) {
            xPoints[i] = worldToScreenX(vertices[i].x + center.x)
            yPoints[i] = worldToScreenY(vertices[i].y + center.y)
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

    private fun drawDebugPoints() {
        val pointSize = 6.0

        val duration = 200 // The amount of Frames the point should be visible

        for (triple: Triple<Vector2, Color, Int> in debugPoints.toList()) {
            val pos: Vector2 = worldToScreen(triple.first)
            setFillColor(triple.second)
            gc.fillOval(
                pos.x - pointSize / 2,
                pos.y - pointSize / 2,
                pointSize,
                pointSize
            )

            // Update the duration of the point
            // If the max duration is reached remove the point
            if (triple.third < duration) {
                debugPoints[debugPoints.indexOf(triple)] = Triple(triple.first, triple.second, triple.third + 1)
            } else {
                debugPoints.remove(triple)
            }
        }
    }

    private fun drawBorder() {
        setStrokeColor(Color.GRAY)
        gc.strokeRect(worldToScreenX(0.0), worldToScreenY(BORDER.height), BORDER.width * zoom, BORDER.height * zoom)
    }

    fun drawBoundingBox(obj: FhysicsObject) {
        val boundingBox: BoundingBox = obj.boundingBox

        setStrokeColor(Color.RED)
        gc.strokeRect(
            worldToScreenX(boundingBox.x),
            worldToScreenY(boundingBox.y + boundingBox.height),
            boundingBox.width * zoom,
            boundingBox.height * zoom
        )
    }

    fun transformAndDrawQuadTreeNode(rect: BoundingBox, contentCount: Int) {
        val x: Double = worldToScreenX(rect.x)
        val y: Double = worldToScreenY(rect.y + rect.height)
        val width: Double = rect.width * zoom
        val height: Double = rect.height * zoom

        // Draw Border
        setStrokeColor(Color.WHITE)
        gc.strokeRect(x, y, width, height)

        // Only draw the fill if the option is enabled
        if (!UIController.drawQTNodeUtilization) return

        // Draw transparent fill
        val quadTreeCapacity: Int = QuadTree.capacity
        setFillColor(Color(66, 164, 245, (contentCount.toFloat() / quadTreeCapacity * 192).toInt().coerceAtMost(255)))
        gc.fillRect(x, y, width, height)
        // Write the amount of objects in the cell
        drawCenteredText(contentCount.toString(), Rectangle2D.Double(x, y, width, height))
    }

    private fun drawCenteredText(text: String, rect: Rectangle2D) {
        val fontSize: Double = (rect.height / 2) // Adjust the divisor for the desired scaling
        val font = Font("Spline Sans", fontSize)

        gc.font = font
        setFillColor(Color(255, 255, 255, 192))

        val textNode = Text(text)
        textNode.font = font

        val textWidth: Double = textNode.layoutBounds.width
        val textHeight: Double = textNode.layoutBounds.height

        val centerX: Double = rect.x + (rect.width - textWidth) / 2
        val centerY: Double = rect.y + (rect.height + textHeight / 2) / 2

        gc.fillText(text, centerX, centerY)
    }

    private fun drawStats() {
        val stats: ArrayList<String> = ArrayList()

        if (UIController.drawMSPU || UIController.drawUPS) { // Check both because UPS is calculated from MSPU
            val mspu: Double = FhysicsCore.updateTimer.average() // Milliseconds per Update
            val mspuRounded: String = String.format(Locale.US, "%.2f", mspu)

            if (UIController.drawMSPU) {
                stats.add("MSPU: $mspuRounded")
            }

            if (UIController.drawUPS) {
                val ups: Double = min(FhysicsCore.UPDATES_PER_SECOND.toDouble(), 1000.0 / mspu)
                val upsRounded: String = String.format(Locale.US, "%.2f", ups)
                stats.add("UPS: $upsRounded")
            }
        }

        if (UIController.drawObjectCount)
            stats.add("Objects: ${QuadTree.root.countUnique()}")

        if (UIController.drawQTCapacity)
            stats.add("QuadTree Capacity: ${QuadTree.capacity}")

        drawStatsList(stats)
    }

    private fun drawStatsList(stats: ArrayList<String>) {
        val fontSize: Double = height / 30.0 // Adjust the divisor for the desired scaling
        val font = Font("Spline Sans", fontSize)
        gc.font = font
        setFillColor(Color.WHITE)
        setStrokeColor(Color.BLACK)

        val lineHeight: Double = font.size
        val borderSpacing = 5.0

        for (i in 0 until stats.size) {
            val text: String = stats[i]

            if (UIController.drawQuadTree) {
                // Outline the text for better readability
                gc.strokeText(text, borderSpacing, height - i * lineHeight - borderSpacing)
            }

            gc.fillText(text, borderSpacing, height - i * lineHeight - borderSpacing)
        }
    }

    /// =====Debug functions=====
    fun addDebugPoint(point: Vector2, color: Color = Color.RED) {
        debugPoints.add(Triple(point.copy(), color, 0))
    }

    /// =====Window size functions=====
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
        stage.height = windowHeight + titleBarHeight
    }

    private fun calculateZoom(): Double {
        // Normal zoom amount
        val borderHeight: Float = BORDER.height
        val windowHeight: Double = stage.height - titleBarHeight

        return windowHeight / borderHeight
    }

    /// =====Utility functions=====
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
}
