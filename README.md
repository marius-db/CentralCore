# CentralCore

> 🇪🇸 [Versión en español](README.es.md)

Desktop application for city administration built on a plugin architecture. The core shell handles auth, module loading, and licence management; actual functionality lives in independently compiled modules that get discovered and loaded at runtime via `URLClassLoader`.

Two modules are included: a citizen records database and a traffic management interface that connects to a simulation server over WebSocket.

## Requirements

- Java 21 JDK
- Gradle 8+

No external database needed. CentralCore uses an embedded H2 database that gets created automatically on first run at `~/centralcore_db.mv.db`.

## Running it

```bash
./gradlew run
```

That's it. Schema is initialized automatically on first launch.

Default credentials:
- Email: `admin@centralcore.local`
- Password: `Admin1234`

## Project structure

This is a multi-project Gradle build with three subprojects:

```
CentralCore/
├── build.gradle                  - root config, shared settings
├── core/                         - the shell application
│   └── src/main/java/com/centralcore/
│       ├── Main.java
│       ├── App.java
│       ├── controller/           - FXML controllers for all shell views
│       │   ├── WelcomeController.java
│       │   ├── LoginController.java
│       │   ├── MainShellController.java
│       │   ├── ModulesViewController.java
│       │   ├── InstallsController.java
│       │   ├── LicencesController.java
│       │   └── SettingsController.java
│       ├── dao/
│       │   └── UserDAO.java
│       ├── db/
│       │   ├── DatabaseConnection.java   - H2 singleton
│       │   └── SchemaInitializer.java    - creates tables and seeds defaults
│       ├── model/
│       │   ├── User.java
│       │   └── Licence.java
│       ├── modules/
│       │   ├── Module.java               - interface all modules must implement
│       │   ├── ModuleConfig.java         - module.json POJO
│       │   ├── ModuleLoader.java         - discovers and loads from ./modules/
│       │   └── ModuleManager.java        - lifecycle manager
│       └── util/
│           ├── CustomTitleBar.java       - draggable custom window chrome
│           ├── ModuleDetailsDialog.java
│           ├── SceneManager.java         - centralized scene/navigation
│           ├── SessionManager.java       - current user holder
│           ├── LicenceStorage.java       - persists licence keys via Java Prefs
│           ├── LicenseValidator.java     - HMAC-SHA256 validation
│           └── TranslationManager.java   - i18n with observer pattern
└── modules/
    ├── CitizenModule/            - citizen records module
    │   ├── module.json
    │   ├── build.gradle
    │   └── src/com/centralcore/modules/citizenmodule/
    │       ├── CitizenModule.java
    │       ├── CitizenModuleController.java
    │       ├── CitizenDAO.java
    │       ├── Citizen.java
    │       ├── CitizenDocument.java
    │       └── ...
    └── TrafficModule/            - traffic management module
        ├── module.json
        ├── build.gradle
        └── src/com/centralcore/modules/trafficmodule/
            ├── TrafficModule.java
            ├── TrafficModuleController.java
            ├── MapCanvas.java            - JavaFX Canvas renderer
            ├── SimConnection.java        - WebSocket client
            ├── TrafficDAO.java           - incident persistence
            └── model/
                ├── SimState.java
                ├── SimCar.java
                ├── TrafficLight.java
                ├── TrafficNode.java
                ├── TrafficEdge.java
                ├── Incident.java
                └── IncidentUpdate.java
```

## Database schema

Tables created automatically on first run:

| Table | Description |
|---|---|
| `users` | App users, BCrypt-hashed passwords, roles |
| `licences` | Module licence keys with expiry dates |
| `ciudadanos` | Citizen records (CitizenModule) |
| `vehiculos` | Registered vehicles linked to citizens |
| `incidentes_trafico` | Traffic incidents with severity, state, and coordinates |

CitizenModule also creates two additional tables on its own init:
- `ciudadano_documentos`: file attachments linked to citizen records

## Navigation flow

```
Welcome -> Login -> MainShell
                     ├── Modules (default view)
                     │     ├── CitizenModule
                     │     └── TrafficModule
                     ├── Installs   - lists loaded modules with details
                     ├── Licences   - add/remove licence keys
                     └── Settings   - language, DB controls, danger zone
```

## Module system

Modules are loaded from a `modules/` folder next to the running app. Each module needs a `module.json` and its compiled classes in `build/classes/java/main/`.

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

The main class must implement `com.centralcore.modules.Module` and have a no-args constructor. Each module gets its own isolated `URLClassLoader` so there are no class conflicts between modules.

To build a module:

```bash
cd modules/CitizenModule
./gradlew jar
```

## Licence system

Licences are HMAC-SHA256 signed keys encoded in Base64 with the format `email|expiry_date|hmac`. The app validates signature and expiry on import, and stores the result locally via Java Preferences API. Demo licence keys for both modules are seeded into the database automatically.

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| JavaFX | 21 | UI framework |
| H2 | 2.4.240 | Embedded database |
| jBCrypt | 0.4 | Password hashing |
| Gson | 2.10.1 | module.json parsing |
| JUnit Jupiter | 5.10.2 | Unit tests |