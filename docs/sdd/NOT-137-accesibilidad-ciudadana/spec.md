# NOT-137 — Accesibilidad ciudadana: cinco correcciones

> **Fase 1 de 5 del flujo SDD.** Define **qué** se corrige. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.
> Este expediente es independiente: no toca `docs/sdd/spec.md`, `plan.md` ni `tasks.md` (cierre QA).

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (decisiones de §5 cerradas) |
| Historia / ticket | NOT-137 (rama `feature/not-137-accesibilidad-ciudadana`) |
| Responsable | Katherine (frontend); revisión de Amelia (UX y pruebas) |
| Aprobada por / fecha | Andrés, 2026-11-17 |
| Última actualización | 2026-11-17 |

## 1. Contexto y objetivo

Hallazgos de accesibilidad de la validación con lector de pantalla (TalkBack) y de tamaños táctiles.
Se usa la lista del encargo, porque `docs/validacion/NOT-139-accesibilidad/` no existe en esta rama.
Objetivo: corregir solo estos cinco puntos, con el cambio mínimo en cada uno y una prueba que falle sin la corrección.

## 2. Criterios de aceptación

- **CA-01 (A11Y-07, Registro)** — _Dado_ el paso 1 con datos válidos, _cuando_ la persona pulsa «Continuar», _entonces_ el foco pasa al encabezado del paso 2 y no se queda en el botón.
- **CA-02 (TB-02, Login)** — _Dado_ el ingreso con correo y contraseña vacíos, _cuando_ se envía, _entonces_ el error se anuncia solo, con un resumen con `role="alert"` que lista los errores, y cada campo con error lleva `aria-invalid` y `aria-describedby` hacia su mensaje. La persona no tiene que explorar la pantalla. La conformidad final la confirma Luz con TalkBack real.
- **CA-03 (TB-03, tipo de documento)** — _Dado_ el selector de documento, _entonces_ muestra y anuncia «CC — Cédula de ciudadanía», «CE — Cédula de extranjería» y «PAS — Pasaporte», con el mismo `id` y el mismo valor enviado de antes. La conformidad final la confirma Luz con TalkBack real.
- **CA-04 (A11Y-01/02)** — _Dado_ cualquier botón de icono o filtro, _entonces_ su área táctil mide al menos 44×44 px (hoy 40×40 y 42 px de alto).
- **CA-05 (TB-01, registro)** — _Dado_ el registro en pantalla de celular, _cuando_ se llega al último campo (en el paso 2, «Confirmar contraseña»), _entonces_ la barra fija inferior no lo tapa y se puede ver y tocar. La conformidad final la confirma Luz con TalkBack real.

## 3. Reglas

- Interfaz en español; sin tocar el contrato de la API ni los valores que se envían.
- Los cambios en `compartido/` tocan pantallas de Katherine: se señalan para su revisión.
- Pocos comentarios en el código y sin citar NOT-xx.

## 4. Fuera de alcance

- Cualquier otro hallazgo de accesibilidad que no esté en la lista.
- Rediseño de la barra de acciones o de los componentes; solo el ajuste mínimo.
- Cambios de backend, datos o catálogos.

## 5. Decisiones tomadas

- CA-01: el foco va al encabezado del paso (Andrés, 2026-11-17).
- CA-02: resumen `role="alert"` más `aria-invalid` y `aria-describedby` por campo (Andrés, 2026-11-17).

## 6. Aprobación

- [x] **Aprobada por el usuario** → se puede pasar a `plan.md`.
