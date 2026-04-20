package com.centralcore;

import com.centralcore.util.SceneManager;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * clase principal de la aplicacion javafx
 *
 * gestiona el stage principal (ventana principal) e inicializa el gestor de escenas
 */
public class App extends Application {

    //el unico stage principal compartido en toda la app
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        Font.loadFont(getClass().getResourceAsStream("/com/centralcore/fonts/Orbitron-VariableFont_wght.ttf"), 14);
        primaryStage = stage;

        //elimina la decoracion de ventana por defecto para usar la barra de titulo personalizada
        stage.initStyle(StageStyle.UNDECORATED);

        //configurar la ventana principal
        stage.setTitle("CentralCore");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setWidth(1280);
        stage.setHeight(720);

        //inicializar el gestor de escenas con nuestro stage
        SceneManager.initialize(stage);

        //cargar la pantalla de bienvenida primero
        SceneManager.showWelcome();

        stage.show();
    }

    /**
     * devuelve el stage principal para su uso en cualquier parte de la app
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() throws Exception {
        //cerrar la conexion de la bd al salir de la app
        com.centralcore.db.DatabaseConnection.close();
        System.out.println("centralcore cerrado");
    }
}
