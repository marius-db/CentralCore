package com.centralcore.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * manages translation and language switching for the application
 * gestiona la traduccion y cambio de idioma de la aplicacion
 *
 * supports english and spanish translations using property files
 * soporta traducciones en ingles y espanol usando archivos de propiedades
 *
 * also supports language persistence and observer notifications for app-wide changes
 * tambien soporta persistencia de idioma y notificaciones de observadores para cambios en toda la app
 */
public class TranslationManager {

    private static ResourceBundle bundle;
    private static String currentLanguage = "en";
    private static final String LANGUAGE_CONFIG_FILE = System.getProperty("user.home") + "/.centralcore/language.conf";
    
    //observer pattern for app-wide language changes / patron observador para cambios de idioma en toda la app
    private static List<LanguageChangeListener> listeners = new ArrayList<>();

    /**
     * functional interface for language change listeners
     * interfaz funcional para escuchadores de cambio de idioma
     */
    public interface LanguageChangeListener {
        void onLanguageChanged(String newLanguageCode);
    }

    /**
     * initialize translation manager with default language (english)
     * also loads saved language preference if available
     * inicializa el gestor de traduccion con idioma por defecto (ingles)
     * tambien carga la preferencia de idioma guardada si esta disponible
     */
    static {
        String savedLanguage = loadSavedLanguage();
        setLanguage(savedLanguage != null ? savedLanguage : "en");
    }

    /**
     * set the current language and load corresponding property file
     * also notifies all observers of the language change
     * establece el idioma actual y carga el archivo de propiedades correspondiente
     * tambien notifica a todos los observadores del cambio de idioma
     *
     * @param languageCode language code ("en" for english, "es" for spanish)
     *                      codigo de idioma ("en" para ingles, "es" para espanol)
     */
    public static void setLanguage(String languageCode) {
        if (currentLanguage.equals(languageCode)) {
            return; // no change needed / no se necesita cambio
        }

        currentLanguage = languageCode;
        try {
            bundle = ResourceBundle.getBundle(
                "messages",
                new Locale(languageCode)
            );
            
            //save language preference / guarda preferencia de idioma
            saveLanguage(languageCode);
            
            //notify all observers / notifica a todos los observadores
            notifyListeners(languageCode);
            
        } catch (Exception e) {
            System.err.println("error loading language file / error al cargar archivo de idioma: " + languageCode);
            bundle = ResourceBundle.getBundle("messages", new Locale("en"));
        }
    }

    /**
     * get translated text for a given key
     * obtiene el texto traducido para una clave dada
     *
     * @param key the translation key / la clave de traduccion
     * @return translated text or the key itself if not found / texto traducido o la clave si no se encuentra
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key; // fallback to key if translation not found
        }
    }

    /**
     * get current language code
     * obtiene el codigo de idioma actual
     *
     * @return current language code / codigo de idioma actual
     */
    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * register a listener for language change events
     * registra un escuchador para eventos de cambio de idioma
     *
     * @param listener the listener to register / el escuchador a registrar
     */
    public static void addLanguageChangeListener(LanguageChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * unregister a listener from language change events
     * desregistra un escuchador de eventos de cambio de idioma
     *
     * @param listener the listener to unregister / el escuchador a desregistrar
     */
    public static void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * notify all observers of language change
     * notifica a todos los observadores del cambio de idioma
     */
    private static void notifyListeners(String languageCode) {
        for (LanguageChangeListener listener : listeners) {
            listener.onLanguageChanged(languageCode);
        }
    }

    /**
     * save language preference to config file
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
            System.err.println("error saving language preference / error al guardar preferencia de idioma");
        }
    }

    /**
     * load saved language preference from config file
     * carga la preferencia de idioma guardada del archivo de configuracion
     *
     * @return saved language code or null if not found / codigo de idioma guardado o null si no se encuentra
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
            System.err.println("error loading language preference / error al cargar preferencia de idioma");
        }
        return null;
    }
}
