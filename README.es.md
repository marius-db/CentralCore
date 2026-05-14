# CentralCore

> 🇬🇧 [English version](README.md)

Aplicación de escritorio para administración de ciudades construida sobre una arquitectura de plugins. El shell principal gestiona la autenticación, la carga de módulos y las licencias. La funcionalidad real vive en módulos compilados de forma independiente que se descubren y cargan en tiempo de ejecución mediante `URLClassLoader`.

Se incluyen dos módulos de serie: una base de datos de registros ciudadanos y una interfaz de gestión de tráfico que se conecta a un servidor de simulación por WebSocket.

El simulador de tráfico es un proyecto separado: [TrafficSim](https://github.com/marius-db/TrafficSim).
Las claves de licencia se pueden generar con la herramienta complementaria: [Licencing-gen](https://github.com/marius-db/Licencing-gen).

---

## Requisitos

Antes de nada, asegúrate de tener lo siguiente instalado:

- **Java 21 JDK**: la aplicación está compilada para Java 21. Vale cualquier distribución de JDK 21 (Eclipse Temurin, Oracle, Amazon Corretto, etc.). Asegúrate de que `java` y `javac` están en el PATH.
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

Esto también ejecuta todos los tests y produce la salida compilada de todos los subproyectos. Los shadow JARs de los módulos (JARs gordos con todas las dependencias incluidas) se generan automáticamente como parte de este paso y quedan aquí:

```
modules/CitizenModule/build/libs/CitizenModule.jar
modules/TrafficModule/build/libs/TrafficModule.jar
```

Para hacer una compilación limpia desde cero (útil si algo parece roto):

```bash
./gradlew clean build
```

---

## Generar el instalador

**Requisitos previos:**
- JDK 21+ con `jpackage`(incluido en la mayoría de distribuciones JDK 21)
- [WiX Toolset 3.14.1](https://github.com/wixtoolset/wix3/releases): usa el repositorio legacy v3, la v4 no es compatible con jpackage

**Pasos:**

1. Compila el proyecto:
   ```bash
   ./gradlew build
   ```

2. Empaqueta la aplicación (reemplaza la ruta de jpackage con la ubicación real de tu JDK):
   ```
   ruta\a\jpackage.exe --type exe --name CentralCore --input core/build/libs --main-jar centralcore-0.1.0.jar --main-class com.centralcore.Main --dest dist --icon core/src/main/resources/icon.ico --win-menu --win-shortcut --win-per-user-install --app-version 0.1.0
   ```

3. El instalador quedará en la carpeta `dist/`.

El flag `--win-per-user-install` instala en la carpeta AppData del usuario, por lo que no se requieren permisos de administrador.

---

## Sistema de módulos

### Cómo se cargan los módulos

Al arrancar, la aplicación escanea `~/.centralcore/modules/` y carga lo que encuentra. Esta carpeta se crea automáticamente en el primer arranque. También puedes forzar una recarga en cualquier momento con el botón **↻ Recargar módulos** de la pantalla de Instalaciones — apaga todos los módulos activos, vuelve a escanear la carpeta y los reinicializa, sin reiniciar la aplicación. Hay dos formatos soportados:

**Modo distribución (archivos JAR):** Coloca un JAR de módulo compilado directamente en `~/.centralcore/modules/`:

```
~/.centralcore/modules/
  CitizenModule.jar
  TrafficModule.jar
```

La aplicación lee el `module.json` desde dentro del JAR, carga la clase del módulo y lo inicializa. Este es el formato que usarías al distribuir la aplicación a otra persona.

**Modo desarrollo (clases compiladas):** Durante el desarrollo, los módulos se pueden cargar directamente desde la salida de compilación de Gradle sin empaquetarlos como JARs primero. Coloca la carpeta del proyecto del módulo dentro de `~/.centralcore/modules/` para que el cargador la encuentre:

```
~/.centralcore/modules/
  CitizenModule/
    build/classes/java/main/
    build/resources/main/
    module.json
  TrafficModule/
    ...
```

La carpeta del proyecto del módulo tiene que estar ahí, no basta con una referencia o acceso directo. Cuando ambos formatos están presentes para el mismo módulo, el JAR tiene prioridad y se omite el directorio.

Cada módulo tiene su propio `URLClassLoader` aislado para que no haya conflictos de clases entre módulos. El shell solo conoce la interfaz `Module`; las clases concretas del módulo son invisibles para él.

### Formato de module.json

Cada módulo necesita un `module.json` en la raíz de su carpeta de proyecto (modo desarrollo) o incluido dentro del JAR (modo distribución):

```json
{
  "id": "mi_modulo",
  "name": "Mi Módulo",
  "version": "1.0.0",
  "description": "Lo que hace este módulo",
  "mainClass": "com.ejemplo.MiModulo",
  "logoPath": "images/logo.png",
  "author": "Nombre del Autor"
}
```

### Crear un nuevo módulo

1. Crea una nueva subcarpeta dentro de `modules/` en el proyecto con su propio `build.gradle`, `module.json` y directorio `src/`.
2. Añádelo a `settings.gradle`: `include 'modules:MiModulo'`
3. La clase principal debe implementar `com.centralcore.modules.Module` y tener un constructor sin argumentos.
4. Para ejecutarlo, coloca la carpeta del proyecto compilada (o el shadow JAR generado) dentro de `~/.centralcore/modules/`.

La interfaz `Module` requiere: `getModuleId()`, `getName()`, `getVersion()`, `getDescription()`, `getLogoPath()`, `setModuleDir(File)`, `initialize()`, `shutdown()`, `reload()` y `getMainUI()`. El módulo puede llamar a su propio equivalente de `initSchema()` en `initialize()` para extender la base de datos H2 compartida con sus propias tablas.

### Compilar los JARs de los módulos

Para compilar el shadow JAR de un módulo concreto:

```bash
./gradlew :modules:CitizenModule:shadowJar
./gradlew :modules:TrafficModule:shadowJar
```

O simplemente ejecuta `./gradlew build` para compilarlo todo de una vez. Tras compilar, copia los JARs desde `modules/CitizenModule/build/libs/` a `~/.centralcore/modules/` para usar el modo distribución.

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
│           ├── DwmManager.java           - integración nativa DWM en Windows
│           ├── ModuleDetailsDialog.java
│           ├── PreferencesStorage.java   - persiste estado de la UI (posiciones de divisores, etc.)
│           ├── RememberMeStorage.java    - guarda credenciales cuando "recuérdame" está activo
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

## Módulos incluidos

### CitizenModule

CRUD completo para registros ciudadanos. El diseño es un `SplitPane`: una tabla con búsqueda en la izquierda y un panel de detalle/edición en la derecha. La posición del divisor se guarda y se restaura entre sesiones mediante `PreferencesStorage`.

El formulario de edición valida cada campo por separado y muestra etiquetas de error por campo. Se admiten archivos adjuntos: cada ciudadano puede tener documentos vinculados a su registro, categorizados por tipo (DNI/pasaporte, certificado de nacimiento, permiso de residencia, licencia de conducir, etc.).

Tablas utilizadas: `ciudadanos`, `ciudadano_documentos`. El módulo también migra la tabla `ciudadanos` en el primer arranque para añadir columnas que el schema del core no incluye (`lugar_nac`, `nacionalidad`, `codigo_postal`, `estado_civil`).

### TrafficModule

Interfaz de gestión de tráfico que se conecta a un servidor de simulación externo por WebSocket. El simulador es un proyecto separado: [TrafficSim](https://github.com/marius-db/TrafficSim).

El mapa se renderiza en un `Canvas` de JavaFX con un `AnimationTimer` para el bucle de renderizado en tiempo real. Todos los colores y fuentes están pre-cacheados como constantes estáticas para evitar alocaciones por frame. Una tabla de colores de densidad (20 pasos) se pre-calcula al cargar la clase para que no haya llamadas a `new Color()` dentro del bucle de renderizado. Las actualizaciones de las listas están limitadas a cada 5 ticks (~400ms) porque los ciclos de los semáforos duran varios segundos.

El panel lateral tiene pestañas para: semáforos (con override manual), densidad de tramos de vía, ruta de emergencia (haz clic para marcar el punto A y el punto B en el mapa, envía la solicitud de ruta al simulador), incidentes activos e historial de incidentes cerrados. El clic derecho en cualquier punto del mapa abre un menú contextual.

`SimConnection` gestiona el ciclo de vida del WebSocket: se conecta a `ws://localhost:8765` por defecto, se reconecta automáticamente cada 3 segundos si se cae la conexión, y enruta los mensajes entrantes por tipo (`map`, `state`, `ev_done`).

Tablas utilizadas: `traffic_incidents`, `traffic_incident_updates`.

---

## Esquema de base de datos

Las tablas se crean automáticamente en el primer arranque. No necesitas configurar nada manualmente.

| Tabla | Descripción |
|---|---|
| `users` | Usuarios de la aplicación con contraseñas hasheadas con BCrypt y roles |
| `licences` | Claves de licencia de módulos con fecha de expiración |
| `ciudadanos` | Registros ciudadanos (gestionados por CitizenModule) |
| `ciudadano_documentos` | Archivos adjuntos vinculados a registros ciudadanos |
| `traffic_incidents` | Incidentes de tráfico con tipo, coordenadas en el mapa y estado |
| `traffic_incident_updates` | Historial de actualizaciones de cada incidente |

El archivo de base de datos se encuentra en `~/.centralcore/centralcore_db.mv.db`. Puedes eliminarlo desde Ajustes dentro de la aplicación, o borrar el archivo manualmente para empezar desde cero.

> El `SchemaInitializer` del core también crea las tablas `vehiculos` e `incidentes_trafico` como parte del schema base, aunque ninguna de las dos la usan los módulos actuales.

---

## Flujo de navegación

```
Bienvenida -> Login -> Shell Principal
                        ├── Módulos (vista por defecto)
                        │     ├── CitizenModule
                        │     └── TrafficModule
                        ├── Instalaciones  - lista los módulos cargados con nombre, icono y descripción; el botón ↻ Recargar módulos en la esquina superior derecha recarga todos los módulos desde disco sin reiniciar la aplicación
                        ├── Licencias      - añadir/eliminar clave de licencia
                        └── Ajustes        - idioma, controles de BD, zona peligrosa
```

La pantalla de bienvenida detecta credenciales guardadas y muestra un botón de "Continuar sesión" si existen, autenticando en segundo plano al pulsarlo. El shell principal muestra un velo de licencia cuando no hay ninguna licencia válida; el velo no bloquea las pantallas de Licencias y Ajustes para que el usuario siempre pueda arreglarlo.

---

## Sistema de licencias

Las licencias son claves firmadas con HMAC-SHA256 codificadas en Base64 con el formato `email|fecha_expiración|hmac`. La aplicación valida la firma y la expiración al importar, comprueba que el email de la clave coincide con el del usuario en sesión, y guarda el resultado mediante la API de Preferences de Java.

El shell usa una sola licencia global almacenada en `centralcore/licences` de Java Prefs. Si no hay ninguna licencia activa, el área de contenido queda cubierta por el velo hasta que se añada una.

Las claves se pueden generar con [Licencing-gen](https://github.com/marius-db/Licencing-gen). Las claves de licencia de demostración para ambos módulos incluidos se insertan automáticamente en la base de datos en el primer arranque, pero son registros de base de datos; la licencia del shell es una clave separada que hay que añadir desde la pantalla de Licencias.

---

## Cuentas de usuario

La tabla `users` soporta dos roles: `admin` y `operator`. Las cuentas nuevas registradas desde la interfaz reciben el rol `operator`. La cuenta de administrador sembrada es:

- **Email:** `admin@centralcore.local`
- **Contraseña:** `Admin1234`
- **Licencia:** `YWRtaW5AY2VudHJhbGNvcmUubG9jYWx8MjExMi0xMi0zMXxrdGpQbWxVMVdwM2NEbkRWNkM4R1Z4OUdkbW9Hbk1NZXhuRkE5MzhRSWhjPQ==`

La casilla "recuérdame" en el login guarda las credenciales (ofuscadas en Base64, no cifradas) en `~/.centralcore/remember.conf`. Es ofuscación de conveniencia, no seguridad real.

---

## Archivos de configuración locales

CentralCore almacena algunos archivos y carpetas en `~/.centralcore/`:

| Ruta | Contenido |
|---|---|
| `modules/` | JARs de módulos o carpetas de proyecto que se cargan al arrancar |
| `centralcore_db.mv.db` | Archivo de base de datos H2 |
| `language.conf` | Último idioma seleccionado (`en` o `es`) |
| `remember.conf` | Credenciales guardadas cuando "recuérdame" está activo |
| `ui_prefs.conf` | Estado de la UI como posiciones de divisores de paneles |

---

## Tests

Los tests están en `core/src/test/java/com/centralcore/`.

`UnitTests.java` cubre `LicenseValidator` (clave válida, clave expirada, clave manipulada, extracción del email) y `UserDAO.hashPassword` (corrección del hash BCrypt, aleatoriedad del salt). No requiere base de datos.

`IntegrationTests.java` inyecta una conexión H2 en memoria en el singleton `DatabaseConnection` mediante reflexión, ejecuta un schema limpio en cada test y lo destruye al terminar. Cubre `testConnection`, `pingConnection`, registro de usuario, autenticación con credenciales correctas, autenticación con contraseña incorrecta y `findById`.

Ejecuta todos los tests con:

```bash
./gradlew test
```

---

## Dependencias

| Librería | Versión | Propósito |
|---|---------|---|
| JavaFX | 21      | Framework de UI |
| H2 | 2.4.240 | Base de datos embebida |
| jBCrypt | 0.4     | Hash de contraseñas |
| Gson | 2.10.1  | Parseo del module.json |
| JNA | 5.14.0  | Integración nativa DWM en Windows |
| Shadow | 9.4.1   | Empaquetado de módulos como fat JARs |
| JUnit Jupiter | 5.10.2  | Tests unitarios e integración |

---

## Repositorios relacionados

- [TrafficSim](https://github.com/marius-db/TrafficSim) - el servidor de simulación Python al que se conecta TrafficModule por WebSocket
- [Licencing-gen](https://github.com/marius-db/Licencing-gen) - la herramienta generadora de claves de licencia firmadas con HMAC