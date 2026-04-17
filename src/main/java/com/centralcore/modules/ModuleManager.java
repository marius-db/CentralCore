package com.centralcore.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * manages the lifecycle and registry of all loaded modules
 * gestiona el ciclo de vida y el registro de todos los modulos cargados
 *
 * keeps track of active modules, initializes them on startup, shuts them down on app exit
 * this is the single point of contact for the main app shell to work with modules
 * mantiene un registro de los modulos activos, los inicializa al inicio, los cierra al salir
 * este es el punto unico de contacto para que el shell de la app trabaje con modulos
 */
public class ModuleManager {

    //single instance / instancia unica
    private static ModuleManager instance;

    //map of module id to module instance / mapa de id de modulo a instancia del modulo
    private Map<String, Module> modules;

    private ModuleManager() {
        this.modules = new HashMap<>();
    }

    /**
     * gets or creates the singleton instance
     * obtiene o crea la instancia singleton
     */
    public static synchronized ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    /**
     * discovers all modules and initializes them
     * should be called once on app startup
     * descubre todos los modulos e los inicializa
     * debe ser llamado una vez al inicio de la app
     */
    public void loadAndInitializeModules() {
        System.out.println("loading modules...");
        List<Module> loadedModules = ModuleLoader.loadAllModules();

        for (Module module : loadedModules) {
            try {
                //call module's initialize method
                //llama el metodo initialize del modulo
                module.initialize();
                modules.put(module.getModuleId(), module);
                System.out.println("initialized module: " + module.getModuleId());
            } catch (Exception e) {
                System.err.println("failed to initialize module " + module.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("module load complete. active modules: " + modules.size());
    }

    /**
     * shuts down all active modules
     * should be called when the app is closing
     * cierra todos los modulos activos
     * debe ser llamado cuando la app se esta cerrando
     */
    public void shutdownAllModules() {
        System.out.println("shutting down modules...");
        for (Module module : modules.values()) {
            try {
                module.shutdown();
                System.out.println("shut down module: " + module.getModuleId());
            } catch (Exception e) {
                System.err.println("error shutting down module " + module.getName() + ": " + e.getMessage());
            }
        }
        modules.clear();
    }

    /**
     * gets a module by its id
     * obtiene un modulo por su id
     */
    public Module getModule(String moduleId) {
        return modules.get(moduleId);
    }

    /**
     * returns all active modules
     * devuelve todos los modulos activos
     */
    public List<Module> getAllModules() {
        return new ArrayList<>(modules.values());
    }

    /**
     * checks if a module is loaded
     * verifica si un modulo esta cargado
     */
    public boolean isModuleLoaded(String moduleId) {
        return modules.containsKey(moduleId);
    }

    /**
     * returns the count of active modules
     * devuelve la cantidad de modulos activos
     */
    public int getModuleCount() {
        return modules.size();
    }
}
