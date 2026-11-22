# Tareas — Mapa del panel: reportes sin ubicación

> **Fase 3 de 5 del flujo SDD.** Desglose de `plan.md` en pasos pequeños y
> verificables. Se actualiza durante la implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **Hecho** (T-01 a T-09; integrado en `main` con el PR #52 el 19/11/2026) |
| Última actualización | 2026-11-22 |

## Cómo se usa

- Una tarea = un cambio pequeño que se puede probar solo.
- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Cada tarea nombra el criterio de aceptación (CA) que cubre y su prueba.
- **Un solo PR** (decisión de Andrés, 12/11/2026), rama `feature/mapa-reportes-sin-ubicacion` cortada desde `origin/main`, con un commit atómico por tarea. No se hace commit ni push sin que Andrés lo pida.
- Se implementa en orden. Cada prueba se escribe primero y se comprueba que **falla** sin el cambio.
- Los comandos del frontend se corren desde `frontend/` con pnpm.

## Tareas

### PR único — Mapa del panel sin pantalla en blanco (rama `feature/mapa-reportes-sin-ubicacion`)

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | `separarPorUbicacion(reportes)` en `admin/servicios/reportesService.js` | Frontend | CA-01, CA-04, CA-05, CA-08 | `reportesService.test.mjs`: mezcla, todos sin ubicación, `null`, `undefined`, `NaN`, texto, `0` válido, no muta la entrada | [x] |
| T-02 | Componente `admin/componentes/AvisoSinUbicacion.jsx` | Frontend | CA-03 | `AvisoSinUbicacion.test.mjs`: 0 no pinta, 1 singular, N plural | [x] |
| T-03 | Componente de clase `compartido/componentes/LimiteErrores.jsx` con «Reintentar» | Frontend | CA-06 | `LimiteErrores.test.mjs`: hijo que lanza error muestra el aviso; hijo sano no cambia; «Reintentar» vuelve a dibujar | [x] |
| T-04 | `LayoutAdmin.jsx` envuelve `<Outlet />` con `<LimiteErrores key={location.pathname}>` | Frontend | CA-06, CA-07 | `LayoutAdmin.test.mjs`: con una ruta que lanza error se ven la cabecera y el aviso; al cambiar de pestaña se ve la nueva | [x] |
| T-05 | `MapaComunas.jsx` (`VistaMapa`): usa `separarPorUbicacion`, dibuja y cuenta solo `conUbicacion` y pinta `AvisoSinUbicacion` | Frontend | CA-01, CA-02, CA-03, CA-04, CA-08 | `MapaComunas.test.mjs` (Leaflet sustituido por stubs en `soporte-pruebas/`: mezcla, ninguno y todos con ubicación) + verificación manual local en el navegador + `pnpm build` | [x] |
| T-06 | Correr `pnpm test && pnpm lint && pnpm build` completos | Frontend | todos | 254 antes → 273 después, 0 fallos; lint y build en verde | [x] |
| T-07 | Subagente `code-reviewer` sobre la rama; corregir los hallazgos con pruebas | — | todos | Hallazgos 1 (prueba del cableado), 2 (texto de «Reintentar»), 3 (plan), 4 (margen del aviso) y 6 corregidos; 5, 7 y 8 dejados por decisión (ver registro) | [x] |
| T-08 | Verificación manual en producción tras el despliegue: `/admin/mapa` se ve, 1 marcador (`NU-000009`) y el aviso «4 reportes no tienen ubicación…» | — | CA-01, CA-02, CA-03 | Captura y consola sin errores; la hace Andrés con su sesión o con la extensión de Chrome | [x] |
| T-09 | Cierre: `docs/contexto/12` (problemas conocidos y número de pruebas), archivar este expediente en `docs/sdd/historico/` | Docs | — | Skill `cierre-de-historia`. Hecho: `docs/contexto/12`, README de la simulación y conteo de la skill de pruebas. Falta: archivar este expediente tras T-08 | [x] |

## Fuera de las tareas (decisión de Andrés)

- Crear reportes con ubicación en producción para tener más puntos en la presentación.
- Historia en Linear (o contarlo dentro de NOT-135/NOT-140) y textos finales con Katherine.
- Integrar `main` en `develop` o decidir el futuro de `develop`.

## Pruebas finales (Fase 5)

Trazabilidad criterio → tarea → resultado. Se rellena al cerrar.

| Criterio | Tarea | Resultado |
| --- | --- | --- |
| CA-01 | T-01, T-05, T-08 | Función y mapa con pruebas en verde; local en navegador OK; producción verificada por Andrés el 19/11/2026 (T-08) |
| CA-02 | T-05, T-08 | Local en navegador OK (casilla de calor sin errores); producción verificada por Andrés el 19/11/2026 (T-08) |
| CA-03 | T-02, T-05, T-08 | Pruebas en verde; local OK («4 reportes no tienen ubicación…»); producción verificada por Andrés el 19/11/2026 (T-08) |
| CA-04 | T-01, T-05 | Pruebas en verde (función y mapa) |
| CA-05 | T-01 | Pruebas en verde |
| CA-06 | T-03, T-04 | Pruebas en verde; visto en el navegador con el fallo real de Leaflet |
| CA-07 | T-04 | Prueba en verde |
| CA-08 | T-01, T-05 | Pruebas en verde (función y mapa) |

## Registro de cambios

- 2026-11-19: spec y plan aprobados por Andrés; se crea este desglose.
- 2026-11-19: implementación T-01 a T-06. Medido: pruebas del frontend 254 → 273, 0 fallos; lint y build en verde. Probado en el navegador con la simulación local (30 visibles + aviso de 4; sin el arreglo de `MapaComunas` el límite de errores atrapa el fallo real de Leaflet).
- 2026-11-19: revisión de `code-reviewer` (T-07). Corregido: prueba del cableado de `MapaComunas` (con mutación comprobada: falla sin el cambio), texto de «Reintentar», nota del plan sobre el registro de errores, margen del aviso, orden de imports. Dejado sin cambio: «1 reportes visibles» en singular (ya existía; fuera de la spec), recorrido doble en `separarPorUbicacion` (claridad) y el helper `texto` repetido en las pruebas.
- 2026-11-19: T-08. Andrés confirmó en producción que `/admin/mapa` se ve y funciona (captura con 1 marcador, `NU-000009`, y el aviso «4 reportes no tienen ubicación y no aparecen en el mapa»). PR #52 integrado en `main`.
- 2026-11-22: T-09. Se archiva el expediente. Nota: el PR #53 (22/11/2026) rehízo el mapa con `GET /api/admin/mapa`, que ya excluye los reportes sin ubicación y vencidos; `separarPorUbicacion` y `AvisoSinUbicacion` quedaron sin uso (anotado en `docs/contexto/08-pendientes.md`). `LimiteErrores` sigue siendo necesario y se conserva.
