# Decisiones de arquitectura (ADR)

Un ADR (Architecture Decision Record) registra una decisión importante: su contexto,
la decisión tomada y sus consecuencias. Sirve para no rediscutir lo ya decidido y para
que cualquiera (o Claude Code) entienda por qué el proyecto es como es.

Formato de cada archivo:

- **Estado**: Aceptada · Abierta · Reemplazada.
- **Contexto**: qué problema había.
- **Decisión**: qué se decidió.
- **Consecuencias**: qué implica (bueno y malo).

Las decisiones abiertas se marcan como tales y no deben cerrarse sin el equipo.

| ADR | Título | Estado |
| --- | --- | --- |
| 0001 | Comunas, no cuadrantes | Aceptada |
| 0002 | Mecanismo de envío de correo | Aceptada (Resend, 29/10/2026) |
| 0003 | Fuente cartográfica de las comunas | Aceptada (provisional) |
| 0004 | Hosting en capas gratuitas | Aceptada (desplegada 28/10/2026, Netlify en vez de Vercel) |
| 0005 | Estructura del frontend: `app/` / `admin/` / `compartido/` | Aceptada |
| 0006 | Almacenamiento de fotos: Cloudinary | Aceptada (29/10/2026) |
| 0007 | Dominio propio para el correo de Resend | Aceptada (31/10/2026, `notiurba.com`) |
| 0008 | Privacidad, autorización única y verificación de correo | Aceptada (01/11/2026; texto legal por validar) |
