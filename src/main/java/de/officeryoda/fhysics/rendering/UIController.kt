/**
 * Sample Skeleton for 'ui.fxml' Controller Class
 */

package de.officeryoda.fhysics.rendering

import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.input.KeyEvent

class UIController {

    @FXML
    private lateinit var txtSpawnRadius: TextField

    @FXML
    fun onRadiusTyped(event: KeyEvent) {
        spawnRadius = txtSpawnRadius.text.toFloat()
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {
        assert(txtSpawnRadius != null) { "fx:id=\"txtSpawnRadius\" was not injected: check your FXML file 'ui.fxml'." }

        restrictToNumericInput(txtSpawnRadius)
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
    }
}

enum class SpawnObjectType {
    CIRCLE,
    RECTANGLE,
    TRIANGLE
}