package com.centralcore;

import com.centralcore.modules.ModuleManager;
import com.centralcore.util.DwmManager;
import com.centralcore.util.SceneManager;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        Font.loadFont(getClass().getResourceAsStream("/com/centralcore/fonts/Orbitron-VariableFont_wght.ttf"), 14);
        primaryStage = stage;

        //transparent permite que DWM dibuje el borde de acento y sombra nativa de Windows
        stage.initStyle(StageStyle.TRANSPARENT);

        stage.setTitle("CentralCore");
        stage.setMinWidth(200);
        stage.setMinHeight(200);
        stage.setWidth(1280);
        stage.setHeight(768);

        //initialize construye la escena compartida y la asigna al stage
        SceneManager.initialize(stage);
        SceneManager.showWelcome();

        stage.show();
        //instalar integración DWM después de show() para que el HWND exista
        DwmManager.install(stage);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() throws Exception {
        ModuleManager.getInstance().shutdownAllModules();
        com.centralcore.db.DatabaseConnection.close();
        System.out.println("centralcore cerrado");
    }
}