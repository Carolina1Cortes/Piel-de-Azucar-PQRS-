# Estado de la Entrega 5

Estado de los 15 elementos comprometidos en la planeación de la Entrega 5, tal como están en `main`.
El canal de mensajería sigue siendo un mock (`CanalSimulado`): la aplicación no envía correos reales.

| ID | Elemento | Estado | Dónde |
|---|---|---|---|
| E5-01 | Lenguaje neutro en las pantallas | Hecho | PR #16 |
| E5-02 | Lenguaje neutro en el código | Hecho | PR #16 |
| E5-03 | README: ejecución local, Codespaces y canal mock | Hecho | PR #16 |
| E5-04 | Ajustes de arranque en la rama principal | Hecho según el PR #16; falta comprobarlo en un Codespace nuevo | PR #16 |
| E5-05 | Canal como columna en la bandeja | Hecho | PR #21 |
| E5-06 | Radicado y confirmación de recepción | Hecho | PR #14 |
| E5-07 | Clasificación automática por palabras clave | Hecho como servicio (`ClasificacionService`) | PR #17 |
| E5-08 | Clasificación integrada al simulador | **No hecho.** El simulador sigue pidiendo el tipo a mano; falta conectar `ClasificacionService`. Pasa a "si hay capacidad" | — |
| E5-09 | Estado Atención inmediata | Hecho: al tomar un caso crítico, visible en bandeja y detalle | PR #13 y #15 |
| E5-10 | Lista de chequeo del protocolo crítico | Hecho | PR #14 |
| E5-11 | Notas internas obligatorias | Hecho | PR #25 |
| E5-12 | Bitácora de todos los cambios de estado | Hecho | PR #25 |
| E5-13 | Resolver caso con resolución categorizada | Hecho | commit `253fdbb` |
| E5-14 | Datos de referencia verificados | Hecho: tipos de consulta y festivos 2026 y 2027 | PR #20 |
| E5-15 | Cobertura y verificación | Hecho: `./mvnw verify` exige 80 % de cobertura en `servicios` (JaCoCo) | PR #18 |

## Decisiones de negocio tomadas
- **Atención inmediata:** un caso crítico pasa a ese estado al tomarlo.
- **Notas internas obligatorias:** al resolver (y al escalar, cuando exista). Nunca se muestran en la conversación con la persona cliente.
- **Resolver un caso crítico:** exige completar los cuatro pasos del protocolo.

## Pendiente
- E5-08 (ver arriba) y los elementos de "si hay capacidad" de la planeación.
- Festivo del 13 de julio de 2026 (Ley 2578 de 2026): hay una demanda ante la Corte Constitucional; revisar si cambia.
