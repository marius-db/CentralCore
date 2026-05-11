# CentralCore

> 🇬🇧 [English version](README.md)

Aplicación de escritorio para administración de ciudades construida sobre una arquitectura de plugins. El shell principal gestiona la autenticación, la carga de módulos y las licencias. La funcionalidad real vive en módulos compilados de forma independiente que se descubren y cargan en tiempo de ejecución mediante `URLClassLoader`.

Se incluyen dos módulos de serie: una base de datos de registros ciudadanos y una interfaz de gestión de tráfico que se conecta a un servidor de simulación por WebSocket.

---

## Requisitos

Antes de nada, asegúrate de tener lo siguiente instalado:

- **Java 21 JDK**:  la aplicación está compilada para Java 21. Vale cualquier distribución de JDK 21 (Eclipse Temurin, Oracle, Amazon Corretto, etc.). Asegúrate de que `java` y `javac` están en el PATH.
- **Git**: para clonar el repositorio.
- **IntelliJ IDEA** (recomendado): el proyecto está configurado como proyecto Gradle e IntelliJ lo importa todo automáticamente. La edición Community es gratuita y funciona perfectamente.

No necesitas instalar Gradle por separado. El proyecto incluye un wrapper de Gradle (`gradlew`) que descarga la versión correcta automáticamente la primera vez.

No necesitas ninguna base de datos externa. CentralCore usa una base de datos H2 embebida que se crea sola en el primer arranque.

---

## Obtener el proyecto

Clona el repositorio:

```bash
git clone <url-del-repositorio>
cd CentralCore
```

O descarga y extrae el ZIP si no tienes Git.

---

## Ejecutar la aplicación (desarrollo)

Desde la raíz del proyecto, ejecuta:

```bash
./gradlew :core:run
```
La primera vez tardará un momento mientras Gradle descarga las dependencias. Después arranca rápido.

En el primer arranque la aplicación crea automáticamente la base de datos H2 e inicializa todas las tablas. Puedes iniciar sesión de inmediato con la cuenta de administrador por defecto:

- **Email:** `admin@centralcore.local`
- **Contraseña:** `Admin1234`

Si usas IntelliJ IDEA, abre la carpeta del proyecto y deja que lo importe como proyecto Gradle. Luego ejecuta la clase `Main` dentro de `core/src/main/java/com/centralcore/`.

---

## Compilar el proyecto

Para compilar todo (core + todos los módulos):

```bash
./gradlew build
```

Esto también ejecuta los tests y produce la salida compilada de todos los subproyectos. Los shadow JARs de los módulos (JARs gordos con todas las dependencias incluidas) se generan automáticamente como parte de este paso y quedan aquí:

```
modules/CitizenModule/build/libs/CitizenModule.jar
modules/TrafficModule/build/libs/TrafficModule.jar
```

Para hacer una compilación limpia desde cero (útil si algo parece roto):

```bash
./gradlew clean build
```

---

## Sistema de módulos

### Cómo se cargan los módulos

Al arrancar, la aplicación escanea la carpeta `modules/` junto a la aplicación en ejecución y carga lo que encuentra. Hay dos formatos soportados:

**Modo distribución (archivos JAR):** Coloca un JAR de módulo compilado directamente en la raíz de `modules/`:

```
modules/
  CitizenModule.jar
  TrafficModule.jar
```

La aplicación lee el `module.json` desde dentro del JAR, carga la clase del módulo y lo inicializa. Este es el formato que usarías al distribuir la aplicación a otra persona.

**Modo desarrollo (clases compiladas):** Durante el desarrollo, los módulos se cargan directamente desde la salida de compilación de Gradle sin necesidad de empaquetarlos como JARs primero:

```
modules/
  CitizenModule/
    build/classes/java/main/
    build/resources/main/
    module.json
  TrafficModule/
    ...
```

Cuando ambos formatos están presentes para el mismo módulo, el JAR tiene prioridad y se omite el directorio.

### Formato de module.json

Cada módulo necesita un `module.json` en la raíz de su carpeta de proyecto (modo desarrollo) o incluido dentro del JAR (modo distribución):

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

### Crear un nuevo módulo

1. Crea una nueva subcarpeta dentro de `modules/` con su propio `build.gradle`, `module.json` y directorio `src/`.
2. Añádelo a `settings.gradle`: `include 'modules:MiModulo'`
3. La clase principal debe implementar `com.centralcore.modules.Module` y tener un constructor sin argumentos.
4. Cada módulo tiene su propio `URLClassLoader` aislado para que no haya conflictos de clases entre módulos.

### Compilar los JARs de los módulos

Para compilar el shadow JAR de un módulo concreto:

```bash
./gradlew :modules:CitizenModule:shadowJar
./gradlew :modules:TrafficModule:shadowJar
```

O simplemente ejecuta `./gradlew build` para compilarlo todo de una vez. Tras compilar, copia los JARs desde `modules/CitizenModule/build/libs/` a la raíz de `modules/` si quieres usar el modo distribución.

---

## Estructura del proyecto

```
CentralCore/
├── build.gradle                  - configuración raíz
├── settings.gradle               - declara todos los subproyectos
├── gradlew / gradlew.bat         - scripts del wrapper de Gradle
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
│       │   ├── DatabaseConnection.java   - singleton de conexión H2
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
    │       └── CitizenDocument.java
    └── TrafficModule/            - módulo de gestión del tráfico
        ├── module.json
        ├── build.gradle
        └── src/com/centralcore/modules/trafficmodule/
            ├── TrafficModule.java
            ├── TrafficModuleController.java
            ├── MapCanvas.java            - renderer Canvas de JavaFX para el mapa
            ├── SimConnection.java        - cliente WebSocket para el simulador
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

---

## Esquema de base de datos

Las tablas se crean automáticamente en el primer arranque. No necesitas configurar nada manualmente.

| Tabla | Descripción |
|---|---|
| `users` | Usuarios de la aplicación con contraseñas hasheadas con BCrypt y roles |
| `licences` | Claves de licencia de módulos con fecha de expiración |
| `ciudadanos` | Registros ciudadanos (gestionados por CitizenModule) |
| `vehiculos` | Vehículos registrados vinculados a ciudadanos |
| `incidentes_trafico` | Incidentes de tráfico con gravedad, estado y coordenadas en el mapa |
| `ciudadano_documentos` | Archivos adjuntos vinculados a registros ciudadanos |

El archivo de base de datos se encuentra en `~/centralcore_db.mv.db` (en tu directorio home). Puedes eliminarlo desde Ajustes dentro de la aplicación, o borrar el archivo manualmente para empezar desde cero.

---

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

---

## Sistema de licencias

Las licencias son claves firmadas con HMAC-SHA256 codificadas en Base64 con el formato `email|fecha_expiración|hmac`. La aplicación valida la firma y la expiración al importar, y guarda el resultado localmente mediante la API de Preferences de Java. Las claves de licencia de demostración para ambos módulos incluidos se insertan automáticamente en la base de datos en el primer arranque.

---

## Dependencias

| Librería | Versión | Propósito |
|---|---|---|
| JavaFX | 21 | Framework de UI |
| H2 | 2.4.240 | Base de datos embebida |
| jBCrypt | 0.4 | Hash de contraseñas |
| Gson | 2.10.1 | Parseo del module.json |
| JNA | 5.14.0 | Integración nativa DWM en Windows |
| Shadow | 8.3.0 | Empaquetado de módulos como fat JARs |
| JUnit Jupiter | 5.10.2 | Tests unitarios |