# CentralCore

Desktop application for city administration and management.
Aplicación de escritorio para la administración y gestión de ciudades.

## Requirements / Requisitos

- Java 21 JDK
- Gradle 8+
- MySQL 8+

## Setup / Configuración

### 1. Database / Base de datos

```sql
mysql -u root -p < schema.sql
```

This creates the `centralcore` database with all tables and a default admin user.
Esto crea la base de datos `centralcore` con todas las tablas y un usuario admin por defecto.

Default credentials / Credenciales por defecto:
- Email: `admin@centralcore.local`
- Password: `Admin1234`

### 2. Database connection / Conexión a la base de datos

Edit `src/main/java/com/centralcore/db/DatabaseConnection.java` and update:
- `USER` → your MySQL username
- `PASSWORD` → your MySQL password

### 3. Run / Ejecutar

```bash
./gradlew run
```

## Project Structure / Estructura del Proyecto

```
src/main/java/com/centralcore/
├── Main.java                  - entry point
├── App.java                   - JavaFX application
├── controller/                - FXML controllers
│   ├── WelcomeController.java
│   ├── LoginController.java
│   ├── MainShellController.java
│   ├── ModulesController.java
│   ├── InstallsController.java
│   ├── LicencesController.java
│   └── SettingsController.java
├── model/                     - data models
│   └── User.java
├── dao/                       - database access objects
│   └── UserDAO.java
├── db/                        - database connection
│   └── DatabaseConnection.java
└── util/                      - utilities
    ├── SceneManager.java
    └── SessionManager.java

src/main/resources/com/centralcore/
├── fxml/                      - UI layouts
├── css/                       - stylesheets
└── images/                    - images (add logo here)
```

## Navigation Flow / Flujo de Navegación

```
Welcome → Login → MainShell
                    ├── Modules (default)
                    │     ├── → CitizenModule (loads into content pane)
                    │     └── → TrafficModule (loads into content pane)
                    ├── Installs
                    ├── Licences
                    └── Settings
```
