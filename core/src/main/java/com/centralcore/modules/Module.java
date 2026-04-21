package com.centralcore.modules;

import javafx.scene.Parent;

//interfaz que todos los modulos deben implementar
//el shell solo conoce esta interfaz, no los modulos concretos
public interface Module {

    String getModuleId();
    String getName();
    String getVersion();
    String getDescription();
    String getLogoPath();

    //llamado al cargar el modulo: inicializar recursos aqui
    void initialize() throws Exception;

    //llamado al descargar: cerrar conexiones y limpiar
    void shutdown();

    //devuelve la ui del modulo para inyectarla en el contentPane del shell
    Parent getMainUI() throws Exception;
}