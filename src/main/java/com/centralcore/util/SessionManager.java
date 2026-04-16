package com.centralcore.util;

import com.centralcore.model.User;

/**
 * holds the currently logged-in user for the duration of the app session
 * mantiene el usuario actualmente conectado durante la sesion de la aplicacion
 *
 * static class - accessible from anywhere in the app without passing user around
 * clase estatica - accesible desde cualquier parte de la app sin pasar el usuario
 *
 * usage: SessionManager.getCurrentUser().getRole()
 * uso: SessionManager.getCurrentUser().getRole()
 */
public class SessionManager {

    //the currently logged in user / el usuario actualmente conectado
    private static User currentUser = null;

    //private constructor - no instantiation
    //constructor privado - sin instanciacion
    private SessionManager() {}

    /**
     * sets the current user after successful login
     * establece el usuario actual despues de un login exitoso
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * returns the currently logged in user
     * devuelve el usuario actualmente conectado
     * returns null if no user is logged in / devuelve null si no hay usuario conectado
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * returns true if a user is currently logged in
     * devuelve true si hay un usuario actualmente conectado
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * clears the session on logout
     * limpia la sesion al cerrar sesion
     */
    public static void clearSession() {
        currentUser = null;
    }
}
