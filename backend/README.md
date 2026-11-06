# NotiUrba · Backend

Java 21 · Spring Boot 3.3 · Maven · PostgreSQL 16 + PostGIS 3.4 · Flyway.

## Primer arranque

1. En la raíz del repositorio: `cp .env.example .env`, cambiar la contraseña y `docker compose up -d`.
2. Abrir la carpeta `backend/` en IntelliJ IDEA (se importa como proyecto Maven).
3. Definir las variables `DB_PASSWORD` (y, si cambian, `DB_URL`, `DB_USER`, `CORS_ORIGINS`) en la configuración de ejecución.
4. Ejecutar `NotiUrbaApplication`. Flyway crea las tablas de `src/main/resources/db/migration`.
5. Probar: `GET http://localhost:8080/api/health` → `{"estado":"ok","baseDeDatos":"ok"}` (503 con `"baseDeDatos":"error"` si la base no responde).

## Base de datos

| Archivo | Contenido |
| --- | --- |
| `V1__esquema.sql` | 24 tablas, restricciones e índices (GiST incluido). |
| `V2__reglas_y_vistas.sql` | Triggers de las reglas B1–B12 y vistas (`v_reporte_destino`, `v_reporte_no_enviado_por_correo`, `v_reporte_comuna`, `v_reporte_mapa`). |
| `V3__catalogos_iniciales.sql` | Catálogos, entidades, dependencias, canales y categorías. |
| `V13__asignar_comuna_por_punto.sql` | `fn_asignar_comuna` (contiene, solape, hueco cercano, fuera de cobertura), la distancia máxima en `configuracion` y la vista `v_reporte_comuna` con esas reglas (NOT-55). |
| `V14__capa_comunas_experimental.sql` | Carga la capa de comunas SINTÉTICA y no oficial de NOT-31 en `comuna.geom` y anota la fuente en `configuracion`. Se reemplaza generando una migración nueva con `scripts/generar-capa-comunas.py`. |
| `V15__barrio_interno.sql` | Columna `barrio.interno`: marca como interno el barrio «Administración NotiUrba» para que no salga en el catálogo ni se pueda elegir. |

- Las migraciones aplicadas **no se editan**: los cambios van en `V4__…`, `V5__…`.
- Pendientes de datos: barrios, polígonos de `comuna.geom` y la validación jurídica del aviso de privacidad (la versión 3 ya es la vigente, V12).
- Verificar el SQL en una base desechable: `./db/probar.sh` (desde la raíz).

## Cargar datos de territorio (scripts)

Los scripts de `backend/scripts/` convierten un archivo en una migración nueva de Flyway. No tocan código.

- **Comunas** (`generar-capa-comunas.py`): reemplaza la capa de polígonos. Hoy está la experimental (V14).
- **Barrios** (`generar-barrios.py`): llena la tabla `barrio` desde un CSV con las columnas `comuna` y `barrio`
  (plantilla en `plantilla-barrios.csv`). Solo agrega y se puede repetir sin duplicar.

Ejemplo: `python3 backend/scripts/generar-barrios.py barrios.csv > backend/src/main/resources/db/migration/V17__barrios_reales.sql`
(poner el número de versión que siga; cada script explica sus opciones con `--help` o en su encabezado).
