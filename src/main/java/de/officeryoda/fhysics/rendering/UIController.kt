/**
 * Sample Skeleton for 'ui.fxml' Controller Class
 */

package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import javafx.fxml.FXML
import javafx.scene.control.TextField

class UIController {

    /// =====Spawn Object fields=====
    @FXML
    private lateinit var txtSpawnRadius: TextField

    /// =====Gravity fields=====
    @FXML
    private lateinit var txtGravityDirectionX: TextField

    @FXML
    private lateinit var txtGravityDirectionY: TextField

    @FXML
    private lateinit var txtGravityPointX: TextField

    @FXML
    private lateinit var txtGravityPointY: TextField

    @FXML
    private lateinit var txtGravityPointStrength: TextField

    /// =====Spawn Object functions=====
    @FXML
    fun onCircleClicked() {
        spawnObjectType = SpawnObjectType.CIRCLE

        // enable radius field
        txtSpawnRadius.isDisable = false
    }

    fun onRectangleClicked() {
        spawnObjectType = SpawnObjectType.RECTANGLE

        // disable radius field
        txtSpawnRadius.isDisable = true
    }

    fun onTriangleClicked() {
        spawnObjectType = SpawnObjectType.TRIANGLE

        // disable radius field
        txtSpawnRadius.isDisable = true
    }

    @FXML
    fun onRadiusTyped() {
        spawnRadius = parseTextField(txtSpawnRadius)
    }

    /// =====Gravity functions=====

    @FXML
    fun onDirectionClicked() {
        gravityType = GravityType.DIRECTIONAL

        // enable direction fields
        txtGravityDirectionX.isDisable = false
        txtGravityDirectionY.isDisable = false

        // disable point fields
        txtGravityPointX.isDisable = true
        txtGravityPointY.isDisable = true
        txtGravityPointStrength.isDisable = true
    }

    @FXML
    fun onPointClicked() {
        gravityType = GravityType.TOWARDS_POINT

        // disable direction fields
        txtGravityDirectionX.isDisable = true
        txtGravityDirectionY.isDisable = true

        // enable point fields
        txtGravityPointX.isDisable = false
        txtGravityPointY.isDisable = false
        txtGravityPointStrength.isDisable = false
    }

    @FXML
    fun onDirectionXTyped() {
        gravityDirection.x = parseTextField(txtGravityDirectionX)
    }

    @FXML
    fun onDirectionYTyped() {
        gravityDirection.y = parseTextField(txtGravityDirectionY)
    }

    @FXML
    fun onPointXTyped() {
        gravityPoint.x = parseTextField(txtGravityPointX)
    }

    @FXML
    fun onPointYTyped() {
        gravityPoint.y = parseTextField(txtGravityPointY)
    }

    @FXML
    fun onStrengthTyped() {
        gravityPointStrength = parseTextField(txtGravityPointStrength)
    }

    /// =====Initialization and helper functions=====

    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {
        restrictToNumericInput(txtSpawnRadius, false)

        restrictToNumericInput(txtGravityDirectionX)
        restrictToNumericInput(txtGravityDirectionY)
        restrictToNumericInput(txtGravityPointX)
        restrictToNumericInput(txtGravityPointY)
        restrictToNumericInput(txtGravityPointStrength)

        txtSpawnRadius.text = spawnRadius.toString()

        txtGravityDirectionX.text = gravityDirection.x.toString()
        txtGravityDirectionY.text = gravityDirection.y.toString()
        txtGravityPointX.text = gravityPoint.x.toString()
        txtGravityPointY.text = gravityPoint.y.toString()
        txtGravityPointStrength.text = gravityPointStrength.toString()
    }

    private fun restrictToNumericInput(textField: TextField, allowNegatives: Boolean = true) {
        textField.textProperty().addListener { _, oldValue, newValue ->
            val regexPattern: String = if (allowNegatives) "-?\\d*\\.?\\d*" else "\\d*\\.?\\d*"
            if (!newValue.matches(regexPattern.toRegex())) {
                textField.text = oldValue
            }
        }
    }

    private fun parseTextField(textField: TextField): Float {
        return textField.text.toFloatOrNull() ?: 0.0F
    }

    companion object {
        /// =====Spawn Object fields=====
        var spawnRadius: Float = 1.0F
            private set
        var spawnObjectType: SpawnObjectType = SpawnObjectType.CIRCLE
            private set

        /// =====Gravity fields=====
        var gravityType: GravityType = GravityType.DIRECTIONAL
            private set
        val gravityDirection: Vector2 = Vector2(0.0F, 0.0F)
        val gravityPoint: Vector2 = Vector2( // the center of the world
            (FhysicsCore.BORDER.width / 2.0).toFloat(),
            (FhysicsCore.BORDER.height / 2.0).toFloat()
        )
        var gravityPointStrength: Float = 1.0F
            private set
    }
}

enum class SpawnObjectType {
    CIRCLE,
    RECTANGLE,
    TRIANGLE
}

enum class GravityType {
    DIRECTIONAL,
    TOWARDS_POINT
}