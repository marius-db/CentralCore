package com.centralcore.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.dao.UserDAO;
import com.centralcore.model.User;
import com.centralcore.util.RememberMeStorage;
import com.centralcore.util.SceneManager;
import com.centralcore.util.SessionManager;
import com.centralcore.util.TranslationManager;

import javafx.concurrent.Task;
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

    //si hay credenciales guardadas el botón entra en modo "continuar sesión"
    private boolean hasSession = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TranslationManager.addLanguageChangeListener(this);

        cmbLanguage.getItems().addAll("English", "Español");
        cmbLanguage.setValue(TranslationManager.getCurrentLanguage().equals("en") ? "English" : "Español");
        cmbLanguage.setOnAction(e -> onLanguageComboChanged());

        loadCityImage();

        //detectar si hay sesión guardada para cambiar el texto del botón, sin autologin automático
        hasSession = RememberMeStorage.hasCredentials();
        updateLabels();
    }

    private void loadCityImage() {
        try {
            URL imageUrl = getClass().getResource("/com/centralcore/image/city.png");
            if (imageUrl == null) return;

            //cargar sin esperar el tamaño completo, el binding lo ajusta
            imgCity.setImage(new Image(imageUrl.toExternalForm(), true));

            //vincular al scene una vez disponible
            imgCity.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    imgCity.fitWidthProperty().bind(newScene.widthProperty());
                    imgCity.fitHeightProperty().bind(newScene.heightProperty());
                }
            });
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
        //si hay sesion guardada mostrar "continuar sesion", si no el login normal
        String btnKey = hasSession ? "welcome.continue" : "btn.login";
        btnLogin.setText(TranslationManager.get(btnKey));
        lblTaglineTitle.setText(TranslationManager.get("welcome.title"));
        lblTaglineSubtitle.setText(TranslationManager.get("welcome.subtitle"));
    }

    @FXML
    private void onLoginClicked() {
        if (hasSession) {
            //verificar credenciales en segundo plano cuando el usuario pulsa el boton
            runSessionAuth();
        } else {
            SceneManager.showLogin();
        }
    }

    //autentica en segundo plano con las credenciales guardadas tras pulsar el boton
    private void runSessionAuth() {
        String[] creds = RememberMeStorage.load();
        if (creds == null) {
            //las credenciales desaparecieron entre el check y el click
            hasSession = false;
            updateLabels();
            return;
        }

        btnLogin.setText(TranslationManager.get("welcome.loading"));
        btnLogin.setDisable(true);

        Task<User> authTask = new Task<>() {
            @Override
            protected User call() {
                //bcrypt es lento intencionadamente, nunca llamar desde el hilo de la ui
                return new UserDAO().authenticate(creds[0], creds[1]);
            }
        };

        authTask.setOnSucceeded(ev -> {
            User user = authTask.getValue();
            if (user != null) {
                SessionManager.setCurrentUser(user);
                SceneManager.showMainShell();
            } else {
                //credenciales caducadas o cambiadas, limpiar y dejar al usuario hacer login manual
                RememberMeStorage.clear();
                hasSession = false;
                btnLogin.setDisable(false);
                updateLabels();
            }
        });

        authTask.setOnFailed(ev -> {
            System.err.println("error verificando sesion guardada: " + authTask.getException().getMessage());
            btnLogin.setDisable(false);
            updateLabels();
        });

        new Thread(authTask, "session-auth-thread").start();
    }
}