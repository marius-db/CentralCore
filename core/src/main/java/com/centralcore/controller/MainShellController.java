package com.centralcore.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.centralcore.modules.ModuleManager;
import com.centralcore.util.LicenceStorage;
import com.centralcore.util.SceneManager;
import com.centralcore.util.SessionManager;
import com.centralcore.util.TranslationManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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
    @FXML private Label lblPlaceholder;

    //velo semitransparente que bloquea el contenido cuando no hay licencia
    private StackPane licenceVeil;

    //referencia estática para que LicencesController pueda actualizar el velo
    private static MainShellController instance;

    private static final String FXML_PATH = "/com/centralcore/fxml/";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        instance = this;
        TranslationManager.addLanguageChangeListener(this);

        SceneManager.setMainShellContentPane(contentPane);
        SceneManager.setSidebar(sidebar);

        ModuleManager moduleManager = ModuleManager.getInstance();
        moduleManager.loadAndInitializeModules();

        if (SessionManager.getCurrentUser() != null) {
            lblUsername.setText(SessionManager.getCurrentUser().getUsername());
        }

        buildLicenceVeil();

        loadView("ModulesView.fxml");
        setActiveNav(btnModules);
        updateLabels();
        applyLicenceVeil();
    }

    //velo de licencia

    private void buildLicenceVeil() {
        licenceVeil = new StackPane();
        licenceVeil.setStyle(
                "-fx-background-color: rgba(13,15,20,0.82);" +
                        "-fx-background-radius: 0;"
        );
        licenceVeil.setAlignment(Pos.CENTER);

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 32; -fx-max-width: 340;");

        Label icon = new Label("🔒");
        icon.setStyle("-fx-font-size: 36;");

        Label msg = new Label(TranslationManager.get("licence.veil.message"));
        msg.setStyle(
                "-fx-font-size: 15; -fx-font-weight: bold;" +
                        "-fx-text-fill: -cc-text-primary; -fx-text-alignment: center; -fx-wrap-text: true;"
        );

        Button btnGo = new Button(TranslationManager.get("licence.veil.button"));
        btnGo.setStyle(
                "-fx-background-color: -cc-blue; -fx-text-fill: white;" +
                        "-fx-padding: 10 24; -fx-font-weight: bold; -fx-cursor: hand;" +
                        "-fx-background-radius: 6;"
        );
        btnGo.setOnAction(e -> {
            loadView("Licences.fxml");
            setActiveNav(btnLicences);
        });

        box.getChildren().addAll(icon, msg, btnGo);
        licenceVeil.getChildren().add(box);

        //el velo se superpone sobre contentPane via StackPane del shell
        //pero contentPane es el StackPane donde van las vistas, así que lo añadimos directamente a él y lo ponemos encima
        licenceVeil.setMouseTransparent(false);
        StackPane.setAlignment(licenceVeil, Pos.CENTER);

        //añadir ya a contentPane (encima de cualquier contenido)
        contentPane.getChildren().add(licenceVeil);
    }

    private void applyLicenceVeil() {
        boolean licensed = LicenceStorage.hasActiveLicence();
        licenceVeil.setVisible(!licensed);
        licenceVeil.setManaged(!licensed);
    }

    //llamado desde LicencesController cuando se añade/quita una licencia
    public static void refreshLicenceVeil() {
        if (instance != null) {
            instance.applyLicenceVeil();
        }
    }

    //navegación

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
        //limpiar sesión y credenciales guardadas para que welcome no ofrezca continuar
        SessionManager.clearSession();
        com.centralcore.util.RememberMeStorage.clear();
        SceneManager.showWelcome();
    }

    void loadView(String fxmlFile) {
        try {
            URL fxmlUrl = getClass().getResource(FXML_PATH + fxmlFile);
            if (fxmlUrl == null) {
                System.err.println("vista no encontrada: " + fxmlFile);
                return;
            }
            Node view = FXMLLoader.load(fxmlUrl);
            //insertar la vista por debajo del velo
            if (contentPane.getChildren().contains(licenceVeil)) {
                contentPane.getChildren().remove(licenceVeil);
                contentPane.getChildren().setAll(view);
                contentPane.getChildren().add(licenceVeil);
            } else {
                contentPane.getChildren().setAll(view);
            }
        } catch (IOException e) {
            System.err.println("error cargando vista: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActiveNav(Button active) {
        btnModules.getStyleClass().remove("nav-item-active");
        btnInstalls.getStyleClass().remove("nav-item-active");
        btnLicences.getStyleClass().remove("nav-item-active");
        btnSettings.getStyleClass().remove("nav-item-active");

        if (!active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }

        //si la pantalla activa es Licences o Settings, el velo no bloquea
        boolean isExempt = (active == btnLicences || active == btnSettings);
        licenceVeil.setMouseTransparent(isExempt);
        licenceVeil.setVisible(!isExempt && !LicenceStorage.hasActiveLicence());
        licenceVeil.setManaged(!isExempt && !LicenceStorage.hasActiveLicence());
    }

    private void updateLabels() {
        btnModules.setText(TranslationManager.get("nav.modules"));
        btnInstalls.setText(TranslationManager.get("nav.installs"));
        btnLicences.setText(TranslationManager.get("nav.licences"));
        btnSettings.setText(TranslationManager.get("nav.settings"));
        btnLogout.setText(TranslationManager.get("btn.logout"));
        if (lblPlaceholder != null) lblPlaceholder.setText(TranslationManager.get("shell.placeholder"));
    }

    @Override
    public void onLanguageChanged(String newLanguageCode) {
        updateLabels();
        //actualizar texto del botón del velo
        if (licenceVeil != null) {
            VBox box = (VBox) licenceVeil.getChildren().get(0);
            Label msg = (Label) box.getChildren().get(1);
            Button btn = (Button) box.getChildren().get(2);
            msg.setText(TranslationManager.get("licence.veil.message"));
            btn.setText(TranslationManager.get("licence.veil.button"));
        }
    }
}