package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.Rectangle
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
import kotlin.math.min

// can't be converted to object because it is a JavaFX Application
class FhysicsObjectDrawer : Application() {

    // rendering properties
    private lateinit var stage: Stage
    lateinit var gc: GraphicsContext
        private set

    // zoom properties
    var targetZoom: Double = -1.0
    var zoom: Double = targetZoom
    var targetZoomCenter: Vector2 = Vector2((BORDER.width / 2).toFloat(), (BORDER.height / 2).toFloat())
    var zoomCenter: Vector2 = targetZoomCenter

    // debug properties
    private val debugPoints: MutableList<Triple<Vector2, Color, Int>> = ArrayList()

    /// window size properties
    val width: Double get() = stage.scene.width
    val height: Double get() = stage.scene.height // use scene height to prevent including the window's title bar
    private val titleBarHeight: Double = 39.0 // that's the default height of the window's title bar (in windows)

    /// =====start functions=====
    fun launch() {
        launch(FhysicsObjectDrawer::class.java)
    }

    override fun start(stage: Stage) {
        // set the drawer in the RenderUtil
        RenderUtil.drawer = this

        this.stage = stage

        setWindowSize()

        val root = Group()
        val scene = Scene(root, stage.width, stage.height)
        stage.title = "Fhysics"
        stage.scene = scene
        stage.isResizable = false
        stage.scene.root.clip = null // to draw things which are partially outside the window

        val canvas = Canvas(width, height)
        val uiRoot = Accordion()
        root.children.add(canvas)
        root.children.add(uiRoot)

        loadUI(uiRoot)

        // set the background color
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
        // draw functions need to be run on the JavaFX Application Thread
        // to prevent exceptions (just stopping to update the visuals)
        object : AnimationTimer() {
            override fun handle(now: Long) {
                drawFrame()
            }
        }.start()
    }

    /// =====draw functions=====
    fun drawFrame() {
        lerpZoom()

        // clear the stage
        gc.clearRect(0.0, 0.0, width, height)

        drawAllObjects()
        drawBorder()

        if(UIController.drawSpawnPreview) drawSpawnPreview()
        if (UIController.drawQuadTree) drawQuadTree()
        if (UIController.drawBoundingBoxes) drawBoundingBoxes()


        drawDebugPoints()
        drawStats()
    }

    private fun lerpZoom() {
        // a value I think looks good
        val interpolation = 0.12F

        // lerp the zoom and zoomCenter
        zoom = lerp(zoom.toFloat(), targetZoom.toFloat(), interpolation).toDouble()
        zoomCenter = lerpV2(zoomCenter, targetZoomCenter, interpolation)
    }

    private fun drawAllObjects() {
        for (obj: FhysicsObject in FhysicsCore.fhysicsObjects.toList()) {
            drawObject(obj)
        }
    }

    private fun drawObject(obj: FhysicsObject) {
        setFillColor(obj.color)
        if (obj is Circle) {
            drawCircle(obj)
        } else if (obj is Rectangle) {
            drawRectangle(obj)
        }
    }

    private fun drawCircle(circle: Circle) {
        val pos: Vector2 = worldToScreen(circle.position)
        val radius: Double = circle.radius * zoom
        val diameter: Double = 2.0 * radius

        gc.fillOval(
            pos.x - radius,
            pos.y - radius,
            diameter,
            diameter
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

        // Restore the original state of the graphics context
        gc.restore()
    }

    private fun drawSpawnPreview() {
        // triangle temp for nothing selected to spawn
        if (UIController.spawnObjectType == SpawnObjectType.TRIANGLE) return

        // instantiate a temporary object
        val obj: FhysicsObject = if(UIController.spawnObjectType == SpawnObjectType.CIRCLE) {
            Circle(SceneListener.mouseWorldPos, UIController.spawnRadius)
        } else {
            Rectangle(SceneListener.mouseWorldPos, UIController.spawnWidth, UIController.spawnHeight)
        }
        // set the alpha value to 50%
        obj.color = Color(obj.color.red, obj.color.green, obj.color.blue, 128)

        drawObject(obj)
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

            // update the duration of the point
            // if the duration is reached remove the point
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

    private fun drawQuadTree() {
        FhysicsCore.quadTree.draw(::transformAndDrawQuadTreeCapacity)
    }

    private fun drawBoundingBoxes() {
        FhysicsCore.fhysicsObjects.toList().forEach { obj ->
            val (pos: Vector2, size) = when (obj) {
                is Rectangle -> Vector2(obj.minX, obj.minY) to Vector2(obj.maxX - obj.minX, obj.maxY - obj.minY)
                is Circle -> Vector2(
                    obj.position.x - obj.radius,
                    obj.position.y - obj.radius
                ) to Vector2(obj.radius * 2, obj.radius * 2)

                else -> return@forEach
            }

            setStrokeColor(Color.RED)
            gc.strokeRect(
                worldToScreenX(pos.x),
                worldToScreenY(pos.y + size.y),
                size.x * zoom,
                size.y * zoom
            )
        }
    }

    private fun transformAndDrawQuadTreeCapacity(rect: Rectangle2D, contentCount: Int) {
        val x: Double = worldToScreenX(rect.x)
        val y: Double = worldToScreenY(rect.y + rect.height)
        val width: Double = rect.width * zoom
        val height: Double = rect.height * zoom

        // draw Border
        setStrokeColor(Color.WHITE)
        gc.strokeRect(x, y, width, height)

        // only draw the fill if the option is enabled
        if (!UIController.drawQTNodeUtilization) return

        // draw transparent fill
        val quadTreeCapacity: Int = QuadTree.capacity
        setFillColor(Color(66, 164, 245, (contentCount.toFloat() / quadTreeCapacity * 192).toInt().coerceAtMost(255)))
        gc.fillRect(x, y, width, height)
        // write the amount of objects in the cell
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

        if (UIController.drawMSPU || UIController.drawUPS) { // check both because UPS is calculated from MSPU
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
            stats.add("Objects: ${FhysicsCore.objectCount}")
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
                // outline the text for better readability
                gc.strokeText(text, borderSpacing, height - i * lineHeight - borderSpacing)
            }

            gc.fillText(text, borderSpacing, height - i * lineHeight - borderSpacing)
        }
    }

    // =====debug functions=====
    fun addDebugPoint(point: Vector2, color: Color = Color.RED) {
        debugPoints.add(Triple(point.copy(), color, 0))
    }

    /// =====window size functions=====
    private fun setWindowSize() {
        // calculate the window size
        val border: Rectangle2D = BORDER
        val borderWidth: Double = border.width
        val borderHeight: Double = border.height

        // calculate the aspect ratio based on world space
        val ratio: Double = borderHeight / borderWidth
        val maxWidth = 1440.0
        val maxHeight = 960.0

        // calculate the window size
        var windowWidth: Double = maxWidth
        var windowHeight: Double = windowWidth * ratio

        // stretch the window horizontally if the window is too tall
        if (windowHeight > maxHeight) {
            windowHeight = maxHeight
            windowWidth = windowHeight / ratio
        }

        stage.width = windowWidth + 16 // the 16 is a magic number to correct the width
        stage.height = windowHeight + titleBarHeight
    }

    private fun calculateZoom(): Double {
        // normal zoom amount
        val borderHeight: Double = BORDER.height
        val windowHeight: Double = stage.height - titleBarHeight

        return windowHeight / borderHeight
    }

    /// =====utility functions=====
    fun resetZoom() {
        targetZoom = calculateZoom()
        zoom = targetZoom
        targetZoomCenter = Vector2((BORDER.width / 2).toFloat(), (BORDER.height / 2).toFloat())
        zoomCenter = targetZoomCenter
    }
}
