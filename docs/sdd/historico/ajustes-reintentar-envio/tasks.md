# Tareas — Ajustes al envío de reportes tras la auditoría del PR #54

> **Fase 3 de 5 del flujo SDD.** Desglose de `plan.md` en pasos pequeños y
> verificables. Se actualiza durante la implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **Cerrado con una tarea sin registro** (T-01 a T-09 y T-11 hechas; integrado en `main` con el PR #54 el 20/11/2026; T-10 sin registro) |
| Última actualización | 2026-11-22 |

## Cómo se usa

- Una tarea = un cambio pequeño que se puede probar solo.
- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Cada tarea nombra el criterio de aceptación (CA) que cubre y su prueba.
- **Un solo PR** (decisión de Andrés, 12/11/2026), rama `feature/ajustes-reintentar-envio` cortada desde `origin/fix/reintentar-envio-20261120` (PR #54), con commits atómicos. T-01 y T-02 van juntas en un commit porque la prueba y el helper son un solo cambio. No se hace commit ni push sin que Andrés lo pida.
- Los comandos del frontend se corren desde `frontend/` con pnpm.
- Es un refactor sin efecto visible: **no hay pruebas que fallen antes**. Cada prueba se valida con una mutación manual (se rompe el código, la prueba debe fallar, se revierte).

## Tareas

### PR único — Ajustes del envío (rama `feature/ajustes-reintentar-envio`)

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | `estadoEnvio.test.mjs`: tabla de 6 combinaciones (`guardado` × `permiteCorreo`) y tabla de `textoProgresoEnvio`; quitar las filas con `enviando` y la prueba «centraliza…» | Frontend | CA-04, CA-05, CA-06 | La propia tabla; mutación comprobada (T-04) | [x] |
| T-02 | `estadoEnvio.js`: `textoAccionConfirmacion({ guardado, permiteCorreo })` sin `enviando` | Frontend | CA-04, CA-06 | Tabla de T-01 | [x] |
| T-03 | `PasoConfirmacion.jsx`: `accion` sin `enviando`; en el `catch` de `enviar`, liberar `ocupado` y `enviando` en las dos primeras líneas; comentario de una línea sobre el éxito; sangría de la llave | Frontend | CA-01, CA-02, CA-03, CA-07 | `pnpm lint`; revisión de código; T-05 | [x] |
| T-04 | Mutaciones manuales de las pruebas: invertir `permiteCorreo` y devolver «Reintentar» sin `guardado` (tabla de acción); devolver siempre «Guardando y enviando…» (progreso). Cada una debe hacer fallar la prueba; se revierte | Frontend | CA-04, CA-05, CA-06 | Resultado anotado en el registro | [x] |
| T-05 | Prueba local con un arnés desechable de `react-test-renderer` y `fetch` falso, en un worktree temporal (no hubo navegador): envío exitoso (la hoja no muestra «Reintentar» ni «Confirmar» activo), error de envío (hoja cierra y botón se habilita) y **mutación local** (`decidirFalloEnvio` lanza una excepción; el botón se habilita; se revierte, no se sube) | Frontend | CA-01, CA-02, CA-03 | Capturas o texto de lo observado en el registro | [x] |
| T-06 | `pnpm test && pnpm lint && pnpm build` completos en la rama y en una fusión de prueba con `origin/main` (worktree temporal, sin subir) | Frontend | CA-08 | Conteo de pruebas antes → después en el registro | [x] |
| T-07 | `docs/contexto/08-pendientes.md`: una viñeta en «Deuda técnica conocida» (después de la de Postman) con lo fuera de alcance de la spec (sección 7) | Docs | — | `git merge-file` contra `origin/main` y contra la rama del PR #53 sin conflicto | [x] |
| T-08 | Subagente `code-reviewer` sobre la rama; corregir los hallazgos con pruebas | — | todos | Informe en el registro | [x] |
| T-09 | Texto de la descripción del PR con los 5 pasos de prueba manual (éxito con el archivo de «Reporte enviado» bloqueado, error 500, fallo de foto, entidad sin correo, «Enviar de todas maneras») | Docs | CA-09 | Entregado a Andrés | [x] |
| T-10 | Prueba manual en la vista previa de Netlify tras abrir el PR (crea un reporte real: el correo sale a la bandeja de pruebas) | — | CA-01, CA-02 | La hace Andrés | [ ] |
| T-11 | Cierre: archivar este expediente en `docs/sdd/historico/` tras integrar el PR | Docs | — | Skill `cierre-de-historia`; carpeta `historico/ajustes-reintentar-envio/` (sin NOT-nn, no hay historia en Linear) | [x] |

## Fuera de las tareas (decisión de Andrés)

- Destino del PR: decidido por Andrés (2026-11-20), se empuja directo a la rama del #54.
- Traer `main` a la rama con un merge.
- Los puntos de la sección 7 de la spec (van a pendientes, no se construyen).

## Pruebas finales (Fase 5)

Trazabilidad criterio → tarea → resultado. Se rellena al cerrar.

| Criterio | Tarea | Resultado |
| --- | --- | --- |
| CA-01 | T-03, T-05, T-10 | Arnés desechable: en el éxito la hoja sigue abierta con «Confirmar» bloqueado y sin «Reintentar»; la mutación con `finally` lo hace fallar. Vista previa pendiente (T-10) |
| CA-02 | T-03, T-05, T-10 | Arnés: error 500 cierra la hoja, libera el botón y deja reintentar; fallo tras guardar muestra «Reintentar» y queda libre. Vista previa pendiente (T-10) |
| CA-03 | T-03, T-05 | Arnés con `decidirFalloEnvio` lanzando excepción: el código nuevo deja el botón libre; el de Amelia (`edb2002`) lo deja bloqueado |
| CA-04 | T-01, T-02, T-04 | Tabla de 6 combinaciones en verde; 3 mutaciones la hacen fallar |
| CA-05 | T-01, T-04 | Tabla de 3 valores en verde; 2 mutaciones la hacen fallar |
| CA-06 | T-01, T-02, T-04, T-08 | Sin filas con `enviando`; `guardado` con la forma real (`null` u objeto); `code-reviewer` revisó la tabla |
| CA-07 | T-03, T-08 | Comentario junto a la liberación y sangría hechos, `pnpm lint` y `git diff --check` en verde; `code-reviewer` conforme |
| CA-08 | T-06 | Rama: 232 → 232 pruebas, lint y build en verde. Fusión de prueba con `origin/main` y el PR #54: 275 pruebas, lint y build en verde |
| CA-09 | T-09 | Texto de la descripción del PR entregado a Andrés (pasos manuales) |

## Registro de cambios

- 2026-11-20: spec y plan aprobados por Andrés; se crea este desglose.
- 2026-11-20: implementación T-01 a T-07. Medido: pruebas 232 → 232 en la rama (se reemplazaron 2 por 2) y 275 en la fusión de prueba con `main`; lint y build en verde. Mutaciones: 5 sobre el helper (todas hacen fallar la prueba) y 1 con `finally` sobre `enviar` (hace fallar el arnés). Cambio frente al plan: la viñeta de `08-pendientes.md` va a mitad de la lista y no al final, porque `main` agregó viñetas al final y chocaban; `CrearReporte.jsx` ya estaba anotado. El arnés de `react-test-renderer` con `fetch` falso se usó solo en un worktree temporal y no se sube.
- 2026-11-20: revisión de `code-reviewer` (T-08). Sin bugs en el código. Corregido: la tabla de pruebas pasaba `guardado` como booleano y un helper endurecido a `=== true` seguía en verde (ahora usa `null` y un objeto; esa mutación y otras 4 hacen fallar la prueba); el comentario pasó junto a la liberación y dice el motivo real; la viñeta de pendientes se dividió en 3, con «en celular» y «lo decide Andrés»; CA-02 de la spec se redactó por caso (reporte parecido, límite y foto no muestran mensaje de error); el plan ya no atribuye la ausencia de doble envío al agrupamiento de React sino a que el `catch` es síncrono y `ocupado` es un `ref`; T-05 y T-11 reflejan el método real y el nombre de la carpeta histórica. Dejado sin cambio: el `catch` protege un caso que hoy no puede ocurrir (`decidirFalloEnvio` no lanza), por decisión del plan.
- 2026-11-22: T-11. Los 3 commits llegaron a `main` dentro del PR #54 (integrado el 20/11/2026). La prueba manual en la vista previa (T-10) no quedó registrada y **no se marca como hecha**; Amelia amplió después el envío (estados de carga y modo demostración) en el PR #53. Se archiva el expediente.
