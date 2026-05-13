package com.centralcore.util;

import java.io.*;
import java.util.Properties;

//guarda y carga preferencias de estado de la ui en ~/.centralcore/ui_prefs.conf
//usado para persistir posiciones de divisores, tamaños de paneles y similares
public class PreferencesStorage {

    private static final String PREFS_FILE = System.getProperty("user.home") + "/.centralcore/ui_prefs.conf";

    //guarda un valor double con la clave dada
    public static void putDouble(String key, double value) {
        Properties props = loadAll();
        props.setProperty(key, String.valueOf(value));
        saveAll(props);
    }

    //carga un valor double, devuelve defaultValue si no existe
    public static double getDouble(String key, double defaultValue) {
        Properties props = loadAll();
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Properties loadAll() {
        Properties props = new Properties();
        File file = new File(PREFS_FILE);
        if (!file.exists()) return props;
        try (FileReader fr = new FileReader(file)) {
            props.load(fr);
        } catch (Exception e) {
            System.err.println("error al cargar preferencias ui: " + e.getMessage());
        }
        return props;
    }

    private static void saveAll(Properties props) {
        try {
            File dir = new File(System.getProperty("user.home") + "/.centralcore");
            if (!dir.exists()) dir.mkdirs();
            try (FileWriter fw = new FileWriter(PREFS_FILE)) {
                props.store(fw, null);
            }
        } catch (Exception e) {
            System.err.println("error al guardar preferencias ui: " + e.getMessage());
        }
    }
}