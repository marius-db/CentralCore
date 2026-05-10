package com.centralcore.controller;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import com.centralcore.dao.UserDAO;
import com.centralcore.db.DatabaseConnection;
import com.centralcore.util.SessionManager;
import com.centralcore.util.TranslationManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class SettingsController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private Label lblTitle;
    @FXML private Label lblLanguageSection;
    @FXML private Label lblInterfaceLanguage;
    @FXML private Label lblLanguageDesc;
    @FXML private ComboBox<String> cmbLanguage;

    @FXML private Label lblDatabase;
    @FXML private Label lblConnectionStatus;
    @FXML private Label lblConnStatus;
    @FXML private Button btnTestConn;
    @FXML private Label lblDbControls;
    @FXML private Button btnDbStop;
    @FXML private Button btnDbStart;
    @FXML private Button btnDbRestart;
    @FXML private Label lblDangerZone;
    @FXML private Label lblDeleteDbDesc;
    @FXML private Button btnDeleteDb;

    @FXML private Label lblAbout;
    @FXML private Label lblDeleteDbTitle;
    @FXML private Label lblAboutApp;
    @FXML private Label lblAboutDesc;
    @FXML private Label lblAboutCopy;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbLanguage.getItems().addAll("English", "Español");
        cmbLanguage.setValue(TranslationManager.getCurrentLanguage().equals("en") ? "English" : "Español");
        cmbLanguage.setOnAction(e -> onLanguageComboChanged());

        TranslationManager.addLanguageChangeListener(this);

        pingStatus();
        updateLabels();
    }

    private void onLanguageComboChanged() {
        String selected = cmbLanguage.getValue();
        String langCode = selected.equals("English") ? "en" : "es";
        TranslationManager.setLanguage(langCode);
    }

    @Override
    public void onLanguageChanged(String newLanguageCode) {
        if (!cmbLanguage.getValue().equals(newLanguageCode.equals("en") ? "English" : "Español")) {
            cmbLanguage.setValue(newLanguageCode.equals("en") ? "English" : "Español");
        }
        updateLabels();
    }

    private void updateLabels() {
        lblTitle.setText(TranslationManager.get("settings.title"));
        lblLanguageSection.setText(TranslationManager.get("settings.language"));
        lblInterfaceLanguage.setText(TranslationManager.get("settings.interfaceLanguage"));
        lblLanguageDesc.setText(TranslationManager.get("settings.interfaceLanguageDesc"));
        lblDatabase.setText(TranslationManager.get("settings.database"));
        lblConnectionStatus.setText(TranslationManager.get("settings.connectionStatus"));
        btnTestConn.setText(TranslationManager.get("btn.test"));
        lblDbControls.setText(TranslationManager.get("settings.dbControls"));
        btnDbStop.setText(TranslationManager.get("settings.db.stop"));
        btnDbStart.setText(TranslationManager.get("settings.db.start"));
        btnDbRestart.setText(TranslationManager.get("settings.db.restart"));
        lblDangerZone.setText(TranslationManager.get("settings.dangerZone"));
        lblDeleteDbDesc.setText(TranslationManager.get("settings.deleteDbDesc"));
        btnDeleteDb.setText(TranslationManager.get("settings.deleteDb"));
        lblAbout.setText(TranslationManager.get("settings.about"));
        if (lblDeleteDbTitle != null) lblDeleteDbTitle.setText(TranslationManager.get("settings.deleteDb"));
        if (lblAboutApp     != null) lblAboutApp.setText(TranslationManager.get("settings.about.app"));
        if (lblAboutDesc    != null) lblAboutDesc.setText(TranslationManager.get("settings.about.desc"));
        if (lblAboutCopy    != null) lblAboutCopy.setText(TranslationManager.get("settings.about.copy"));
    }

    //solo comprueba si la conexion activa responde, no intenta reabrir
    private void pingStatus() {
        boolean ok = DatabaseConnection.pingConnection();
        if (ok) {
            lblConnStatus.setText(TranslationManager.get("msg.connectedSuccess"));
            lblConnStatus.setStyle("-fx-text-fill: -cc-success;");
        } else {
            lblConnStatus.setText(TranslationManager.get("msg.connectedFailed"));
            lblConnStatus.setStyle("-fx-text-fill: -cc-error;");
        }
    }

    @FXML
    private void onTestConnectionClicked() {
        pingStatus();
    }

    @FXML
    private void onDbStopClicked() {
        DatabaseConnection.close();
        lblConnStatus.setText(TranslationManager.get("settings.db.stopped"));
        lblConnStatus.setStyle("-fx-text-fill: -cc-warning;");
    }

    @FXML
    private void onDbStartClicked() {
        boolean ok = DatabaseConnection.testConnection();
        if (ok) {
            lblConnStatus.setText(TranslationManager.get("msg.connectedSuccess"));
            lblConnStatus.setStyle("-fx-text-fill: -cc-success;");
        } else {
            lblConnStatus.setText(TranslationManager.get("msg.connectedFailed"));
            lblConnStatus.setStyle("-fx-text-fill: -cc-error;");
        }
    }

    @FXML
    private void onDbRestartClicked() {
        DatabaseConnection.close();
        new Thread(() -> {
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                boolean ok = DatabaseConnection.testConnection();
                if (ok) {
                    lblConnStatus.setText(
                            TranslationManager.get("msg.connectedSuccess") +
                                    " (" + TranslationManager.get("settings.db.restarted") + ")"
                    );
                    lblConnStatus.setStyle("-fx-text-fill: -cc-success;");
                } else {
                    lblConnStatus.setText(TranslationManager.get("msg.connectedFailed"));
                    lblConnStatus.setStyle("-fx-text-fill: -cc-error;");
                }
            });
        }).start();
    }

    @FXML
    private void onDeleteDbClicked() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(TranslationManager.get("settings.deleteDb.confirm.title"));
        confirm.setHeaderText(TranslationManager.get("settings.deleteDb.confirm.header"));
        confirm.setContentText(TranslationManager.get("settings.deleteDb.confirm.body"));
        confirm.getDialogPane().setStyle("-fx-background-color: #1c1f2b; -fx-text-fill: #f0f2f8;");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        Dialog<String> passDialog = new Dialog<>();
        passDialog.setTitle(TranslationManager.get("settings.deleteDb.pass.title"));
        passDialog.setHeaderText(TranslationManager.get("settings.deleteDb.pass.header"));
        passDialog.getDialogPane().setStyle("-fx-background-color: #1c1f2b; -fx-text-fill: #f0f2f8;");

        PasswordField pwField = new PasswordField();
        pwField.setPromptText(TranslationManager.get("login.password"));
        pwField.setStyle(
                "-fx-background-color: #151820; -fx-text-fill: #f0f2f8;" +
                        "-fx-border-color: #252a3a; -fx-padding: 8 12;"
        );

        VBox content = new VBox(8);
        Label lbl = new Label(TranslationManager.get("settings.deleteDb.pass.prompt"));
        lbl.setStyle("-fx-text-fill: -cc-text-secondary;");
        content.getChildren().addAll(lbl, pwField);
        content.setStyle("-fx-padding: 12;");
        passDialog.getDialogPane().setContent(content);
        passDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        passDialog.setResultConverter(btn -> btn == ButtonType.OK ? pwField.getText() : null);

        Optional<String> passResult = passDialog.showAndWait();
        if (passResult.isEmpty() || passResult.get() == null) return;

        String email = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getEmail() : "";
        UserDAO dao = new UserDAO();
        if (dao.authenticate(email, passResult.get()) == null) {
            Alert err = new Alert(Alert.AlertType.ERROR,
                    TranslationManager.get("settings.deleteDb.wrongPass"), ButtonType.OK);
            err.getDialogPane().setStyle("-fx-background-color: #1c1f2b; -fx-text-fill: #f0f2f8;");
            err.showAndWait();
            return;
        }

        DatabaseConnection.close();

        new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            String dbPath = System.getProperty("user.home") + "/centralcore_db";
            new File(dbPath + ".mv.db").delete();
            new File(dbPath + ".trace.db").delete();
            Platform.runLater(this::exitAfterDelete);
        }).start();
    }

    private void exitAfterDelete() {
        Alert bye = new Alert(Alert.AlertType.INFORMATION);
        bye.setTitle(TranslationManager.get("settings.deleteDb.done.title"));
        bye.setContentText(TranslationManager.get("settings.deleteDb.done.body"));
        bye.getDialogPane().setStyle("-fx-background-color: #1c1f2b; -fx-text-fill: #f0f2f8;");
        bye.showAndWait();
        Platform.exit();
    }
}