package com.centralcore.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.modules.Module;
import com.centralcore.modules.ModuleManager;
import com.centralcore.util.ModuleDetailsDialog;
import com.centralcore.util.SceneManager;
import com.centralcore.util.TranslationManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class InstallsController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private ListView<Module> moduleListView;
    @FXML private Label lblTitle;
    @FXML private Button btnRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TranslationManager.addLanguageChangeListener(this);
        updateLabels();
        setupModuleList();
    }

    private void setupModuleList() {
        ModuleManager moduleManager = ModuleManager.getInstance();
        moduleListView.getItems().addAll(moduleManager.getAllModules());
        moduleListView.setCellFactory(param -> new ModuleListCell());
    }

    @FXML
    private void onRefreshModules() {
        ModuleManager.getInstance().reloadAll();
        moduleListView.getItems().clear();
        moduleListView.getItems().addAll(ModuleManager.getInstance().getAllModules());
    }

    private void updateLabels() {
        if (lblTitle != null) lblTitle.setText(TranslationManager.get("installs.title"));
        if (btnRefresh != null) btnRefresh.setText(TranslationManager.get("installs.btn.reload"));
    }

    @Override
    public void onLanguageChanged(String newLanguageCode) {
        updateLabels();
        //refrescar la lista para que las celdas regeneren sus menus con el idioma nuevo
        moduleListView.refresh();
    }

    //celda personalizada para renderizar cada módulo en la lista
    private class ModuleListCell extends ListCell<Module> {

        private HBox container;
        private Label lblName;
        private Label lblDescription;
        private Button btnMenu;
        private StackPane logoPlaceholder;

        public ModuleListCell() {
            setupCell();
        }

        private void setupCell() {
            container = new HBox();
            container.setStyle(
                    "-fx-padding: 10; " +
                            "-fx-spacing: 12; " +
                            "-fx-border-color: #34495e; " +
                            "-fx-border-width: 0 0 1 0; " +
                            "-fx-background-color: #2c3e50;"
            );
            container.setMinHeight(70);

            //logo placeholder, se sobreescribe en updateItem con la imagen real
            logoPlaceholder = new StackPane();
            logoPlaceholder.setPrefSize(50, 50);
            logoPlaceholder.setMinSize(50, 50);
            logoPlaceholder.setMaxSize(50, 50);

            VBox infoBox = new VBox();
            infoBox.setStyle("-fx-spacing: 5;");
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            lblName = new Label();
            lblName.setStyle(
                    "-fx-font-size: 13; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #ecf0f1;"
            );

            lblDescription = new Label();
            lblDescription.setStyle(
                    "-fx-font-size: 11; " +
                            "-fx-text-fill: #bdc3c7; " +
                            "-fx-wrap-text: true;"
            );
            lblDescription.setWrapText(true);

            infoBox.getChildren().addAll(lblName, lblDescription);

            btnMenu = new Button("⋯");
            btnMenu.setStyle(
                    "-fx-font-size: 14; " +
                            "-fx-padding: 4 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-background-color: #34495e; " +
                            "-fx-text-fill: #ecf0f1; " +
                            "-fx-border-color: #7f8c8d; " +
                            "-fx-border-width: 1;"
            );

            container.getChildren().addAll(logoPlaceholder, infoBox, btnMenu);
        }

        @Override
        protected void updateItem(Module module, boolean empty) {
            super.updateItem(module, empty);

            if (empty || module == null) {
                setGraphic(null);
            } else {
                lblName.setText(module.getName());
                lblDescription.setText(module.getDescription());

                //actualizar el logo con la imagen del módulo
                logoPlaceholder.getChildren().setAll(
                        ModulesViewController.buildLogoNode(module, 50, 50)
                );

                //se recrea el menu en cada update para reflejar el módulo actual y el idioma actual
                ContextMenu contextMenu = createContextMenu(module);
                btnMenu.setOnAction(e -> contextMenu.show(btnMenu, Side.BOTTOM, -60, 0));

                setGraphic(container);
            }
        }

        private ContextMenu createContextMenu(Module module) {
            ContextMenu menu = new ContextMenu();

            MenuItem itemViewDetails = new MenuItem(TranslationManager.get("installs.menu.viewDetails"));
            itemViewDetails.setOnAction(e -> ModuleDetailsDialog.show(SceneManager.getStage(), module));

            menu.getItems().addAll(itemViewDetails);
            return menu;
        }
    }
}