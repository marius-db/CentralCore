package com.centralcore.util;

import com.centralcore.model.User;

//guarda el usuario en sesión, accesible globalmente sin pasar referencias
public class SessionManager {

    private static User currentUser = null;

    private SessionManager() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clearSession() {
        currentUser = null;
    }
}