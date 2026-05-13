package com.centralcore.util;

import com.centralcore.modules.Module;
import com.centralcore.modules.ModuleManager;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ModuleDetailsDialog {

    public static void show(Stage parentStage, Module module) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(parentStage);
        dialogStage.setTitle(module.getName() + " - " + TranslationManager.get("module.details.title"));
        dialogStage.setWidth(450);
        dialogStage.setHeight(320);

        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color: #2c3e50; " +
                        "-fx-padding: 20; " +
                        "-fx-spacing: 15;"
        );

        Label lblName = new Label(TranslationManager.get("module.details.label.module") + " " + module.getName());
        lblName.setStyle(
                "-fx-font-size: 18; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #ecf0f1;"
        );

        Label lblId = new Label(TranslationManager.get("module.details.label.id") + " " + module.getModuleId());
        lblId.setStyle(
                "-fx-font-size: 12; " +
                        "-fx-text-fill: #bdc3c7;"
        );

        Label lblVersion = new Label(TranslationManager.get("module.details.label.version") + " " + module.getVersion());
        lblVersion.setStyle(
                "-fx-font-size: 12; " +
                        "-fx-text-fill: #bdc3c7;"
        );

        Label lblDescLabel = new Label(TranslationManager.get("module.details.label.description"));
        lblDescLabel.setStyle(
                "-fx-font-size: 13; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #ecf0f1;"
        );

        Label lblDescription = new Label(module.getDescription());
        lblDescription.setStyle(
                "-fx-font-size: 12; " +
                        "-fx-text-fill: #bdc3c7; " +
                        "-fx-wrap-text: true;"
        );
        lblDescription.setWrapText(true);

        //label de estado para mostrar resultado del reload
        Label lblStatus = new Label();
        lblStatus.setStyle("-fx-font-size: 11; -fx-text-fill: #2ecc71;");
        lblStatus.setManaged(false);
        lblStatus.setVisible(false);

        HBox buttonsBox = new HBox();
        buttonsBox.setStyle("-fx-spacing: 10; -fx-alignment: center-right;");
        buttonsBox.setPadding(new Insets(15, 0, 0, 0));

        Button btnUpdate = new Button(TranslationManager.get("module.details.btn.update"));
        btnUpdate.setStyle(
                "-fx-padding: 8 16; " +
                        "-fx-font-size: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white;"
        );
        btnUpdate.setOnAction(e -> {
            //recarga el módulo en caliente sin reiniciar la app
            String moduleId = module.getModuleId();
            Module reloaded = ModuleManager.getInstance().reloadModule(moduleId);

            if (reloaded != null) {
                System.out.println("modulo recargado: " + moduleId);
                lblStatus.setText(TranslationManager.get("module.details.status.reloaded"));
                lblStatus.setStyle("-fx-font-size: 11; -fx-text-fill: #2ecc71;");
            } else {
                System.err.println("fallo al recargar modulo: " + moduleId);
                lblStatus.setText(TranslationManager.get("module.details.status.reloadFailed"));
                lblStatus.setStyle("-fx-font-size: 11; -fx-text-fill: #e74c3c;");
            }

            lblStatus.setManaged(true);
            lblStatus.setVisible(true);
        });

        Button btnDelete = new Button(TranslationManager.get("module.details.btn.delete"));
        btnDelete.setStyle(
                "-fx-padding: 8 16; " +
                        "-fx-font-size: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white;"
        );
        btnDelete.setOnAction(e -> {
            System.out.println("eliminar modulo: " + module.getName());
            dialogStage.close();
        });

        Button btnClose = new Button(TranslationManager.get("module.details.btn.close"));
        btnClose.setStyle(
                "-fx-padding: 8 16; " +
                        "-fx-font-size: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-color: #34495e; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #7f8c8d; " +
                        "-fx-border-width: 1;"
        );
        btnClose.setOnAction(e -> dialogStage.close());

        buttonsBox.getChildren().addAll(btnUpdate, btnDelete, btnClose);

        root.getChildren().addAll(
                lblName,
                lblId,
                lblVersion,
                lblDescLabel,
                lblDescription,
                lblStatus,
                buttonsBox
        );

        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }
}