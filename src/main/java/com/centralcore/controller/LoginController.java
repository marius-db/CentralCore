package com.centralcore.controller;

import com.centralcore.dao.UserDAO;
import com.centralcore.model.User;
import com.centralcore.util.SceneManager;
import com.centralcore.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * controller for the login screen
 * controlador para la pantalla de inicio de sesion
 *
 * handles form input validation, authentication against the db, and navigation
 * gestiona la validacion del formulario, autenticacion contra la bd y la navegacion
 */
public class LoginController {

    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;
    @FXML private Button        btnLogin;

    /**
     * called when the login button is clicked or enter is pressed (defaultButton=true)
     * llamado cuando se hace clic en el boton login o se pulsa enter (defaultButton=true)
     */
    @FXML
    private void onLoginClicked() {
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();

        // basic empty field validation / validacion basica de campos vacios
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password. / Por favor introduce tu email y contraseña.");
            return;
        }

        // attempt authentication against the database
        // intentar autenticacion contra la base de datos
        UserDAO userDAO = new UserDAO();
        User user = userDAO.authenticate(email, password);

        if (user != null) {
            // login successful - save user in session and go to main shell
            // login exitoso - guardar usuario en sesion e ir al shell principal
            SessionManager.setCurrentUser(user);
            hideError();
            SceneManager.showMainShell();
        } else {
            // wrong credentials / credenciales incorrectas
            showError("Invalid email or password. / Email o contraseña incorrectos.");
            txtPassword.clear();
        }
    }

    /**
     * called when the back button is clicked
     * llamado cuando se hace clic en el boton volver
     */
    @FXML
    private void onBackClicked() {
        SceneManager.showWelcome();
    }

    // --- private helpers / ayudantes privados ---

    /** shows an error message under the form / muestra un mensaje de error bajo el formulario */
    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    /** hides the error message / oculta el mensaje de error */
    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
