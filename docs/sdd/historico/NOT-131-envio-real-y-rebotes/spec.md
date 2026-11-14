# Especificación — Envío real a las entidades y aviso de rebotes

> **Fase 1 de 5 del flujo SDD** (Especificación → Planificación → Tareas → Implementación → Pruebas).
> Aquí se define **qué** se construye y **por qué**. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (Borrador → En revisión → Aprobada) |
| Historia / ticket | NOT-131 (Linear), 3 puntos |
| Responsable | Andrés |
| Aprobada por / fecha | Andrés, 13 nov 2026 |
| Última actualización | 2026-11-13 |

## 1. Contexto y problema

La función central de NotiUrba es «hacer llegar el reporte a la entidad correspondiente». Hoy todo correo sale a la
bandeja de pruebas (`RESEND_PRUEBA_DESTINO`) y nadie se entera si un correo rebota. Ya existe casi todo:

- La cola de envío, el enrutamiento por categoría y el destino de pruebas con «[PRUEBA]» en el asunto (NOT-129, NOT-130).
- `POST /api/webhooks/resend`, que marca el envío como fallido con rebote al recibir `email.bounced` o `email.complained`.
  Rechaza todo con `FIRMA_INVALIDA` mientras `RESEND_WEBHOOK_SECRET` no esté puesta en Render (hoy no lo está).
- Los textos «El correo no llegó» y los canales alternos, ya hechos para la pantalla de resultado del envío.

Lo que falta:

1. En el **detalle del reporte** («Mis reportes») la persona no ve si el correo se entregó o rebotó, ni tiene a mano los
   canales de la entidad ni «Copiar mi mensaje».
2. La API de detalle solo devuelve `estado` y `motivo`: no dice si fue un rebote ni da los canales.
3. No hay una guía escrita para **activar el envío real** y para **volver al destino de pruebas**.
4. Hay que **probar** que las protecciones funcionan: el destino de pruebas no deja salir nada real, la firma falsa se
   rechaza y la cola reintenta lo transitorio.

Estado de producción (captura de Render, 13 nov 2026): están `NOTIURBA_ENVIO_REPORTES_HABILITADO` (valor sin verificar),
`RESEND_API_KEY`, `RESEND_REMITENTE` y `RESEND_PRUEBA_DESTINO`. **Falta `RESEND_WEBHOOK_SECRET`.**
También hay `MAIL_FROM` y `MAIL_TEST_TO`, que el código no usa (ver sección 8).

## 2. Objetivo

Cuando el equipo y el docente autoricen, activar el envío real es cambiar variables y seguir una lista de pasos probada;
y cuando un correo rebota, la persona lo ve en el detalle de su reporte con palabras claras y puede seguir por otro canal.
Mientras no haya autorización, nada sale a una entidad real.

## 3. Usuarios y historias

- Como **ciudadano**, quiero ver en el detalle de mi reporte si el correo llegó o no a la entidad, para saber si debo
  hacer algo más.
- Como **ciudadano** con un correo rebotado, quiero ver los canales de la entidad y copiar mi mensaje, para reportar por
  otra vía sin volver a escribir todo.
- Como **administrador / líder técnico**, quiero una lista de pasos para activar el envío real y volver atrás, para hacerlo
  sin riesgo cuando haya autorización.

## 4. Requisitos

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RF-16 | Informar al ciudadano, en el detalle del reporte, el resultado del correo: en camino, enviado, no entregado (rebote) o no enviado. | Esencial | Decisión 3 (01/11/2026) |
| RF-16a | Si el correo no llegó, mostrar los canales de la entidad y «Copiar mi mensaje» en el detalle. | Esencial | `08-pendientes.md` |
| RF-30 | Recibir el aviso de rebote o queja de Resend, validar su firma y marcar el envío como fallido con rebote. | Esencial | Ya implementado (V16); falta activarlo y probarlo |
| RNF-xx | Con destino de pruebas activo, ningún correo sale a una dirección real y el asunto lleva «[PRUEBA]». | Esencial | NOT-130 |

IDs `RF-16a` y `RNF-xx` se confirman contra `docs/contexto/04` y `05` al planificar.

## 5. Criterios de aceptación

- **CA-01** — _Dado_ que `RESEND_PRUEBA_DESTINO` está puesta, _cuando_ la cola envía un reporte, _entonces_ el correo va solo
  a esa dirección, con «[PRUEBA]» en el asunto y la línea que dice a quién habría llegado.
- **CA-02** — _Dado_ que `RESEND_PRUEBA_DESTINO` está vacía y la ruta permite correo, _cuando_ la cola envía, _entonces_ el
  correo va al destino de la ruta, sin «[PRUEBA]», y el envío pasa a «Enviado».
- **CA-03** — _Dado_ un envío ya aceptado por Resend y un aviso `email.bounced` con firma válida, _cuando_ llega al webhook,
  _entonces_ el envío queda fallido con rebote y el detalle del reporte lo muestra como «El correo no llegó».
- **CA-04** — Caso de error: _dado_ un aviso con firma inválida, vencida o sin firma, _entonces_ responde 401 `FIRMA_INVALIDA`
  y no cambia nada.
- **CA-05** — _Dado_ el mismo aviso repetido, _entonces_ no se procesa dos veces.
- **CA-06** — _Dado_ un reporte con rebote o envío fallido, _cuando_ la persona abre el detalle, _entonces_ ve el texto claro,
  los canales de la entidad y el botón «Copiar mi mensaje», sin palabras técnicas (RN-24).
- **CA-07** — _Dado_ un reporte con envío en camino o enviado, _cuando_ abre el detalle, _entonces_ ve ese estado en palabras
  claras y no ve el aviso de fallo.
- **CA-08** — _Dado_ un fallo transitorio del proveedor, _entonces_ el envío queda fallido y reintentable, y el detalle ofrece
  «Reintentar envío» (el reintento lo pide la persona, no la cola); si el fallo no es reintentable, queda fallido con el
  motivo amable y los canales de la entidad.
- **CA-09** — _Dado_ que la persona abre el detalle de un reporte ajeno, _entonces_ recibe 404 (sin cambios).
- **CA-10** — _Dado_ el documento de activación, _entonces_ lista las variables (nombres, sin valores), el orden de los pasos,
  la prueba con un reporte controlado y cómo volver a pruebas.

## 6. Reglas y restricciones que aplican

- Reglas de negocio: RN-24 (textos sin palabras técnicas), B11 (el estado del reporte no retrocede por un rebote).
- Decisiones cerradas: ADR-0002 (Resend), ADR-0007 (dominio). Decisión 3 del 01/11/2026: «Enviado» = Resend aceptó.
- Datos personales (Ley 1581): el correo a la entidad ya lleva datos del ciudadano autorizados al enviar. Esta historia no
  agrega datos nuevos. La API de detalle sigue siendo solo para el autor del reporte.
- Solo salen por correo las categorías con correo confirmado: hoy CAIC (Alcaldía) y Centroaguas (`info@`). Gas, energía y aseo
  no tienen ruta de correo; las emergencias nunca salen por correo.
- Interfaz en español, tono «usted», botones de envío con `useUnaVez`.
- No se sube ningún secreto al repositorio.

## 7. Fuera de alcance

- Activar el envío real en producción. Eso exige autorización del equipo, del docente y respuesta del CAIC; se hace según la
  guía, después de esta historia.
- Correo al ciudadano avisando del rebote (la decisión 3 lo menciona; para el MVP basta verlo en la app).
- Reintento manual por parte del ciudadano tras un rebote (hoy se bloquea a propósito).
- Tabla de rebotes en el panel de administración.
- Nuevas rutas de correo para gas, energía o aseo.
- Poner `RESEND_WEBHOOK_SECRET` en Render y registrar el webhook en Resend: lo hace Andrés con la guía.

## 8. Preguntas abiertas

- Resuelto (13 nov 2026, Andrés): `NOTIURBA_ENVIO_REPORTES_HABILITADO` está en `true` en Render.
- Resuelto (13 nov 2026, Andrés): el envío real a las entidades ya está validado: usa canales oficiales establecidos y los
  datos personales son los que la persona autoriza al enviar. Queda como paso de activación (NOT-133), no como bloqueo.
- Resuelto (13 nov 2026, Andrés): se borran `MAIL_FROM` y `MAIL_TEST_TO` de Render; ningún archivo del repo las usa.
- Resuelto (13 nov 2026, Andrés): texto del rebote: título «El correo no llegó» y etiqueta «No entregado».

## 9. Aprobación

- [x] La especificación está completa y sin ambigüedades.
- [x] Las preguntas abiertas están resueltas o aceptadas como riesgo.
- [x] **Aprobada por el usuario** → se puede pasar a `plan.md`.

## Anexo A — Ajustes tras la prueba en producción (13 nov 2026)

> **Estado: Aprobado por Andrés el 14 nov 2026.** Andrés probó la app publicada (celular y panel) y encontró
> lo siguiente. Todo se resuelve dentro de NOT-131; la historia no pasa a Done en Linear hasta que el QA de `tasks.md`
> (sección «QA») salga completo. Mapa de calor del panel: es NOT-136, no entra aquí. Copia del correo al ciudadano:
> sigue sin enviarse (decidido; el correo sale solo a la entidad, con el correo de la persona en «Responder a»).

### Errores

- **CA-11** — _Dado_ un administrador en el panel, _cuando_ cambia el estado de un reporte con o sin observación,
  _entonces_ el cambio se guarda y aparece en el historial. La observación es opcional de verdad; si falta, el historial
  guarda una frase por defecto. Si algo falla, el mensaje dice por qué (no solo «Intente de nuevo»).
- **CA-12** — _Dado_ que una persona entra con Google, _entonces_ ve solo su cuenta: si el correo de Google no tiene cuenta,
  pasa a completar su perfil con sus datos de Google y nunca ve datos de otra persona. Si hay una vinculación equivocada
  en la base, se corrige con aprobación de Andrés antes de tocar datos.
- **CA-13** — _Dado_ un tipo de reporte con ubicación opcional, _cuando_ llega al paso 2, _entonces_ ve el mapa y puede marcar
  un punto si quiere, o seguir sin marcarlo.
- **CA-14** — _Dado_ que la persona avanza o retrocede de paso, _entonces_ la pantalla vuelve al inicio.
- **CA-15** — _Dado_ que la persona abre el aviso de privacidad desde Ajustes, _cuando_ pulsa atrás, _entonces_ vuelve a Ajustes.
- **CA-16** — _Dado_ un reporte sin ubicación, _cuando_ se ve su detalle (ciudadano y panel), _entonces_ el lugar dice
  «Sin ubicación» y no queda vacío.
- **CA-17** — _Dado_ el panel de reportes, _entonces_ los filtros de tipo y de comuna muestran solo las opciones que tienen
  reportes.
- **CA-22** — _Dado_ el webhook de Resend, _entonces_ se comprueba de punta a punta: el registro de entregas en Resend muestra
  200, y un correo que rebota de verdad marca el reporte como «El correo no llegó» en el detalle.

### Mejoras de pantalla y correo

- **CA-18** — Descripción del paso 3: el cuadro tiene texto de ejemplo (placeholder), el contador de caracteres va a la
  derecha, en la misma fila del título del campo, y el texto de ayuda es corto.
- **CA-19** — Mapa del paso 2: las instrucciones son breves y pensadas para el celular (sin explicar el teclado).
- **CA-20** — Correo a la entidad: hay espacio entre la descripción, la fecha del registro y la evidencia, y el HTML tiene
  presentación institucional (encabezado azul, secciones separadas). Se mantiene el contenido de la carta formal: sin código
  de seguimiento y sin la palabra NotiUrba (decisión de NOT-130). El texto plano lleva una línea en blanco entre cada fila (hechos, ubicación, fecha y evidencia), con el mismo contenido. El HTML usa tablas para verse bien también en Outlook.
- **CA-21** — Panel del administrador: más espacio entre elementos (por ejemplo, los filtros), colores con más contraste y
  sin la etiqueta «Administrador · todas las comunas». Un rediseño completo queda en `08-pendientes.md`.

### Segunda tanda de hallazgos (14 nov 2026, de la prueba en el celular)

- **CA-23** — _Dado_ que la persona abre el aviso de privacidad (desde el registro, Ajustes o el envío), _entonces_ ve el aviso
  vigente (versión 3) y nunca una propuesta con marcadores como «[RESPONSABLE PENDIENTE DE DEFINIR]». Causa: un archivo
  estático (`frontend/public/aviso-privacidad.html`) tapaba la pantalla de la app en Netlify; se elimina.
- **CA-24** — _Dado_ un tipo de reporte con ubicación opcional, _entonces_ el paso 2 dice con claridad que no es obligatorio
  marcar el lugar y que puede continuar sin hacerlo.
- **CA-25** — _Dado_ que una persona avisa «Ya se resolvió» o «Me respondieron» en «Actualizar mi reporte», _cuando_ el
  administrador abre el panel, _entonces_ ve una etiqueta «Aviso del ciudadano» en la lista y el aviso destacado en el
  detalle. El estado **no** cambia solo: la etiqueta desaparece cuando el administrador responde: cambia el estado o, con la opción
  «Mantener el estado y solo anotar», deja una observación (obligatoria en ese caso). Una nota de un administrador nunca
  cuenta como «del ciudadano».
- **CA-26** — _Dado_ el campo de dirección de correspondencia, _entonces_ el límite y el contador se ven mientras se escribe
  (ya existen en el registro: 100 caracteres). Si en otra pantalla el límite no se ve, se corrige. [Por confirmar con Andrés: en
  qué pantalla vio el campo sin límite.]

### Fuera de alcance de este anexo

- Mapa de calor del panel (NOT-136), copia del correo al ciudadano, rediseño completo del panel.

### Decisiones de Andrés al cerrar (14 nov 2026)

- La vinculación de Google por correo **se queda**: quien ya tiene cuenta y entra con Google con el mismo correo entra a esa cuenta.
- El correo a la entidad **sigue sin la marca NotiUrba ni el código** de seguimiento (decisión de NOT-130 confirmada).
- El QA de este anexo lo ejecuta **Luz Amelia**.

