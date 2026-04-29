package com.centralcore.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.dao.UserDAO;
import com.centralcore.model.User;
import com.centralcore.util.SceneManager;
import com.centralcore.util.SessionManager;
import com.centralcore.util.TranslationManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Label lblEmail;
    @FXML private Label lblPassword;
    @FXML private Button btnLogin;
    @FXML private Button btnBack;
    @FXML private ComboBox<String> cmbLanguage;

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
        //sincroniza el combobox si el cambio vino de otra fuente
        if (!cmbLanguage.getValue().equals(newLanguageCode.equals("en") ? "English" : "Español")) {
            cmbLanguage.setValue(newLanguageCode.equals("en") ? "English" : "Español");
        }
        updateLabels();
    }

    private void updateLabels() {
        lblEmail.setText(TranslationManager.get("login.email"));
        lblPassword.setText(TranslationManager.get("login.password"));
        btnLogin.setText(TranslationManager.get("btn.login"));
        btnBack.setText(TranslationManager.get("btn.back"));
    }

    @FXML
    private void onLoginClicked() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Por favor introduce tu email y contraseña.");
            return;
        }

        UserDAO userDAO = new UserDAO();
        User user = userDAO.authenticate(email, password);

        if (user != null) {
            //login exitoso - persiste sesion y navega al shell
            SessionManager.setCurrentUser(user);
            hideError();
            SceneManager.showMainShell();
        } else {
            showError("Email o contraseña incorrectos.");
            txtPassword.clear();
        }
    }

    @FXML
    private void onBackClicked() {
        SceneManager.showWelcome();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}