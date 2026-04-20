package com.centralcore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * singleton db connection manager, now using H2 in embedded mode
 * el archivo .mv.db se crea automaticamente en la primera ejecucion en el directorio home del usuario
 *
 * uso: Connection conn = DatabaseConnection.getConnection();
 */
public class DatabaseConnection {

    //ruta del archivo h2 embebido — guardado en el directorio home del usuario
    private static final String DB_PATH = System.getProperty("user.home") + "/centralcore_db";

    //url jdbc para modo embebido de h2
    //AUTO_SERVER=TRUE permite multiples conexiones si es necesario
    private static final String URL =
            "jdbc:h2:" + DB_PATH
                    + ";AUTO_SERVER=TRUE"
                    + ";DB_CLOSE_DELAY=-1"
                    + ";NON_KEYWORDS=VALUE";

    //credenciales por defecto de h2 — validas para una bd local embebida
    private static final String USER     = "sa";
    private static final String PASSWORD = "";

    //conexion compartida unica
    private static Connection connection = null;

    //sin instanciacion — clase utilitaria estatica
    private DatabaseConnection() {}

    /**
     * devuelve la conexion activa, creandola si no existe o se cerro
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("conexion a bd establecida");

                //ejecutar el esquema cada vez que se abre una conexion nueva — es seguro por el IF NOT EXISTS
                SchemaInitializer.initialize(connection);
            }
        } catch (SQLException e) {
            System.err.println("fallo la conexion a bd: " + e.getMessage());
            return null;
        }
        return connection;
    }

    /**
     * cierra la conexion — llamar al cerrar la app
     */
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("conexion a bd cerrada");
            }
        } catch (SQLException e) {
            System.err.println("error cerrando conexion: " + e.getMessage());
        }
    }

    /**
     * comprobacion rapida — devuelve true si podemos acceder a la bd
     */
    public static boolean testConnection() {
        return getConnection() != null;
    }
}