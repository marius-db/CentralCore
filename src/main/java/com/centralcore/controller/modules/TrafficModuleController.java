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
 * controller for the traffic control module
 * controlador para el modulo de control de trafico
 *
 * TODO full implementation:
 * - load incidentes_trafico from db into the incidents table
 * - load vehiculos into the vehicles table
 * - filter by status and severity
 * - report incident dialog
 * - update incident status (open → in_progress → resolved)
 */
public class TrafficModuleController implements Initializable {

    @FXML private TextField         txtSearchIncident;
    @FXML private TextField         txtSearchVehicle;
    @FXML private ComboBox<?>       cmbStatus;
    @FXML private ComboBox<?>       cmbGravedad;

    @FXML private TableView<?>      tableIncidents;
    @FXML private TableColumn<?,?>  colTipo;
    @FXML private TableColumn<?,?>  colUbicacion;
    @FXML private TableColumn<?,?>  colGravedad;
    @FXML private TableColumn<?,?>  colEstado;
    @FXML private TableColumn<?,?>  colFechaHora;
    @FXML private TableColumn<?,?>  colActions;

    @FXML private TableView<?>      tableVehicles;
    @FXML private TableColumn<?,?>  colMatricula;
    @FXML private TableColumn<?,?>  colMarca;
    @FXML private TableColumn<?,?>  colModelo;
    @FXML private TableColumn<?,?>  colColor;
    @FXML private TableColumn<?,?>  colTipo2;
    @FXML private TableColumn<?,?>  colOwner;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //TODO: populate status and severity combo boxes / poblar combos de estado y gravedad
        //TODO: load incidents and vehicles from db / cargar incidentes y vehiculos desde la bd
        System.out.println("traffic module loaded / modulo de trafico cargado");
    }

    @FXML
    private void onReportClicked() {
        //TODO: open report incident dialog / abrir dialogo para reportar incidente
        System.out.println("report incident / reportar incidente");
    }

    /** navigates back to modules grid / navega de vuelta a la cuadricula de modulos */
    @FXML
    private void onBackClicked() {
        try {
            Node modulesView = FXMLLoader.load(
                getClass().getResource("/com/centralcore/fxml/Modules.fxml")
            );
            StackPane contentPane = (StackPane) txtSearchIncident.getScene().lookup("#contentPane");
            if (contentPane != null) {
                contentPane.getChildren().setAll(modulesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
