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

/**
 * muestra todos los modulos cargados como una grilla de tiles clickeables
 *
 * crea dinamicamente tiles para cada modulo encontrado por ModuleManager
 * sin hardcoding de modulos especificos en ningun lado
 */
public class ModulesViewController implements Initializable {

    @FXML
    private TilePane tilePane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTiles();
    }

    /**
     * carga todos los modulos y crea un tile para cada uno
     */
    private void setupTiles() {
        ModuleManager moduleManager = ModuleManager.getInstance();

        //limpia tiles existentes
        tilePane.getChildren().clear();

        //obtiene todos los modulos cargados
        for (Module module : moduleManager.getAllModules()) {
            VBox tile = createModuleTile(module);
            tilePane.getChildren().add(tile);
        }
    }

    /**
     * crea un tile individual del modulo con nombre y handler de click
     */
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

        //rectangulo placeholder para logo
        Rectangle logo = new Rectangle(150, 100);
        logo.setFill(Color.web("#3498db"));
        logo.setArcWidth(8);
        logo.setArcHeight(8);

        //etiqueta del nombre del modulo
        Label nameLabel = new Label(module.getName());
        nameLabel.setStyle(
            "-fx-font-size: 14; " +
            "-fx-font-weight: bold; " +
            "-fx-text-alignment: center; " +
            "-fx-wrap-text: true; " +
            "-fx-text-fill: #ecf0f1;"
        );

        tile.getChildren().addAll(logo, nameLabel);

        //handler para abrir modulo al hacer click
        tile.setOnMouseClicked(e -> openModule(module));

        //efecto de hover
        tile.setOnMouseEntered(e -> tile.setStyle(
            tile.getStyle().replace("-fx-background-color: #34495e;", "-fx-background-color: #2c3e50;")
        ));
        tile.setOnMouseExited(e -> tile.setStyle(
            tile.getStyle().replace("-fx-background-color: #2c3e50;", "-fx-background-color: #34495e;")
        ));

        return tile;
    }

    /**
     * abre el modulo seleccionado en el shell principal
     */
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
