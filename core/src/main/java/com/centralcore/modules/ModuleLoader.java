package com.centralcore.modules;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.gson.Gson;

//descubre y carga módulos desde la carpeta /modules usando reflexion
//prioridad de carga:
//  1. JARs sueltos en la raíz de /módulos (distribución): modules/MiModulo.jar
//  2. Subdirectorios con build/classes/java/main (desarrollo): modules/MiModulo/build/...
public class ModuleLoader {

    private static final Gson gson = new Gson();
    private static final String MODULES_FOLDER = System.getProperty("user.home") + "/.centralcore/modules";

    private ModuleLoader() {
    }

    //recarga un modulo ya cargado: shutdown, nuevo classloader, nueva instancia, initialize
    public static Module reloadModule(String moduleId) {
        try {
            Path modulesPath = Paths.get(MODULES_FOLDER);
            File modulesDir = modulesPath.toFile();

            //buscar primero en JARs sueltos
            File[] jars = modulesDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    Module m = loadModuleFromJar(jar);
                    if (m != null && moduleId.equals(m.getModuleId())) return m;
                }
            }

            //luego en subdirectorios (modo desarrollo)
            File[] subdirs = modulesDir.listFiles(File::isDirectory);
            if (subdirs == null) return null;

            for (File moduleDir : subdirs) {
                File configFile = new File(moduleDir, "module.json");
                if (!configFile.exists()) continue;

                ModuleConfig config = gson.fromJson(new FileReader(configFile), ModuleConfig.class);
                if (!moduleId.equals(config.id)) continue;

                return loadModuleFromDir(moduleDir);
            }
        } catch (Exception e) {
            System.err.println("error al recargar modulo " + moduleId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

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

            //ids ya cargados desde JAR, para no cargarlos de nuevo desde directorio
            java.util.Set<String> loadedIds = new java.util.HashSet<>();

            //primera pasada: JARs sueltos en la raíz de /modules
            File[] jars = modulesDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    Module module = loadModuleFromJar(jar);
                    if (module != null) {
                        loadedModules.add(module);
                        loadedIds.add(module.getModuleId());
                        System.out.println("loaded module from JAR: " + module.getName() + " v" + module.getVersion());
                    }
                }
            }

            //segunda pasada: subdirectorios (modo desarrollo), saltando los ya cargados desde JAR
            File[] subdirs = modulesDir.listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File moduleDir : subdirs) {
                    Module module = loadModuleFromDir(moduleDir);
                    if (module != null) {
                        if (loadedIds.contains(module.getModuleId())) {
                            System.out.println("skipping dir (already loaded from JAR): " + module.getModuleId());
                            continue;
                        }
                        loadedModules.add(module);
                        System.out.println("loaded module from dir: " + module.getName() + " v" + module.getVersion());
                    }
                }
            }

            if (loadedModules.isEmpty()) {
                System.out.println("no modules found in " + modulesPath.toAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("error al escanear carpeta de modulos: " + e.getMessage());
            e.printStackTrace();
        }

        return loadedModules;
    }

    //carga un módulo desde un JAR suelto en la raiz de /modules
    //lee module.json desde dentro del JAR via JarFile
    private static Module loadModuleFromJar(File jarFile) {
        try {
            ModuleConfig config;
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry("module.json");
                if (entry == null) {
                    System.out.println("skipping " + jarFile.getName() + ": no module.json inside JAR");
                    return null;
                }
                config = gson.fromJson(new InputStreamReader(jar.getInputStream(entry)), ModuleConfig.class);
            }

            if (config == null || config.mainClass == null || config.mainClass.isEmpty()) {
                System.err.println("invalid module.json in " + jarFile.getName());
                return null;
            }

            //classloader aislado apuntando al JAR
            URLClassLoader moduleClassLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    Module.class.getClassLoader()
            );

            Class<?> moduleClass = Class.forName(config.mainClass, true, moduleClassLoader);

            if (!Module.class.isAssignableFrom(moduleClass)) {
                System.err.println(config.mainClass + " does not implement Module interface");
                return null;
            }

            Constructor<?> constructor = moduleClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Module module = (Module) constructor.newInstance();

            //para JARs sueltos el moduleDir es la carpeta /modules en si
            module.setModuleDir(jarFile.getParentFile());

            return module;

        } catch (ClassNotFoundException e) {
            System.err.println("module class not found in " + jarFile.getName() + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("error loading module from JAR " + jarFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    //carga un modulo desde un subdirectorio con clases compiladas sueltas (modo desarrollo)
    private static Module loadModuleFromDir(File moduleDir) {
        try {
            File configFile = new File(moduleDir, "module.json");
            if (!configFile.exists()) {
                System.out.println("skipping " + moduleDir.getName() + ": no module.json found");
                return null;
            }

            ModuleConfig config = gson.fromJson(new FileReader(configFile), ModuleConfig.class);

            if (config.mainClass == null || config.mainClass.isEmpty()) {
                System.err.println("invalid module config in " + moduleDir.getName() + ": missing mainClass");
                return null;
            }

            File classesDir = new File(moduleDir, "build/classes/java/main");
            File resourcesDir = new File(moduleDir, "build/resources/main");

            if (!classesDir.exists()) {
                System.err.println("classes directory not found for " + moduleDir.getName() + ": " + classesDir.getAbsolutePath());
                return null;
            }

            //classloader aislado por módulo para evitar conflictos entre clases
            URLClassLoader moduleClassLoader = new URLClassLoader(
                    new URL[]{classesDir.toURI().toURL(), resourcesDir.toURI().toURL()},
                    Module.class.getClassLoader()
            );

            Class<?> moduleClass = Class.forName(config.mainClass, true, moduleClassLoader);

            if (!Module.class.isAssignableFrom(moduleClass)) {
                System.err.println(config.mainClass + " does not implement Module interface");
                return null;
            }

            Constructor<?> constructor = moduleClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Module module = (Module) constructor.newInstance();

            module.setModuleDir(moduleDir);

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