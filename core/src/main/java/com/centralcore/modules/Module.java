package com.centralcore.modules;

import java.io.File;
import javafx.scene.Parent;

//interfaz que todos los modulos deben implementar
//el shell solo conoce esta interfaz, no los modulos concretos
public interface Module {

    String getModuleId();

    String getName();

    String getVersion();

    String getDescription();

    String getLogoPath();

    //inyectado por ModuleLoader tras instanciar: permite al modulo localizar su module.json e imagenes
    void setModuleDir(File moduleDir);

    //llamado al cargar el modulo: inicializar recursos aqui
    void initialize() throws Exception;

    //llamado al descargar: cerrar conexiones y limpiar
    void shutdown();

    //recarga completa del modulo sin reiniciar la app
    void reload() throws Exception;

    //devuelve la ui del modulo para inyectarla en el contentPane del shell
    Parent getMainUI() throws Exception;
}