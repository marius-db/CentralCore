package com.centralcore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * singleton db connection manager, now using H2 in embedded mode
 * the .mv.db file gets created automatically on first run in the user's home dir
 *
 * usage: Connection conn = DatabaseConnection.getConnection();
 * uso: Connection conn = DatabaseConnection.getConnection();
 */
public class DatabaseConnection {

    //h2 embedded db file path — stored in the user's home directory
    //ruta del archivo h2 embebido — guardado en el directorio home del usuario
    private static final String DB_PATH = System.getProperty("user.home") + "/centralcore_db";

    //jdbc url for h2 embedded mode
    //url jdbc para modo embebido de h2
    //AUTO_SERVER=TRUE lets multiple connections work if needed
    private static final String URL =
            "jdbc:h2:" + DB_PATH
                    + ";AUTO_SERVER=TRUE"
                    + ";DB_CLOSE_DELAY=-1"
                    + ";NON_KEYWORDS=VALUE";

    //h2 default credentials — fine for an embedded local db
    //credenciales por defecto de h2 — validas para una bd local embebida
    private static final String USER     = "sa";
    private static final String PASSWORD = "";

    //single shared connection / conexion compartida unica
    private static Connection connection = null;

    //no instantiation — static utility class
    //sin instanciacion — clase utilitaria estatica
    private DatabaseConnection() {}

    /**
     * returns the active connection, creating it if it doesn't exist or was closed
     * devuelve la conexion activa, creandola si no existe o se cerro
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("db connection established / conexion a bd establecida");

                //run schema init every time we open a fresh connection
                //ejecutar el esquema cada vez que se abre una conexion nueva — es seguro por el IF NOT EXISTS
                SchemaInitializer.initialize(connection);
            }
        } catch (SQLException e) {
            System.err.println("db connection failed / fallo la conexion a bd: " + e.getMessage());
            return null;
        }
        return connection;
    }

    /**
     * closes the connection — call this on app shutdown
     * cierra la conexion — llamar al cerrar la app
     */
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("db connection closed / conexion a bd cerrada");
            }
        } catch (SQLException e) {
            System.err.println("error closing connection / error cerrando conexion: " + e.getMessage());
        }
    }

    /**
     * quick check — returns true if we can reach the db
     * comprobacion rapida — devuelve true si podemos acceder a la bd
     */
    public static boolean testConnection() {
        return getConnection() != null;
    }
}