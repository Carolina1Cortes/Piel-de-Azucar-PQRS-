# APOLO

Aplicación web para gestionar las PQRS (peticiones, quejas, reclamos y sugerencias) de **Piel de Azúcar**, una PyME colombiana de cuidado facial. Reemplaza el manejo manual por WhatsApp, Gmail e Instagram con una bandeja única de casos clasificados, priorizados y con control del plazo legal de 15 días hábiles.

- **Curso:** Ingeniería de Software I (SIA 2016701), Grupo 4, 2026-II, Universidad Nacional de Colombia, Sede Bogotá
- **Docente:** Santiago Leonardo Delgado Mejía
- **Equipo Apolo:** Linda Carolina Cortes Bustos, Antonio Garay Pinzón, Angel David Baez Camargo, Jorshua Leandro Coronado Romero, Sebastian David Perez Perez

## Estado actual: Sprint 1

Objetivo del incremento: un operador autenticado puede abrir un caso crítico, tomarlo y enviar una respuesta personalizada, y cada acción queda en la bitácora. Todo funciona sin servicios externos, con el canal simulado.

Ya funciona:

- **Inicio de sesión** con roles Operador y Admin (Spring Security + BCrypt). Sin sesión no se entra a ninguna pantalla; un operador bloqueado no puede ingresar.
- **Entrada simulada de correos** (`/simulador`): crea una PQRS con su clienta, su mensaje entrante, canal Gmail, prioridad según el tipo de consulta y fecha límite a 15 días hábiles.
- **Bandeja** con filtro Todos / Críticos y plazo en días hábiles (sin fines de semana ni festivos de la tabla `festivo`).
- **Caso crítico sin asignar:** banner "Automatización pausada" y caja de respuesta bloqueada.
- **Tomar caso:** autoasignación, estado "En atención", desbloqueo del envío y registro en bitácora.
- **Enviar respuesta manual** por `CanalSimulado`, guardada en la conversación y registrada en bitácora.

Todavía **no** hay `GmailCanal` real, ni sugerencia de plantillas por palabras clave (extras del sprint), ni escalar, delegar o administrar (siguiente incremento). Ver `docs/BACKLOG_SPRINT_1.md` y la sección "Qué sigue".

## Requisitos

Solo **JDK 21**. No hay que instalar Maven, servidores de base de datos ni variables de entorno. La primera ejecución necesita internet para descargar Maven y las dependencias; después todo funciona sin conexión.

## Cómo ejecutarlo (un solo comando)

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

Luego abrir <http://localhost:8080>. La base de datos se crea sola en la carpeta `data/` con el esquema y los datos de prueba.

Para generar el artefacto ejecutable único:

```bash
./mvnw package
java -jar target/apolo-0.1.0-SNAPSHOT.jar
```

## Pruebas

```bash
./mvnw test       # corre todas las pruebas
./mvnw verify     # además exige 80 % de cobertura de líneas en el paquete servicios
```

La suite contempla pruebas de la lógica de negocio, controladores, persistencia, autenticación, autorización y del flujo de atención de casos. En particular:

- `SeguridadIntegracionTest` verifica que las pantallas protegidas redirijan al login cuando no hay sesión, que las credenciales válidas permitan ingresar y que un operador bloqueado no pueda autenticarse.
- `InicioControllerTest` verifica la bandeja, el filtro de casos críticos y la presentación de los plazos en días hábiles.
- Las pruebas de servicios verifican las reglas para tomar casos, enviar respuestas y registrar las acciones en la bitácora.

El informe de cobertura queda en `target/site/jacoco/index.html`. La verificación de JaCoCo exige al menos un 80 % de cobertura de líneas en el paquete `servicios`.

## Arquitectura por capas

```
co.edu.unal.apolo
├── presentacion    Controladores Spring MVC (las vistas están en resources/templates)
├── servicios       Lógica de negocio. Es lo que mide la cobertura del 80 %
├── integracion     CanalMensajeria: Gmail real o canal simulado
├── persistencia    Repositorios Spring Data sobre H2
├── dominio         Entidades y enumeraciones (usadas por todas las capas)
├── seguridad       (transversal) login, roles y bloqueo, con Spring Security
└── configuracion   Beans y propiedades
```

Regla de dependencias: `presentacion → servicios → (integracion, persistencia) → dominio`. Un controlador nunca llama a un repositorio.

## Base de datos

- H2 embebida en archivo (`data/apolo.mv.db`, ignorado por git).
- `src/main/resources/schema.sql` define las 9 tablas (`cliente`, `usuario`, `tipo_consulta`, `plantilla_respuesta`, `pqrs`, `mensaje`, `adjunto`, `registro_bitacora`, `festivo`). Es la **única fuente de verdad del esquema**: Hibernate está en `ddl-auto=none`.
- `src/main/resources/data.sql` carga los datos de prueba. Es idempotente: se puede ejecutar en cada arranque.
- Los ids de datos de prueba son 1 a 999; los ids generados arrancan en 1000.
- Para empezar de cero: detener la app y borrar la carpeta `data/`.
- Consola H2 para la sustentación: <http://localhost:8080/h2-console> con JDBC URL `jdbc:h2:file:./data/apolo`, usuario `sa`, sin contraseña.

### Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Admin | `paula@pieldeazucar.co` | `apolo2026` |
| Operador | `operador1@pieldeazucar.co` | `apolo2026` |
| Operador **bloqueado** | `operador2@pieldeazucar.co` | `apolo2026` (no puede ingresar; sirve para mostrar el bloqueo) |

### Guion corto para la demostración

1. Abrir <http://localhost:8080>: redirige al login. Probar `operador2@...` (bloqueado) y ver que no ingresa.
2. Ingresar con `operador1@pieldeazucar.co`. En la bandeja, filtrar por **Críticos** (caso #2).
3. Abrir el caso #2: banner "Automatización pausada" y envío bloqueado.
4. **Tomar caso**: pasa a "En atención", queda el operador como responsable y aparece en la bitácora.
5. Escribir y enviar la respuesta: queda en la conversación y en la bitácora (el envío sale en el log como `[CANAL SIMULADO]`).
6. Opcional: **Simular correo entrante** con un tipo de "Reacciones" para crear un caso crítico nuevo.

## Configuración de Gmail

En `src/main/resources/application.properties`:

```properties
apolo.gmail.usuario=
apolo.gmail.clave-aplicacion=
```

Con ambos valores vacíos (el valor por defecto) la aplicación usa el **canal simulado** con datos de prueba y sigue siendo útil. Gmail es un servicio externo no esencial: sin él, APOLO funciona completo. Nunca subir credenciales al repositorio; usar `application-local.properties` (ignorado por git).

## Qué sigue

Ver `docs/BACKLOG_SPRINT_1.md` y el tablero del equipo: <https://github.com/users/Carolina1Cortes/projects/1/views/1>. Próximas entregas: reglas de negocio de extremo a extremo con pruebas (E5, 23/11) y sistema completo (E6, 09/12).

## Convenciones del equipo

Ver [`CONVENCIONES.md`](CONVENCIONES.md).

## Declaración del uso de IA

El andamiaje inicial de este sprint (estructura del proyecto, `pom.xml`, entidades, repositorios, `schema.sql`, `data.sql`, pruebas y documentación) fue generado con la asistencia de **Claude (Anthropic)** a partir del documento de contexto del proyecto. Los datos de `tipo_consulta` y el calendario de festivos son reconstrucciones provisionales que el equipo debe contrastar con la tabla de clasificación oficial y con el calendario de Colombia. El equipo es responsable de revisar, entender y ajustar todo el código entregado.

> **Equipo: completar aquí** qué partes revisó y modificó cada integrante.
