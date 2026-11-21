# Plan técnico — Ajustes al envío de reportes tras la auditoría del PR #54

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se construye lo que dice `spec.md`.
> Solo se redacta con la especificación **Aprobada**. No se escribe código hasta que
> este plan esté **Aprobado**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado** (Borrador → En revisión → Aprobado) |
| Especificación | [spec.md](spec.md) |
| Aprobado por / fecha | Andrés, 2026-11-20 |
| Última actualización | 2026-11-20 |

## 1. Enfoque general

Tres cambios pequeños en `frontend/src/app/reportar/`, sobre el código del PR #54:

1. **Soltar el candado primero.** En el `catch` de `enviar`, las dos líneas que reinician `ocupado.current` y `enviando` pasan a ser las **primeras**, antes de `decidirFalloEnvio`. Así un fallo al interpretar el error ya no puede dejar el botón bloqueado. El camino de éxito no cambia: sigue sin liberar, porque la pantalla se desmonta al navegar.
2. **Helper con lo que se ve.** `textoAccionConfirmacion` deja de recibir `enviando` (esa rama no se ve: `Boton` pinta `textoCargando` mientras carga, y el título de la hoja sale de `hoja`). Queda `({ guardado, permiteCorreo })`. `textoProgresoEnvio` sigue siendo la única fuente del texto de progreso.
3. **Legibilidad.** Un comentario de una línea sobre por qué no hay `finally`, y la llave del `catch` bien sangrada.

Más la verificación de `main` (fusión de prueba) y la prueba manual escrita para el PR. La liberación en el `catch` va **sin extraer helpers nuevos** (MVP sin sobreingeniería): son dos líneas movidas.

## 2. Stack y límites (fijados, no se cambian)

React 19 + Vite 8 + pnpm, `node --test`. No se agrega ninguna dependencia: no hay jsdom ni testing-library y no se incorporan. No se toca backend, base de datos, contrato de API ni variables de entorno.

## 3. Cambios por capa

### Base de datos
- Ninguno.

### Backend
- Ninguno.

### API
- Sin cambios en el contrato.

### Frontend (`frontend/src/…`)
- `app/reportar/PasoConfirmacion.jsx`
  - En `enviar`, bloque `catch`: mover `ocupado.current = false` y `setEnviando(false)` a las dos primeras líneas, antes de `decidirFalloEnvio`. Los demás `set…` del `catch` quedan en el mismo orden. El bloque `catch` es síncrono (sin `await`) y `ocupado` es un `ref`, así que ningún clic puede entrar entre estas líneas: no hay ventana de doble envío.
  - Comentario de una línea sobre el `navigate` de éxito: tras el éxito no se libera el candado, la pantalla se desmonta al navegar.
  - Llave de cierre del `catch` con 4 espacios.
  - `const accion = textoAccionConfirmacion({ guardado, permiteCorreo: ruta?.permiteCorreo })` (sin `enviando`). `enviando` se sigue usando en `cargando={enviando}` y en las guardas; la rama de `accion` por `enviando` no es necesaria.
- `app/reportar/estadoEnvio.js`
  - `textoAccionConfirmacion({ guardado, permiteCorreo })` → `'Reintentar'` si hay `guardado`; si no, `'Guardar y enviar'` con correo y `'Guardar reporte'` en cualquier otro caso. `textoProgresoEnvio(permiteCorreo)` no cambia.
- `app/reportar/estadoEnvio.test.mjs`
  - Tabla con las 6 combinaciones de `guardado` (`false`/`true`) × `permiteCorreo` (`true`/`false`/`undefined`) y otra con `textoProgresoEnvio` (`true`/`false`/`undefined`). Se quita la prueba «centraliza…», que repetía literales, y las filas con `enviando: true`.
  - `guardado` se pasa con la forma real de la pantalla: `null` o un objeto con `codigoSeguimiento`, para que endurecer el helper a `=== true` haga fallar la prueba.
- `docs/contexto/08-pendientes.md`
  - Una viñeta en «Deuda técnica conocida» con los puntos fuera de alcance de la spec (sección 7), **después de la de la colección Postman** (a mitad de la lista). No va al final porque `main` ya agregó viñetas ahí, ni al principio porque el #53 agrega la suya arriba: en el medio no choca con ninguno (comprobado con `git merge-file`). `CrearReporte.jsx` ya estaba anotado en esa sección y no se repite.

### Integraciones y configuración
- Sin variables nuevas. Resend y Cloudinary no se ven afectados (`RESEND_PRUEBA_DESTINO` sigue activa).

## 4. Estrategia de pruebas

Es un cambio de refactor sobre una rama sin efecto visible; por eso **no hay pruebas que fallen antes** del cambio. Se comprueba cada prueba con una **mutación manual** (se rompe el código a propósito, la prueba debe fallar, se revierte).

| Criterio | Tipo de prueba | Dónde |
| --- | --- | --- |
| CA-04 | Unitaria en tabla: 6 combinaciones de `guardado` × `permiteCorreo`. Mutación: invertir `permiteCorreo` o devolver «Reintentar» sin `guardado` → debe fallar | `app/reportar/estadoEnvio.test.mjs` |
| CA-05 | Unitaria: `textoProgresoEnvio` con `true`, `false`, `undefined`. Mutación: devolver siempre «Guardando y enviando…» → debe fallar | mismo archivo |
| CA-06 | Revisión del archivo de pruebas: ninguna fila con `enviando`; las 6 combinaciones presentes | mismo archivo + `code-reviewer` |
| CA-01, CA-02 | Arnés desechable de `react-test-renderer` con `fetch` falso en un worktree temporal (no se sube) y **manual** en la vista previa de Netlify (pasos de `spec.md` CA-09) | descripción del PR |
| CA-03 | **Mutación local** (no se sube): hacer que `decidirFalloEnvio` lance una excepción con el arnés, forzar un error de envío y comprobar que el botón queda libre. Solo cubre un caso hipotético: hoy esa función no puede lanzar | notas en `tasks.md` |
| CA-07 | Revisión de código: comentario de una línea y sangría; `pnpm lint` | `code-reviewer` |
| CA-08 | `pnpm test && pnpm lint && pnpm build` en la rama y en una **fusión de prueba** con `origin/main` (worktree temporal, sin subir) | `tasks.md` |
| CA-09 | La descripción del PR trae los 5 pasos manuales | descripción del PR |

Se corre el mismo método de las pruebas actuales (`node --test`, desde `frontend/` con pnpm). Las pruebas existentes (232 en la rama de Amelia, 286 con `main` y el #53) deben seguir en verde.

## 5. Riesgos y mitigaciones

| Riesgo | Probabilidad / impacto | Mitigación |
| --- | --- | --- |
| Liberar el candado antes de cerrar la hoja deja un instante el botón libre con la hoja abierta | Baja / baja | El `catch` es síncrono y `ocupado` es un `ref`: ningún clic entra a mitad del bloque |
| El arreglo real (no liberar tras el éxito) sigue sin prueba automática | Alta / media | Pasos manuales escritos en el PR (CA-09) y revisión de `code-reviewer`; el render con jsdom queda como pendiente |
| Conflicto de `08-pendientes.md` con `main` o con el PR #53 | Media / baja | La viñeta va a mitad de la lista; se simula con `git merge-file` contra `origin/main` y contra la rama del #53 (0 conflictos) |
| La rama sigue atrasada respecto a `main` | Media / baja | Se verifica con una fusión de prueba; la decisión de traer `main` queda para quien integra |
| Toca la pantalla del ciudadano | — | Se señala para revisión de Katherine y de Amelia en el PR |

## 6. Alternativas descartadas

- **Volver al `finally`:** reaparece el «Reintentar» al terminar con éxito (era el defecto original).
- **Extraer un helper `ejecutarEnvio` con la lógica de éxito y fallo para poder probarla:** daría una prueba automática de CA-02 y CA-03, pero agrega una capa para mover dos líneas. Va contra «MVP sin sobreingeniería».
- **Cambiar `PasoConfirmacion` a `useUnaVez`:** `useUnaVez` siempre libera al terminar y aquí no se debe liberar tras un éxito.
- **Agregar jsdom o testing-library:** cambia el stack fijado.

## 7. Decisiones pendientes

- [RESUELTO] ¿A dónde va el PR? Andrés decidió el 2026-11-20 empujar directamente a la rama del #54 (`fix/reintentar-envio-20261120`); no hay PR aparte.
- [PENDIENTE] ¿Se trae `main` a la rama con un merge? El plan asume que no (solo fusión de prueba) — decide: Andrés.
- [PENDIENTE] ¿Quién ejecuta la prueba manual en la vista previa (crea un reporte real en producción)? — decide: Andrés.

## 8. Aprobación

- [x] Cada criterio de `spec.md` tiene un cambio técnico y una prueba.
- [x] No introduce otro framework ni otra base de datos.
- [x] Contrato de API y migración revisados (no hay).
- [x] **Aprobado por el usuario** → se puede pasar a `tasks.md`.
