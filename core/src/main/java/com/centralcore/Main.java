package com.centralcore;

import javafx.application.Application;

/**
 * punto de entrada principal de centralcore
 *
 * esta clase existe como solucion al problema del launcher de javafx en java 21
 */
public class Main {
    public static void main(String[] args) {
        //delegar el lanzamiento a la clase de la aplicacion javafx
        Application.launch(App.class, args);
    }
}
