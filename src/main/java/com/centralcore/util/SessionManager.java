package com.centralcore.util;

import com.centralcore.model.User;

/**
 * mantiene el usuario actualmente conectado durante la sesion de la aplicacion
 *
 * clase estatica - accesible desde cualquier parte de la app sin pasar el usuario
 *
 * uso: SessionManager.getCurrentUser().getRole()
 */
public class SessionManager {

    //el usuario actualmente conectado
    private static User currentUser = null;

    //constructor privado - sin instanciacion
    private SessionManager() {}

    /**
     * establece el usuario actual despues de un login exitoso
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * devuelve el usuario actualmente conectado
     * devuelve null si no hay usuario conectado
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * devuelve true si hay un usuario actualmente conectado
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * limpia la sesion al cerrar sesion
     */
    public static void clearSession() {
        currentUser = null;
    }
}
