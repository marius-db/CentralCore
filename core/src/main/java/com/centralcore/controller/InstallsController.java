package com.centralcore.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.modules.Module;
import com.centralcore.modules.ModuleManager;
import com.centralcore.util.ModuleDetailsDialog;
import com.centralcore.util.SceneManager;

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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class InstallsController implements Initializable {

    @FXML
    private ListView<Module> moduleListView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupModuleList();
    }

    private void setupModuleList() {
        ModuleManager moduleManager = ModuleManager.getInstance();
        moduleListView.getItems().addAll(moduleManager.getAllModules());
        moduleListView.setCellFactory(param -> new ModuleListCell());
    }

    //celda personalizada para renderizar cada modulo en la lista
    private class ModuleListCell extends ListCell<Module> {

        private HBox container;
        private Label lblName;
        private Label lblDescription;
        private Button btnMenu;

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

            Rectangle logo = new Rectangle(50, 50);
            logo.setFill(Color.web("#3498db"));
            logo.setArcWidth(4);
            logo.setArcHeight(4);

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

            container.getChildren().addAll(logo, infoBox, btnMenu);
        }

        @Override
        protected void updateItem(Module module, boolean empty) {
            super.updateItem(module, empty);

            if (empty || module == null) {
                setGraphic(null);
            } else {
                lblName.setText(module.getName());
                lblDescription.setText(module.getDescription());

                //se recrea el menu en cada update para reflejar el modulo actual
                ContextMenu contextMenu = createContextMenu(module);
                btnMenu.setOnAction(e -> contextMenu.show(btnMenu, Side.BOTTOM, -60, 0));

                setGraphic(container);
            }
        }

        private ContextMenu createContextMenu(Module module) {
            ContextMenu menu = new ContextMenu();
            menu.setStyle(
                    "-fx-background-color: #34495e; " +
                            "-fx-border-color: #7f8c8d; " +
                            "-fx-border-width: 1;"
            );

            MenuItem itemViewDetails = new MenuItem("View Details");
            itemViewDetails.setStyle("-fx-padding: 6 12; -fx-font-size: 11;");
            itemViewDetails.setOnAction(e -> {
                ModuleDetailsDialog.show(SceneManager.getStage(), module);
            });

            MenuItem itemDelete = new MenuItem("Delete");
            itemDelete.setStyle("-fx-padding: 6 12; -fx-font-size: 11;");
            itemDelete.setOnAction(e -> {
                System.out.println("eliminar modulo: " + module.getName());
            });

            menu.getItems().addAll(itemViewDetails, itemDelete);
            return menu;
        }
    }
}