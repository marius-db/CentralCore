package com.centralcore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//singleton para la conexion h2 embebida
//el archivo .mv.db se crea solo en el home del usuario la primera vez
public class DatabaseConnection {

    private static final String DB_PATH = System.getProperty("user.home") + "/centralcore_db";

    //AUTO_SERVER=TRUE por si acaso se abre desde dos sitios, DB_CLOSE_DELAY=-1 para que no cierre sola
    private static final String URL =
            "jdbc:h2:" + DB_PATH
                    + ";AUTO_SERVER=TRUE"
                    + ";DB_CLOSE_DELAY=-1"
                    + ";NON_KEYWORDS=VALUE";

    private static final String USER     = "sa";
    private static final String PASSWORD = "";

    private static Connection connection = null;

    private DatabaseConnection() {}

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("conexion a bd establecida");

                //seguro llamarlo siempre porque usa IF NOT EXISTS internamente
                SchemaInitializer.initialize(connection);
            }
        } catch (SQLException e) {
            System.err.println("fallo la conexion a bd: " + e.getMessage());
            return null;
        }
        return connection;
    }

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

    public static boolean testConnection() {
        return getConnection() != null;
    }
}