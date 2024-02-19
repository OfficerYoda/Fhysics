package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.stage.Stage
import java.awt.Color
import java.awt.Insets
import java.awt.MouseInfo
import java.awt.Point
import java.awt.geom.Rectangle2D
import java.util.*


class FhysicsObjectDrawer : Application() {

    val fhysics: FhysicsCore = FhysicsCore()

    private lateinit var stage: Stage
    private lateinit var gc: GraphicsContext

    private var zoom: Double = 1.0 // TODO add a minus; leaving it out for now
    private var quadTreeHighlightSize: Double = 20.0
    private val debugPoints: MutableList<Pair<Vector2, Color>> = ArrayList()

    private val width: Double get() = stage.width
    private val height: Double get() = stage.height

    /// =====start functions=====

    override fun start(stage: Stage) {
        println("FhysicsFX")

        this.stage = stage

        setWindowSize(stage)
        zoom = calculateZoom(stage)

        val root = Group()
        val canvas = Canvas(width, height)
        root.children.add(canvas)

        val scene = Scene(root, width, height)
        stage.title = "Fhysics"
        stage.scene = scene
        stage.isResizable = false

        // set the background color
        stage.scene.fill = colorToPaint(Color.decode("#010409"))

        canvas.setOnScroll { onMouseWheel(it.deltaY) }
        canvas.setOnMousePressed { onMousePressed(Vector2(it.x, it.y)) }
        canvas.setOnKeyPressed { keyPressed(it.text[0]) }

        gc = canvas.graphicsContext2D

        fhysics.drawer = this
        fhysics.startUpdateLoop()

        stage.show()
    }

    fun launch() {
        launch(FhysicsObjectDrawer::class.java)
    }

    /// =====draw functions=====

    fun drawFrame() {
        // clear the stage
        gc.clearRect(0.0, 0.0, width, height)

        drawAllObjects()

        drawDebug()

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

    private fun drawDebug() {
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
        val mousePoint: Point = MouseInfo.getPointerInfo().location
        stage.scene.root.screenToLocal(mousePoint.x.toDouble(), mousePoint.y.toDouble())

        val mousePos: Vector2 = Vector2(mousePoint.x.toDouble(), height - mousePoint.y.toDouble()) / zoom

        var rectX: Double = mousePos.x - quadTreeHighlightSize / 2
        var rectY: Double = mousePos.y - quadTreeHighlightSize / 2

        var queryRect: Rectangle2D.Double =
            Rectangle2D.Double(rectX, rectY, quadTreeHighlightSize.toDouble(), quadTreeHighlightSize.toDouble())

        val currentCenterX: Double = queryRect.x + (queryRect.width / 2)
        val currentCenterY: Double = queryRect.y + (queryRect.height / 2)

        val newWidth: Double = queryRect.width / zoom
        val newHeight: Double = queryRect.height / zoom

        queryRect =
            Rectangle2D.Double(currentCenterX - (newWidth / 2), currentCenterY - (newHeight / 2), newWidth, newHeight)

        QuadTree.count = 0
        setFillColor(Color.BLUE)
        fhysics.quadTree.query(queryRect).filterIsInstance<Circle>().forEach {
            drawCircle(it)
        }

        mousePos *= zoom
//        mousePos.x -= insets.left // TODO check if this is necessary
//        mousePos.y += insets.top
        mousePos /= zoom

        rectX = (transformX(mousePos.x) - quadTreeHighlightSize / 2/* + insets.left*/)
        rectY = (transformY(mousePos.y) - quadTreeHighlightSize / 2/* + insets.top*/)

        gc.clearRect(rectX, rectY, quadTreeHighlightSize, quadTreeHighlightSize)
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

        setFillColor(Color.WHITE)
        gc.clearRect(x, y, width, height)
    }

    private fun drawStats() {
        val mspu: Double = fhysics.getAverageUpdateTime() // Milliseconds per Update
        val mspuRounded: String = String.format(Locale.US, "%.2f", mspu)
        val fps: Double = 1000.0 / mspu
        val fpsRounded: String = String.format(Locale.US, "%.2f", fps)

        val fontSize: Double = (height / 30) // Adjust the divisor for the desired scaling

        val font: Font = Font("Spline Sans", fontSize.toDouble())
        gc.font = font
        setFillColor(Color.WHITE)

        val lineHeight: Double = font.size
        gc.fillText("MSPU: $mspuRounded", 5.0, lineHeight)
        gc.fillText("FPS: $fpsRounded", 5.0, 2 * lineHeight)
    }

    /// =====end draw functions=====

    private fun setWindowSize(stage: Stage) {
        val insets = Insets(31, 8, 8, 8) // these will be the values
        val border: Rectangle2D = FhysicsCore.BORDER
        val borderWidth: Double = border.width
        val borderHeight: Double = border.height

        val ratio: Double = borderHeight / borderWidth
        val maxWidth = 1440.0
        val maxHeight = 960.0

        var windowWidth: Double = maxWidth
        var windowHeight: Double = (windowWidth * ratio + insets.top)

        if (windowHeight > maxHeight) {
            windowHeight = maxHeight
            windowWidth = ((windowHeight - insets.top) / ratio).toInt().toDouble()
        }

        stage.width = windowWidth
        stage.height = windowHeight
    }

    private fun calculateZoom(stage: Stage): Double {
        val border: Rectangle2D = FhysicsCore.BORDER
        val borderWidth: Double = border.width
        val windowWidth: Double = stage.width - (8 + 8) // -(insets.left[8] + insets.right[8])
        return windowWidth / borderWidth
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

    /// =====utility functions=====

    private fun setFillColor(color: Color) {
        gc.fill = colorToPaint(color)
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
//        TODO("Not yet implemented")
//        zoom -= delta * 0.2
//        drawFrame()
    }

    private fun onMousePressed(mousePos: Vector2) {
//        TODO("Not yet implemented")
//        val transformedMousePos = mousePos / zoom
//        fhysics.fhysicsObjects.add(Circle(transformedMousePos, 1.0))
//        drawFrame()
    }

    private fun keyPressed(pressedChar: Char) {
        // if pressed char is p toggle isRunning in FhysicsCore
        // if it is Enter or space call the update function
        when (pressedChar) {
            'p' -> fhysics.isRunning = !fhysics.isRunning
            ' ' -> fhysics.update() // space
            '\n' -> fhysics.update() // enter
        }
    }
}

