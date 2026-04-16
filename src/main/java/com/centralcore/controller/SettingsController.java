package com.centralcore.controller;

import com.centralcore.db.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * controller for the settings view
 * controlador para la vista de configuracion
 */
public class SettingsController implements Initializable {

    @FXML private TextField     txtDbPath;
    @FXML private ToggleButton  toggleTheme;
    @FXML private Label         lblConnStatus;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // show the current db connection string (without password)
        // mostrar la cadena de conexion actual (sin contraseña)
        txtDbPath.setText("localhost:3306/centralcore");
    }

    @FXML
    private void onChangeDbClicked() {
        //TODO: allow editing db connection settings / permitir editar configuracion de conexion a bd
        System.out.println("change db clicked / cambiar bd clicado");
    }

    /**
     * tests the db connection and shows the result in the status label
     * prueba la conexion a bd y muestra el resultado en la etiqueta de estado
     */
    @FXML
    private void onTestConnectionClicked() {
        boolean ok = DatabaseConnection.testConnection();
        if (ok) {
            lblConnStatus.setText("✓ Connected successfully");
            lblConnStatus.getStyleClass().removeAll("status-error");
            lblConnStatus.getStyleClass().add("status-ok");
        } else {
            lblConnStatus.setText("✗ Connection failed - check host, port and credentials");
            lblConnStatus.getStyleClass().removeAll("status-ok");
            lblConnStatus.getStyleClass().add("status-error");
        }
    }
}
