# QA de punta a punta del panel: mapa, calor y estadísticas

Prueba `GET /api/admin/mapa`, `/mapa/calor`, `/estadisticas` y el listado del panel contra el
**backend real** (el jar, no MockMvc) y una base PostGIS **desechable**. No toca la base de desarrollo
ni producción.

## Qué hace

1. Crea una base vacía en el contenedor `notiurba-db` (puerto 5433, `docker compose up -d`).
2. Arranca `backend/target/*.jar` en el puerto 8089 con las variables del `.env`, apuntando a esa base
   (Flyway la migra) y con el envío de correos apagado.
3. Siembra una ciudadana y 13 reportes: vigentes en 6 comunas, vencidos, uno sin ubicación y uno sin comuna.
4. Inicia sesión como administrador y como ciudadana y llama a las rutas.
5. Verifica seguridad (401/403 reales), validación (400 con su campo), CORS, que los totales cuadren entre
   sí y con la base (vista `v_reporte_mapa`), filtros, orden, paginación y tiempos.
6. Apaga el servidor y borra la base, aunque algo falle.

No imprime contraseñas ni tokens. Las respuestas y el log quedan en una carpeta temporal.

## Cómo se corre

```bash
cd backend && mvn -q -DskipTests package && cd ..
tools/qa-panel-admin/qa-panel.sh
```

Requisitos: Docker con `notiurba-db` arriba, `.env` con `ADMIN_DEV_PASSWORD`, `JWT_SECRET`,
`GOOGLE_CLIENT_ID` y `CORS_ORIGINS`, y `curl`, `openssl` y `python3`. Resultado esperado:
`44 de 44 verificaciones en verde`.
