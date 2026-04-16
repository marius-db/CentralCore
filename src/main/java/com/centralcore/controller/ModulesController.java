package com.centralcore.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * controller for the modules view
 * controlador para la vista de modulos
 *
 * shows available licensed modules as clickable cards
 * muestra los modulos licenciados disponibles como tarjetas clicables
 *
 * currently hardcoded to the two demo modules: citizen db and traffic
 * actualmente codificado para los dos modulos de demo: bd ciudadana y trafico
 */
public class ModulesController implements Initializable {

    @FXML private FlowPane modulesGrid;

    //path for module fxml files / ruta para archivos fxml de modulos
    private static final String FXML_PATH = "/com/centralcore/fxml/";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //build the module cards / construir las tarjetas de modulos
        //this needs to be replaced with java reflect later
        addModuleCard(
            "Citizen Database",
            "Manage all city residents, personal data and records.",
            "modules/CitizenModule.fxml"
        );
        addModuleCard(
            "Traffic Control",
            "Monitor traffic infrastructure and manage incidents in real time.",
            "modules/TrafficModule.fxml"
        );
    }

    /**
     * creates a module card and adds it to the grid
     * crea una tarjeta de modulo y la agrega a la cuadricula
     *
     * @param title       module display name / nombre de visualizacion del modulo
     * @param description short description / descripcion corta
     * @param fxmlPath    fxml file to load when clicked / archivo fxml a cargar al hacer clic
     */
    private void addModuleCard(String title, String description, String fxmlPath) {
        VBox card = new VBox();
        card.getStyleClass().add("module-card");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("module-card-title");

        Label lblDesc = new Label(description);
        lblDesc.getStyleClass().add("module-card-desc");
        lblDesc.setWrapText(true);

        card.getChildren().addAll(lblTitle, lblDesc);

        //on click, load the module view into a new scene or the content pane
        //al hacer clic, cargar la vista del modulo en una nueva escena o el panel de contenido
        card.setOnMouseClicked(e -> loadModule(fxmlPath));

        modulesGrid.getChildren().add(card);
    }

    /**
     * loads a module fxml into the parent content pane
     * carga un fxml de modulo en el panel de contenido padre
     *
     * walks up the scene graph to find the shell's contentPane and replaces it
     * sube por el grafo de escena para encontrar el contentPane del shell y lo reemplaza
     */
    private void loadModule(String fxmlPath) {
        try {
            URL fxmlUrl = getClass().getResource(FXML_PATH + fxmlPath);

            if (fxmlUrl == null) {
                System.err.println("module fxml not found / fxml del modulo no encontrado: " + fxmlPath);
                return;
            }

            Node moduleView = FXMLLoader.load(fxmlUrl);

            //get the parent StackPane (contentPane in MainShell)
            //obtener el StackPane padre (contentPane en MainShell)
            StackPane contentPane = (StackPane) modulesGrid.getScene()
                .lookup("#contentPane");

            if (contentPane != null) {
                contentPane.getChildren().setAll(moduleView);
            }

        } catch (IOException e) {
            System.err.println("error loading module / error cargando modulo: " + fxmlPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}
