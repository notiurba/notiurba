# Ejecutar desde PowerShell local. No lee .env ni usa bases existentes.
[CmdletBinding()]
param(
    [string]$JavaHome = 'C:\notiurba\validation-tools\jdk21\jdk-21.0.13+9',
    [string]$MavenCommand = 'C:\Users\Usuario\.m2\wrapper\dists\apache-maven-3.9.9\8e74001100ff70d6af083c5511fcc5ec49282d7017cde82c3698eee8fdf86698\bin\mvn.cmd',
    [string]$DependencyCache = 'C:\notiurba\validation-tools\m2'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (Test-Path variable:PSNativeCommandUseErrorActionPreference) { $PSNativeCommandUseErrorActionPreference = $false }
$repo = Split-Path $PSScriptRoot -Parent
if ([IO.Path]::GetFullPath($repo).TrimEnd('\') -ne 'C:\notiurba\noti-urba') { throw 'Directorio incorrecto.' }
Set-Location -LiteralPath $repo
if ((& git branch --show-current) -ne 'feat/admin-estados-vigencia') { throw 'Rama incorrecta.' }
if ((& "$JavaHome\bin\java.exe" --version | Out-String) -notmatch '^(openjdk|java) 21\.') { throw 'Se requiere Java 21.' }
if ($env:DOCKER_HOST -and $env:DOCKER_HOST -notmatch '^npipe:') { throw 'DOCKER_HOST no es local.' }
$endpoint = (& docker context inspect desktop-linux --format '{{.Endpoints.docker.Host}}' | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $endpoint -notmatch '^npipe:') { throw 'Se requiere Docker Desktop local.' }
& docker --context desktop-linux version
if ($LASTEXITCODE -ne 0) { throw 'Motor Docker inaccesible; no se creó ningún contenedor.' }
if ((& docker --context desktop-linux info --format '{{.OSType}}') -ne 'linux') { throw 'Se requiere el motor Linux.' }
$variables = @('DB_URL','DB_USER','DB_PASSWORD','NOTIURBA_TEST_DB_URL','NOTIURBA_TEST_DB_USER',
    'NOTIURBA_TEST_DB_PASSWORD','JAVA_HOME','PATH')
$anteriores = @{}
foreach ($nombre in $variables) { $anteriores[$nombre] = [Environment]::GetEnvironmentVariable($nombre,'Process') }
$id = [Guid]::NewGuid().ToString('N')
$contenedor = "notiurba-admin-$id"
$creado = $false
try {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
    foreach ($nombre in $variables | Where-Object { $_ -match '^(DB_|NOTIURBA_TEST_DB_)' }) {
        [Environment]::SetEnvironmentVariable($nombre,$null,'Process')
    }
    $clave = [Guid]::NewGuid().ToString('N')
    $null = & docker --context desktop-linux run --detach --rm --name $contenedor --label "notiurba.admin-validation=$id" `
        --publish '127.0.0.1::5432' --env 'POSTGRES_USER=admin_test' --env "POSTGRES_PASSWORD=$clave" `
        --env 'POSTGRES_DB=notiurba_test_admin_bootstrap' postgis/postgis:16-3.4
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el contenedor desechable.' }
    $creado = $true
    $listo = $false
    for ($i=0; $i -lt 45; $i++) {
        $null = & docker --context desktop-linux exec $contenedor pg_isready -U admin_test -d notiurba_test_admin_bootstrap 2>$null
        if ($LASTEXITCODE -eq 0) { $listo=$true; break }
        Start-Sleep -Seconds 1
    }
    if (!$listo) { throw 'PostGIS no quedó disponible.' }
    $publicacion = (& docker --context desktop-linux port $contenedor 5432/tcp | Out-String).Trim()
    if ($publicacion -notmatch '^127\.0\.0\.1:(\d+)$') { throw 'Puerto no limitado a localhost.' }
    $env:DB_URL = "jdbc:postgresql://127.0.0.1:$($Matches[1])/notiurba_test_admin_bootstrap"
    $env:DB_USER = 'admin_test'
    $env:DB_PASSWORD = $clave
    $argumentos = @('-B',"-Dmaven.repo.local=$DependencyCache",'-f',"$repo\backend\pom.xml")
    $agente = Join-Path $DependencyCache 'net\bytebuddy\byte-buddy-agent\1.14.19\byte-buddy-agent-1.14.19.jar'
    if (Test-Path -LiteralPath $agente) { $argumentos += "-DargLine=-javaagent:$agente" }
    $inicioAdmin = Get-Date
    & $MavenCommand @argumentos '-Dtest=AdminControllerTest,AdminIntegracionTest,ConfiguracionIntegracionTest' test
    $falloAdmin = $LASTEXITCODE
    foreach ($clase in @('AdminControllerTest','AdminIntegracionTest','ConfiguracionIntegracionTest')) {
        $informe = "$repo\backend\target\surefire-reports\TEST-com.notiurba.admin.$clase.xml"
        if (!(Test-Path -LiteralPath $informe)) { $falloAdmin=1; continue }
        if ((Get-Item -LiteralPath $informe).LastWriteTime -lt $inicioAdmin) { $falloAdmin=1; continue }
        Copy-Item -LiteralPath $informe -Destination "$repo\backend\target\admin-local-$clase.xml"
        [xml]$resultado = Get-Content -Raw $informe
        if ([int]$resultado.testsuite.tests -eq 0 -or [int]$resultado.testsuite.skipped -gt 0 `
            -or [int]$resultado.testsuite.errors -gt 0 -or [int]$resultado.testsuite.failures -gt 0) { $falloAdmin=1 }
    }
    # Ejecutar también la suite general aunque haya errores administrativos; conservar ambos resultados.
    foreach ($nombre in @('DB_URL','DB_USER','DB_PASSWORD')) { [Environment]::SetEnvironmentVariable($nombre,$null,'Process') }
    & $MavenCommand @argumentos package
    $falloGeneral = $LASTEXITCODE
    if ($falloAdmin -ne 0 -or $falloGeneral -ne 0) { throw "Validación incompleta: administración=$falloAdmin; suite general=$falloGeneral. Consulta Surefire y admin-local-*.xml." }
    Write-Output 'Pruebas administrativas reales, suite general y empaquetado aprobados. No se hizo commit ni push.'
} finally {
    if ($creado) {
        $etiqueta = (& docker --context desktop-linux inspect --format '{{index .Config.Labels "notiurba.admin-validation"}}' $contenedor | Out-String).Trim()
        if ($etiqueta -eq $id) { $null = & docker --context desktop-linux rm --force $contenedor }
        else { Write-Warning "No se pudo verificar la etiqueta para limpiar $contenedor." }
    }
    foreach ($nombre in $variables) { [Environment]::SetEnvironmentVariable($nombre,$anteriores[$nombre],'Process') }
}
