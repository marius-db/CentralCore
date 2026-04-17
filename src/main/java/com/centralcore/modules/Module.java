package com.centralcore.modules;

import javafx.scene.Parent;

/**
 * base interface that all centralcore modules must implement
 * interfaz base que todos los modulos de centralcore deben implementar
 *
 * modules are independent plugins that provide functionality to the main app
 * the app shell doesn't know about specific modules, just loads anything that implements this
 * los modulos son plugins independientes que proporcionan funcionalidad a la aplicacion principal
 * el shell de la app no sabe sobre modulos especificos, solo carga lo que implemente esto
 */
public interface Module {

    //module metadata / metadatos del modulo

    /**
     * unique identifier for this module
     * identificador unico para este modulo
     */
    String getModuleId();

    /**
     * human-readable name
     * nombre legible por humanos
     */
    String getName();

    /**
     * semantic version string
     * cadena de version semantica
     */
    String getVersion();

    /**
     * short description of what this module does
     * descripcion breve de lo que hace este modulo
     */
    String getDescription();

    /**
     * path to the logo image file (relative to module folder)
     * ruta al archivo de imagen del logo (relativo a la carpeta del modulo)
     */
    String getLogoPath();

    //lifecycle / ciclo de vida

    /**
     * called when the module is about to be loaded
     * initialize any resources, database connections, etc here
     * se llama cuando el modulo esta a punto de cargarse
     * inicializa recursos, conexiones de base de datos, etc aqui
     */
    void initialize() throws Exception;

    /**
     * called when the module is being unloaded
     * cleanup resources, close connections, save state
     * se llama cuando el modulo se esta descargando
     * limpia recursos, cierra conexiones, guarda estado
     */
    void shutdown();

    //ui / interfaz

    /**
     * returns the root javafx node for this module's ui
     * this gets placed into the main shell's content pane
     * devuelve el nodo javafx raiz para la ui de este modulo
     * esto se coloca en el panel de contenido del shell principal
     */
    Parent getMainUI() throws Exception;
}
