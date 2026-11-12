# Tareas — NOT-129: API del mapa, del mapa de calor y de las estadísticas

> **Fase 3 de 5 del flujo SDD.** Desglose de [plan.md](plan.md) (aprobado el 11 nov) en pasos
> pequeños y verificables. Se actualiza durante la implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **Implementado, revisado y probado de punta a punta** — mapa `aa4eec0`, calor `f29a9a2`, estadísticas `355d0c3`; revisión y QA sin commit; falta el PR |
| Última actualización | 2026-11-11 |

## Cómo se usa

- Una tarea = un cambio pequeño que se puede probar solo.
- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Cada tarea nombra el criterio (CA) que cubre y su prueba. **La prueba se escribe primero y se ve fallar.**
- Commits y PR solo cuando Andrés lo pida, **sin coautoría de Claude**.

## PR A — Mapa (rama `feature/not-129-api-mapa-calor-estadisticas`)

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | Validación compartida de filtros (`FiltroMapa`): comuna 1–10, estado, fechas; el estado reutiliza la regla del listado sin cambiar su comportamiento | Backend | CA-10 | `MapaAdminServicioTest` (6) | [x] |
| T-02 | Pruebas de integración del mapa (vigente, vencido, sin ubicación, sin comuna, filtros, 401/403, sin datos personales) | Pruebas | CA-01, 02, 07, 11, 12 | `MapaAdminIntegracionTest` (8) | [x] |
| T-03 | `MapaAdminRepositorio.mapa` con `count(*) OVER()` y tope de 1.000 | Backend | CA-01, 02, 12, 13 | `MapaAdminIntegracionTest` | [x] |
| T-04 | `MapaAdminController` `GET /api/admin/mapa` | Backend | CA-01, 07 | `MapaAdminIntegracionTest` | [x] |
| T-05 | Prueba de rendimiento: 1.001 reportes, ≤ 2 s (la prueba completa tardó 1,1 s) | Pruebas | CA-08 | `MapaAdminIntegracionTest` | [x] |
| T-06 | Contrato §5.2: `GET /api/admin/mapa` y filtros comunes, con ejemplo y errores | Docs | CA-09 | revisión | [x] |

Resultado del PR A (11 nov): pruebas escritas primero y vistas fallar (no compilaban); luego 14/14 en verde
contra PostGIS real. Mutación manual: al aflojar la regla de vigencia, la prueba de vigentes falla. Suite
completa del backend: 269 pruebas, 0 fallos, 10 omitidas que no tocan este cambio (8 de enrutamiento de
correo que piden `NOTIURBA_TEST_DB_URL` y 2 con proveedores reales). Sin commit todavía.

## PR B — Calor y comunas críticas

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-07 | Pruebas: pesos que suman, ranking por comuna, paginación (5/10), `sinComuna`, tope | Pruebas | CA-03, 04, 10, 11, 13 | `MapaAdminIntegracionTest` (+7), `MapaAdminServicioTest` (+4) | [x] |
| T-08 | `GET /api/admin/mapa/calor` con `comunasCriticas` y `categoriaPrincipal` | Backend | CA-03, 04 | ídem | [x] |
| T-09 | Contrato §5.3: calor | Docs | CA-09 | revisión | [x] |

Resultado del PR B (11 nov): pruebas vistas fallar (no compilaban) y luego 25/25 en verde contra PostGIS
real. Mutación manual: al invertir el desempate de la categoría principal, la prueba de comunas críticas
falla. Medición aislada: con ~1.200 reportes, el mapa tarda ~0,6 s y el calor ~0,9 s. Suite completa: 280
pruebas, 0 fallos, las mismas 10 omitidas. Sin commit todavía.

## PR C — Estadísticas

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-10 | Pruebas: totales con ceros, vigencia (3 valores), atendidos, base vacía | Pruebas | CA-05, 06, 10, 11 | `MapaAdminIntegracionTest` (+6), `MapaAdminServicioTest` (+3) | [x] |
| T-11 | `GET /api/admin/estadisticas` (una consulta agrupada, suma en Java) | Backend | CA-05, 06 | ídem | [x] |
| T-12 | Contrato §5.4: estadísticas; `docs/contexto/12` al día | Docs | CA-09 | revisión | [x] |

Resultado del PR C (11 nov): pruebas vistas fallar (no compilaban) y luego 34/34 en verde contra PostGIS
real. Mutación manual: al quitar la condición de «fuera de plazo», la prueba de vigencia falla. Medición
aislada: con 1.000 reportes las estadísticas tardan ~0,3 s. Suite completa: 289 pruebas, 0 fallos, las
mismas 10 omitidas. Sin commit todavía.

## Revisión de código y QA de punta a punta (11 nov)

`/code-review` (nivel alto) sobre `develop...HEAD` dio 7 hallazgos; 6 corregidos con pruebas y 1 dejado
como propuesta. El QA contra el servidor real encontró un octavo problema (el JIT), el más grave.

| Hallazgo | Antes | Después |
| --- | --- | --- |
| Calor: dos consultas podían descuadrarse si entraba un reporte entre ellas | READ COMMITTED | `REPEATABLE READ`: ranking + sinComuna = totalReportes siempre |
| `porCategoria` con filtro de categoría mostraba las demás en 0 («filtrada» parecía «sin reportes») | 15 filas, 14 en 0 | solo la categoría filtrada |
| Validación de filtros copiada del listado del panel | 2 copias | `FiltroPanel.de(...)` única |
| SQL de filtros copiado del listado del panel | 2 copias | `FiltroPanel.condiciones(...)` única |
| Filtros como 5 a 7 argumentos posicionales del mismo tipo | fácil de intercambiar | un `FiltroPanel` por nombre |
| Pruebas de tiempo frágiles (una sola medición) | ruido de la máquina | mejor de 3 tras calentar |
| **JIT de PostgreSQL compilaba cada consulta espacial** (hallado en el QA real) | estadísticas 0,28 s y listado 0,30 s con 13 reportes; 1.000 reportes: mapa 0,57 s, calor 0,90 s, estadísticas 0,31 s | `SET LOCAL jit = off`: 0,017 s y 0,020 s; 1.000 reportes: 0,054 s, 0,125 s y 0,039 s |
| Comuna recalculada en cada lectura (costo crece con los reportes) | — | **sin cambio**: propuesta de V19 para guardar `id_comuna` (decisión de Andrés) |

Pruebas: backend 291, 0 fallos, 10 omitidas que no tocan este cambio. Pruebas nuevas: `FiltroPanelTest` (6),
categoría filtrada (1) y JIT apagado en las cuatro consultas (1). QA de punta a punta
(`tools/qa-panel-admin/qa-panel.sh`): **44 de 44** en verde contra el jar real, con base desechable:
401/403 reales, 400 con su campo, CORS, totales que cuadran con la base, filtros, orden, paginación,
regresión del listado y tiempos; 0 errores en el log del servidor.

## V19 y V20 — Comuna más rápida (aprobado el 12 nov)

| ID | Tarea | Capa | Prueba | Estado |
| --- | --- | --- | --- | --- |
| T-13 | V19: `comuna.geog` generada + índice GiST + función con `geog` | DB | `ComunaGeografiaIntegracionTest` (misma respuesta que la función anterior) | [x] `5864258` |
| T-14 | V20: `reporte.id_comuna` y `comuna_origen` + triggers + índice + vista | DB | `ComunaGuardadaIntegracionTest` (crear, mover, cambiar capa, barrio y distancia) | [x] `fcb0bcf` |
| T-15 | Panel, detalle y correo leen `r.id_comuna`; quitar `sinJit` si sobra | Backend | suites existentes + QA de punta a punta | [x] `fb4ea64`, `6fae1ec` |
| T-16 | Medición con 10.000 reportes antes/después | Pruebas | `medir-comuna` | [x] plan.md §12 |

Resultado (12 nov): con 10.000 reportes, estadísticas de 31,5 s a 12 ms y mapa filtrado por comuna de 16 s
a 2 ms; el costo estimado bajó a 1.000–2.300 y el JIT ya no se activa, así que se quitó `sinJit` y su
prueba. `ComunaGeografiaIntegracionTest` compara 900 puntos contra la función anterior: 0 diferencias.
Mutación manual: al quitar la condición del barrio en el trigger, la prueba del barrio falla. Suite
completa: 300 pruebas, 0 fallos, las mismas 10 omitidas. QA de punta a punta: 44 de 44, rutas en 6–11 ms.
Figma: la pestaña «Estadísticas» se agregó a la navegación de «Panel administrativo · Reportes» y
«· Mapa de calor» (y «Cerrar sesión» quedó en 280 de ancho para que quepa).

## Pruebas finales (Fase 5)

- [x] Todos los criterios CA verificados (tabla de trazabilidad).
- [x] Backend: `mvn clean test` en verde con base PostGIS: 300 pruebas (12 nov), 0 fallos, 10 omitidas que
      no tocan este cambio (8 de enrutamiento de correo sin `NOTIURBA_TEST_DB_URL` y 2 con proveedores reales).
- [x] Llamada real a la API levantada (no solo MockMvc) con token ADMIN: QA de punta a punta, 44 de 44.
- [x] Documentos al día: `spec.md`, `plan.md`, `docs/contexto/11` y `12`, Linear (NOT-129, NOT-133, NOT-136) y vault.

## Trazabilidad

| Criterio | Tareas | Prueba | Resultado |
| --- | --- | --- | --- |
| CA-01 | T-02, T-03, T-04 | `MapaAdminIntegracionTest` | ✅ |
| CA-02 | T-02, T-03 | ídem | ✅ |
| CA-03 | T-07, T-08 | ídem | ✅ |
| CA-04 | T-07, T-08 | ídem | ✅ |
| CA-05 | T-10, T-11 | ídem | ✅ |
| CA-06 | T-10, T-11 | ídem | ✅ |
| CA-07 | T-02, T-04 | ídem | ✅ las tres rutas |
| CA-08 | T-05, T-13 a T-16 | ídem + medición con 10.000 | ✅ con 10.000 reportes: mapa 11 ms, calor 9 ms, estadísticas 12 ms (base local) |
| CA-09 | T-06, T-09, T-12 | revisión | ✅ contrato §5.2 a §5.4 |
| CA-10 | T-01, T-07, T-10 | `MapaAdminServicioTest` | ✅ filtros, paginación y vigencia |
| CA-11 | T-02, T-07, T-10 | `MapaAdminIntegracionTest` | ✅ las tres rutas |
| CA-12 | T-02, T-03 | ídem | ✅ |
| CA-13 | T-03, T-07 | ídem | ✅ |

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-11 | Plan aprobado; tareas creadas; Figma y Linear (NOT-129, 133, 136) ajustados | tasks.md |
| 2026-11-11 | PR A (mapa) en commit `aa4eec0`; PR B (calor) hecho y probado | tasks.md |
| 2026-11-11 | Figma: nueva pantalla «Panel administrativo · Estadísticas» (vigencia, indicadores, por estado, comuna y categoría) | plan.md §7 |
| 2026-11-11 | Calor en commit `f29a9a2`; comentarios del código más cortos y sin citar tareas (pedido de Andrés) | tasks.md |
| 2026-11-11 | Pantalla de estadísticas rehecha con el Design System (Nunito Sans, tokens de color, chips de estado, filtros de 42 px) | plan.md §7 |
| 2026-11-11 | Estadísticas implementadas y probadas (PR C) | tasks.md |
| 2026-11-11 | Revisión de código (6 de 7 corregidos), arreglo del JIT, QA de punta a punta 44/44; Neon: JIT no disponible (`pg_jit_available() = false`) | tasks.md |
| 2026-11-11 | Medición con 10.000 reportes y propuesta V19 (A y C) pendiente de aprobación | plan.md §12 |
| 2026-11-12 | V19 y V20 aprobados e implementados; lecturas con `r.id_comuna`; `sinJit` quitado; medición y QA 44/44 | plan.md §12, tasks.md |
| 2026-11-12 | Hook del equipo y subagente `code-reviewer` (`.claude/`) | CLAUDE.md |
| 2026-11-12 | Figma: pestaña «Estadísticas» en la navegación de «Reportes» y «Mapa de calor» | plan.md §7 |
