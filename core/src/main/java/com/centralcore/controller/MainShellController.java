package com.centralcore.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.modules.ModuleManager;
import com.centralcore.util.SceneManager;
import com.centralcore.util.SessionManager;
import com.centralcore.util.TranslationManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainShellController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private VBox sidebar;
    @FXML private StackPane contentPane;
    @FXML private Label lblUsername;
    @FXML private Button btnModules;
    @FXML private Button btnInstalls;
    @FXML private Button btnLicences;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    private static final String FXML_PATH = "/com/centralcore/fxml/";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TranslationManager.addLanguageChangeListener(this);

        SceneManager.setMainShellContentPane(contentPane);
        SceneManager.setSidebar(sidebar);

        ModuleManager moduleManager = ModuleManager.getInstance();
        moduleManager.loadAndInitializeModules();

        if (SessionManager.getCurrentUser() != null) {
            lblUsername.setText(SessionManager.getCurrentUser().getUsername());
        }

        //carga la vista por defecto al arrancar
        loadView("ModulesView.fxml");
        setActiveNav(btnModules);

        updateLabels();
    }

    @FXML
    private void onModulesClicked() {
        loadView("ModulesView.fxml");
        setActiveNav(btnModules);
    }

    @FXML
    private void onInstallsClicked() {
        loadView("Installs.fxml");
        setActiveNav(btnInstalls);
    }

    @FXML
    private void onLicencesClicked() {
        loadView("Licences.fxml");
        setActiveNav(btnLicences);
    }

    @FXML
    private void onSettingsClicked() {
        loadView("Settings.fxml");
        setActiveNav(btnSettings);
    }

    @FXML
    private void onLogoutClicked() {
        SessionManager.clearSession();
        SceneManager.showWelcome();
    }

    private void loadView(String fxmlFile) {
        try {
            URL fxmlUrl = getClass().getResource(FXML_PATH + fxmlFile);

            if (fxmlUrl == null) {
                System.err.println("vista no encontrada: " + fxmlFile);
                return;
            }

            Node view = FXMLLoader.load(fxmlUrl);
            contentPane.getChildren().setAll(view);

        } catch (IOException e) {
            System.err.println("error cargando vista: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    //el guard evita duplicar la clase si ya estaba activa
    private void setActiveNav(Button active) {
        btnModules.getStyleClass().remove("nav-item-active");
        btnInstalls.getStyleClass().remove("nav-item-active");
        btnLicences.getStyleClass().remove("nav-item-active");
        btnSettings.getStyleClass().remove("nav-item-active");

        if (!active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }
    }

    private void updateLabels() {
        btnModules.setText(TranslationManager.get("nav.modules"));
        btnInstalls.setText(TranslationManager.get("nav.installs"));
        btnLicences.setText(TranslationManager.get("nav.licences"));
        btnSettings.setText(TranslationManager.get("nav.settings"));
        btnLogout.setText(TranslationManager.get("btn.logout"));
    }

    @Override
    public void onLanguageChanged(String newLanguageCode) {
        updateLabels();
    }
}