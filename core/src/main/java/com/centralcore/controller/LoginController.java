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
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class LoginController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtUsername;
    @FXML private Label lblError;
    @FXML private Label lblTitle;
    @FXML private Label lblEmail;
    @FXML private Label lblPassword;
    @FXML private Label lblConfirmPassword;
    @FXML private Label lblUsername;
    @FXML private Label lblSwitchLink;
    @FXML private Button btnLogin;
    @FXML private Button btnBack;
    @FXML private ComboBox<String> cmbLanguage;
    @FXML private StackPane loginRoot;

    //false = modo login, true = modo registro
    private boolean isRegisterMode = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TranslationManager.addLanguageChangeListener(this);

        cmbLanguage.getItems().addAll("English", "Español");
        cmbLanguage.setValue(TranslationManager.getCurrentLanguage().equals("en") ? "English" : "Español");
        cmbLanguage.setOnAction(e -> onLanguageComboChanged());

        loadBackground();
        updateLabels();
    }

    private void loadBackground() {
        try {
            URL imageUrl = getClass().getResource("/com/centralcore/image/city.png");
            if (imageUrl == null) return;

            ImageView bg = new ImageView(new Image(imageUrl.toExternalForm(), true));
            bg.setPreserveRatio(false);
            bg.setEffect(new GaussianBlur(20));

            //vincular al tamaño de la escena igual que en el welcome
            bg.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    bg.fitWidthProperty().bind(newScene.widthProperty());
                    bg.fitHeightProperty().bind(newScene.heightProperty());
                }
            });

            //insertar como primera capa, por debajo de todo el contenido existente
            loginRoot.getChildren().add(0, bg);

        } catch (Exception e) {
            System.err.println("error al cargar fondo del login: " + e.getMessage());
        }
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
        btnBack.setText(TranslationManager.get("btn.back"));

        if (isRegisterMode) {
            lblTitle.setText(TranslationManager.get("login.register.title"));
            lblTitle.setVisible(true);
            lblTitle.setManaged(true);
            btnLogin.setText(TranslationManager.get("btn.register"));
            lblConfirmPassword.setText(TranslationManager.get("login.confirmPassword"));
            lblUsername.setText(TranslationManager.get("login.username"));
            lblSwitchLink.setText(TranslationManager.get("login.switchToLogin"));
        } else {
            lblTitle.setVisible(false);
            lblTitle.setManaged(false);
            btnLogin.setText(TranslationManager.get("btn.login"));
            lblSwitchLink.setText(TranslationManager.get("login.switchToRegister"));
        }
    }

    //alterna entre modo login y modo registro
    @FXML
    private void onSwitchModeClicked() {
        isRegisterMode = !isRegisterMode;
        hideError();
        clearFields();
        setRegisterFieldsVisible(isRegisterMode);
        updateLabels();
    }

    private void setRegisterFieldsVisible(boolean visible) {
        txtConfirmPassword.setVisible(visible);
        txtConfirmPassword.setManaged(visible);
        lblConfirmPassword.setVisible(visible);
        lblConfirmPassword.setManaged(visible);
        txtUsername.setVisible(visible);
        txtUsername.setManaged(visible);
        lblUsername.setVisible(visible);
        lblUsername.setManaged(visible);
    }

    @FXML
    private void onLoginClicked() {
        if (isRegisterMode) {
            handleRegister();
        } else {
            handleLogin();
        }
    }

    private void handleLogin() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError(TranslationManager.get("login.error.emptyFields"));
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
            showError(TranslationManager.get("login.error.invalidCredentials"));
            txtPassword.clear();
        }
    }

    private void handleRegister() {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError(TranslationManager.get("login.error.emptyFields"));
            return;
        }

        if (!password.equals(confirm)) {
            showError(TranslationManager.get("login.error.passwordMismatch"));
            txtPassword.clear();
            txtConfirmPassword.clear();
            return;
        }

        if (password.length() < 8) {
            showError(TranslationManager.get("login.error.passwordTooShort"));
            return;
        }

        UserDAO userDAO = new UserDAO();
        boolean created = userDAO.register(username, email, password);

        if (created) {
            //registro exitoso - vuelve al modo login con mensaje de confirmacion
            isRegisterMode = false;
            setRegisterFieldsVisible(false);
            clearFields();
            updateLabels();
            showSuccess(TranslationManager.get("login.register.success"));
        } else {
            showError(TranslationManager.get("login.error.emailInUse"));
        }
    }

    @FXML
    private void onBackClicked() {
        SceneManager.showWelcome();
    }

    private void clearFields() {
        txtEmail.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
        txtUsername.clear();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setStyle("");
        lblError.getStyleClass().removeAll("login-success");
        if (!lblError.getStyleClass().contains("login-error")) {
            lblError.getStyleClass().add("login-error");
        }
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showSuccess(String message) {
        lblError.setText(message);
        lblError.getStyleClass().removeAll("login-error");
        if (!lblError.getStyleClass().contains("login-success")) {
            lblError.getStyleClass().add("login-success");
        }
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}