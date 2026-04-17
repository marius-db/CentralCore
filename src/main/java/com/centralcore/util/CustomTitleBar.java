package com.centralcore.util;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/**
 * custom window title bar to replace the ugly windows default
 * barra de titulo de ventana personalizada para reemplazar la desagradable barra por defecto de windows
 *
 * draggable, includes window controls (minimize, maximize, close)
 * arrastrable, incluye controles de ventana (minimizar, maximizar, cerrar)
 */
public class CustomTitleBar extends HBox {

    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;

    public CustomTitleBar(Stage stage) {
        this.stage = stage;
        setupUI();
    }

    private void setupUI() {
        //style the bar / estiliza la barra
        setStyle(
            "-fx-background-color: #2c3e50; " +
            "-fx-padding: 4 12; " +
            "-fx-spacing: 8; " +
            "-fx-alignment: center-left; " +
            "-fx-border-color: #34495e; " +
            "-fx-border-width: 0 0 1 0;"
        );
        setPrefHeight(20);

        //app title / titulo de la app
        Label lblTitle = new Label("CentralCore");
        lblTitle.setStyle(
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #ecf0f1;"
        );

        //spacer to push buttons to the right / espaciador para empujar botones a la derecha
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        //window control buttons / botones de control de ventana
        Button btnMinimize = createWindowButton("−", "#7f8c8d");
        Button btnMaximize = createWindowButton("□", "#7f8c8d");
        Button btnClose = createWindowButton("✕", "#e74c3c");

        //button actions / acciones de botones
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

        //make title bar draggable / hace la barra de titulo arrastrable
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

    /**
     * creates a styled window control button
     * crea un boton de control de ventana estilizado
     *
     * @param text button label / etiqueta del boton
     * @param color button color / color del boton
     * @return the styled button / el boton estilizado
     */
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

        btn.setOnMouseEntered(e -> btn.setStyle(
            btn.getStyle().replace("transparent", "#34495e")
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            btn.getStyle().replace("#34495e", "transparent")
        ));

        return btn;
    }
}
