package com.centralcore.controller;

import com.centralcore.db.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * controller for the installs view
 * controlador para la vista de instalaciones
 *
 * stub - full implementation comes after shell is working
 * stub - implementacion completa viene despues de que el shell funcione
 */
class InstallsController implements Initializable {

    @FXML private TableView<?>  tableInstalls;
    @FXML private TableColumn<?, ?> colName;
    @FXML private TableColumn<?, ?> colVersion;
    @FXML private TableColumn<?, ?> colStatus;
    @FXML private TableColumn<?, ?> colActions;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //TODO: load installed modules from db / cargar modulos instalados desde la bd
    }

    @FXML
    private void onAddModuleClicked() {
        //TODO: open add module dialog / abrir dialogo para agregar modulo
        System.out.println("add module clicked / agregar modulo clicado");
    }
}
