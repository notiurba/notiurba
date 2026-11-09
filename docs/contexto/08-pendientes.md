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

- ~~Mecanismo de envío del correo~~: decidido el 29/10/2026, Resend (ADR-0002).
- Categorías 8 y 9: confirmar con la Alcaldía.
- Dominio para Resend antes del 8 de noviembre (ADR-0007): comprar un `.com` (~US$10–12/año en Cloudflare o Porkbun), enviar desde un subdominio y dejar por escrito quién es el dueño. Sin él Resend no envía a las Secretarías; el borrador ya no es respaldo.

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

## Login con Google y modelo v8 (28/10/2026)

Rama `feat/login-google`: migraciones V5 (Google), V6 (nombres/apellidos) y V7 (ajustes v8),
`POST /api/auth/google`, `/google/completar`, `/recuperar`, `/restablecer` y `GET /api/categorias`.

- **Credenciales OAuth de Google Cloud**: crear el ID de cliente (origen del frontend en Netlify
  y localhost) y ponerlo en `GOOGLE_CLIENT_ID` (Render y local). Sin él el backend no arranca.
- **Vinculación automática por correo** (caso 2 de `/api/auth/google`): es una *propuesta*,
  por confirmar con Andrés. Está aislada en `GoogleAuthServicio.vincularPorCorreo`.
- **Respaldo en Neon antes de aplicar V6**: V6 hace `DROP COLUMN` (irreversible). Crear un
  branch de respaldo en Neon antes de desplegar.
- **Reportes anónimos y de menores de edad**: fuera del MVP; requieren acuerdo con las
  empresas de servicios públicos y la Alcaldía (Ley 1581 de 2012, art. 7).
- **Validación jurídica del aviso de privacidad**: la versión 3 ya es la vigente (V12, 01/11/2026), pero su texto legal sigue sin validar con el docente o un asesor jurídico (puntos en ADR-0008).
- **Barrios reales y polígonos de comunas (NOT-54)** (los polígonos experimentales ya están en V14 y los scripts
  `backend/scripts/generar-capa-comunas.py` y `generar-barrios.py` dejan la carga lista; falta el CSV de barrios
  de Amelia y la capa oficial; V17 ya carga 132 barrios históricos del PMGR 2020, pendientes de confirmar con el municipio): sin barrios nadie completa el perfil, y
  sin polígonos un reporte con solo GPS (B3) no deriva comuna ni barrio.
- **Registro con correo y contraseña**: aún no existe en el backend. Cuando se haga, usa
  `nombres`/`apellidos` y la regla de `auth/ReglaContrasena` (8+ caracteres, letras y números).
- **Envío real del correo de recuperación**: hoy queda detrás de `EnviadorEnlaceRecuperacion`
  con una implementación que solo escribe en el log (`RECUPERACION_MOSTRAR_ENLACE=true` en
  desarrollo). Falta conectarlo a `EnviadorCorreo` (Resend, ya implementado).
- **Ruta del frontend «Nueva contraseña»**: el enlace apunta a `RECUPERACION_ENLACE`
  (por defecto `http://localhost:5173/app/nueva-contrasena?token=...`); la página no existe aún.
- **Categoría 10 y canal SEDAMA (resuelto)**: el commit `741c2b3` los cambiaba editando V3,
  lo que rompe el checksum de Flyway. Se revirtió y el cambio pasó a
  `V8__categoria_10_y_sedama.sql`. Regla: una migración aplicada no se edita; se corrige con otra.
- **`db/probar.sh` estaba roto en `develop`** desde V4 (barrio 1 y usuario 1 repetidos con
  `datos_ejemplo.sql`). Arreglado en `datos_ejemplo.sql`: conserva el barrio 1 de V4 y corre
  al admin de V4 al id 1000.

## Despliegue en Netlify + Render + Neon (28/10/2026)

Hecho y verificado: ver «Despliegue actual» en `02-stack.md` y ADR-0004. Health, login ADMIN,
`/api/categorias` (15) y `/api/admin/ping` responden 200 contra Render.

Queda por hacer:

- **Login con Google en producción**: el cliente OAuth está en «Prueba» (3 usuarios de prueba).
  Para abrirlo a cualquiera hay que completar la página de marca y publicar la app.
- **Rotar la contraseña de admin** (`ADMIN_DEV_PASSWORD` en Render y el hash en Neon con
  `UPDATE usuario SET password_hash = crypt('NUEVA', gen_salt('bf')) WHERE correo = 'admin.dev@notiurba.local';`).
  Quedó expuesta en capturas de pantalla durante las pruebas.
- **Frontend**: no existen aún las pantallas de registro, login, recuperación, crear reporte ni mapa.
- **`GET /api/categorias` exige JWT**: decidir si debe ser pública (la pantalla de crear reporte la necesita).
- **Correo real** (NOT-15, 39, 40) y almacenamiento de fotos (NOT-28): correo (Resend) y fotos (Cloudinary, ADR-0006) integrados detrás de interfaces (`EnviadorCorreo`, `AlmacenFotos`). Fotos: conectadas al endpoint de subida y probadas en Render el 01/11/2026. Correo: conectado al registro (verificación); falta conectarlo a la recuperación de contraseña y al endpoint `enviar`.
- **Colección Postman** (`docs/api/`): Postman convirtió los `.json` a YAML; revisar que no haya
  secretos antes de hacer commit de `.postman/`, `postman/` y los YAML.
- Auto-Deploy de Render está activo: cada commit a la rama enlazada redespliega.
- El netlify del proyecto está bajo el equipo `cafe-smart`; moverlo si hace falta.

## Verificaciones técnicas

- Prueba mínima del mapa: OSM + marcadores + GeoJSON de comunas + Leaflet.heat (NOT-9).
- Probar desde Render el envío con la API HTTP de Resend (probado desde local el 01/11/2026; Render Free bloquea SMTP 25/465/587, por eso no se usa).
- ~~PostGIS en Neon en el plan Free~~: verificado el 28/10/2026 (V1 corrió en Neon).

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
