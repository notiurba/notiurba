# Ejecutar desde PowerShell de Windows con acceso al motor Docker Desktop Linux.
# No usa docker compose, .env, bases remotas ni contenedores preexistentes.
[CmdletBinding()]
param(
    [string]$Worktree = 'C:\notiurba\enrutamiento-correos',
    [string]$JavaHome = 'C:\notiurba\validation-tools\jdk21\jdk-21.0.13+9',
    [string]$MavenCommand = 'C:\Users\Usuario\.m2\wrapper\dists\apache-maven-3.9.9\8e74001100ff70d6af083c5511fcc5ec49282d7017cde82c3698eee8fdf86698\bin\mvn.cmd',
    [string]$MavenSettings = 'C:\notiurba\validation-tools\maven-settings.xml',
    [string]$DependencyCache = 'C:\notiurba\validation-tools\m2'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
# PowerShell 7.3+: los codigos nativos se comprueban explicitamente.
if (Test-Path variable:PSNativeCommandUseErrorActionPreference) { $PSNativeCommandUseErrorActionPreference = $false }

$expected = @(
    'catorceRutasConservanIdsYMunicipalesVanSoloAlCaic',
    'gasEmergenteYCanalesAlternosNoLlamanProveedor',
    'pendienteAceptadoYReintentoNoCreanReportesNiDuplicanCorreos',
    'falloConservaReporteYReintentoReutilizaMensajeYClave',
    'propiedadImpideConsultarOEnviarReporteAjeno',
    'reboteFirmadoNoRetrocedeHistorialYEsIdempotente',
    'colaDeshabilitadaConservaPendienteSinLlamarAlProveedor',
    'conservaPendientesHistoricosYElTriggerSincronizaReporte'
)
$branch = (& git -C $Worktree branch --show-current).Trim()
if ($LASTEXITCODE -ne 0 -or $branch -ne 'feat/enrutamiento-correos-notiurba') { throw 'Worktree o rama incorrectos.' }
& git -C $Worktree merge-base --is-ancestor 5a4faba HEAD
if ($LASTEXITCODE -ne 0) { throw 'El commit de referencia 5a4faba no es antecesor del HEAD.' }
$dirty = @(& git -C $Worktree status --porcelain)
if ($LASTEXITCODE -ne 0 -or $dirty.Count -gt 0) { throw 'El worktree debe estar limpio antes de comenzar.' }
$headCommit = (& git -C $Worktree rev-parse HEAD).Trim()

$java = Join-Path $JavaHome 'bin\java.exe'
foreach ($required in @($java, $MavenCommand, $MavenSettings)) {
    if (!(Test-Path -LiteralPath $required -PathType Leaf)) { throw "Falta herramienta o configuracion: $required" }
}
$javaVersion = (& $java --version | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '^(openjdk|java) 21\.') { throw 'Se requiere Java 21.' }
if ($env:DOCKER_HOST -and $env:DOCKER_HOST -notmatch '^npipe:') { throw 'DOCKER_HOST apunta fuera de la tuberia local de Windows.' }
$context = (& docker context show | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $context -ne 'desktop-linux') { throw 'Se requiere el contexto local desktop-linux; no se cambiara automaticamente.' }
$endpoint = (& docker context inspect desktop-linux --format '{{.Endpoints.docker.Host}}' | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $endpoint -notmatch '^npipe:') { throw 'El endpoint de Docker Desktop debe ser una tuberia local.' }
& docker version
if ($LASTEXITCODE -ne 0) { throw 'Docker no es accesible desde esta terminal; no se ha creado ningun contenedor.' }
$dockerOs = (& docker info --format '{{.OSType}}').Trim()
if ($LASTEXITCODE -ne 0 -or $dockerOs -ne 'linux') { throw 'Se requiere el motor Docker Linux.' }

$agent = Join-Path $DependencyCache 'net\bytebuddy\byte-buddy-agent\1.14.19\byte-buddy-agent-1.14.19.jar'
$driver = Join-Path $DependencyCache 'org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar'
foreach ($required in @($agent, $driver)) {
    if (!(Test-Path -LiteralPath $required -PathType Leaf)) { throw "Falta dependencia oficial en la cache: $required" }
}

$runId = [Guid]::NewGuid().ToString('N')
$containerName = "notiurba-jdbc-$runId"
$database = "notiurba_test_$runId"
$testUser = 'jdbc_test'
$label = "notiurba.jdbc-audit=$runId"
$outputDir = Join-Path $Worktree "backend\target\jdbc-local-$runId"
$reportDir = Join-Path $outputDir 'surefire'
$temporaryPom = Join-Path $Worktree "backend\pom-jdbc-test-$runId.xml"
$probeFile = Join-Path $outputDir 'JdbcProbe.java'
$containerId = $null
$cleanupFailed = $false
$failure = $null
$stage = 'Preparacion'
$mavenExit = $null
$rows = @()
$envNames = @('JAVA_HOME','PATH','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','MAVEN_OPTS','MAVEN_ARGS',
    'POSTGRES_DB','POSTGRES_USER','POSTGRES_PASSWORD',
    'NOTIURBA_TEST_DB_URL','NOTIURBA_TEST_DB_USER','NOTIURBA_TEST_DB_PASSWORD',
    'NOTIURBA_ENVIO_REPORTES_HABILITADO','RESEND_API_KEY','RESEND_REMITENTE',
    'RESEND_PRUEBA_DESTINO','RESEND_WEBHOOK_SECRET','DB_URL','DB_USER','DB_PASSWORD',
    'SPRING_PROFILES_ACTIVE','SPRING_APPLICATION_JSON','SPRING_CONFIG_LOCATION',
    'SPRING_CONFIG_ADDITIONAL_LOCATION','SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME',
    'SPRING_DATASOURCE_PASSWORD','NOTIURBA_CORREO_REPORTES_HABILITADOS')
$savedEnv = @{}
foreach ($name in $envNames) { $savedEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
    foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $null, 'Process') }
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$(Join-Path $JavaHome 'bin');$($savedEnv['PATH'])"
    $env:MAVEN_OPTS = "-javaagent:`"$agent`""
    $env:NOTIURBA_ENVIO_REPORTES_HABILITADO = 'false'
    $env:NOTIURBA_CORREO_REPORTES_HABILITADOS = 'false'
    $env:POSTGRES_DB = $database
    $env:POSTGRES_USER = $testUser
    # Secreto aleatorio de esta ejecucion; no se escribe en archivos ni en argumentos.
    $env:POSTGRES_PASSWORD = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    & $MavenCommand -version
    if ($LASTEXITCODE -ne 0) { throw 'Maven no puede iniciarse con Java 21.' }

    $stage = 'Docker'
    # La imagen y versiones coinciden con docker-compose.yml; no se usa su servicio db.
    # Docker elige un puerto libre, ligado solo a loopback. Los datos viven en tmpfs.
    $created = @(& docker run --detach --name $containerName --label $label `
        --publish '127.0.0.1::5432' --tmpfs '/var/lib/postgresql/data:rw,size=512m' `
        --env POSTGRES_DB --env POSTGRES_USER --env POSTGRES_PASSWORD 'postgis/postgis:16-3.4')
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el contenedor aislado.' }
    $containerId = ($created | Select-Object -Last 1).Trim()
    if ($containerId -notmatch '^[a-f0-9]{64}$') { throw 'Docker no devolvio un ID de contenedor valido.' }
    $binding = (& docker port $containerId '5432/tcp').Trim()
    if ($LASTEXITCODE -ne 0 -or $binding -notmatch '^127\.0\.0\.1:(\d+)$') { throw 'Publicacion de puerto inesperada; no se conectara a otra base.' }
    $localPort = [int]$Matches[1]
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        & docker exec $containerId pg_isready -h 127.0.0.1 -U $testUser -d $database *> $null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw 'PostgreSQL no estuvo listo en 60 segundos.' }
    $identity = (& docker exec $containerId psql -X -v ON_ERROR_STOP=1 -U $testUser -d $database -Atc 'SELECT current_database()' | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $identity -ne $database) { throw 'Identidad inesperada de la base.' }
    $tables = (& docker exec $containerId psql -X -v ON_ERROR_STOP=1 -U $testUser -d $database -Atc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('usuario','reporte','flyway_schema_history')" | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $tables -ne '0') { throw 'La base contiene tablas de NotiUrba preexistentes.' }
    & docker exec $containerId psql -X -v ON_ERROR_STOP=1 -U $testUser -d $database -c 'CREATE EXTENSION IF NOT EXISTS postgis; CREATE EXTENSION IF NOT EXISTS pgcrypto;'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron preparar las extensiones en la base nueva.' }

    $env:NOTIURBA_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$localPort/${database}?connectTimeout=10&socketTimeout=30"
    $env:NOTIURBA_TEST_DB_USER = $testUser
    $env:NOTIURBA_TEST_DB_PASSWORD = $env:POSTGRES_PASSWORD
    $stage = 'Conexion JDBC'
    $probeSource = @'
import java.sql.*;
class JdbcProbe {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        try (var c = DriverManager.getConnection(System.getenv("NOTIURBA_TEST_DB_URL"),
                System.getenv("NOTIURBA_TEST_DB_USER"), System.getenv("NOTIURBA_TEST_DB_PASSWORD"));
             var s = c.createStatement(); var r = s.executeQuery("SELECT current_database(), postgis_version()")) {
            if (!r.next() || !r.getString(1).startsWith("notiurba_test_")) throw new Exception("Base inesperada");
            System.out.println("JDBC_OK; base=" + r.getString(1) + "; PostGIS=" + r.getString(2));
        } catch (SQLException e) {
            System.err.println("JDBC_ERROR; tipo=" + e.getClass().getName() + "; SQLState=" + e.getSQLState());
            System.exit(2);
        }
    }
}
'@
    [IO.File]::WriteAllText($probeFile, $probeSource, (New-Object Text.UTF8Encoding($false)))
    $ErrorActionPreference = 'Continue'
    & $java --class-path $driver $probeFile 2>&1 | Tee-Object -FilePath (Join-Path $outputDir 'jdbc.log')
    $probeExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($probeExit -ne 0) { throw 'Fallo de la sonda JDBC Java 21; no se ejecutaron las integraciones.' }

    # POM temporal en el mismo basedir: mismas dependencias, fuentes y recursos.
    # Solo configura Surefire para separar informes y evitar el attach de Mockito.
    $pom = New-Object Xml.XmlDocument
    $pom.Load((Join-Path $Worktree 'backend\pom.xml'))
    $ns = $pom.DocumentElement.NamespaceURI
    $plugin = $pom.CreateElement('plugin', $ns)
    foreach ($pair in @(@('groupId','org.apache.maven.plugins'), @('artifactId','maven-surefire-plugin'), @('version','3.2.5'))) {
        $element = $pom.CreateElement($pair[0], $ns); $element.InnerText = $pair[1]
        [void]$plugin.AppendChild($element)
    }
    $config = $pom.CreateElement('configuration', $ns)
    foreach ($pair in @(@('reportsDirectory',$reportDir), @('forkCount','0'), @('failIfNoTests','true'))) {
        $element = $pom.CreateElement($pair[0], $ns); $element.InnerText = $pair[1]
        [void]$config.AppendChild($element)
    }
    [void]$plugin.AppendChild($config)
    [void]$pom.project.build.plugins.AppendChild($plugin)
    $pom.Save($temporaryPom)
    $stage = 'Maven / migracion / aserciones'
    $ErrorActionPreference = 'Continue'
    & $MavenCommand -s $MavenSettings -B -ntp -f $temporaryPom `
        '-Dtest=EnrutamientoIntegracionTest,MigracionEnrutamientoTest' `
        '-Dnotiurba.correo.reportes-habilitados=false' test 2>&1 |
        Tee-Object -FilePath (Join-Path $outputDir 'maven.log')
    $mavenExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    $stage = 'Resultados Surefire'
    $cases = @()
    foreach ($class in @('EnrutamientoIntegracionTest','MigracionEnrutamientoTest')) {
        $xmlPath = Join-Path $reportDir "TEST-com.notiurba.correo.$class.xml"
        if (Test-Path -LiteralPath $xmlPath) {
            $suite = New-Object Xml.XmlDocument
            $suite.Load($xmlPath)
            $cases += @($suite.SelectNodes('/testsuite/testcase'))
        }
    }
    foreach ($name in $expected) {
        $found = @($cases | Where-Object { $_.GetAttribute('name') -eq $name })
        $result = 'NO EJECUTADA'
        if ($found.Count -eq 1) {
            if ($found[0].SelectSingleNode('skipped')) { $result = 'OMITIDA' }
            elseif ($found[0].SelectSingleNode('error')) { $result = 'ERROR' }
            elseif ($found[0].SelectSingleNode('failure')) { $result = 'FALLO' }
            else { $result = 'APROBADA' }
        } elseif ($found.Count -gt 1) { $result = 'DUPLICADA' }
        $rows += [pscustomobject]@{ Prueba = $name; Resultado = $result }
    }
    $rows | Format-Table -AutoSize | Out-Host
    $passed = @($rows | Where-Object Resultado -eq 'APROBADA').Count
    $lines = @('# Integraciones JDBC locales', '', "Commit: $headCommit", "Java: 21; Maven exit: $mavenExit", '',
        '| Prueba | Resultado |', '| --- | --- |')
    $lines += @($rows | ForEach-Object { "| $($_.Prueba) | $($_.Resultado) |" })
    $lines += @('', "Casos en XML: $($cases.Count). Aprobadas: $passed/8.",
        'Las pruebas utilizan EnviadorCorreo Mockito. No se inicia Spring Boot ni el programador de correo.')
    $lines | Set-Content -LiteralPath (Join-Path $outputDir 'RESULTADOS.md') -Encoding UTF8
    if ($mavenExit -ne 0 -or $cases.Count -ne 8 -or $passed -ne 8) {
        throw 'Validacion NO aprobada. Revisar maven.log y los XML: errores de conexion, migracion, configuracion o asercion.'
    }
} catch {
    $failure = $_
    Write-Warning "Etapa: $stage. $($_.Exception.Message)"
} finally {
    # Solo se retira un contenedor con la etiqueta aleatoria de ESTA ejecucion.
    # No se usa prune, compose down ni seleccion global de contenedores/volumenes.
    $ErrorActionPreference = 'Continue'
    $owned = @(& docker inspect --format '{{index .Config.Labels "notiurba.jdbc-audit"}}' $containerName 2>$null)
    if ($LASTEXITCODE -eq 0) {
        if (($owned -join '').Trim() -eq $runId) {
            & docker rm --force --volumes $containerName
            if ($LASTEXITCODE -ne 0) { $cleanupFailed = $true }
        } else { Write-Warning 'Etiqueta no coincidente: no se elimina ese contenedor.'; $cleanupFailed = $true }
    } elseif ($containerId) { $cleanupFailed = $true }
    foreach ($temporaryFile in @($temporaryPom, $probeFile)) {
        try {
            if (Test-Path -LiteralPath $temporaryFile) { Remove-Item -LiteralPath $temporaryFile -ErrorAction Stop }
        } catch { Write-Warning "No se pudo retirar el archivo temporal $temporaryFile"; $cleanupFailed = $true }
    }
    foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $savedEnv[$name], 'Process') }
    $ErrorActionPreference = 'Stop'
    Write-Host "Evidencia de esta ejecucion: $outputDir"
    if ($cleanupFailed) { Write-Warning "Limpieza incompleta: inspeccionar solo $containerName, etiqueta $runId." }
}
if ($failure) { throw $failure }
if ($cleanupFailed) { throw 'No se certifica cierre: limpieza del contenedor incompleta.' }
Write-Host 'APROBACION: ocho integraciones JDBC aprobadas; contenedor de pruebas eliminado.' -ForegroundColor Green
