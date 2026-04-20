package com.centralcore.modules;

import javafx.scene.Parent;

/**
 * interfaz base que todos los modulos de centralcore deben implementar
 *
 * los modulos son plugins independientes que proporcionan funcionalidad a la aplicacion principal
 * el shell de la app no sabe sobre modulos especificos, solo carga lo que implemente esto
 */
public interface Module {

    //metadatos del modulo

    /**
     * identificador unico para este modulo
     */
    String getModuleId();

    /**
     * nombre legible por humanos
     */
    String getName();

    /**
     * cadena de version semantica
     */
    String getVersion();

    /**
     * descripcion breve de lo que hace este modulo
     */
    String getDescription();

    /**
     * ruta al archivo de imagen del logo (relativo a la carpeta del modulo)
     */
    String getLogoPath();

    //ciclo de vida

    /**
     * se llama cuando el modulo esta a punto de cargarse
     * inicializa recursos, conexiones de base de datos, etc aqui
     */
    void initialize() throws Exception;

    /**
     * se llama cuando el modulo se esta descargando
     * limpia recursos, cierra conexiones, guarda estado
     */
    void shutdown();

    //interfaz

    /**
     * devuelve el nodo javafx raiz para la ui de este modulo
     * esto se coloca en el panel de contenido del shell principal
     */
    Parent getMainUI() throws Exception;
}
