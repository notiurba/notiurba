# ADR-0007 — Dominio propio para el correo de Resend

**Estado:** Aceptada (31/10/2026). Dominio `notiurba.com` comprado; subdominio de envío `reportes.notiurba.com` verificado en Resend.

## Estado (31/10/2026)

- El dominio `notiurba.com` ya está comprado. Lo compró Andrés Mauricio Peña López, **a su nombre**: él es el dueño y quien paga la renovación.
- El subdominio de envío es `reportes.notiurba.com` y está verificado en Resend (dato dado por Andrés).
- Probado el 01/11/2026 desde la máquina de Andrés: la API HTTP de Resend respondió 200 con este subdominio (envío a una dirección de pruebas de Resend).
- Pendiente: probar el envío desde Render. El `MailService` aún no está implementado.
- El plazo del 8 de noviembre se cumplió en lo que toca a la compra y la verificación.

Lo que sigue (Contexto, Propuesta y Pasos) es el análisis original del 30/10/2026 y se deja como historia.

## Contexto

Resend (ADR-0002) exige un **dominio verificado**; sin él solo envía a la cuenta dueña y el
envío a las Secretarías no funciona. «Dominio» es una dirección web propia (por ejemplo
`notiurba.com`), no un correo: se verifica agregando registros DNS (SPF y DKIM, más MX o
CNAME) en el proveedor del dominio. No sirven `gmail.com`, un dominio de la Universidad ni
`notiurba.netlify.app`, porque no controlamos su DNS.

## Propuesta

- Comprar un `.com`. Precios publicados por Porkbun el 29/10/2026 (primer año / renovación, US$):
  Cloudflare 10,46 / 10,46 · Porkbun 11,08 / 11,08 · Namecheap 11,48 / 18,68 · GoDaddy 13,19 / 23,19.
  Fijarse en la renovación. No se confirmó si incluyen WHOIS privacy.
- Lo compra Andrés a su nombre. Decir por escrito quién es el dueño: el dominio **no** es del
  proyecto por defecto, y quien lo compra paga la renovación.
- Enviar desde un **subdominio** (por ejemplo `reportes.<dominio>.com`), como recomienda Resend,
  para aislar la reputación del correo de NotiUrba de cualquier uso personal del dominio raíz.
  Cada subdominio se agrega por separado; el plan gratuito permite hasta 3 dominios.
- Alternativas: pedir un subdominio a la Universidad o a la Alcaldía, o usar un dominio que
  ya tenga el equipo. Evitar dominios gratuitos tipo `.tk` (los filtros de spam los penalizan).

## Pasos de configuración (Resend)

1. Crear cuenta en Resend y entrar a Domains → Add Domain.
2. Escribir el subdominio y elegir la región más cercana a los destinatarios.
3. Copiar los registros DKIM y SPF al DNS exactamente como los muestra Resend. Si un registro
   es CNAME, desactivar el proxy.
4. Esperar la verificación (suele tardar ~15 minutos; la propagación puede llegar a 72 horas).
5. Agregar un registro DMARC después de verificar.
6. Crear la clave API; en Render: `RESEND_API_KEY` y el remitente. Nunca en el repo.
7. Probar con un envío a una dirección propia antes de apuntar a las Secretarías.

## Consecuencias

- Mientras no haya dominio verificado, el envío real a las Secretarías está bloqueado.
- Riesgos: si el dominio queda a nombre de una persona, esa persona responde por él; si se
  usa la raíz para correo personal mal configurado, se afecta el envío de NotiUrba.
- Cuando se compre, pasar este ADR a «Aceptada» con el dominio elegido y el dueño.
