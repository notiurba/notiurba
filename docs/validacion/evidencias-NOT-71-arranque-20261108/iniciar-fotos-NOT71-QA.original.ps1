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
# Solo la clave del contenedor QA verificado; no se consulta .env ni se imprime Config.Env.
$containerEnv=(& $docker inspect --format '{{json .Config.Env}}' $container) | ConvertFrom-Json
if($LASTEXITCODE -ne 0) { throw 'No se pudo leer la configuracion del contenedor QA.' }
$qaPassword=@($containerEnv|Where-Object { $_.StartsWith('POSTGRES_PASSWORD=') })
if($qaPassword.Count -ne 1) { throw 'Falta clave PostgreSQL QA.' }
$qaPassword=$qaPassword[0].Substring('POSTGRES_PASSWORD='.Length)
$runtime=Join-Path ([IO.Path]::GetTempPath()) ('not71-fotos-'+[DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6))
[IO.Directory]::CreateDirectory($runtime)|Out-Null
function Start-LocalQa([string]$Program,[string[]]$Arguments,[hashtable]$Extra) {
  $i=[Diagnostics.ProcessStartInfo]::new();$i.FileName=$Program;$i.WorkingDirectory=$repo
  $i.UseShellExecute=$false;$i.CreateNoWindow=$true;$i.Environment.Clear()
  foreach($k in @('SystemRoot','WINDIR','PATH','TEMP','TMP','USERPROFILE','APPDATA','LOCALAPPDATA','PROGRAMDATA','COMSPEC','PATHEXT','SystemDrive','USERNAME','COMPUTERNAME','OS')) {
    $v=[Environment]::GetEnvironmentVariable($k);if($null-ne $v){$i.Environment[$k]=$v}
  }
  foreach($k in $Extra.Keys){$i.Environment[$k]=$Extra[$k]}
  foreach($arg in $Arguments){$i.ArgumentList.Add($arg)}
  [Diagnostics.Process]::Start($i)
}
$classes=([Uri]((Join-Path $repo 'backend/target/test-classes')+[IO.Path]::DirectorySeparatorChar)).AbsoluteUri
$apiProcess=Start-LocalQa $java @("-Dloader.path=$classes",'-Dloader.main=com.notiurba.qa.Not71ServidorQA','-cp',$jar,'org.springframework.boot.loader.launch.PropertiesLauncher') @{
 DB_URL='jdbc:postgresql://127.0.0.1:15435/notiurba_barrios_qa';DB_USER='qa_barrios';DB_PASSWORD=$qaPassword;NOT71_QA_STORAGE_DIR=$runtime
}
$qaPassword=$null;$containerEnv=$null
$ready=$false
for($n=0;$n-lt 60;$n++){
 if($apiProcess.HasExited){throw "El backend termino; revisar $runtime/backend.log"}
 try{$r=Invoke-WebRequest http://127.0.0.1:18092/api/catalogos/tipos-documento -TimeoutSec 2;if($r.StatusCode-eq 200){$ready=$true;break}}catch{}
 Start-Sleep -Seconds 2
}
if(!$ready){throw "Backend no listo; revisar $runtime/backend.log"}
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
$vite=Start-LocalQa $node @((Join-Path $repo 'frontend/node_modules/vite/bin/vite.js'),'--config',$viteConfig,'--configLoader','runner') @{}
$ready=$false
for($n=0;$n-lt 30;$n++){
 if($vite.HasExited){throw 'Vite QA termino; se conserva la base y el backend para diagnostico.'}
 try{$r=Invoke-WebRequest http://127.0.0.1:5176/login -TimeoutSec 2;if($r.StatusCode-eq 200){$ready=$true;break}}catch{}
 Start-Sleep -Seconds 2
}
if(!$ready){throw 'Vite no esta listo.'}
$result=@{fecha=[DateTimeOffset]::Now.ToString('o');api='http://127.0.0.1:18092';frontend='http://127.0.0.1:5176/login';fotos='http://127.0.0.1:19027/fotos/';storage=$runtime;apiPid=$apiProcess.Id;vitePid=$vite.Id;database='notiurba_barrios_qa';container=$container;correoHabilitado=$false;flywayHabilitado=$false;fotoSubidaProbada=$false;reporteExistente='NU-000001';estadoNOT71='NO_APROBADA'}
$out=Join-Path $repo 'docs/validacion/evidencias-NOT-71-reporte-20261108'
[IO.Directory]::CreateDirectory($out)|Out-Null
$result|ConvertTo-Json -Depth 4|Set-Content (Join-Path $out ('almacen-local-'+[DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'.json')) -Encoding utf8
Write-Host 'Adaptador local iniciado. Abrir http://127.0.0.1:5176/login con la cuenta QA ya verificada.'
Write-Host 'Reintentar la foto sobre NU-000001; no crear otro reporte. El arranque no acredita la subida.'
Write-Host '5175/18091 y todos los contenedores existentes siguen intactos. Sin correo externo ni migraciones.'
