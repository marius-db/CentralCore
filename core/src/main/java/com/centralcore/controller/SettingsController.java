package com.centralcore.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.db.DatabaseConnection;
import com.centralcore.util.TranslationManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class SettingsController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private Label          lblTitle;
    @FXML private Label          lblAppearance;
    @FXML private Label          lblDarkMode;
    @FXML private Label          lblDarkModeDesc;
    @FXML private Label          lblLanguageSection;
    @FXML private Label          lblInterfaceLanguage;
    @FXML private Label          lblLanguageDesc;
    @FXML private Label          lblNotifications;
    @FXML private Label          lblModuleUpdates;
    @FXML private Label          lblModuleUpdatesDesc;
    @FXML private Label          lblSystemAlerts;
    @FXML private Label          lblSystemAlertsDesc;
    @FXML private Label          lblDatabase;
    @FXML private Label          lblConnectionStatus;
    @FXML private Label          lblAbout;
    @FXML private CheckBox       chkDarkMode;
    @FXML private CheckBox       chkModuleUpdates;
    @FXML private CheckBox       chkSystemAlerts;
    @FXML private ComboBox<String> cmbLanguage;
    @FXML private Label          lblConnStatus;
    @FXML private Button         btnTestConn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbLanguage.getItems().addAll("English", "Español");
        cmbLanguage.setValue(TranslationManager.getCurrentLanguage().equals("en") ? "English" : "Español");
        cmbLanguage.setOnAction(e -> onLanguageComboChanged());

        TranslationManager.addLanguageChangeListener(this);

        chkDarkMode.selectedProperty().addListener((obs, oldVal, newVal) -> onDarkModeToggled(newVal));
        chkModuleUpdates.selectedProperty().addListener((obs, oldVal, newVal) -> onModuleUpdatesToggled(newVal));
        chkSystemAlerts.selectedProperty().addListener((obs, oldVal, newVal) -> onSystemAlertsToggled(newVal));

        testConnection();
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
        lblTitle.setText(TranslationManager.get("settings.title"));
        lblAppearance.setText(TranslationManager.get("settings.appearance"));
        lblDarkMode.setText(TranslationManager.get("settings.darkMode"));
        lblDarkModeDesc.setText(TranslationManager.get("settings.darkModeDesc"));
        lblLanguageSection.setText(TranslationManager.get("settings.language"));
        lblInterfaceLanguage.setText(TranslationManager.get("settings.interfaceLanguage"));
        lblLanguageDesc.setText(TranslationManager.get("settings.interfaceLanguageDesc"));
        lblNotifications.setText(TranslationManager.get("settings.notifications"));
        lblModuleUpdates.setText(TranslationManager.get("settings.moduleUpdates"));
        lblModuleUpdatesDesc.setText(TranslationManager.get("settings.moduleUpdatesDesc"));
        lblSystemAlerts.setText(TranslationManager.get("settings.systemAlerts"));
        lblSystemAlertsDesc.setText(TranslationManager.get("settings.systemAlertsDesc"));
        lblDatabase.setText(TranslationManager.get("settings.database"));
        lblConnectionStatus.setText(TranslationManager.get("settings.connectionStatus"));
        lblAbout.setText(TranslationManager.get("settings.about"));
        btnTestConn.setText(TranslationManager.get("btn.test"));
    }

    private void onDarkModeToggled(boolean enabled) {
        System.out.println("dark mode toggled: " + enabled);
        //TODO: aplicar cambio de tema dinamicamente
    }

    private void onModuleUpdatesToggled(boolean enabled) {
        System.out.println("module updates notifications: " + enabled);
    }

    private void onSystemAlertsToggled(boolean enabled) {
        System.out.println("system alerts: " + enabled);
    }

    private void testConnection() {
        boolean ok = DatabaseConnection.testConnection();
        if (ok) {
            lblConnStatus.setText(TranslationManager.get("msg.connectedSuccess"));
            lblConnStatus.setStyle("-fx-text-fill: #27ae60;");
        } else {
            lblConnStatus.setText(TranslationManager.get("msg.connectedFailed"));
            lblConnStatus.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void onTestConnectionClicked() {
        testConnection();
    }
}