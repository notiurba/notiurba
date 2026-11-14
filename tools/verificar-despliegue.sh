#!/usr/bin/env bash
# Revisa lo que se puede comprobar sin iniciar sesión: salud del servidor, CORS y que el sitio cargue.
# No usa secretos. Uso: tools/verificar-despliegue.sh [API] [SITIO] [VISTA_PREVIA]
set -u

API="${1:-https://notiurba-api.onrender.com}"
SITIO="${2:-https://notiurba.netlify.app}"
VISTA_PREVIA="${3:-https://deploy-preview-1--notiurba.netlify.app}"
fallos=0

ok()    { echo "  ok    $1"; }
falla() { echo "  FALLA $1"; fallos=$((fallos + 1)); }

echo "Salud ($API/api/health, puede tardar hasta 3 min si el servidor dormía)"
inicio=$(date +%s)
respuesta=$(curl -sS -m 180 -w '\n%{http_code}' "$API/api/health" 2>/dev/null)
estado=$?
duracion=$(( $(date +%s) - inicio ))
codigo=$(echo "$respuesta" | tail -n1)
if [ "$estado" -eq 0 ] && [ "$codigo" = "200" ] && echo "$respuesta" | grep -Eq '"baseDeDatos" *: *"ok"'; then
  ok "responde 200 con la base de datos en ok (${duracion} s)"
else
  falla "curl terminó con $estado, código $codigo (${duracion} s)"
fi

# Imprime la cabecera Access-Control-Allow-Origin de la respuesta; vacío si no hay o si curl falló.
cabecera_cors() {
  curl -sS -m 60 -i -X OPTIONS "$API/api/auth/login" \
    -H "Origin: $1" -H "Access-Control-Request-Method: POST" 2>/dev/null |
    tr -d '\r' | awk -F': ' 'tolower($1)=="access-control-allow-origin" {print $2}'
}

echo "CORS"
for origen in "$SITIO" "$VISTA_PREVIA"; do
  if [ "$(cabecera_cors "$origen")" = "$origen" ]; then ok "acepta $origen"; else falla "no acepta $origen"; fi
done
# «Rechaza» solo cuenta si el servidor respondió (con el servidor caído no hay cabecera y daría un falso ok).
ajeno="https://sitio-ajeno.example"
codigo=$(curl -sS -m 60 -o /dev/null -w '%{http_code}' -X OPTIONS "$API/api/auth/login" \
  -H "Origin: $ajeno" -H "Access-Control-Request-Method: POST" 2>/dev/null)
if [ -z "$codigo" ] || [ "$codigo" = "000" ]; then
  falla "no se pudo comprobar el sitio ajeno (el servidor no respondió)"
elif [ "$(cabecera_cors "$ajeno")" = "$ajeno" ]; then
  falla "acepta un sitio ajeno"
else
  ok "rechaza un sitio ajeno (respondió $codigo)"
fi

echo "Sitio"
pagina=$(curl -sS -m 60 -w '\n%{http_code}' "$SITIO/app/estado" 2>/dev/null)
codigo=$(echo "$pagina" | tail -n1)
if [ "$codigo" = "200" ] && echo "$pagina" | grep -q 'id="root"'; then
  ok "$SITIO/app/estado carga la aplicación"
else
  falla "$SITIO/app/estado dio $codigo o no trae la aplicación"
fi

echo
if [ "$fallos" -eq 0 ]; then echo "Todo en orden."; else echo "$fallos comprobación(es) fallaron."; exit 1; fi
