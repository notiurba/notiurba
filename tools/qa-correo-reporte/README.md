# QA de punta a punta del mensaje a la entidad

Prueba las rutas con canales, `POST /api/reportes/vista-previa`, `GET /api/reportes/{codigo}/mensaje` y el
correo encolado contra el **backend real** (el jar) y una base PostGIS **desechable**. No toca la base de
desarrollo ni producción, y **no envía correos**: el envío queda apagado y el mensaje solo se encola.

## Qué hace

1. Crea una base vacía en el contenedor `notiurba-db` y arranca `backend/target/*.jar` en el puerto 8090.
2. Siembra dos ciudadanos e inicia sesión con los dos.
3. Revisa las 14 rutas y sus canales, la vista previa (y que no escriba en la base), sus errores (400, 401,
   422) y CORS.
4. Crea un reporte de vías y uno de acueducto, los encola y compara la vista previa, `GET /mensaje` y el
   `mensaje_json` guardado. Un usuario ajeno recibe 404.
5. Apaga el servidor y borra la base, aunque algo falle.

## Cómo se corre

```bash
cd backend && mvn -q -DskipTests package && cd ..
tools/qa-correo-reporte/qa-correo.sh
```

Requisitos: los mismos de `tools/qa-panel-admin` (Docker con `notiurba-db`, `.env`, `curl`, `openssl` y
`python3`). Resultado esperado: todas las verificaciones en verde.
