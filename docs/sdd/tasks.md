# Tareas — NOT-129: API del mapa, del mapa de calor y de las estadísticas

> **Fase 3 de 5 del flujo SDD.** Desglose de [plan.md](plan.md) (aprobado el 11 nov) en pasos
> pequeños y verificables. Se actualiza durante la implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **Implementado y probado** — mapa (`aa4eec0`) y calor (`f29a9a2`) con commit; estadísticas sin commit; falta la llamada real y el PR |
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

## Pruebas finales (Fase 5)

- [x] Todos los criterios CA verificados (tabla de trazabilidad).
- [x] Backend: `mvn clean test` en verde con base PostGIS: 289 pruebas, 0 fallos, 10 omitidas que no tocan
      este cambio (8 de enrutamiento de correo sin `NOTIURBA_TEST_DB_URL` y 2 con proveedores reales).
- [ ] Llamada real a la API levantada (no solo MockMvc) con token ADMIN.
- [ ] Documentos al día: `spec.md`, `plan.md` y `docs/contexto/11` y `12` hechos; faltan el Google Doc y el vault.

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
| CA-08 | T-05 | ídem | ✅ mapa ~0,6 s, calor ~0,9 s, estadísticas ~0,3 s (base local) |
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
