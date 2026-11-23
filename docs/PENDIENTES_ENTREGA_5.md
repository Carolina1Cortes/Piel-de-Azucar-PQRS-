# Pendientes de la Entrega 5 (APOLO)

Fuente: *Planeación de la Entrega 5 — APOLO* (borrador, noviembre de 2026), contrastada con el código de `main` (commit `08937cb`, 23/11/2026).

> **Cómo leer el estado.** Se revisó el código y los textos del repositorio, pero **no se ejecutó `./mvnw verify`** (sin acceso a Maven desde donde se redactó esta lista). Todo lo marcado "Hecho" en código debe confirmarse con pruebas en verde antes de cerrarlo.

Leyenda: ✅ hecho en `main` · 🟡 parcial o por verificar · ⬜ pendiente

## 1. Alcance comprometido (Debe)

| ID | Elemento | Resp. | Tam. | Estado | Qué falta |
|---|---|---|---|---|---|
| E5-01 | Lenguaje neutro en las pantallas | Antonio | S | 🟡 | No quedan "Clienta" en plantillas; revisar a ojo bandeja, detalle y simulador (p. ej. "Simulada", "registrada", "entrante") y confirmar con el profesor. |
| E5-02 | Lenguaje neutro en el código | Antonio | S | 🟡 | Sin coincidencias de "clienta" en `src`; revisar variables, validaciones, log de `CanalSimulado` y comentarios, y correr las pruebas. |
| E5-03 | README: ejecución local, Codespaces y canal mock | Antonio | S | ✅ | Ya explica H2 solo en local, URL de la pestaña Puertos y que el canal es mock. Solo releerlo al final. |
| E5-04 | Ajustes de arranque en rama principal | Antonio | S | 🟡 | Hay `forward-headers-strategy` y reloj de Bogotá (`RelojConfig`). Falta probar en un Codespace **nuevo**: arranque, cierre de sesión al login y hora de Bogotá en la bitácora. |
| E5-05 | Canal como columna en la bandeja | Jorshua | S | ✅ | Columna "Canal" en `inicio.html`. |
| E5-06 | Radicado y confirmación de recepción | Sebas | M | ✅ | Radicado `PQRS-AAAA-NNNNNN`, visible en bandeja y detalle, con mensaje de confirmación saliente por el canal mock. Falta confirmar que tiene pruebas. |
| E5-07 | Clasificación automática por palabras clave | Jorshua | M | 🟡 | `ClasificacionService` existe. Falta **validar la lista de palabras clave con Paula Mesa** (hoy son provisionales) y verificar el caso "enrojecimiento → crítico" y "sin coincidencia → por clasificar". |
| E5-08 | Clasificación integrada al simulador | Carolina | S | ⬜ | `registrarCorreoEntrante` aún exige `tipoConsultaId` y `ClasificacionService` no se usa en ningún lado. Quitar el selector manual (o dejarlo opcional), clasificar con asunto y cuerpo, y calcular prioridad y plazo con el tipo resultante. |
| E5-09 | Estado Atención inmediata | Angel | M | 🟡 | `tomarCaso` ya pasa los casos críticos a `ATENCION_INMEDIATA`. Falta que el equipo confirme la regla (propuesta: al tomarlo), verificar que se vea en bandeja y detalle, y probar el filtro "Críticos" con este estado. |
| E5-10 | Lista de chequeo del protocolo crítico | Sebas | M | ✅ | `completarPaso` y `POST /pqrs/{id}/protocolo/{paso}`. Verificar pruebas y que quede en la bitácora. |
| E5-11 | Notas internas obligatorias | Jorshua | M | ⬜ | Existe la columna `notas_internas`, pero no hay captura. Definir en qué acciones es obligatoria (propuesta: al resolver), validarlo en el servicio y garantizar que **nunca** aparece en la conversación con la persona cliente. |
| E5-12 | Bitácora de todos los cambios de estado | Carolina | S | 🟡 | Hoy se registran tomar, responder y pasos del protocolo. Falta registrar resolver (y escalar/delegar si entran) y revisar que cada registro tenga fecha, usuario y acción. |
| E5-13 | Resolver caso con resolución categorizada | Angel | M | ⬜ | No existe endpoint ni pantalla de resolver. Hay que definir las categorías de resolución (hoy solo hay `descripcion_solucion` libre), agregar la columna/enumeración en `schema.sql`, pasar a `RESUELTA`, registrar en bitácora y bloquear nuevas respuestas. |
| E5-14 | Datos de referencia verificados | Sebas | S | 🟡 | El último PR (#20) corrigió festivos y tipos de consulta y el README cita las fuentes (verificado 22/11/2026). Falta una segunda persona que contraste con el calendario oficial. |
| E5-15 | Cobertura y verificación | Jorshua | M | ⬜ | Correr `./mvnw verify` (BUILD SUCCESS, ≥ 80 % en `servicios`) y agregar pruebas para cada historia nueva (E5-08, E5-11, E5-12, E5-13). |

## 2. Si hay capacidad (no comprometido)

Ninguno está implementado en `main`.

| ID | Elemento | Tam. | Prioridad | Notas |
|---|---|---|---|---|
| E5-16 | Evidencia en casos críticos | M | Debería | Existe la tabla/entidad `adjunto`, pero no hay subida ni registro en bitácora (falta también un tipo de acción para archivos). |
| E5-17 | Sugerencia de plantilla de respuesta | M | Debería | Hay entidad y datos de `plantilla_respuesta` con `palabras_clave`; falta el servicio y la UI editable. |
| E5-18 | Escalar caso | M | Debería | Existen `ESCALADA` en estados y bitácora; falta servicio, motivo y nota obligatorios, y pantalla. |
| E5-19 | Delegar caso | M | Debería | Existen `DELEGADA` en estados y bitácora; falta servicio y pantalla. |
| E5-20 | Administración de operadores (solo Admin) | M | Debería | El bloqueo ya se valida al iniciar sesión y al actuar; falta pantalla y restringir la ruta al rol Admin. |
| E5-21 | Historial general de casos (CU-07) | M | Debería | Falta consulta con filtros que incluya resueltos. |
| E5-22 | Control de concurrencia al tomar un caso | M | Debería | Sin `@Version` ni bloqueo; dos operadores pueden tomar el mismo caso a la vez. |
| E5-23 | Visibilidad por rol | M | Debería | Cualquier usuario autenticado ve todo; no hay `hasRole` en `SeguridadConfig`. |
| E5-24 | Documentación alineada | S | Debería | Ver sección 4. |
| E5-25 | Refresco de mensajes nuevos | M | Podría | Sin refresco ni notificaciones. |
| E5-26 | Consola H2 solo para Admin | S | Podría | `/h2-console` solo exige estar autenticado (`anyRequest().authenticated()`). |

## 3. Decisiones y confirmaciones del equipo (sección 10 de la planeación)

- [ ] Confirmar responsable, tamaño y prioridad de cada elemento.
- [ ] Definir la fecha de la Entrega 5.
- [ ] Copiar a la planeación los **compromisos de mejora de la retrospectiva** (sección 3.4) que afecten alcance o forma de trabajo.
- [ ] Decidir la regla de **Atención inmediata** (¿al tomar el caso o al llegar?).
- [ ] Decidir en qué acciones se exige **nota interna** (propuesta: al resolver).
- [ ] Definir la lista de **categorías de resolución**.
- [ ] Validar las **palabras clave** con la dueña del negocio.
- [ ] Si no alcanza el tiempo, mover elementos de la sección 1 a la sección 2 e informar el motivo en la revisión.

## 4. Documentación y proceso

- [ ] `docs/BACKLOG_SPRINT_1.md` está desactualizado: dice que inicio de sesión, días hábiles, clasificación y CU-03 quedan "fuera del Sprint 1", cuando ya existen. Crear el backlog de la Entrega 5 con una sola numeración de historias.
- [ ] README: la sección "Estado actual" y "Qué sigue" siguen en Sprint 1 y listan la clasificación automática como futura. Actualizar al terminar (qué se completó, qué no y por qué).
- [ ] README: aclarar que la columna `canal` solo admite `GMAIL` (los otros canales quedan fuera del alcance).
- [ ] Backlog S1-10: comprobar que JaCoCo **falla** `./mvnw verify` por debajo del 80 %.
- [ ] Backlog S1-13: **no hay `.github/`**; crear el workflow `CI` y proteger `main` (PR con una revisión).
- [ ] Backlog S1-09: declaración de uso de IA con la autoría real del equipo.
- [ ] Crear un issue por cada elemento y ligarlo al tablero de GitHub Projects (CONVENCIONES.md).

## 5. Fuera del alcance de la Entrega 5

- Envío real por Gmail (`GmailCanal`) y otros canales además del simulado.

## 6. Definición de terminado (checklist final)

- [ ] Todo lo comprometido está en `main` antes de iniciar la sesión.
- [ ] La aplicación corre en un Codespace nuevo siguiendo solo el README.
- [ ] `./mvnw verify` termina en BUILD SUCCESS con ≥ 80 % de cobertura de líneas en servicios.
- [ ] Cada historia nueva tiene pruebas y registra en la bitácora cuando cambia el estado de un caso.
- [ ] README y backlog reflejan el estado real.
