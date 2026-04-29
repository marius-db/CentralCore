package com.centralcore.controller;

import com.centralcore.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class WelcomeController {

    @FXML private Button btnLogin;

    @FXML private void onLoginClicked() {
        SceneManager.showLogin();
    }
}