package de.officeryoda.fhysics.rendering;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class JavaFXTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Pane pane = new Pane();

        // Create a ball
        Circle ball = new Circle(2, Color.BLUE);
        ball.relocate(50, 50);

        // Set up the animation
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(16), event -> {
            // Update ball position
            ball.setLayoutX(ball.getLayoutX() + 5);
            ball.setLayoutY(ball.getLayoutY() + 5);

            // Bounce off the walls
            if (ball.getLayoutX() < 0 || ball.getLayoutX() > pane.getWidth() - 2 * ball.getRadius()) {
                ball.setLayoutX(Math.min(Math.max(ball.getLayoutX(), 0), pane.getWidth() - 2 * ball.getRadius()));
            }

            if (ball.getLayoutY() < 0 || ball.getLayoutY() > pane.getHeight() - 2 * ball.getRadius()) {
                ball.setLayoutY(Math.min(Math.max(ball.getLayoutY(), 0), pane.getHeight() - 2 * ball.getRadius()));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Add the ball to the pane
        pane.getChildren().add(ball);

        // Set up the scene
        Scene scene = new Scene(pane, 400, 300);

        // Set up the stage
        primaryStage.setTitle("Bouncing Ball");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
