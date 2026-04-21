package com.centralcore;

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

        //sin decoracion nativa, usamos CustomTitleBar
        stage.initStyle(StageStyle.UNDECORATED);

        stage.setTitle("CentralCore");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setWidth(1280);
        stage.setHeight(720);

        SceneManager.initialize(stage);
        SceneManager.showWelcome();

        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() throws Exception {
        com.centralcore.db.DatabaseConnection.close();
        System.out.println("centralcore cerrado");
    }
}