package com.centralcore.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.util.SceneManager;
import com.centralcore.util.TranslationManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class WelcomeController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private Button btnLogin;
    @FXML private ComboBox<String> cmbLanguage;
    @FXML private Label lblTaglineTitle;
    @FXML private Label lblTaglineSubtitle;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TranslationManager.addLanguageChangeListener(this);

        cmbLanguage.getItems().addAll("English", "Español");
        cmbLanguage.setValue(TranslationManager.getCurrentLanguage().equals("en") ? "English" : "Español");
        cmbLanguage.setOnAction(e -> onLanguageComboChanged());

        updateLabels();
    }

    private void onLanguageComboChanged() {
        String selected = cmbLanguage.getValue();
        String langCode = selected.equals("English") ? "en" : "es";
        TranslationManager.setLanguage(langCode);
    }

    @Override
    public void onLanguageChanged(String newLanguageCode) {
        String expectedValue = newLanguageCode.equals("en") ? "English" : "Español";
        if (!cmbLanguage.getValue().equals(expectedValue)) {
            cmbLanguage.setValue(expectedValue);
        }
        updateLabels();
    }

    private void updateLabels() {
        btnLogin.setText(TranslationManager.get("btn.login"));
        lblTaglineTitle.setText(TranslationManager.get("welcome.title"));
        lblTaglineSubtitle.setText(TranslationManager.get("welcome.subtitle"));
    }

    @FXML
    private void onLoginClicked() {
        SceneManager.showLogin();
    }
}