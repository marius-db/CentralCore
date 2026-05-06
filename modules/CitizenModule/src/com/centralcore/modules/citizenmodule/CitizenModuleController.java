package com.centralcore.modules.citizenmodule;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CitizenModuleController implements Initializable {

    //tabla
    @FXML private TextField searchField;
    @FXML private TableView<Citizen> citizenTable;
    @FXML private TableColumn<Citizen, String> colDni;
    @FXML private TableColumn<Citizen, String> colName;
    @FXML private TableColumn<Citizen, String> colMunicipality;
    @FXML private TableColumn<Citizen, String> colPhone;
    @FXML private Button btnNew;

    //panel derecho
    @FXML private StackPane rightPanel;
    @FXML private VBox emptyState;
    @FXML private ScrollPane detailView;
    @FXML private ScrollPane editView;

    //detalle (modo lectura)
    @FXML private Label detailName;
    @FXML private Label detailDni;
    @FXML private Label detailBirthDate;
    @FXML private Label detailNationality;
    @FXML private Label detailGender;
    @FXML private Label detailAddress;
    @FXML private Label detailMunicipality;
    @FXML private Label detailPostalCode;
    @FXML private Label detailPhone;
    @FXML private Label detailEmail;
    @FXML private Label detailMaritalStatus;
    @FXML private Label detailStatus;

    //documentos
    @FXML private ListView<CitizenDocument> docList;
    @FXML private ComboBox<String> docTypeCombo;

    //formulario editar/crear
    @FXML private Label editTitle;
    @FXML private TextField editDni;
    @FXML private TextField editFirstName;
    @FXML private TextField editLastName;
    @FXML private DatePicker editBirthDate;
    @FXML private TextField editBirthPlace;
    @FXML private TextField editNationality;
    @FXML private ComboBox<String> editGender;
    @FXML private TextField editAddress;
    @FXML private TextField editMunicipality;
    @FXML private TextField editPostalCode;
    @FXML private TextField editPhone;
    @FXML private TextField editEmail;
    @FXML private ComboBox<String> editMaritalStatus;
    @FXML private CheckBox editActive;
    @FXML private Label editError;

    private final CitizenDAO dao = new CitizenDAO();
    private final ObservableList<Citizen> citizenData = FXCollections.observableArrayList();
    private Citizen selectedCitizen = null;
    private boolean isNewRecord = false;

    private static final String[] DOC_TYPES = {
            "DNI / Pasaporte",
            "Certificado de Empadronamiento",
            "Certificado de Nacimiento",
            "Certificado de Matrimonio",
            "Certificado de Defunción",
            "Permiso de Residencia",
            "Tarjeta de Identidad Extranjera",
            "Licencia de Conducir",
            "Otro"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupForm();
        setupDocumentPanel();
        loadAllCitizens();
        showEmptyState();

        searchField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) loadAllCitizens();
            else filterCitizens(val.trim());
        });
    }

    //configuración
    private void setupTable() {
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colName.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getNombreCompleto()));
        colMunicipality.setCellValueFactory(new PropertyValueFactory<>("municipio"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        citizenTable.setItems(citizenData);
        citizenTable.setPlaceholder(new Label("No se encontraron ciudadanos"));

        citizenTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                selectedCitizen = selected;
                showDetailView(selected);
            }
        });
    }

    private void setupForm() {
        editGender.setItems(FXCollections.observableArrayList("M", "F", "Otro"));
        editMaritalStatus.setItems(FXCollections.observableArrayList(
                "Soltero/a", "Casado/a", "Divorciado/a", "Viudo/a", "Pareja de hecho"
        ));
    }

    private void setupDocumentPanel() {
        docTypeCombo.setItems(FXCollections.observableArrayList(DOC_TYPES));
        docTypeCombo.getSelectionModel().selectFirst();

        docList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CitizenDocument doc, boolean empty) {
                super.updateItem(doc, empty);
                if (empty || doc == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    Label name = new Label(doc.getNombreArchivo());
                    name.getStyleClass().add("doc-name");
                    Label type = new Label(doc.getTipoDocumento());
                    type.getStyleClass().add("doc-type");
                    box.getChildren().addAll(name, type);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
    }

    //cargar datos
    private void loadAllCitizens() {
        citizenData.setAll(dao.getAll());
    }

    private void filterCitizens(String query) {
        citizenData.setAll(dao.search(query));
    }

    //visibilidad del panel
    private void showEmptyState() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        detailView.setVisible(false);
        detailView.setManaged(false);
        editView.setVisible(false);
        editView.setManaged(false);
    }

    private void showDetailView(Citizen c) {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        detailView.setVisible(true);
        detailView.setManaged(true);
        editView.setVisible(false);
        editView.setManaged(false);

        populateDetailPanel(c);
        loadDocuments(c.getId());
    }

    private void showEditView(boolean isNew) {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        detailView.setVisible(false);
        detailView.setManaged(false);
        editView.setVisible(true);
        editView.setManaged(true);

        editTitle.setText(isNew ? "Nuevo Ciudadano" : "Editar Ciudadano");
        editError.setVisible(false);

        if (isNew) {
            clearForm();
        } else if (selectedCitizen != null) {
            populateForm(selectedCitizen);
        }
    }

    //rellenar

    private void populateDetailPanel(Citizen c) {
        detailName.setText(c.getNombreCompleto());
        detailDni.setText(orDash(c.getDni()));
        detailBirthDate.setText(
                c.getFechaNacimiento() != null
                        ? c.getFechaNacimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + (c.getLugarNacimiento() != null && !c.getLugarNacimiento().isBlank()
                        ? "  ·  " + c.getLugarNacimiento() : "")
                        : "—"
        );
        detailNationality.setText(orDash(c.getNacionalidad()));
        detailGender.setText(orDash(c.getSexo()));
        detailAddress.setText(orDash(c.getDireccion()));
        detailMunicipality.setText(orDash(c.getMunicipio()));
        detailPostalCode.setText(orDash(c.getCodigoPostal()));
        detailPhone.setText(orDash(c.getTelefono()));
        detailEmail.setText(orDash(c.getEmail()));
        detailMaritalStatus.setText(orDash(c.getEstadoCivil()));
        detailStatus.setText(c.isActivo() ? "Activo" : "Inactivo");
        detailStatus.getStyleClass().removeAll("status-active", "status-inactive");
        detailStatus.getStyleClass().add(c.isActivo() ? "status-active" : "status-inactive");
    }

    private void populateForm(Citizen c) {
        editDni.setText(c.getDni());
        editFirstName.setText(c.getNombre());
        editLastName.setText(c.getApellidos());
        editBirthDate.setValue(c.getFechaNacimiento());
        editBirthPlace.setText(orEmpty(c.getLugarNacimiento()));
        editNationality.setText(orEmpty(c.getNacionalidad()));
        editGender.setValue(c.getSexo());
        editAddress.setText(orEmpty(c.getDireccion()));
        editMunicipality.setText(orEmpty(c.getMunicipio()));
        editPostalCode.setText(orEmpty(c.getCodigoPostal()));
        editPhone.setText(orEmpty(c.getTelefono()));
        editEmail.setText(orEmpty(c.getEmail()));
        editMaritalStatus.setValue(c.getEstadoCivil());
        editActive.setSelected(c.isActivo());
    }

    private void clearForm() {
        editDni.clear();
        editFirstName.clear();
        editLastName.clear();
        editBirthDate.setValue(null);
        editBirthPlace.clear();
        editNationality.clear();
        editGender.getSelectionModel().clearSelection();
        editAddress.clear();
        editMunicipality.clear();
        editPostalCode.clear();
        editPhone.clear();
        editEmail.clear();
        editMaritalStatus.getSelectionModel().clearSelection();
        editActive.setSelected(true);
    }

    //acciones tabla

    @FXML
    private void onNew() {
        isNewRecord = true;
        selectedCitizen = null;
        citizenTable.getSelectionModel().clearSelection();
        showEditView(true);
    }

    //acciones detalle

    @FXML
    private void onEdit() {
        if (selectedCitizen == null) return;
        isNewRecord = false;
        showEditView(false);
    }

    @FXML
    private void onDelete() {
        if (selectedCitizen == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("¿Eliminar a " + selectedCitizen.getNombreCompleto() + "?");
        confirm.setContentText("Se eliminarán también todos sus documentos. Esta acción no se puede deshacer.");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                //eliminar archivos físicos del disco
                deleteDocumentFiles(selectedCitizen.getId());
                dao.delete(selectedCitizen.getId());
                selectedCitizen = null;
                loadAllCitizens();
                showEmptyState();
            } catch (Exception e) {
                showError("Error al eliminar ciudadano: " + e.getMessage());
            }
        }
    }

    //acciones formulario

    @FXML
    private void onSave() {
        if (!validateForm()) return;

        Citizen c = isNewRecord ? new Citizen() : selectedCitizen;
        readFormInto(c);

        try {
            if (isNewRecord) {
                dao.insert(c);
            } else {
                dao.update(c);
            }
            selectedCitizen = c;
            loadAllCitizens();
            //reseleccionar en la tabla
            for (Citizen item : citizenData) {
                if (item.getId() == c.getId()) {
                    citizenTable.getSelectionModel().select(item);
                    break;
                }
            }
            showDetailView(c);
        } catch (Exception e) {
            editError.setText("Error al guardar: " + e.getMessage());
            editError.setVisible(true);
        }
    }

    @FXML
    private void onCancel() {
        if (selectedCitizen != null) {
            showDetailView(selectedCitizen);
        } else {
            showEmptyState();
        }
    }

    //validación

    private boolean validateForm() {
        editError.setVisible(false);

        if (editDni.getText().isBlank()) {
            editError.setText("El DNI/NIE es obligatorio");
            editError.setVisible(true);
            return false;
        }
        if (editFirstName.getText().isBlank()) {
            editError.setText("El nombre es obligatorio");
            editError.setVisible(true);
            return false;
        }
        if (editLastName.getText().isBlank()) {
            editError.setText("Los apellidos son obligatorios");
            editError.setVisible(true);
            return false;
        }
        if (editBirthDate.getValue() == null) {
            editError.setText("La fecha de nacimiento es obligatoria");
            editError.setVisible(true);
            return false;
        }
        if (editBirthDate.getValue().isAfter(LocalDate.now())) {
            editError.setText("La fecha de nacimiento no puede ser futura");
            editError.setVisible(true);
            return false;
        }
        return true;
    }

    private void readFormInto(Citizen c) {
        c.setDni(editDni.getText().trim().toUpperCase());
        c.setNombre(editFirstName.getText().trim());
        c.setApellidos(editLastName.getText().trim());
        c.setFechaNacimiento(editBirthDate.getValue());
        c.setLugarNacimiento(editBirthPlace.getText().trim());
        c.setNacionalidad(editNationality.getText().trim());
        c.setSexo(editGender.getValue());
        c.setDireccion(editAddress.getText().trim());
        c.setMunicipio(editMunicipality.getText().trim());
        c.setCodigoPostal(editPostalCode.getText().trim());
        c.setTelefono(editPhone.getText().trim());
        c.setEmail(editEmail.getText().trim());
        c.setEstadoCivil(editMaritalStatus.getValue());
        c.setActivo(editActive.isSelected());
    }

    //documentos

    private void loadDocuments(int citizenId) {
        List<CitizenDocument> docs = dao.getDocuments(citizenId);
        docList.setItems(FXCollections.observableArrayList(docs));
    }

    @FXML
    private void onAddDoc() {
        if (selectedCitizen == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar Documento");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.jpg", "*.jpeg", "*.png", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        Stage stage = (Stage) docList.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        try {
            //copia el archivo a ~/.centralcore/documents/{citizenId}/
            Path destDir = Paths.get(System.getProperty("user.home"), ".centralcore", "documents",
                    String.valueOf(selectedCitizen.getId()));
            Files.createDirectories(destDir);

            //si un archivo con ese nombre ya existe, se añade una marca de tiempo para evitar sobrescribir.
            String fileName = file.getName();
            Path dest = destDir.resolve(fileName);
            if (Files.exists(dest)) {
                String ts = String.valueOf(System.currentTimeMillis());
                String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
                fileName = base + "_" + ts + ext;
                dest = destDir.resolve(fileName);
            }

            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            CitizenDocument doc = new CitizenDocument();
            doc.setCitizenId(selectedCitizen.getId());
            doc.setTipoDocumento(docTypeCombo.getValue() != null ? docTypeCombo.getValue() : "Otro");
            doc.setNombreArchivo(fileName);
            doc.setRutaArchivo(dest.toString());

            dao.insertDocument(doc);
            loadDocuments(selectedCitizen.getId());

        } catch (Exception e) {
            showError("Error al subir documento: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenDoc() {
        CitizenDocument doc = docList.getSelectionModel().getSelectedItem();
        if (doc == null) return;

        File file = new File(doc.getRutaArchivo());
        if (!file.exists()) {
            showError("El archivo ya no existe en disco: " + doc.getRutaArchivo());
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showError("No se puede abrir el archivo: operación no soportada en este sistema");
            }
        } catch (IOException e) {
            showError("Error al abrir archivo: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteDoc() {
        CitizenDocument doc = docList.getSelectionModel().getSelectedItem();
        if (doc == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar documento");
        confirm.setHeaderText("Eliminar " + doc.getNombreArchivo() + "?");
        confirm.setContentText("El archivo también se borrará del disco.");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                //eliminando el archivo del disco
                File file = new File(doc.getRutaArchivo());
                if (file.exists()) file.delete();

                dao.deleteDocument(doc.getId());
                loadDocuments(selectedCitizen.getId());
            } catch (Exception e) {
                showError("Error al eliminar documento: " + e.getMessage());
            }
        }
    }

    //helpers

    private void deleteDocumentFiles(int citizenId) {
        List<CitizenDocument> docs = dao.getDocuments(citizenId);
        for (CitizenDocument doc : docs) {
            File f = new File(doc.getRutaArchivo());
            if (f.exists()) f.delete();
        }
        //directorio del ciudadano
        Path dir = Paths.get(System.getProperty("user.home"), ".centralcore", "documents",
                String.valueOf(citizenId));
        try {
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        alert.getDialogPane().setStyle(
                "-fx-background-color: #1c1f2b; -fx-border-color: #252a3a; -fx-border-width: 1;"
        );
        Node label = alert.getDialogPane().lookup(".content.label");
        if (label != null) {
            label.setStyle("-fx-text-fill: #f0f2f8;");
        }
    }

    private String orDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String orEmpty(String s) {
        return s == null ? "" : s;
    }
}