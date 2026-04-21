package com.centralcore.util;

import java.io.IOException;
import java.net.URL;

import com.centralcore.modules.Module;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

//punto unico de navegacion, todos los cambios de pantalla pasan por aqui
public class SceneManager {

    private static Stage stage;
    private static StackPane mainShellContentPane;
    private static javafx.scene.layout.VBox sidebar;

    private static final String FXML_PATH = "/com/centralcore/fxml/";
    private static final String CSS_PATH = "/com/centralcore/css/";

    private SceneManager() {}

    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void setMainShellContentPane(StackPane contentPane) {
        mainShellContentPane = contentPane;
    }

    public static void setSidebar(javafx.scene.layout.VBox sidebarPane) {
        sidebar = sidebarPane;
    }

    public static void showWelcome() {
        loadScene("Welcome.fxml", "welcome");
    }

    public static void showLogin() {
        loadScene("Login.fxml", "auth");
    }

    public static void showMainShell() {
        loadScene("MainShell.fxml", "main");
    }

    public static void showModule(Module module) {
        if (mainShellContentPane == null) {
            System.err.println("panel de contenido del shell principal no registrado");
            return;
        }

        //oculta el sidebar para dar pantalla completa al modulo
        if (sidebar != null) {
            sidebar.setManaged(false);
            sidebar.setVisible(false);
        }

        try {
            Parent moduleUI = module.getMainUI();

            HBox headerBar = createModuleHeader(module);

            VBox moduleWrapper = new VBox();
            moduleWrapper.setStyle("-fx-spacing: 0; -fx-padding: 0;");
            moduleWrapper.getChildren().add(headerBar);
            moduleWrapper.getChildren().add(moduleUI);

            VBox.setVgrow(moduleUI, Priority.ALWAYS);

            mainShellContentPane.getChildren().setAll(moduleWrapper);
            System.out.println("loaded module: " + module.getName());

        } catch (Exception e) {
            System.err.println("error al cargar ui del modulo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static HBox createModuleHeader(Module module) {
        HBox header = new HBox();
        header.setStyle(
                "-fx-padding: 5 15; " +
                        "-fx-background-color: #34495e; " +
                        "-fx-border-color: #2c3e50; " +
                        "-fx-border-width: 0 0 1 0; " +
                        "-fx-alignment: center-left; " +
                        "-fx-spacing: 10;"
        );
        header.setPrefHeight(24);

        Button btnBack = new Button("← Back");
        btnBack.setStyle(
                "-fx-font-size: 10; " +
                        "-fx-padding: 4 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #ecf0f1; " +
                        "-fx-background-color: #2c3e50; " +
                        "-fx-border-color: #1a252f; " +
                        "-fx-border-width: 1;"
        );
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(
                btnBack.getStyle().replace("#2c3e50", "#1a252f")
        ));
        btnBack.setOnMouseExited(e -> btnBack.setStyle(
                btnBack.getStyle().replace("#1a252f", "#2c3e50")
        ));
        btnBack.setOnAction(e -> showModulesView());

        Label lblModuleName = new Label(module.getName());
        lblModuleName.setStyle(
                "-fx-font-size: 13; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #ecf0f1;"
        );

        //dos espaciadores para centrar el titulo con el boton a la izquierda
        HBox spacerLeft = new HBox();
        HBox spacerRight = new HBox();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        header.getChildren().addAll(btnBack, spacerLeft, lblModuleName, spacerRight);

        return header;
    }

    private static void showModulesView() {
        if (sidebar != null) {
            sidebar.setManaged(true);
            sidebar.setVisible(true);
        }

        if (mainShellContentPane == null) {
            return;
        }

        try {
            URL fxmlUrl = SceneManager.class.getResource(FXML_PATH + "ModulesView.fxml");
            if (fxmlUrl != null) {
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent modulesView = loader.load();
                mainShellContentPane.getChildren().setAll(modulesView);
                System.out.println("returned to modules view");
            }
        } catch (IOException e) {
            System.err.println("error al cargar vista de modulos: " + e.getMessage());
        }
    }

    private static void loadScene(String fxmlFile, String cssName) {
        try {
            URL fxmlUrl = SceneManager.class.getResource(FXML_PATH + fxmlFile);

            if (fxmlUrl == null) {
                System.err.println("fxml no encontrado: " + FXML_PATH + fxmlFile);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            VBox sceneWithTitleBar = new VBox();
            sceneWithTitleBar.setStyle("-fx-spacing: 0; -fx-padding: 0;");

            CustomTitleBar titleBar = new CustomTitleBar(stage);
            sceneWithTitleBar.getChildren().add(titleBar);
            sceneWithTitleBar.getChildren().add(root);

            VBox.setVgrow(root, Priority.ALWAYS);

            Scene scene = new Scene(sceneWithTitleBar);

            URL globalCss = SceneManager.class.getResource(CSS_PATH + "global.css");
            if (globalCss != null) {
                scene.getStylesheets().add(globalCss.toExternalForm());
            }

            //css especifico de pantalla es opcional, no pasa nada si no existe
            URL specificCss = SceneManager.class.getResource(CSS_PATH + cssName + ".css");
            if (specificCss != null) {
                scene.getStylesheets().add(specificCss.toExternalForm());
            }

            stage.setScene(scene);

        } catch (IOException e) {
            System.err.println("error cargando escena: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Stage getStage() {
        return stage;
    }
}