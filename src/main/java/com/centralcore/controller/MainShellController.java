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
 * controller for the main application shell
 * controlador para el shell principal de la aplicacion
 *
 * manages sidebar navigation and dynamically loads module views into the content pane
 * also listens for language changes and updates ui automatically
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

    // path prefix for module fxml files / prefijo de ruta para archivos fxml de modulos
    private static final String FXML_PATH = "/com/centralcore/fxml/";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //register language change listener / registra escuchador de cambio de idioma
        TranslationManager.addLanguageChangeListener(this);

        //register content pane and sidebar with scene manager for module loading
        //registra el panel de contenido y sidebar con el gestor de escenas para carga de modulos
        SceneManager.setMainShellContentPane(contentPane);
        SceneManager.setSidebar(sidebar);

        //initialize the module manager and load all available modules
        //inicializa el gestor de modulos y carga todos los modulos disponibles
        ModuleManager moduleManager = ModuleManager.getInstance();
        moduleManager.loadAndInitializeModules();

        // show the logged in username in the sidebar
        // mostrar el nombre del usuario conectado en el sidebar
        if (SessionManager.getCurrentUser() != null) {
            lblUsername.setText(SessionManager.getCurrentUser().getUsername());
        }

        // load modules view by default on startup
        // cargar vista de modulos por defecto al iniciar
        loadView("ModulesView.fxml");
        setActiveNav(btnModules);
        
        //update button labels with current language / actualiza etiquetas de botones con idioma actual
        updateLabels();
    }

    //sidebar navigation handlers / manejadores de navegacion del sidebar

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
        //clear session and return to welcome screen
        //limpiar sesion y volver a la pantalla de bienvenida
        SessionManager.clearSession();
        SceneManager.showWelcome();
    }

    //private helpers / ayudantes privados

    /**
     * loads an fxml view into the content pane, replacing whatever was there
     * carga una vista fxml en el panel de contenido, reemplazando lo que habia
     *
     * @param fxmlFile the fxml filename e.g. "Modules.fxml"
     */
    private void loadView(String fxmlFile) {
        try {
            URL fxmlUrl = getClass().getResource(FXML_PATH + fxmlFile);

            if (fxmlUrl == null) {
                System.err.println("view not found / vista no encontrada: " + fxmlFile);
                return;
            }

            Node view = FXMLLoader.load(fxmlUrl);

            // clear current content and load the new view
            // limpiar el contenido actual y cargar la nueva vista
            contentPane.getChildren().setAll(view);

        } catch (IOException e) {
            System.err.println("error loading view / error cargando vista: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * highlights the active nav button and removes highlight from others
     * resalta el boton de navegacion activo y elimina el resaltado de los demas
     */
    private void setActiveNav(Button active) {
        // remove active class from all nav buttons
        // eliminar clase activa de todos los botones de navegacion
        btnModules.getStyleClass().remove("nav-item-active");
        btnInstalls.getStyleClass().remove("nav-item-active");
        btnLicences.getStyleClass().remove("nav-item-active");
        btnSettings.getStyleClass().remove("nav-item-active");

        //add active class to the selected button
        //agregar clase activa al boton seleccionado
        if (!active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }
    }

    /**
     * updates all ui labels with translated text from translation manager
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
     * observer callback for language changes from any source
     * devolucuon de observador para cambios de idioma de cualquier fuente
     */
    @Override
    public void onLanguageChanged(String newLanguageCode) {
        updateLabels();
    }
}
