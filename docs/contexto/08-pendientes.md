# Pendientes

Esta lista refleja la pestaña «Pendientes» del Google Doc. Prioridad de arriba hacia abajo.

## Entregables del Sprint 1

- **Requisitos y reglas de negocio**: existen (ver 04 y 05). Falta confirmar los tiempos
  de RNF-04, la respuesta del docente sobre datos personales (Ley 1581) y validar las
  categorías 8 y 9.
- **Diseño técnico (arquitectura y modelo de datos)**: parcial. Falta todo lo listado en
  `03-arquitectura-y-datos.md` (diagramas de arquitectura y despliegue, modelo de datos
  completo, ER, API REST, flujo de estados, notificaciones). **Responsable: Andrés.**
- **Prototipo navegable en Figma**: existe, falta el enlace en el documento y una pestaña
  que lo describa. **Responsable: Amelia.**
- **Delimitación de las 10 comunas**: base real lista (GeoJSON del POT 2015 convertido);
  falta validarlo con Planeación y aclarar que se convirtió, no se digitalizó.

## Decisiones abiertas

- Mecanismo de envío del correo: cuenta del ciudadano vs Resend (ADR-0002). Fin del
  Sprint 2 o Sprint 3.
- Categorías 8 y 9: confirmar con la Alcaldía.
- Dominio para Resend antes del 8 de noviembre (si no, solo modo borrador, RF-13).

## Estructura del frontend y auth por rol (25/10/2026)

Resuelto: ver ADR-0005. `app/` (ciudadano), `admin/` (panel, ya con el mapa/filtros/
diagnóstico de Katherine), `compartido/` (geo, utils, estilos, assets, andamiaje de
sesión). Backend: paquetes `seguridad/`, `auth/`, `admin/`, `usuario/`, con
`POST /api/auth/login` devolviendo JWT con rol y `/api/admin/**` exigiendo `ROLE_ADMIN`.

**Verificado de punta a punta el 25/10/2026** contra Postgres real (Docker local):
`V1`–`V4` migran limpio, `POST /api/auth/login` con el usuario ADMIN de desarrollo
devuelve un JWT válido, `GET /api/admin/ping` responde 200 con ese token y 401 sin
token o con uno inválido/con contraseña incorrecta. La migración `V4` ya no depende de
que alguien cargue barrios antes: si no encuentra ninguno, siembra uno administrativo
mínimo ella misma (ver el comentario en `V4__usuario_admin_semilla.sql`).

Queda por hacer:

- Crear la página `/login` y conectar `RutaProtegida` a las rutas `/app/*` y `/admin/*`
  (hoy existe el componente pero no está en uso, para no dejar una redirección a un
  `/login` que no existe).
- Limpieza menor: Spring Security genera una contraseña aleatoria de desarrollo en el
  log al arrancar ("Using generated security password") porque no hay un
  `UserDetailsService` propio — no afecta el login real (que no usa ese mecanismo),
  pero vale la pena silenciarlo cuando se revise `SecurityConfig` a fondo.
- El puerto de Postgres en `docker-compose.yml` quedó en **5433** (no 5432) porque la
  máquina de Andrés ya tenía otro contenedor (`postgres-cursos`, de otro proyecto) usando
  5432. Si alguien más del equipo no tiene ese conflicto, puede dejarlo en 5432 en su
  copia local del `.env`/`docker-compose.yml` — no hace falta que todos compartan el
  mismo puerto.
- Responsable: Andrés (backend/auth) + Katherine (página de login en el frontend).

## Verificaciones técnicas

- Prueba mínima del mapa: OSM + marcadores + GeoJSON de comunas + Leaflet.heat (NOT-9).
- Envío de Resend por el puerto 2587 (o 2465) desde Render (Render Free bloquea 25/465/587).
- PostGIS en Neon en el plan Free (`CREATE EXTENSION postgis`).

## Reglas de negocio por definir

- Qué hace el sistema con un punto que cae en un **hueco** o **solapamiento** de la capa
  de comunas (asignar la más cercana, enviar a revisión, etc.). Hoy no está en las RN.

## Deuda del prototipo del frontend

- Sacar los archivos pesados del repo: `src/data/geo/plano-p28-tulua-2015-300dpi.png`
  (~39 MB) y `plano-p28-tulua-2015.pdf` (~5 MB). Ya están en el historial de git; GitHub
  bloquea sobre 100 MB por archivo. Moverlos fuera de `src/` o usar Git LFS. (No se
  tocaron en la reorganización del 25/10/2026: sacarlos de `src/` no libera espacio sin
  reescribir el historial, y eso es una decisión aparte.)
- Mover la asignación de comuna al backend (PostGIS) como fuente de verdad.
- El panel «Diagnóstico de límites» (ahora en `admin/componentes/DiagnosticoLimites.jsx`)
  es una herramienta interna: ocultarlo en producción.
- Normalizar tildes en los textos de la interfaz.
