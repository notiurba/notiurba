#!/bin/bash
# QA de punta a punta de /api/admin/mapa, /mapa/calor y /estadisticas contra el backend real.
set -u
REPO=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
QA=$(cd "$(dirname "$0")" && pwd)
SALIDA=$(mktemp -d)
cd "$REPO"
set -a; source .env; set +a
BASE="notiurba_e2e_$(date +%s)"
PUERTO=8089
API="http://localhost:$PUERTO"
LOG="$SALIDA/servidor.log"
sql() { docker exec -i notiurba-db psql -U "$POSTGRES_USER" -d "$1" -v ON_ERROR_STOP=1 -At -q; }

limpiar() {
  [ -n "${PID:-}" ] && kill "$PID" 2>/dev/null && wait "$PID" 2>/dev/null
  echo "DROP DATABASE IF EXISTS $BASE WITH (FORCE);" | sql postgres
  echo "== base $BASE borrada y servidor detenido; respuestas y log en $SALIDA"
}
trap limpiar EXIT

echo "CREATE DATABASE $BASE;" | sql postgres
export DB_URL="jdbc:postgresql://localhost:5433/$BASE" PORT=$PUERTO NOTIURBA_ENVIO_REPORTES_HABILITADO=false
JAR=$(ls backend/target/*.jar | grep -v original | head -1)
java -jar "$JAR" > "$LOG" 2>&1 &
PID=$!
for i in $(seq 1 90); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' $API/api/health)" = "200" ] && break; sleep 1
done
echo "== servidor arriba en ${i}s (pid $PID), base $BASE"

CLAVE_CIUDADANO="Qa$(openssl rand -hex 8)1"
sql "$BASE" <<SQL
INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio, direccion_correspondencia,
                     correo, correo_verificado_en, password_hash, id_rol, id_aviso_privacidad, acepto_privacidad_en)
SELECT 'Ana', 'Prueba', 1, '99000111', (SELECT min(id_barrio) FROM barrio WHERE id_comuna = 1), 'Cra 1 # 2-3',
       'ana.qa@prueba.local', now(), crypt('$CLAVE_CIUDADANO', gen_salt('bf')), 1, 1, now();

CREATE TEMP TABLE semilla(comuna int, categoria int, dias int, estado text);
INSERT INTO semilla VALUES (5,2,1,null),(5,2,2,'En trámite'),(5,1,3,'Resuelto'),(2,3,1,null),(2,1,4,null),
  (3,1,2,'Resuelto'),(1,4,1,null),(4,1,5,'En trámite'),(7,2,6,null),(5,1,40,'Resuelto'),(2,2,35,null);
WITH nuevos AS (
  INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion, autorizacion_envio_en, creado_en)
  SELECT u.id_usuario, s.categoria, (SELECT min(id_barrio) FROM barrio WHERE id_comuna = 1), 'Reporte de QA del panel',
         ST_PointOnSurface(ST_Difference(c.geom, (SELECT ST_Union(o.geom) FROM comuna o
                                                 WHERE o.id_comuna <> c.id_comuna AND o.geom IS NOT NULL))),
         now() - make_interval(days => s.dias), now() - make_interval(days => s.dias)
    FROM semilla s JOIN comuna c ON c.id_comuna = s.comuna, usuario u WHERE u.correo = 'ana.qa@prueba.local'
  RETURNING id_reporte, creado_en)
SELECT count(*) FROM nuevos;
INSERT INTO historial_estado (id_reporte, id_estado_reporte, observacion)
SELECT r.id_reporte, e.id_estado_reporte, 'QA'
  FROM reporte r JOIN semilla s ON r.creado_en::date = (now() - make_interval(days => s.dias))::date
                               AND r.id_categoria = s.categoria
  JOIN estado_reporte e ON e.nombre = s.estado WHERE s.estado IS NOT NULL;
-- sin ubicación (comuna por barrio) y sin comuna (lejos de la capa, sin barrio)
INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, autorizacion_envio_en, creado_en)
SELECT id_usuario, 1, (SELECT min(id_barrio) FROM barrio WHERE id_comuna = 1), 'Sin punto en el mapa', now(), now()
  FROM usuario WHERE correo = 'ana.qa@prueba.local';
INSERT INTO reporte (id_usuario, id_categoria, descripcion, ubicacion, autorizacion_envio_en, creado_en)
SELECT id_usuario, 1, 'Lejos de toda comuna', ST_SetSRID(ST_MakePoint(-75.5, 4.6), 4326), now(), now()
  FROM usuario WHERE correo = 'ana.qa@prueba.local';
SQL

login() { curl -s -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"correo\":\"$1\",\"password\":\"$2\"}" | python3 -I -c 'import json,sys; print(json.load(sys.stdin).get("token",""))'; }
TOKEN_ADMIN=$(login admin.dev@notiurba.local "$ADMIN_DEV_PASSWORD")
TOKEN_ANA=$(login ana.qa@prueba.local "$CLAVE_CIUDADANO")
[ -n "$TOKEN_ADMIN" ] && echo "== login admin: ok" || echo "== login admin: FALLA"
[ -n "$TOKEN_ANA" ] && echo "== login ciudadana: ok" || echo "== login ciudadana: FALLA"

ORACULO=$(sql "$BASE" <<'SQL'
SELECT json_build_object(
  'mapa', (SELECT count(*) FROM v_reporte_mapa),
  'mapaSinComuna', (SELECT count(*) FROM v_reporte_mapa WHERE id_comuna IS NULL),
  'todos', (SELECT count(*) FROM reporte),
  'vigentes', (SELECT count(*) FROM reporte WHERE vigente_hasta > now()),
  'resueltos', (SELECT count(*) FROM reporte r JOIN estado_reporte e USING (id_estado_reporte) WHERE e.nombre = 'Resuelto'),
  'categoriasActivas', (SELECT count(*) FROM categoria WHERE activa));
SQL
)
echo "== oráculo de la base: $ORACULO"

mkdir -p "$SALIDA/respuestas"
pedir() { # nombre ruta token
  local auth=(); [ -n "$3" ] && auth=(-H "Authorization: Bearer $3")
  curl -s -o "$SALIDA/respuestas/$1.json" -w '%{http_code} %{time_total}' ${auth[@]+"${auth[@]}"} "$API$2"
}
for r in mapa mapa/calor estadisticas; do pedir calentar "/api/admin/$r" "$TOKEN_ADMIN" >/dev/null; done

: > "$SALIDA/respuestas/_http.txt"
while read -r nombre ruta quien; do
  tok=""; [ "$quien" = admin ] && tok=$TOKEN_ADMIN; [ "$quien" = ana ] && tok=$TOKEN_ANA
  echo "$nombre $(pedir "$nombre" "$ruta" "$tok")" >> "$SALIDA/respuestas/_http.txt"
done <<'RUTAS'
mapa /api/admin/mapa admin
mapa_comuna5 /api/admin/mapa?comuna=5 admin
mapa_tramite /api/admin/mapa?estado=EN_TRAMITE admin
mapa_sin_token /api/admin/mapa nadie
mapa_ana /api/admin/mapa ana
calor /api/admin/mapa/calor admin
calor_p2 /api/admin/mapa/calor?pagina=2 admin
calor_t10 /api/admin/mapa/calor?tamano=10 admin
calor_ana /api/admin/mapa/calor ana
calor_t11 /api/admin/mapa/calor?tamano=11 admin
est /api/admin/estadisticas admin
est_vig /api/admin/estadisticas?vigencia=vigentes admin
est_fuera /api/admin/estadisticas?vigencia=FUERA_DE_PLAZO admin
est_cat1 /api/admin/estadisticas?idCategoria=1 admin
est_ana /api/admin/estadisticas ana
est_vig_mala /api/admin/estadisticas?vigencia=vencidos admin
est_comuna11 /api/admin/estadisticas?comuna=11 admin
est_fechas /api/admin/estadisticas?desde=2026-11-10&hasta=2026-11-01 admin
est_fecha_mala /api/admin/estadisticas?desde=manana admin
listado_comuna5 /api/admin/reportes?comuna=5 admin
listado_comuna11 /api/admin/reportes?comuna=11 admin
RUTAS

CORS_OK=$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS "$API/api/admin/mapa" \
  -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: GET' -H 'Access-Control-Request-Headers: authorization')
CORS_MALO=$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS "$API/api/admin/mapa" \
  -H 'Origin: https://sitio-ajeno.example' -H 'Access-Control-Request-Method: GET')

export ORACULO CORS_OK CORS_MALO SALIDA
python3 -I "$QA/verificar.py"
echo "--- 5 llamadas seguidas a cada ruta (segundos)"
for r in mapa mapa/calor estadisticas reportes; do
  printf '%-14s' "$r"
  for i in 1 2 3 4 5; do printf ' %s' "$(pedir repeticion /api/admin/$r "$TOKEN_ADMIN" | cut -d' ' -f2)"; done; echo
done
grep -c -E ' ERROR ' "$LOG" | xargs -I{} echo "== líneas ERROR en el log del servidor: {}"
