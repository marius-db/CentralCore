package com.centralcore.util;

import java.io.*;
import java.util.Base64;
import java.util.Properties;

//guarda y recupera las credenciales del usuario cuando "recuerdame" esta activo
//no es cifrado real, solo ofuscacion basica para no guardar en texto plano visible
public class RememberMeStorage {

    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.centralcore/remember.conf";

    //guarda email y contrasena ofuscados en el archivo de configuracion
    public static void save(String email, String password) {
        try {
            File dir = new File(System.getProperty("user.home") + "/.centralcore");
            if (!dir.exists()) dir.mkdirs();

            Properties props = new Properties();
            props.setProperty("email", Base64.getEncoder().encodeToString(email.getBytes()));
            props.setProperty("password", Base64.getEncoder().encodeToString(password.getBytes()));
            props.setProperty("enabled", "true");

            try (FileWriter fw = new FileWriter(CONFIG_FILE)) {
                props.store(fw, null);
            }
        } catch (Exception e) {
            System.err.println("error al guardar credenciales: " + e.getMessage());
        }
    }

    //devuelve {email, password} o null si no hay credenciales guardadas
    public static String[] load() {
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) return null;

            Properties props = new Properties();
            try (FileReader fr = new FileReader(file)) {
                props.load(fr);
            }

            if (!"true".equals(props.getProperty("enabled"))) return null;

            String emailB64 = props.getProperty("email");
            String passB64  = props.getProperty("password");
            if (emailB64 == null || passB64 == null) return null;

            return new String[]{
                    new String(Base64.getDecoder().decode(emailB64)),
                    new String(Base64.getDecoder().decode(passB64))
            };
        } catch (Exception e) {
            System.err.println("error al cargar credenciales: " + e.getMessage());
            return null;
        }
    }

    //elimina las credenciales guardadas
    public static void clear() {
        try {
            File file = new File(CONFIG_FILE);
            if (file.exists()) file.delete();
        } catch (Exception e) {
            System.err.println("error al borrar credenciales: " + e.getMessage());
        }
    }

    //comprueba si hay credenciales guardadas
    public static boolean hasCredentials() {
        return load() != null;
    }
}