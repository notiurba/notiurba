# Revision Cloudinary authenticated - continuacion de EDT 5.1 v3

Fecha de revision: 27-10-2026. Las marcas de ejecucion siguientes estan en UTC.
Base local conservada: e4abfb475c86674b2f299812cde8c58cb9545c47 (v3). Rama local codex/edt-5-1-validaciones; historial reconstruido que NO debe enviarse con push.

## Hallazgo y contraste oficial

La sospecha es razonable, pero no se ha demostrado un fallo remoto de la v3. La guia REST Customizing uploads muestra `/image/authenticated`; la tabla de Upload API afirma que `type` se incorpora a la ruta en REST. Sin embargo, el SDK oficial Java 2.3.1 implementa `Uploader.upload` llamando a la accion `upload`, `Util.buildUploadParams` incluye `type` y `Cloudinary.cloudinaryApiUrl` construye `/image/upload`. Su metodo destroy tambien transmite type en los parametros, y privateDownload construye `/image/download` con type, expires_at y timestamp firmados. La v3 se parece a ese contrato del SDK, por lo que no se sustituyo a ciegas toda ruta por authenticated ni se elimino type de todas las firmas.

Se consultaron tanto documentacion oficial como codigo del SDK publicado. Las dos presentaciones difieren; solo la prueba contra el proveedor puede confirmar el comportamiento efectivo del entorno del proyecto. Se elimina la implementacion manual de peticiones en CloudinaryPhotoStore y se delega al SDK oficial para reducir el mantenimiento de rutas, serializacion y firmas.

SDK fijado: `com.cloudinary:cloudinary-http5:2.3.1`, release oficial, codigo revisado del commit 4d2e62b (no se afirma que sea la ultima version). Resolucion y compatibilidad binaria local permanecen pendientes porque Maven no puede obtener siquiera el parent Spring Boot. No se cambio la version de Spring ni las migraciones.

Fuentes primarias consultadas el 27-10-2026:
- https://cloudinary.com/documentation/upload_parameters#authenticated_assets
- https://cloudinary.com/documentation/image_upload_api_reference
- https://cloudinary.com/documentation/authentication_signatures
- https://cloudinary.com/documentation/control_access_to_media
- https://cloudinary.com/documentation/java_integration
- https://github.com/cloudinary/cloudinary_java/releases
- https://github.com/cloudinary/cloudinary_java/blob/4d2e62b/cloudinary-core/src/main/java/com/cloudinary/Uploader.java
- https://github.com/cloudinary/cloudinary_java/blob/4d2e62b/cloudinary-core/src/main/java/com/cloudinary/Util.java
- https://github.com/cloudinary/cloudinary_java/blob/4d2e62b/cloudinary-core/src/main/java/com/cloudinary/Cloudinary.java
- https://github.com/cloudinary/cloudinary_java/blob/4d2e62b/cloudinary-http5/src/main/java/com/cloudinary/http5/UploaderStrategy.java

## Implementacion

- Carga: JPEG validado y recodificado antes de enviar; ID UUID4 bajo notiurba/edt51; uploader.upload con resource_type=image, type=authenticated, overwrite=false, allowed_formats=[jpg], backup=false.
- Se comprueban public_id, resource_type, type y format de la respuesta. Una respuesta publica o incompleta nunca se acepta como exito. Si la carga queda incierta o la respuesta contradice la privacidad se intenta limpieza compensatoria exclusivamente del UUID generado en esa llamada. Si falla la limpieza, se registra solo ese identificador y su tipo para permitir retirar el sintetico; no se registra la respuesta ni credenciales.
- Recuperacion: privateDownload del SDK, type=authenticated, format=jpg, attachment=false, expires_at=ahora+60s; se exige HTTPS y api.cloudinary.com. No se sustituye por una firma CDN sin expiracion.
- Eliminacion: uploader.destroy con el mismo public_id, resource_type=image, type=authenticated e invalidate=true; solo ok/not found confirman la operacion. No se promete invalidacion instantanea de caches; la prueba de ausencia usa Admin API y el endpoint download, no una suposicion sobre el CDN.
- El controlador convierte errores de generacion del enlace en 502 generico. SDK y HttpClient no propagan mensajes o causas con URLs firmadas a los reportes. Los diagnosticos siguen desactivados por defecto y requieren ADMIN.
- CloudinarySignature permanece para vectores de prueba locales y comparacion contra el SDK. Ya no firma peticiones de produccion ni decide rutas en CloudinaryPhotoStore.

## Pruebas anadidas y ampliadas

CloudinaryPhotoStoreTest: ocho pruebas JUnit de contrato y errores, incluyendo opciones authenticated, sanitizacion antes de subir, rechazo local sin invocar proveedor, compensacion por carga incierta o tipo publico, eliminacion idempotente, TTL60s, errores sin datos sensibles y generacion real del SDK sin red. Son pruebas locales; no certifican almacenamiento remoto.

DiagnosticsSecurityTest: nuevo caso de error del proveedor al generar enlace, con respuesta 502 generica.

CloudinaryIntegrationTest: prueba optativa sin mocks del almacen. Crea exclusivamente JPEG sintetico 32x24, sube mediante Spring MVC con ADMIN y consulta de forma independiente Admin API por el public_id exacto. Exige type=authenticated, resource_type=image, jpg, dimensiones y bytes <=1MB. Un cliente HTTP sin cookies ni Authorization debe obtener el JPEG mediante el enlace HTTPS temporal y decodificarlo; el enlace debe declarar entre1 y60 segundos restantes. Exige rechazo del enlace correctamente firmado pero ya vencido, firma alterada, URL authenticated sin firma, ruta publica upload y variante transformada sin firma. El finally elimina el sintetico; despues exige NotFound de Admin API y rechazo de download. Un error de red o autenticacion no se confunde con ausencia confirmada.

La prueba del enlace vencido usa expires_at ya pasado con firma valida; no simula transcurso de60s de un enlace previamente usado. El navegador sin sesion sigue siendo una comprobacion separada de C03. No se ha ejecutado ninguna de estas comprobaciones remotas en este entorno.

## Resultado de ejecucion real

| Comprobacion | Resultado | Estado |
|---|---|---|
| Contraste con documentacion y fuente oficial | Realizado; discrepancia de presentacion REST/SDK identificada | Revision completada |
| Validador JPEG Java21 | 12 aserciones, cero fallos; 2026-10-27T10:52:40.628535700Z | APROBADO, alcance local |
| Maven clean test | Salida1 antes de compilar; parent Boot3.3.5 no descargable | PENDIENTE |
| Maven package | Salida1 antes de compilar; mismo bloqueo | PENDIENTE |
| Maven -Pcloudinary-tests verify | Intentado; salida1 antes de arrancar JUnit/Spring | PENDIENTE |
| Almacenamiento authenticated real | No ejecutado | PENDIENTE C02 |
| Denegacion anonima, enlace valido/expirado, borrado remoto | No ejecutado | PENDIENTE C02/C03 |
| JUnit SDK/MVC nuevos | No ejecutados | PENDIENTE C04 |

Evidencia: `logs/cloudinary-review/`, con comandos y UTC en los tres logs Maven, baseline previo, preflight y salida del validador. La resolucion DNS ahora funciona para api.cloudinary.com y repo.maven.apache.org. El error actual de Maven es **Permission denied: getsockopt**, no DNS: la conexion se bloquea antes de descargar dependencias. No se desactivo TLS ni se modificaron proxy, certificados, reglas o permisos.

CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET y CLOUDINARY_URL estan ausentes del proceso. El conector Cloudinary conectado permite operaciones propias, pero no ofrece entrega de credenciales al proceso Java; usarlo no demostraria Spring Boot. No se extrajeron secretos del navegador ni de archivos privados. No hubo cargas ni recursos remotos nuevos que limpiar durante esta revision.

## Reproduccion autorizada

En una maquina con Java21, Maven y acceso HTTPS a Maven Central/Cloudinary, inyectar CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET desde el almacen autorizado del equipo. La clave debe permitir cargar, consultar metadata del recurso y eliminar solamente los sinteticos de la prueba. No pegar sus valores en comandos guardados, informes o el repositorio.

```powershell
mvn -B -f backend/pom.xml clean test
mvn -B -f backend/pom.xml package
mvn -B -f backend/pom.xml -Pcloudinary-tests verify
```

El perfil externo es explicito y falla si faltan secretos; no omite el test para aparentar un aprobado. Conservar Surefire/Failsafe y verificar que la prueba se ejecuto con cero errores. Las salidas con UUID sintetico y type son evidencia admisible; no guardar las URLs firmadas. Un resultado local con mocks o la mera existencia de estos archivos no aprueba C02/C03/C04.
Comprobacion adicional: logs/cloudinary-review/java-syntax.log registra parseo Java21 de cinco fuentes, cero errores de sintaxis, sin comprobacion de tipos. logs/cloudinary-review/https-probe.log registra un intento HEAD sin credenciales ni imagen; su resultado es solo conectividad, nunca una integracion aprobada.

Para incorporar sobre una copia que ya contiene v3, revisar y aplicar NotiUrba-Cloudinary-review.patch usando primero git apply --check. El parche completo NotiUrba-EDT51-v3-cloudinary.patch parte de la base original del backend y sirve para la integracion en un clon autentico, con --exclude=render.yaml cuando ese archivo ya este publicado. No enviar el historial local reconstruido. El ZIP actualizado es una copia parcial del backend/configuracion/evidencia; conserva el frontend del repositorio real al aplicar el parche.
## Correccion puntual SHA256 del SDK Java 2.3.1

2026-10-27 UTC. Se comprobo en Configuration.java del commit oficial 4d2e62b que la configuracion utiliza SignatureAlgorithm.valueOf; SignatureAlgorithm.java define exactamente SHA1 y SHA256. El valor sha256 introducido en la migracion al SDK era incorrecto. Se reemplazo exclusivamente ese literal en CloudinaryPhotoStore, CloudinaryIntegrationTest y CloudinaryPhotoStoreTest. No se cambia la ruta, el tipo authenticated ni la logica de las operaciones.

El parche NotiUrba-Cloudinary-SHA256.patch contiene tres sustituciones; git apply --check contra los originales del commit d0e39bb y git apply --reverse --check contra los archivos corregidos pasaron. No se genero otro ZIP ni se presenta una nueva version funcional aprobada.

Se reintentaron mvn clean test, mvn package y mvn -Pcloudinary-tests verify con backend/pom.xml. Los tres devolvieron1 antes de compilar, por Permission denied: getsockopt al descargar el parent Spring Boot3.3.5. Las variables CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET siguen ausentes; no se mostraron valores. Evidencia con UTC y comandos: logs/cloudinary-sha256-fix/. JUnit no arranco, no hubo cargas remotas y C02/C03/C04 siguen PENDIENTES. La comprobacion de aplicacion del parche no acredita inicializacion del SDK ni una integracion real.

Fuentes oficiales:
- https://github.com/cloudinary/cloudinary_java/blob/4d2e62b/cloudinary-core/src/main/java/com/cloudinary/Configuration.java
- https://github.com/cloudinary/cloudinary_java/blob/4d2e62b/cloudinary-core/src/main/java/com/cloudinary/SignatureAlgorithm.java