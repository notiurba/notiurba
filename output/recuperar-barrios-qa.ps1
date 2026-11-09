#requires -Version 7.0
[CmdletBinding()]
param([int]$DbPort=15435,[int]$ApiPort=18091,[int]$WebPort=5175)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $repo
if ((git branch --show-current).Trim() -ne 'fix/registro-pr20') { throw 'Rama incorrecta; no se modifica nada.' }
$head=(git rev-parse HEAD).Trim()
$java='C:\notiurba\validation-tools\jdk21\jdk-21.0.13+9\bin\java.exe'
$maven='C:\Users\Usuario\.m2\wrapper\dists\apache-maven-3.9.9\8e74001100ff70d6af083c5511fcc5ec49282d7017cde82c3698eee8fdf86698\bin\mvn.cmd'
$settings='C:\notiurba\validation-tools\maven-settings.xml'
$sql=Join-Path $repo 'backend/migraciones-pendientes/barrios-pmgr-2020/V17__catalogo_barrios_pmgr_2020.sql'
$migrations=Join-Path $repo 'backend/src/main/resources/db/migration'
$viteCli=Join-Path $repo 'frontend/node_modules/vite/bin/vite.js'
foreach($p in @($java,$maven,$settings,$sql,$viteCli)) { if (!(Test-Path -LiteralPath $p)) { throw "Falta dependencia: $p" } }
$docker=(Get-Command docker -ErrorAction Stop).Source
$node=(Get-Command node -ErrorAction Stop).Source
$server=& $docker version --format '{{.Server.Version}}' 2>$null
if($LASTEXITCODE -ne 0 -or !$server) { throw 'Docker no es accesible. Detenido antes de crear recursos.' }
& $docker image inspect postgis/postgis:16-3.4 *> $null
if($LASTEXITCODE -ne 0) { throw 'Falta la imagen postgis/postgis:16-3.4. No se descarga automaticamente.' }
if(@($DbPort,$ApiPort,$WebPort | Sort-Object -Unique).Count -ne 3 -or @($DbPort,$ApiPort,$WebPort | Where-Object { $_ -le 1024 -or $_ -ge 65536 -or $_ -eq 5174 }).Count) { throw 'Puertos QA invalidos.' }
foreach($p in @($DbPort,$ApiPort,$WebPort)) {
  $listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$p)
  try { $listener.Start() } catch { throw "Puerto $p ocupado o no permitido; no se detiene su proceso." } finally { $listener.Stop() }
}
$sourceHash=(Get-FileHash -LiteralPath $sql -Algorithm SHA256).Hash
if((Get-ChildItem -LiteralPath $migrations -Filter 'V17*').Count) { throw 'V17 activa inesperada. Revisar antes de continuar.' }
# Compilar el checkout actual: el JAR encontrado era anterior a las correcciones.
$oldJava=$env:JAVA_HOME
try {
  $env:JAVA_HOME=Split-Path (Split-Path $java)
  & $maven -s $settings -o -B -ntp -f (Join-Path $repo 'backend/pom.xml') '-Dmaven.test.skip=true' package
  if($LASTEXITCODE -ne 0) { throw 'Empaquetado offline fallido; no se crea la base.' }
} finally { $env:JAVA_HOME=$oldJava }
$jar=Join-Path $repo 'backend/target/backend-0.0.1-SNAPSHOT.jar'
if(!(Test-Path -LiteralPath $jar)) { throw 'No se genero el JAR.' }
$id='notiurba-barrios-qa-'+[DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$runtime=Join-Path ([IO.Path]::GetTempPath()) $id
[IO.Directory]::CreateDirectory($runtime) | Out-Null
$container=$id
$database='notiurba_barrios_qa'
$dbUser='qa_barrios'
$qaPassword=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')
$emptyConfig=Join-Path $runtime 'empty.properties'
[IO.File]::WriteAllText($emptyConfig,'# QA local: no carga application.yml ni credenciales externas.')
function Start-QaProcess([string]$Program,[string[]]$Arguments) {
  $info=[Diagnostics.ProcessStartInfo]::new()
  $info.FileName=$Program; $info.WorkingDirectory=$repo
  $info.UseShellExecute=$false; $info.CreateNoWindow=$true
  # Lista permitida: no heredar DB_*, SPRING_*, RESEND_*, CLOUDINARY_* ni VITE_*.
  $info.Environment.Clear()
  foreach($k in @('SystemRoot','WINDIR','PATH','TEMP','TMP','USERPROFILE','APPDATA','LOCALAPPDATA','PROGRAMDATA','COMSPEC','PATHEXT','SystemDrive','USERNAME','COMPUTERNAME','OS')) {
    $v=[Environment]::GetEnvironmentVariable($k)
    if($null -ne $v) { $info.Environment[$k]=$v }
  }
  foreach($a in $Arguments) { $info.ArgumentList.Add($a) }
  return [Diagnostics.Process]::Start($info)
}
function Read-QaSql([string]$Query) {
  $value=& $docker exec $container psql -X -v ON_ERROR_STOP=1 -U $dbUser -d $database -Atc $Query
  if($LASTEXITCODE -ne 0) { throw 'Consulta QA fallida.' }
  return ($value -join "`n").Trim()
}
# Contenedor y volumen nuevos; no se usan compose, contenedores o volumenes existentes.
& $docker run -d --name $container --label notiurba.scope=barrios-qa-historico --restart no -p "127.0.0.1:${DbPort}:5432" --mount "type=volume,source=$id-data,target=/var/lib/postgresql/data" -e "POSTGRES_DB=$database" -e "POSTGRES_USER=$dbUser" -e "POSTGRES_PASSWORD=$qaPassword" postgis/postgis:16-3.4 | Out-Null
if($LASTEXITCODE -ne 0) { throw 'No se pudo crear el contenedor QA nuevo.' }
$ready=$false
for($i=0;$i -lt 40;$i++) {
  & $docker exec $container pg_isready -U $dbUser -d $database *> $null
  if($LASTEXITCODE -eq 0) { $ready=$true; break }; Start-Sleep -Seconds 2
}
if(!$ready) { throw "PostgreSQL no listo. Se conserva el contenedor $container para diagnostico." }
if((Read-QaSql 'SELECT count(*) FROM information_schema.tables WHERE table_schema=''public'' AND table_name<>''spatial_ref_sys'';') -ne '0') { throw 'La base nueva no esta vacia.' }
$props=[ordered]@{
  'spring.config.location'=('file:'+($emptyConfig -replace '\\','/'))
  'spring.config.import'=''
  'spring.profiles.active'='qa-barrios-visual'
  'spring.application.name'='notiurba-barrios-qa-historico'
  'server.address'='127.0.0.1'; 'server.port'="$ApiPort"
  'spring.datasource.url'="jdbc:postgresql://127.0.0.1:$DbPort/$database"
  'spring.datasource.username'=$dbUser; 'spring.datasource.password'=$qaPassword
  'spring.jpa.open-in-view'='false'; 'spring.jpa.hibernate.ddl-auto'='validate'
  'spring.flyway.enabled'='true'
  'spring.flyway.locations'=('filesystem:'+($migrations -replace '\\','/'))
  'spring.flyway.placeholders.adminPassword'=([Guid]::NewGuid().ToString('N'))
  'spring.servlet.multipart.max-file-size'='1MB'; 'spring.servlet.multipart.max-request-size'='2MB'
  'notiurba.diagnostics.enabled'='false'; 'notiurba.diagnostics.photos-enabled'='false'
  'notiurba.correo.reportes-habilitados'='false'; 'notiurba.correo.destino-prueba'=''
  'notiurba.correo.webhook-secret'=''; 'notiurba.correo.resend.url'='http://127.0.0.1:9'
  'notiurba.correo.resend.api-key'=''; 'notiurba.correo.resend.remitente'='qa@example.test'
  'notiurba.fotos.cloudinary.url'='http://127.0.0.1:9'; 'notiurba.fotos.cloudinary.cloud-name'='qa'
  'notiurba.fotos.cloudinary.api-key'=''; 'notiurba.fotos.cloudinary.api-secret'=''
  'notiurba.fotos.cloudinary.carpeta'='qa-historico'
  'notiurba.cors.allowed-origins'="http://127.0.0.1:$WebPort"
  'notiurba.jwt.secreto'=([Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N'))
  'notiurba.jwt.expiracion-minutos'='60'; 'notiurba.google.client-id'='qa.apps.googleusercontent.com'
  'notiurba.verificacion.enlace'="http://127.0.0.1:$WebPort/app/verificar-correo"
  'notiurba.recuperacion.enlace'="http://127.0.0.1:$WebPort/app/nueva-contrasena"
  'notiurba.recuperacion.mostrar-enlace-en-log'='false'
  'logging.file.name'=((Join-Path $runtime 'backend.log') -replace '\\','/')
}
$javaArgs=@('-jar',$jar)+@($props.GetEnumerator() | ForEach-Object { '--'+$_.Key+'='+$_.Value })
$apiProcess=Start-QaProcess $java $javaArgs
$apiUrl="http://127.0.0.1:$ApiPort/api/catalogos/barrios"
$ready=$false
for($i=0;$i -lt 75;$i++) {
  if($apiProcess.HasExited) { throw "Backend termino. Revisar $runtime/backend.log" }
  try { $initial=Invoke-RestMethod -Uri $apiUrl -TimeoutSec 2; $ready=$true; break } catch { Start-Sleep -Seconds 2 }
}
if(!$ready) { throw "Backend no listo; revisar $runtime/backend.log" }
if($null -ne $initial -and @($initial).Count -ne 0) { throw 'Catalogo inicial inesperado; no se ejecuta V17.' }
if((Read-QaSql 'SELECT count(*) FROM barrio WHERE NOT interno;') -ne '0') { throw 'La base ya tiene barrios publicos.' }
if((Read-QaSql 'SELECT count(*) FROM flyway_schema_history WHERE success;') -ne '16') { throw 'No se verifican las 16 migraciones base.' }
# V17 solo como fixture historico local; nunca se copia a Flyway activo.
$oldEncoding=$OutputEncoding
try {
  $OutputEncoding=[Text.UTF8Encoding]::new($false)
  [IO.File]::ReadAllText($sql,[Text.Encoding]::UTF8) | & $docker exec -i $container psql -X -v ON_ERROR_STOP=1 -U $dbUser -d $database
  if($LASTEXITCODE -ne 0) { throw 'Carga historica QA fallida.' }
} finally { $OutputEncoding=$oldEncoding }
$catalog=@(Invoke-RestMethod -Uri $apiUrl -TimeoutSec 10)
if($catalog.Count -ne 135) { throw "Se esperaban 135 barrios; recibidos $($catalog.Count)." }
$centro=@($catalog | Where-Object nombre -eq 'Centro')
if($centro.Count -ne 1 -or $centro[0].idComuna -ne 4) { throw 'Centro no coincide con la fuente historica.' }
if((Read-QaSql 'SELECT count(*) FROM barrio WHERE interno;') -ne '1') { throw 'Barrio interno inesperado.' }
if((Get-FileHash -LiteralPath $sql -Algorithm SHA256).Hash -ne $sourceHash) { throw 'El archivo original cambio durante la ejecucion.' }
# Configuracion independiente sin leer .env.local ni importar el proxy a Render.
$viteConfig=Join-Path $runtime 'vite.config.mjs'
$frontendUrl=([Uri]((Join-Path $repo 'frontend')+[IO.Path]::DirectorySeparatorChar)).AbsoluteUri
$viteModule=([Uri](Join-Path $repo 'frontend/node_modules/vite/dist/node/index.js')).AbsoluteUri
$reactModule=([Uri](Join-Path $repo 'frontend/node_modules/@vitejs/plugin-react/dist/index.js')).AbsoluteUri
$viteText=@"
import { fileURLToPath } from 'node:url';
import { defineConfig } from '$viteModule';
import react from '$reactModule';
export default defineConfig({root:fileURLToPath('$frontendUrl'),envDir:'$($runtime -replace '\\','/')',plugins:[react()],define:{'import.meta.env.VITE_API_URL':JSON.stringify(''),'import.meta.env.VITE_USAR_MOCKS':JSON.stringify('false')},server:{host:'127.0.0.1',port:$WebPort,strictPort:true,proxy:{'/api':{target:'http://127.0.0.1:$ApiPort',changeOrigin:true}}}});
"@
[IO.File]::WriteAllText($viteConfig,$viteText,[Text.UTF8Encoding]::new($false))
$viteProcess=Start-QaProcess $node @($viteCli,'--config',$viteConfig,'--configLoader','runner')
$qaUrl="http://127.0.0.1:$WebPort/registro"
$ready=$false
for($i=0;$i -lt 30;$i++) {
  if($viteProcess.HasExited) { throw 'Vite QA termino; se conserva el backend y la base para diagnostico.' }
  try { $viaVite=@(Invoke-RestMethod -Uri "http://127.0.0.1:$WebPort/api/catalogos/barrios" -TimeoutSec 2); if($viaVite.Count -eq 135) { $ready=$true; break } } catch {}
  Start-Sleep -Seconds 2
}
if(!$ready) { throw 'Vite no acredita 135 barrios. No se declara exito.' }
$result=[ordered]@{fecha=[DateTimeOffset]::Now.ToString('o');head=$head;url=$qaUrl;api=$apiUrl;dbHost='127.0.0.1';dbPort=$DbPort;dbName=$database;container=$container;volume="$id-data";apiPid=$apiProcess.Id;vitePid=$viteProcess.Id;barrios=$catalog.Count;centro=$centro[0];sqlSha256=$sourceHash;jarSha256=(Get-FileHash $jar -Algorithm SHA256).Hash;runtime=$runtime;mocks=$false;visualChrome='pendiente';NOT71='no terminada';alcance='QA local: PMGR 2020 historico no aprobado; no produccion'}
$evidence=Join-Path $repo 'docs/validacion/qa-barrios-recuperacion'
[IO.Directory]::CreateDirectory($evidence) | Out-Null
$result | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $evidence "$id.json") -Encoding utf8
Write-Host "QA preparada: $qaUrl"
Write-Host "API local: $apiUrl (135 barrios). Centro: $($centro[0].idBarrio)."
Write-Host 'Falta la comprobacion interactiva en Chrome. NOT-71 NO se declara terminada.'
Write-Host "Contenedor nuevo conservado: $container. Configuracion temporal: $runtime"
