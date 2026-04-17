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

/**
 * controller for the settings view
 * controlador para la vista de configuracion
 *
 * listens for language changes and updates ui automatically
 * escucha cambios de idioma y actualiza la ui automaticamente
 */
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
        //setup language options / configura opciones de idioma
        cmbLanguage.getItems().addAll("English", "Español");
        cmbLanguage.setValue(TranslationManager.getCurrentLanguage().equals("en") ? "English" : "Español");
        cmbLanguage.setOnAction(e -> onLanguageComboChanged());

        //register language change listener / registra escuchador de cambio de idioma
        TranslationManager.addLanguageChangeListener(this);

        //setup checkboxes / configura checkboxes
        chkDarkMode.selectedProperty().addListener((obs, oldVal, newVal) -> onDarkModeToggled(newVal));
        chkModuleUpdates.selectedProperty().addListener((obs, oldVal, newVal) -> onModuleUpdatesToggled(newVal));
        chkSystemAlerts.selectedProperty().addListener((obs, oldVal, newVal) -> onSystemAlertsToggled(newVal));

        //test connection on startup / prueba conexion al iniciar
        testConnection();
        
        //update labels with current language / actualiza etiquetas con idioma actual
        updateLabels();
    }

    /**
     * handles language combo box change
     * maneja cambio del combobox de idioma
     */
    private void onLanguageComboChanged() {
        String selected = cmbLanguage.getValue();
        String langCode = selected.equals("English") ? "en" : "es";
        TranslationManager.setLanguage(langCode);
    }

    /**
     * observer callback for language changes from any source
     * devolucuon de observador para cambios de idioma de cualquier fuente
     * called whenever language is changed anywhere in the app
     * llamado cada vez que se cambia el idioma en cualquier parte de la app
     */
    @Override
    public void onLanguageChanged(String newLanguageCode) {
        //update combo box to match new language / actualiza combobox para que coincida con nuevo idioma
        if (!cmbLanguage.getValue().equals(newLanguageCode.equals("en") ? "English" : "Español")) {
            cmbLanguage.setValue(newLanguageCode.equals("en") ? "English" : "Español");
        }
        
        //update all labels / actualiza todas las etiquetas
        updateLabels();
    }

    /**
     * updates all labels with translated text from translation manager
     * actualiza todas las etiquetas con texto traducido del gestor de traducciones
     */
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

    /**
     * handles dark mode toggle / maneja toggle de modo oscuro
     */
    private void onDarkModeToggled(boolean enabled) {
        System.out.println("dark mode toggled: " + enabled);
        //TODO: apply theme change dynamically / aplicar cambio de tema dinamicamente
    }

    /**
     * handles module updates notification toggle / maneja toggle de notificaciones de actualizaciones de modulos
     */
    private void onModuleUpdatesToggled(boolean enabled) {
        System.out.println("module updates notifications: " + enabled);
    }

    /**
     * handles system alerts toggle / maneja toggle de alertas del sistema
     */
    private void onSystemAlertsToggled(boolean enabled) {
        System.out.println("system alerts: " + enabled);
    }

    /**
     * tests the db connection and shows the result in the status label
     * prueba la conexion a bd y muestra el resultado en la etiqueta de estado
     */
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
