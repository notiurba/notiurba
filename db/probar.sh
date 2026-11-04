#!/usr/bin/env bash
# Crea una base DESECHABLE en el contenedor, aplica todas las migraciones en orden numérico, carga los datos de
# ejemplo y corre las pruebas. No toca la base "notiurba". Requiere `docker compose up -d`.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] && set -a && . ./.env && set +a
USUARIO="${POSTGRES_USER:-notiurba}"
BASE="notiurba_prueba"
psql_db() { docker compose exec -T db psql -U "$USUARIO" -v ON_ERROR_STOP=1 -q "$@"; }

psql_db -d postgres -c "DROP DATABASE IF EXISTS $BASE" -c "CREATE DATABASE $BASE"
# Orden numérico (V2 antes que V10): el patrón V*.sql ordena como texto y rompería a partir de la V10.
MIGRACIONES=$(ls backend/src/main/resources/db/migration/V*.sql | sort -V)
for f in $MIGRACIONES db/datos_ejemplo.sql db/pruebas.sql; do
  echo "--- $f"
  psql_db -d "$BASE" < "$f"
done
psql_db -d postgres -c "DROP DATABASE $BASE"
