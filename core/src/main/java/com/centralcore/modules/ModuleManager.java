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

    public int getModuleCount() {
        return modules.size();
    }
}