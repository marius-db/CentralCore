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

//descubre y carga modulos desde la carpeta /modules usando reflexion
//cada modulo vive en su propio subdirectorio con un module.json y sus clases compiladas
public class ModuleLoader {

    private static final Gson gson = new Gson();
    private static final String MODULES_FOLDER = "modules";

    private ModuleLoader() {}

    public static List<Module> loadAllModules() {
        List<Module> loadedModules = new ArrayList<>();

        try {
            Path modulesPath = Paths.get(MODULES_FOLDER);

            if (!Files.exists(modulesPath)) {
                Files.createDirectories(modulesPath);
                System.out.println("modules folder created at: " + modulesPath.toAbsolutePath());
                return loadedModules;
            }

            File modulesDir = modulesPath.toFile();
            File[] subdirs = modulesDir.listFiles(File::isDirectory);

            if (subdirs == null || subdirs.length == 0) {
                System.out.println("no modules found in " + modulesPath.toAbsolutePath());
                return loadedModules;
            }

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

    private static Module loadModule(File moduleDir) {
        try {
            File configFile = new File(moduleDir, "module.json");
            if (!configFile.exists()) {
                System.out.println("skipping " + moduleDir.getName() + ": no module.json found");
                return null;
            }

            ModuleConfig config = gson.fromJson(new FileReader(configFile), ModuleConfig.class);

            //las clases compiladas del modulo se esperan en build/classes/java/main
            File classesDir = new File(moduleDir, "build/classes/java/main");
            File resourcesDir = new File(moduleDir, "build/resources/main");
            System.out.println("DEBUG: Looking for classes in: " + classesDir.getAbsolutePath());
            System.out.println("DEBUG: Classes dir exists? " + classesDir.exists());
            System.out.println("DEBUG: URLClassLoader URL: " + classesDir.toURI().toURL());

            if (!classesDir.exists()) {
                System.err.println("classes directory not found for " + moduleDir.getName() + ": " + classesDir.getAbsolutePath());
                return null;
            }

            if (config.mainClass == null || config.mainClass.isEmpty()) {
                System.err.println("invalid module config in " + moduleDir.getName() + ": missing mainClass");
                return null;
            }

            //classloader aislado por modulo para evitar conflictos entre clases
            URLClassLoader moduleClassLoader = new URLClassLoader(
                    new URL[]{
                            classesDir.toURI().toURL(),
                            resourcesDir.toURI().toURL()
                    },
                    Module.class.getClassLoader()
            );
            System.out.println("DEBUG: Attempting to load class: " + config.mainClass);

            Class<?> moduleClass = Class.forName(config.mainClass, true, moduleClassLoader);

            if (!Module.class.isAssignableFrom(moduleClass)) {
                System.err.println(config.mainClass + " does not implement Module interface");
                return null;
            }

            //setAccessible por si el constructor es package-private
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