# EDT 5.1: ejecucion externa con recursos de prueba

Corte: 2026-10-27 UTC. Rama unica: codex/edt-5-1-validaciones. No crear PR, fusionar, desplegar produccion ni modificar V1-V4.

## Evidencia que ya esta aprobada

La ejecucion https://github.com/notiurba/notiurba/actions/runs/40507497490 del commit a5995934455e5f6685cc2687b56dee27b377a2c7 tiene backend-unit y migration-postgres18 SUCCESS. Se consultaron ambos jobs y sus logs reales. Evidencia filtrada: logs/external-config/confirmed-actions.json. Son 25 pruebas unitarias/MVC, cero fallos/errores/omitidas, Maven test/package exitosos, y una MigrationIntegrationTest exitosa. El contenedor realmente informo PostgreSQL 18.6 (Debian 18.6-1.pgdg13+2), PostGIS 3.6.4; NO es evidencia de conexion Java a Neon. No se repiten localmente esas pruebas para sustituir esta evidencia.

## Bloqueos comprobados

- Neon conectado: el proyecto NotiUrba solo muestra production como rama primaria/default. No se obtuvo su contrasena ni se consultaron sus tablas. No existe una rama aislada disponible para N02/N03. El equipo debe preparar una base nueva o rama schema-only, sin datos ciudadanos, confirmando cuota gratuita. No crear una copia de datos de production.
- TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD, JWT_SECRET y las tres variables CLOUDINARY no estan en el proceso. No se buscaron en archivos privados ni sesiones del navegador.
- gh auth status no tiene autenticacion; no se pueden listar/configurar Environment Secrets ni emitir dispatch desde esta terminal. El conector GitHub disponible no ofrece gestion de secretos/dispatch. No se afirma que los secretos del repositorio esten ausentes: su existencia no pudo consultarse.
- Docker: el pipe dockerDesktopLinuxEngine no existe. No se arranco ni altero ningun contenedor existente.
- R2: las evidencias anteriores O01/O04 ya acreditan creacion/limpieza del bucket temporal. O02/O03 siguen NO CONCLUYENTES; este proceso no tiene credenciales S3 y el conector devuelve cuerpos binarios como texto, por lo que no se repite una descarga que no puede probar SHA-256. No se creo otro bucket ni se habilito publicacion.
- Render: no hay URL de servicio aislado, API key ni conector Render disponible; no se pudo verificar servicio/plan/variables. No se creo ni desplego un servicio publico. Falta aprobacion explicita para crear/exponer uno si no existe.

## GitHub Environment y secretos (sin valores en el chat)

Un administrador autorizado debe abrir Settings > Environments y preparar edt51-external-validation. Configurar Deployment branches: Selected branches, solo codex/edt-5-1-validaciones. Agregar revisores requeridos si el plan/repositorio lo admite. No cambiar el plan ni comprar funciones. Este Environment protege jobs, no implica un despliegue de Render.

Agregar estos Environment Secrets EXCLUSIVAMENTE de prueba:

| Proveedor | Secrets |
| --- | --- |
| Neon | TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD, JWT_SECRET |
| Cloudinary | CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET |

Agregar la variable de Environment EDT51_EXTERNAL_ALLOWED=true solamente despues de verificar aislamiento, ausencia de datos ciudadanos y autorizacion para crear/borrar recursos sinteticos. No usar secretos de produccion ni reutilizar un Environment de produccion.

Desde una terminal normal autorizada, los siguientes comandos solicitan los secretos interactivamente, sin incorporarlos al historial:

```powershell
gh auth status
$repo = 'notiurba/notiurba'
$environment = 'edt51-external-validation'
gh secret list --repo $repo --env $environment
# Muestra nombres/fechas, no valores. Despues, solo los que falten:
gh secret set TEST_DB_URL --repo $repo --env $environment
gh secret set TEST_DB_USER --repo $repo --env $environment
gh secret set TEST_DB_PASSWORD --repo $repo --env $environment
gh secret set JWT_SECRET --repo $repo --env $environment
gh secret set CLOUDINARY_CLOUD_NAME --repo $repo --env $environment
gh secret set CLOUDINARY_API_KEY --repo $repo --env $environment
gh secret set CLOUDINARY_API_SECRET --repo $repo --env $environment
```

No copiar sus valores a logs, capturas ni commits. EDT51_EXTERNAL_ALLOWED puede configurarse como variable desde el Environment, tras la revision indicada.

## Neon: TLS, esquema y solo lectura

TEST_DB_URL debe tener este formato (reemplazar host y base, sin usuario/password embebidos):

```text
jdbc:postgresql://<endpoint-de-pruebas>.us-east-2.aws.neon.tech/<base-aislada>?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory
```

Se usa el truststore de Java y verificacion del hostname. No sustituir verify-full por require ni usar NonValidatingFactory. Usar la conexion directa para preparar el esquema; el lector de pruebas puede usar el endpoint directo o pooled aprobado. El runner rechaza parametros JDBC ajenos a la lista permitida.

El administrador de la base aislada debe aplicar V1-V4 originales una sola vez, con un secreto de semilla sintetico, y proporcionar un usuario de lectura (CONNECT, USAGE, SELECT sobre tablas/secuencias necesarias, sin permisos de creacion/escritura). No usar la cuenta owner en la ejecucion de validacion. La configuracion Hikari establece read-only=true y SET default_transaction_read_only=on. Flyway permanece disabled y Hibernate validate.

Procedimiento separado para provisionar, SOLO con la conexion owner de la base aislada recien creada, nunca con production. Despues de mvn package, variables TEST_DB_* y JWT_SECRET se inyectan por el almacen autorizado. TEST_SCHEMA_ADMIN_PASSWORD se pide de forma segura. Los argumentos contienen referencias literales a variables, no sus valores:

```powershell
if ((git branch --show-current) -ne 'codex/edt-5-1-validaciones') { throw 'Rama incorrecta' }
# Confirmar manualmente host/base aislada y vacia antes de continuar.
$secure = Read-Host 'Semilla ADMIN sintetica de la base aislada' -AsSecureString
$env:TEST_SCHEMA_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', $secure).Password
try {
  java -jar backend/target/backend-0.0.1-SNAPSHOT.jar `
    '--spring.profiles.active=validation' `
    '--spring.flyway.enabled=true' `
    '--spring.datasource.hikari.read-only=false' `
    '--spring.datasource.hikari.connection-init-sql=SET default_transaction_read_only=off' `
    '--spring.flyway.placeholders.adminPassword=${TEST_SCHEMA_ADMIN_PASSWORD}'
  # Detener con Ctrl+C tras verificar migraciones/arranque; no dejar este modo activo.
} finally { Remove-Item Env:TEST_SCHEMA_ADMIN_PASSWORD -ErrorAction SilentlyContinue }
```

Provisionar no aprueba N02/N03. Sustituir TEST_DB_USER/PASSWORD por el lector, cerrar el proceso anterior y ejecutar despues el perfil validation normal. No mantener los argumentos de provisioning en Render.

Se agrego una comprobacion de arranque validation al test del contenedor desechable: sin DB_PASSWORD/ADMIN_DEV_PASSWORD, Flyway apagado, Hibernate validate, read-only y salud HTTP200. Esta nueva comprobacion aun requiere CI; el aprobado historico de migraciones no la acredita.

## Ejecucion manual externa

El workflow conserva push solo en la rama de validaciones y los dos jobs existentes. Los dos jobs externos requieren workflow_dispatch, ref exacta, seleccion explicita de proveedor y Environment dedicado. No reciben secretos en pull_request/push. Permissions: contents: read; no push/commits. El runner comprueba tambien el SHA porque checkout en Actions usa detached HEAD.

IMPORTANTE: GitHub documenta que workflow_dispatch necesita el workflow en la rama predeterminada. Este archivo NO existe en origin/main. La configuracion queda preparada pero no se promete disponibilidad de Run workflow. NO se modificara main ni la rama predeterminada para desbloquearlo. Si la API permite el dispatch del workflow ya registrado por push, comprobar una ejecucion real; si lo rechaza, conservar el bloqueo y usar el runner local con secretos autorizados.

```powershell
gh workflow run edt51-validation.yml --repo notiurba/notiurba `
  --ref codex/edt-5-1-validaciones -f neon=true -f cloudinary=false
# Ejecutar Cloudinary por separado despues de configurar sus secretos:
gh workflow run edt51-validation.yml --repo notiurba/notiurba `
  --ref codex/edt-5-1-validaciones -f neon=false -f cloudinary=true
gh run list --repo notiurba/notiurba --branch codex/edt-5-1-validaciones
# Consultar run view ID --log-failed; no publicar logs crudos externos.
```

Alternativa inmediata Windows con PowerShell7, Java21 y Maven en PATH: inyectar unicamente las variables del proveedor elegido en el proceso. Un ejemplo sin escribir valores en el historial:

```powershell
$names = @('TEST_DB_URL','TEST_DB_USER','TEST_DB_PASSWORD','JWT_SECRET')
# Para Cloudinary usar en su lugar sus tres nombres de la tabla.
try {
  foreach ($name in $names) {
    $secure = Read-Host $name -AsSecureString
    [Environment]::SetEnvironmentVariable($name, [Net.NetworkCredential]::new('', $secure).Password, 'Process')
  }
  # Solo tras confirmar aislamiento/autorizacion:
  $env:EDT51_EXTERNAL_ALLOWED = 'true'
  ./tools/Invoke-Edt51External.ps1 -Provider Neon
  # O, con sus variables y autorizacion: -Provider Cloudinary
} finally {
  foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name,$null,'Process') }
  Remove-Item Env:EDT51_EXTERNAL_ALLOWED -ErrorAction SilentlyContinue
}
```

El runner ejecuta exactamente mvn -B -f backend/pom.xml -Pspatial-tests verify o -Pcloudinary-tests verify. Si faltan variables se detiene ANTES de Maven/proveedor. Los reportes crudos quedan en backend/target y no deben compartirse. En external-results/<proveedor>/ se exportan JUnit XML sin properties/system-out/system-err/trazas/mensajes del proveedor y result.json con fecha UTC, salida Maven y conteos. Actions solo sube estos archivos saneados (7 dias). No equivale a prueba de navegador.

Cloudinary conserva SDK2.3.2, SHA256 y authenticated. La prueba confirma metadatos, JWT ADMIN, JPG sintetico32x24, enlaces temporales, acceso unsigned denegado, firmas alteradas/vencidas y eliminacion. Ahora confirma ausencia en el bloque finally incluso si falla una asercion anterior. Si el proceso es terminado por fuerza o el proveedor no permite borrar, revisar en la consola el prefijo notiurba/edt51/ y limpiar exclusivamente el UUID sintetico registrado; no dar limpieza por aprobada.

C03 navegador: despues de una prueba real aprobada, una persona autorizada puede abrir el enlace temporal de un JPEG sintetico en una ventana privada, sin sesion de Cloudinary. Verificar HTTP200 y JPEG decodificable, cerrar y borrar el recurso. No guardar URL/captura de la barra/HAR. Esto sigue pendiente en esta ejecucion.

## R2: reproducir O02 sin convertir bytes a texto

Usar AWS CLI S3 con credenciales de prueba de alcance minimo, un bucket temporal vacio propio ya aprobado y un JPEG sintetico. No utilizar el endpoint REST del conector para decodificar binarios. AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY se suministran por el almacen seguro. Ejemplo Windows PowerShell:

```powershell
# Variables aprobadas: R2_ENDPOINT (https://<account>.r2.cloudflarestorage.com), R2_TEST_BUCKET.
# Confirmar que es un bucket temporal vacio, no compartido.
$key = 'edt51-' + [guid]::NewGuid() + '.jpg'
Add-Type -AssemblyName System.Drawing
$source = Join-Path $env:TEMP ($key + '.source.jpg')
$download = Join-Path $env:TEMP ($key + '.download.jpg')
$bitmap = [Drawing.Bitmap]::new(32,24)
try { $bitmap.Save($source,[Drawing.Imaging.ImageFormat]::Jpeg) } finally { $bitmap.Dispose() }
try {
  aws s3api put-object --endpoint-url $env:R2_ENDPOINT --bucket $env:R2_TEST_BUCKET --key $key --body $source --content-type image/jpeg
  if ($LASTEXITCODE) { throw 'Fallo de carga' }
  $metadata = aws s3api get-object --endpoint-url $env:R2_ENDPOINT --bucket $env:R2_TEST_BUCKET --key $key $download | ConvertFrom-Json
  if ($LASTEXITCODE) { throw 'Fallo de descarga' }
  if ($metadata.ContentType -ne 'image/jpeg') { throw 'MIME incorrecto' }
  if ((Get-FileHash $source).Hash -ne (Get-FileHash $download).Hash) { throw 'Integridad incorrecta' }
  $image = [Drawing.Image]::FromFile($download)
  try { if ($image.Width -ne 32 -or $image.Height -ne 24) { throw 'Dimensiones incorrectas' } } finally { $image.Dispose() }
} finally {
  aws s3api delete-object --endpoint-url $env:R2_ENDPOINT --bucket $env:R2_TEST_BUCKET --key $key
  Remove-Item -LiteralPath $source,$download -ErrorAction SilentlyContinue
}
```

No atribuir HTTP200 a la CLI sin conservar tambien evidencia del transporte. Para O03 se requiere autorizar r2.dev solo en ese bucket aislado, recuperar por HTTPS mediante HttpClient/ReadAsByteArrayAsync sin Authorization, comprobar HTTP200/MIME/SHA256/decodificacion y deshabilitar publicacion EN FINALLY antes de borrar objeto/bucket. No se habilita automaticamente con los scripts de esta entrega. Confirmar enabled=false/publicacion apagada desde API/listado antes de aprobar O04 de una nueva ejecucion. La limpieza historica anterior se conserva.

## Docker, Render y latencia

render.yaml ahora fija tambien branch: codex/edt-5-1-validaciones. Se conservan rootDir: backend, dockerfilePath: ./Dockerfile, dockerContext: ., plan free y autodeploy off. La guia monorepo de Render especifica estas rutas relativas a rootDir. No se ha desplegado.

Con motor Docker disponible: docker build -f backend/Dockerfile -t notiurba-edt51:validation backend. Para arrancar, usar un archivo privado fuera del repo con variables de validation (TEST_DB_*, JWT_SECRET, DIAGNOSTICS_ENABLED=true si se valida ADMIN, PHOTO_DIAGNOSTICS_ENABLED=false), y docker run --rm --name notiurba-edt51-validation --env-file <archivo-privado> -p 8080:8080 notiurba-edt51:validation. Consultar /api/health y el diagnostico espacial ADMIN; detener solamente ese contenedor nuevo.

Antes de Render: el responsable debe confirmar URL/ID de servicio aislado, plan free sin extras, rama autorizada, Neon aislado, variables y aprobacion de exposicion publica. No se despliega sin esa informacion. La prueba nueva de validation verificara si se requieren DB_PASSWORD/ADMIN_DEV_PASSWORD; no se inventan valores de respaldo en Render.

R03: ejecutar tools/Measure-Edt51Latency.ps1 contra la URL aprobada, cinco BEFORE, verificar ausencia de monitores/trafico16min, cinco AFTER. Si no se verifica la inactividad, conservar NO CONCLUYENTE para ese criterio; nunca inventar tiempos. Ninguna solicitud Render se ejecuto en este corte.

## Fuentes oficiales consultadas

- https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow
- https://jdbc.postgresql.org/documentation/ssl/
- https://render.com/docs/monorepo-support

Los comandos de infraestructura son instrucciones para recursos aislados autorizados; no se ejecutaron ni acreditan integraciones por estar documentados.
