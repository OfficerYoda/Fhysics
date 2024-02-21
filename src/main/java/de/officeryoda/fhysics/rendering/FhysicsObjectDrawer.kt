package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
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
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.stage.Stage
import java.awt.Color
import java.awt.MouseInfo
import java.awt.Point
import java.awt.geom.Rectangle2D
import java.util.*


class FhysicsObjectDrawer : Application() {

    // fhysics properties
    val fhysics: FhysicsCore = FhysicsCore()

    // rendering properties
    private lateinit var stage: Stage
    private lateinit var gc: GraphicsContext

    // default properties
    private var zoom: Double = -1.0
    private var quadTreeHighlightSize: Double = 20.0
    private val debugPoints: MutableList<Pair<Vector2, Color>> = ArrayList()

    /// window size properties
    private val width: Double get() = stage.scene.width
    private val height: Double get() = stage.scene.height // use scene height to prevent including the window's title bar
    private val titleBarHeight: Double = 39.0 // that's the default height of the window's title bar (in windows), I calculated it at a pont in time

    /// =====start functions=====

    init {
        INSTANCE = this
    }

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

        scene.setOnScroll { onMouseWheel(it.deltaY) }
        scene.setOnMousePressed { onMousePressed(Vector2(it.x, it.y)) }
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

        drawDebugPoints()
//        drawHighlightQuadTree()
//        drawQuadTree()
//        drawBorder()

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
        val pos: Vector2 = transformPosition(circle.position)
        val radius: Double = circle.radius * zoom
        val diameter: Double = 2 * radius
        gc.fillOval(
            pos.x - radius, pos.y - radius,
            diameter, diameter
        )
    }

    private fun drawBox(box: Box) {
        val pos: Vector2 = transformPosition(box.position)
        gc.fillRect(
            pos.x,
            pos.y - box.height * zoom,
            box.width * zoom,
            box.height * zoom
        )
    }

    private fun drawDebugPoints() {
        val pointSize = 6.0

        for (pair in debugPoints.toList()) {
            val pos = transformPosition(pair.first)
            setFillColor(pair.second)
            gc.fillOval(
                pos.x - pointSize / 2,
                pos.y - pointSize / 2,
                pointSize,
                pointSize
            )
        }

        debugPoints.clear()
    }

    private fun drawHighlightQuadTree() {
        // get the mouse position
        val mousePoint: Point = MouseInfo.getPointerInfo().location
        stage.scene.root.screenToLocal(mousePoint.x.toDouble(), mousePoint.y.toDouble())

        /// ==stuff in world space==

        // convert mouse position to world space
        val mousePos = Vector2(
            mousePoint.x.toDouble() - width / 2 - 52, // the 52 is a magic number to correct the position
            height - mousePoint.y.toDouble() + titleBarHeight + 27 // the 27 is a magic number to correct the position
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
        val screenRectX = (transformX(rectX) - quadTreeHighlightSize / 2)
        val screenRectY = (transformY(rectY) - quadTreeHighlightSize / 2)

        // draw the query rectangle on screen
        setStrokeColor(Color.BLUE)
        gc.strokeRect(screenRectX, screenRectY, quadTreeHighlightSize, quadTreeHighlightSize)
    }

    private fun drawQuadTree() {
        fhysics.quadTree.draw(::transformAndDrawRect)
    }

    private fun drawBorder() {
        transformAndDrawRect(FhysicsCore.BORDER)
    }

    private fun transformAndDrawRect(rect: Rectangle2D) {
        val x: Double = transformX(rect.x)
        val y: Double = transformY((rect.y + rect.height))
        val width: Double = rect.width * zoom
        val height: Double = rect.height * zoom

        setStrokeColor(Color.WHITE)
        gc.strokeRect(x, y, width, height)
    }

    private fun drawStats() {
        val mspu: Double = fhysics.getAverageUpdateTime() // Milliseconds per Update
        val mspuRounded: String = String.format(Locale.US, "%.2f", mspu)
        val fps: Double = 1000.0 / mspu
        val fpsRounded: String = String.format(Locale.US, "%.2f", fps)

        val fontSize: Double = (height / 30) // Adjust the divisor for the desired scaling

        val font = Font("Spline Sans", fontSize)
        gc.font = font
        setFillColor(Color.WHITE)

        val lineHeight: Double = font.size
        gc.fillText("MSPU: $mspuRounded", 5.0, lineHeight)
        gc.fillText("FPS: $fpsRounded", 5.0, 2 * lineHeight)
    }

    fun drawDebugPoint(point: Vector2, color: Color) {
        debugPoints.add(Pair(point, color))
    }

    fun drawDebugPoint(point: Vector2) {
        drawDebugPoint(point, Color.RED)
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
    private fun transformPosition(pos: Vector2): Vector2 {
        val newX: Double = transformX(pos.x)
        val newY: Double = transformY(pos.y)
        return Vector2(newX, newY)
    }

    private fun transformX(x: Double): Double {
        return x * zoom
    }

    private fun transformY(y: Double): Double {
        return height - (y * zoom)
    }

    /// =====window functions=====

    private fun setWindowSize() {
        // calculate the window size
        val border: Rectangle2D = FhysicsCore.BORDER
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
        val borderHeight: Double = FhysicsCore.BORDER.height
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

    private fun onMouseWheel(delta: Double) {
        zoom += delta * 0.01
        quadTreeHighlightSize += delta * 0.1
//        drawFrame()
    }

    private fun onMousePressed(mousePos: Vector2) {
        val transformedMousePos: Vector2 = mousePos
        transformedMousePos.y = height - transformedMousePos.y
        fhysics.fhysicsObjects.add(Circle(transformedMousePos / zoom, 1.0))
        drawFrame()
    }

    private fun keyPressed(event: KeyEvent) {
        // if pressed char is p toggle isRunning in FhysicsCore
        // if it is Enter or space call the update function
        when (event.code) {
            KeyCode.P -> fhysics.isRunning = !fhysics.isRunning
            KeyCode.SPACE -> fhysics.update()
            KeyCode.ENTER -> fhysics.update()
            else -> {}
        }
    }

    companion object {
        lateinit var INSTANCE: FhysicsObjectDrawer
    }
}

