package com.centralcore.util;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

//barra de titulo personalizada, reemplaza la nativa de windows que no pega con el tema
public class CustomTitleBar extends HBox {

    private final Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;

    private static final String WRAPPER_WINDOWED =
            "-fx-spacing: 0; -fx-padding: 0; " +
                    "-fx-background-color: #1e2738; " +
                    "-fx-border-color: #3d5270; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8;";

    private static final String WRAPPER_MAXIMIZED =
            "-fx-spacing: 0; -fx-padding: 0; " +
                    "-fx-background-color: #1e2738; " +
                    "-fx-border-color: transparent; " +
                    "-fx-border-radius: 0; " +
                    "-fx-background-radius: 0;";

    private static final String BAR_WINDOWED =
            "-fx-background-color: #2c3e50; " +
                    "-fx-padding: 0 12; " +
                    "-fx-spacing: 6; " +
                    "-fx-alignment: center-left; " +
                    "-fx-border-color: transparent transparent #34495e transparent; " +
                    "-fx-border-width: 0 0 1 0; " +
                    "-fx-background-radius: 8 8 0 0;";

    private static final String BAR_MAXIMIZED =
            "-fx-background-color: #2c3e50; " +
                    "-fx-padding: 0 12; " +
                    "-fx-spacing: 6; " +
                    "-fx-alignment: center-left; " +
                    "-fx-border-color: transparent transparent #34495e transparent; " +
                    "-fx-border-width: 0 0 1 0; " +
                    "-fx-background-radius: 0;";

    public CustomTitleBar(Stage stage) {
        this.stage = stage;
        setupUI();
    }

    private void setupUI() {
        setStyle(BAR_WINDOWED);
        //altura fija, los botones se centran dentro con alignment
        setPrefHeight(34);
        setMinHeight(34);
        setMaxHeight(34);
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblTitle = new Label("CentralCore");
        lblTitle.setStyle(
                "-fx-font-size: 11; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #ecf0f1;"
        );
        lblTitle.setMouseTransparent(true);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnMinimize = createWindowButton("\u2212", "#95a5a6");
        Button btnMaximize = createWindowButton("\u25a1", "#95a5a6");
        Button btnClose    = createWindowButton("\u2715", "#e74c3c");

        btnMinimize.setOnAction(e -> stage.setIconified(true));
        btnMaximize.setOnAction(e -> toggleMaximize());
        btnClose.setOnAction(e -> Platform.exit());

        //escuchar cambios de maximizado que vengan del sistema (snap de windows, tecla win+flecha)
        stage.maximizedProperty().addListener((obs, wasMax, isMax) -> {
            setStyle(isMax ? BAR_MAXIMIZED : BAR_WINDOWED);
            updateWrapper(isMax);
        });

        getChildren().addAll(lblTitle, spacer, btnMinimize, btnMaximize, btnClose);

        setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
            setCursor(Cursor.CLOSED_HAND);
        });
        setOnMouseReleased(e -> setCursor(Cursor.DEFAULT));
        setOnMouseDragged(e -> {
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            }
        });
        //doble click para maximizar o restaurar
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) toggleMaximize();
        });
    }

    private void toggleMaximize() {
        boolean willMax = !stage.isMaximized();
        stage.setMaximized(willMax);
        setStyle(willMax ? BAR_MAXIMIZED : BAR_WINDOWED);
        updateWrapper(willMax);
    }

    //actualiza el radio y clip del wrapper raiz según el estado de la ventana
    private void updateWrapper(boolean maximized) {
        VBox wrapper = SceneManager.getSceneWrapper();
        if (wrapper == null) return;
        wrapper.setStyle(maximized ? WRAPPER_MAXIMIZED : WRAPPER_WINDOWED);
        //quitar clip al maximizar para que ocupe toda la pantalla sin huecos en las esquinas
        SceneManager.applyRoundedClip(maximized ? 0 : 8);
    }

    private Button createWindowButton(String symbol, String color) {
        Button btn = new Button(symbol);
        //centrado explícito para evitar que el texto del boton quede desplazado
        btn.setAlignment(javafx.geometry.Pos.CENTER);
        btn.setStyle(buildBtnStyle(color, "transparent"));

        btn.setOnMouseEntered(e -> btn.setStyle(buildBtnStyle(color, "#3d5270")));
        btn.setOnMouseExited(e ->  btn.setStyle(buildBtnStyle(color, "transparent")));
        return btn;
    }

    private String buildBtnStyle(String textColor, String bgColor) {
        return  "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-background-color: " + bgColor + "; " +
                "-fx-border-color: #4a6080; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 3; " +
                "-fx-background-radius: 3; " +
                "-fx-pref-width: 30; " +
                "-fx-min-width: 30; " +
                "-fx-max-width: 30; " +
                "-fx-pref-height: 24; " +
                "-fx-min-height: 24; " +
                "-fx-max-height: 24; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-alignment: center;";
    }
}