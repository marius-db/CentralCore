package com.centralcore.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * controller for the licences view
 * controlador para la vista de licencias
 *
 * stub - full implementation comes after shell is working
 * stub - implementacion completa viene despues de que el shell funcione
 */
public class LicencesController implements Initializable {

    @FXML private TableView<?>      tableLicences;
    @FXML private TableColumn<?, ?> colModule;
    @FXML private TableColumn<?, ?> colKey;
    @FXML private TableColumn<?, ?> colExpiry;
    @FXML private TableColumn<?, ?> colActive;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //TODO: load licences from db / cargar licencias desde la bd
    }

    @FXML
    private void onAddLicenceClicked() {
        //TODO: open add licence dialog / abrir dialogo para agregar licencia
        System.out.println("add licence clicked / agregar licencia clicada");
    }
}
