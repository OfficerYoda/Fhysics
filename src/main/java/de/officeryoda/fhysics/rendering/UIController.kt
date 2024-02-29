/**
 * Sample Skeleton for 'ui.fxml' Controller Class
 */

package de.officeryoda.fhysics.rendering

import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent

class UIController {

    @FXML
    private lateinit var txtSpawnRadius: TextField

    @FXML
    fun onRadiusTyped(event: KeyEvent) {
        spawnRadius = txtSpawnRadius.text.toFloat()
    }

    @FXML
    fun onCircleClicked(event: MouseEvent?) {
        spawnObjectType = SpawnObjectType.CIRCLE
    }

    fun onRectangleClicked(event: MouseEvent?) {
        spawnObjectType = SpawnObjectType.RECTANGLE
    }

    fun onTriangleClicked(event: MouseEvent?) {
        spawnObjectType = SpawnObjectType.TRIANGLE
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {
        restrictToNumericInput(txtSpawnRadius)

        txtSpawnRadius.text = spawnRadius.toString()
    }

    private fun restrictToNumericInput(textField: TextField) {
        textField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches("\\d*\\.?\\d*".toRegex())) {
                textField.text = oldValue
            }
        }
    }

    companion object {
        var spawnRadius: Float = 1.0F
            private set

        var spawnObjectType: SpawnObjectType = SpawnObjectType.CIRCLE
            private set
    }
}

enum class SpawnObjectType {
    CIRCLE,
    RECTANGLE,
    TRIANGLE
}