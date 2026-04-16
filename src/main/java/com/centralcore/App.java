package com.centralcore;

import com.centralcore.util.SceneManager;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * main javafx application class
 * clase principal de la aplicacion javafx
 *
 * handles the primary stage (main window) and bootstraps the scene manager
 * gestiona el stage principal (ventana principal) e inicializa el gestor de escenas
 */
public class App extends Application {

    // the single primary stage shared across the whole app
    // el unico stage principal compartido en toda la app
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        Font.loadFont(getClass().getResourceAsStream("/com/centralcore/fonts/Orbitron-VariableFont_wght.ttf"), 14);
        primaryStage = stage;

        //configure the main window
        //configurar la ventana principal
        stage.setTitle("CentralCore");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setWidth(1280);
        stage.setHeight(720);

        //remove default window decoration if you want custom title bar later
        //eliminar decoracion por defecto si se quiere barra de titulo personalizada despues
        //stage.initStyle(StageStyle.UNDECORATED); // uncomment when ready

        //initialize the scene manager with our stage
        //inicializar el gestor de escenas con nuestro stage
        SceneManager.initialize(stage);

        //load the welcome screen first
        //cargar la pantalla de bienvenida primero
        SceneManager.showWelcome();

        stage.show();
    }

    /**
     * returns the primary stage for use anywhere in the app
     * devuelve el stage principal para su uso en cualquier parte de la app
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() throws Exception {
        //close db connection on app exit
        //cerrar la conexion de la bd al salir de la app
        com.centralcore.db.DatabaseConnection.close();
        System.out.println("centralcore closed / centralcore cerrado");
    }
}
