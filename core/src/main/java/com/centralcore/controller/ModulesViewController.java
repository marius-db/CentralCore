package com.centralcore.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.modules.Module;
import com.centralcore.modules.ModuleManager;
import com.centralcore.util.SceneManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ModulesViewController implements Initializable {

    @FXML private TilePane tilePane;

    @Override public void initialize(URL url, ResourceBundle rb) {
        setupTiles();
    }

    private void setupTiles() {
        ModuleManager moduleManager = ModuleManager.getInstance();
        tilePane.getChildren().clear();

        for (Module module : moduleManager.getAllModules()) {
            tilePane.getChildren().add(createModuleTile(module));
        }
    }

    private VBox createModuleTile(Module module) {
        VBox tile = new VBox();
        tile.setStyle(
                "-fx-border-color: #34495e; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-color: #34495e; " +
                        "-fx-cursor: hand;"
        );
        tile.setSpacing(8);
        tile.setPadding(new Insets(15));
        tile.setPrefWidth(180);
        tile.setPrefHeight(180);
        tile.setStyle(
                tile.getStyle() +
                        " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 4, 0, 0, 2);"
        );

        Rectangle logo = new Rectangle(150, 100);
        logo.setFill(Color.web("#3498db"));
        logo.setArcWidth(8);
        logo.setArcHeight(8);

        Label nameLabel = new Label(module.getName());
        nameLabel.setStyle(
                "-fx-font-size: 14; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-alignment: center; " +
                        "-fx-wrap-text: true; " +
                        "-fx-text-fill: #ecf0f1;"
        );

        tile.getChildren().addAll(logo, nameLabel);

        tile.setOnMouseClicked(e -> openModule(module));

        //el hover manipula el string de estilo porque javafx no soporta :hover en css inline
        tile.setOnMouseEntered(e -> tile.setStyle(
                tile.getStyle().replace("-fx-background-color: #34495e;", "-fx-background-color: #2c3e50;")
        ));
        tile.setOnMouseExited(e -> tile.setStyle(
                tile.getStyle().replace("-fx-background-color: #2c3e50;", "-fx-background-color: #34495e;")
        ));

        return tile;
    }

    private void openModule(Module module) {
        try {
            System.out.println("opening module: " + module.getName());
            SceneManager.showModule(module);
        } catch (Exception e) {
            System.err.println("error al abrir modulo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}