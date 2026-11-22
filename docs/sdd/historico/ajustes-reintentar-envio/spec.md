# Ajustes al envío de reportes tras la auditoría del PR #54

> **Fase 1 de 5 del flujo SDD.** Define **qué** se corrige. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.
> Este expediente es independiente: no toca `docs/sdd/spec.md`, `plan.md` ni `tasks.md` (cierre QA).

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (Borrador → En revisión → Aprobada) |
| Historia / ticket | Sin historia de Linear. Hallazgos menores de la segunda auditoría del PR #54 (de Amelia) del 20/11/2026 |
| Responsable | Andrés; revisión de Katherine (pantallas del ciudadano) y de Amelia (autora del PR) |
| Aprobada por / fecha | Andrés, 2026-11-20 |
| Última actualización | 2026-11-20 |

## 1. Contexto y problema

El PR #54 evita que, al confirmar el envío de un reporte, aparezca un instante «Reintentar». En la segunda auditoría (pruebas, lint y build en verde; revisión con `code-reviewer`) los tres hallazgos importantes de la primera ya estaban corregidos: tras un envío exitoso el botón y la hoja siguen bloqueados hasta que carga la pantalla siguiente, el texto de progreso depende de `permiteCorreo` y el título de la hoja queda fijo. Quedan ajustes **menores** que no cambian lo que ve la persona:

1. **La liberación del candado quedó al final del `catch`** (`PasoConfirmacion.jsx`, función `enviar`). Antes un `finally` la garantizaba. Ahora, si `decidirFalloEnvio` lanzara una excepción, `ocupado.current` y `enviando` no se reiniciarían y el botón y la hoja quedarían bloqueados hasta recargar.
2. **La rama `enviando` de `textoAccionConfirmacion` no se ve nunca** (`estadoEnvio.js`). `Boton.jsx` pinta `textoCargando` en lugar de su contenido mientras `cargando` es verdadero, y el título de la hoja sale de `hoja`, fijado al abrirla. Las filas de prueba con `enviando: true` dan una seguridad que no existe.
3. **Falta un comentario corto** que explique por qué no hay `finally` (quien lo vea querrá «arreglarlo» y reaparecería el parpadeo), y una llave `}` del `catch` está mal sangrada. El lint no lo detecta porque no tiene regla `indent`.
4. **La rama está 22 commits atrás de `main`.** No hay conflictos, pero los «232 aprobados» del PR se midieron sobre la base vieja.
5. **La descripción del PR no trae los pasos de la prueba manual**, y el arreglo real (no reiniciar el candado en el éxito) solo se puede comprobar a mano porque el repo no tiene jsdom ni testing-library.

## 2. Objetivo

Que el envío de reportes quede protegido contra bloqueos permanentes, con un código y unas pruebas que digan lo que de verdad hacen, y con una prueba manual repetible por cualquiera del equipo.

## 3. Usuarios y historias

- Como **ciudadano**, quiero que el botón de enviar nunca quede bloqueado para siempre después de un error, para poder reintentar sin recargar.
- Como **ciudadano**, quiero que lo que ya funciona no cambie: durante el envío se ve el progreso y al terminar paso a «Reporte enviado» sin ver «Reintentar».
- Como **integrante del equipo**, quiero pruebas y comentarios que reflejen el comportamiento real, para no «arreglar» por error la solución.

## 4. Requisitos

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RF-AR-01 | Liberar el candado de envío (`ocupado` y `enviando`) al **inicio** del manejo de un error, antes de interpretarlo | Esencial | RF-11, RNF de usabilidad |
| RF-AR-02 | Mantener sin cambios el comportamiento observable del envío exitoso y del fallido | Esencial | PR #54 |
| RF-AR-03 | Calcular el texto de la acción solo con lo que se ve en pantalla, y el texto de progreso por separado | Alta | PR #54 |
| RF-AR-04 | Dejar la razón de no usar `finally` documentada con un comentario corto, y el código bien sangrado | Alta | CLAUDE.md |
| RF-AR-05 | Dejar pasos de prueba manual escritos en la descripción del PR | Alta | Auditoría |

## 5. Criterios de aceptación

- **CA-01** — _Dado_ un envío que sale bien, _cuando_ el reporte se guardó y se está cargando «Reporte enviado», _entonces_ el botón y la hoja siguen bloqueados con el texto de progreso y no se ve «Reintentar» ni «Confirmar» activo en ningún momento.
- **CA-02** — _Dado_ un envío que falla, _entonces_ la hoja de confirmación se cierra y el botón se habilita. Si es un error común (500, foto, fuera de cobertura), se ve el mensaje del error; si es reporte parecido, límite diario o foto necesaria, se ve la hoja o el aviso correspondiente (no un mensaje de error). Si el reporte ya se había guardado, el botón dice «Reintentar».
- **CA-03** — Caso de error: _dado_ que interpretar el error lanzara una excepción, _entonces_ el botón y la hoja quedan libres igualmente (el candado se libera antes de interpretarlo).
- **CA-04** — _Dado_ un reporte aún no guardado, _entonces_ el botón dice «Guardar y enviar» si la entidad atiende por correo y «Guardar reporte» si no; _dado_ un reporte ya guardado, _entonces_ dice «Reintentar» en ambos casos.
- **CA-05** — _Dado_ el texto de progreso, _entonces_ es «Guardando y enviando…» solo si la entidad atiende por correo; en cualquier otro caso (incluida una ruta aún sin cargar) es «Guardando…».
- **CA-06** — _Dado_ el conjunto de pruebas de `estadoEnvio`, _entonces_ cubre todas las combinaciones de reporte guardado (sí o no) con entidad con correo, sin correo y sin ruta, y el texto de progreso con los tres valores; ninguna prueba afirma un estado que la pantalla no muestra.
- **CA-07** — _Dado_ el código de `enviar`, _entonces_ lleva un comentario de una línea que dice que tras el éxito no se libera el candado porque la pantalla se desmonta al navegar, y el bloque `catch` está bien sangrado.
- **CA-08** — _Dado_ el PR con `main` integrado (fusión de prueba), _entonces_ `pnpm test`, `pnpm lint` y `pnpm build` terminan en verde.
- **CA-09** — _Dado_ la descripción del PR, _entonces_ trae los pasos manuales: éxito con el archivo de la pantalla siguiente bloqueado en DevTools, error 500 antes de guardar, fallo de foto después de guardar, entidad sin correo, y «Enviar de todas maneras» con la hoja de reporte parecido.

## 6. Reglas y restricciones que aplican

- Cambio **solo de frontend**, en `frontend/src/app/reportar/`. No hay migración, ruta nueva ni cambio de contrato.
- Se respeta el stack fijado y no se agregan dependencias (no hay jsdom ni testing-library; no se agregan).
- `PasoConfirmacion` conserva su propio candado `ocupado` en lugar de `useUnaVez`: `useUnaVez` siempre libera al terminar y aquí **no** se debe liberar tras un éxito. Esto ya era así antes del PR.
- Comentarios en el código: pocos, cortos, sin citar tareas (NOT-xx).
- Interfaz en español, frases cortas.
- Toca la pantalla del ciudadano: **se señala para revisión de Katherine y de Amelia** en el PR.
- Se trabaja en una **rama aparte** a partir de la del PR #54 (`fix/reintentar-envio-20261120`); no se escribe en la rama de Amelia sin su acuerdo.

## 7. Fuera de alcance

Se anotan como pendientes en `docs/contexto/08-pendientes.md` (los decide Andrés):

- Código muerto `app/paginas/CrearReporte.jsx` (otro «Guardando…» que ninguna ruta usa).
- Un límite de errores en la app del ciudadano y un tiempo máximo si la pantalla «Reporte enviado» nunca carga.
- Un render con jsdom para probar `enviar()` de punta a punta (el stack está fijado).
- Accesibilidad del texto de progreso (región `aria-live`) y la altura fija del botón con tamaños de letra muy grandes.
- La pantalla de fondo cambia a «Su reporte está guardado» mientras se envía, detrás de la hoja.
- Los hallazgos del PR #53.

## 8. Preguntas abiertas

- [RESUELTO] ¿A dónde va el PR de estos ajustes? Andrés decidió el 2026-11-20 la opción (B): empujar directamente a la rama del #54 (`fix/reintentar-envio-20261120`), sin PR aparte. Las otras eran (A) un PR hacia esa rama y (C) un PR nuevo a `main`.
- [PENDIENTE] ¿Se trae `main` a la rama con un merge o solo se verifica con una fusión de prueba (CA-08)? Esta especificación asume **solo la fusión de prueba**, porque GitHub fusiona sin conflicto y el merge mezclaría 22 commits ajenos en el PR. — decide: Andrés.
- [PENDIENTE] ¿Los pasos de prueba manual (CA-09) los ejecuta Andrés en la vista previa de Netlify? Crean un reporte real en producción: el correo sale a la bandeja de pruebas. — decide: Andrés.

## 9. Aprobación

- [x] La especificación está completa y sin ambigüedades.
- [x] Las preguntas abiertas están resueltas o aceptadas como riesgo (destino del PR, merge de `main` y prueba manual siguen **[PENDIENTE]** y no bloquean el plan).
- [x] **Aprobada por el usuario** → se puede pasar a `plan.md`.
