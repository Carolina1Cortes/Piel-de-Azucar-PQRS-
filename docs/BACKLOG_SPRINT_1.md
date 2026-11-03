# Backlog del Sprint 1 (Entrega 4, 04/11/2026)

Alcance: proyecto base Spring Boot + H2, estructura por capas, capa de acceso a datos, repositorio y README.
Cada fila es un issue para crear en el repositorio y vincular al tablero de GitHub Projects.

| # | Issue | Estado | Criterio de aceptación |
|---|---|---|---|
| S1-01 | Crear proyecto Maven con Spring Boot 3, Java 21 y `mvnw` | Hecho en el andamiaje | `./mvnw spring-boot:run` arranca la app |
| S1-02 | Estructura por capas (paquetes y `package-info`) | Hecho | Paquetes `presentacion`, `servicios`, `integracion`, `persistencia`, `dominio`, `seguridad`, `configuracion` |
| S1-03 | Esquema relacional versionado (`schema.sql`) | Hecho | 9 tablas con CHECK, FK y UNIQUE; se puede ejecutar varias veces |
| S1-04 | Entidades y enumeraciones del dominio | Hecho | 9 entidades y 8 enumeraciones mapeadas al esquema |
| S1-05 | Repositorios Spring Data para las 9 entidades | Hecho | Consultas derivadas y `findAllConDetalle` probadas |
| S1-06 | Datos de prueba (`data.sql`) | Hecho, **revisar** | Cubre todos los estados, prioridades, un caso crítico y uno vencido |
| S1-07 | Pruebas de la capa de acceso a datos | Hecho | `RepositoriosTest` pasa sin internet |
| S1-08 | Bandeja de PQRS de solo lectura (Thymeleaf) | Hecho | `/` lista las PQRS, funciona en el celular |
| S1-09 | README, convenciones y declaración de IA | Hecho, **completar** | Un solo comando documentado; declaración con la autoría real |
| S1-10 | Verificar que JaCoCo exige 80 % en `servicios` | Por hacer | `./mvnw verify` falla si baja del 80 % |
| S1-11 | Reemplazar `tipo_consulta` por la tabla oficial de clasificación | **Por hacer (equipo)** | Las 8 categorías con su prioridad y automatización reales |
| S1-12 | Verificar festivos de Colombia 2026 y 2027 | **Por hacer (equipo)** | Contrastar `festivo` con el calendario oficial |
| S1-13 | Configurar integración continua y protección de `main` | Por hacer | El PR exige el workflow `CI` en verde y una revisión |
| S1-14 | Repartir la exposición de la Entrega 4 | Por hacer | Los cinco integrantes intervienen |

## Fuera del Sprint 1
Inicio de sesión (Spring Security), cálculo de días hábiles, clasificación por palabras clave, `GmailCanal`, tarea programada, CU-02 y CU-03. Resolver también la incoherencia pendiente de `asignarResponsable` entre la secuencia de CU-03 y el diagrama de clases.
