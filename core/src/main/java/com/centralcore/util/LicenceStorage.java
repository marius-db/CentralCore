package com.centralcore.util;

import java.util.prefs.Preferences;

//almacena UNA sola licencia global para la shell app
public class LicenceStorage {

    private static final Preferences prefs = Preferences.userRoot().node("centralcore/licences");
    private static final String KEY_LICENCE = "app_licence";
    private static final String KEY_EXPIRY  = "app_expiry";

    public static void saveAppLicence(String key, String expiry) {
        prefs.put(KEY_LICENCE, key);
        prefs.put(KEY_EXPIRY, expiry);
    }

    //devuelve [clave, expiración] o null si no hay licencia guardada
    public static String[] loadAppLicence() {
        String key    = prefs.get(KEY_LICENCE, null);
        String expiry = prefs.get(KEY_EXPIRY, null);
        if (key == null || expiry == null) return null;
        return new String[]{ key, expiry };
    }

    public static void removeAppLicence() {
        prefs.remove(KEY_LICENCE);
        prefs.remove(KEY_EXPIRY);
    }

    public static boolean hasActiveLicence() {
        String[] data = loadAppLicence();
        if (data == null) return false;
        return LicenseValidator.isActive(data[0]);
    }

    //métodos heredados por compatibilidad con módulos

    public static void save(String module, String key, String expiry) {
        prefs.put(module, key + "|" + expiry);
    }

    public static String[] load(String module) {
        String val = prefs.get(module, null);
        return val != null ? val.split("\\|") : null;
    }

    public static void remove(String module) {
        prefs.remove(module);
    }

    public static java.util.prefs.Preferences getAll() {
        return prefs;
    }
}
