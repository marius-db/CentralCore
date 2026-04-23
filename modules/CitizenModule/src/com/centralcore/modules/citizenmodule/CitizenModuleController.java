package com.centralcore.modules.citizenmodule;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;import javafx.scene.layout.*;
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

    //--- tabla ---
    @FXML private TextField searchField;
    @FXML private TableView<Citizen> citizenTable;
    @FXML private TableColumn<Citizen, String> colDni;
    @FXML private TableColumn<Citizen, String> colNombre;
    @FXML private TableColumn<Citizen, String> colMunicipio;
    @FXML private TableColumn<Citizen, String> colTelefono;
    @FXML private Button btnNuevo;

    //--- panel derecho: modos ---
    @FXML private StackPane rightPanel;
    @FXML private VBox emptyState;
    @FXML private ScrollPane detailView;
    @FXML private ScrollPane editView;

    //--- detalle (modo lectura) ---
    @FXML private Label detailNombre;
    @FXML private Label detailDni;
    @FXML private Label detailNacimiento;
    @FXML private Label detailNacionalidad;
    @FXML private Label detailSexo;
    @FXML private Label detailDireccion;
    @FXML private Label detailMunicipio;
    @FXML private Label detailCp;
    @FXML private Label detailTelefono;
    @FXML private Label detailEmail;
    @FXML private Label detailEstadoCivil;
    @FXML private Label detailEstado;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    //--- documentos (en detailView) ---
    @FXML private ListView<CitizenDocument> docList;
    @FXML private ComboBox<String> docTypeCombo;
    @FXML private Button btnAddDoc;
    @FXML private Button btnOpenDoc;
    @FXML private Button btnDeleteDoc;

    //--- formulario edicion/creacion ---
    @FXML private Label editTitle;
    @FXML private TextField editDni;
    @FXML private TextField editNombre;
    @FXML private TextField editApellidos;
    @FXML private DatePicker editFechaNac;
    @FXML private TextField editLugarNac;
    @FXML private TextField editNacionalidad;
    @FXML private ComboBox<String> editSexo;
    @FXML private TextField editDireccion;
    @FXML private TextField editMunicipio;
    @FXML private TextField editCp;
    @FXML private TextField editTelefono;
    @FXML private TextField editEmail;
    @FXML private ComboBox<String> editEstadoCivil;
    @FXML private CheckBox editActivo;
    @FXML private Label editError;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

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

    //--- setup ---

    private void setupTable() {
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colNombre.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getNombreCompleto()));
        colMunicipio.setCellValueFactory(new PropertyValueFactory<>("municipio"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

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
        editSexo.setItems(FXCollections.observableArrayList("M", "F", "Otro"));
        editEstadoCivil.setItems(FXCollections.observableArrayList(
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

    //--- carga de datos ---

    private void loadAllCitizens() {
        citizenData.setAll(dao.getAll());
    }

    private void filterCitizens(String query) {
        citizenData.setAll(dao.search(query));
    }

    //--- visibilidad de paneles ---

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

    //--- populate ---

    private void populateDetailPanel(Citizen c) {
        detailNombre.setText(c.getNombreCompleto());
        detailDni.setText(orDash(c.getDni()));
        detailNacimiento.setText(
                c.getFechaNacimiento() != null
                        ? c.getFechaNacimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + (c.getLugarNacimiento() != null && !c.getLugarNacimiento().isBlank()
                        ? "  ·  " + c.getLugarNacimiento() : "")
                        : "—"
        );
        detailNacionalidad.setText(orDash(c.getNacionalidad()));
        detailSexo.setText(orDash(c.getSexo()));
        detailDireccion.setText(orDash(c.getDireccion()));
        detailMunicipio.setText(orDash(c.getMunicipio()));
        detailCp.setText(orDash(c.getCodigoPostal()));
        detailTelefono.setText(orDash(c.getTelefono()));
        detailEmail.setText(orDash(c.getEmail()));
        detailEstadoCivil.setText(orDash(c.getEstadoCivil()));
        detailEstado.setText(c.isActivo() ? "Activo" : "Inactivo");
        detailEstado.getStyleClass().removeAll("status-active", "status-inactive");
        detailEstado.getStyleClass().add(c.isActivo() ? "status-active" : "status-inactive");
    }

    private void populateForm(Citizen c) {
        editDni.setText(c.getDni());
        editNombre.setText(c.getNombre());
        editApellidos.setText(c.getApellidos());
        editFechaNac.setValue(c.getFechaNacimiento());
        editLugarNac.setText(orEmpty(c.getLugarNacimiento()));
        editNacionalidad.setText(orEmpty(c.getNacionalidad()));
        editSexo.setValue(c.getSexo());
        editDireccion.setText(orEmpty(c.getDireccion()));
        editMunicipio.setText(orEmpty(c.getMunicipio()));
        editCp.setText(orEmpty(c.getCodigoPostal()));
        editTelefono.setText(orEmpty(c.getTelefono()));
        editEmail.setText(orEmpty(c.getEmail()));
        editEstadoCivil.setValue(c.getEstadoCivil());
        editActivo.setSelected(c.isActivo());
    }

    private void clearForm() {
        editDni.clear();
        editNombre.clear();
        editApellidos.clear();
        editFechaNac.setValue(null);
        editLugarNac.clear();
        editNacionalidad.clear();
        editSexo.getSelectionModel().clearSelection();
        editDireccion.clear();
        editMunicipio.clear();
        editCp.clear();
        editTelefono.clear();
        editEmail.clear();
        editEstadoCivil.getSelectionModel().clearSelection();
        editActivo.setSelected(true);
    }

    //--- acciones tabla ---

    @FXML
    private void onNuevo() {
        isNewRecord = true;
        selectedCitizen = null;
        citizenTable.getSelectionModel().clearSelection();
        showEditView(true);
    }

    //--- acciones detalle ---

    @FXML
    private void onEditar() {
        if (selectedCitizen == null) return;
        isNewRecord = false;
        showEditView(false);
    }

    @FXML
    private void onEliminar() {
        if (selectedCitizen == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("¿Eliminar a " + selectedCitizen.getNombreCompleto() + "?");
        confirm.setContentText("Se eliminarán también todos sus documentos. Esta acción no se puede deshacer.");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                //eliminar archivos fisicos del disco
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

    //--- acciones formulario ---

    @FXML
    private void onGuardar() {
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
    private void onCancelar() {
        if (selectedCitizen != null) {
            showDetailView(selectedCitizen);
        } else {
            showEmptyState();
        }
    }

    //--- validacion ---

    private boolean validateForm() {
        editError.setVisible(false);

        if (editDni.getText().isBlank()) {
            editError.setText("El DNI/NIE es obligatorio");
            editError.setVisible(true);
            return false;
        }
        if (editNombre.getText().isBlank()) {
            editError.setText("El nombre es obligatorio");
            editError.setVisible(true);
            return false;
        }
        if (editApellidos.getText().isBlank()) {
            editError.setText("Los apellidos son obligatorios");
            editError.setVisible(true);
            return false;
        }
        if (editFechaNac.getValue() == null) {
            editError.setText("La fecha de nacimiento es obligatoria");
            editError.setVisible(true);
            return false;
        }
        if (editFechaNac.getValue().isAfter(LocalDate.now())) {
            editError.setText("La fecha de nacimiento no puede ser futura");
            editError.setVisible(true);
            return false;
        }
        return true;
    }

    private void readFormInto(Citizen c) {
        c.setDni(editDni.getText().trim().toUpperCase());
        c.setNombre(editNombre.getText().trim());
        c.setApellidos(editApellidos.getText().trim());
        c.setFechaNacimiento(editFechaNac.getValue());
        c.setLugarNacimiento(editLugarNac.getText().trim());
        c.setNacionalidad(editNacionalidad.getText().trim());
        c.setSexo(editSexo.getValue());
        c.setDireccion(editDireccion.getText().trim());
        c.setMunicipio(editMunicipio.getText().trim());
        c.setCodigoPostal(editCp.getText().trim());
        c.setTelefono(editTelefono.getText().trim());
        c.setEmail(editEmail.getText().trim());
        c.setEstadoCivil(editEstadoCivil.getValue());
        c.setActivo(editActivo.isSelected());
    }

    //--- documentos ---

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

            //si ya existe un archivo con ese nombre se le añade timestamp para no pisar
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
        confirm.setHeaderText("¿Eliminar " + doc.getNombreArchivo() + "?");
        confirm.setContentText("El archivo también se borrará del disco.");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                //borra el archivo fisico
                File file = new File(doc.getRutaArchivo());
                if (file.exists()) file.delete();

                dao.deleteDocument(doc.getId());
                loadDocuments(selectedCitizen.getId());
            } catch (Exception e) {
                showError("Error al eliminar documento: " + e.getMessage());
            }
        }
    }

    //--- helpers ---

    private void deleteDocumentFiles(int citizenId) {
        List<CitizenDocument> docs = dao.getDocuments(citizenId);
        for (CitizenDocument doc : docs) {
            File f = new File(doc.getRutaArchivo());
            if (f.exists()) f.delete();
        }
        //directorio del ciudadano
        Path dir = Paths.get(System.getProperty("user.home"), ".centralcore", "documents",
                String.valueOf(citizenId));
        try { Files.deleteIfExists(dir); } catch (IOException ignored) {}
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

    private String orDash(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String orEmpty(String s) { return s == null ? "" : s; }
}