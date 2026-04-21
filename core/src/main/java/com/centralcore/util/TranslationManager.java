package com.centralcore.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

//gestiona traducciones y persiste la preferencia de idioma en ~/.centralcore/language.conf
public class TranslationManager {

    private static ResourceBundle bundle;
    private static String currentLanguage = "en";
    private static final String LANGUAGE_CONFIG_FILE = System.getProperty("user.home") + "/.centralcore/language.conf";

    private static List<LanguageChangeListener> listeners = new ArrayList<>();

    public interface LanguageChangeListener {
        void onLanguageChanged(String newLanguageCode);
    }

    static {
        String savedLanguage = loadSavedLanguage();
        setLanguage(savedLanguage != null ? savedLanguage : "en");
    }

    public static void setLanguage(String languageCode) {
        if (currentLanguage.equals(languageCode)) {
            return;
        }

        currentLanguage = languageCode;
        try {
            bundle = ResourceBundle.getBundle(
                    "messages",
                    new Locale(languageCode)
            );

            saveLanguage(languageCode);
            notifyListeners(languageCode);

        } catch (Exception e) {
            System.err.println("error al cargar archivo de idioma: " + languageCode);
            bundle = ResourceBundle.getBundle("messages", new Locale("en"));
        }
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            //devuelve la clave como fallback para que no explote la ui si falta una traduccion
            return key;
        }
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    public static void addLanguageChangeListener(LanguageChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(String languageCode) {
        for (LanguageChangeListener listener : listeners) {
            listener.onLanguageChanged(languageCode);
        }
    }

    private static void saveLanguage(String languageCode) {
        try {
            File configDir = new File(System.getProperty("user.home") + "/.centralcore");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(LANGUAGE_CONFIG_FILE)) {
                writer.write(languageCode);
            }
        } catch (Exception e) {
            System.err.println("error al guardar preferencia de idioma");
        }
    }

    private static String loadSavedLanguage() {
        try {
            File configFile = new File(LANGUAGE_CONFIG_FILE);
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    StringBuilder sb = new StringBuilder();
                    int c;
                    while ((c = reader.read()) != -1) {
                        sb.append((char) c);
                    }
                    return sb.toString().trim();
                }
            }
        } catch (Exception e) {
            System.err.println("error al cargar preferencia de idioma");
        }
        return null;
    }
}