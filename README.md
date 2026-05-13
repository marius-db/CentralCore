# CentralCore

> 🇪🇸 [Versión en español](README.es.md)

Desktop application for city administration built on a plugin architecture. The core shell handles authentication, module loading, and licence management. Actual functionality lives in independently compiled modules that get discovered and loaded at runtime via `URLClassLoader`.

Two modules are included out of the box: a citizen records database and a traffic management interface that connects to a simulation server over WebSocket.

The traffic simulator is a separate project: [TrafficSim](https://github.com/marius-db/TrafficSim).
Licence keys can be generated with the companion tool: [Licencing-gen](https://github.com/marius-db/Licencing-gen).

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

This also runs all tests and produces the compiled output for all subprojects. The module shadow JARs (fat JARs with all dependencies bundled) are produced automatically as part of this step and land here:

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

Each module gets its own isolated `URLClassLoader` so there are no class conflicts between modules. The module interface is the only thing the core shell knows about; module classes are invisible to it otherwise.

### module.json format

Every module needs a `module.json` either at the root of its project folder (dev mode) or bundled inside the JAR (distribution mode):

```json
{
  "id": "my_module",
  "name": "My Module",
  "version": "1.0.0",
  "description": "What this module does",
  "mainClass": "com.example.MyModule",
  "logoPath": "images/logo.png",
  "author": "Author Name"
}
```

### Creating a new module

1. Create a new subfolder under `modules/` with its own `build.gradle`, `module.json`, and `src/` directory.
2. Add it to `settings.gradle`: `include 'modules:MyModule'`
3. The main class must implement `com.centralcore.modules.Module` and have a no-args constructor.
4. Each module gets its own isolated `URLClassLoader` so there are no class conflicts between modules.

The `Module` interface requires: `getModuleId()`, `getName()`, `getVersion()`, `getDescription()`, `getLogoPath()`, `setModuleDir(File)`, `initialize()`, `shutdown()`, `reload()`, and `getMainUI()`. The module can also call `CitizenDAO.initSchema()` or its own equivalent in `initialize()` to extend the shared H2 database with its own tables.

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
│           ├── DwmManager.java           - native Windows DWM border/shadow integration
│           ├── ModuleDetailsDialog.java
│           ├── PreferencesStorage.java   - persists UI state (divider positions, etc.)
│           ├── RememberMeStorage.java    - saves credentials when remember me is on
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

## Included modules

### CitizenModule

Full CRUD for citizen records. The layout is a `SplitPane`: a searchable table on the left and a detail/edit panel on the right. The divider position is saved and restored between sessions via `PreferencesStorage`.

The edit form validates each field individually and shows per-field error labels. Document attachments are supported: each citizen can have files linked to their record, categorized by document type (DNI/passport, birth certificate, residence permit, driving licence, etc.).

Tables used: `ciudadanos`, `ciudadano_documentos`. The module also migrates the `ciudadanos` table on first run to add columns that the core schema does not include (`lugar_nac`, `nacionalidad`, `codigo_postal`, `estado_civil`).

### TrafficModule

Traffic management interface that connects to an external simulation server over WebSocket. The simulator is a separate project: [TrafficSim](https://github.com/marius-db/TrafficSim).

The map is rendered on a JavaFX `Canvas` with an `AnimationTimer` for a real-time render loop. All colors and fonts are pre-cached as static constants to avoid allocations per frame. A density color lookup table (20 steps) is pre-computed at class load time so no `new Color()` calls happen inside the render loop. List refreshes are throttled to every 5 ticks (~400ms) since traffic light cycles are several seconds long.

The side panel has tabs for: traffic lights (with manual override), road segment density, emergency routing (click to set point A and point B on the map, sends a route request to the sim), active incidents, and closed incident history. Right-clicking anywhere on the map opens a context menu.

`SimConnection` handles the WebSocket lifecycle: connects to `ws://localhost:8765` by default, auto-reconnects every 3 seconds if the connection drops, and routes incoming messages by type (`map`, `state`, `ev_done`).

Tables used: `traffic_incidents`, `traffic_incident_updates`.

---

## Database schema

Tables are created automatically on first run. You never need to set anything up manually.

| Table | Description |
|---|---|
| `users` | App users with BCrypt-hashed passwords and roles |
| `licences` | Module licence keys with expiry dates |
| `ciudadanos` | Citizen records (managed by CitizenModule) |
| `ciudadano_documentos` | File attachments linked to citizen records |
| `traffic_incidents` | Traffic incidents with type, map coordinates, and state |
| `traffic_incident_updates` | Update history for each incident |

The database file lives at `~/centralcore_db.mv.db` (in your home directory). You can delete it from Settings inside the app, or manually delete the file to start fresh.

> The core `SchemaInitializer` also creates `vehiculos` and `incidentes_trafico` tables as part of the base schema, though neither is actively used by the current modules.

---

## Navigation flow

```
Welcome -> Login -> Main Shell
                     ├── Modules (default view)
                     │     ├── CitizenModule
                     │     └── TrafficModule
                     ├── Installs   - lists loaded modules with details
                     ├── Licences   - add/remove licence key
                     └── Settings   - language, DB controls, danger zone
```

The Welcome screen detects saved credentials and shows a "Continue session" button if they exist, authenticating in the background on click. The Main Shell shows a licence veil overlay when no valid licence is present; the veil is bypassed on the Licences and Settings screens so the user can always fix it.

---

## Licence system

Licences are HMAC-SHA256 signed keys encoded in Base64 with the format `email|expiry_date|hmac`. The app validates the signature and expiry on import, checks that the email in the key matches the logged-in user's email, and stores the result via the Java Preferences API.

The shell uses a single global licence stored under `centralcore/licences` in Java Prefs. If no active licence is present, the content area is covered by the licence veil until one is added.

Keys can be generated with [Licencing-gen](https://github.com/marius-db/Licencing-gen). Demo licence keys for both included modules are seeded into the database automatically on first run, but those are DB-side records only; the shell licence is a separate key that must be added through the Licences screen.

---

## User accounts

The `users` table supports two roles: `admin` and `operator`. New accounts registered through the UI get the `operator` role. The seeded admin account is:

- **Email:** `admin@centralcore.local`
- **Password:** `Admin1234`

The "remember me" checkbox on login saves credentials (Base64 obfuscated, not encrypted) to `~/.centralcore/remember.conf`. This is convenience obfuscation only, not real security.

---

## Local config files

CentralCore stores a few files in `~/.centralcore/`:

| File | Contents |
|---|---|
| `language.conf` | Last selected language (`en` or `es`) |
| `remember.conf` | Saved credentials when remember me is active |
| `ui_prefs.conf` | UI state like split pane divider positions |

---

## Tests

Tests live in `core/src/test/java/com/centralcore/`.

`UnitTests.java` covers `LicenseValidator` (valid key, expired key, tampered key, email extraction) and `UserDAO.hashPassword` (BCrypt hash correctness, salt randomness). No database required.

`IntegrationTests.java` injects an H2 in-memory connection into the `DatabaseConnection` singleton via reflection, runs a fresh schema on each test, and tears it down after. Covers `testConnection`, `pingConnection`, user registration, authentication with correct credentials, authentication with wrong credentials, and `findById`.

Run all tests with:

```bash
./gradlew test
```

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
| JUnit Jupiter | 5.10.2 | Unit and integration tests |

---

## Related repositories

- [TrafficSim](https://github.com/marius-db/TrafficSim) - the Python simulation server that TrafficModule connects to over WebSocket
- [Licencing-gen](https://github.com/marius-db/Licencing-gen) - the key generator tool for producing valid HMAC-signed licence keys