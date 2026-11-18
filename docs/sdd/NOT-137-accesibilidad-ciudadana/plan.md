# Plan técnico — NOT-137 Accesibilidad ciudadana

> **Fase 2 de 5.** Responde **cómo** se cumple la [spec](spec.md) (aprobada el 2026-11-17).
> No se escribe código hasta que este plan esté **Aprobado**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado** |
| Aprobado por / fecha | Andrés, 2026-11-17 |

## 1. Alcance técnico

Solo frontend (`frontend/src/compartido/`). Sin backend, migraciones, API ni dependencias nuevas.
Pantallas de Katherine: se señalan en el PR.

## 2. Cambios por criterio

| CA | Archivos | Cambio mínimo |
| --- | --- | --- |
| CA-01 | `componentes/PasosBarra.jsx`, `paginas/Registro.jsx` | La etiqueta del paso pasa a ser un `<h2 tabIndex={-1}>` que recibe un `ref`. `Registro` guarda el `ref` y, en un `useEffect` sobre `paso`, enfoca el encabezado solo cuando el paso cambia a 2 (no en el primer render ni al volver al 1, que ya tenía su comportamiento). |
| CA-02 | `componentes/formulario/ResumenErrores.jsx` (nuevo, pequeño), `componentes/formulario/CampoBase.jsx` (quita `role="alert"` del mensaje), `paginas/Login.jsx` | `ResumenErrores` dibuja un contenedor `role="alert"` con la lista de mensajes (`Revise: …`) o nada si no hay errores. `Login` lo muestra sobre los campos. `Campo` y `Selector` ya ponen `aria-invalid` y `aria-describedby` con `ariaCampo`; se verifica que `CampoPassword` también, y se corrige si falta. El enfoque al primer error (`enfocarPrimerError`) se conserva. |
| CA-03 | `componentes/formulario/CamposPerfil.jsx:55` | `texto: \`${t.codigo} — ${t.nombre}\``. `valor` e `id` no cambian. |
| CA-04 | `componentes/BotonIcono.css`, `componentes/Filtro.css` | `.boton-icono`: `width` y `height` a 44px. `.filtro`: `height` a `min-height: 44px`. Se revisan los lugares donde se usan por si el cambio rompe una fila. |
| CA-05 | `componentes/formulario/FormularioPantalla.css` | La barra es `sticky` y va en el flujo: no tapa nada al final del scroll, sino al enfocar un campo que queda bajo ella. El margen de desplazamiento (`scroll-margin-bottom`) era de 140 px y la barra del paso 2 mide unos 175 px (3 botones de 48 px) más el área segura. Se sube a 200 px y se aplica también a `.campo` (etiqueta y error), no solo al control. Ajustado frente al plan original, que proponía relleno inferior. |

## 3. Pruebas (`node --test`, sin dependencias nuevas)

El proyecto no tiene DOM en las pruebas: se usa `vite ssrLoadModule` y `renderToStaticMarkup`, como en `ListaCanales.test.mjs`. Cada prueba debe fallar sin su corrección.

| CA | Prueba | Falla hoy porque… |
| --- | --- | --- |
| CA-01 | `PasosBarra.test.mjs`: el marcado trae un `h2` con `tabindex="-1"` y el texto de la etiqueta. | Hoy es un `span` no enfocable. |
| CA-02 | `ResumenErrores.test.mjs`: con errores sale `role="alert"` y cada mensaje; sin errores no sale nada. `Campo` y `CampoPassword` con `error` traen `aria-invalid="true"` y `aria-describedby="<id>-error"`, y el mensaje `<id>-error` no lleva `role`. | El componente no existe. |
| CA-03 | `CamposPerfil.test.mjs`: el marcado trae «CC — Cédula de ciudadanía», «CE — Cédula de extranjería», «PAS — Pasaporte», con `id="idTipoDocumento"` y `value` intactos. | Hoy solo hay «CC», «CE», «PAS». |
| CA-04 | `tamanosTactiles.test.mjs`: lee los dos CSS y comprueba 44 px o más. | Hoy 40 y 42. |
| CA-05 | Misma prueba: `scroll-margin-bottom` de 200 px o más en los controles y en `.campo`. | Hoy es 140 px y `.campo` no lo tiene. |

Límites, dichos claros: estas pruebas no ejecutan el foco real (CA-01) ni el diseño real (CA-04, CA-05). Eso se comprueba a mano a 360 px y con TalkBack. **Luz confirma la conformidad final de CA-02, CA-03 y CA-05 con TalkBack real**; hasta entonces quedan «implementado, pendiente de confirmación».

## 4. Riesgos

- Un `h2` nuevo cambia el orden de encabezados: se revisa el contraste y el estilo `.pasos__etiqueta` para que se vea igual.
- Botones de 44 px pueden desalinear filas: revisión visual en las pantallas que los usan.
- Doble anuncio, **resuelto (Andrés, 2026-11-17)**: solo el resumen lleva `role="alert"`. El mensaje de cada campo pierde el `role` solo dentro de un `ResumenErrores` (contexto leído por `CampoBase.jsx`) y se enlaza con `aria-invalid` y `aria-describedby`. Hoy solo `Login` lleva el resumen; las demás pantallas con campos conservan el `role` en el mensaje (la prueba `ResumenErrores.test.mjs` las clasifica a todas). Extender el resumen a otras pantallas queda fuera de alcance. Luz lo confirma con TalkBack.

## 5. Reversión

Cambios pequeños y separados por commit: se revierte cualquiera sin afectar a los demás.

## 6. Aprobación

- [x] **Plan aprobado por el usuario** → tareas aprobadas; se implementa.
