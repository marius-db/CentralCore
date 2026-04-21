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

/**
 * controlador para el shell principal de la aplicacion
 *
 * gestiona la navegacion del sidebar y carga dinamicamente las vistas de modulos en el panel de contenido
 * tambien escucha cambios de idioma y actualiza la ui automaticamente
 */
public class MainShellController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private VBox      sidebar;
    @FXML private StackPane contentPane;
    @FXML private Label     lblUsername;
    @FXML private Button    btnModules;
    @FXML private Button    btnInstalls;
    @FXML private Button    btnLicences;
    @FXML private Button    btnSettings;
    @FXML private Button    btnLogout;

    // prefijo de ruta para archivos fxml de modulos
    private static final String FXML_PATH = "/com/centralcore/fxml/";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //registra escuchador de cambio de idioma
        TranslationManager.addLanguageChangeListener(this);

        //registra el panel de contenido y sidebar con el gestor de escenas para carga de modulos
        SceneManager.setMainShellContentPane(contentPane);
        SceneManager.setSidebar(sidebar);

        //inicializa el gestor de modulos y carga todos los modulos disponibles
        ModuleManager moduleManager = ModuleManager.getInstance();
        moduleManager.loadAndInitializeModules();


        // mostrar el nombre del usuario conectado en el sidebar
        if (SessionManager.getCurrentUser() != null) {
            lblUsername.setText(SessionManager.getCurrentUser().getUsername());
        }

        // cargar vista de modulos por defecto al iniciar
        loadView("ModulesView.fxml");
        setActiveNav(btnModules);
        
        //actualiza etiquetas de botones con idioma actual
        updateLabels();
    }

    //manejadores de navegacion del sidebar

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
        //limpiar sesion y volver a la pantalla de bienvenida
        SessionManager.clearSession();
        SceneManager.showWelcome();
    }

    //ayudantes privados

    /**
     * carga una vista fxml en el panel de contenido, reemplazando lo que habia
     *
     * @param fxmlFile the fxml filename e.g. "Modules.fxml"
     */
    private void loadView(String fxmlFile) {
        try {
            URL fxmlUrl = getClass().getResource(FXML_PATH + fxmlFile);

            if (fxmlUrl == null) {
                System.err.println("vista no encontrada: " + fxmlFile);
                return;
            }

            Node view = FXMLLoader.load(fxmlUrl);

            // limpiar el contenido actual y cargar la nueva vista
            contentPane.getChildren().setAll(view);

        } catch (IOException e) {
            System.err.println("error cargando vista: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * resalta el boton de navegacion activo y elimina el resaltado de los demas
     */
    private void setActiveNav(Button active) {
        // eliminar clase activa de todos los botones de navegacion
        btnModules.getStyleClass().remove("nav-item-active");
        btnInstalls.getStyleClass().remove("nav-item-active");
        btnLicences.getStyleClass().remove("nav-item-active");
        btnSettings.getStyleClass().remove("nav-item-active");

        //agregar clase activa al boton seleccionado
        if (!active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }
    }

    /**
     * actualiza todas las etiquetas ui con texto traducido del gestor de traducciones
     */
    private void updateLabels() {
        btnModules.setText(TranslationManager.get("nav.modules"));
        btnInstalls.setText(TranslationManager.get("nav.installs"));
        btnLicences.setText(TranslationManager.get("nav.licences"));
        btnSettings.setText(TranslationManager.get("nav.settings"));
        btnLogout.setText(TranslationManager.get("btn.logout"));
    }

    /**
     * devolucuon de observador para cambios de idioma de cualquier fuente
     */
    @Override
    public void onLanguageChanged(String newLanguageCode) {
        updateLabels();
    }
}
