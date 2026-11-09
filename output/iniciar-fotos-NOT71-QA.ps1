#requires -Version 7.0
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if((git -C $repo branch --show-current).Trim() -ne 'fix/registro-pr20') { throw 'Rama incorrecta.' }
$docker=(Get-Command docker -ErrorAction Stop).Source
$container='notiurba-barrios-qa-20261108-153240-458c80'
$version=& $docker version --format '{{.Server.Version}}' 2>$null
if($LASTEXITCODE -ne 0 -or !$version) { throw 'Docker inaccesible. No se ha iniciado ni modificado ningun servicio.' }
$portsJson=& $docker inspect --format '{{json .NetworkSettings.Ports}}' $container
if($LASTEXITCODE -ne 0) { throw 'No se encontro el contenedor QA autorizado.' }
$map=@(($portsJson|ConvertFrom-Json).'5432/tcp')
if($map.Count -ne 1 -or $map[0].HostIp -ne '127.0.0.1' -or $map[0].HostPort -ne '15435') { throw 'Mapeo distinto del QA aislado.' }
$identity=& $docker exec $container psql -X -qAt -v ON_ERROR_STOP=1 -U qa_barrios -d notiurba_barrios_qa -c 'SELECT current_database() || ''|'' || current_user;'
if($LASTEXITCODE -ne 0 -or ($identity -join '').Trim() -ne 'notiurba_barrios_qa|qa_barrios') { throw 'Identidad SQL incorrecta.' }
foreach($p in @(18092,19027,5176)) {
  $listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$p)
  try { $listener.Start() } catch { throw "Puerto $p ocupado. No se detiene ningun proceso existente." } finally { $listener.Stop() }
}
$java='C:\notiurba\validation-tools\jdk21\jdk-21.0.13+9\bin\java.exe'
$jar=Join-Path $repo 'backend/target/backend-0.0.1-SNAPSHOT.jar'
$testClass=Join-Path $repo 'backend/target/test-classes/com/notiurba/qa/Not71ServidorQA.class'
$source=Join-Path $repo 'backend/src/test/java/com/notiurba/qa/Not71ServidorQA.java'
foreach($p in @($java,$jar,$testClass)) { if(!(Test-Path -LiteralPath $p)) { throw "Falta dependencia compilada: $p" } }
if((Get-Item $testClass).LastWriteTimeUtc -lt (Get-Item $source).LastWriteTimeUtc) { throw 'Compilar el harness con Maven test-compile antes de iniciar.' }
# Verificar el launcher dentro del JAR sin ejecutar ni reempaquetar la aplicacion.
$zip=[IO.Compression.ZipFile]::OpenRead($jar)
try {
 if(!$zip.GetEntry('org/springframework/boot/loader/launch/PropertiesLauncher.class') -or !$zip.GetEntry('BOOT-INF/classes/com/notiurba/NotiUrbaApplication.class')) { throw 'JAR Spring Boot incompatible.' }
} finally {$zip.Dispose()}
# Solo la clave del contenedor QA verificado; no se consulta .env ni se imprime Config.Env.
$containerEnv=(& $docker inspect --format '{{json .Config.Env}}' $container) | ConvertFrom-Json
if($LASTEXITCODE -ne 0) { throw 'No se pudo leer la configuracion del contenedor QA.' }
$qaPassword=@($containerEnv|Where-Object { $_.StartsWith('POSTGRES_PASSWORD=') })
if($qaPassword.Count -ne 1) { throw 'Falta clave PostgreSQL QA.' }
$qaPassword=$qaPassword[0].Substring('POSTGRES_PASSWORD='.Length)
$runtime=Join-Path ([IO.Path]::GetTempPath()) ('not71-fotos-'+[DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6))
[IO.Directory]::CreateDirectory($runtime)|Out-Null
Write-Host "Diagnostico real: $runtime"
# NO incluir target/test-classes completo: contiene configuraciones de otras pruebas
# que excluyen DataSourceAutoConfiguration e impiden crear JdbcTemplate.
$isolated=Join-Path $runtime 'classes/com/notiurba/qa'
[IO.Directory]::CreateDirectory($isolated)|Out-Null
Get-ChildItem (Split-Path $testClass) -Filter 'Not71ServidorQA*.class' | Copy-Item -Destination $isolated
function Start-LocalQa([string]$Program,[string[]]$Arguments,[hashtable]$Extra,[string]$LogName) {
  $i=[Diagnostics.ProcessStartInfo]::new();$i.FileName=$Program;$i.WorkingDirectory=$repo
  $i.UseShellExecute=$false;$i.CreateNoWindow=$true;$i.Environment.Clear()
  $i.RedirectStandardOutput=$true;$i.RedirectStandardError=$true
  foreach($k in @('SystemRoot','WINDIR','PATH','TEMP','TMP','USERPROFILE','APPDATA','LOCALAPPDATA','PROGRAMDATA','COMSPEC','PATHEXT','SystemDrive','USERNAME','COMPUTERNAME','OS')) {
    $v=[Environment]::GetEnvironmentVariable($k);if($null-ne $v){$i.Environment[$k]=$v}
  }
  foreach($k in $Extra.Keys){$i.Environment[$k]=$Extra[$k]}
  foreach($arg in $Arguments){$i.ArgumentList.Add($arg)}
  $outPath=Join-Path $runtime "$LogName.stdout.log";$errPath=Join-Path $runtime "$LogName.stderr.log"
  # Los archivos existen incluso si Java/PropertiesLauncher falla antes de Spring.
  $outFile=[IO.File]::Open($outPath,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::ReadWrite)
  $errFile=[IO.File]::Open($errPath,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::ReadWrite)
  try {
    $process=[Diagnostics.Process]::Start($i)
    $outTask=$process.StandardOutput.BaseStream.CopyToAsync($outFile)
    $errTask=$process.StandardError.BaseStream.CopyToAsync($errFile)
    return @{Process=$process;OutTask=$outTask;ErrTask=$errTask;OutFile=$outFile;ErrFile=$errFile;OutPath=$outPath;ErrPath=$errPath}
  } catch {
    $outFile.Dispose();$errFile.Dispose()
    [IO.File]::AppendAllText($errPath,"No se pudo iniciar el ejecutable local. $($_.Exception.GetType().Name)")
    throw "Fallo de lanzamiento; diagnostico en $outPath y $errPath"
  }
}
function Complete-ExitedLog($launch) {
  $null=$launch.OutTask.GetAwaiter().GetResult();$null=$launch.ErrTask.GetAwaiter().GetResult()
  $launch.OutFile.Dispose();$launch.ErrFile.Dispose()
}
$classes=([Uri]((Join-Path $runtime 'classes')+[IO.Path]::DirectorySeparatorChar)).AbsoluteUri
$apiLaunch=Start-LocalQa $java @("-Dlogging.file.name=$(Join-Path $runtime 'backend.log')","-Dloader.path=$classes",'-Dloader.main=com.notiurba.qa.Not71ServidorQA','-cp',$jar,'org.springframework.boot.loader.launch.PropertiesLauncher') @{
 DB_URL='jdbc:postgresql://127.0.0.1:15435/notiurba_barrios_qa';DB_USER='qa_barrios';DB_PASSWORD=$qaPassword;NOT71_QA_STORAGE_DIR=$runtime
} 'backend'
$apiProcess=$apiLaunch.Process
$qaPassword=$null;$containerEnv=$null
$ready=$false
for($n=0;$n-lt 60;$n++){
 if($apiProcess.HasExited){Complete-ExitedLog $apiLaunch;throw "Backend termino (exit $($apiProcess.ExitCode)); revisar $runtime/backend.stderr.log y backend.stdout.log; Spring: backend.log"}
 try{$r=Invoke-WebRequest http://127.0.0.1:18092/api/catalogos/tipos-documento -TimeoutSec 2;if($r.StatusCode-eq 200 -and (Test-Path (Join-Path $runtime 'ready.json'))){$ready=$true;break}}catch{}
 Start-Sleep -Seconds 2
}
if(!$ready){$apiLaunch.OutFile.Flush();$apiLaunch.ErrFile.Flush();throw "Backend no listo; revisar $runtime/backend.stdout.log, backend.stderr.log y backend.log. No se detienen servicios."}
$rootUri=([Uri]((Join-Path $repo 'frontend')+[IO.Path]::DirectorySeparatorChar)).AbsoluteUri
$viteModule=([Uri](Join-Path $repo 'frontend/node_modules/vite/dist/node/index.js')).AbsoluteUri
$reactModule=([Uri](Join-Path $repo 'frontend/node_modules/@vitejs/plugin-react/dist/index.js')).AbsoluteUri
$runtimeJs=$runtime.Replace('\','/')
$viteConfig=Join-Path $runtime 'vite.config.mjs'
$content=@"
import {fileURLToPath} from 'node:url';
import {defineConfig} from '$viteModule';
import react from '$reactModule';
export default defineConfig({root:fileURLToPath('$rootUri'),envDir:'$runtimeJs',cacheDir:'$runtimeJs/cache',plugins:[react()],define:{'import.meta.env.VITE_API_URL':JSON.stringify(''),'import.meta.env.VITE_USAR_MOCKS':JSON.stringify('false')},server:{host:'127.0.0.1',port:5176,strictPort:true,proxy:{'/api':{target:'http://127.0.0.1:18092',changeOrigin:true}}}});
"@
[IO.File]::WriteAllText($viteConfig,$content,[Text.UTF8Encoding]::new($false))
$node=(Get-Command node -ErrorAction Stop).Source
$viteLaunch=Start-LocalQa $node @((Join-Path $repo 'frontend/node_modules/vite/bin/vite.js'),'--config',$viteConfig,'--configLoader','runner') @{} 'vite'
$vite=$viteLaunch.Process
$ready=$false
for($n=0;$n-lt 30;$n++){
 if($vite.HasExited){Complete-ExitedLog $viteLaunch;throw "Vite QA termino; revisar $runtime/vite.stderr.log y vite.stdout.log. Se conserva la base y el backend."}
 try{$r=Invoke-WebRequest http://127.0.0.1:5176/login -TimeoutSec 2;if($r.StatusCode-eq 200){$ready=$true;break}}catch{}
 Start-Sleep -Seconds 2
}
if(!$ready){throw 'Vite no esta listo.'}
$result=@{fecha=[DateTimeOffset]::Now.ToString('o');api='http://127.0.0.1:18092';frontend='http://127.0.0.1:5176/login';fotos='http://127.0.0.1:19027/fotos/';storage=$runtime;apiPid=$apiProcess.Id;vitePid=$vite.Id;database='notiurba_barrios_qa';container=$container;correoHabilitado=$false;flywayHabilitado=$false;fotoSubidaProbada=$false;reporteExistente='NU-000001';estadoNOT71='NO_APROBADA'}
$out=Join-Path $repo 'docs/validacion/evidencias-NOT-71-reporte-20261108'
[IO.Directory]::CreateDirectory($out)|Out-Null
$result|ConvertTo-Json -Depth 4|Set-Content (Join-Path $out ('almacen-local-'+[DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'.json')) -Encoding utf8
Write-Host 'Adaptador local iniciado. Abrir http://127.0.0.1:5176/login con la cuenta QA ya verificada.'
Write-Host '5175/18091 y todos los contenedores existentes siguen intactos. Sin correo externo ni migraciones.'
# Continuacion automatica: solo el reporte y la cuenta sinteticos existentes.
$proofFile=Join-Path $out ('foto-local-'+[DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'.json')
$proof=@{fecha=[DateTimeOffset]::Now.ToString('o');api='http://127.0.0.1:18092';codigo='NU-000001';estado='PENDIENTE';NOT71='NO_APROBADA'}
try {
 $photoDir=Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Temp/not71-foto-internet-20261108'
 [IO.Directory]::CreateDirectory($photoDir)|Out-Null
 $photo=Join-Path $photoDir 'Pothole_Small.jpg'
 if(!(Test-Path -LiteralPath $photo)) {
   Invoke-WebRequest 'https://upload.wikimedia.org/wikipedia/commons/e/eb/Pothole_Small.jpg' -OutFile $photo -TimeoutSec 30
 }
 $expectedHash='761d2113cbbccd6600366f6885f1cc1e656d33476780628150878faf178b6b67'
 if((Get-Item $photo).Length -ne 84460 -or (Get-FileHash -LiteralPath $photo -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash){throw 'El JPEG no coincide con el original aprobado.'}
 $proof.foto=@{bytes=84460;sha256=$expectedHash;source='https://commons.wikimedia.org/wiki/File:Pothole_Small.jpg';licencia='PD-self; Uncl3dad'}
 $login=Invoke-WebRequest 'http://127.0.0.1:18092/api/auth/login' -Method Post -ContentType 'application/json' -Body (@{correo='qa.not71.20261108.2147@example.test';password='QaNot71-2026-2147!'}|ConvertTo-Json -Compress) -SkipHttpErrorCheck -MaximumRedirection 0 -TimeoutSec 15
 $proof.loginStatus=[int]$login.StatusCode
 if($login.StatusCode -ne 200){throw 'Login de la cuenta QA existente no aprobado.'}
 $jwt=($login.Content|ConvertFrom-Json).token
 if(!$jwt){throw 'Login sin credencial de sesion.'}
 $headers=@{Authorization="Bearer $jwt"}
 $before=Invoke-RestMethod 'http://127.0.0.1:18092/api/reportes/NU-000001' -Headers $headers -MaximumRedirection 0 -TimeoutSec 15
 $localUrl="http://127.0.0.1:19027/fotos/$expectedHash.jpg"
 if(@($before.fotos|Where-Object {$_.url -eq $localUrl}).Count -eq 0){
   if(@($before.fotos).Count -ne 0){throw 'Existen fotos distintas: se conservan y se detiene antes de duplicarlas.'}
   $upload=Invoke-WebRequest 'http://127.0.0.1:18092/api/reportes/NU-000001/fotos' -Method Post -Headers $headers -Form @{foto=Get-Item -LiteralPath $photo} -SkipHttpErrorCheck -MaximumRedirection 0 -TimeoutSec 30
   $proof.uploadStatus=[int]$upload.StatusCode
   if($upload.StatusCode -ne 201){$proof.errorApi=($upload.Content|ConvertFrom-Json).codigo;throw 'La API no aprobo la subida de la foto.'}
 } else {$proof.reutilizada=$true}
 $after=Invoke-RestMethod 'http://127.0.0.1:18092/api/reportes/NU-000001' -Headers $headers -MaximumRedirection 0 -TimeoutSec 15
 if(@($after.fotos|Where-Object {$_.url -eq $localUrl}).Count -ne 1){throw 'La foto no figura exactamente una vez en el reporte.'}
 $proof.reporteHttp=$after
 $retrieved=Join-Path $runtime 'foto-recuperada.jpg'
 Invoke-WebRequest $localUrl -OutFile $retrieved -MaximumRedirection 0 -TimeoutSec 15
 if((Get-FileHash -LiteralPath $retrieved -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash){throw 'Los bytes recuperados no coinciden.'}
 $proof.urlRecuperable=$localUrl;$proof.hashRecuperado=$expectedHash
 $sqlProof=& (Join-Path $PSScriptRoot 'consultar-reporte-NOT71-QA.ps1') -PassThru
 if(@($sqlProof.persistencia.reporte.fotos|Where-Object {$_.url -eq $localUrl -and $_.idUsuario -eq 2}).Count -ne 1){throw 'La foto no quedo asociada al reporte y usuario esperados en SQL.'}
 $proof.sql=$sqlProof;$proof.estado='APROBADO_FOTO_Y_PERSISTENCIA_QA'
 Write-Host 'Foto QA aprobada: POST/consulta/URL y SHA256; SQL de NU-000001 y foto comprobados. No implica aprobacion productiva de NOT-71.'
} catch {
 $proof.estado='BLOQUEADO';$proof.tipoError=$_.Exception.GetType().Name
 $proof.motivo='La comprobacion no finalizo; revisar estados HTTP registrados y logs locales. Se omiten mensajes de excepcion para no exponer credenciales.'
 Write-Host "Validacion pendiente. Evidencia: $proofFile. Diagnostico: $runtime"
 # No volcar respuestas de login, cabeceras, tokens, variables ni excepciones completas.
 throw 'La validacion automatica QA se detuvo; revisar la evidencia sanitizada y los logs locales.'
} finally {
 $jwt=$null;$headers=$null;$login=$null
 $proof|ConvertTo-Json -Depth 14|Set-Content -LiteralPath $proofFile -Encoding utf8
}
Write-Host "Evidencia foto/HTTP/SQL: $proofFile"
Write-Host 'Confirmacion visual pendiente: http://127.0.0.1:5176/reportar/enviado/NU-000001'
