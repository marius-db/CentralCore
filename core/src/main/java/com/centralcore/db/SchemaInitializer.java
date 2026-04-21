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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS licences (
                    id            INT          AUTO_INCREMENT PRIMARY KEY,
                    module_name   VARCHAR(100) NOT NULL,
                    licence_key   VARCHAR(255) NOT NULL UNIQUE,
                    issued_to     VARCHAR(150),
                    expiry_date   DATE         NOT NULL,
                    active        BOOLEAN      NOT NULL DEFAULT TRUE,
                    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ciudadanos (
                    id            INT          AUTO_INCREMENT PRIMARY KEY,
                    dni           VARCHAR(20)  NOT NULL UNIQUE,
                    nombre        VARCHAR(100) NOT NULL,
                    apellidos     VARCHAR(150) NOT NULL,
                    fecha_nac     DATE         NOT NULL,
                    sexo          CHAR(1)      NOT NULL,
                    direccion     VARCHAR(255),
                    municipio     VARCHAR(100),
                    telefono      VARCHAR(20),
                    email         VARCHAR(150),
                    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
                    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vehiculos (
                    id              INT         AUTO_INCREMENT PRIMARY KEY,
                    matricula       VARCHAR(20) NOT NULL UNIQUE,
                    marca           VARCHAR(80),
                    modelo          VARCHAR(80),
                    color           VARCHAR(40),
                    tipo            VARCHAR(40),
                    propietario_id  INT,
                    activo          BOOLEAN     NOT NULL DEFAULT TRUE,
                    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (propietario_id) REFERENCES ciudadanos(id) ON DELETE SET NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS incidentes_trafico (
                    id              INT          AUTO_INCREMENT PRIMARY KEY,
                    tipo            VARCHAR(80)  NOT NULL,
                    descripcion     VARCHAR(1000),
                    ubicacion       VARCHAR(255),
                    latitud         DECIMAL(10,7),
                    longitud        DECIMAL(10,7),
                    gravedad        VARCHAR(20)  NOT NULL DEFAULT 'low',
                    estado          VARCHAR(20)  NOT NULL DEFAULT 'open',
                    vehiculo_id     INT,
                    reportado_por   VARCHAR(100),
                    fecha_hora      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    fecha_cierre    TIMESTAMP,
                    FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id) ON DELETE SET NULL
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

            stmt.execute("""
                MERGE INTO licences (module_name, licence_key, issued_to, expiry_date)
                KEY (licence_key)
                VALUES ('citizen_db', 'CC-CIT-DEMO-0001', 'Demo City', '2027-12-31')
            """);
            stmt.execute("""
                MERGE INTO licences (module_name, licence_key, issued_to, expiry_date)
                KEY (licence_key)
                VALUES ('traffic', 'CC-TRF-DEMO-0001', 'Demo City', '2027-12-31')
            """);

            System.out.println("esquema listo");

        } catch (SQLException e) {
            System.err.println("fallo la inicializacion del esquema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}