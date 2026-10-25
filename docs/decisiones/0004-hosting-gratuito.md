# ADR-0004 — Hosting en capas gratuitas

**Estado:** Aceptada como propuesta; confirmar antes del 8 de noviembre.

## Contexto

Presupuesto de US$0. Hay que alojar tres piezas: frontend (PWA), backend (Spring Boot
en Docker) y PostgreSQL con PostGIS. Varias opciones gratuitas con límites distintos.

## Decisión

- **Frontend:** Vercel Hobby (uso personal/académico).
- **Backend:** Render Web Service (Docker).
- **Base de datos:** Neon (PostgreSQL con PostGIS).
- **Fotos:** Cloudinary Free (respaldo: Cloudflare R2).
- Desarrollo local: Docker con `postgis/postgis`.

## Consecuencias

- Render Free se apaga tras 15 min sin tráfico (~1 min en despertar): limitar la memoria
  de la JVM y «calentar» el servicio antes de cada demostración.
- Render Free bloquea puertos SMTP 25/465/587 → afecta el correo (ver ADR-0002).
- Verificar que **Neon admite PostGIS** en el plan Free; si no, usar Supabase (se pausa
  tras 1 semana sin actividad).
- Las fotos van como enlace (no adjuntas); el backend guarda solo URL + id.
- Los límites gratuitos cambian: revisarlos antes del despliegue.
