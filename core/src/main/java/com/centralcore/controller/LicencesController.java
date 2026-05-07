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
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LicencesController implements Initializable, TranslationManager.LanguageChangeListener {

    @FXML private TableView<Licence> tableLicences;
    @FXML private TableColumn<Licence, String> colKey;
    @FXML private TableColumn<Licence, String> colExpiry;
    @FXML private TableColumn<Licence, Boolean> colActive;
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

        // columna key con censura: 4 chars reales + asteriscos de longitud falsa
        colKey.setCellValueFactory(c -> c.getValue().keyProperty());
        colKey.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String key, boolean empty) {
                super.updateItem(key, empty);
                if (empty || key == null) {
                    setText(null);
                } else {
                    String visible = key.length() > 4 ? key.substring(0, 4) : key;
                    // longitud de asteriscos engañosa: entre 8 y 14, no el tamaño real
                    int fakeLen = 8 + (key.hashCode() & 0xFF) % 7;
                    setText(visible + "*".repeat(fakeLen));
                    setStyle("-fx-text-fill: -cc-text-secondary;");
                }
            }
        });

        colExpiry.setCellValueFactory(c -> c.getValue().expiryProperty());

        colActive.setCellValueFactory(c -> c.getValue().activeProperty().asObject());
        colActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                } else {
                    setText(active ? "✔" : "✘");
                    setStyle(active
                            ? "-fx-text-fill: -cc-success;"
                            : "-fx-text-fill: -cc-error;");
                }
            }
        });

        tableLicences.setItems(licences);

        // estilo de la tabla para que siga el tema
        tableLicences.setStyle("-fx-background-color: -cc-bg-dark; -fx-border-color: -cc-border;");

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
        applyDialogTheme(keyDialog.getDialogPane());

        String key = keyDialog.showAndWait().orElse(null);
        if (key == null || key.isBlank()) return;

        if (!LicenseValidator.validate(key)) {
            Alert err = new Alert(Alert.AlertType.ERROR,
                    TranslationManager.get("licences.error.invalid"), ButtonType.OK);
            applyDialogTheme(err.getDialogPane());
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
            err.showAndWait();
            return;
        }

        String expiry = LicenseValidator.extractExpiry(key);
        boolean active = LicenseValidator.isActive(key);

        LicenceStorage.saveAppLicence(key, expiry);
        licences.clear();
        licences.add(new Licence("CentralCore", key, expiry, active));
        refreshView();

        // notificar al shell para quitar el velo
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
        tableLicences.setVisible(!empty);
        tableLicences.setManaged(!empty);
        if (btnRemove != null) {
            btnRemove.setDisable(empty);
        }
    }

    private void updateLabels() {
        if (lblTitle    != null) lblTitle.setText(TranslationManager.get("licences.title"));
        if (lblEmptyTitle != null) lblEmptyTitle.setText(TranslationManager.get("licences.noLicences"));
        if (lblEmptyDesc  != null) lblEmptyDesc.setText(TranslationManager.get("licences.noLicencesDesc"));
        if (btnAdd      != null) btnAdd.setText(TranslationManager.get("licences.btn.add"));
        if (btnRemove   != null) btnRemove.setText(TranslationManager.get("licences.btn.remove"));
        if (btnAddFirst != null) btnAddFirst.setText(TranslationManager.get("licences.addFirst"));
        if (colKey    != null) colKey.setText(TranslationManager.get("licences.col.key"));
        if (colExpiry != null) colExpiry.setText(TranslationManager.get("licences.col.expiry"));
        if (colActive != null) colActive.setText(TranslationManager.get("licences.col.status"));
    }

    @Override
    public void onLanguageChanged(String newLang) {
        updateLabels();
    }

    // aplica fondo oscuro a los dialogos para que no rompan el tema
    private void applyDialogTheme(DialogPane pane) {
        pane.setStyle("-fx-background-color: #1c1f2b; -fx-text-fill: #f0f2f8;");
    }
}