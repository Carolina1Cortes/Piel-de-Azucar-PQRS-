# APOLO

Aplicación web para gestionar las PQRS (peticiones, quejas, reclamos y sugerencias) de **Piel de Azúcar**, una PyME colombiana de cuidado facial. Busca reemplazar el manejo manual de solicitudes recibidas por distintos canales con una bandeja única de casos clasificados, priorizados y con control del plazo legal de 15 días hábiles.

- **Curso:** Ingeniería de Software I (SIA 2016701), Grupo 4, 2026-II, Universidad Nacional de Colombia, Sede Bogotá
- **Docente:** Santiago Leonardo Delgado Mejía
- **Equipo Apolo:** Linda Carolina Cortes Bustos, Antonio Garay Pinzón, Angel David Baez Camargo, Jorshua Leandro Coronado Romero, Sebastian David Perez Perez

## Estado actual: Sprint 1 (Incremento 1, revisión 04/11/2026)

El objetivo del incremento es permitir que un operador autenticado consulte un caso crítico, lo tome y envíe una respuesta personalizada. Las acciones relevantes quedan registradas en la bitácora. El flujo funciona localmente y no depende de servicios externos de mensajería.

### Funcionalidades implementadas

- **Inicio de sesión** con roles Operador y Admin mediante Spring Security. Las contraseñas se almacenan con hash BCrypt. Las rutas de la aplicación requieren autenticación, excepto las rutas públicas necesarias para el inicio de sesión y los recursos estáticos.
- **Entrada simulada de correos** (`/simulador`): permite registrar un correo entrante y crear una PQRS con su clienta y mensaje. En el formulario se selecciona manualmente el tipo de consulta; la prioridad se toma de la configuración de ese tipo.
- **Bandeja de casos** con filtros Todos / Críticos y visualización de estado, responsable y plazo en días hábiles.
- **Cálculo de plazos:** calcula la fecha límite de 15 días hábiles, excluyendo sábados, domingos y los festivos registrados en la tabla `festivo`.
- **Atención de casos críticos:** cuando un caso crítico no tiene responsable, se muestra el aviso "Automatización pausada" y se bloquea el formulario de respuesta hasta que un operador lo tome.
- **Toma de casos:** asigna el caso al operador, cambia su estado a "En atención", habilita el envío de respuestas y registra la acción en la bitácora.
- **Respuesta manual:** guarda el mensaje saliente en la conversación y lo entrega a `CanalSimulado`, que registra el envío en el log de la aplicación.
- **Trazabilidad:** las acciones relevantes de atención quedan asociadas al caso mediante registros de bitácora.

## Cómo funciona el flujo de atención

1. Un operador inicia sesión y accede a la bandeja de PQRS.
2. Un correo se registra mediante el simulador de entrada. El tipo de consulta se selecciona en el formulario y el caso hereda la prioridad configurada para ese tipo.
3. El sistema calcula la fecha límite de atención y presenta el caso en la bandeja.
4. El operador abre el detalle y consulta la información de la clienta, la conversación y la bitácora.
5. Si el caso es crítico y no tiene responsable, la aplicación pausa la automatización y exige que un operador lo tome antes de responder.
6. Al tomarlo, el operador queda como responsable y el caso pasa a estado "En atención".
7. El operador redacta y envía una respuesta. El mensaje queda guardado en la conversación y la acción se registra en la bitácora.

Las reglas de negocio de este flujo se encuentran en la capa de servicios. Los controladores reciben las solicitudes y coordinan la navegación, pero no acceden directamente a los repositorios ni concentran las reglas.

## Arquitectura del sistema

APOLO es un monolito web desarrollado con Spring Boot. Está organizado en capas con responsabilidades separadas:

| Capa | Responsabilidad |
|---|---|
| Presentación (`presentacion`) | Controladores Spring MVC y coordinación de las vistas Thymeleaf. |
| Servicios (`servicios`) | Reglas de negocio para registrar, consultar, tomar y responder PQRS, además del cálculo de días hábiles. |
| Integración (`integracion`) | Abstracción del canal de mensajería y su implementación simulada. |
| Persistencia (`persistencia`) | Repositorios Spring Data JPA para consultar y guardar información. |
| Dominio (`dominio`) | Entidades y enumeraciones que representan los conceptos del sistema. |
| Seguridad (`seguridad`) | Autenticación, roles y bloqueo de usuarios mediante Spring Security. |
| Configuración (`configuracion`) | Beans y propiedades compartidas de la aplicación. |

La dirección de dependencias es:

```text
presentacion → servicios → (integracion, persistencia) → dominio
```

La capa de servicios concentra las reglas de negocio. Esta separación permite probarlas sin levantar la interfaz web y facilita cambiar componentes de integración sin modificar los controladores.

### Estructura principal

```text
src/
├── main/
│   ├── java/co/edu/unal/apolo/
│   │   ├── presentacion/
│   │   ├── servicios/
│   │   ├── integracion/
│   │   ├── persistencia/
│   │   ├── dominio/
│   │   ├── seguridad/
│   │   └── configuracion/
│   └── resources/
│       ├── templates/
│       ├── static/
│       ├── schema.sql
│       ├── data.sql
│       └── application.properties
└── test/
```

## Seguridad

La autenticación y el control de acceso se implementan con Spring Security.

- Las contraseñas se verifican mediante BCrypt; no se almacenan en texto plano.
- La aplicación diferencia los roles Admin y Operador.
- Los usuarios bloqueados no pueden iniciar sesión.
- Las operaciones de tomar y responder también comprueban que el operador no esté bloqueado. Por tanto, el bloqueo se vuelve a validar al ejecutar estas acciones, incluso si el usuario ya tenía una sesión iniciada.
- Los formularios que modifican información utilizan protección CSRF.

En el alcance actual, los roles existen para la autenticación, pero todavía no hay restricciones de lectura de casos por responsable o por rol: cualquier usuario autenticado puede consultar los casos.

## Base de datos y persistencia

- **Motor:** H2 embebida en modo archivo.
- **Ubicación:** `./data/apolo` (el archivo de base de datos se encuentra dentro de `data/`, carpeta ignorada por Git).
- **Acceso a datos:** Spring Data JPA.
- **Esquema:** `src/main/resources/schema.sql` define las tablas y sus restricciones. Es la fuente versionada del esquema; Hibernate está configurado con `ddl-auto=none` y no crea ni modifica las tablas.
- **Datos iniciales:** `src/main/resources/data.sql` carga los datos de prueba y se puede ejecutar en cada arranque.
- **Tablas:** `cliente`, `usuario`, `tipo_consulta`, `plantilla_respuesta`, `pqrs`, `mensaje`, `adjunto`, `registro_bitacora` y `festivo`.

Para reiniciar la base de datos local desde cero, detén la aplicación y elimina la carpeta `data/`. Al iniciar de nuevo, se crearán las tablas y se cargarán los datos iniciales.

### Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Admin | `paula@pieldeazucar.co` | `apolo2026` |
| Operador | `operador1@pieldeazucar.co` | `apolo2026` |
| Operador bloqueado | `operador2@pieldeazucar.co` | `apolo2026` |

Estas credenciales son únicamente para el entorno de desarrollo y demostración. No deben utilizarse en producción.

## Requisitos

- **JDK 21**.
- No es necesario instalar Maven ni un servidor de base de datos: el proyecto incluye Maven Wrapper y utiliza H2.
- La primera ejecución necesita conexión a internet para descargar Maven y las dependencias. Una vez descargadas, la aplicación puede ejecutarse localmente sin servicios externos.

## Instalación y ejecución

Clona el repositorio y entra en la carpeta del proyecto. Luego ejecuta:

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

Abre <http://localhost:8080> en el navegador.

Para generar el artefacto ejecutable:

```bash
./mvnw package
java -jar target/apolo-0.1.0-SNAPSHOT.jar
```

La aplicación utiliza el puerto `8080` de forma predeterminada.

### Consola H2

La consola de desarrollo está disponible en <http://localhost:8080/h2-console>.

- **JDBC URL:** `jdbc:h2:file:./data/apolo`
- **Usuario:** `sa`
- **Contraseña:** vacía

La consola debe utilizarse únicamente en el entorno local de desarrollo.

## Pruebas y cobertura

Ejecutar todas las pruebas:

```bash
./mvnw test
```

Ejecutar la verificación completa, incluida la regla de cobertura de JaCoCo:

```bash
./mvnw verify
```

JaCoCo exige al menos un **80 % de cobertura de líneas en el paquete `servicios`**. El informe HTML se genera en:

```text
target/site/jacoco/index.html
```

La suite contempla distintos niveles de prueba:

| Tipo | Alcance |
|---|---|
| Servicios | Reglas de negocio, mediante pruebas unitarias con Mockito. |
| Repositorios | Consultas y persistencia sobre el esquema de H2. |
| Controladores | Rutas, pantallas y validación de formularios. |
| Integración | Autenticación, seguridad y flujos completos de atención. |
| Aplicación | Verificación de que el contexto de Spring Boot inicia correctamente. |

Entre las pruebas se encuentran `PqrsServiceTest`, `DiasHabilesServiceTest`, `EntradaSimuladaServiceTest`, `RepositoriosTest`, `InicioControllerTest`, `CasoControllerTest`, `SimuladorControllerTest`, `SeguridadIntegracionTest`, `FlujoCasoCriticoIntegracionTest` y `ApoloApplicationTests`.

## Alcance actual y funcionalidades pendientes

El estado de las funcionalidades debe interpretarse según el incremento documentado (Sprint 1, Incremento 1).

| Funcionalidad | Estado |
|---|---|
| Registro simulado de correos entrantes | Implementada |
| Selección manual del tipo de consulta | Implementada |
| Asignación de prioridad según el tipo de consulta | Implementada |
| Cálculo de plazos en días hábiles | Implementado |
| Bandeja con filtros Todos / Críticos | Implementada |
| Toma y autoasignación de casos | Implementada |
| Respuesta mediante canal simulado | Implementada |
| Registro de acciones en bitácora | Implementado |
| Clasificación automática por palabras clave | Pendiente |
| Sugerencia de plantillas por palabras clave | Pendiente |
| Envío real mediante Gmail (`GmailCanal`) | Pendiente |
| Escalar y delegar casos desde la interfaz | Pendiente |
| Resolver casos desde la interfaz | Pendiente |
| Administración de operadores desde la interfaz | Pendiente |

### Limitaciones conocidas

- **Clasificación manual:** el tipo de consulta se selecciona en el simulador. Todavía no se clasifica automáticamente a partir del contenido del correo.
- **Canal de mensajería:** `CanalSimulado` registra los envíos en el log; no se envían correos reales. Las propiedades de Gmail presentes en la configuración no significan que el envío real esté implementado.
- **Plantillas:** existen datos y entidades para plantillas de respuesta, pero todavía no se sugiere una plantilla automáticamente según palabras clave.
- **Gestión de casos:** escalar, delegar y resolver casos desde la interfaz están fuera del alcance de este incremento.
- **Administración de operadores:** el bloqueo de usuarios está contemplado en la lógica, pero no existe una pantalla para administrarlo.
- **Concurrencia:** todavía no hay control optimista de concurrencia para impedir que dos operadores tomen simultáneamente el mismo caso.
- **Visibilidad de casos:** cualquier usuario autenticado puede consultar cualquier caso; no existe todavía un filtro de lectura por responsable o rol.
- **Datos de referencia:** los tipos de consulta y los festivos cargados inicialmente son provisionales y deben contrastarse con la clasificación oficial y el calendario colombiano vigente. La exactitud de estos datos afecta el cálculo de los plazos.

Las funcionalidades pendientes se encuentran identificadas en el backlog del proyecto y corresponden a trabajo posterior o a elementos adicionales que no forman parte del alcance implementado en este incremento.

## Configuración de Gmail

El proyecto contempla propiedades reservadas para una posible configuración de Gmail en `src/main/resources/application.properties`:

```properties
apolo.gmail.usuario=
apolo.gmail.clave-aplicacion=
```

En el incremento actual no está implementada la clase `GmailCanal`; por tanto, completar estas propiedades no habilita por sí solo el envío real de correos. La respuesta se realiza mediante `CanalSimulado`.

Nunca subas credenciales al repositorio. Para configuraciones locales, utiliza un archivo `application-local.properties` que esté excluido de Git, cuando se implemente y habilite el mecanismo correspondiente.

## Qué sigue

Consultar [`docs/BACKLOG_SPRINT_1.md`](docs/BACKLOG_SPRINT_1.md) y el tablero del equipo: <https://github.com/users/Carolina1Cortes/projects/1/views/1>.

Entre las funcionalidades previstas para incrementos posteriores están la clasificación automática, la sugerencia de plantillas, la integración real con Gmail, la administración de operadores y las acciones de escalar, delegar y resolver casos.

## Convenciones del equipo

Consultar [`CONVENCIONES.md`](CONVENCIONES.md).

## Declaración del uso de inteligencia artificial

El andamiaje inicial de este sprint (estructura del proyecto, `pom.xml`, entidades, repositorios, `schema.sql`, `data.sql`, pruebas y documentación) fue generado con la asistencia de **Claude (Anthropic)** a partir del documento de contexto del proyecto. Los datos de `tipo_consulta` y el calendario de festivos son reconstrucciones provisionales que el equipo debe contrastar con la tabla de clasificación oficial y con el calendario de Colombia. El equipo es responsable de revisar, entender y ajustar todo el código entregado.

El equipo debe completar esta declaración indicando con precisión qué partes revisó, modificó o desarrolló cada integrante y qué herramientas de inteligencia artificial utilizó durante el incremento. La declaración debe reflejar el trabajo realizado realmente.
