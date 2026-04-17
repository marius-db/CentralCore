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
 * central scene/navigation manager for the entire app
 * gestor central de escenas/navegacion para toda la aplicacion
 *
 * all screen transitions go through here - never load scenes directly
 * todos los cambios de pantalla pasan por aqui - nunca cargar escenas directamente
 *
 * usage: SceneManager.showLogin();
 * uso: SceneManager.showLogin();
 */
public class SceneManager {

    //the primary stage set during app startup
    //el stage principal definido durante el inicio de la app
    private static Stage stage;

    //reference to main shell controller for dynamic content loading
    //referencia al controlador del shell principal para carga dinamica de contenido
    private static StackPane mainShellContentPane;

    //reference to sidebar for toggling visibility when opening modules
    //referencia al sidebar para toggle de visibilidad al abrir modulos
    private static javafx.scene.layout.VBox sidebar;

    //path prefix for all fxml files
    //prefijo de ruta para todos los archivos fxml
    private static final String FXML_PATH = "/com/centralcore/fxml/";

    //path prefix for all css files
    //prefijo de ruta para todos los archivos css
    private static final String CSS_PATH = "/com/centralcore/css/";

    //private constructor - utility class, no instantiation
    //constructor privado - clase utilitaria, sin instanciacion
    private SceneManager() {}

    /**
     * must be called once during app startup with the primary stage
     * debe llamarse una vez durante el inicio de la app con el stage principal
     */
    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
    }

    /**
     * registers the main shell content pane for dynamic module loading
     * must be called by MainShellController during initialization
     * registra el panel de contenido del shell principal para carga dinamica de modulos
     * debe ser llamado por MainShellController durante la inicializacion
     */
    public static void setMainShellContentPane(StackPane contentPane) {
        mainShellContentPane = contentPane;
    }

    /**
     * registers the sidebar for toggling visibility when modules open/close
     * registra el sidebar para toggle de visibilidad cuando modulos abren/cierran
     */
    public static void setSidebar(javafx.scene.layout.VBox sidebarPane) {
        sidebar = sidebarPane;
    }

    //navigation methods / metodos de navegacion

    /** shows the welcome/splash screen / muestra la pantalla de bienvenida */
    public static void showWelcome() {
        loadScene("Welcome.fxml", "welcome");
    }

    /** shows the login screen / muestra la pantalla de inicio de sesion */
    public static void showLogin() {
        loadScene("Login.fxml", "auth");
    }

    /** shows the main shell (sidebar + content area) / muestra el shell principal (sidebar + area de contenido) */
    public static void showMainShell() {
        loadScene("MainShell.fxml", "main");
    }

    /**
     * loads a module's ui into the main shell's content pane with a header bar
     * wraps the module ui so it fills the entire content pane with back button
     * hides the sidebar for full module focus
     * carga la ui de un modulo en el panel de contenido del shell principal con una barra de cabecera
     * envuelve la ui del modulo para que llene todo el panel de contenido con boton volver
     * oculta el sidebar para enfoque total del modulo
     *
     * @param module the module to display / el modulo a mostrar
     */
    public static void showModule(Module module) {
        if (mainShellContentPane == null) {
            System.err.println("main shell content pane not registered / panel de contenido del shell principal no registrado");
            return;
        }

        //hide sidebar when opening module / oculta el sidebar al abrir modulo
        if (sidebar != null) {
            sidebar.setManaged(false);
            sidebar.setVisible(false);
        }

        try {
            Parent moduleUI = module.getMainUI();

            //create header bar / crea la barra de cabecera
            HBox headerBar = createModuleHeader(module);

            //create wrapper vbox with header + module content
            //crea vbox envolvente con cabecera + contenido del modulo
            VBox moduleWrapper = new VBox();
            moduleWrapper.setStyle("-fx-spacing: 0; -fx-padding: 0;");
            moduleWrapper.getChildren().add(headerBar);
            moduleWrapper.getChildren().add(moduleUI);

            //make module content grow to fill available space
            //hace que el contenido del modulo crezca para llenar el espacio disponible
            VBox.setVgrow(moduleUI, Priority.ALWAYS);

            mainShellContentPane.getChildren().setAll(moduleWrapper);
            System.out.println("loaded module: " + module.getName());

        } catch (Exception e) {
            System.err.println("error loading module ui / error al cargar ui del modulo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * creates the header bar for a module with back button and title
     * crea la barra de cabecera para un modulo con boton volver y titulo
     *
     * @param module the module / el modulo
     * @return the header HBox / la cabecera HBox
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

        //back button / boton volver
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

        //module name label / etiqueta del nombre del modulo
        Label lblModuleName = new Label(module.getName());
        lblModuleName.setStyle(
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #ecf0f1;"
        );

        //spacers to center the label / espaciadores para centrar la etiqueta
        HBox spacerLeft = new HBox();
        HBox spacerRight = new HBox();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        header.getChildren().addAll(btnBack, spacerLeft, lblModuleName, spacerRight);

        return header;
    }

    /**
     * goes back to the modules view
     * restores sidebar visibility
     * vuelve a la vista de modulos
     * restaura la visibilidad del sidebar
     */
    private static void showModulesView() {
        //restore sidebar visibility / restaura la visibilidad del sidebar
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
            System.err.println("error loading modules view / error al cargar vista de modulos: " + e.getMessage());
        }
    }

    //private helpers / ayudantes privados

    /**
     * loads an fxml file and applies its matching css file if it exists
     * wraps the scene with a custom title bar
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
                System.err.println("fxml not found / fxml no encontrado: " + FXML_PATH + fxmlFile);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            //wrap with custom title bar / envuelve con barra de titulo personalizada
            VBox sceneWithTitleBar = new VBox();
            sceneWithTitleBar.setStyle("-fx-spacing: 0; -fx-padding: 0;");
            
            CustomTitleBar titleBar = new CustomTitleBar(stage);
            sceneWithTitleBar.getChildren().add(titleBar);
            sceneWithTitleBar.getChildren().add(root);
            
            VBox.setVgrow(root, Priority.ALWAYS);

            Scene scene = new Scene(sceneWithTitleBar);

            // apply global stylesheet / aplicar hoja de estilos global
            URL globalCss = SceneManager.class.getResource(CSS_PATH + "global.css");
            if (globalCss != null) {
                scene.getStylesheets().add(globalCss.toExternalForm());
            }

            // apply screen-specific stylesheet if it exists
            // aplicar hoja de estilos especifica de la pantalla si existe
            URL specificCss = SceneManager.class.getResource(CSS_PATH + cssName + ".css");
            if (specificCss != null) {
                scene.getStylesheets().add(specificCss.toExternalForm());
            }

            stage.setScene(scene);

        } catch (IOException e) {
            System.err.println("error loading scene / error cargando escena: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * returns the current stage - use for dialogs, popups etc
     * devuelve el stage actual - usar para dialogos, popups etc
     */
    public static Stage getStage() {
        return stage;
    }
}
