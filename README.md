# CentralCore

Desktop application for city administration and management.
Aplicación de escritorio para la administración y gestión de ciudades.

## Requirements / Requisitos

- Java 21 JDK
- Gradle 8+

No external database required. CentralCore uses an embedded H2 database that is created automatically on first run.
No se necesita base de datos externa. CentralCore usa una base de datos H2 embebida que se crea automáticamente en el primer arranque.

## Setup / Configuración

### 1. Run / Ejecutar

```bash
./gradlew run
```

That's it. The database file is created at `~/centralcore_db.mv.db` on first launch and the schema is initialized automatically.
Eso es todo. El archivo de base de datos se crea en `~/centralcore_db.mv.db` en el primer arranque y el esquema se inicializa automáticamente.

### 2. Default credentials / Credenciales por defecto

- Email: `admin@centralcore.local`
- Password: `Admin1234`

### 3. Modules / Módulos

Place module folders inside the `modules/` directory next to the JAR. Each module folder must contain a `module.json` descriptor:

```json
{
  "id": "my_module",
  "name": "My Module",
  "version": "1.0.0",
  "description": "What this module does",
  "mainClass": "com.example.MyModule",
  "author": "Author Name"
}
```

The main class must implement `com.centralcore.modules.Module` and have a no-args constructor.

## Project Structure / Estructura del Proyecto

```
src/main/java/
├── com/centralcore/
│   ├── Main.java                    - entry point
│   ├── App.java                     - JavaFX application
│   ├── controller/                  - FXML controllers
│   │   ├── WelcomeController.java
│   │   ├── LoginController.java
│   │   ├── MainShellController.java
│   │   ├── ModulesViewController.java
│   │   ├── InstallsController.java
│   │   ├── LicencesController.java
│   │   └── SettingsController.java
│   ├── model/                       - data models
│   │   └── User.java
│   ├── dao/                         - database access objects
│   │   └── UserDAO.java
│   ├── db/                          - database
│   │   ├── DatabaseConnection.java  - H2 embedded connection singleton
│   │   └── SchemaInitializer.java   - creates tables and seeds default data
│   ├── modules/                     - module system
│   │   ├── Module.java              - interface all modules must implement
│   │   ├── ModuleConfig.java        - module.json POJO
│   │   ├── ModuleLoader.java        - discovers and loads modules from ./modules/
│   │   └── ModuleManager.java       - lifecycle manager (singleton)
│   └── util/
│       ├── CustomTitleBar.java      - draggable custom window chrome
│       ├── ModuleDetailsDialog.java - modal dialog for module info
│       ├── SceneManager.java        - centralized scene/navigation manager
│       ├── SessionManager.java      - current logged-in user holder
│       └── TranslationManager.java  - i18n with observer pattern
├── citizenmodule/
│   └── CitizenModule.java           - built-in citizen records module
└── trafficmodule/
    └── TrafficModule.java           - built-in traffic management module

src/main/resources/com/centralcore/
├── fxml/                            - UI layouts
├── css/                             - stylesheets (global, auth, main, views, welcome)
└── fonts/                           - Orbitron variable font

modules/
├── CitizenModule/module.json
└── TrafficModule/module.json
```

## Database Schema / Esquema de Base de Datos

Tables created automatically on first run:

- `users` — application users with BCrypt-hashed passwords
- `licences` — module licence keys
- `ciudadanos` — citizen records (used by CitizenModule)
- `vehiculos` — registered vehicles linked to citizens
- `incidentes_trafico` — traffic incidents (used by TrafficModule)

## Navigation Flow / Flujo de Navegación

```
Welcome → Login → MainShell
                    ├── Modules (default view)
                    │     ├── → CitizenModule (loads into content pane)
                    │     └── → TrafficModule (loads into content pane)
                    ├── Installs  - lists loaded modules
                    ├── Licences  - manage licence keys
                    └── Settings  - language, appearance, DB status
```

## Dependencies / Dependencias

| Library | Version | Purpose |
|---|---|---|
| JavaFX | 21 | UI framework |
| H2 | 2.4.240 | Embedded database |
| jBCrypt | 0.4 | Password hashing |
| Gson | 2.10.1 | module.json parsing |
| JUnit Jupiter | 5.10.2 | Unit tests |