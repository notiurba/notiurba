# ADR-0004 — Hosting en capas gratuitas

**Estado:** Aceptada. Desplegada el 28/10/2026, con Netlify en lugar de Vercel.

**Actualización 28/10/2026:** el frontend quedó en Netlify (https://notiurba.netlify.app), el backend en
Render (https://notiurba-api.onrender.com) y la base en Neon con PostGIS confirmado en el plan Free.
Las fotos quedaron decididas el 29/10/2026: Cloudinary (ADR-0006).

## Contexto

Presupuesto de US$0. Hay que alojar tres piezas: frontend (PWA), backend (Spring Boot
en Docker) y PostgreSQL con PostGIS. Varias opciones gratuitas con límites distintos.

## Decisión

- **Frontend:** Netlify (originalmente Vercel Hobby; cambiado al desplegar).
- **Backend:** Render Web Service (Docker).
- **Base de datos:** Neon (PostgreSQL con PostGIS).
- **Fotos:** Cloudinary Free (respaldo: Cloudflare R2).
- Desarrollo local: Docker con `postgis/postgis`.

## Consecuencias

- Render Free se apaga tras 15 min sin tráfico (~1 min en despertar): limitar la memoria
  de la JVM y «calentar» el servicio antes de cada demostración.
- Render Free bloquea puertos SMTP 25/465/587 → afecta el correo (ver ADR-0002).
- **Neon admite PostGIS** en el plan Free (verificado el 28/10/2026); Supabase ya no hace falta.
- Las fotos van como enlace (no adjuntas); el backend guarda solo URL + id.
- Los límites gratuitos cambian: revisarlos antes del despliegue.
