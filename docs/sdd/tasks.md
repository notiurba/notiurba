# Tareas — Envío real a las entidades y aviso de rebotes

> **Fase 3 de 5 del flujo SDD.** Desglose de `plan.md` en pasos pequeños y
> verificables. Se crea con el plan **Aprobado** y se actualiza durante la
> implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **En curso** |
| Última actualización | 2026-11-13 |

## Cómo se usa

- Una tarea = un cambio pequeño que se puede probar solo.
- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Cada tarea nombra el criterio de aceptación (CA) que cubre y su prueba.
- Un solo PR para toda la historia (decisión de Andrés, 12 nov 2026), con un commit por tarea.
- Se implementa **en orden**; si una tarea cambia el plan, se actualiza `plan.md` primero.
- Al terminar una tarea se marca aquí y, si cambió algo de fondo, se actualiza `docs/`.

## Tareas

### PR único — Envío real y rebotes (rama `feature/not-131-envio-real-y-rebotes`)

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | Pruebas de la cola con y sin destino de pruebas: con destino sale solo a esa dirección con «[PRUEBA]»; sin destino sale al destino de la ruta y el envío pasa a «Enviado» | Backend | CA-01, CA-02 | `EnrutamientoIntegracionTest` | [x] |
| T-02 | Pruebas del webhook: sin firma, firma falsa y firma vencida dan 401 `FIRMA_INVALIDA` sin cambiar nada; `email.complained` marca rebote; identificador vacío o de más de 100 caracteres da 400; otros eventos se ignoran | Backend | CA-03, CA-04, CA-05 | `EnrutamientoIntegracionTest` | [x] |
| T-03 | Pruebas de propiedad y de fallo no reintentable: `/envio` y `/mensaje` de un reporte ajeno dan 404; un fallo no reintentable queda fallido con el motivo amable y no se reintenta solo | Backend | CA-08, CA-09 | `EnrutamientoIntegracionTest` | [x] |
| T-04 | Función de presentación del bloque de correo del detalle (qué texto, etiqueta y si se ofrecen otros canales) con sus pruebas | Frontend | CA-06, CA-07 | `estadoCorreo.test.mjs` | [x] |
| T-05 | Bloque «Correo a la entidad» en `DetalleReporte.jsx`: carga `estadoCorreo`, muestra el estado y, si el correo no llegó, `OtrosCanales`; si `/envio` falla, el detalle se ve igual | Frontend | CA-06, CA-07 | `pnpm test`, `pnpm lint`, `pnpm build` y revisión en el navegador (360 y 1366 px) | [x] |
| T-06 | Guía `docs/contexto/14-activar-envio-real.md`, contrato de API, variables de Render y sprint al día | Docs | CA-10 | Revisión contra `application.yml` | [x] |

## Pruebas finales (Fase 5)

- [ ] Todos los criterios CA-01 a CA-10 verificados (tabla de trazabilidad abajo).
- [ ] Backend: `mvn clean test` en verde. Frontend: `pnpm test` y `pnpm lint` en verde.
- [ ] Verificación manual en local (detalle con envío en camino, enviado y rebotado; español, 360 px y 1366 px).
- [ ] Revisión con el subagente `code-reviewer` y hallazgos corregidos.
- [ ] Documentos actualizados: `spec.md`, `plan.md`, `docs/contexto/`.

## Trazabilidad

| Criterio | Tareas | Prueba | Resultado |
| --- | --- | --- | --- |
| CA-01 | T-01 | `EnrutamientoIntegracionTest` | pendiente |
| CA-02 | T-01 | `EnrutamientoIntegracionTest` | pendiente |
| CA-03 | T-02 | `EnrutamientoIntegracionTest` | pendiente |
| CA-04 | T-02 | `EnrutamientoIntegracionTest` | pendiente |
| CA-05 | T-02 | `EnrutamientoIntegracionTest` | pendiente |
| CA-06 | T-04, T-05 | `estadoCorreo.test.mjs` y revisión manual | pendiente |
| CA-07 | T-04, T-05 | `estadoCorreo.test.mjs` y revisión manual | pendiente |
| CA-08 | T-03 | `EnrutamientoIntegracionTest` | pendiente |
| CA-09 | T-03 | `EnrutamientoIntegracionTest` | pendiente |
| CA-10 | T-06 | Revisión manual | pendiente |

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-13 | Spec y plan aprobados; tareas escritas | spec.md, plan.md, tasks.md |
