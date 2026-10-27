# Cierre tecnico parcial de las integraciones externas

Se verificaron los logs reales de Actions40518412910, commit f38ad7c, rama feature/edt-5.1-technical-validation:25 unitarias/MVC y2 pruebas de migracion aprobadas. Incluyen arranque validation HTTP200, Flyway disabled/readOnly true sin DB_PASSWORD/ADMIN_DEV_PASSWORD. Los jobs Neon y Cloudinary fueron SKIPPED; no se aprueban por el CI verde.

| Validacion | Estado actual |
| --- | --- |
| B01/B02/N04/S02/C04 | APROBADO en f38ad7c; ver logs/cierre-externo/ci-verificado.json |
| N02/N03 | PENDIENTE: solo existe production en Neon; falta entorno aislado autorizado y credenciales lector |
| C02/C03 | PENDIENTE: faltan credenciales de prueba; no se genero enlace ni prueba de navegador |
| O02/O03 | NO CONCLUYENTE historico: falta cliente S3 binario y permisos/autorizacion temporal |
| R01/R02/R03 | PENDIENTE: Docker/WSL bloqueados; sin servicio Render aislado/URL ni mediciones |
| G01 | Publicacion previa f38ad7c aprobada; esta actualizacion pendiente de publicar |

Evidencias nuevas en logs/cierre-externo/: CSV previo conservado intacto, ci-verificado.json, entorno-y-limpieza.json, docker-build.log y runner-regression.log. El CSV vigente incluye fecha UTC, comando realmente ejecutado, ambiente, resultado, problema, solucion y pendientes por ID. Los logs y secciones historicos no se reescriben.

Neon: organizacion Free y1 proyecto verificados. Solo rama production; no se consultaron sus datos ni credenciales. La pagina oficial neon.com/pricing incluye100 proyectos Free. Propuesta pendiente de autorizacion: proyecto VACIO NotiUrba-EDT51-isolated, PostgreSQL18, AWS us-east-2, sin copiar production y sin upgrade/pago. No se creo el recurso. V1-V4 se aprovisionarian solo alli y despues se usaria un lector con CONNECT/USAGE/SELECT.

Cloudinary: busqueda del prefijo notiurba/edt51/ encontro0 recursos. No se crearon fotografias ni enlaces nuevos. R2: GET buckets HTTP200 confirma ausente el temporal anterior; el bucket test conservado tiene r2.dev desactivado y0 dominios personalizados. No se altero ese bucket ni se afirma un inventario exhaustivo de otros recursos.

Docker Desktop instalado: arranque normal solicitado. WSL devolvio E_ACCESSDENIED; docker build devolvio1 por acceso denegado a buildx .lock. No llego a construirse ni arrancar la imagen. No se cambiaron permisos para eludirlo. Render: sin conector/API/URL de servicio aislado,0 solicitudes y0 muestras de latencia.

Correccion del runner: pruebas ejecutadas con fallos/errores se clasifican FALLIDO; ejecucion ausente/omitida sigue PENDIENTE. Doce controles locales pasaron, sin llamadas a proveedores. Esto no acredita las integraciones externas ni demuestra defectos del backend.

Disponibilidad manual de Actions NO CONCLUYENTE: gh no tiene autenticacion y el conector no permite el endpoint de workflows/dispatch/Secrets. La documentacion oficial contempla despacho por API/CLI de un workflow ya ejecutado; no se declara imposible solo porque no esta en main. No se modifico main/default branch. Usar el runner local autorizado si no hay dispatch disponible.

Fuentes oficiales: https://neon.com/pricing y https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_dispatch .

Las secciones anteriores quedan conservadas debajo como historial; este corte las actualiza. No se declara EDT5.1 terminada.

---

## Corte vigente: integraciones externas, 2026-10-27 UTC

Se consultaron los logs reales de [Actions40507497490](https://github.com/notiurba/notiurba/actions/runs/40507497490), commit a599593: backend-unit y migration-postgres18 SUCCESS. Se preservan B01/B02/N04 aprobados:25 pruebas unitarias/MVC,0 fallos/errores/skipped; JAR generado;1 MigrationIntegrationTest exitosa. PostgreSQL18.6 Debian/PostGIS3.6.4 del contenedor, distinto de acreditar Neon. SDK Cloudinary2.3.2 y SHA256 conservados. Evidencia filtrada: logs/external-config/confirmed-actions.json.

| Validaciones | Estado vigente y alcance |
| --- | --- |
| B01/B02, S02/C04 | APROBADO en a599593: Maven y seguridad MVC; no proveedores |
| N04 | APROBADO en a599593: migracion real en contenedor; nueva prueba de perfil validation aun pendiente |
| N02/N03 | PENDIENTE: Neon solo tiene production; falta base/branch aislada, usuario lector y secretos autorizados |
| C02/C03 | PENDIENTE: no credenciales de proceso; no llamadas Cloudinary ni navegador |
| O02/O03 | NO CONCLUYENTE historico conservado; sin credenciales S3/canal binario no se repiten recursos |
| R01/R02/R03 | PENDIENTE: motor Docker inactivo, servicio aislado/URL/permiso Render no disponibles |
| H01-H03, N01/C01/O01/O04 | Se conservan las evidencias historicas y su alcance original |
| Configuracion local |27 comprobaciones YAML/rutas y8 controles de seguridad de runner/reportes aprobados; cero llamadas a proveedores |

Configuracion nueva: workflow_dispatch con seleccion de proveedor, jobs externos independientes limitados a ref autorizada, Environment edt51-external-validation y permisos contents:read. Ningun push/PR llama a proveedores. Los jobs existentes se conservan. Reportes externos exportados con lista blanca, sin properties/stdout/stderr/trazas ni enlaces firmados. El runner contempla detached HEAD de Actions, rechaza credenciales incompletas y TLS inseguro antes de Maven. La prueba espacial ahora arranca SpringBoot completo, exige TLS y transaccion de solo lectura. Hikari fija tambien default_transaction_read_only=on. Cloudinary comprueba ausencia del recurso en finally. Render fija la rama de validaciones, sin despliegue.

No se ejecutaron los perfiles externos con credenciales incompletas. test-compile offline local fallo porque no existe el parent Boot3.3.5 en cache; no sustituye el CI aprobado ni acredita los nuevos cambios. Docker info fallo por pipe linuxEngine ausente. Logs y fechas: logs/external-config/environment.log, offline-guards.log, configuration-checks.log.

Limitacion verificada en documentacion oficial: workflow_dispatch requiere archivo en rama predeterminada; este workflow no existe en origin/main. Se prepara la configuracion solicitada pero no se modifica main para activarla. Si GitHub rechaza el dispatch por ese requisito, usar el runner local seguro. Los secretos de Environment no pudieron listarse porque gh no esta autenticado; no se afirma que no existan en GitHub.

Procedimientos exactos, configuracion segura de Secrets, esquema Neon separado, Windows, R2 binario y condiciones de Render: [INTEGRACIONES-EXTERNAS.md](INTEGRACIONES-EXTERNAS.md). No se crearon recursos externos ni se extrajeron contrasenas de production. Las nuevas pruebas Java no quedan aprobadas hasta recibir ejecucion verificable.

---

# NotiUrba — Validaciones técnicas EDT 5.1

**Corte:** 27 de octubre de 2026, UTC. **Base revisada:** `notiurba/notiurba`, commit `2d6ceca3804ceebe1df95e838ef90bab911bcd9b` de main.
**Rama local inicial:** `codex/edt-5-1-validaciones`. Main remoto no fue modificado.
**Conclusión inicial (ver continuación al final):** hay código y pruebas preparados, pero no se puede certificar la integración de Spring Boot ni el despliegue. Maven no pudo resolver dependencias y GitHub denegó la creación de la rama remota. Un test escrito o un mock no constituye evidencia de integración real.

## 1. Inspección antes de modificar

Se consultó el árbol completo de GitHub y se leyeron el backend completo, POM, configuración, V1–V4, seguridad y contexto/decisiones del proyecto. Se revisaron 67 archivos textuales seleccionados. Se conservan el login, JWT, reglas ADMIN y migraciones existentes.

Estado encontrado:
- Java 21 y Spring Boot 3.3.5; Spring Data JPA, Hibernate Spatial, PostgreSQL JDBC, Flyway PostgreSQL, JWT y Spring Security presentes.
- Login real en `/api/auth/login`, salud en `/api/health` y `/api/admin/ping`. Los demás endpoints requieren sesión.
- No había pruebas del backend, proveedor de fotografías, Dockerfile del backend ni diagnóstico espacial.
- La salud existente solo verifica HTTP; no acredita conexión a PostGIS.
- Inconsistencia de puerto: el valor local por defecto del backend era 5432 y Compose usa 5433. Se alineó a 5433.
- El puerto del servidor estaba fijo en 8080. Ahora respeta PORT.
- V4 crea un administrador de desarrollo mediante un placeholder. No es una política de aprovisionamiento de producción.
- Boot/Flyway de esta versión no se han probado aquí con PostgreSQL 18.6. No se presupone compatibilidad solo porque PostGIS responde.

La descarga Git estándar falló por DNS. Se materializó una copia parcial de trabajo con los archivos del backend y configuración obtenidos del commit fijo. El parche contiene únicamente cambios relativos a esos originales; no borra ni reemplaza el frontend ni archivos ausentes en la copia local.

## 2. Tabla inicial de resultados (corte actualizado en seccion 9 y CSV)

| ID | Prueba / objetivo | Estado | Resultado y evidencia |
|---|---|---|---|
| H01 | Neon Free PostgreSQL/PostGIS previo | Aprobado, antecedente | Se conserva lo informado por el usuario: PostgreSQL 18.6 y PostGIS 3.6.4; corroborado con consulta actual. |
| H02 | Cloudinary: carga JPG, transformación y borrado previos | Aprobado, antecedente | Usuario informó original 864×576, 109669 bytes; transformación 320×213, 19259 bytes; borrado aprobado. No se atribuyen esas pruebas a Spring. |
| H03 | R2: carga, lectura autenticada y borrado previos | Aprobado, antecedente | Se conserva el resultado aportado. No acredita entrega anónima. |
| N01 | Operaciones PostGIS con geometrías sintéticas | Aprobado | 2026-10-27T05:04:47.126Z: interior contains=true, borde contains=false, borde covers=true y GeoJSON Point [-76.195,4.085]. Transporte SQL conectado, no JDBC. |
| N02 | Spring Boot → Neon JDBC/JPA | Pendiente | Prueba automatizada preparada; Maven no compila y no hay TEST_DB_* inyectadas. No se creó ni migró una base compartida. |
| N03 | Hibernate Spatial → JTS MultiPolygon | Pendiente | Entidad inmutable y consulta nativa sintética preparadas; no ejecutadas con Hibernate. |
| N04 | Flyway y Hibernate validate con PostgreSQL 18 | Pendiente | No se ejecutaron V1–V4 en Neon ni se modificaron migraciones. Perfil validation deshabilita DDL/Flyway; no valida migraciones. |
| R01 | Docker y arranque Spring en Render | Pendiente | Dockerfile y render.yaml preparados. No hay conexión Render ni URL confirmada; motor Docker local inactivo. No se desplegó. |
| R02 | Diagnóstico Render HTTP 200 + PostGIS | Pendiente | Endpoint ADMIN preparado, desactivado por defecto. Ningún HTTP real de Spring fue medido. |
| R03 | Latencia antes/después de >15 minutos de reposo | Pendiente | Script PowerShell de dos fases, 5 solicitudes por fase y separación ≥16 minutos. No ejecutado contra Render; no se inventan tiempos. |
| C01 | Validación local JPG / 1 MB / metadatos | Aprobado | Java 21.0.10, 12 comprobaciones, 0 fallos. Última ejecución: 2026-10-27T05:10:42.567167Z; logs/jpeg-java21.log. |
| C02 | Ciclo Cloudinary desde Spring MVC | Pendiente | Adaptador REST, controlador y prueba real optativa preparados. Sin credenciales Cloudinary en el proceso y sin dependencias descargables. |
| C03 | HTTPS en navegador sin sesión | Pendiente | Enlace privado firmado de 60 segundos preparado; no se abrió en navegador. La cuenta conectada no entrega automáticamente credenciales a Java. |
| C04 | Errores HTTP y autorización de fotografías | Pendiente | Pruebas MockMvc escritas, no ejecutadas. Los mocks solo sirven para errores/seguridad, nunca como prueba de proveedor. |
| O01 | R2 nuevo bucket aislado y JPG sintético | Aprobado | Creación 05:07:08.478Z; carga 05:07:31.266Z, HTTP 200, 646 bytes, 32×24. |
| O02 | Lectura autenticada R2 en esta ejecución | No concluyente | HTTP 200; el canal devuelve binarios como texto con caracteres de sustitución. No se verificó igualdad byte a byte de la descarga actual. El antecedente H03 se conserva. |
| O03 | Recuperación pública anónima R2 | No concluyente | URL temporal habilitada HTTP 200. A las 05:07:55.6486153Z el cliente externo falló por DNS; otra herramienta externa tampoco pudo acceder. No hay HTTP 200 externo, Content-Type ni JPEG descargado verificables. |
| O04 | Retirar acceso público y limpiar R2 | Aprobado | enabled=false leído de vuelta, borrado del objeto HTTP 200 y lista vacía. Bucket temporal eliminado HTTP 200; ausencia confirmada por listado. Bucket anterior intacto. |
| S01 | Búsqueda de secretos conocidos | Aprobado, alcance limitado | 67 archivos textuales, cero coincidencias en patrones de alta confianza. No cubre todo el historial, binarios ni secretos de proveedor; no es garantía absoluta. |
| S02 | Autenticación JWT / ADMIN de los diagnósticos | Pendiente | Reutiliza SecurityConfig; pruebas de 401, token inválido/vencido, 403 y ADMIN escritas. No ejecutadas por Maven. |
| B01 | mvn test | Pendiente | Ejecutado; salida 1 antes de compilar: parent Spring Boot 3.3.5 no resoluble por DNS. logs/mvn-test.log. |
| B02 | mvn package | Pendiente | Ejecutado por separado; salida 1 por el mismo bloqueo. logs/mvn-package.log. No existe JAR validado. |
| G01 | Rama remota y pull request | Pendiente | Crear rama devolvió HTTP 403 “Resource not accessible by integration”. Rama local, parche y descripción de PR preparados. No hay PR publicado. |

Los antecedentes H01–H03 tienen como fuente la tarea del usuario; no se encontró un informe original identificable en la búsqueda realizada. No se inventan sus fechas ni se sustituyen evidencias originales. El documento actual complementa esos antecedentes.

## 3. Procedimientos y evidencias

### Neon / PostGIS

Se ejecutó SELECT con constantes y geometrías sintéticas; no se consultaron reportes ni fotografías reales y no se escribieron tablas, extensiones ni migraciones. Se obtuvieron versión, fecha UTC, ST_Contains, ST_Covers y ST_AsGeoJSON. Resultado completo resumido en EDT-5.1-evidencia.json.

La prueba SpatialIntegrationTest requiere RUN_SPATIAL_INTEGRATION=true y el perfil validation. Usa TEST_DB_URL, TEST_DB_USER y TEST_DB_PASSWORD, deshabilita reemplazo por H2, creación/actualización DDL y Flyway, y configura conexiones de solo lectura. En la continuación se activó ddl-auto=validate para comprobar el esquema existente sin crearlo. Una consulta JDBC verifica predicados; otra consulta nativa JPA materializa una MultiPolygon sintética como Comuna con SRID 4326 y área 4. No necesita insertar comunas.

Es necesario proporcionar una base/branch de prueba aislada. No se creó una branch Neon que quedara ocupando recursos sin poder ejecutar Java.

### Fotografías

Se implementaron:
- Inspección del contenido JPEG, no del nombre ni MIME declarado.
- Máximo de 1000000 bytes de entrada y salida. Se define MB decimal; si el equipo elige MiB, debe ajustar requisitos y pruebas juntos.
- Límite de 12 millones de píxeles antes de decodificar.
- Rechazo de imágenes vacías, falsas, truncadas, PNG y errores/advertencias del decodificador.
- Reencodificación desde una imagen limpia para retirar EXIF/GPS/comentarios.
- UUID generados por el servidor, sin nombre original del ciudadano.
- Cloudinary tipo authenticated, firma SHA-256, HTTPS, timeouts y errores genéricos.
- Enlace firmado de recuperación con expiración de 60 segundos y Cache-Control: no-store.
- Borrado del espacio de diagnóstico limitado por UUID y prefijo notiurba/edt51, con invalidación de caché solicitada al proveedor.

Los endpoints /api/admin/diagnostics/photos son exclusivamente de prueba sintética; no constituyen un flujo ciudadano de producción ni guardan asociaciones foto–reporte. Faltan autorización de propiedad y persistencia dentro del flujo de reportes futuro. No se publica ninguna foto por defecto.

Las 12 aserciones ejecutadas fueron: JPG decodificable con ancho 32; alto 24; rechazo de null; vacío; exceso de bytes; contenido falso; PNG; marcadores falsos; truncado; retirada de metadatos; exceso de píxeles; y salida sanitizada decodificable. El código del runner contiene la enumeración exacta.

CloudinaryIntegrationTest usa Spring MVC real y proveedor real bajo opt-in. Genera una imagen sintética, carga con ADMIN, recupera por HTTPS y verifica JPEG/dimensiones; el borrado se intenta en finally. Todavía no se ejecutó. Una descarga desde HttpClient no reemplaza la comprobación solicitada en navegador.

### Render y latencia

Se preparó un contenedor de Java 21 con usuario sin privilegios, memoria JVM limitada y compilación con pruebas. render.yaml solicita exclusivamente plan free y desactiva despliegue automático. Nada fue contratado ni desplegado.

El perfil validation no ejecuta migraciones en Neon. Los diagnósticos están desactivados hasta habilitarlos explícitamente. SpatialDiagnosticsController exige ADMIN mediante la configuración existente y devuelve 503 genérico si falla la consulta.

Measure-Edt51Latency.ps1 registra UTC, HTTP, milisegundos y validez espacial. No considera aprobada una página HTML de “despertando” aunque responda 200. Exige separación mínima de 960 segundos y declaración de ausencia de otros clientes/monitores. Las dos fases se ejecutan manualmente; no existe un temporizador que genere tráfico durante el reposo. La comparación incluye la primera respuesta y la media de las cuatro posteriores.

No se comprobó si ya existe servicio Render: no hay conector ni URL disponible y la consulta de deployments de GitHub no está habilitada por la herramienta. No debe confundirse ese bloqueo con inexistencia del despliegue.

### Cloudflare R2

Se usó únicamente el nuevo bucket notiurba-edt51-20261027-isolated. JPG negro sintético de 32×24, 646 bytes. Se configuró Content-Type image/jpeg al cargar. Se habilitó r2.dev temporalmente y se intentó acceso externo sin credenciales; falló el DNS. La recuperación binaria actual tampoco permite comparación exacta por limitación del canal de lectura.

Limpieza confirmada: publicación desactivada y consultada de nuevo, objeto eliminado, lista vacía, bucket eliminado y ausencia verificada. No se modificó el bucket anterior notiurba-edt51-jpg-test-20261026. No se afirma que los bytes descargados de Internet fueran decodificables.

## 4. Seguridad y decisiones del equipo

Revisión limitada de patrones de secretos: no se detectaron claves en los archivos revisados. Los marcadores de .env.example son ejemplos, no credenciales. Los secretos se inyectan en el proceso y no se incluyen en logs, imágenes, Docker ni informes.

Pendientes de decisión/implementación del flujo ciudadano:
1. Aprobar política de consentimiento, destinatarios, conservación, supresión y acceso a las fotos; no se certifica cumplimiento legal por este trabajo.
2. Resolver el requisito de enlace estable en correo frente a recuperación privada temporal; no abrir fotos ciudadanas al público para facilitar correos.
3. Asociar cada foto al reporte y propietario antes de exponer endpoints a ciudadanos. Por ahora solo ADMIN y material sintético.
4. Aprobar manejo de caras/documentos/matrículas que sigan visibles; retirar EXIF no anonimiza contenido visual.
5. Confirmar JPG frente al requisito anterior JPG/PNG. Esta tarea valida JPG exclusivamente sin modificar la decisión de alcance general.
6. Separar aprovisionamiento del administrador de desarrollo V4 de producción. No se cambió la migración aplicada. El placeholder se interpola en SQL: una contraseña con comillas simples requiere revisar una estrategia segura de aprovisionamiento, no editar V4 ya aplicada.

No se cambiaron reglas territoriales, datos de ciudadanos, correos ni políticas compartidas.

## 5. Errores corregidos y límites

| Hallazgo | Cambio | Comprobación |
|---|---|---|
| Puerto DB local no coincide con Compose | Default 5433 | Revisión de configuración; arranque pendiente |
| Puerto HTTP fijo | PORT con fallback 8080 | Revisión de configuración; Render pendiente |
| Riesgo de migrar esquema compartido al probar | Perfil validation sin Flyway/DDL y con pool read-only | Código preparado; JDBC real pendiente |
| No había validación de contenido/tamaño de fotos | Validador JPEG y límites multipart | 12 comprobaciones Java aprobadas |
| Metadatos GPS podían viajar con la foto | Reencodificación limpia | Aserción ejecutada de retirada de marcador APP1 |
| No había diagnóstico de PostGIS | Endpoint sintético ADMIN, opt-in | Consulta SQL aprobada; endpoint pendiente |
| No había medición reproducible del reposo | Script por fases con validación del cuerpo | Parser de PowerShell: 0 errores de sintaxis; tráfico real pendiente |

Bloqueos del entorno corregidos parcialmente: Maven se localizó en IntelliJ; se cambió su repositorio local a un directorio escribible. Persisten DNS de Maven Central/GitHub/r2.dev, Docker sin motor, falta de credenciales de proceso y 403 de escritura GitHub.

## 6. Qué falta exactamente

- Acceso DNS/HTTPS desde Java/Maven a repo.maven.apache.org para descargar dependencias y ejecutar compilación/pruebas.
- Base Neon aislada y TEST_DB_* suministradas al proceso, con TLS verify-full. La conexión Neon de herramientas no equivale a credenciales JDBC disponibles.
- Ejecutar y revisar SpatialIntegrationTest; probar por separado migración limpia y validate en PostgreSQL 18, sin tocar una base compartida.
- CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET en el proceso. Ejecutar ciclo real desde Spring y abrir un enlace sintético firmado en navegador sin sesión.
- Cuenta/servicio Render accesible o URL existente confirmada. Desplegar únicamente al plan gratuito, configurar variables, producir token ADMIN de prueba y ejecutar las dos fases sin otro tráfico.
- Repetir R2 en otro bucket temporal desde un cliente que acceda a Internet, comprobando HTTP 200, Content-Type y decodificación/bytes, con limpieza equivalente.
- Permiso de escritura de la integración GitHub para crear rama/commit/PR, o aplicar el parche desde un clon con permisos del equipo.
- Decisiones de privacidad anteriores antes del uso ciudadano.

No se obtuvieron tiempos de Render ni logs de aplicación remota. No se ejecutó la arquitectura completa React → Render → Neon. No se marcan esas condiciones como aprobadas.

## 7. Archivos y reproducción

Ver README-ejecucion.md para comandos y pull-request.md para la descripción preparada.
Evidencia: EDT-5.1-evidencia.json y logs/.
Se verificó con git hash-object que V1–V4 conservan exactamente sus SHA Git originales:
- V1: 853eabc2a8d5818ec7ce5c64d2d1341e0008baa2
- V2: 61056cde4cd5de29c7f2d292f6b3337b74c19dff
- V3: a301c32136457d0952700e913202151b1d7ff7fd
- V4: 7e9a7d6f168f60bc86b2d1f090c343aecb15e9d6

Verificación adicional: el script de latencia se analizó con el parser de PowerShell, sin errores de sintaxis. No se ejecutaron solicitudes de latencia. git diff --check no encontró errores de espacios.

Referencias técnicas consultadas el 27/10/2026:
- https://cloudinary.com/documentation/image_upload_api_reference
- https://cloudinary.com/documentation/authentication_signatures
- https://render.com/docs/blueprint-spec
- https://render.com/docs/free
- Cloudflare API: R2 buckets, objetos y domains/managed.

## 8. Continuación y sincronización — 27/10/2026

Esta sección actualiza el corte anterior; conserva los antecedentes y sus evidencias.
Se retomaron los archivos existentes sin aplicar de nuevo el parche.

### Estado inicial y conservación

Se ejecutaron git remote -v, status, branch -a, branch -vv, log --oneline -10 y fetch origin --prune. La raíz del proyecto ChatGPT no es un repositorio; la copia de trabajo está en edt51-work.

Antes de modificar código:
- Rama local: codex/edt-5-1-validaciones.
- HEAD local previo: 92f510adc6b9f77bff7face4cf15bcb23eadaf23.
- Árbol limpio, tres commits de historial reconstruido local; no son descendientes del main auténtico.
- No había origin configurado. Se añadió exclusivamente https://github.com/notiurba/notiurba.git.
- Se creó y verificó NotiUrba-EDT51-backup.bundle, con todo el historial local.
- Se compararon los 51 archivos del ZIP original con el directorio local: cero diferencias.
- SHA-256 del ZIP original: D075CC1F3EAF1E40EBA7D87E7B1375EAB1DD4496F00909859216E02F0C92B36F.

GitHub confirmó main y develop en 2d6ceca3804ceebe1df95e838ef90bab911bcd9b, sin commits nuevos respecto al corte anterior. Los 25 blobs originales de la copia parcial coinciden con la base remota; los demás archivos del frontend no se editaron.

Fetch no pudo descargar objetos por “Could not resolve host: github.com”. El primer intento sin origin también intentó un transporte SSH no válido para esta copia y produjo un error MSYS. Tras configurar origin HTTPS explícito, el bloqueo reproducible fue DNS. No existe origin/main local fiable; no se fabricó esa referencia, ni se intentó mezclar historiales no relacionados. Tampoco se ejecutaron reset --hard, clean -fd ni push --force.

### Publicación parcial real en GitHub

El conector volvió a rechazar crear la rama con HTTP 403 “Resource not accessible by integration”. La sesión existente de Chrome permitió:
1. Crear codex/edt-5-1-validaciones desde main.
2. Publicar render.yaml mediante edición de texto.

HEAD remoto de validaciones: **9bd16279dbb01c82e584e677225551d7676bf68c**.
Comparación autenticada de GitHub: 1 commit por delante, 0 por detrás de main; único archivo añadido: render.yaml. Main permanece en 2d6ceca3804ceebe1df95e838ef90bab911bcd9b.

**La sincronización completa y el PR siguen pendientes.** La rama remota contiene main y render.yaml; el resto del código revisado y las pruebas permanecen locales y en el paquete actualizado. El historial local sigue separado del historial real. No hacer push directo de esta copia reconstruida.

La carga de archivos en Chrome falló porque la extensión no tiene “Allow access to file URLs”. Posteriormente el control de navegador se detuvo por no poder determinar la URL activa de Windows con suficiente confianza para aplicar su política. Se cesaron las acciones de navegador. No se ampliaron permisos ni se intentó eludir esa restricción. No se abrió ni publicó un PR.

### Correcciones y ampliaciones realizadas

- Render conserva rootDir=backend y dockerfilePath=./Dockerfile; ahora declara dockerContext=. de forma explícita. Estas rutas operan respecto a rootDir según la documentación oficial de monorepos. autoDeployTrigger queda como cadena "off", sin ambigüedad de YAML 1.1. Plan free, Java 21 y PORT conservados. No hubo despliegue.
- application-validation.yml cambia ddl-auto de none a validate. Flyway sigue deshabilitado y Hikari es read-only. Ahora la prueba exige un esquema aislado existente compatible con las entidades; no lo crea ni corrige automáticamente.
- SpatialIntegrationTest incorpora lectura mediante un repositorio Spring Data JPA de solo lectura y mapeo de Point con Hibernate, además de MultiPolygon, SRID, coordenadas y consultas JDBC espaciales.
- MigrationIntegrationTest crea exclusivamente un contenedor Testcontainers nuevo postgis/postgis:18-3.6, aplica V1–V4, comprueba PostgreSQL 18/PostGIS 3.6 y arranque Hibernate validate. Las credenciales y el administrador sintético se generan para el contenedor; ninguna URL Neon se usa en esa prueba. Un Docker ausente provoca fallo, no un aprobado por omisión. La imagen está documentada por el proyecto oficial PostGIS; su arranque no se ejecutó aquí.
- Surefire excluye *IntegrationTest del ciclo normal. Failsafe selecciona pruebas mediante perfiles explícitos spatial-tests, cloudinary-tests y migration-tests; no hay condiciones de entorno que las omitan silenciosamente al seleccionar un perfil.
- Actions separa pruebas unitarias/MVC del job de migraciones PostgreSQL 18. Ambos usan Java 21 y conservan informes aunque fallen. Neon y Cloudinary no se ejecutan automáticamente y no necesitan secretos para las pruebas normales.
- Cloudinary usa el endpoint canónico upload con type=authenticated incluido en los campos firmados, coherente con el SDK oficial. Se verifica también format=jpg en la respuesta. La firma se extrajo a una clase Java independiente para comprobar el orden y los campos permitidos sin proveedor.
- Se ampliaron las pruebas MVC de salud pública, fotos ADMIN, rechazo ciudadano y diagnósticos desactivados. Son pruebas con mocks y no acreditan servicios externos.

### Resultados de esta continuación

| Prueba | Resultado real | Estado |
|---|---|---|
| Conservación del ZIP, respaldo Git y comparación de base | ZIP 51 archivos sin diferencias; bundle verificado; main sin cambios nuevos | Aprobado |
| Publicación de rama y render.yaml | Rama y commit 9bd1627 confirmados por navegador y API | Aprobado, alcance parcial |
| Sincronizar todo el código y publicar PR | Fetch DNS; conector 403; carga de archivos sin permiso; navegador detenido por política de URL | Pendiente |
| JPEG | 12 aserciones Java 21, cero fallos; logs/continuacion/jpeg-java21.log | Aprobado |
| Firma Cloudinary | 8 aserciones Java 21, cero fallos; vector SHA-256 corroborado con .NET; logs/continuacion/signatures-java21.log | Aprobado, solo firma local |
| YAML/XML/rutas | 18 comprobaciones con SnakeYAML 2.6 disponible y parser XML; logs/continuacion/configuration-java21.log | Aprobado, sintaxis/rutas locales |
| mvn clean test | Intentado; salida 1 antes de compilar por DNS de Maven Central | Pendiente |
| mvn package | Intentado por separado; salida 1 por el mismo bloqueo | Pendiente |
| GitHub Actions | La API no mostró ejecuciones existentes. Workflow local preparado; aún no publicado ni ejecutado | Pendiente |
| JDBC/JPA/Hibernate con Neon | Pruebas ampliadas; sin dependencias descargables ni TEST_DB_* en el proceso | Pendiente |
| Migración limpia PostgreSQL 18 | Prueba añadida; motor Docker inactivo y Maven bloqueado | Pendiente |
| Ciclo Cloudinary desde Spring | Sin credenciales CLOUDINARY_* en el proceso; no se ejecutó el proveedor | Pendiente |
| R2 externo | Curl volvió a fallar por DNS; no se creó otro bucket mientras faltaba conectividad | No concluyente |
| Render y latencia tras 16 minutos | No hay URL/servicio verificado ni arranque disponible; no se inventan mediciones | Pendiente |

Los conteos 12+8+18 corresponden a comprobaciones ejecutadas; no se presentan como 38 pruebas Spring o integraciones aprobadas. Las marcas de tiempo exactas están en los logs. Las migraciones mantienen sus hashes originales.

Bloqueos restantes: DNS/HTTPS de Git y Maven Central desde la terminal; motor Docker inactivo; credenciales de base aislada/Cloudinary no inyectadas; Render sin URL verificada; permiso de carga de la extensión; control de navegador detenido en este turno; escritura del conector GitHub denegada. No se buscaron secretos en archivos privados, no se desplegaron servicios ni se modificó infraestructura compartida.

### Comandos de integración actualizados

Las instrucciones anteriores con RUN_* y -Dtest han sido sustituidas por perfiles Failsafe:

```powershell
mvn -B -f backend/pom.xml clean test
mvn -B -f backend/pom.xml package
# Solo con TEST_DB_* de una base aislada que tenga esquema:
mvn -B -f backend/pom.xml -Pspatial-tests verify
# Solo con CLOUDINARY_* del entorno de prueba:
mvn -B -f backend/pom.xml -Pcloudinary-tests verify
# Solo con Docker local activo; no utiliza Neon:
mvn -B -f backend/pom.xml -Pmigration-tests verify
```

La prueba de migración no certifica la versión 18.6 exacta de Neon: comprueba el PostgreSQL 18 que entregue la imagen y registra la familia esperada. Para Neon debe ejecutarse además la integración contra su base aislada.

Fuentes técnicas adicionales:
- https://render.com/docs/monorepo-support
- https://github.com/postgis/docker-postgis
- https://github.com/cloudinary/pycloudinary/blob/master/cloudinary/uploader.py

## 9. Corte final de esta ejecucion: 2026-10-27 UTC

Se continuo desde el arbol de la version 2, sin reemplazarlo. Se conservaron el ZIP y ambos parches anteriores. La tabla vigente completa esta en **EDT-5.1-resultados.csv** y en final_review de **EDT-5.1-evidencia.json**. Las secciones anteriores son antecedentes cronologicos; no deben interpretarse como ejecuciones nuevas.

### Resumen ejecutivo y sincronizacion

GitHub se consulto nuevamente: main sigue en 2d6ceca3804ceebe1df95e838ef90bab911bcd9b y validaciones en 9bd16279dbb01c82e584e677225551d7676bf68c. No hay nuevos commits en main: incorporados 0. La rama remota sigue un commit por delante y contiene solamente render.yaml. No existen ejecuciones de Actions para ese HEAD. No se creo PR. La copia local sigue teniendo historial reconstruido; no se debe publicar su historial. El intento de clon completo fallo otra vez por DNS, antes de descargar objetos. No hubo merge ni conflictos resueltos; la ausencia de conflictos no equivale a una sincronizacion completa.

La escritura del conector GitHub devolvio 403 al crear un blob; la carga por navegador fallo otra vez por falta del permiso local de archivos de la extension. No se cambiaron permisos ni se uso un transporte alternativo para eludirlos.

### Cambios nuevos respecto de v2

- MigrationIntegrationTest ahora imprime server_version exacto, PostGIS_Full_Version y referencia de imagen. Al ejecutarse acreditara la version real del contenedor, sin atribuirle la version 18.6 de Neon. Este test sigue sin ejecutar aqui.
- CloudinaryIntegrationTest desactiva el volcado automatico MockMvc para evitar publicar URLs con firma. Los errores de transporte omiten la URI. Se agregaron comprobaciones reales previstas de enlace correctamente firmado pero ya vencido, acceso sin firma y recuperacion despues del borrado. Una respuesta inesperada hara fallar la prueba; no se presume que Cloudinary soporte esas condiciones sin ejecutarlas.
- tools/Complete-Edt51.ps1 permite clonar el repositorio autentico, rechaza arboles sucios e historiales no relacionados, integra la rama remota y main mediante merge y comprueba el parche antes de aplicarlo. No reemplaza render.yaml ni permite cambios de migraciones respecto de main. Ejecuta Maven, registra UTC/salidas y permite publicar un PR borrador con -Publish solo si las pruebas seleccionadas pasaron. No despliega ni fusiona. Si main cambia durante las pruebas, se detiene antes del push para exigir otra validacion.

### Procedimientos efectivamente ejecutados

Ambiente: Windows, OpenJDK 21.0.10; Maven 3.9.16 de IntelliJ; repositorio Maven temporal ../maven-cache. El repositorio normal de usuario no contiene el parent Boot 3.3.5. No hay settings.xml del usuario ni variables HTTP_PROXY/HTTPS_PROXY/ALL_PROXY. No se modifico TLS, certificados ni seguridad global. DNS falla para GitHub, Maven Central y Cloudinary; por ello no se ha podido verificar negociacion HTTPS/TLS desde la terminal. Docker no encuentra la tuberia del motor Linux.

Se ejecutaron por separado estos cinco comandos, con UTC y salida en logs/cierre:

```powershell
mvn -B -f backend/pom.xml clean test
mvn -B -f backend/pom.xml package
mvn -B -f backend/pom.xml -Pspatial-tests verify
mvn -B -f backend/pom.xml -Pmigration-tests verify
mvn -B -f backend/pom.xml -Pcloudinary-tests verify
```

Todos devolvieron 1 por resolucion del parent, ANTES de compilar o arrancar pruebas. Se uso adicionalmente -Dmaven.repo.local=../maven-cache para evitar escribir fuera del espacio permitido. No se ejecutaron JUnit, Spring MVC, JPA, Hibernate, Flyway ni llamadas Java a Cloudinary. Esto se clasifica PENDIENTE, no como fallo demostrado de su codigo.

Las comprobaciones Java independientes se compilaron con javac21 y ejecutaron con java21: JPEG12, firma8 y configuracion18, todas sin fallos. Fechas exactas 06:27:09.798956100Z, 06:27:10.056878400Z y 06:27:11.497663800Z. El parser de PowerShell acepto Complete-Edt51.ps1; su intento de clon fallo correctamente y no toco el arbol v2. Ese resultado acredita solo sintaxis y detencion por red, no una sincronizacion exitosa.

Se verifico git diff --check sin errores. El diff de migraciones respecto de 7097e16 sigue vacio. La revision adicional encontro cero patrones de alta confianza en 58 textos actuales; no cubre todo el historial ni binarios. S01 tiene exclusivamente ese alcance.

Render documenta que dockerfilePath y dockerContext son relativos a rootDir al configurarlo. Se corroboraron las rutas locales y Java21, pero no se ejecutaron validacion de blueprint por API, build Docker ni despliegue.
Fuente: https://render.com/docs/monorepo-support (Setting a root directory y Root-relative settings, consulta 2026-10-27).

### Bloqueos exactos y alternativa preparada

1. GitHub por Git: DNS impide clone/fetch; conector de escritura403. El codigo completo y workflow siguen sin publicar, por tanto no hay CI ni PR ejecutados. Usar el runner en una maquina autorizada con conectividad y Git/gh autenticados.
2. Maven Central: DNS y parent ausente. Ejecutar los mismos comandos desde esa maquina o publicar el workflow para que el PR dispare los jobs Java21 y migraciones. No atribuir un CI verde a proveedores externos.
3. Docker: motor Linux inactivo. Iniciar Docker Desktop antes de -Migration o usar el job migration-postgres18. Registrar las versiones emitidas y la limpieza real del contenedor.
4. Neon: TEST_DB_URL/USER/PASSWORD no estan inyectadas. Se necesita una base aislada ya migrada para -Spatial. No se crearon recursos sin poder usarlos ni se buscaron claves privadas.
5. Cloudinary: CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET no estan inyectadas en Java. Inyectarlas mediante el almacen autorizado y ejecutar -Cloudinary. Abrir despues un enlace temporal de imagen sintetica en navegador sin sesion para completar C03; no guardar la URL firmada en evidencias.
6. R2: persiste DNS externo. Se conserva NO CONCLUYENTE en O02/O03 y la evidencia de limpieza ya aprobada; no se crearon buckets adicionales inutilmente. O02 exige un canal binario sin conversion UTF8 y comparacion SHA256.
7. Render: no hay URL ni servicio aislado autorizado verificado. No hay build, HTTP ni latencia. Usar el blueprint free sin autoDeploy y el script antes/despues de16min, documentando otros monitores y trafico. No afirmar reposo exclusivo solo por tiempo transcurrido.

Privacidad pendiente de decision del equipo: retencion y responsables de fotos, consentimiento y finalidades del tratamiento, acceso por propietario/ADMIN, expiracion y revocacion, trazabilidad y borrado. Los endpoints actuales son diagnosticos ADMIN con JPEG sinteticos y no constituyen el flujo ciudadano definitivo. Las fotos no se publican por defecto.

Resultado: 5 de los 20 identificadores actuales aprobados con el alcance indicado, 13 pendientes, 2 no concluyentes. H01-H03 se conservan aprobados como antecedentes aportados, sin inventar fechas originales.
## 10. Revision puntual Cloudinary authenticated sobre v3

Ver Cloudinary-authenticated-revision.md para el contraste oficial, decisiones, comandos y evidencia nueva. Se reemplazo la construccion manual de peticiones por el SDK oficial Java. No se demostro que la ruta v3 fallara contra el proveedor: el SDK revisado usa upload con type firmado, pese a la presentacion distinta del ejemplo REST.

Se agregaron ocho pruebas de contrato SDK/errores, un caso MVC de error de enlace y verificacion remota independiente de metadata authenticated, acceso sin firma, enlace valido/vencido y ausencia tras eliminar. Todas las pruebas nuevas JUnit y la integracion REAL siguen PENDIENTES: Maven termina antes de compilar con Permission denied: getsockopt; DNS ahora resuelve. Las tres variables CLOUDINARY_* requeridas siguen ausentes. No hubo cargas remotas en esta revision. El JPEG local volvio a superar sus12 aserciones. El parser Java21 reviso solamente sintaxis de las cinco fuentes cambiadas: no acredita compilacion, resolucion de imports ni ejecucion.
## Correccion puntual SHA256 del SDK Java 2.3.1

2026-10-27 UTC. Se comprobo en Configuration.java del commit oficial 4d2e62b que la configuracion utiliza SignatureAlgorithm.valueOf; SignatureAlgorithm.java define exactamente SHA1 y SHA256. El valor sha256 introducido en la migracion al SDK era incorrecto. Se reemplazo exclusivamente ese literal en CloudinaryPhotoStore, CloudinaryIntegrationTest y CloudinaryPhotoStoreTest. No se cambia la ruta, el tipo authenticated ni la logica de las operaciones.

El parche NotiUrba-Cloudinary-SHA256.patch contiene tres sustituciones; git apply --check contra los originales del commit d0e39bb y git apply --reverse --check contra los archivos corregidos pasaron. No se genero otro ZIP ni se presenta una nueva version funcional aprobada.

Se reintentaron mvn clean test, mvn package y mvn -Pcloudinary-tests verify con backend/pom.xml. Los tres devolvieron1 antes de compilar, por Permission denied: getsockopt al descargar el parent Spring Boot3.3.5. Las variables CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET siguen ausentes; no se mostraron valores. Evidencia con UTC y comandos: logs/cloudinary-sha256-fix/. JUnit no arranco, no hubo cargas remotas y C02/C03/C04 siguen PENDIENTES. La comprobacion de aplicacion del parche no acredita inicializacion del SDK ni una integracion real.

Fuentes oficiales:
- https://github.com/cloudinary/cloudinary_java/blob/4d2e62b/cloudinary-core/src/main/java/com/cloudinary/Configuration.java
- https://github.com/cloudinary/cloudinary_java/blob/4d2e62b/cloudinary-core/src/main/java/com/cloudinary/SignatureAlgorithm.java
## Corte actualizado: copia de publicacion y bloqueo de escritura

2026-10-27 UTC. Se recupero el clon autentico original y se comprobo c681a8a. Se incorporaron los commits remotos propios sin conflictos en ese clon, cuyo checkpoint es 84e459f. No se escribio sobre main ni develop y no se creo PR. main sigue en 2d6ceca3804ceebe1df95e838ef90bab911bcd9b.

El navegador publico cuatro archivos adicionales en validaciones: CloudinaryPhotoStore, CloudinarySignature, JpegValidator y PhotoUploadErrors. La rama remota quedo en 649fe0b7833817948c965b5674f999d5bf0915f3. Los seis archivos publicados antes de esta ejecucion conservan exactamente su contenido. El SDK publicado contiene SHA256. No se reaplico el parche.

El push por terminal sigue bloqueado por Git Credential Manager: sh.exe falla con NtCreateDirectoryObject 0xC0000022, seguido de imposibilidad de obtener Username. El conector GitHub devuelve 403 Resource not accessible by integration. La edicion web permitio commits parciales pero su editor interrumpe operaciones; no se afirma una publicacion completa.

Para dejar un push de avance directo sin mas fusiones, se creo la copia autentica notiurba-edt51-publicacion, directamente desde la ultima rama remota, y se aplico exclusivamente el delta faltante conservado, con git apply --check. No se reconstruyo historial ni se modificaron los diez archivos ya publicados. El cambio de newline del SDK remoto se conservo. Frontend y V1-V4 siguen identicos a origin/main. El trabajo previo no se elimina: permanece en notiurba-authentic-edt51.

Complete-Edt51.ps1 ahora rechaza estados desactualizados mediante merge-base --is-ancestor y no ejecuta git merge, gh pr create ni gh pr merge. El workflow tiene push limitado a codex/edt-5-1-validaciones; jobs Java21 de unitarias/paquete y migracion PostGIS18, con informes Surefire/Failsafe, sin secretos externos. El empaquetado usa mvn package sin omitir pruebas.

Se preparo tools/Publish-Edt51Branch.ps1 para Windows PowerShell5.1 o posterior. Verifica rama exacta, origin, historial completo, arbol limpio, commits remotos incluidos, archivos requeridos, SHA256, ausencia de diferencias en frontend/migraciones y diff --check. Solo puede ejecutar push de HEAD a refs/heads/codex/edt-5-1-validaciones. No crea commits, PR, fusiones, despliegues ni cambios de permisos. -CheckOnly permite revisar sin publicar. Si hay cambios concurrentes, se detiene en vez de sobrescribirlos.

Maven clean test se intento tambien desde esta copia: salida1 antes de compilar por Permission denied: getsockopt al resolver el parent Boot3.3.5. Evidencia: logs/publicacion-validaciones/maven-clean-test.log. La validacion sintactica de ambos scripts paso; no equivale a Maven ni a CI aprobado.

Actions aun no tiene ejecuciones y el workflow no esta publicado al momento de este corte. G01 sigue PENDIENTE, al igual que las integraciones Java/remotas y las migraciones no ejecutadas. No se genera ZIP.

Comando para publicar desde una terminal normal de Windows, fuera del entorno restringido de esta sesion:

```powershell
& "C:\Users\Usuario\.codex\.chatgpt-projects\g-p-6aacbd0a1a688191b438845119074d96\edt51-work\notiurba-edt51-publicacion\tools\Publish-Edt51Branch.ps1"
```

Despues del push, consultar https://github.com/notiurba/notiurba/actions?query=branch%3Acodex%2Fedt-5-1-validaciones y exigir logs/reportes reales antes de cambiar estados. La publicacion por si sola no acredita compilacion ni integraciones.
La ejecucion real del preflight detecto un falso rechazo de SHA256: -match de PowerShell ignora mayusculas y tambien reconocia SHA256 como sha256. Se corrigio con -cmatch/-cnotmatch. El intento fallido se conserva en logs/publicacion-validaciones/windows-publisher-preflight.log. La comprobacion se repite despues de guardar la correccion; esto no ejecuta push ni pruebas Java.

Cierre 2026-10-27T12:24:37.6737447Z : preflight repetido APROBADO (logs/publicacion-validaciones/windows-publisher-preflight-retry.log). Comprueba publicacion segura; no ejecuta Maven ni Actions. La publicacion completa y los jobs continuan pendientes por los bloqueos documentados.

### Cierre de publicacion de este corte

Los cambios de integraciones externas quedaron guardados localmente en 673e33f. El push exclusivo de validaciones fallo: Credential Manager/sh NtCreateDirectoryObject 0xC0000022 y unable to get password from user. La alternativa del conector devolvio HTTP403 Resource not accessible by integration. Ver logs/external-config/publication.log. NO estan publicados estos cambios; NO hay nueva ejecucion CI que acredite la prueba de arranque validation anadida. G01 queda pendiente para esta actualizacion, conservando la publicacion/CI previa de a599593.

Desde PowerShell normal de Windows, publicar con el script existente; este verifica arbol limpio, main incluido y ausencia de avances remotos concurrentes, y no fusiona ni crea PR:

```powershell
& "C:\Users\Usuario\.codex\.chatgpt-projects\g-p-6aacbd0a1a688191b438845119074d96\edt51-work\notiurba-edt51-publicacion\tools\Publish-Edt51Branch.ps1"
```

Despues de publicar, comprobar los jobs normales del nuevo push; no atribuirles integraciones externas. Para estas, configurar el Environment/secretos y comprobar el dispatch o utilizar el runner local como explica INTEGRACIONES-EXTERNAS.md. main conserva 2d6ceca3804ceebe1df95e838ef90bab911bcd9b. No se hicieron PR, fusiones ni despliegues.
