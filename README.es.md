# CentralCore

> 🇬🇧 [English version](README.md)

Aplicación de escritorio para administración de ciudades construida sobre una arquitectura de plugins. El shell principal gestiona la autenticación, la carga de módulos y las licencias; la funcionalidad real vive en módulos compilados de forma independiente que se descubren y cargan en tiempo de ejecución mediante `URLClassLoader`.

Se incluyen dos módulos: una base de datos de registros ciudadanos y una interfaz de gestión de tráfico que se conecta a un servidor de simulación por WebSocket.

## Requisitos

- Java 21 JDK
- Gradle 8+

No se necesita base de datos externa. CentralCore usa una base de datos H2 embebida que se crea automáticamente en el primer arranque en `~/centralcore_db.mv.db`.

## Ejecución

```bash
./gradlew run
```

Eso es todo. El esquema se inicializa automáticamente en el primer arranque.

Credenciales por defecto:
- Email: `admin@centralcore.local`
- Contraseña: `Admin1234`

## Estructura del proyecto

Este es un build Gradle multi-proyecto con tres subproyectos:

```
CentralCore/
├── build.gradle                  - configuración raíz
├── core/                         - la aplicación shell
│   └── src/main/java/com/centralcore/
│       ├── Main.java
│       ├── App.java
│       ├── controller/           - controladores FXML para todas las vistas
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
│       │   ├── DatabaseConnection.java   - singleton H2
│       │   └── SchemaInitializer.java    - crea tablas y siembra datos iniciales
│       ├── model/
│       │   ├── User.java
│       │   └── Licence.java
│       ├── modules/
│       │   ├── Module.java               - interfaz que todos los módulos deben implementar
│       │   ├── ModuleConfig.java         - POJO del module.json
│       │   ├── ModuleLoader.java         - descubre y carga desde ./modules/
│       │   └── ModuleManager.java        - gestor del ciclo de vida
│       └── util/
│           ├── CustomTitleBar.java       - barra de título personalizada y arrastrable
│           ├── ModuleDetailsDialog.java
│           ├── SceneManager.java         - navegación centralizada entre escenas
│           ├── SessionManager.java       - usuario actual en sesión
│           ├── LicenceStorage.java       - persiste claves de licencia con Java Prefs
│           ├── LicenseValidator.java     - validación HMAC-SHA256
│           └── TranslationManager.java   - i18n con patrón observer
└── modules/
    ├── CitizenModule/            - módulo de registros ciudadanos
    │   ├── module.json
    │   ├── build.gradle
    │   └── src/com/centralcore/modules/citizenmodule/
    │       ├── CitizenModule.java
    │       ├── CitizenModuleController.java
    │       ├── CitizenDAO.java
    │       ├── Citizen.java
    │       ├── CitizenDocument.java
    │       └── ...
    └── TrafficModule/            - módulo de gestión del tráfico
        ├── module.json
        ├── build.gradle
        └── src/com/centralcore/modules/trafficmodule/
            ├── TrafficModule.java
            ├── TrafficModuleController.java
            ├── MapCanvas.java            - renderer Canvas de JavaFX
            ├── SimConnection.java        - cliente WebSocket
            ├── TrafficDAO.java           - persistencia de incidentes
            └── model/
                ├── SimState.java
                ├── SimCar.java
                ├── TrafficLight.java
                ├── TrafficNode.java
                ├── TrafficEdge.java
                ├── Incident.java
                └── IncidentUpdate.java
```

## Esquema de base de datos

Tablas creadas automáticamente en el primer arranque:

| Tabla | Descripción |
|---|---|
| `users` | Usuarios de la aplicación, contraseñas hasheadas con BCrypt, roles |
| `licences` | Claves de licencia de módulos con fecha de expiración |
| `ciudadanos` | Registros ciudadanos (CitizenModule) |
| `vehiculos` | Vehículos registrados vinculados a ciudadanos |
| `incidentes_trafico` | Incidentes de tráfico con gravedad, estado y coordenadas |

CitizenModule también crea dos tablas adicionales en su propio init:
- `ciudadano_documentos`: archivos adjuntos vinculados a registros ciudadanos

## Flujo de navegación

```
Bienvenida -> Login -> Shell Principal
                        ├── Módulos (vista por defecto)
                        │     ├── CitizenModule
                        │     └── TrafficModule
                        ├── Instalaciones  - lista los módulos cargados con detalles
                        ├── Licencias      - añadir/eliminar claves de licencia
                        └── Ajustes        - idioma, controles de BD, zona peligrosa
```

## Sistema de módulos

Los módulos se cargan desde una carpeta `modules/` junto a la aplicación. Cada módulo necesita un `module.json` y sus clases compiladas en `build/classes/java/main/`.

```json
{
  "id": "mi_modulo",
  "name": "Mi Módulo",
  "version": "1.0.0",
  "description": "Lo que hace este módulo",
  "mainClass": "com.ejemplo.MiModulo",
  "author": "Nombre del Autor"
}
```

La clase principal debe implementar `com.centralcore.modules.Module` y tener un constructor sin argumentos. Cada módulo obtiene su propio `URLClassLoader` aislado para que no haya conflictos de clases entre módulos.

Para compilar un módulo:

```bash
cd modules/CitizenModule
./gradlew jar
```

## Sistema de licencias

Las licencias son claves firmadas con HMAC-SHA256 codificadas en Base64 con el formato `email|fecha_expiración|hmac`. La aplicación valida la firma y la expiración al importar, y guarda el resultado localmente mediante la API de Preferences de Java. Las claves de licencia de demostración para ambos módulos se insertan automáticamente en la base de datos.

## Dependencias

| Librería | Versión | Propósito |
|---|---|---|
| JavaFX | 21 | Framework de UI |
| H2 | 2.4.240 | Base de datos embebida |
| jBCrypt | 0.4 | Hash de contraseñas |
| Gson | 2.10.1 | Parseo del module.json |
| JUnit Jupiter | 5.10.2 | Tests unitarios |