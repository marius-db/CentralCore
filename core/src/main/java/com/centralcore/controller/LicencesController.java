package com.centralcore.controller;

import com.centralcore.model.Licence;
import com.centralcore.util.LicenceStorage;
import com.centralcore.util.LicenseValidator;
import com.centralcore.util.TranslationManager;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
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
        //reemplazar los botones built-in de textinputdialog con translated buttontype
        ButtonType btnOkInput     = new ButtonType(TranslationManager.get("dialog.btn.ok"),     ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelInput = new ButtonType(TranslationManager.get("dialog.btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        TextInputDialog keyDialog = new TextInputDialog();
        keyDialog.setTitle(TranslationManager.get("licences.add.title"));
        keyDialog.setHeaderText(TranslationManager.get("licences.add.header"));
        keyDialog.setContentText(TranslationManager.get("licences.add.prompt"));
        keyDialog.getDialogPane().getButtonTypes().setAll(btnOkInput, btnCancelInput);
        //aplicar tema antes y después de mostrar para cubrir todos los nodos del scene graph
        applyDialogTheme(keyDialog.getDialogPane());

        String key = keyDialog.showAndWait().orElse(null);
        if (key == null || key.isBlank()) return;

        if (!LicenseValidator.validate(key)) {
            ButtonType btnOk = new ButtonType(TranslationManager.get("dialog.btn.ok"), ButtonBar.ButtonData.OK_DONE);
            Alert err = new Alert(Alert.AlertType.ERROR,
                    TranslationManager.get("licences.error.invalid"), btnOk);
            applyDialogTheme(err.getDialogPane());
            err.showAndWait();
            return;
        }

        //verificar que el email de la licencia coincide con el usuario en sesión
        String tokenEmail = LicenseValidator.extractEmail(key);
        String userEmail = com.centralcore.util.SessionManager.getCurrentUser() != null
                ? com.centralcore.util.SessionManager.getCurrentUser().getEmail()
                : "";
        if (!tokenEmail.equals(userEmail)) {
            ButtonType btnOk = new ButtonType(TranslationManager.get("dialog.btn.ok"), ButtonBar.ButtonData.OK_DONE);
            Alert err = new Alert(Alert.AlertType.ERROR,
                    TranslationManager.get("licences.error.emailMismatch"), btnOk);
            applyDialogTheme(err.getDialogPane());
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

        ButtonType btnYes = new ButtonType(TranslationManager.get("dialog.btn.yes"), ButtonBar.ButtonData.YES);
        ButtonType btnNo  = new ButtonType(TranslationManager.get("dialog.btn.no"),  ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                TranslationManager.get("licences.remove.confirm"),
                btnYes, btnNo);
        confirm.setTitle(TranslationManager.get("licences.remove.title"));
        applyDialogTheme(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == btnYes) {
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
        if (lblTitle != null) lblTitle.setText(TranslationManager.get("licences.title"));
        if (lblEmptyTitle != null) lblEmptyTitle.setText(TranslationManager.get("licences.noLicences"));
        if (lblEmptyDesc != null) lblEmptyDesc.setText(TranslationManager.get("licences.noLicencesDesc"));
        if (btnAdd != null) btnAdd.setText(TranslationManager.get("licences.btn.add"));
        if (btnRemove != null) btnRemove.setText(TranslationManager.get("licences.btn.remove"));
        if (btnAddFirst != null) btnAddFirst.setText(TranslationManager.get("licences.addFirst"));
        //refrescar celdas para que el cambio de idioma se refleje en los labels
        if (licenceListView != null) licenceListView.refresh();
    }

    @Override
    public void onLanguageChanged(String newLang) {
        updateLabels();
    }

    //inyecta dialog.css en el scene del diálogo cuando ya está montado
    //los estilos inline se ignoran porque Módena los sobreescribe con mayor especificidad;
    //un stylesheet propio en el scene del diálogo gana correctamente
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