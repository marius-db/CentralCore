package com.centralcore.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * runs all CREATE TABLE IF NOT EXISTS statements on startup
 * replaces the old schema.sql — h2 handles this in-process so no external script needed
 * safe to call every launch because IF NOT EXISTS means it won't wipe existing data
 *
 * also seeds the default admin user and demo licences if they aren't there yet
 * uso: SchemaInitializer.initialize(connection);
 */
public class SchemaInitializer {

    //no instantiation needed / sin instanciacion necesaria
    private SchemaInitializer() {}

    /**
     * creates all tables and inserts seed data
     * called automatically by DatabaseConnection on first open
     * crea todas las tablas e inserta datos iniciales
     *
     * @param conn active h2 connection / conexion h2 activa
     */
    public static void initialize(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            //users table - people who can log into centralcore
            //tabla de usuarios - personas que pueden iniciar sesion en centralcore
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

            //licences table - which modules this installation is allowed to use
            //tabla de licencias - que modulos tiene permitido usar esta instalacion
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

            //ciudadanos - the main citizen registry for the citizen module
            //ciudadanos - el registro principal de ciudadanos para el modulo ciudadano
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

            //vehiculos - vehicles registered to citizens, used by the traffic module
            //vehiculos - vehiculos registrados a ciudadanos, usados por el modulo de trafico
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

            //incidentes_trafico - traffic incidents reported in the city
            //incidentes_trafico - incidentes de trafico reportados en la ciudad
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

            //seed default admin - password is Admin1234
            //only inserts if the email doesn't exist yet so reruns are safe
            //insertar admin por defecto - contraseña Admin1234
            //solo inserta si el email no existe todavia, asi que es seguro relanzar
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

            //seed demo licences for both modules
            //licencias de demo para ambos modulos
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

            System.out.println("schema ready / esquema listo");

        } catch (SQLException e) {
            System.err.println("schema init failed / fallo la inicializacion del esquema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}