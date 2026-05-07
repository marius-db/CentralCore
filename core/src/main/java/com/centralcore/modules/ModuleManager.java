package com.centralcore.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {

    private static ModuleManager instance;
    private Map<String, Module> modules;

    private ModuleManager() {
        this.modules = new HashMap<>();
    }

    public static synchronized ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    public void loadAndInitializeModules() {
        System.out.println("loading modules...");
        List<Module> loadedModules = ModuleLoader.loadAllModules();

        for (Module module : loadedModules) {
            try {
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

    //llamar en App.stop(), si no los modulos no hacen shutdown limpio
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

    public Module getModule(String moduleId) {
        return modules.get(moduleId);
    }

    public List<Module> getAllModules() {
        return new ArrayList<>(modules.values());
    }

    public boolean isModuleLoaded(String moduleId) {
        return modules.containsKey(moduleId);
    }

    //recarga un módulo en caliente: shutdown, nueva instancia desde disco y después initialize
    //devuelve el módulo nuevo si tuvo exito, null si fallo
    public Module reloadModule(String moduleId) {
        Module old = modules.get(moduleId);
        if (old != null) {
            try {
                old.shutdown();
            } catch (Exception e) {
                System.err.println("error en shutdown antes de recargar " + moduleId + ": " + e.getMessage());
            }
            modules.remove(moduleId);
        }

        Module fresh = ModuleLoader.reloadModule(moduleId);
        if (fresh == null) {
            System.err.println("fallo al recargar modulo: " + moduleId);
            return null;
        }

        try {
            fresh.initialize();
            modules.put(fresh.getModuleId(), fresh);
            System.out.println("modulo recargado: " + fresh.getModuleId());
            return fresh;
        } catch (Exception e) {
            System.err.println("error al inicializar modulo recargado " + moduleId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public int getModuleCount() {
        return modules.size();
    }
}