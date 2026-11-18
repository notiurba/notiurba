# Tareas — NOT-137 Accesibilidad ciudadana

> **Fase 3 de 5.** Una tarea a la vez, en orden, con prueba primero. Un solo PR (`feature/not-137-accesibilidad-ciudadana` → `develop`), commits atómicos por tarea y solo cuando Andrés lo pida.
> Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha.

| ID | Tarea | Criterio | Comprobación | Estado |
| --- | --- | --- | --- | --- |
| T-01 | Prueba roja de `PasosBarra`; luego `h2` enfocable y enfoque al pasar al paso 2 en `Registro` | CA-01 | `pnpm test` en verde; a mano: «Continuar» lleva el foco al encabezado | [x] |
| T-02 | Prueba roja de `ResumenErrores` y de `aria-*` en `Campo`/`CampoPassword`; luego el componente, su uso en `Login` y el ajuste de `CampoPassword` si falta | CA-02 | Prueba en verde; ingreso con ambos campos vacíos muestra el resumen | [x] |
| T-03 | Prueba roja de `CamposPerfil`; luego el texto «CÓDIGO — Nombre» | CA-03 | Prueba en verde; valor del selector sin cambio | [x] |
| T-04 | Prueba roja de tamaños; luego `BotonIcono.css` y `Filtro.css` a 44 px | CA-04 | Prueba en verde; revisión visual de las pantallas que los usan | [x] |
| T-05 | Prueba roja del relleno inferior; luego `FormularioPantalla.css` | CA-05 | Prueba en verde; a 360 px, «Confirmar contraseña» queda visible | [x] |
| T-06 | `pnpm lint`, `pnpm test`, `pnpm build` | todos | Todo en verde | [ ] |
| T-07 | Subagente `code-reviewer` sobre la rama; corregir hallazgos con pruebas y reportar antes/después | todos | Informe y correcciones | [ ] |
| T-08 | Entregar a Luz la lista de comprobación con TalkBack real (CA-02, CA-03, CA-05) y a Katherine la señal de pantallas tocadas | CA-02, 03, 05 | Confirmación de Luz registrada aquí | [ ] |
| T-09 | Cierre de historia (skill `cierre-de-historia`) | — | Docs al día; archivo en `historico/` | [ ] |

## Trazabilidad

| CA | Tarea | Prueba automática | Confirmación manual |
| --- | --- | --- | --- |
| CA-01 | T-01 | `PasosBarra.test.mjs` | Katherine / Amelia |
| CA-02 | T-02 | `ResumenErrores.test.mjs` | **Luz, TalkBack** |
| CA-03 | T-03 | `CamposPerfil.test.mjs` | **Luz, TalkBack** |
| CA-04 | T-04 | `tamanosTactiles.test.mjs` | Revisión visual |
| CA-05 | T-05 | `tamanosTactiles.test.mjs` | **Luz, TalkBack** |
