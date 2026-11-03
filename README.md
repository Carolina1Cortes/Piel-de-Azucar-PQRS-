# APOLO

Aplicación web para gestionar las PQRS (peticiones, quejas, reclamos y sugerencias) de **Piel de Azúcar**, una PyME colombiana de cuidado facial. Reemplaza el manejo manual por WhatsApp, Gmail e Instagram con una bandeja única de casos clasificados, priorizados y con control del plazo legal de 15 días hábiles.

- **Curso:** Ingeniería de Software I (SIA 2016701), Grupo 4, 2026-II, Universidad Nacional de Colombia, Sede Bogotá
- **Docente:** Santiago Leonardo Delgado Mejía
- **Equipo Apolo:** Linda Carolina Cortes Bustos, Antonio Garay Pinzón, Angel David Baez Camargo, Jorshua Leandro Coronado Romero, Sebastian David Perez Perez

## Estado actual: Sprint 1 (Entrega 4)

Proyecto base funcionando: monolito Spring Boot, base de datos H2 con el esquema versionado, estructura por capas, capa de acceso a datos completa y una bandeja de solo lectura para ver los datos de prueba. Todavía **no** hay inicio de sesión, ni reglas de negocio, ni conexión real con Gmail (ver `docs/BACKLOG_SPRINT_1.md` y la sección "Qué sigue").

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
./mvnw test       # corre todas las pruebas (sin internet)
./mvnw verify     # además exige 80 % de cobertura de líneas en el paquete servicios
```

El informe de cobertura queda en `target/site/jacoco/index.html`.

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

(Aún no se usan: el inicio de sesión llega en un sprint posterior.)

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
