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

/**
 * controlador para la pantalla de inicio de sesion
 *
 * gestiona la validacion del formulario, autenticacion contra la bd, navegacion y cambio de idioma
 */
public class LoginController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;
    @FXML private Label         lblEmail;
    @FXML private Label         lblPassword;
    @FXML private Button        btnLogin;
    @FXML private Button        btnBack;
    @FXML private ComboBox<String> cmbLanguage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //registra escuchador de cambio de idioma
        TranslationManager.addLanguageChangeListener(this);

        //configura opciones de idioma
        cmbLanguage.getItems().addAll("English", "Español");
        cmbLanguage.setValue(TranslationManager.getCurrentLanguage().equals("en") ? "English" : "Español");
        cmbLanguage.setOnAction(e -> onLanguageComboChanged());

        //actualiza etiquetas con idioma actual
        updateLabels();
    }

    /**
     * maneja cambio del combobox de idioma
     */
    private void onLanguageComboChanged() {
        String selected = cmbLanguage.getValue();
        String langCode = selected.equals("English") ? "en" : "es";
        TranslationManager.setLanguage(langCode);
    }

    /**
     * devolucuon de observador para cambios de idioma de cualquier fuente
     */
    @Override
    public void onLanguageChanged(String newLanguageCode) {
        //actualiza combobox para que coincida con nuevo idioma
        if (!cmbLanguage.getValue().equals(newLanguageCode.equals("en") ? "English" : "Español")) {
            cmbLanguage.setValue(newLanguageCode.equals("en") ? "English" : "Español");
        }
        
        //actualiza todas las etiquetas
        updateLabels();
    }

    /**
     * actualiza todas las etiquetas con texto traducido del gestor de traducciones
     */
    private void updateLabels() {
        lblEmail.setText(TranslationManager.get("login.email"));
        lblPassword.setText(TranslationManager.get("login.password"));
        btnLogin.setText(TranslationManager.get("btn.login"));
        btnBack.setText(TranslationManager.get("btn.back"));
    }

    /**
     * llamado cuando se hace clic en el boton login o se pulsa enter (defaultButton=true)
     */
    @FXML
    private void onLoginClicked() {
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();

        // validacion basica de campos vacios
        if (email.isEmpty() || password.isEmpty()) {
            showError("Por favor introduce tu email y contraseña.");
            return;
        }

        // intentar autenticacion contra la base de datos
        UserDAO userDAO = new UserDAO();
        User user = userDAO.authenticate(email, password);

        if (user != null) {
            // login exitoso - guardar usuario en sesion e ir al shell principal
            SessionManager.setCurrentUser(user);
            hideError();
            SceneManager.showMainShell();
        } else {
            // credenciales incorrectas
            showError("Email o contraseña incorrectos.");
            txtPassword.clear();
        }
    }

    /**
     * llamado cuando se hace clic en el boton volver
     */
    @FXML
    private void onBackClicked() {
        SceneManager.showWelcome();
    }

    //ayudantes privados

    /**
     * muestra un mensaje de error bajo el formulario
     */
    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    /**
     * oculta el mensaje de error
     */
    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
