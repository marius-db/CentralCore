package com.centralcore.util;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

//barra de titulo personalizada, reemplaza la nativa de windows que no pega con el tema
public class CustomTitleBar extends HBox {

    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;

    public CustomTitleBar(Stage stage) {
        this.stage = stage;
        setupUI();
    }

    private void setupUI() {
        setStyle(
                "-fx-background-color: #2c3e50; " +
                        "-fx-padding: 4 12; " +
                        "-fx-spacing: 8; " +
                        "-fx-alignment: center-left; " +
                        "-fx-border-color: #34495e; " +
                        "-fx-border-width: 0 0 1 0;"
        );
        setPrefHeight(20);

        Label lblTitle = new Label("CentralCore");
        lblTitle.setStyle(
                "-fx-font-size: 11; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #ecf0f1;"
        );

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnMinimize = createWindowButton("−", "#7f8c8d");
        Button btnMaximize = createWindowButton("□", "#7f8c8d");
        Button btnClose = createWindowButton("✕", "#e74c3c");

        btnMinimize.setOnAction(e -> stage.setIconified(true));
        btnMaximize.setOnAction(e -> {
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                stage.setMaximized(true);
            }
        });
        btnClose.setOnAction(e -> Platform.exit());

        getChildren().addAll(lblTitle, spacer, btnMinimize, btnMaximize, btnClose);

        //guarda el offset al presionar para que el drag no salte al origen de la ventana
        setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
            setCursor(Cursor.CLOSED_HAND);
        });

        setOnMouseReleased(e -> setCursor(Cursor.DEFAULT));

        setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
    }

    private Button createWindowButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-font-size: 11; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-background-color: transparent; " +
                        "-fx-border-color: #7f8c8d; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 2; " +
                        "-fx-padding: 2 6; " +
                        "-fx-cursor: hand;"
        );

        //mismo truco de string replace que en los tiles, javafx no tiene :hover en inline
        btn.setOnMouseEntered(e -> btn.setStyle(
                btn.getStyle().replace("transparent", "#34495e")
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                btn.getStyle().replace("#34495e", "transparent")
        ));

        return btn;
    }
}