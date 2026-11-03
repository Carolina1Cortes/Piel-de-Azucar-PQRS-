# Convenciones del equipo Apolo

## Idioma y nombres
- Dominio, clases, métodos, variables, tablas y columnas en **español sin tildes ni eñes** (`fechaLimite`, `fecha_limite`, `tomarCaso`). Los métodos de Spring Data llevan el nombre en inglés que exige el framework (`findByEstado`).
- Clases en `PascalCase`, métodos y atributos en `camelCase`, constantes y valores de enumeración en `MAYUSCULAS_CON_GUION_BAJO`.
- Tablas y columnas en `snake_case`, tabla en singular (`pqrs`, `cliente`).
- Textos visibles para la usuaria, comentarios y mensajes de commit en español.
- Interfaces sin prefijo `I`. Implementaciones con nombre propio (`GmailCanal`, `CanalSimulado`).

## Capas y dependencias
- `presentacion → servicios → (integracion, persistencia) → dominio`. Está prohibido saltarse una capa.
- Los controladores solo reciben, validan formato, llaman a un servicio y eligen la vista. Nada de reglas de negocio.
- Las reglas de negocio (clasificación, plazo de 15 días hábiles, bloqueo de casos críticos, escalamiento, prioridad y estado) viven en `servicios` y deben tener prueba unitaria. Las operaciones CRUD no cuentan como lógica de negocio.
- Inyección de dependencias por constructor, sin `@Autowired` en atributos.
- Las enumeraciones se guardan como texto (`@Enumerated(EnumType.STRING)`), nunca por ordinal.

## Base de datos
- Todo cambio de esquema se hace en `schema.sql` (con `IF NOT EXISTS`) y se avisa en el PR. Nada de `ddl-auto`.
- Los datos de prueba van en `data.sql` con ids entre 1 y 999 y `MERGE ... KEY(id)`.

## Git
- Rama principal: `main`, siempre compila y pasa las pruebas. No se hace push directo.
- Una rama por issue: `feature/12-escalar-pqrs`, `fix/18-festivos`, `docs/20-readme`.
- Commits pequeños, en presente e imperativo: `Agrega repositorio de festivos`, `Corrige cálculo de fecha límite`.
- Todo cambio entra por Pull Request con al menos **una revisión** de otra persona del equipo. El PR referencia el issue (`Closes #12`).
- Desde la Entrega 4 todo trabajo parte de un issue en el repositorio, ligado al tablero de GitHub Projects.

## Pruebas
- JUnit 5 + AssertJ + Mockito. Nombre de la prueba en español describiendo el comportamiento: `rechazaElEnvioSiElCasoCriticoNoSeHaTomado`.
- Los servicios se prueban con Mockito (sin Spring). Los repositorios con `@DataJpaTest`. Los controladores con `@WebMvcTest`.
- Ninguna prueba puede depender de internet ni de Gmail: se usa `CanalSimulado`.
- Cobertura mínima del 80 % de líneas en `servicios` (`./mvnw verify`).

## Seguridad y datos
- Nunca subir contraseñas, correos reales ni la carpeta `data/`.
- Las contraseñas se guardan con BCrypt (`contrasena_hash`).
- Las credenciales de Gmail van en `application-local.properties`, que está en `.gitignore`.

## Revisión antes de abrir un PR
1. `./mvnw verify` pasa.
2. No hay código comentado ni `System.out.println`.
3. Las reglas de negocio nuevas tienen prueba.
4. El README o este documento se actualizan si cambia algo de cómo se ejecuta o se trabaja.
