package com.centralcore.modules;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

/**
 * dynamically loads modules from the modules folder using java reflection
 * carga dinamicamente modulos desde la carpeta de modulos usando reflexion de java
 *
 * scans for module.json files, reads config, instantiates the main class
 * the app doesn't need to know about specific modules, this loader finds them all
 * escanea archivos module.json, lee la configuracion, instancia la clase principal
 * la app no necesita saber sobre modulos especificos, este cargador los encuentra todos
 */
public class ModuleLoader {

    private static final Gson gson = new Gson();

    //find modules in the modules folder relative to app home
    //busca modulos en la carpeta de modulos relativa al home de la app
    private static final String MODULES_FOLDER = "modules";

    //no instantiation / sin instanciacion
    private ModuleLoader() {}

    /**
     * discovers and loads all modules from the modules folder
     * devuelve una lista de modulos cargados exitosamente
     * si una carpeta no tiene module.json o no es valida, se ignora
     *
     * discovers and loads all modules from the modules folder
     * returns a list of successfully loaded modules
     * if a folder has no module.json or is invalid, it's skipped
     */
    public static List<Module> loadAllModules() {
        List<Module> loadedModules = new ArrayList<>();

        try {
            Path modulesPath = Paths.get(MODULES_FOLDER);

            //create modules folder if it doesn't exist
            //crea la carpeta de modulos si no existe
            if (!Files.exists(modulesPath)) {
                Files.createDirectories(modulesPath);
                System.out.println("modules folder created at: " + modulesPath.toAbsolutePath());
                return loadedModules;
            }

            //scan each subdirectory in modules folder
            //escanea cada subdirectorio en la carpeta de modulos
            File modulesDir = modulesPath.toFile();
            File[] subdirs = modulesDir.listFiles(File::isDirectory);

            if (subdirs == null || subdirs.length == 0) {
                System.out.println("no modules found in " + modulesPath.toAbsolutePath());
                return loadedModules;
            }

            //try to load each module
            //intenta cargar cada modulo
            for (File moduleDir : subdirs) {
                Module module = loadModule(moduleDir);
                if (module != null) {
                    loadedModules.add(module);
                    System.out.println("loaded module: " + module.getName() + " v" + module.getVersion());
                }
            }

        } catch (Exception e) {
            System.err.println("error scanning modules folder / error al escanear carpeta de modulos: " + e.getMessage());
            e.printStackTrace();
        }

        return loadedModules;
    }

    /**
     * loads a single module from a directory
     * returns the loaded Module or null if something went wrong
     * carga un modulo individual desde un directorio
     * devuelve el Modulo cargado o null si algo fallo
     */
    private static Module loadModule(File moduleDir) {
        try {
            //look for module.json
            //busca module.json
            File configFile = new File(moduleDir, "module.json");
            if (!configFile.exists()) {
                System.out.println("skipping " + moduleDir.getName() + ": no module.json found");
                return null;
            }

            //parse the config
            //parsea la configuracion
            ModuleConfig config = gson.fromJson(new FileReader(configFile), ModuleConfig.class);

            if (config.mainClass == null || config.mainClass.isEmpty()) {
                System.err.println("invalid module config in " + moduleDir.getName() + ": missing mainClass");
                return null;
            }

            //load the main class using reflection
            //carga la clase principal usando reflexion
            Class<?> moduleClass = Class.forName(config.mainClass);

            //check it implements Module
            //verifica que implemente Module
            if (!Module.class.isAssignableFrom(moduleClass)) {
                System.err.println(config.mainClass + " does not implement Module interface");
                return null;
            }

            //instantiate with no-arg constructor
            //instancia con constructor sin argumentos
            Constructor<?> constructor = moduleClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Module module = (Module) constructor.newInstance();

            return module;

        } catch (ClassNotFoundException e) {
            System.err.println("module class not found in " + moduleDir.getName() + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("error loading module from " + moduleDir.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
