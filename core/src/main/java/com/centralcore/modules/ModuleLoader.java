package com.centralcore.modules;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.net.URLClassLoader;

import com.google.gson.Gson;

/**
 * carga dinamicamente modulos desde la carpeta de modulos usando reflexion de java
 *
 * escanea archivos module.json, lee la configuracion, instancia la clase principal
 * la app no necesita saber sobre modulos especificos, este cargador los encuentra todos
 */
public class ModuleLoader {

    private static final Gson gson = new Gson();

    //busca modulos en la carpeta de modulos relativa al home de la app
    private static final String MODULES_FOLDER = "modules";

    //sin instanciacion
    private ModuleLoader() {}

    /**
     * descubre y carga todos los modulos desde la carpeta de modulos
     * devuelve una lista de modulos cargados exitosamente
     * si una carpeta no tiene module.json o no es valida, se ignora
     */
    public static List<Module> loadAllModules() {
        List<Module> loadedModules = new ArrayList<>();

        try {
            Path modulesPath = Paths.get(MODULES_FOLDER);

            //crea la carpeta de modulos si no existe
            if (!Files.exists(modulesPath)) {
                Files.createDirectories(modulesPath);
                System.out.println("modules folder created at: " + modulesPath.toAbsolutePath());
                return loadedModules;
            }

            //escanea cada subdirectorio en la carpeta de modulos
            File modulesDir = modulesPath.toFile();
            File[] subdirs = modulesDir.listFiles(File::isDirectory);

            if (subdirs == null || subdirs.length == 0) {
                System.out.println("no modules found in " + modulesPath.toAbsolutePath());
                return loadedModules;
            }

            //intenta cargar cada modulo
            for (File moduleDir : subdirs) {
                Module module = loadModule(moduleDir);
                if (module != null) {
                    loadedModules.add(module);
                    System.out.println("loaded module: " + module.getName() + " v" + module.getVersion());
                }
            }

        } catch (Exception e) {
            System.err.println("error al escanear carpeta de modulos: " + e.getMessage());
            e.printStackTrace();
        }

        return loadedModules;
    }

    /**
     * carga un modulo individual desde un directorio
     * devuelve el Modulo cargado o null si algo fallo
     */
    private static Module loadModule(File moduleDir) {
        try {
            //busca module.json
            File configFile = new File(moduleDir, "module.json");
            if (!configFile.exists()) {
                System.out.println("skipping " + moduleDir.getName() + ": no module.json found");
                return null;
            }

            //parsea la configuracion
            ModuleConfig config = gson.fromJson(new FileReader(configFile), ModuleConfig.class);

            // construye la ruta al directorio de clases compiladas del modulo
            File classesDir = new File(moduleDir, "build/classes/java/main");
            System.out.println("DEBUG: Looking for classes in: " + classesDir.getAbsolutePath());
            System.out.println("DEBUG: Classes dir exists? " + classesDir.exists());
            System.out.println("DEBUG: URLClassLoader URL: " + classesDir.toURI().toURL());

            // verifica si el directorio existe
            if (!classesDir.exists()) {
                System.err.println("classes directory not found for " + moduleDir.getName() + ": " + classesDir.getAbsolutePath());
                return null;
            }

            if (config.mainClass == null || config.mainClass.isEmpty()) {
                System.err.println("invalid module config in " + moduleDir.getName() + ": missing mainClass");
                return null;
            }

            // crea un URLClassLoader que apunta al directorio de clases del modulo
            URLClassLoader moduleClassLoader = new URLClassLoader(
                    new URL[]{classesDir.toURI().toURL()},
                    Module.class.getClassLoader()
            );
            System.out.println("DEBUG: Attempting to load class: " + config.mainClass);

            // carga la clase usando el classloader del modulo
            Class<?> moduleClass = Class.forName(config.mainClass, true, moduleClassLoader);

            //verifica que implemente Module
            if (!Module.class.isAssignableFrom(moduleClass)) {
                System.err.println(config.mainClass + " does not implement Module interface");
                return null;
            }

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
