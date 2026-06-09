package com.centralcore.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

//crea las tablas al arrancar y siembra datos iniciales si no existen
//seguro llamarlo en cada lanzamiento por el IF NOT EXISTS y los MERGE KEY
public class SchemaInitializer {

    private SchemaInitializer() {}

    public static void initialize(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            stmt.execute("""
                 CREATE TABLE IF NOT EXISTS users (
                 id            INT          AUTO_INCREMENT PRIMARY KEY,
                 username      VARCHAR(80)  NOT NULL,
                 email         VARCHAR(150) NOT NULL UNIQUE,
                 password_hash VARCHAR(255) NOT NULL,
                 role          VARCHAR(40)  NOT NULL DEFAULT 'operator',
                 active        BOOLEAN      NOT NULL DEFAULT TRUE,
                 created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            //contrasena: Admin1234
            stmt.execute("""
                MERGE INTO users (username, email, password_hash, role, active)
                KEY (email)
                VALUES (
                    'Admin',
                    'admin@centralcore.local',
                    '$2a$12$emSS5auR.dvoJOAz5s27FuUgTbOx9ZHtYi.8NWVKrBbVj5R4VkuT2',
                    'admin',
                    TRUE
                )
            """);

            System.out.println("esquema listo");

        } catch (SQLException e) {
            System.err.println("fallo la inicializacion del esquema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}