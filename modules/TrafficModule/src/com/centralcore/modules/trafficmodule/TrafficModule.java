package com.centralcore.modules.trafficmodule;

import com.centralcore.modules.Module;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TrafficModule implements Module {

    private Parent uiRoot;
    private TrafficModuleController controller;

    @Override
    public String getModuleId() {
        return "traffic_module";
    }

    @Override
    public String getName() {
        return "Traffic Management";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Control de tráfico, semáforos y vehículos de emergencia";
    }

    @Override
    public String getLogoPath() {
        return "images/logo.png";
    }

    @Override
    public void initialize() throws Exception {
        //inicializar schema de incidentes en la base de datos
        TrafficDAO.initSchema();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trafficmodule/fxml/Main.fxml"));
        loader.setClassLoader(getClass().getClassLoader());
        uiRoot = loader.load();
        controller = loader.getController();
        System.out.println("traffic module initialized");
    }

    @Override
    public void shutdown() {
        if (controller != null) controller.onShutdown();
        uiRoot = null;
        controller = null;
        System.out.println("traffic module shut down");
    }

    @Override
    public Parent getMainUI() throws Exception {
        return uiRoot;
    }
}
