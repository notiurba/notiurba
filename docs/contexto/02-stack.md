# Stack tecnológico

Acordado en el Acta y en la planificación (sección 3.2). Las filas de fotos y
hosting eran «por definir» y quedan como propuesta.

| Área | Tecnología |
| --- | --- |
| Backend | Java 21 + Spring Boot 3.3 (web, data-jpa, security, validation) |
| Geolocalización | PostgreSQL 16 + PostGIS 3.4 (índice GiST), Hibernate Spatial |
| Seguridad | Spring Security + JWT (jjwt) |
| Frontend | React 18 + Vite + vite-plugin-pwa |
| Mapas | Leaflet 1.9 + OpenStreetMap + Leaflet.heat |
| Correo | Resend (decidido 29/10/2026; plan gratuito 3.000/mes y 100/día). Se envía por la API HTTP de Resend (HTTPS), implementada en `EnviadorCorreoResend`; ver abajo |
| Contenedores | Docker (postgis/postgis) para desarrollo local |
| Fotos | Cloudinary, plan Free (decidido 29/10/2026, ADR-0006) |
| Hosting | **Desplegado (28/10/2026):** Netlify (frontend) + Render (backend) + Neon (PostgreSQL/PostGIS). Ver «Despliegue actual» |

## Mapas (Leaflet + OpenStreetMap)

- Leaflet 1.9.4 es la versión estable; no usar la 2.0 alpha.
- `react-leaflet` como envoltura: verificar compatibilidad con React 18 (por verificar).
- Mapa de calor con el plugin **Leaflet.heat**: `L.heatLayer([[lat, lng, intensidad], ...], {radius, blur, max, gradient})`.
  - Riesgo: Leaflet.heat 0.2.0 es de 2015, sin mantenimiento. Basta para el MVP, pero
    probarlo con Leaflet 1.9.4 antes de comprometerse.
- Política de teselas de OSM: usar solo `https://tile.openstreetmap.org/{z}/{x}/{y}.png`,
  mantener la atribución visible, no cachear teselas para uso sin conexión. El
  service worker (PWA) puede guardar la app, pero **no** precargar teselas.

## Almacenamiento de fotos

El correo lleva un **enlace** a la foto, no la adjunta. Criterios: sin costo, de
preferencia sin tarjeta, enlace público estable, sin depender del disco del servidor.

- **Cloudinary Free** (decidido el 29/10/2026, ADR-0006): sin tarjeta, 25 créditos/mes compartidos entre almacenamiento, tráfico y transformaciones (cifra de una fuente de terceros, por confirmar en cloudinary.com/pricing), redimensión por URL. Mega descartado.
- Cloudflare R2 como respaldo si se agotan los créditos.
- Descartado el disco del servidor: en Render Free es efímero.
- Flujo: el frontend comprime la foto; el backend la sube con la API key (variable de
  entorno) y guarda en PostgreSQL solo la URL y el identificador.
- Las fotos pueden mostrar personas: requieren aviso de privacidad (consulta pendiente
  al docente sobre la Ley 1581 de 2012).

## Despliegue actual (28/10/2026)

Verificado de punta a punta el 28/10/2026.

| Pieza | Servicio | Detalle |
| --- | --- | --- |
| Frontend (PWA) | Netlify | https://notiurba.netlify.app · `frontend/public/_redirects` (`/*  /index.html  200`) para el router SPA · variable `VITE_API_URL` |
| Backend | Render Web Service (Docker, Free, Virginia) | https://notiurba-api.onrender.com · Auto-Deploy «On Commit» · arranque en frío ~2 min |
| Base de datos | Neon (Ohio, conexión directa) | Flyway aplicó V1–V8; PostGIS se activa en V1 (`CREATE EXTENSION postgis`); admin sembrado por V4 |
| Login con Google | Google Auth Platform (cliente OAuth Web) | En estado «Prueba» con 3 usuarios de prueba; no publicado |

Variables de entorno en Render: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`,
`ADMIN_DEV_PASSWORD`, `GOOGLE_CLIENT_ID`, `CORS_ORIGINS`, `RECUPERACION_ENLACE`,
`RECUPERACION_MOSTRAR_ENLACE`, `PORT`. Ningún secreto va en el repo.

Pruebas que pasan contra Render (colección Postman en `docs/api/`):
`GET /api/health` → 200 `{estado, baseDeDatos: ok}`; `POST /api/auth/login` (admin) → 200 con
rol ADMIN; `GET /api/categorias` con JWT → 200 y 15 categorías; `GET /api/admin/ping` con
JWT de admin → 200. La página `/app/estado` del frontend consulta `/api/health`.

El admin sembrado es `admin.dev@notiurba.local`; su contraseña es la de `ADMIN_DEV_PASSWORD`
en el primer despliegue (V4 no se reaplica si la variable cambia después).

## Hosting

Tres piezas: frontend estático (PWA), backend Spring Boot (Docker) y PostgreSQL+PostGIS.

- **Netlify** (frontend): reemplazó a Vercel Hobby, que era la propuesta inicial (ver ADR-0004).
- **Render Web Service** (backend): 750 h/mes, 512 MB RAM. Se apaga tras 15 min sin
  tráfico y tarda ~1 min en despertar. Disco efímero.
- **Neon** (base de datos): gratis, sin tarjeta, 1 GB. Se suspende tras 5 min sin uso.
  PostGIS con `CREATE EXTENSION postgis`: **confirmado** en el plan Free (V1 corrió en Neon).
- Render PostgreSQL descartado: expira a los 30 días.

### Bloqueo de puertos de correo en Render

Render Free **bloquea los puertos salientes 25, 465 y 587**. Por eso el envío no usa SMTP:
`EnviadorCorreoResend` llama a la API HTTP de Resend (`POST https://api.resend.com/emails`),
que sale por HTTPS y evita el bloqueo. Se quitó `spring-boot-starter-mail`. Probado con un
envío real desde local el 01/11/2026 (200); **falta probarlo desde Render**. Variables en
Render: `RESEND_API_KEY` y `RESEND_REMITENTE`. Resend también acepta SMTP por 2465 y 2587
(no bloqueados), pero no se usa.

## Pendientes del stack

- Hosting desplegado y fotos con Cloudinary: el almacén (`AlmacenFotos`) ya está integrado y probado con una subida real; falta agregar las variables `CLOUDINARY_*` en Render y el endpoint de subida (NOT-52).
- Probar desde Render el envío con la API HTTP de Resend (ya probado desde local; PostGIS en Neon ya verificado).
- Ejecutar la prueba mínima del mapa y probar Leaflet.heat con Leaflet 1.9.4 (Linear NOT-9).
- Confirmar si se usa react-leaflet y su compatibilidad con React 18.

Fuente: pestaña «Stack tecnológico» del Google Doc.
