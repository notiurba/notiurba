# Tareas — <nombre de la funcionalidad>

> **Fase 3 de 5 del flujo SDD.** Desglose de `plan.md` en pasos pequeños y
> verificables. Se crea con el plan **Aprobado** y se actualiza durante la
> implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **Sin iniciar** |
| Última actualización | 2026-11-11 |

## Cómo se usa

- Una tarea = un cambio pequeño que se puede probar solo.
- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Cada tarea nombra el criterio de aceptación (CA) que cubre y su prueba.
- Las tareas se agrupan en **PR pequeños** (rama `feature/…` desde `develop`).
- Se implementa **en orden**; si una tarea cambia el plan, se actualiza `plan.md` primero.
- Al terminar una tarea se marca aquí y, si cambió algo de fondo, se actualiza `docs/`.

## Tareas

### PR 1 — _título_ (rama `feature/___`)

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | Migración `V19_____.sql` | DB | CA-__ | Flyway migra limpio | [ ] |
| T-02 | ___ | Backend | CA-__ | `___Test` | [ ] |
| T-03 | ___ | Frontend | CA-__ | `___.test.jsx` | [ ] |

### PR 2 — _título_ (rama `feature/___`)

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-04 | ___ | ___ | CA-__ | ___ | [ ] |

## Pruebas finales (Fase 5)

- [ ] Todos los criterios CA-__ verificados (tabla de trazabilidad abajo).
- [ ] Backend: `mvn test` en verde. Frontend: `pnpm test` y `pnpm lint` en verde.
- [ ] Verificación manual en local (flujo completo, español, 360 px y 1366 px).
- [ ] Documentos actualizados: `spec.md`, `plan.md`, `docs/contexto/` si aplica.

## Trazabilidad

| Criterio | Tareas | Prueba | Resultado |
| --- | --- | --- | --- |
| CA-01 | T-02, T-03 | ___ | pendiente |

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-11 | Plantilla creada | tasks.md |
