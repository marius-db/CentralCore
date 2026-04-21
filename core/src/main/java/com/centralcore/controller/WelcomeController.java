package com.centralcore.controller;

import com.centralcore.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * controlador para la pantalla de bienvenida/splash
 *
 * unica responsabilidad: navegar a la pantalla de login cuando se hace clic en el boton
 */
public class WelcomeController {

    @FXML private Button btnLogin;

    /**
     * llamado cuando se hace clic en el boton "log in"
     */
    @FXML
    private void onLoginClicked() {
        SceneManager.showLogin();
    }
}
