package com.centralcore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * singleton database connection manager
 * gestor singleton de la conexion a la base de datos
 *
 * provides a single shared connection to the mysql database
 * proporciona una unica conexion compartida a la base de datos mysql
 *
 * usage: Connection conn = DatabaseConnection.getConnection();
 * uso: Connection conn = DatabaseConnection.getConnection();
 */
public class DatabaseConnection {

    // --- database config / configuracion de la base de datos ---
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DATABASE = "centralcore";
    private static final String USER     = "root";       // change to your mysql user / cambia a tu usuario mysql
    private static final String PASSWORD = "root";       // change to your mysql password / cambia a tu contraseña mysql

    // full jdbc connection url / url de conexion jdbc completa
    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
        + "?useSSL=false"
        + "&serverTimezone=UTC"
        + "&allowPublicKeyRetrieval=true"
        + "&characterEncoding=UTF-8";

    // single shared connection instance / instancia de conexion compartida
    private static Connection connection = null;

    // private constructor - no instantiation allowed
    // constructor privado - no se permite instanciacion
    private DatabaseConnection() {}

    /**
     * returns the active connection, creating it if it doesnt exist or was closed
     * devuelve la conexion activa, creandola si no existe o estaba cerrada
     */
    public static Connection getConnection() {
        try {
            // if connection doesnt exist or was closed, create a new one
            // si la conexion no existe o se cerro, crear una nueva
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("db connection established / conexion a bd establecida");
            }
        } catch (SQLException e) {
            System.err.println("db connection failed / fallo la conexion a bd: " + e.getMessage());
            // we return null here - callers must handle null
            // devolvemos null - los llamadores deben manejar null
            return null;
        }
        return connection;
    }

    /**
     * closes the connection when the app exits
     * cierra la conexion cuando la aplicacion termina
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
     * tests if the db is reachable and returns true/false
     * comprueba si la bd es accesible y devuelve true/false
     */
    public static boolean testConnection() {
        Connection conn = getConnection();
        return conn != null;
    }
}
