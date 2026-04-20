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

/**
 * gestor central de escenas/navegacion para toda la aplicacion
 *
 * todos los cambios de pantalla pasan por aqui - nunca cargar escenas directamente
 *
 * uso: SceneManager.showLogin();
 */
public class SceneManager {

    //el stage principal definido durante el inicio de la app
    private static Stage stage;

    //referencia al controlador del shell principal para carga dinamica de contenido
    private static StackPane mainShellContentPane;

    //referencia al sidebar para toggle de visibilidad al abrir modulos
    private static javafx.scene.layout.VBox sidebar;

    //prefijo de ruta para todos los archivos fxml
    private static final String FXML_PATH = "/com/centralcore/fxml/";

    //prefijo de ruta para todos los archivos css
    private static final String CSS_PATH = "/com/centralcore/css/";

    //constructor privado - clase utilitaria, sin instanciacion
    private SceneManager() {}

    /**
     * debe llamarse una vez durante el inicio de la app con el stage principal
     */
    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
    }

    /**
     * registra el panel de contenido del shell principal para carga dinamica de modulos
     * debe ser llamado por MainShellController durante la inicializacion
     */
    public static void setMainShellContentPane(StackPane contentPane) {
        mainShellContentPane = contentPane;
    }

    /**
     * registra el sidebar para toggle de visibilidad cuando modulos abren/cierran
     */
    public static void setSidebar(javafx.scene.layout.VBox sidebarPane) {
        sidebar = sidebarPane;
    }

    //metodos de navegacion

    /** muestra la pantalla de bienvenida */
    public static void showWelcome() {
        loadScene("Welcome.fxml", "welcome");
    }

    /** muestra la pantalla de inicio de sesion */
    public static void showLogin() {
        loadScene("Login.fxml", "auth");
    }

    /** muestra el shell principal (sidebar + area de contenido) */
    public static void showMainShell() {
        loadScene("MainShell.fxml", "main");
    }

    /**
     * carga la ui de un modulo en el panel de contenido del shell principal con una barra de cabecera
     * envuelve la ui del modulo para que llene todo el panel de contenido con boton volver
     * oculta el sidebar para enfoque total del modulo
     *
     * @param module el modulo a mostrar
     */
    public static void showModule(Module module) {
        if (mainShellContentPane == null) {
            System.err.println("panel de contenido del shell principal no registrado");
            return;
        }

        //oculta el sidebar al abrir modulo
        if (sidebar != null) {
            sidebar.setManaged(false);
            sidebar.setVisible(false);
        }

        try {
            Parent moduleUI = module.getMainUI();

            //crea la barra de cabecera
            HBox headerBar = createModuleHeader(module);

            //crea vbox envolvente con cabecera + contenido del modulo
            VBox moduleWrapper = new VBox();
            moduleWrapper.setStyle("-fx-spacing: 0; -fx-padding: 0;");
            moduleWrapper.getChildren().add(headerBar);
            moduleWrapper.getChildren().add(moduleUI);

            //hace que el contenido del modulo crezca para llenar el espacio disponible
            VBox.setVgrow(moduleUI, Priority.ALWAYS);

            mainShellContentPane.getChildren().setAll(moduleWrapper);
            System.out.println("loaded module: " + module.getName());

        } catch (Exception e) {
            System.err.println("error al cargar ui del modulo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * crea la barra de cabecera para un modulo con boton volver y titulo
     *
     * @param module el modulo
     * @return la cabecera HBox
     */
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

        //boton volver
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

        //etiqueta del nombre del modulo
        Label lblModuleName = new Label(module.getName());
        lblModuleName.setStyle(
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #ecf0f1;"
        );

        //espaciadores para centrar la etiqueta
        HBox spacerLeft = new HBox();
        HBox spacerRight = new HBox();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        header.getChildren().addAll(btnBack, spacerLeft, lblModuleName, spacerRight);

        return header;
    }

    /**
     * vuelve a la vista de modulos
     * restaura la visibilidad del sidebar
     */
    private static void showModulesView() {
        //restaura la visibilidad del sidebar
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

    //ayudantes privados

    /**
     * carga un archivo fxml y aplica su css correspondiente si existe
     * envuelve la escena con una barra de titulo personalizada
     *
     * @param fxmlFile  the fxml filename e.g. "Welcome.fxml"
     * @param cssName   the css filename without extension e.g. "welcome" -> welcome.css
     */
    private static void loadScene(String fxmlFile, String cssName) {
        try {
            URL fxmlUrl = SceneManager.class.getResource(FXML_PATH + fxmlFile);

            if (fxmlUrl == null) {
                System.err.println("fxml no encontrado: " + FXML_PATH + fxmlFile);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            //envuelve con barra de titulo personalizada
            VBox sceneWithTitleBar = new VBox();
            sceneWithTitleBar.setStyle("-fx-spacing: 0; -fx-padding: 0;");
            
            CustomTitleBar titleBar = new CustomTitleBar(stage);
            sceneWithTitleBar.getChildren().add(titleBar);
            sceneWithTitleBar.getChildren().add(root);
            
            VBox.setVgrow(root, Priority.ALWAYS);

            Scene scene = new Scene(sceneWithTitleBar);

            // aplicar hoja de estilos global
            URL globalCss = SceneManager.class.getResource(CSS_PATH + "global.css");
            if (globalCss != null) {
                scene.getStylesheets().add(globalCss.toExternalForm());
            }

            // aplicar hoja de estilos especifica de la pantalla si existe
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

    /**
     * devuelve el stage actual - usar para dialogos, popups etc
     */
    public static Stage getStage() {
        return stage;
    }
}
