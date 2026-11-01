# ADR-0002 — Mecanismo de envío de correo

**Estado:** Aceptada (29/10/2026). NotiUrba envía el correo con **Resend**. Se descarta el borrador que el ciudadano envía.

## Contexto

El reporte se canaliza por correo a la entidad competente. El correo debe salir con la
identidad del ciudadano (su nombre en el remitente, su correo en Reply-To) y dejar
trazabilidad (RN-07, RNF-07). Se evaluaron dos caminos:

1. **Resend**: la API envía el correo y la Secretaría responde directo al ciudadano por Reply-To.
2. **Borrador en el cliente del ciudadano** (`mailto:`): la app solo prellena y el ciudadano envía.

Un docente de Aplicaciones móviles señaló que prellenar el correo para que el usuario lo
envíe es una complejidad innecesaria: es mejor que la app lo envíe y que el ciudadano
quede como Reply-To para recibir la respuesta.

## Decisión

**Usar Resend como servicio de correo.** El borrador (RF-13) queda descartado.

- Remitente: dominio propio verificado de NotiUrba.
- Reply-To: correo del ciudadano. La Secretaría responde y la respuesta llega a él.
- Cada envío se registra (RF-15) en `registro_envio_correo`.

## Límites del plan gratuito (verificados el 29/10/2026 en la documentación de Resend)

- 3.000 correos al mes y **100 al día** (día UTC, se reinicia a medianoche UTC).
- Cada destinatario cuenta aparte: un correo con copia al ciudadano **gasta 2**.
- 10 peticiones por segundo; hasta 3 dominios verificados.
- Los correos de recuperación de contraseña comparten la misma cuota.

## Consecuencias

- **Riesgo crítico: el dominio.** Resend exige un dominio verificado para enviar. Según
  la documentación del propio proyecto, sin dominio solo envía al dueño de la cuenta. Si
  no se consigue, el envío a las Secretarías no funciona. Decisión del dominio antes del 8 de noviembre.
- **Render Free bloquea los puertos SMTP 25, 465 y 587.** Propuesta técnica (sin probar):
  usar la API HTTP de Resend (`POST https://api.resend.com/emails`, por HTTPS), que evita
  el bloqueo. SMTP por 2465 o 2587 con JavaMailSender queda como alternativa.
- Con 100 correos diarios, el tope es ~100 reportes al día (menos si se copia al ciudadano o se envían recuperaciones).
- **Privacidad (Ley 1581):** el Reply-To expone el correo del ciudadano a la Secretaría.
  La app debe avisar y pedir consentimiento antes de enviar.
- RN-18 y RF-13 se actualizan. El diseño de notificaciones (EDT 5.5) ya puede avanzar.
- Se mantiene el envío detrás de una interfaz (como `EnviadorEnlaceRecuperacion`) para poder cambiar de proveedor.
- Variables nuevas en Render: `RESEND_API_KEY` y el remitente. No van en el repo.
