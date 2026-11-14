---
name: notiurba-pruebas-y-despliegue
description: Cómo correr las pruebas del backend y del frontend de NotiUrba, levantar una base PostGIS desechable, revisar producción (Render, Netlify, CORS) y evitar los tropiezos conocidos. Úsese antes de decir que algo "pasa" o de tocar migraciones, correo o despliegue.
---

# Pruebas y despliegue de NotiUrba

Contexto general: `docs/contexto/12-estado-actual-y-traspaso.md`. Trabaje en una rama `feature/*` o `docs/*`
(nunca directo en `develop`) y sin líneas de coautoría de Claude en commits ni PR.

## Backend (Java 21, Maven)

1. Docker Desktop abierto. Base desechable: contenedor `postgis/postgis:16-3.4` (no use la base real).
2. Variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`; para las pruebas de administración de Amelia,
   `NOTIURBA_TEST_DB_URL` y una base `notiurba_test_admin_bootstrap` en `127.0.0.1`.
3. `mvn clean test` desde `backend/`. **Siempre `clean`** al cambiar de rama: un `target/` viejo trae
   migraciones de otra rama y Flyway falla.
4. `MigrationIntegrationTest` falla si las variables `DB_*` del `.env` están exportadas en el mismo comando:
   ejecútelo con `env -i` y solo las variables necesarias.
5. Meta de referencia (14 nov 2026): 454 pruebas, 0 fallos, con una base PostGIS desechable y estas variables a la vez: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_DEV_PASSWORD` y `NOTIURBA_TEST_DB_*` (apuntan a una base `notiurba_test*`). Sin `DB_*` solo corren unas 278: las pruebas con Spring se omiten en silencio. `AdminIntegracionTest` pide `DB_URL` a una base llamada `notiurba_test_admin_bootstrap`. Quedan fuera `MigrationIntegrationTest` (va con `env -i`) y las que piden credenciales reales (Cloudinary, base con TLS).
   Para no omitir las 10 de enrutamiento: cree una base `notiurba_test_<algo>` en `notiurba-db` con `postgis` y
   `pgcrypto` y pase `NOTIURBA_TEST_DB_URL`, `NOTIURBA_TEST_DB_USER` y `NOTIURBA_TEST_DB_PASSWORD`; bórrela al final.
   QA de punta a punta con el jar: `tools/qa-panel-admin/qa-panel.sh` (panel) y `tools/qa-correo-reporte/qa-correo.sh`
   (mensaje a la entidad; no envía correos).
6. Si no hay Docker/DB, las pruebas de integración se omiten con `assumeTrue`: dígalo, no lo cuente como pasado.

## Frontend (React 19 + Vite, pnpm)

`cd frontend && pnpm install && pnpm test && pnpm lint && pnpm build` (230 pruebas con `node --test`).
Nunca npm ni `package-lock.json`. `VITE_USAR_MOCKS=true` simula la API solo en desarrollo.

## Migraciones (Flyway)

- Las aplicadas **no se editan**; se corrige con una nueva (hoy la siguiente es V23).
- En un `.sql` con GeoJSON, `${` choca con los marcadores de Flyway: deje un espacio tras `$geo$`.
- Una migración destructiva pide antes un branch de respaldo en Neon.
- Los scripts `backend/scripts/generar-*.py` convierten un CSV/GeoJSON en una migración.

## Producción

- Render Free duerme: la primera llamada tarda 60 a 90 s. `curl -m 90` y espere.
- Salud: `GET https://notiurba-api.onrender.com/api/health`. CORS: `curl -i -X OPTIONS .../api/auth/login
  -H "Origin: https://notiurba.netlify.app" -H "Access-Control-Request-Method: POST"`.
- Correo: `RESEND_PRUEBA_DESTINO` debe seguir puesta; el envío real exige autorización del equipo y el docente.
- «No pudimos comunicarnos con el servidor» = el navegador no recibió respuesta (Render dormido, red o CORS).
- Nunca imprima ni pegue secretos; las variables van en Render/Netlify, no en el repo.

## Antes de dar algo por hecho

Pruebe lo que dice haber arreglado (prueba automática o llamada real) y reporte lo que NO pudo verificar.
