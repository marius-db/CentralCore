package com.centralcore.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

//stub - implementacion completa pendiente
public class LicencesController implements Initializable {

    @FXML private TableView<?>      tableLicences;
    @FXML private TableColumn<?, ?> colModule;
    @FXML private TableColumn<?, ?> colKey;
    @FXML private TableColumn<?, ?> colExpiry;
    @FXML private TableColumn<?, ?> colActive;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //TODO: cargar licencias desde la bd
    }

    @FXML
    private void onAddLicenceClicked() {
        //TODO: abrir dialogo para agregar licencia
        System.out.println("agregar licencia clicada");
    }
}