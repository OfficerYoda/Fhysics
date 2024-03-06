/**
 * Sample Skeleton for 'ui.fxml' Controller Class
 */

package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton

class UIController {

    /// =====Spawn Object=====
    @FXML
    private lateinit var txtSpawnRadius: TextField

    /// =====Gravity=====
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

    /// =====Time=====
    @FXML
    private lateinit var btnPause: ToggleButton

    @FXML
    private lateinit var btnStep: Button

    @FXML
    private lateinit var txtTimeSpeed: TextField

    /// =====Debug=====
    @FXML
    private lateinit var cbQuadTree: CheckBox

    @FXML
    private lateinit var cbQTNodeUtilization: CheckBox

    @FXML
    private lateinit var cbMSPU: CheckBox

    @FXML
    private lateinit var cbUPS: CheckBox

    @FXML
    private lateinit var cbObjectCount: CheckBox

    @FXML
    private lateinit var cbQTCapacity: CheckBox

    /// =====Spawn Object=====
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

    /// =====Gravity=====
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

    /// =====Time=====
    @FXML
    fun onPauseClicked() {
        FhysicsCore.running = !btnPause.isSelected
        btnStep.isDisable = FhysicsCore.running
    }

    @FXML
    fun onStepClicked() {
        FhysicsCore.update()
    }

    @FXML
    fun onTimeSpeedTyped() {
        timeSpeed = parseTextField(txtTimeSpeed)
        FhysicsCore.dt = 1.0F / FhysicsCore.UPDATES_PER_SECOND * timeSpeed
    }

    /// =====Debug=====
    @FXML
    fun onQuadTreeClicked() {
        drawQuadTree = cbQuadTree.isSelected

        // node utilization is only drawn if the quad tree is drawn
        cbQTNodeUtilization.isDisable = !drawQuadTree
    }

    @FXML
    fun onQTNodeUtilizationClicked() {
        drawQTNodeUtilization = cbQTNodeUtilization.isSelected
    }

    @FXML
    fun onMSPUClicked() {
        drawMSPU = cbMSPU.isSelected
    }

    @FXML
    fun onUPSClicked() {
        drawUPS = cbUPS.isSelected
    }

    @FXML
    fun onObjectCountClicked() {
        drawObjectCount = cbObjectCount.isSelected
    }

    @FXML
    fun onQTCapacityClicked() {
        drawQTCapacity = cbQTCapacity.isSelected
    }

    /// =====Initialization and helper=====
    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {
        restrictToNumericInput(txtSpawnRadius, false)

        restrictToNumericInput(txtGravityDirectionX)
        restrictToNumericInput(txtGravityDirectionY)
        restrictToNumericInput(txtGravityPointX)
        restrictToNumericInput(txtGravityPointY)
        restrictToNumericInput(txtGravityPointStrength)

        restrictToNumericInput(txtTimeSpeed, false)

        /// =====Spawn Object=====
        txtSpawnRadius.text = spawnRadius.toString()

        /// =====Gravity=====
        txtGravityDirectionX.text = gravityDirection.x.toString()
        txtGravityDirectionY.text = gravityDirection.y.toString()
        txtGravityPointX.text = gravityPoint.x.toString()
        txtGravityPointY.text = gravityPoint.y.toString()
        txtGravityPointStrength.text = gravityPointStrength.toString()
        txtGravityDirectionX.isDisable = gravityType != GravityType.DIRECTIONAL
        txtGravityDirectionY.isDisable = gravityType != GravityType.DIRECTIONAL
        txtGravityPointX.isDisable = gravityType != GravityType.TOWARDS_POINT
        txtGravityPointY.isDisable = gravityType != GravityType.TOWARDS_POINT
        txtGravityPointStrength.isDisable = gravityType != GravityType.TOWARDS_POINT

        /// =====Time=====
        btnPause.isSelected = !FhysicsCore.running
        txtTimeSpeed.text = timeSpeed.toString()

        /// =====Debug=====
        cbQuadTree.isSelected = drawQuadTree
        cbQTNodeUtilization.isSelected = drawQTNodeUtilization
        cbQTNodeUtilization.isDisable = !drawQuadTree
        cbQTNodeUtilization.isSelected = drawQTNodeUtilization
        cbMSPU.isSelected = drawMSPU
        cbUPS.isSelected = drawUPS
        cbObjectCount.isSelected = drawObjectCount
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
        /// =====Spawn Object=====
        var spawnRadius: Float = 1.0F
            private set
        var spawnObjectType: SpawnObjectType = SpawnObjectType.CIRCLE
            private set

        /// =====Gravity=====
        var gravityType: GravityType = GravityType.TOWARDS_POINT
            private set
        val gravityDirection: Vector2 = Vector2(0.0F, 0.0F)
        val gravityPoint: Vector2 = Vector2( // the center of the world
            (FhysicsCore.BORDER.width / 2.0).toFloat(),
            (FhysicsCore.BORDER.height / 2.0).toFloat()
        )
        var gravityPointStrength: Float = 100.0F
            private set

        /// =====Time=====
        var timeSpeed: Float = 1.0F
            private set

        /// =====Debug=====
        var drawQuadTree: Boolean = true
            private set
        var drawQTNodeUtilization: Boolean = false
            private set
        var drawMSPU: Boolean = true
            private set
        var drawUPS: Boolean = false
            private set
        var drawObjectCount: Boolean = false
            private set
        var drawQTCapacity: Boolean = true
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