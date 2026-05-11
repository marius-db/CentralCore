# CentralCore

> 🇪🇸 [Versión en español](README.es.md)

Desktop application for city administration built on a plugin architecture. The core shell handles authentication, module loading, and licence management. Actual functionality lives in independently compiled modules that get discovered and loaded at runtime via `URLClassLoader`.

Two modules are included out of the box: a citizen records database and a traffic management interface that connects to a simulation server over WebSocket.

---

## Requirements

Before anything, make sure you have the following installed:

- **Java 21 JDK**: the app targets Java 21. Any JDK 21 distribution works (Eclipse Temurin, Oracle, Amazon Corretto, etc.). Make sure `java` and `javac` are on your PATH.
- **Git**: to clone the repository.
- **IntelliJ IDEA** (recommended): the project is set up as a Gradle project and IntelliJ handles everything automatically. Community Edition is free and works fine.

You do **not** need to install Gradle separately. The project includes a Gradle wrapper (`gradlew`) that downloads the right version automatically on first use.

You do **not** need any external database. CentralCore uses an embedded H2 database that creates itself automatically on first launch.

---

## Getting the project

Clone the repository:

```bash
git clone <repository-url>
cd CentralCore
```

Or download and extract the ZIP if you don't have Git.

---

## Running the app (development)

From the project root, run:

```bash
./gradlew :core:run
```

On Windows use `gradlew.bat` instead of `./gradlew`:

```bat
gradlew.bat :core:run
```

The first run will take a moment while Gradle downloads dependencies. After that it starts quickly.

On first launch the app automatically creates the H2 database and initializes all tables. You can log in immediately with the default admin account:

- **Email:** `admin@centralcore.local`
- **Password:** `Admin1234`

If you are using IntelliJ IDEA, just open the project folder and let it import as a Gradle project. Then run the `Main` class inside `core/src/main/java/com/centralcore/`.

---

## Building the project

To compile everything (core + all modules):

```bash
./gradlew build
```

This also runs any tests and produces the compiled output for all subprojects. The module shadow JARs (fat JARs with all dependencies bundled) are produced automatically as part of this step and land here:

```
modules/CitizenModule/build/libs/CitizenModule.jar
modules/TrafficModule/build/libs/TrafficModule.jar
```

To do a clean build from scratch (useful if something seems broken):

```bash
./gradlew clean build
```

---

## Module system

### How modules are loaded

On startup the app scans the `modules/` folder next to the running application and loads whatever it finds. There are two supported formats:

**Distribution mode (JAR files):** Drop a compiled module JAR directly into the `modules/` root:

```
modules/
  CitizenModule.jar
  TrafficModule.jar
```

The app reads `module.json` from inside the JAR, loads the module class, and initializes it. This is the format you would use when distributing the app to someone else.

**Development mode (compiled classes):** During development, modules are loaded directly from their Gradle build output without needing to package them as JARs first:

```
modules/
  CitizenModule/
    build/classes/java/main/
    build/resources/main/
    module.json
  TrafficModule/
    ...
```

When both formats are present for the same module, the JAR takes priority and the directory is skipped.

### module.json format

Every module needs a `module.json` either at the root of its project folder (dev mode) or bundled inside the JAR (distribution mode):

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

### Creating a new module

1. Create a new subfolder under `modules/` with its own `build.gradle`, `module.json`, and `src/` directory.
2. Add it to `settings.gradle`: `include 'modules:MyModule'`
3. The main class must implement `com.centralcore.modules.Module` and have a no-args constructor.
4. Each module gets its own isolated `URLClassLoader` so there are no class conflicts between modules.

### Building module JARs

To build the shadow JAR for a specific module:

```bash
./gradlew :modules:CitizenModule:shadowJar
./gradlew :modules:TrafficModule:shadowJar
```

Or just run `./gradlew build` to build everything at once. After building, copy the JARs from `modules/CitizenModule/build/libs/` to the `modules/` root if you want to use distribution mode.

---

## Project structure

```
CentralCore/
├── build.gradle                  - root config, shared settings
├── settings.gradle               - declares all subprojects
├── gradlew / gradlew.bat         - Gradle wrapper scripts
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
│       │   ├── DatabaseConnection.java   - H2 singleton connection
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
│           ├── SceneManager.java         - centralized scene navigation
│           ├── SessionManager.java       - current logged-in user holder
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
    │       └── CitizenDocument.java
    └── TrafficModule/            - traffic management module
        ├── module.json
        ├── build.gradle
        └── src/com/centralcore/modules/trafficmodule/
            ├── TrafficModule.java
            ├── TrafficModuleController.java
            ├── MapCanvas.java            - JavaFX Canvas renderer for the map
            ├── SimConnection.java        - WebSocket client for the simulator
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

---

## Database schema

Tables are created automatically on first run. You never need to set anything up manually.

| Table | Description |
|---|---|
| `users` | App users with BCrypt-hashed passwords and roles |
| `licences` | Module licence keys with expiry dates |
| `ciudadanos` | Citizen records (managed by CitizenModule) |
| `vehiculos` | Registered vehicles linked to citizens |
| `incidentes_trafico` | Traffic incidents with severity, state, and map coordinates |
| `ciudadano_documentos` | File attachments linked to citizen records |

The database file lives at `~/centralcore_db.mv.db` (in your home directory). You can delete it from Settings inside the app, or manually delete the file to start fresh.

---

## Navigation flow

```
Welcome -> Login -> Main Shell
                     ├── Modules (default view)
                     │     ├── CitizenModule
                     │     └── TrafficModule
                     ├── Installs   - lists loaded modules with details
                     ├── Licences   - add/remove licence keys
                     └── Settings   - language, DB controls, danger zone
```

---

## Licence system

Licences are HMAC-SHA256 signed keys encoded in Base64 with the format `email|expiry_date|hmac`. The app validates the signature and expiry on import and stores the result locally via the Java Preferences API. Demo licence keys for both included modules are seeded into the database automatically on first run.

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| JavaFX | 21 | UI framework |
| H2 | 2.4.240 | Embedded database |
| jBCrypt | 0.4 | Password hashing |
| Gson | 2.10.1 | module.json parsing |
| JNA | 5.14.0 | Native Windows DWM integration |
| Shadow | 8.3.0 | Fat JAR packaging for modules |
| JUnit Jupiter | 5.10.2 | Unit tests |