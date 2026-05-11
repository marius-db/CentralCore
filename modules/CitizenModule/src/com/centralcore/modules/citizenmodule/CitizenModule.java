package com.centralcore.modules.citizenmodule;

import com.centralcore.modules.Module;
import com.centralcore.modules.ModuleConfig;
import com.google.gson.Gson;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.File;
import java.io.FileReader;

public class CitizenModule implements Module {

    private Parent uiRoot;
    private CitizenModuleController controller;

    //directorio del modulo inyectado por ModuleLoader
    private File moduleDir;

    //config leida de module.json, cargada en setModuleDir
    private ModuleConfig config;

    @Override
    public void setModuleDir(File dir) {
        this.moduleDir = dir;
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(moduleDir, "module.json");

        if (configFile.exists()) {
            //modo desarrollo: leer desde disco
            try {
                config = new Gson().fromJson(new FileReader(configFile), ModuleConfig.class);
                return;
            } catch (Exception e) {
                System.err.println("error al leer module.json de CitizenModule: " + e.getMessage());
            }
        } else {
            //modo JAR: leer desde dentro del JAR via classloader
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/module.json");
                if (is != null) {
                    config = new Gson().fromJson(new java.io.InputStreamReader(is), ModuleConfig.class);
                    return;
                }
            } catch (Exception e) {
                System.err.println("error al leer module.json desde JAR: " + e.getMessage());
            }
        }

        //ultimo recurso: valores hardcodeados
        config = new ModuleConfig();
        config.id = "citizen_module";
        config.name = "Base de Datos Ciudadanos";
        config.version = "0.1.0";
        config.description = "Gestionar los registros y perfiles de los ciudadanos de la ciudad.";
        config.logoPath = "images/logo.png";
        config.author = "CentralCore Team";
    }

    @Override
    public String getModuleId() {
        return config != null ? config.id : "citizen_module";
    }

    @Override
    public String getName() {
        return config != null ? config.name : "Base de Datos Ciudadanos";
    }

    @Override
    public String getVersion() {
        return config != null ? config.version : "0.1.0";
    }

    @Override
    public String getDescription() {
        return config != null ? config.description : "";
    }

    @Override
    public String getLogoPath() {
        return config != null ? config.logoPath : "images/logo.png";
    }

    //devuelve el archivo de imagen de logo resuelto desde el directorio del modulo
    public File getLogoFile() {
        if (moduleDir == null || config == null) return null;
        File f = new File(moduleDir, "resources/" + config.logoPath);
        return f.exists() ? f : null;
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
    public void reload() throws Exception {
        shutdown();
        initialize();
    }

    @Override
    public Parent getMainUI() throws Exception {
        return uiRoot;
    }
}