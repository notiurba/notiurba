# Guía de despliegue, respaldo y administrador real

Cómo montar NotiUrba desde cero, respaldar la base, dejar un administrador real y comprobar que todo funciona.
**Sin secretos**: solo nombres de variables. Los valores los escribe una persona del equipo en cada servicio.

Resumen del despliegue vivo: [02-stack.md](02-stack.md). Envío real a las entidades:
[14-activar-envio-real.md](14-activar-envio-real.md).

## 1. Piezas y orden de montaje

| Orden | Servicio | Para qué | Dónde se configura |
| --- | --- | --- | --- |
| 1 | Neon | Base PostgreSQL 16 + PostGIS | neon.tech |
| 2 | Cloudinary | Fotos de los reportes | cloudinary.com |
| 3 | Resend | Correo (dominio `reportes.notiurba.com`) | resend.com |
| 4 | Google Cloud | Ingreso con Google | console.cloud.google.com |
| 5 | Render | Backend (Docker, plan Free) | render.com |
| 6 | Netlify | Frontend (PWA) | netlify.com |

El backend necesita 1 a 4 antes de arrancar. El frontend necesita la dirección del backend (5).

## 2. Montaje paso a paso

### Neon (base de datos)
1. Crear un proyecto (PostgreSQL 16). Copiar la **cadena de conexión directa** (no la de pooler) y guardarla en un lugar
   privado. Neon la entrega como `postgresql://usuario:clave@host/base?sslmode=require`; el backend la pide separada:
   `DB_URL` = `jdbc:postgresql://host/base?sslmode=require` (con el prefijo `jdbc:` y sin usuario ni clave),
   `DB_USER` = el usuario y `DB_PASSWORD` = la clave.
2. No hay que crear tablas: Flyway aplica las migraciones (V1 en adelante) al arrancar el backend. V1 activa PostGIS.
3. El administrador inicial lo siembra V4 con la clave de `ADMIN_DEV_PASSWORD`, o con una clave aleatoria si la variable
   no existe. En ese caso se crea el administrador real con la sección 5.

### Cloudinary
1. Crear una cuenta (plan Free). En el panel copiar `Cloud name`, `API Key` y `API Secret`.
2. Van como `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` y `CLOUDINARY_API_SECRET`.

### Resend
1. Verificar el dominio `reportes.notiurba.com` (registros DNS que indica Resend).
2. Crear una API key (`RESEND_API_KEY`). El remitente es una dirección del dominio verificado (`RESEND_REMITENTE`).
3. Registrar el webhook de rebotes: paso a paso en [14-activar-envio-real.md](14-activar-envio-real.md); su secreto
   (`whsec_…`) va en `RESEND_WEBHOOK_SECRET`.

### Google (ingreso con Google)
1. Google Auth Platform → crear un cliente OAuth de tipo «Aplicación web».
2. Orígenes autorizados de JavaScript: `https://notiurba.netlify.app` (y `http://localhost:5173` para desarrollo).
3. Copiar el ID de cliente: es `GOOGLE_CLIENT_ID` en Render y `VITE_GOOGLE_CLIENT_ID` en Netlify.
4. Mientras la app esté en estado «Prueba», solo entran los usuarios de prueba que se agreguen en Google.

### Render (backend)
1. New → Web Service → conectar el repositorio, rama `develop` (o `main` para la versión final), **Docker**, plan Free.
   Directorio raíz `backend`. Auto-Deploy «On Commit».
2. Cargar las variables de la sección 3.
3. Al desplegar, el servicio corre las migraciones y responde en `/api/health`.

### Netlify (frontend)
1. Importar el repositorio. Rama de producción `develop` (o `main`), base `frontend`, comando `pnpm build`, publish `dist`.
2. Variables: `VITE_API_URL` (dirección del backend en Render) y `VITE_GOOGLE_CLIENT_ID`.
3. La redirección de la SPA vive en `frontend/public/_redirects`; no se borra.
4. Cambiar una variable de Netlify exige **volver a desplegar** (se hornean en el build).

## 3. Variables de entorno

### Render (backend)

| Variable | Obligatoria | Para qué |
| --- | --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Sí | Conexión a Neon |
| `JWT_SECRET` | Sí | Firma de sesiones (larga y aleatoria) |
| `GOOGLE_CLIENT_ID` | Sí | Validar el ingreso con Google |
| `CORS_ORIGINS` | Sí | Sitios permitidos, con comodín para las vistas previas: `https://notiurba.netlify.app,https://deploy-preview-*--notiurba.netlify.app` |
| `RECUPERACION_ENLACE`, `VERIFICACION_ENLACE` | Sí | Páginas del frontend a las que apuntan los correos |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Sí | Fotos |
| `RESEND_API_KEY`, `RESEND_REMITENTE` | Sí | Correo |
| `RESEND_WEBHOOK_SECRET` | Sí | Firma de los avisos de rebote |
| `NOTIURBA_ENVIO_REPORTES_HABILITADO` | Sí | `true` enciende el envío de reportes |
| `RESEND_PRUEBA_DESTINO` | Solo en pruebas | Desvía todos los correos a la bandeja de pruebas |
| `ADMIN_DEV_PASSWORD` | **No** | Solo para sembrar el admin de una base nueva. **Se retira** de producción (sección 5) |

### Netlify (frontend)

`VITE_API_URL` y `VITE_GOOGLE_CLIENT_ID`.

## 4. Respaldo y restauración de la base

Los respaldos contienen datos personales. **No se suben al repo, no se pegan en chats y se borran al terminar la prueba.**

### Respaldo rápido antes de un cambio (branch de Neon)
1. Neon → Branches → Create branch, desde la rama principal y «current data».
2. Ponerle un nombre con la fecha (por ejemplo `respaldo-2026-11-14`). Un branch es una copia instantánea que se puede
   consultar y restaurar desde el panel.
3. Mantenerlo hasta confirmar que el cambio salió bien. Es la protección de la sección 5.

### Copia a un archivo (`pg_dump`)
Neon corre PostgreSQL **18** (el 14/11/2026 respondió 18.7). El `pg_dump` y la base de restauración deben ser de la versión 18 o mayor:
con la imagen 16 falla con «server version mismatch».

1. Con Docker abierto, en la terminal (la cadena se lee de la variable sin mostrarla):

```bash
read -rs NEON_URL; export NEON_URL
docker run --rm -e NEON_URL postgis/postgis:18-3.6 sh -c 'pg_dump --format=custom --no-owner --no-privileges "$NEON_URL"' > respaldo.dump
ls -lh respaldo.dump
```

   La cadena tiene la forma `postgresql://usuario:clave@host/base?sslmode=require`.

### Restaurar en una base vacía (comprobación)
1. Levantar un PostgreSQL desechable y restaurar:

```bash
docker run -d --name notiurba-restauracion -e POSTGRES_PASSWORD=prueba postgis/postgis:18-3.6
until docker exec notiurba-restauracion pg_isready -U postgres >/dev/null 2>&1; do sleep 2; done
docker exec notiurba-restauracion createdb -U postgres restaurada
docker exec -i notiurba-restauracion pg_restore -f - --no-owner < respaldo.dump \
  | grep -v 'SET (pg_attver=' \
  | docker exec -i notiurba-restauracion psql -U postgres -d restaurada -q
```

   **Por qué no se usa `pg_restore -d` directo:** el volcado de Neon trae una opción propia de Neon
   (`ALTER COLUMN … SET (pg_attver=…)`) que un PostgreSQL normal rechaza («unrecognized parameter»), y cada tabla que la
   lleva no se crea. El `grep -v` quita esas líneas y el resto se restaura completo. Si al restaurar aparecen errores,
   revisa que no haya otra opción rara en el volcado.
2. Comparar los conteos en Neon y en la copia restaurada. La consulta es la misma:

```sql
SELECT (SELECT count(*) FROM usuario) AS usuarios, (SELECT count(*) FROM reporte) AS reportes,
       (SELECT count(*) FROM comuna) AS comunas, (SELECT count(*) FROM barrio) AS barrios,
       (SELECT max(version) FROM flyway_schema_history) AS ultima_migracion;
```

   Se ejecuta en la copia con
   `docker exec notiurba-restauracion psql -U postgres -d restaurada -c "<la consulta>"` y en Neon con su editor SQL.
   Los valores deben coincidir.
3. Borrar lo que quedó:

```bash
docker rm -f notiurba-restauracion && rm respaldo.dump
```

### Resultado de la última comprobación
14/11/2026: archivo de 215 KB (PostgreSQL 18.7 en Neon). Restaurado en una base vacía sin errores. Conteos iguales en Neon y en la
copia: 6 usuarios, 6 reportes, 10 comunas, 133 barrios y última migración 22. El archivo y la base de prueba se borraron.

## 5. Administrador real

Hasta ahora el administrador es la cuenta sembrada `admin.dev@notiurba.local`. Se **convierte** en la cuenta real
(nombre, correo y clave propios). Pasos, en este orden:

1. **Antes:** crear el branch de respaldo en Neon (sección 4).
2. Preparar los valores sin escribirlos en ningún archivo (la clave no se ve al teclearla):

```bash
read -rs NEON_URL; export NEON_URL
read -r  CORREO_ADMIN;    export CORREO_ADMIN
read -r  NOMBRES_ADMIN;   export NOMBRES_ADMIN
read -r  APELLIDOS_ADMIN; export APELLIDOS_ADMIN
read -rs CLAVE_ADMIN;     export CLAVE_ADMIN
```

3. Ejecutar. El cambio solo se confirma si los datos son válidos (correo con forma de correo, nombres y apellidos no
   vacíos, clave de 12 caracteres o más), el correo nuevo **no existe** ya como otra cuenta y se actualiza **una** fila;
   si no, se deshace y no cambia nada:

```bash
docker run --rm -i -e NEON_URL -e CORREO_ADMIN -e NOMBRES_ADMIN -e APELLIDOS_ADMIN -e CLAVE_ADMIN \
  postgis/postgis:18-3.6 sh -c 'psql "$NEON_URL"' <<'SQL'
\set ON_ERROR_STOP on
\set correo `printenv CORREO_ADMIN`
\set nombres `printenv NOMBRES_ADMIN`
\set apellidos `printenv APELLIDOS_ADMIN`
\set clave `printenv CLAVE_ADMIN`
BEGIN;
SELECT NOT EXISTS (SELECT 1 FROM usuario WHERE lower(correo) = lower(:'correo')) AS libre,
       (:'correo' ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'
        AND length(btrim(:'nombres')) > 0 AND length(btrim(:'apellidos')) > 0
        AND length(:'clave') >= 12) AS valido \gset
WITH cambio AS (
  UPDATE usuario
     SET correo = :'correo', nombres = :'nombres', apellidos = :'apellidos',
         password_hash = crypt(:'clave', gen_salt('bf')), correo_verificado_en = now()
   WHERE correo = 'admin.dev@notiurba.local' AND id_rol = 2 AND :'libre'::boolean AND :'valido'::boolean
  RETURNING 1)
SELECT (count(*) = 1) AS hecho FROM cambio \gset
\if :hecho
  COMMIT;
  \echo 'Listo: el administrador real quedó configurado.'
\else
  ROLLBACK;
  \if :valido
    \echo 'No se cambió nada: el correo ya existe como otra cuenta, o ya no queda la cuenta sembrada.'
  \else
    \echo 'No se cambió nada: revisa que el correo tenga forma de correo, que nombres y apellidos no estén vacíos y que la clave tenga 12 caracteres o más.'
  \endif
\endif
SQL
```

4. Limpiar las variables de la terminal: `unset NEON_URL CORREO_ADMIN NOMBRES_ADMIN APELLIDOS_ADMIN CLAVE_ADMIN`.
5. **Probar el inicio de sesión** en el panel con el correo y la clave nuevos.
6. Solo si el paso 5 salió bien: en Render, **borrar `ADMIN_DEV_PASSWORD`**. Render reinicia; comprobar `/api/health`.
7. Si algo falla, el branch de Neon permite volver atrás. El branch se borra cuando todo esté probado.

Notas:
- Si el correo ya existe como cuenta de ciudadano o de Google, el cambio no se hace. Hay que usar otro correo o resolver
  esa cuenta antes.
- La contraseña vieja deja de servir para **iniciar sesión** en cuanto se confirma el cambio. Pero una sesión ya abierta
  con la cuenta vieja sigue valiendo hasta 60 minutos (el servidor no consulta la base en cada llamada). Si se sospecha
  de la clave vieja, cambiar `JWT_SECRET` en Render cierra todas las sesiones.
- El nombre, el correo y la clave del administrador **no** se escriben en el repo.

## 6. Render Free: servidor dormido y arranque en frío

- Render Free duerme el servicio tras 15 minutos sin tráfico. La primera llamada despierta el servidor.
- **Aviso periódico:** el flujo `.github/workflows/mantener-despierto.yml` consulta el servidor cada 5 minutos para que no
  se duerma. Se ve en GitHub → Actions. Si falla, GitHub avisa por correo.
  - **Solo corre desde `main`** (la rama por defecto del repo): hasta que el flujo llegue a `main`, no hace nada. Mientras
    tanto se puede lanzar a mano con «Run workflow».
  - GitHub no garantiza la hora exacta de los avisos programados: pueden retrasarse. Reduce las veces que se duerme,
    pero no las elimina. Si hace falta más seguridad, se añade un servicio externo gratis (por ejemplo cron-job.org).
  - Si GitHub lo pausa por falta de actividad del repo (60 días), se reactiva con «Run workflow».
  - Consulta una ruta protegida (`/api/admin/ping`, responde 401) y **no toca la base**: despierta a Render y deja que
    Neon se suspenda solo, para no gastar sus horas gratis.
- **Horas del plan:** Render Free da 750 horas al mes y un servicio encendido todo el mes usa ~744. Si faltan horas,
  se desactiva el flujo y se acepta el arranque en frío.
- **Arranque en frío medido:** el 14/11/2026 una primera llamada con el servidor dormido superó los 120 s, y el arranque de un despliegue en Render tardó 124 s («Started NotiUrbaApplication in 124.397 seconds»). Con el
  servidor despierto, `/api/health` responde en menos de 0,3 s.
  _(se completa con la medición final con y sin aviso)_
- Mientras el servidor despierta, el navegador no recibe encabezados CORS y la app muestra «No pudimos comunicarnos con
  el servidor». La app ya explica el reintento; el pulido de ese aviso es NOT-137.

## 7. Versión final y etiqueta

1. Integrar `develop` en `main` por PR.
2. Crear la etiqueta (por ejemplo `v1.0.0`) sobre `main` desde GitHub → Releases.
3. Render y Netlify de producción deben desplegar ese mismo commit (revisar el commit mostrado en cada panel).

## 8. Verificación después de desplegar

Con la terminal, sin secretos:

```bash
tools/verificar-despliegue.sh
```

Comprueba que `/api/health` responde con la base en `ok`, que CORS acepta el sitio y las vistas previas y rechaza un
sitio ajeno, y que las rutas directas de la SPA responden. Después, a mano en `https://notiurba.netlify.app`:

- [ ] Registrarse con un correo de prueba y verificar el correo.
- [ ] Crear un reporte con foto, ubicación y categoría; ver el código de seguimiento.
- [ ] Ver el reporte en «Mis reportes».
- [ ] Entrar al panel de administración con la cuenta real, ver el reporte y cambiar su estado.
- [ ] Con `RESEND_PRUEBA_DESTINO` puesta, el correo llega a la bandeja de pruebas con «[PRUEBA]».

## 9. Envío real a las entidades

La activación (quitar `RESEND_PRUEBA_DESTINO` y probar con un reporte controlado) y la vuelta a pruebas están en
[14-activar-envio-real.md](14-activar-envio-real.md). Es una decisión de Andrés y no forma parte del despliegue.
