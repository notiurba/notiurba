# NotiUrba · Backend

Java 21 · Spring Boot 3.3 · Maven · PostgreSQL 16 + PostGIS 3.4 · Flyway.

## Primer arranque

1. En la raíz del repositorio: `cp .env.example .env`, cambiar la contraseña y `docker compose up -d`.
2. Abrir la carpeta `backend/` en IntelliJ IDEA (se importa como proyecto Maven).
3. Definir las variables `DB_PASSWORD` (y, si cambian, `DB_URL`, `DB_USER`, `CORS_ORIGINS`) en la configuración de ejecución.
4. Ejecutar `NotiUrbaApplication`. Flyway crea las tablas de `src/main/resources/db/migration`.
5. Probar: `GET http://localhost:8080/api/health` → `{"estado":"ok"}`.

## Base de datos

| Archivo | Contenido |
| --- | --- |
| `V1__esquema.sql` | 24 tablas, restricciones e índices (GiST incluido). |
| `V2__reglas_y_vistas.sql` | Triggers de las reglas B1–B12 y vistas (`v_reporte_destino`, `v_reporte_no_enviado_por_correo`, `v_reporte_comuna`, `v_reporte_mapa`). |
| `V3__catalogos_iniciales.sql` | Catálogos, entidades, dependencias, canales y categorías. |

- Las migraciones aplicadas **no se editan**: los cambios van en `V4__…`, `V5__…`.
- Pendientes de datos: barrios, polígonos de `comuna.geom` y el texto oficial del aviso de privacidad.
- Verificar el SQL en una base desechable: `./db/probar.sh` (desde la raíz).
