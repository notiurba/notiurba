#!/bin/bash
# QA de punta a punta del mensaje a la entidad: rutas con canales, vista previa y correo encolado.
set -u
REPO=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
QA=$(cd "$(dirname "$0")" && pwd)
SALIDA=$(mktemp -d)
cd "$REPO"
set -a; source .env; set +a
BASE="notiurba_e2e_$(date +%s)"
PUERTO=8090
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
# El envío queda apagado: el correo se encola pero no sale a ningún lado.
export DB_URL="jdbc:postgresql://localhost:5433/$BASE" PORT=$PUERTO NOTIURBA_ENVIO_REPORTES_HABILITADO=false
JAR=$(ls backend/target/*.jar | grep -v original | head -1)
java -jar "$JAR" > "$LOG" 2>&1 &
PID=$!
for i in $(seq 1 90); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' $API/api/health)" = "200" ] && break; sleep 1
done
echo "== servidor arriba en ${i}s (pid $PID), base $BASE"

CLAVE="Qa$(openssl rand -hex 8)1"
sql "$BASE" <<SQL
INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio, direccion_correspondencia,
                     correo, correo_verificado_en, password_hash, id_rol, id_aviso_privacidad, acepto_privacidad_en)
SELECT n, 'Prueba', 1, d, (SELECT min(id_barrio) FROM barrio WHERE id_comuna = 1), 'Cra 1 # 2-3',
       c, now(), crypt('$CLAVE', gen_salt('bf')), 1, 1, now()
  FROM (VALUES ('Ana', '99000111', 'ana.qa@prueba.local'), ('Beto', '99000222', 'beto.qa@prueba.local')) v(n, d, c);
SQL
PUNTO=$(echo "SELECT ST_Y(p) || ' ' || ST_X(p) FROM (SELECT ST_PointOnSurface(geom) p FROM comuna WHERE id_comuna = 5) s;" | sql "$BASE")
read -r LAT LON <<< "$PUNTO"
BARRIO=$(echo "SELECT min(id_barrio) FROM barrio WHERE id_comuna = 1 AND NOT interno;" | sql "$BASE")

login() { curl -s -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"correo\":\"$1\",\"password\":\"$2\"}" | python3 -I -c 'import json,sys; print(json.load(sys.stdin).get("token",""))'; }
TOKEN=$(login ana.qa@prueba.local "$CLAVE")
TOKEN_BETO=$(login beto.qa@prueba.local "$CLAVE")
[ -n "$TOKEN" ] && [ -n "$TOKEN_BETO" ] && echo "== login: ok" || echo "== login: FALLA"

mkdir -p "$SALIDA/respuestas"
: > "$SALIDA/respuestas/_http.txt"
llamar() { # nombre método ruta token [cuerpo]
  local auth=(); [ -n "$4" ] && auth=(-H "Authorization: Bearer $4")
  local datos=(); [ -n "${5:-}" ] && datos=(-H 'Content-Type: application/json' -d "$5")
  echo "$1 $(curl -s -o "$SALIDA/respuestas/$1.json" -w '%{http_code} %{time_total}' -X "$2" \
    ${auth[@]+"${auth[@]}"} ${datos[@]+"${datos[@]}"} "$API$3")" >> "$SALIDA/respuestas/_http.txt"
}
contar() { echo "SELECT (SELECT count(*) FROM reporte) || ' ' || (SELECT count(*) FROM registro_envio_correo);" | sql "$BASE"; }

VIAS="{\"idCategoria\":1,\"latitud\":$LAT,\"longitud\":$LON,\"descripcion\":\"Hay un hueco enorme frente al parque\",\"totalFotos\":2}"
llamar rutas GET /api/enrutamiento/categorias "$TOKEN"
ANTES=$(contar)
llamar previa POST /api/reportes/vista-previa "$TOKEN" "$VIAS"
llamar previa_aseo POST /api/reportes/vista-previa "$TOKEN" "{\"idCategoria\":14,\"idBarrio\":$BARRIO,\"descripcion\":\"No pasó el carro de la basura\"}"
llamar previa_corta POST /api/reportes/vista-previa "$TOKEN" '{"idCategoria":6,"descripcion":"corta"}'
llamar previa_sin_lugar POST /api/reportes/vista-previa "$TOKEN" '{"idCategoria":1,"descripcion":"Hay un hueco enorme frente al parque"}'
llamar previa_sin_fotos POST /api/reportes/vista-previa "$TOKEN" "${VIAS%,\"totalFotos\":2\}}}"
llamar previa_fuera POST /api/reportes/vista-previa "$TOKEN" '{"idCategoria":1,"latitud":4.6,"longitud":-74.08,"descripcion":"Hay un hueco enorme frente al parque"}'
llamar previa_sin_token POST /api/reportes/vista-previa "" "$VIAS"
DESPUES=$(contar)

llamar crear_vias POST /api/reportes "$TOKEN" "${VIAS%,\"totalFotos\":2\}},\"autorizaEnvio\":true}"
COD_VIAS=$(python3 -I -c 'import json,sys; print(json.load(open(sys.argv[1])).get("codigoSeguimiento",""))' "$SALIDA/respuestas/crear_vias.json")
llamar mensaje_antes GET "/api/reportes/$COD_VIAS/mensaje" "$TOKEN"
llamar enviar_vias POST "/api/reportes/$COD_VIAS/enviar" "$TOKEN"
llamar mensaje_despues GET "/api/reportes/$COD_VIAS/mensaje" "$TOKEN"
llamar mensaje_ajeno GET "/api/reportes/$COD_VIAS/mensaje" "$TOKEN_BETO"
llamar mensaje_mal_escrito GET /api/reportes/abc/mensaje "$TOKEN"

llamar crear_agua POST /api/reportes "$TOKEN" "{\"idCategoria\":11,\"idBarrio\":$BARRIO,\"descripcion\":\"No llega el agua desde ayer\",\"matriculaCuentaServicio\":\"778899\",\"autorizaEnvio\":true}"
COD_AGUA=$(python3 -I -c 'import json,sys; print(json.load(open(sys.argv[1])).get("codigoSeguimiento",""))' "$SALIDA/respuestas/crear_agua.json")
llamar enviar_agua POST "/api/reportes/$COD_AGUA/enviar" "$TOKEN"

ENCOLADOS=$(sql "$BASE" <<SQL
SELECT json_object_agg(r.codigo_seguimiento, json_build_object('destino', e.destino, 'canal', e.id_canal_atencion,
       'modo', m.nombre, 'mensaje', e.mensaje_json::json))
  FROM registro_envio_correo e JOIN reporte r USING (id_reporte) JOIN modo_envio m USING (id_modo_envio);
SQL
)
CORS=$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS "$API/api/reportes/vista-previa" \
  -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: authorization,content-type')

export SALIDA ANTES DESPUES ENCOLADOS COD_VIAS COD_AGUA CORS
python3 -I "$QA/verificar.py"
grep -c -E ' ERROR ' "$LOG" | xargs -I{} echo "== líneas ERROR en el log del servidor: {}"
