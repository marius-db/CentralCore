package com.centralcore;

import javafx.application.Application;

/**
 * main entry point for centralcore
 * punto de entrada principal de centralcore
 *
 * this class exists as a workaround for the javafx launcher issue on java 21
 * esta clase existe como solucion al problema del launcher de javafx en java 21
 * launching from a non-application class avoids the "missing javafx.graphics module" error
 */
public class Main {
    public static void main(String[] args) {
        //delegate launch to the actual javafx app class
        //delegar el lanzamiento a la clase de la aplicacion javafx
        Application.launch(App.class, args);
    }
}
