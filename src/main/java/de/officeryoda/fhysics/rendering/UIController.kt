package de.officeryoda.fhysics.rendering

/**
 * Sample Skeleton for 'ui.fxml' Controller Class
 */

import javafx.event.ActionEvent
import javafx.fxml.FXML
import java.net.URL
import java.util.*

class UIController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private lateinit var resources: ResourceBundle

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private lateinit var location: URL

    @FXML
    fun onSelectBox(event: ActionEvent) {
        println("Box")
    }

    @FXML
    fun onSelectSphere(event: ActionEvent) {
        println("Sphere")
    }

    @FXML
    fun onSelectTriangle(event: ActionEvent) {
        println("Triangle")
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {

    }
}
