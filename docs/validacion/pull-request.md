Se retoma EDT 5.1 sin perder el trabajo anterior. Render declara contexto backend explícito; el perfil validation valida el esquema sin migrarlo; se añaden mapeo Point/MultiPolygon, acceso Spring Data JPA de solo lectura y una migración PostgreSQL 18 desechable con Testcontainers.

Surefire ejecuta pruebas normales sin credenciales; Failsafe selecciona integraciones mediante perfiles spatial-tests, cloudinary-tests y migration-tests. Actions separa unitarias y migraciones, con informes de fallos. Cloudinary mantiene almacenamiento authenticated y firma SHA-256 comprobada localmente. V1–V4 y el frontend permanecen intactos.

Ejecutado: 12 comprobaciones JPEG, 8 firmas y 18 configuración/rutas aprobadas. mvn clean test y mvn package intentados y bloqueados antes de compilar por DNS. Integraciones de Spring, migraciones, Render/latencia y Cloudinary real siguen pendientes. R2 externo continúa no concluyente por DNS.

Estado de publicación: rama remota creada desde main 2d6ceca; render.yaml publicado en 9bd1627. Los demás cambios todavía están locales. El conector devuelve 403; la extensión no permite cargas locales y el control del navegador se detuvo por no verificar con suficiente confianza la URL de Windows. Este texto es una descripción preparada; no existe PR remoto.

Antes de publicar el PR, integrar el parche en un clon auténtico con fetch operativo y revisar el diff. No subir el historial reconstruido local ni considerar un CI verde sin integraciones como aprobación de proveedores.
