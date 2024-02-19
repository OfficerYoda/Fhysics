package de.officeryoda.fhysics.rendering

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.util.Duration
import kotlin.math.max
import kotlin.math.min

class JavaFXTest : Application() {
    override fun start(primaryStage: Stage) {
        val pane = Pane()

        // Create a ball
        val ball = Circle(2.0, Color.BLUE)
        ball.relocate(50.0, 50.0)

        // Set up the animation
        val timeline = Timeline(KeyFrame(Duration.millis(16.0), { event: ActionEvent? ->
            // Update ball position
            ball.layoutX = ball.layoutX + 5
            ball.layoutY = ball.layoutY + 5

            // Bounce off the walls
            if (ball.layoutX < 0 || ball.layoutX > pane.width - 2 * ball.radius) {
                ball.layoutX = min(max(ball.layoutX, 0.0), pane.width - 2 * ball.radius)
            }
            if (ball.layoutY < 0 || ball.layoutY > pane.height - 2 * ball.radius) {
                ball.layoutY = min(max(ball.layoutY, 0.0), pane.height - 2 * ball.radius)
            }
        }))
        timeline.cycleCount = Timeline.INDEFINITE
        timeline.play()

        // Add the ball to the pane
        pane.children.add(ball)

        // Set up the scene
        val scene = Scene(pane, 400.0, 300.0)

        // Set up the stage
        primaryStage.title = "Bouncing Ball"
        primaryStage.scene = scene
        primaryStage.show()
    }
}
