package com.centralcore.controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.modules.Module;
import com.centralcore.modules.ModuleManager;
import com.centralcore.util.SceneManager;
import com.centralcore.util.TranslationManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ModulesViewController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private TilePane tilePane;
    @FXML private javafx.scene.control.Label lblHeader;

    @Override public void initialize(URL url, ResourceBundle rb) {
        TranslationManager.addLanguageChangeListener(this);
        if (lblHeader != null) lblHeader.setText(TranslationManager.get("nav.modules"));
        setupTiles();
    }

    @Override
    public void onLanguageChanged(String newLanguageCode) {
        if (lblHeader != null) lblHeader.setText(TranslationManager.get("nav.modules"));
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

        //intentar cargar imagen del modulo, fallback a rectangulo coloreado si no existe
        //envuelto en StackPane para centrar el logo dentro del tile
        StackPane logoContainer = new StackPane(buildLogoNode(module, 150, 100));
        logoContainer.setAlignment(javafx.geometry.Pos.CENTER);
        logoContainer.setPrefWidth(Double.MAX_VALUE);

        Label nameLabel = new Label(module.getName());
        nameLabel.setStyle(
                "-fx-font-size: 14; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-alignment: center; " +
                        "-fx-wrap-text: true; " +
                        "-fx-text-fill: #ecf0f1;"
        );

        tile.getChildren().addAll(logoContainer, nameLabel);

        tile.setOnMouseClicked(e -> openModule(module));

        //el hover manipula el string de estilo porque javafx no soporta ":hover" en css inline
        tile.setOnMouseEntered(e -> tile.setStyle(
                tile.getStyle().replace("-fx-background-color: #34495e;", "-fx-background-color: #2c3e50;")
        ));
        tile.setOnMouseExited(e -> tile.setStyle(
                tile.getStyle().replace("-fx-background-color: #2c3e50;", "-fx-background-color: #34495e;")
        ));

        return tile;
    }

    //carga la imagen del módulo o devuelve un rectangulo de fallback
    static javafx.scene.Node buildLogoNode(Module module, double width, double height) {
        try {
            File logoFile = resolveLogoFile(module);
            if (logoFile != null && logoFile.exists()) {
                ImageView iv = new ImageView(new Image(logoFile.toURI().toString()));
                iv.setFitWidth(width);
                iv.setFitHeight(height);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception e) {
            System.err.println("error al cargar logo de " + module.getName() + ": " + e.getMessage());
        }

        //fallback: rectangulo azul si la imagen no esta disponible
        Rectangle fallback = new Rectangle(width, height);
        fallback.setFill(Color.web("#3498db"));
        fallback.setArcWidth(8);
        fallback.setArcHeight(8);
        return fallback;
    }

    //resuelve la ruta del logo del modulo en disco
    private static File resolveLogoFile(Module module) {
        String logoPath = module.getLogoPath();
        if (logoPath == null || logoPath.isBlank()) return null;

        //el modulo ya esta en la carpeta modules/<NombreModulo>/resources/<logoPath>
        //se busca via reflection en el classloader del modulo
        try {
            URL resource = module.getClass().getResource("/" + logoPath);
            if (resource != null) return new File(resource.toURI());
        } catch (Exception ignored) {}

        return null;
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