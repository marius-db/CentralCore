package com.centralcore.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * gestiona la traduccion y cambio de idioma de la aplicacion
 *
 * soporta traducciones en ingles y espanol usando archivos de propiedades
 *
 * tambien soporta persistencia de idioma y notificaciones de observadores para cambios en toda la app
 */
public class TranslationManager {

    private static ResourceBundle bundle;
    private static String currentLanguage = "en";
    private static final String LANGUAGE_CONFIG_FILE = System.getProperty("user.home") + "/.centralcore/language.conf";
    
    //patron observador para cambios de idioma en toda la app
    private static List<LanguageChangeListener> listeners = new ArrayList<>();

    /**
     * interfaz funcional para escuchadores de cambio de idioma
     */
    public interface LanguageChangeListener {
        void onLanguageChanged(String newLanguageCode);
    }

    /**
     * inicializa el gestor de traduccion con idioma por defecto (ingles)
     * tambien carga la preferencia de idioma guardada si esta disponible
     */
    static {
        String savedLanguage = loadSavedLanguage();
        setLanguage(savedLanguage != null ? savedLanguage : "en");
    }

    /**
     * establece el idioma actual y carga el archivo de propiedades correspondiente
     * tambien notifica a todos los observadores del cambio de idioma
     *
     * @param languageCode codigo de idioma ("en" para ingles, "es" para espanol)
     */
    public static void setLanguage(String languageCode) {
        if (currentLanguage.equals(languageCode)) {
            return; // no se necesita cambio
        }

        currentLanguage = languageCode;
        try {
            bundle = ResourceBundle.getBundle(
                "messages",
                new Locale(languageCode)
            );
            
            //guarda preferencia de idioma
            saveLanguage(languageCode);
            
            //notifica a todos los observadores
            notifyListeners(languageCode);
            
        } catch (Exception e) {
            System.err.println("error al cargar archivo de idioma: " + languageCode);
            bundle = ResourceBundle.getBundle("messages", new Locale("en"));
        }
    }

    /**
     * obtiene el texto traducido para una clave dada
     *
     * @param key la clave de traduccion
     * @return texto traducido o la clave si no se encuentra
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key; // vuelve a la clave si no encuentra la traduccion
        }
    }

    /**
     * obtiene el codigo de idioma actual
     *
     * @return codigo de idioma actual
     */
    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * registra un escuchador para eventos de cambio de idioma
     *
     * @param listener el escuchador a registrar
     */
    public static void addLanguageChangeListener(LanguageChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * desregistra un escuchador de eventos de cambio de idioma
     *
     * @param listener el escuchador a desregistrar
     */
    public static void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * notifica a todos los observadores del cambio de idioma
     */
    private static void notifyListeners(String languageCode) {
        for (LanguageChangeListener listener : listeners) {
            listener.onLanguageChanged(languageCode);
        }
    }

    /**
     * guarda la preferencia de idioma en archivo de configuracion
     */
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

    /**
     * carga la preferencia de idioma guardada del archivo de configuracion
     *
     * @return codigo de idioma guardado o null si no se encuentra
     */
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
