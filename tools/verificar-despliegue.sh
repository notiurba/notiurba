#!/usr/bin/env bash
# Revisa lo que se puede comprobar sin iniciar sesión: salud del servidor y CORS.
# No usa secretos. Uso: tools/verificar-despliegue.sh [API] [SITIO]
set -u

API="${1:-https://notiurba-api.onrender.com}"
SITIO="${2:-https://notiurba.netlify.app}"
VISTA_PREVIA="${3:-https://deploy-preview-1--notiurba.netlify.app}"
fallos=0

ok()    { echo "  ok    $1"; }
falla() { echo "  FALLA $1"; fallos=$((fallos + 1)); }

echo "Salud ($API/api/health, puede tardar hasta 3 min si el servidor dormía)"
inicio=$(date +%s)
cuerpo=$(curl -sS -m 180 -w '\n%{http_code}' "$API/api/health" 2>&1)
codigo=$(echo "$cuerpo" | tail -n1)
duracion=$(( $(date +%s) - inicio ))
if [ "$codigo" = "200" ] && echo "$cuerpo" | grep -q '"baseDeDatos":"ok"'; then
  ok "responde 200 con la base de datos en ok (${duracion} s)"
else
  falla "código $codigo en ${duracion} s"
fi

cors() {
  local origen="$1" permitido
  permitido=$(curl -sS -m 60 -i -X OPTIONS "$API/api/auth/login" \
    -H "Origin: $origen" -H "Access-Control-Request-Method: POST" 2>/dev/null |
    tr -d '\r' | awk -F': ' 'tolower($1)=="access-control-allow-origin" {print $2}')
  [ "$permitido" = "$origen" ]
}

echo "CORS"
cors "$SITIO" && ok "acepta $SITIO" || falla "no acepta $SITIO"
cors "$VISTA_PREVIA" && ok "acepta las vistas previas ($VISTA_PREVIA)" || falla "no acepta las vistas previas"
cors "https://sitio-ajeno.example" && falla "acepta un sitio ajeno" || ok "rechaza un sitio ajeno"

echo "Sitio"
codigo=$(curl -sS -m 60 -o /dev/null -w '%{http_code}' "$SITIO/app/estado")
[ "$codigo" = "200" ] && ok "$SITIO/app/estado responde 200 (redirección de la SPA)" || falla "$SITIO/app/estado dio $codigo"

echo
if [ "$fallos" -eq 0 ]; then echo "Todo en orden."; else echo "$fallos comprobación(es) fallaron."; exit 1; fi
