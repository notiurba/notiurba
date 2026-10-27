#requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet('Neon','Cloudinary')][string]$Provider,
    [string]$MavenExecutable = 'mvn'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = Split-Path $PSScriptRoot -Parent
Push-Location $repo
try {
    if ($env:GITHUB_ACTIONS -eq 'true') {
        if ($env:GITHUB_EVENT_NAME -ne 'workflow_dispatch' -or $env:GITHUB_REF -ne 'refs/heads/feature/edt-5.1-technical-validation') {
            throw 'External jobs require explicit dispatch on the validation branch.'
        }
        # actions/checkout uses detached HEAD; validate the actual checked-out SHA.
        if ([string]::IsNullOrWhiteSpace($env:GITHUB_SHA) -or (& git rev-parse HEAD) -cne $env:GITHUB_SHA) { throw 'Unexpected checkout; no provider calls allowed.' }
    } elseif ((& git branch --show-current) -ne 'feature/edt-5.1-technical-validation') {
        throw 'Wrong branch; no provider calls allowed.'
    }
    if ($env:EDT51_EXTERNAL_ALLOWED -cne 'true') { throw 'Test-only resource approval missing: EDT51_EXTERNAL_ALLOWED must be true.' }
    $names = if ($Provider -eq 'Neon') { @('TEST_DB_URL','TEST_DB_USER','TEST_DB_PASSWORD','JWT_SECRET') } else { @('CLOUDINARY_CLOUD_NAME','CLOUDINARY_API_KEY','CLOUDINARY_API_SECRET') }
    $missing = @($names | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_)) })
    if ($missing.Count) { throw ('Missing authorized test configuration: ' + ($missing -join ', ')) }
    if ($Provider -eq 'Neon') {
        try { $uri = [Uri]($env:TEST_DB_URL.Substring(5)) } catch { throw 'Invalid test JDBC URL; value omitted.' }
        if (!$env:TEST_DB_URL.StartsWith('jdbc:postgresql://') -or !$uri.Host.EndsWith('.neon.tech') -or $uri.UserInfo -or $uri.Fragment) { throw 'Expected Neon JDBC URL without inline credentials.' }
        $query = @{}
        foreach ($pair in $uri.Query.TrimStart('?').Split('&')) {
            $kv = $pair.Split('=',2)
            if ($kv.Count -ne 2 -or $query.ContainsKey($kv[0])) { throw 'Invalid or duplicate JDBC options; values omitted.' }
            $query[$kv[0]] = [Net.WebUtility]::UrlDecode($kv[1])
        }
        # No custom socket/SSL factories, passwords or weakening overrides in the URL.
        foreach ($key in $query.Keys) { if ($key -cnotin @('sslmode','sslfactory','channelBinding','connectTimeout')) { throw 'Unapproved JDBC option; value omitted.' } }
        if ($query['sslmode'] -cne 'verify-full' -or $query['sslfactory'] -cne 'org.postgresql.ssl.DefaultJavaSSLFactory') { throw 'JDBC must verify hostname and certificate using the Java truststore.' }
        if ($env:JWT_SECRET.Length -lt 32) { throw 'JWT test key must have at least 32 characters.' }
    }
    . (Join-Path $PSScriptRoot 'Edt51SafeReports.ps1')
    $profile = if ($Provider -eq 'Neon') { 'spatial-tests' } else { 'cloudinary-tests' }
    $expected = if ($Provider -eq 'Neon') { 'com.notiurba.SpatialIntegrationTest' } else { 'com.notiurba.CloudinaryIntegrationTest' }
    $resultPath = Join-Path $repo ('external-results/' + $Provider.ToLowerInvariant())
    New-Item -ItemType Directory -Force $resultPath | Out-Null
    $started = [DateTime]::UtcNow
    $privateLog = Join-Path ([IO.Path]::GetTempPath()) ('notiurba-' + [Guid]::NewGuid() + '.log')
    $exit = 1
    try {
        # Raw Maven output and raw XML can contain capability URLs or Spring properties.
        # Never echo them or upload them as Actions artifacts.
        & $MavenExecutable -B -f backend/pom.xml "-P$profile" verify *> $privateLog
        $exit = $LASTEXITCODE
        $reports = @(Get-ChildItem backend/target/failsafe-reports/TEST-*.xml -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTimeUtc -ge $started })
        $totals = Export-Edt51SafeReports -Files $reports -Destination $resultPath -ExpectedClass $expected
        $state = if ($exit -eq 0 -and $totals.Tests -gt 0 -and $totals.Failures -eq 0 -and $totals.Errors -eq 0 -and $totals.Skipped -eq 0) { 'APROBADO' } else { 'PENDIENTE' }
        [ordered]@{ provider=$Provider; utc_start=$started.ToString('o'); utc_end=[DateTime]::UtcNow.ToString('o'); command="mvn -B -f backend/pom.xml -P$profile verify"; maven_exit=$exit; tests=$totals.Tests; failures=$totals.Failures; errors=$totals.Errors; skipped=$totals.Skipped; state=$state; evidence_scope='Java integration only; browser and Render not tested' } | ConvertTo-Json | Set-Content (Join-Path $resultPath 'result.json')
        Write-Host "UTC=$([DateTime]::UtcNow.ToString('o')) provider=$Provider MavenExit=$exit tests=$($totals.Tests) failures=$($totals.Failures) errors=$($totals.Errors) skipped=$($totals.Skipped) state=$state"
        if ($state -ne 'APROBADO') { throw 'Integration did not pass; inspect sanitized reports. Raw details retained only in the local Maven reports, never publish them.' }
    } finally {
        if (Test-Path -LiteralPath $privateLog) { Remove-Item -LiteralPath $privateLog }
    }
} finally { Pop-Location }
