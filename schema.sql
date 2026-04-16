-- centralcore database schema
-- esquema de base de datos de centralcore
-- run this file once to set up the database / ejecutar este archivo una vez para configurar la base de datos
-- mysql -u root -p < schema.sql

-- create and use the database / crear y usar la base de datos
CREATE DATABASE IF NOT EXISTS centralcore
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE centralcore;

-- ─── users table / tabla de usuarios ─────────────────────────────────────────
-- stores system users who can log into centralcore
-- almacena los usuarios del sistema que pueden iniciar sesion en centralcore

CREATE TABLE IF NOT EXISTS users (
    id            INT          AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(80)  NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,          -- bcrypt hash / hash bcrypt
    role          VARCHAR(40)  NOT NULL DEFAULT 'operator', -- 'admin' or 'operator' / 'admin' o 'operador'
    active        TINYINT(1)   NOT NULL DEFAULT 1,           -- 0 = disabled / 0 = deshabilitado
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── licences table / tabla de licencias ─────────────────────────────────────
-- stores module licences assigned to this city installation
-- almacena las licencias de modulos asignadas a esta instalacion de ciudad

CREATE TABLE IF NOT EXISTS licences (
    id            INT          AUTO_INCREMENT PRIMARY KEY,
    module_name   VARCHAR(100) NOT NULL,           -- e.g. 'citizen_db', 'traffic' / ej. 'citizen_db', 'trafico'
    licence_key   VARCHAR(255) NOT NULL UNIQUE,
    issued_to     VARCHAR(150),                    -- city or organisation name / nombre de ciudad u organizacion
    expiry_date   DATE         NOT NULL,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── citizen database module / modulo de base de datos ciudadana ──────────────
-- stores all city residents / almacena todos los residentes de la ciudad

CREATE TABLE IF NOT EXISTS ciudadanos (
    id            INT          AUTO_INCREMENT PRIMARY KEY,
    dni           VARCHAR(20)  NOT NULL UNIQUE,    -- national id / dni nacional
    nombre        VARCHAR(100) NOT NULL,
    apellidos     VARCHAR(150) NOT NULL,
    fecha_nac     DATE         NOT NULL,           -- date of birth / fecha de nacimiento
    sexo          CHAR(1)      NOT NULL,           -- 'M' or 'F' / 'H' o 'M'
    direccion     VARCHAR(255),
    municipio     VARCHAR(100),
    telefono      VARCHAR(20),
    email         VARCHAR(150),
    activo        TINYINT(1)   NOT NULL DEFAULT 1, -- 0 = deceased or removed / 0 = fallecido o eliminado
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ─── traffic module / modulo de trafico ──────────────────────────────────────
-- vehicles registered in the city / vehiculos registrados en la ciudad

CREATE TABLE IF NOT EXISTS vehiculos (
    id            INT          AUTO_INCREMENT PRIMARY KEY,
    matricula     VARCHAR(20)  NOT NULL UNIQUE,    -- license plate / matricula
    marca         VARCHAR(80),                     -- brand / marca
    modelo        VARCHAR(80),                     -- model / modelo
    color         VARCHAR(40),
    tipo          VARCHAR(40),                     -- 'car','truck','motorcycle' etc
    propietario_id INT,                            -- FK to ciudadanos / FK a ciudadanos
    activo        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (propietario_id) REFERENCES ciudadanos(id) ON DELETE SET NULL
);

-- traffic incidents / incidentes de trafico

CREATE TABLE IF NOT EXISTS incidentes_trafico (
    id            INT          AUTO_INCREMENT PRIMARY KEY,
    tipo          VARCHAR(80)  NOT NULL,           -- 'accident','roadblock','alert' etc
    descripcion   TEXT,
    ubicacion     VARCHAR(255),                    -- street or coordinates / calle o coordenadas
    latitud       DECIMAL(10,7),                   -- gps lat / latitud gps
    longitud      DECIMAL(10,7),                   -- gps lon / longitud gps
    gravedad      VARCHAR(20)  NOT NULL DEFAULT 'low', -- 'low','medium','high','critical'
    estado        VARCHAR(20)  NOT NULL DEFAULT 'open', -- 'open','in_progress','resolved'
    vehiculo_id   INT,                             -- vehicle involved if any / vehiculo involucrado si aplica
    reportado_por VARCHAR(100),                    -- officer or system / agente o sistema
    fecha_hora    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre  DATETIME,                        -- null if still open / null si aun abierto
    FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id) ON DELETE SET NULL
);

-- ─── seed data / datos de prueba ──────────────────────────────────────────────
-- default admin user (password: Admin1234) / usuario admin por defecto (contraseña: Admin1234)
-- bcrypt hash generated with work factor 12 / hash bcrypt generado con factor de trabajo 12

INSERT IGNORE INTO users (username, email, password_hash, role)
VALUES (
    'Admin',
    'admin@centralcore.local',
    '$2b$12$emSS5auR.dvoJOAz5s27FuUgTbOx9ZHtYi.8NWVKrBbVj5R4VkuT2',
    'admin'
);

-- sample licence / licencia de muestra
INSERT IGNORE INTO licences (module_name, licence_key, issued_to, expiry_date)
VALUES
    ('citizen_db', 'CC-CIT-DEMO-0001', 'Demo City', '2027-12-31'),
    ('traffic',    'CC-TRF-DEMO-0001', 'Demo City', '2027-12-31');
