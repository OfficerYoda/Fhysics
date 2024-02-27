package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import java.awt.Color
import java.awt.MouseInfo
import java.awt.Point
import java.awt.geom.Rectangle2D
import java.util.*
import kotlin.math.exp
import kotlin.math.sign


class FhysicsObjectDrawer : Application() {

    // fhysics properties
    val fhysics: FhysicsCore = FhysicsCore

    // rendering properties
    private lateinit var stage: Stage
    private lateinit var gc: GraphicsContext

    // zoom properties
    private var zoom: Double = -1.0

    // in world space
    private var zoomCenter: Vector2 = Vector2((BORDER.width / 2).toFloat(), (BORDER.height / 2).toFloat())

    // quad tree properties
    private var quadTreeHighlightSize: Double = 20.0
    private val debugPoints: MutableList<Triple<Vector2, Color, Int>> = ArrayList()

    /// window size properties
    private val width: Double get() = stage.scene.width
    private val height: Double get() = stage.scene.height // use scene height to prevent including the window's title bar
    private val titleBarHeight: Double = 39.0 // that's the default height of the window's title bar (in windows)

    /// =====start functions=====

    override fun start(stage: Stage) {
        this.stage = stage

        setWindowSize()

        val root = Group()
        val scene = Scene(root, stage.width, stage.height)
        stage.title = "Fhysics"
        stage.scene = scene
        stage.isResizable = false
        stage.scene.root.clip = null // to draw things which are partially outside the window

        val canvas = Canvas(width, height)
        root.children.add(canvas)

        // set the background color
        stage.scene.fill = colorToPaint(Color.decode("#010409"))

        scene.setOnScroll { onMouseWheel(it) }
        scene.setOnMousePressed { onMousePressed(Vector2(it.x.toFloat(), it.y.toFloat())) }
        scene.setOnKeyPressed { keyPressed(it) }

        gc = canvas.graphicsContext2D

        zoom = calculateZoom()

        startAnimationTimer()

        stage.show()
    }

    fun launch() {
        launch(FhysicsObjectDrawer::class.java)
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
        // clear the stage
        gc.clearRect(0.0, 0.0, width, height)

        drawAllObjects()

        drawBorder()

        drawDebugPoints()
//        drawHighlightQuadTree()
//        drawQuadTree()

        drawStats()
    }

    private fun drawAllObjects() {
        for (obj in fhysics.fhysicsObjects.toList()) {
            drawObject(obj)
        }
    }

    private fun drawObject(obj: FhysicsObject) {
        setFillColor(obj.color)
        if (obj is Circle) {
            drawCircle(obj)
        } else if (obj is Box) {
            drawBox(obj)
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

    private fun drawBox(box: Box) {
        val pos: Vector2 = worldToScreen(box.position)
        gc.fillRect(
            pos.x.toDouble(),
            pos.y - box.height * zoom,
            box.width * zoom,
            box.height * zoom
        )
    }

    private fun drawDebugPoints() {
        val pointSize = 6.0

        val duration = 60 // The amount of Frames the point should be visible

        for (triple in debugPoints.toList()) {
            val pos = worldToScreen(triple.first)
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

    private fun drawHighlightQuadTree() {
        // get the mouse position
        val mousePoint: Point = MouseInfo.getPointerInfo().location
        stage.scene.root.screenToLocal(mousePoint.x.toDouble(), mousePoint.y.toDouble())

        /// ==stuff in world space==

        // convert mouse position to world space
        val mousePos = Vector2(
            (mousePoint.x - width / 2 - 52).toFloat(), // the 52 is a magic number to correct the position
            (height - mousePoint.y + titleBarHeight + 27).toFloat() // the 27 is a magic number to correct the position
        )

        // calculate the query rectangle's position
        val rectX: Double = mousePos.x / zoom
        val rectY: Double = mousePos.y / zoom


        // calculate the width and height of the query rectangle
        val queryWidth: Double = quadTreeHighlightSize / zoom
        val queryHeight: Double = quadTreeHighlightSize / zoom

        // create the query rectangle
        val queryRect =
            Rectangle2D.Double(
                rectX - queryWidth / 2,
                rectY - queryHeight / 2,
                queryWidth,
                queryHeight
            )

        // query the quad tree for objects in query rectangle
        val queriedObjects = fhysics.quadTree.query(queryRect).filterIsInstance<Circle>().toList()

        // draw the queried objects
        setFillColor(Color.BLUE)
        queriedObjects.forEach {
            drawCircle(it)
        }

        /// ==stuff in screen space==

        // calculate the position of the query rectangle
        val screenRectX: Double = worldToScreenX(rectX) - quadTreeHighlightSize / 2
        val screenRectY: Double = worldToScreenY(rectY) - quadTreeHighlightSize / 2

        // draw the query rectangle on screen
        setStrokeColor(Color.BLUE)
        gc.strokeRect(
            screenRectX,
            screenRectY,
            quadTreeHighlightSize,
            quadTreeHighlightSize
        )
    }

    private fun drawQuadTree() {
        fhysics.quadTree.draw(::transformAndDrawQuadTreeCapacity)
    }

    private fun transformAndDrawQuadTreeCapacity(rect: Rectangle2D, contentCount: Int) {
        val x: Double = worldToScreenX(rect.x)
        val y: Double = worldToScreenY(rect.y + rect.height)
        val width: Double = rect.width * zoom
        val height: Double = rect.height * zoom

        // draw Border
        setStrokeColor(Color.WHITE)
        gc.strokeRect(x, y, width, height)

        // draw transparent fill
        val quadTreeCapacity = fhysics.QUAD_TREE_CAPACITY
        setFillColor(Color(66, 164, 245, (contentCount.toFloat() / quadTreeCapacity * 192).toInt()))
        gc.fillRect(x, y, width, height)
        // write the amount of objects in the cell
        drawCenteredText(contentCount.toString(), Rectangle2D.Double(x, y, width, height))
    }

    private fun drawCenteredText(text: String, rect: Rectangle2D) {
        val fontSize: Double = (rect.height / 2) // Adjust the divisor for the desired scaling
        val font = Font("Spline Sans", fontSize)

        gc.font = font
        setFillColor(Color.WHITE)

        val textNode = Text(text)
        textNode.font = font

        val textWidth: Double = textNode.layoutBounds.width
        val textHeight: Double = textNode.layoutBounds.height

        val centerX: Double = rect.x + (rect.width - textWidth) / 2
        val centerY: Double = rect.y + (rect.height + textHeight / 2) / 2

        gc.fillText(text, centerX, centerY)
    }

    private fun drawStats() {
        val mspu: Float = fhysics.getAverageUpdateDuration() // Milliseconds per Update
        val mspuRounded: String = String.format(Locale.US, "%.2f", mspu)
        val fps: Double = 1000.0 / mspu
        val fpsRounded: String = String.format(Locale.US, "%.2f", fps)

        val fontSize: Double = height / 30.0 // Adjust the divisor for the desired scaling

        val font = Font("Spline Sans", fontSize)
        gc.font = font
        setFillColor(Color.WHITE)

        val lineHeight: Double = font.size
        gc.fillText("MSPU: $mspuRounded", 5.0, lineHeight)
        gc.fillText("FPS: $fpsRounded", 5.0, 2 * lineHeight)
        gc.fillText("Objects: ${FhysicsCore.objectCount}", 5.0, 3 * lineHeight)
    }

    // =====ui functions=====

    // =====debug functions=====

    fun addDebugPoint(point: Vector2, color: Color) {
        debugPoints.add(Triple(point.copy(), color, 0))
    }

    fun addDebugPoint(point: Vector2) {
        addDebugPoint(point, Color.RED)
    }

    /// =====transform functions=====

    /**
     * Transforms the position to make objects with y-pos 0
     * appear at the bottom of the window instead of at the top.
     * Takes into account the window's height, and zoom factor.
     *
     * @param pos the original position
     * @return the transformed position
     */
    private fun worldToScreen(pos: Vector2): Vector2 {
        val newX: Float = worldToScreenX(pos.x).toFloat()
        val newY: Float = worldToScreenY(pos.y).toFloat()
        return Vector2(newX, newY)
    }

    private fun worldToScreenX(x: Double): Double {
        return x * zoom - zoomCenter.x * zoom + width / 2
    }

    private fun worldToScreenX(x: Float): Double {
        return worldToScreenX(x.toDouble())
    }

    private fun worldToScreenY(y: Double): Double {
        return height - (y * zoom - zoomCenter.y * zoom + height / 2)
    }

    private fun worldToScreenY(y: Float): Double {
        return worldToScreenY(y.toDouble())
    }

    private fun screenToWorld(pos: Vector2): Vector2 {
        val newX: Float = screenToWorldX(pos.x).toFloat()
        val newY: Float = screenToWorldY(pos.y).toFloat()
        return Vector2(newX, newY)
    }

    private fun screenToWorldX(x: Double): Double {
        return (x - width / 2 + zoomCenter.x * zoom) / zoom
    }

    private fun screenToWorldX(x: Float): Double {
        return screenToWorldX(x.toDouble())
    }

    private fun screenToWorldY(y: Double): Double {
        return (height - y - height / 2 + zoomCenter.y * zoom) / zoom
    }

    private fun screenToWorldY(y: Float): Double {
        return screenToWorldY(y.toDouble())
    }

    /// =====window functions=====

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

    private fun setFillColor(color: Color) {
        gc.fill = colorToPaint(color)
    }

    private fun setStrokeColor(color: Color) {
        gc.stroke = colorToPaint(color)
    }

    private fun colorToPaint(javafxColor: Color): Paint {
        return javafx.scene.paint.Color(
            javafxColor.red / 255.0,
            javafxColor.green / 255.0,
            javafxColor.blue / 255.0,
            javafxColor.alpha / 255.0
        )
    }

    /// =====listener functions=====
    private fun onMouseWheel(e: ScrollEvent) {
        val deltaZoom: Double = exp(zoom * 0.02)

        // Record the mouse position  before zooming
        var mousePosBeforeZoom = Vector2(e.x.toFloat(), e.y.toFloat())
        mousePosBeforeZoom = screenToWorld(mousePosBeforeZoom)

        // Adjust the zoom amount
        zoom += deltaZoom * e.deltaY.sign
        zoom = zoom.coerceIn(0.5, 120.0)

        // Record the mouse position after zooming
        var mousePosAfterZoom = Vector2(e.x.toFloat(), e.y.toFloat())
        mousePosAfterZoom = screenToWorld(mousePosAfterZoom)

        // Calculate the difference in mouse position caused by zooming
        val deltaMousePos = mousePosBeforeZoom - mousePosAfterZoom

        // Update the zoom center to keep the mouse at the same world position
        zoomCenter = zoomCenter + deltaMousePos
    }

    private fun onMousePressed(mousePos: Vector2) {
        val transformedMousePos: Vector2 = screenToWorld(mousePos)
        fhysics.spawn(Circle(transformedMousePos, 1.0F))

        zoomCenter = transformedMousePos
        println(zoomCenter)
    }

    private fun keyPressed(event: KeyEvent) {
        // if pressed char is p toggle isRunning in FhysicsCore
        // if it is Enter or space call the update function
        when (event.code) {
            KeyCode.P -> fhysics.isRunning = !fhysics.isRunning
            KeyCode.SPACE -> fhysics.update()
            KeyCode.ENTER -> fhysics.update()
            KeyCode.Z -> resetZoom()
            else -> {}
        }
    }

    private fun resetZoom() {
        zoom = calculateZoom()
        zoomCenter = Vector2((BORDER.width / 2).toFloat(), (BORDER.height / 2).toFloat())
    }
}
