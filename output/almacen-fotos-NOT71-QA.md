# Continuar NU-000001 con archivos locales aislados

El 8 de noviembre se creó `NU-000001` mediante Chrome en QA: categoría 1, ubicación de prueba `(4.084847878371718, -76.1953353881836)`, barrio Centro, comuna derivada 4 y destino Alcaldía de Tuluá / Secretaría de Hábitat e Infraestructura. La subida real por HTTP de `foto-sintetica-NOT71.png` devolvió 502 `ALMACEN_FOTOS_NO_DISPONIBLE`; no se declaró aprobada. El reporte se consulta con HTTP 200 y conserva `fotos: []`.

El servidor local previo de NOT-25 (19025/19026) está detenido. Su harness no debe reutilizarse sin cambios: fija otra base y siembra un usuario/barrio adicionales. Se preparó `backend/src/test/java/com/notiurba/qa/Not71ServidorQA.java`, que reutiliza el contrato real `AlmacenFotos` con archivo local y servidor HTTP de lectura. No intercepta respuestas Spring, no activa Cloudinary y no cambia código de producción.

## Actualizacion del launcher (8 de noviembre, posterior al fallo de arranque)

Se corrigio el fallo reproducido de JdbcTemplate causado por cargar todas las clases test y se agrego captura real de stdout/stderr y logging temprano de Spring. El launcher ya no termina solo en el arranque: continua automaticamente con la foto publica sobre NU-000001, su URL/SHA256 y la consulta SQL de solo lectura. No requiere un segundo comando de subida. Informe y limites reales: `docs/validacion/2026-11-08-NOT-71-arranque-fotos-QA.md`. Los pasos manuales inferiores se conservan como referencia historica; todavia no se acredita una ejecucion exitosa de la foto local.

## Arranque cuando Docker sea accesible

Desde PowerShell 7 de Windows:

```powershell
& 'C:\notiurba\noti-urba\output\iniciar-fotos-NOT71-QA.ps1'
```

Requiere el harness compilado por Maven `test-compile`, el JAR local existente y el contenedor QA actual. Se ejecutó compilación de prueba en esta revisión; revisar su resultado en el informe. El launcher se detiene si la clase falta o es anterior al fuente.

El launcher verifica mapeo 127.0.0.1:15435 e identidad SQL y lee en memoria solamente la clave de ese contenedor QA, sin imprimirla ni guardarla. No usa `.env`, credenciales productivas o una conexión Neon. No crea bases, no registra usuarios y no aplica migraciones. Conserva 5175/18091 y todos los contenedores existentes.

Puertos nuevos: frontend `http://127.0.0.1:5176/login`, backend `127.0.0.1:18092`, archivos `127.0.0.1:19027/fotos/`. Los tres se enlazan a loopback. Archivos y logs quedan en una carpeta temporal exclusiva `not71-fotos-*`; no se borran automáticamente. JWT nuevo generado en memoria, configuración fija de base QA, `ddl-auto=validate`, Flyway apagado, cola de correo apagada y proveedores externos sin credenciales. El código de pruebas no forma parte del JAR productivo.

## Validación que sigue pendiente

1. Iniciar sesión con la **misma cuenta sintética ya verificada** en el backend nuevo. No repetir registro, barrios o verificación de correo.
2. Reintentar solo la fotografía sobre `NU-000001`, sin crear otro reporte. La galería de la extensión Chrome requiere permiso para archivos locales; en esta sesión ese permiso bloqueó `setFiles`. No se cambió. Puede seleccionarse manualmente en Chrome o validarse el POST multipart real por HTTP, distinguiendo este alcance de la selección visual.
3. Exigir HTTP 201 del POST `/api/reportes/NU-000001/fotos`, GET del mismo reporte con una foto asociada y URL local `19027`. Abrir la URL y comparar hash/bytes con el archivo sintético enviado. El mero arranque del adaptador no aprueba una subida.
4. Consultar persistencia sin escribir:

```powershell
& 'C:\notiurba\noti-urba\output\consultar-reporte-NOT71-QA.ps1'
```

El script de lectura devuelve código, usuario, categoría, coordenadas, comuna/origen, entidad, destino, cola y fotografías del caso exacto, sin contraseñas/JWT/tokens. La URL requiere además la prueba HTTP anterior.

5. No procesar la cola pendiente ni activar envíos. La ruta contiene direcciones oficiales por ser el catálogo del producto, pero este ensayo no autoriza correos a entidades.

La sesión actual no puede arrancar este adaptador ni ejecutar el SQL por acceso denegado al motor Docker. Debe distinguirse lo preparado/compilado de lo ejecutado. No se modifica V17 ni se cierran B01–B05. NOT-71 requiere por separado la E2E desplegada Netlify/Render/Neon.
