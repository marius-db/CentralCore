package com.centralcore.modules.citizenmodule;

import com.centralcore.modules.Module;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class CitizenModule implements Module {

    private Parent uiRoot;
    private CitizenModuleController controller;

    @Override
    public String getModuleId() {
        return "citizen_module";
    }

    @Override
    public String getName() {
        return "Citizen Database";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Manage city citizen records and documents";
    }

    @Override
    public String getLogoPath() {
        return "images/logo.png";
    }

    @Override
    public void initialize() throws Exception {
        //inicializa el schema del modulo antes de cargar la ui
        CitizenDAO.initSchema();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
        loader.setClassLoader(getClass().getClassLoader());
        uiRoot = loader.load();
        controller = loader.getController();
        System.out.println("citizen module initialized");
    }

    @Override
    public void shutdown() {
        uiRoot = null;
        controller = null;
        System.out.println("citizen module shut down");
    }

    @Override
    public Parent getMainUI() throws Exception {
        return uiRoot;
    }
}