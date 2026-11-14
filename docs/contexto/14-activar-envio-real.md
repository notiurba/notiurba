# Activar el envío real a las entidades (y volver a pruebas)

Guía para pasar de «todo sale a la bandeja de pruebas» a «el reporte llega a la entidad». Sin valores
secretos: solo nombres de variables. La usa también NOT-133 (despliegue).

## Cómo funciona hoy

- El correo de cada reporte sale desde `reportes.notiurba.com` con Resend.
- Si `RESEND_PRUEBA_DESTINO` tiene valor, el correo va solo a esa dirección, con «[PRUEBA]» en el asunto y una
  línea que dice a quién habría llegado. Si está vacía o no existe, va al destino de la ruta.
- Solo salen por correo las categorías con correo confirmado: CAIC (categorías 1 a 10) y Centroaguas
  (`info@`, categoría 11). Gas, energía y aseo muestran sus canales alternos. Las emergencias nunca salen por correo.
- Si el correo rebota, Resend avisa a `POST /api/webhooks/resend` y el detalle del reporte muestra «El correo no llegó»
  con los canales de la entidad y «Copiar mi mensaje».

## Variables en Render (nombres, sin valores)

| Variable | Para qué | Estado esperado |
| --- | --- | --- |
| `NOTIURBA_ENVIO_REPORTES_HABILITADO` | Enciende la cola de envío | `true` |
| `RESEND_API_KEY` | Clave de Resend | puesta |
| `RESEND_REMITENTE` | Remitente (`reportes.notiurba.com`) | puesta |
| `RESEND_WEBHOOK_SECRET` | Firma de los avisos de rebote (empieza por `whsec_`) | puesta |
| `RESEND_PRUEBA_DESTINO` | Desvía todo a la bandeja de pruebas | **puesta en pruebas; se quita para el envío real** |

`MAIL_FROM` y `MAIL_TEST_TO` ya no existen: el código nunca las leyó.

## Registrar el webhook en Resend (una sola vez)

1. Resend → Webhooks → Add Webhook.
2. Dirección: `https://notiurba-api.onrender.com/api/webhooks/resend`.
3. Eventos: `email.bounced` y `email.complained`.
4. Copiar el Signing Secret (`whsec_…`) a `RESEND_WEBHOOK_SECRET` en Render.
5. Si la firma no coincide, el servicio responde 401 `FIRMA_INVALIDA` y no cambia nada. Si Render estaba dormido, Resend
   reintenta el aviso.

## Activar el envío real

Antes de empezar, marcar cada punto (si falta uno, no se activa):

- [ ] Andrés validó el envío real (13 nov 2026: canales oficiales y datos que la persona autoriza al enviar).
- [ ] El docente está informado de la fecha de activación.
- [ ] El CAIC (y Centroaguas, si se usa la categoría 11) fueron avisados y su buzón recibe correos de `reportes.notiurba.com`.
- [ ] El webhook está registrado y el paso 2 de abajo salió bien.

1. Con `RESEND_PRUEBA_DESTINO` todavía puesta, crear un reporte de prueba en la categoría 1 y comprobar que llega a la
   bandeja de pruebas con «[PRUEBA]».
2. Probar el rebote con un aviso real: poner **temporalmente** `RESEND_PRUEBA_DESTINO=bounced@resend.dev` (dirección de
   pruebas de Resend que siempre rebota; confirmarla en la documentación de Resend antes de usarla), crear un reporte en
   la categoría 1 y esperar unos minutos. El detalle del reporte debe mostrar «El correo no llegó» con los canales. No sirve
   el «evento de prueba» del panel de Resend: trae un identificador de envío inventado y no coincide con ningún reporte.
   Al terminar, volver a poner la bandeja de pruebas en `RESEND_PRUEBA_DESTINO`.
3. **Quitar** `RESEND_PRUEBA_DESTINO` en Render (borrar la variable; vacía también funciona). Render reinicia el servicio.
4. Crear **un** reporte controlado en una categoría con correo confirmado y avisar antes a la entidad. Comprobar en
   Resend → Emails que salió al destino de la ruta, sin «[PRUEBA]».
5. Abrir el detalle del reporte: el correo debe verse como enviado, no como fallido.

## Volver a pruebas

1. En Render, volver a poner `RESEND_PRUEBA_DESTINO` con la bandeja de pruebas.
2. Esperar el reinicio y crear un reporte: debe llegar con «[PRUEBA]».
3. Los reportes que ya salieron no se pueden retirar de la entidad.

## Lo que esta guía no cubre

- Nuevas rutas de correo (gas, energía, aseo): no tienen correo confirmado.
- Aviso por correo al ciudadano cuando un correo rebota: queda para después del MVP.
