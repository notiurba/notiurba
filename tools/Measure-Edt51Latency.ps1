param(
    [Parameter(Mandatory=$true)][ValidateSet('before','after')][string]$Phase,
    [string]$Evidence = 'edt51-latency.json',
    [switch]$ConfirmExclusiveIdle
)
$ErrorActionPreference = 'Stop'
$origin = $env:EDT51_RENDER_URL
$token = $env:EDT51_ADMIN_TOKEN
if ([string]::IsNullOrWhiteSpace($origin) -or [string]::IsNullOrWhiteSpace($token)) {
    throw 'Defina EDT51_RENDER_URL y EDT51_ADMIN_TOKEN en el proceso'
}
$uri = [Uri]$origin
if ($uri.Scheme -ne 'https' -or $uri.Host -notlike '*.onrender.com' -or $uri.AbsolutePath -ne '/' -or $uri.Query -or $uri.Fragment -or $uri.UserInfo) {
    throw 'Use el origen HTTPS de un servicio aislado de Render'
}
$origin = $origin.TrimEnd('/')
$data = @{ origin=$origin; phases=@{}; idle_exclusivity_verified=$false }
if (Test-Path -LiteralPath $Evidence) { $data = Get-Content -LiteralPath $Evidence -Raw | ConvertFrom-Json -AsHashtable }
if ($data.origin -ne $origin) { throw 'El archivo corresponde a otro servicio' }
if ($data.phases.ContainsKey($Phase)) { throw 'La fase ya existe; use otro archivo de evidencia' }
if ($Phase -eq 'after') {
    if (-not $data.phases.ContainsKey('before')) { throw 'Ejecute la fase before primero' }
    $gap = ([DateTimeOffset]::UtcNow - [DateTimeOffset]::Parse($data.phases.before.ended_utc)).TotalSeconds
    if ($gap -lt 960) { throw 'Deben transcurrir al menos 16 minutos sin tráfico' }
    if (-not $ConfirmExclusiveIdle) { throw 'Verifique ausencia de otros clientes/monitores y use -ConfirmExclusiveIdle' }
    $data.observed_gap_seconds = $gap
    $data.idle_exclusivity_verified = $true
}
$rows = @()
for ($i=1; $i -le 5; $i++) {
    $row = @{ utc=[DateTimeOffset]::UtcNow.ToString('o'); sequence=$i; http=$null; spatial_ok=$false }
    $timer = [Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri ($origin+'/api/admin/diagnostics/spatial') -Headers @{
            Authorization=('Bearer '+$token); Accept='application/json'
        } -TimeoutSec 150 -MaximumRedirection 0 -SkipHttpErrorCheck
        $row.http = [int]$response.StatusCode
        $body = $response.Content | ConvertFrom-Json
        $geo = $body.geojson | ConvertFrom-Json
        $row.spatial_ok = ($row.http -eq 200 -and $body.contains_inside -eq $true -and
            $body.contains_border -eq $false -and $body.covers_border -eq $true -and
            $geo.type -eq 'Point' -and $geo.coordinates.Count -eq 2 -and
            $geo.coordinates[0] -eq -76.195 -and $geo.coordinates[1] -eq 4.085)
    } catch {
        # No exception message, token, response body or signed URL is stored.
        $row.error = $_.Exception.GetType().Name
    } finally {
        $timer.Stop()
        $row.milliseconds = [Math]::Round($timer.Elapsed.TotalMilliseconds,2)
    }
    $rows += $row
}
$data.phases[$Phase] = @{ samples=$rows; ended_utc=[DateTimeOffset]::UtcNow.ToString('o') }
if ($Phase -eq 'after') {
    $data.comparison = @{
        first_before_ms=$data.phases.before.samples[0].milliseconds
        first_after_ms=$rows[0].milliseconds
        mean_subsequent_before_ms=($data.phases.before.samples[1..4].milliseconds | Measure-Object -Average).Average
        mean_subsequent_after_ms=($rows[1..4].milliseconds | Measure-Object -Average).Average
    }
}
$data | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $Evidence -Encoding utf8
$rows | ConvertTo-Json -Depth 4
if (@($rows | Where-Object { -not $_.spatial_ok }).Count -gt 0) { exit 1 }
