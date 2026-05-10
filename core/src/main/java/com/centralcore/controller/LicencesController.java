package com.centralcore.controller;

import com.centralcore.model.Licence;
import com.centralcore.util.LicenceStorage;
import com.centralcore.util.LicenseValidator;
import com.centralcore.util.TranslationManager;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.Node;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class LicencesController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private ListView<Licence> licenceListView;
    @FXML private VBox emptyState;
    @FXML private Label lblTitle;
    @FXML private Label lblEmptyTitle;
    @FXML private Label lblEmptyDesc;
    @FXML private Button btnRemove;
    @FXML private Button btnAdd;
    @FXML private Button btnAddFirst;

    private final ObservableList<Licence> licences = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TranslationManager.addLanguageChangeListener(this);

        licenceListView.setItems(licences);
        licenceListView.setCellFactory(param -> new LicenceListCell());

        loadSavedLicence();
        updateLabels();
        refreshView();
    }

    @FXML
    private void onAddLicenceClicked() {
        TextInputDialog keyDialog = new TextInputDialog();
        keyDialog.setTitle(TranslationManager.get("licences.add.title"));
        keyDialog.setHeaderText(TranslationManager.get("licences.add.header"));
        keyDialog.setContentText(TranslationManager.get("licences.add.prompt"));
        //aplicar tema antes y despues de mostrar para cubrir todos los nodos del scene graph
        applyDialogTheme(keyDialog.getDialogPane());
        keyDialog.setOnShown(ev -> applyDialogTheme(keyDialog.getDialogPane()));

        String key = keyDialog.showAndWait().orElse(null);
        if (key == null || key.isBlank()) return;

        if (!LicenseValidator.validate(key)) {
            Alert err = new Alert(Alert.AlertType.ERROR,
                    TranslationManager.get("licences.error.invalid"), ButtonType.OK);
            applyDialogTheme(err.getDialogPane());
            err.setOnShown(ev -> applyDialogTheme(err.getDialogPane()));
            err.showAndWait();
            return;
        }

        //verificar que el email de la licencia coincide con el usuario en sesion
        String tokenEmail = LicenseValidator.extractEmail(key);
        String userEmail  = com.centralcore.util.SessionManager.getCurrentUser() != null
                ? com.centralcore.util.SessionManager.getCurrentUser().getEmail()
                : "";
        if (!tokenEmail.equals(userEmail)) {
            Alert err = new Alert(Alert.AlertType.ERROR,
                    TranslationManager.get("licences.error.emailMismatch"), ButtonType.OK);
            applyDialogTheme(err.getDialogPane());
            err.setOnShown(ev -> applyDialogTheme(err.getDialogPane()));
            err.showAndWait();
            return;
        }

        String expiry = LicenseValidator.extractExpiry(key);
        boolean active = LicenseValidator.isActive(key);

        LicenceStorage.saveAppLicence(key, expiry);
        licences.clear();
        licences.add(new Licence("CentralCore", key, expiry, active));
        refreshView();

        //notificar al shell para quitar el velo
        MainShellController.refreshLicenceVeil();
    }

    @FXML
    private void onRemoveLicenceClicked() {
        if (licences.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                TranslationManager.get("licences.remove.confirm"),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle(TranslationManager.get("licences.remove.title"));
        applyDialogTheme(confirm.getDialogPane());
        confirm.setOnShown(ev -> applyDialogTheme(confirm.getDialogPane()));

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                LicenceStorage.removeAppLicence();
                licences.clear();
                refreshView();
                MainShellController.refreshLicenceVeil();
            }
        });
    }

    private void loadSavedLicence() {
        String[] data = LicenceStorage.loadAppLicence();
        if (data != null) {
            boolean active = LicenseValidator.isActive(data[0]);
            licences.add(new Licence("CentralCore", data[0], data[1], active));
        }
    }

    private void refreshView() {
        boolean empty = licences.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        licenceListView.setVisible(!empty);
        licenceListView.setManaged(!empty);
        if (btnRemove != null) {
            btnRemove.setDisable(empty);
        }
    }

    private void updateLabels() {
        if (lblTitle      != null) lblTitle.setText(TranslationManager.get("licences.title"));
        if (lblEmptyTitle != null) lblEmptyTitle.setText(TranslationManager.get("licences.noLicences"));
        if (lblEmptyDesc  != null) lblEmptyDesc.setText(TranslationManager.get("licences.noLicencesDesc"));
        if (btnAdd        != null) btnAdd.setText(TranslationManager.get("licences.btn.add"));
        if (btnRemove     != null) btnRemove.setText(TranslationManager.get("licences.btn.remove"));
        if (btnAddFirst   != null) btnAddFirst.setText(TranslationManager.get("licences.addFirst"));
        //refrescar celdas para que el cambio de idioma se refleje en los labels
        if (licenceListView != null) licenceListView.refresh();
    }

    @Override
    public void onLanguageChanged(String newLang) {
        updateLabels();
    }

    //aplica el tema oscuro a un dialogo de javafx, cubriendo todos sus paneles y nodos hijos
    private void applyDialogTheme(DialogPane pane) {
        //fondo y texto del panel raiz
        pane.setStyle(
                "-fx-background-color: #2c3e50;" +
                        "-fx-border-color: #3d5270;" +
                        "-fx-border-width: 1;"
        );

        //cabecera del dialogo (area gris clara por defecto)
        Node header = pane.lookup(".header-panel");
        if (header != null) {
            header.setStyle(
                    "-fx-background-color: #34495e;" +
                            "-fx-padding: 14 18 14 18;"
            );
        }

        //label del header
        Node headerLabel = pane.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle(
                    "-fx-text-fill: #ecf0f1;" +
                            "-fx-font-size: 14;" +
                            "-fx-font-weight: bold;"
            );
        }

        //todos los labels del contenido
        pane.lookupAll(".label").forEach(n -> {
            if (!n.getStyleClass().contains("header-panel")) {
                n.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12;");
            }
        });

        //campo de texto del TextInputDialog
        pane.lookupAll(".text-field").forEach(n ->
                n.setStyle(
                        "-fx-background-color: #1e2738;" +
                                "-fx-text-fill: #ecf0f1;" +
                                "-fx-prompt-text-fill: #7f8c8d;" +
                                "-fx-border-color: #4a6080;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 4;" +
                                "-fx-background-radius: 4;" +
                                "-fx-padding: 6 10;" +
                                "-fx-font-size: 12;"
                )
        );

        //barra de botones
        Node btnBar = pane.lookup(".button-bar");
        if (btnBar != null) {
            btnBar.setStyle("-fx-background-color: #2c3e50; -fx-padding: 10 14;");
        }

        //botones del dialogo
        pane.lookupAll(".button").forEach(n ->
                n.setStyle(
                        "-fx-background-color: #34495e;" +
                                "-fx-text-fill: #ecf0f1;" +
                                "-fx-border-color: #4a6080;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 4;" +
                                "-fx-background-radius: 4;" +
                                "-fx-padding: 6 14;" +
                                "-fx-font-size: 12;" +
                                "-fx-cursor: hand;"
                )
        );

        //area de contenido (scroll pane interior)
        Node content = pane.lookup(".content");
        if (content != null) {
            content.setStyle("-fx-background-color: #2c3e50; -fx-padding: 14 18;");
        }

    }

    //celda personalizada que imita el estilo de instalaciones
    private class LicenceListCell extends ListCell<Licence> {

        private final HBox container;
        private final VBox infoBox;
        private final Label lblKey;
        private final Label lblExpiry;
        private final Label lblStatus;

        public LicenceListCell() {
            container = new HBox();
            container.setStyle(
                    "-fx-padding: 10; " +
                            "-fx-spacing: 12; " +
                            "-fx-border-color: #34495e; " +
                            "-fx-border-width: 0 0 1 0; " +
                            "-fx-background-color: #2c3e50;"
            );
            container.setMinHeight(70);
            container.setAlignment(Pos.CENTER_LEFT);

            infoBox = new VBox();
            infoBox.setStyle("-fx-spacing: 5;");
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            lblKey = new Label();
            lblKey.setStyle(
                    "-fx-font-size: 13; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #ecf0f1;"
            );

            lblExpiry = new Label();
            lblExpiry.setStyle(
                    "-fx-font-size: 11; " +
                            "-fx-text-fill: #bdc3c7;"
            );

            lblStatus = new Label();

            infoBox.getChildren().addAll(lblKey, lblExpiry, lblStatus);
            container.getChildren().add(infoBox);
        }

        @Override
        protected void updateItem(Licence licence, boolean empty) {
            super.updateItem(licence, empty);

            if (empty || licence == null) {
                setGraphic(null);
            } else {
                //clave censurada: 4 chars reales + asteriscos de longitud falsa
                String visible = licence.getKey().length() > 4
                        ? licence.getKey().substring(0, 4) : licence.getKey();
                int fakeLen = 8 + (licence.getKey().hashCode() & 0xFF) % 7;
                lblKey.setText(visible + "*".repeat(fakeLen));

                String expiryLabel = TranslationManager.get("licences.col.expiry")
                        + ": " + (licence.getExpiry() != null ? licence.getExpiry() : "—");
                lblExpiry.setText(expiryLabel);

                boolean active = licence.isActive();
                lblStatus.setText(active ? "✔ " + TranslationManager.get("licences.col.status")
                        : "✘ " + TranslationManager.get("licences.col.status"));
                lblStatus.setStyle(active
                        ? "-fx-font-size: 11; -fx-text-fill: #22c55e;"
                        : "-fx-font-size: 11; -fx-text-fill: #ef4444;");

                setGraphic(container);
            }
        }
    }
}