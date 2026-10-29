# NotiUrba — guía para trabajar con Claude Code

NotiUrba es una PWA de reporte ciudadano georreferenciado para la zona urbana de
Tuluá (Valle del Cauca). El ciudadano crea un reporte con foto y GPS; el sistema
lo clasifica en una de las **10 comunas urbanas** y lo hace llegar por correo a la
entidad competente de la Alcaldía o a la empresa de servicios públicos. Proyecto
académico de la materia Gestión de Proyectos (29 sep – 13 dic 2026).

## Cómo usar este repositorio con Claude Code

1. Lee siempre primero `docs/README.md` y el contexto en `docs/contexto/`.
2. Las decisiones cerradas están en `docs/decisiones/` (ADR). No las revivas.
3. Lo que está **sin decidir** está marcado como *pendiente* o *propuesto*. No lo
   cierres por tu cuenta: pregunta o déjalo señalado.
4. La documentación de planeación vive en un Google Doc (fuente formal para la
   universidad). `docs/` es la copia de trabajo para el código. Si cambia algo de
   fondo, se actualiza en ambos.

## Reglas de trabajo

- No inventes endpoints, nombres de tablas ni reglas: revisa `docs/contexto/`.
- Respeta el stack fijado (abajo). No introduzcas otro framework ni otra base de
  datos sin una decisión explícita.
- Español en toda la interfaz. Código y nombres técnicos en inglés está bien.
- La asignación de comuna (punto-en-polígono) es la función central: la fuente de
  verdad es PostGIS en el backend, no el frontend.
- No subas secretos (claves de Resend, Cloudinary, base de datos): van en
  variables de entorno.

## Stack fijado

- **Backend:** Java 21 + Spring Boot 3.3 (web, data-jpa, security, mail, validation).
- **Geo:** PostgreSQL 16 + PostGIS 3.4 (índice GiST), Hibernate Spatial.
- **Seguridad:** Spring Security + JWT (jjwt).
- **Frontend:** React + Vite + vite-plugin-pwa. Gestor de paquetes: **pnpm** (no usar npm; no subir `package-lock.json`).
- **Mapas:** Leaflet 1.9 + OpenStreetMap + Leaflet.heat.
- **Fotos:** Cloudinary (plan Free) — propuesta.
- **Hosting:** Netlify (frontend) + Render (backend) + Neon (PostgreSQL/PostGIS).
  - Netlify despliega desde la rama `develop` con base `frontend` y publish `dist`.
    La redirección SPA vive en `frontend/public/_redirects` (`/*  /index.html  200`);
    sin ella, las rutas directas (p. ej. `/app/nueva-contrasena`) dan 404 al recargar.
  - Variables de entorno, **sin valores en el repo**:
    - Netlify: `VITE_API_URL`.
    - Render: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_DEV_PASSWORD`,
      `GOOGLE_CLIENT_ID`, `CORS_ORIGINS`, `RECUPERACION_ENLACE`.
- **Correo:** JavaMailSender + Resend. Mecanismo de envío **pendiente** (ver ADR-0002).

## Equipo

- **Andrés Mauricio Peña López** — líder y backend/GIS.
- **Katherine López Unás** — frontend.
- **Luz Amelia Ibarguen** — UX/UI y pruebas.
- Docente y aprobador de alcance: **Jairo Rodríguez Martínez**.

## Decisiones abiertas (no cerrar sin el equipo)

- Mecanismo de envío de correo: cuenta del ciudadano (borrador) vs Resend. Decisión
  entre el final del Sprint 2 y el Sprint 3 (ADR-0002).
- Categorías 8 (riesgo de desastres) y 9 (vivienda): por validar con la Alcaldía.
- Fuente cartográfica definitiva de las comunas: por validar con Planeación (ADR-0003).
