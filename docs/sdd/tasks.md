# Tareas — NOT-129: API del mapa, del mapa de calor y de las estadísticas

> **Fase 3 de 5 del flujo SDD.** Desglose de [plan.md](plan.md) (aprobado el 11 nov) en pasos
> pequeños y verificables. Se actualiza durante la implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **En curso** — PR A (mapa) hecho y probado, sin commit; siguen PR B y PR C |
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
| T-07 | Pruebas: pesos que suman, ranking por comuna, paginación (5/10), `sinComuna`, tope | Pruebas | CA-03, 04, 10, 11, 13 | `MapaAdminIntegracionTest`, `MapaAdminServicioTest` | [ ] |
| T-08 | `GET /api/admin/mapa/calor` con `comunasCriticas` y `categoriaPrincipal` | Backend | CA-03, 04 | ídem | [ ] |
| T-09 | Contrato §5: calor | Docs | CA-09 | revisión | [ ] |

## PR C — Estadísticas

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-10 | Pruebas: totales con ceros, vigencia (3 valores), atendidos, base vacía | Pruebas | CA-05, 06, 10, 11 | `MapaAdminIntegracionTest`, `MapaAdminServicioTest` | [ ] |
| T-11 | `GET /api/admin/estadisticas` (una consulta agrupada, suma en Java) | Backend | CA-05, 06 | ídem | [ ] |
| T-12 | Contrato §5: estadísticas; `docs/contexto/12` sin «falta» | Docs | CA-09 | revisión | [ ] |

## Pruebas finales (Fase 5)

- [ ] Todos los criterios CA verificados (tabla de trazabilidad).
- [ ] Backend: `mvn clean test` en verde con base PostGIS (decir si algo se omitió).
- [ ] Llamada real a la API local con token ADMIN.
- [ ] Documentos al día: `spec.md`, `plan.md`, `docs/contexto/11` y `12`; Google Doc y vault al cerrar.

## Trazabilidad

| Criterio | Tareas | Prueba | Resultado |
| --- | --- | --- | --- |
| CA-01 | T-02, T-03, T-04 | `MapaAdminIntegracionTest` | ✅ mapa |
| CA-02 | T-02, T-03 | ídem | ✅ mapa |
| CA-03 | T-07, T-08 | ídem | pendiente |
| CA-04 | T-07, T-08 | ídem | pendiente |
| CA-05 | T-10, T-11 | ídem | pendiente |
| CA-06 | T-10, T-11 | ídem | pendiente |
| CA-07 | T-02, T-04 | ídem | ✅ mapa; falta calor y estadísticas |
| CA-08 | T-05 | ídem | ✅ mapa; falta calor y estadísticas |
| CA-09 | T-06, T-09, T-12 | revisión | ✅ mapa; falta calor y estadísticas |
| CA-10 | T-01, T-07, T-10 | `MapaAdminServicioTest` | ✅ filtros comunes; falta paginación y vigencia |
| CA-11 | T-02, T-07, T-10 | `MapaAdminIntegracionTest` | ✅ mapa; falta calor y estadísticas |
| CA-12 | T-02, T-03 | ídem | ✅ |
| CA-13 | T-03, T-07 | ídem | ✅ mapa; falta calor |

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-11 | Plan aprobado; tareas creadas; Figma y Linear (NOT-129, 133, 136) ajustados | tasks.md |
