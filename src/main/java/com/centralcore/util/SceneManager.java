package com.centralcore.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

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

    // the primary stage set during app startup
    // el stage principal definido durante el inicio de la app
    private static Stage stage;

    // path prefix for all fxml files
    // prefijo de ruta para todos los archivos fxml
    private static final String FXML_PATH = "/com/centralcore/fxml/";

    // path prefix for all css files
    // prefijo de ruta para todos los archivos css
    private static final String CSS_PATH = "/com/centralcore/css/";

    // private constructor - utility class, no instantiation
    // constructor privado - clase utilitaria, sin instanciacion
    private SceneManager() {}

    /**
     * must be called once during app startup with the primary stage
     * debe llamarse una vez durante el inicio de la app con el stage principal
     */
    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
    }

    // --- navigation methods / metodos de navegacion ---

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

    // --- private helpers / ayudantes privados ---

    /**
     * loads an fxml file and applies its matching css file if it exists
     * carga un archivo fxml y aplica su css correspondiente si existe
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

            Scene scene = new Scene(root);

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
