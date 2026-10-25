# Stack tecnológico

Acordado en el Acta y en la planificación (sección 3.2). Las filas de fotos y
hosting eran «por definir» y quedan como propuesta.

| Área | Tecnología |
| --- | --- |
| Backend | Java 21 + Spring Boot 3.3 (web, data-jpa, security, mail, validation) |
| Geolocalización | PostgreSQL 16 + PostGIS 3.4 (índice GiST), Hibernate Spatial |
| Seguridad | Spring Security + JWT (jjwt) |
| Frontend | React 18 + Vite + vite-plugin-pwa |
| Mapas | Leaflet 1.9 + OpenStreetMap + Leaflet.heat |
| Correo | JavaMailSender + Resend (SMTP por 2465 o 2587 en Render Free; ver abajo) |
| Contenedores | Docker (postgis/postgis) para desarrollo local |
| Fotos | Propuesta: Cloudinary (plan Free) |
| Hosting | Propuesta: Vercel (frontend) + Render (backend) + Neon (PostgreSQL/PostGIS) |

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

- **Cloudinary Free** (propuesta): sin tarjeta, 25 créditos/mes, redimensión por URL.
- Cloudflare R2 como respaldo si se agotan los créditos.
- Descartado el disco del servidor: en Render Free es efímero.
- Flujo: el frontend comprime la foto; el backend la sube con la API key (variable de
  entorno) y guarda en PostgreSQL solo la URL y el identificador.
- Las fotos pueden mostrar personas: requieren aviso de privacidad (consulta pendiente
  al docente sobre la Ley 1581 de 2012).

## Hosting

Tres piezas: frontend estático (PWA), backend Spring Boot (Docker) y PostgreSQL+PostGIS.

- **Vercel Hobby** (frontend): gratis, uso personal/no comercial, válido para lo académico.
- **Render Web Service** (backend): 750 h/mes, 512 MB RAM. Se apaga tras 15 min sin
  tráfico y tarda ~1 min en despertar. Disco efímero.
- **Neon** (base de datos): gratis, sin tarjeta, 1 GB. Se suspende tras 5 min sin uso.
  PostGIS con `CREATE EXTENSION postgis` — confirmar en el plan Free (por verificar).
- Render PostgreSQL descartado: expira a los 30 días.

### Bloqueo de puertos de correo en Render

Render Free **bloquea los puertos salientes 25, 465 y 587**. Resend también acepta
SMTP por **2465 (SSL)** y **2587 (STARTTLS)**, que no están bloqueados: JavaMailSender
debe usar uno de ellos. **No está probado**: verificar con un envío real al desplegar.
Respaldo: la API HTTP de Resend (`POST https://api.resend.com/emails`), que sale por HTTPS.

## Pendientes del stack

- Decidir hosting y almacenamiento de fotos antes del 8 de noviembre (riesgo).
- Verificar PostGIS en Neon y el envío real de Resend por el puerto 2587 desde Render.
- Ejecutar la prueba mínima del mapa y probar Leaflet.heat con Leaflet 1.9.4 (Linear NOT-9).
- Confirmar si se usa react-leaflet y su compatibilidad con React 18.

Fuente: pestaña «Stack tecnológico» del Google Doc.
