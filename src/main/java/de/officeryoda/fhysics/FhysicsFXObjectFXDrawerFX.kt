//package de.officeryoda.fhysics
//
//import de.officeryoda.fhysics.engine.FhysicsCore
//import de.officeryoda.fhysics.engine.Vector2
//import de.officeryoda.fhysics.objects.Box
//import de.officeryoda.fhysics.objects.Circle
//import de.officeryoda.fhysics.objects.FhysicsObject
//import javafx.animation.AnimationTimer
//import javafx.scene.canvas.GraphicsContext
//import java.awt.Color
//
//class FhysicsFXObjectFXDrawerFX(
//    private val fhysics: FhysicsCore,
//    private val gc: GraphicsContext,
//) {
//
//    private var zoom = 1.0
//
//    init {
//        setWindowSize()
//
//        object : AnimationTimer() {
//            override fun handle(now: Long) {
//                drawFrame()
//            }
//        }.start()
//    }
//
//    fun drawFrame() {
//        drawAllObjects()
//        drawMSPU()
//    }
//
//    private fun setWindowSize() {
//        // Implement setWindowSize logic here
//    }
//
//    private fun drawAllObjects() {
//        gc.clearRect(0.0, 0.0, Main.WIDTH, Main.HEIGHT)
//        gc.fill = Main.OBJECT_COLOR
//
//        for (obj in fhysics.fhysicsObjects) {
//            drawObject(obj)
//        }
//    }
//
//    private fun drawObject(obj: FhysicsObject) {
//        gc.fill = obj.color
//        when (obj) {
//            is Circle -> drawCircle(obj)
//            is Box -> drawBox(obj)
//        }
//    }
//
//    private fun drawCircle(circle: Circle) {
//        val transformedPos = transformPosition(circle.position)
//        val radius = circle.radius * zoom
//        gc.fillOval(transformedPos.x - radius, transformedPos.y - radius, 2 * radius, 2 * radius)
//    }
//
//    private fun drawBox(box: Box) {
//        val transformedPos = transformPosition(box.position)
//        val width = box.width * zoom
//        val height = box.height * zoom
//        gc.fillRect(transformedPos.x, transformedPos.y - height, width, height)
//    }
//
//    private fun drawMSPU() {
//        val mspu = fhysics.getAverageUpdateTime() // Milliseconds per Update
//        val fps = 1000.0 / mspu
//
//        gc.fill = Color.WHITE
//        gc.fillText("MSPU: ${"%.2f".format(mspu)}", 5.0, 15.0)
//        gc.fillText("FPS: ${"%.2f".format(fps)}", 5.0, 30.0)
//    }
//
//    private fun transformPosition(pos: Vector2): Vector2 {
//        val newX = pos.x * zoom
//        val newY = Main.HEIGHT - (pos.y * zoom)
//        return Vector2(newX, newY)
//    }
//
//    fun onMouseWheel(delta: Double) {
//        zoom -= delta * 0.2
//        drawFrame()
//    }
//
//    fun onMousePressed(mousePos: Vector2) {
//        val transformedMousePos = mousePos / zoom
//        fhysics.fhysicsObjects.add(Circle(transformedMousePos, 1.0))
//        drawFrame()
//    }
//}