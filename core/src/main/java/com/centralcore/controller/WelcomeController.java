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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class WelcomeController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private Button btnLogin;
    @FXML private ComboBox<String> cmbLanguage;
    @FXML private Label lblTaglineTitle;
    @FXML private Label lblTaglineSubtitle;
    @FXML private ImageView imgCity;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TranslationManager.addLanguageChangeListener(this);

        cmbLanguage.getItems().addAll("English", "Español");
        cmbLanguage.setValue(TranslationManager.getCurrentLanguage().equals("en") ? "English" : "Español");
        cmbLanguage.setOnAction(e -> onLanguageComboChanged());

        loadCityImage();
        updateLabels();
    }

    private void loadCityImage() {
        try {
            URL imageUrl = getClass().getResource("/com/centralcore/image/city.png");
            if (imageUrl != null) {
                //cargar sin esperar el tamaño completo, el binding lo ajusta
                imgCity.setImage(new Image(imageUrl.toExternalForm(), true));

                //vincular al scene una vez disponible: scene siempre refleja el tamaño real de la ventana
                //usar la escena en vez del parent evita que la imagen empuje el layout
                imgCity.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        imgCity.fitWidthProperty().bind(newScene.widthProperty());
                        imgCity.fitHeightProperty().bind(newScene.heightProperty());
                    }
                });
            } else {
                System.err.println("imagen city.png no encontrada");
            }
        } catch (Exception e) {
            System.err.println("error al cargar city.png: " + e.getMessage());
        }
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