package com.centralcore.controller.modules;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * controller for the citizen database module
 * controlador para el modulo de base de datos ciudadana
 *
 * TODO full implementation:
 * - load ciudadanos from db into the tableview
 * - wire up search to filter by name/dni/address
 * - add/edit dialogs
 * - delete with confirmation
 */
public class CitizenModuleController implements Initializable {

    @FXML private TextField         txtSearch;
    @FXML private TableView<?>      tableCitizens;
    @FXML private TableColumn<?,?>  colDni;
    @FXML private TableColumn<?,?>  colNombre;
    @FXML private TableColumn<?,?>  colApellidos;
    @FXML private TableColumn<?,?>  colFechaNac;
    @FXML private TableColumn<?,?>  colMunicipio;
    @FXML private TableColumn<?,?>  colTelefono;
    @FXML private TableColumn<?,?>  colActions;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //TODO: load citizens from db / cargar ciudadanos desde la bd
        System.out.println("citizen module loaded / modulo ciudadano cargado");
    }

    @FXML
    private void onSearchTyped() {
        //TODO: filter table by search text / filtrar tabla por texto de busqueda
    }

    @FXML
    private void onAddClicked() {
        //TODO: open add citizen dialog / abrir dialogo para agregar ciudadano
        System.out.println("add citizen / agregar ciudadano");
    }

    /** navigates back to the modules grid / navega de vuelta a la cuadricula de modulos */
    @FXML
    private void onBackClicked() {
        try {
            Node modulesView = FXMLLoader.load(
                getClass().getResource("/com/centralcore/fxml/Modules.fxml")
            );
            StackPane contentPane = (StackPane) txtSearch.getScene().lookup("#contentPane");
            if (contentPane != null) {
                contentPane.getChildren().setAll(modulesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
