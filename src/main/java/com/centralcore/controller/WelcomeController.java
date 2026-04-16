package com.centralcore.controller;

import com.centralcore.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * controller for the welcome/splash screen
 * controlador para la pantalla de bienvenida/splash
 *
 * only responsibility: navigate to the login screen when button is clicked
 * unica responsabilidad: navegar a la pantalla de login cuando se hace clic en el boton
 */
public class WelcomeController {

    @FXML private Button btnLogin;

    /**
     * called when the "log in" button is clicked
     * llamado cuando se hace clic en el boton "log in"
     */
    @FXML
    private void onLoginClicked() {
        SceneManager.showLogin();
    }
}
