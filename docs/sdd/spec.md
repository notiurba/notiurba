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
- **CA-08** — _Dado_ un fallo transitorio del proveedor, _entonces_ la cola reintenta; si el fallo no es reintentable, el
  envío queda fallido con el motivo amable.
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
