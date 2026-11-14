# Plan técnico — Envío real a las entidades y aviso de rebotes

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se construye lo que dice `spec.md`.
> Solo se redacta con la especificación **Aprobada**. No se escribe código hasta que
> este plan esté **Aprobado**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado** (Borrador → En revisión → Aprobado) |
| Especificación | [spec.md](spec.md) |
| Aprobado por / fecha | Andrés, 13 nov 2026 |
| Última actualización | 2026-11-13 |

## 1. Enfoque general

Casi todo el backend ya existe: la cola, el destino de pruebas, el webhook de rebote y los endpoints
`GET /api/reportes/{codigo}/envio` (estado, rebote, canales) y `GET /api/reportes/{codigo}/mensaje`. Lo que falta es
**mostrar** el resultado del correo en el detalle del reporte, **cubrir con pruebas** lo que hoy solo se prueba por partes y
**escribir la guía** de activación.

Decisión central: el detalle del reporte llama al endpoint `/envio` que ya existe, en vez de ampliar
`GET /api/reportes/{codigo}`. Así no hay migración, no hay endpoint nuevo y se reutilizan los componentes ya hechos
(`presentacionCorreo`, `OtrosCanales`, `ListaCanales`). `/envio` ya valida que el reporte sea de quien pregunta (404 si no).

## 2. Stack y límites (fijados, no se cambian)

Java 21 + Spring Boot 3.3 · PostgreSQL 16 + PostGIS 3.4 · JWT · React + Vite + PWA
(pnpm) · Leaflet · Cloudinary (`AlmacenFotos`) · Resend (`EnviadorCorreo`) ·
Netlify + Render + Neon. Esta historia no sale de esa lista.

## 3. Cambios por capa

### Base de datos
- **Sin migración.** `registro_envio_correo` ya tiene `rebote`, `reintentable`, `id_envio_proveedor`; `evento_correo` ya
  existe (V16). La siguiente libre sigue siendo V23.

### Backend (`backend/src/main/java/com/notiurba/correo/`)
- **Sin cambios de código previstos.** `EnvioReporteServicio`, `ReboteController` y `FirmaWebhook` ya cumplen los criterios;
  se confirma con las pruebas de la sección 4.
- Si una prueba nueva destapa un defecto, se corrige con su prueba y se avisa antes de seguir.

### API (contrato en `docs/contexto/11-contrato-api.md`)
| Método y ruta | Rol | Entrada | Salida | Errores |
| --- | --- | --- | --- | --- |
| `GET /api/reportes/{codigo}/envio` | Autor del reporte | — | `estado`, `motivo`, `reintentable`, `rebote`, `ruta` (con canales) | 400 código inválido, 401, 404 |
| `GET /api/reportes/{codigo}/mensaje` | Autor del reporte | — | `asunto`, `texto` | 400, 401, 404 |
| `POST /api/webhooks/resend` | Público, firmado (svix) | evento de Resend | 200 | 400 evento inválido, 401 `FIRMA_INVALIDA` |

Los tres existen. No se agrega ningún endpoint; solo se documenta en el contrato que el detalle usa `/envio`.

### Frontend (`frontend/src/…`)
- Zona: `app/` (ciudadano). Archivo principal: `app/paginas/DetalleReporte.jsx`.
- El detalle carga `estadoCorreo(codigo)` (de `app/servicios/correoReportes.js`) junto con el reporte. Si falla esa carga,
  el detalle se ve igual, sin el bloque del correo (el correo es información extra, no puede tumbar el seguimiento).
- Bloque nuevo «Correo a la entidad» con la etiqueta y el subtítulo de `presentacionCorreo(envio)`:
  - en camino / enviado: texto claro, sin aviso de fallo (CA-07);
  - rebote o fallido: «El correo no llegó» + `OtrosCanales` (canales de la entidad y «Copiar mi mensaje») cuando
    `ofrecerOtrosCanales(envio)` sea verdadero (CA-06).
- Textos en `compartido/textos/` o en `estadoCorreo.js`, sin palabras técnicas (RN-24). Etiqueta del rebote: «No entregado».
- «Copiar mi mensaje» no consulta ni envía datos nuevos al guardar, así que no necesita `useUnaVez`; el botón ya existe y
  muestra «Copiando…» mientras trae el mensaje.
- Las pantallas de Katherine no cambian salvo `DetalleReporte.jsx`; Andrés se lo cuenta personalmente.

### Integraciones y configuración
- Variable que se **usa** (sin valores en el repo): `RESEND_WEBHOOK_SECRET`, que ya lee `application.yml`. La pone Andrés en
  Render y registra el webhook en Resend (`POST /api/webhooks/resend`, eventos `email.bounced` y `email.complained`).
- `RESEND_PRUEBA_DESTINO` sigue puesta hasta activar el envío real.
- **Guía de activación** en `docs/` (archivo nuevo `docs/contexto/14-activar-envio-real.md`): variables por nombre, orden de los
  pasos, prueba con un reporte controlado, cómo volver a pruebas y cómo confirmar el webhook. La reutiliza NOT-133.
- Se actualiza la documentación de variables de Render: `MAIL_FROM` y `MAIL_TEST_TO` se borran de Render (decisión de Andrés).

## 4. Estrategia de pruebas

| Criterio (spec) | Tipo de prueba | Dónde |
| --- | --- | --- |
| CA-01 | Unitaria, ya existe (`conDestinoDePruebaSaleAEsaDireccion…`); se agrega integración: cola con destino de prueba | `backend/src/test/…/correo/` |
| CA-02 | Integración nueva: cola sin destino de prueba envía al destino de la ruta, sin «[PRUEBA]», y el envío pasa a «Enviado» | `backend/src/test/…/correo/` |
| CA-03, CA-05 | Ya existe (`reboteFirmadoNoRetrocedeHistorialYEsIdempotente`); se verifica que cubra `email.complained` | `EnrutamientoIntegracionTest` |
| CA-04 | Integración nueva por HTTP: sin firma, firma falsa y firma vencida → 401 `FIRMA_INVALIDA` y nada cambia | `backend/src/test/…/correo/` |
| CA-06, CA-07 | Prueba del componente o de las funciones de texto con `node --test`: rebote muestra canales y «Copiar mi mensaje»; enviado no | `frontend/src/app/…` |
| CA-08 | Ya existe (`falloConservaReporteYReintentoReutilizaMensajeYClave`, `limiteDeEnvios…`); se verifica el caso no reintentable | `EnrutamientoIntegracionTest`, `EnviadorCorreoResendTest` |
| CA-09 | Integración nueva: `/envio` y `/mensaje` de un reporte ajeno → 404 | `backend/src/test/…/correo/` |
| CA-10 | Revisión manual de la guía contra las variables reales de `application.yml` | `docs/contexto/14-activar-envio-real.md` |

Casos de error y entradas hostiles incluidos (firma alterada, aviso repetido, identificador vacío o de más de 100 caracteres).
Las pruebas existentes (backend 424, frontend 207) deben seguir pasando. Al final, revisión con el subagente `code-reviewer`.

## 5. Riesgos y mitigaciones

| Riesgo | Probabilidad / impacto | Mitigación |
| --- | --- | --- |
| Activar el envío real sin querer manda un reporte de prueba a una entidad | Baja / alto | Guía con orden fijo; probar primero con un reporte controlado y `RESEND_PRUEBA_DESTINO` puesta; sin cambios de código que lo adelanten |
| Con destino de pruebas, el rebote es de la bandeja de pruebas y no de la entidad | Media / bajo | Dejarlo dicho en la guía; la prueba real del webhook usa una dirección que rebota a propósito |
| Resend cambia los nombres de los eventos | Baja / medio | La guía manda confirmarlos en el panel de Resend; la prueba de firma cubre el formato actual |
| Render dormido: el webhook de Resend falla y Resend reintenta | Media / bajo | Resend reintenta solo; el aviso llega al despertar. Se anota en la guía |
| El bloque del correo tumba el detalle si `/envio` falla | Baja / medio | El detalle se muestra aunque `/envio` falle |

## 6. Alternativas descartadas

- Ampliar `GET /api/reportes/{codigo}` con `rebote` y canales — duplica lo que ya entrega `/envio` y obliga a tocar el contrato
  y las pruebas del detalle.
- Correo al ciudadano avisando del rebote — queda fuera del MVP (spec, sección 7).
- Migración nueva — no hace falta: las columnas ya existen.

## 7. Decisiones pendientes

- Ninguna. Los textos del rebote y el estado de las variables de Render quedaron resueltos en la spec.

## 8. Aprobación

- [x] Cada criterio de `spec.md` tiene un cambio técnico y una prueba.
- [x] No introduce otro framework ni otra base de datos.
- [x] Contrato de API y migración revisados.
- [x] **Aprobado por el usuario** → se puede pasar a `tasks.md`.

## Anexo A — Enfoque de los ajustes tras la prueba (aprobado 14 nov 2026)

- **Primero entender, después arreglar.** El error del panel y el de Google se reproducen y se explican antes de cambiar
  código: el del panel falló aun con observación escrita, así que la causa no es solo el campo (hay que mirar la respuesta
  real de la API); el de Google se revisa con una consulta de **solo lectura** a las tablas de usuario e identidad externa.
- **Cada error lleva una prueba que lo reproduce** antes de la corrección (backend con la base desechable; frontend con
  `node --test`).
- **Sin migración prevista.** Si arreglar Google exige corregir datos de producción, se pide aprobación y se hace un
  respaldo en Neon antes.
- **Correo:** solo cambia el HTML (`PlantillaReporte`); el texto plano y el asunto no cambian.
- **Panel:** ajustes de CSS y de listas de filtros; sin rediseño.
- **QA:** vista previa de Netlify del PR (celular a 360 px y computador a 1366 px) y el script `tools/qa-panel-admin/qa-panel.sh`
  contra el jar real.
