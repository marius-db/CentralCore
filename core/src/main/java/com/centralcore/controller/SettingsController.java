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
import javafx.scene.control.ButtonBar;
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

    //solo comprueba si la conexión activa responde, no intenta reabrir
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
        ButtonType btnYes = new ButtonType(TranslationManager.get("btn.yes"), ButtonBar.ButtonData.YES);
        ButtonType btnNo  = new ButtonType(TranslationManager.get("btn.no"),  ButtonBar.ButtonData.NO);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(TranslationManager.get("settings.deleteDb.confirm.title"));
        confirm.setHeaderText(TranslationManager.get("settings.deleteDb.confirm.header"));
        confirm.setContentText(TranslationManager.get("settings.deleteDb.confirm.body"));
        confirm.getButtonTypes().setAll(btnYes, btnNo);
        applyDialogTheme(confirm.getDialogPane());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != btnYes) return;

        ButtonType btnOk     = new ButtonType(TranslationManager.get("btn.ok"),     ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType(TranslationManager.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Dialog<String> passDialog = new Dialog<>();
        passDialog.setTitle(TranslationManager.get("settings.deleteDb.pass.title"));
        passDialog.setHeaderText(TranslationManager.get("settings.deleteDb.pass.header"));

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
        passDialog.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);
        passDialog.setResultConverter(btn -> btn == btnOk ? pwField.getText() : null);
        applyDialogTheme(passDialog.getDialogPane());

        Optional<String> passResult = passDialog.showAndWait();
        if (passResult.isEmpty() || passResult.get() == null) return;

        String email = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getEmail() : "";
        UserDAO dao = new UserDAO();
        if (dao.authenticate(email, passResult.get()) == null) {
            ButtonType btnErrOk = new ButtonType(TranslationManager.get("btn.ok"), ButtonBar.ButtonData.OK_DONE);
            Alert err = new Alert(Alert.AlertType.ERROR,
                    TranslationManager.get("settings.deleteDb.wrongPass"), btnErrOk);
            applyDialogTheme(err.getDialogPane());
            err.showAndWait();
            return;
        }

        DatabaseConnection.close();

        new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            String dbPath = System.getProperty("user.home") + "/.centralcore/centralcore_db";
            File dbFile = new File(dbPath + ".mv.db");

            System.out.println("exists: " + dbFile.exists());

            boolean deleted = dbFile.delete();

            System.out.println("deleted: " + deleted);

            File dbFile2 = new File(dbPath + ".trace.db");

            System.out.println("exists: " + dbFile.exists());

            boolean deleted2 = dbFile.delete();

            System.out.println("deleted: " + deleted);

            Platform.runLater(this::exitAfterDelete);
        }).start();
    }

    private void exitAfterDelete() {
        ButtonType btnOk = new ButtonType(TranslationManager.get("btn.ok"), ButtonBar.ButtonData.OK_DONE);
        Alert bye = new Alert(Alert.AlertType.INFORMATION);
        bye.setTitle(TranslationManager.get("settings.deleteDb.done.title"));
        bye.setContentText(TranslationManager.get("settings.deleteDb.done.body"));
        bye.getButtonTypes().setAll(btnOk);
        applyDialogTheme(bye.getDialogPane());
        bye.showAndWait();
        Platform.exit();
    }

    //inyecta dialog.css en el scene del dialogo cuando ya esta montado
    //los estilos inline se ignoran porque Modena los sobreescribe con mayor especificidad;
    //un stylesheet propio en el scene del dialogo gana correctamente
    private void applyDialogTheme(DialogPane pane) {
        //esperar a que el dialogo tenga scene antes de inyectar el css
        if (pane.getScene() != null) {
            injectDialogCss(pane);
        } else {
            pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) injectDialogCss(pane);
            });
        }
    }

    private void injectDialogCss(DialogPane pane) {
        URL cssUrl = getClass().getResource("/com/centralcore/css/dialog.css");
        if (cssUrl == null) {
            System.err.println("dialog.css no encontrado");
            return;
        }
        String cssStr = cssUrl.toExternalForm();
        if (!pane.getScene().getStylesheets().contains(cssStr)) {
            pane.getScene().getStylesheets().add(cssStr);
        }
    }
}