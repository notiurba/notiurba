#requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Repository,
    [Parameter(Mandatory)][string]$Patch,
    [switch]$Spatial,
    [switch]$Cloudinary,
    [switch]$Migration,
    [switch]$Publish
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Patch = (Resolve-Path -LiteralPath $Patch).Path
$Repository = [IO.Path]::GetFullPath($Repository)
$branch = 'codex/edt-5-1-validaciones'
function Git-Required([string[]]$Arguments) {
    if ($Arguments[0] -in @("apply","add","commit","push")) {
        if ((& git branch --show-current) -ne $branch) { throw "Wrong branch: mutation blocked." }
    }
    $output = & git @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Git failed: $($Arguments[0]); $output" }
    return $output
}
if (!(Test-Path -LiteralPath $Repository)) {
    Git-Required @('clone','--branch',$branch,'--no-single-branch','https://github.com/notiurba/notiurba.git',$Repository) | Out-Host
}
Push-Location -LiteralPath $Repository
try {
    if (([string](Git-Required @('branch','--show-current'))).Trim() -ne $branch) {
        throw 'Wrong branch: no writes allowed.'
    }
    $origin = [string](Git-Required @('remote','get-url','origin'))
    if ($origin.Trim() -notmatch '^(https://github\.com/notiurba/notiurba(?:\.git)?|git@github\.com:notiurba/notiurba\.git)$') {
        throw 'Unexpected origin; no changes made.'
    }
    if (([string](Git-Required @('rev-parse','--is-shallow-repository'))).Trim() -eq 'true') {
        throw 'A complete authentic clone is required; fetch --unshallow first.'
    }
    if (Git-Required @('status','--porcelain')) { throw 'Uncommitted work exists; preserve it before running.' }
    Git-Required @('fetch','origin','--prune') | Out-Host
    # Refuse to write from any other branch; never switch to main/develop.
    if (([string](Git-Required @('branch','--show-current'))).Trim() -ne $branch) {
        throw 'Only codex/edt-5-1-validaciones is allowed; no files changed.'
    }
    & git merge-base HEAD origin/main > $null
    if ($LASTEXITCODE -ne 0) { throw 'Unrelated local history; use an authentic clone.' }
    & git show-ref --verify --quiet "refs/remotes/origin/$branch"
    if ($LASTEXITCODE -eq 0) {
        & git merge-base --is-ancestor "origin/$branch" HEAD
        if ($LASTEXITCODE -ne 0) { throw 'Remote validation commits are missing; no automatic merge is allowed.' }
    }
    & git merge-base --is-ancestor origin/main HEAD
    if ($LASTEXITCODE -ne 0) { throw 'main has newer commits; no automatic merge is allowed.' }
    $testedMain = ([string](Git-Required @('rev-parse','origin/main'))).Trim()
    # Check whole patch before changing any file. Stop rather than overwrite conflicts.
    $patchArgs = @('apply','--exclude=render.yaml')
    & git @patchArgs --reverse --check $Patch 2>$null
    if ($LASTEXITCODE -ne 0) {
        Git-Required ($patchArgs + @('--check',$Patch)) | Out-Host
        Git-Required ($patchArgs + @($Patch)) | Out-Host
    }
    # render.yaml was already published separately; do not replace it blindly.
    Git-Required @('diff','--exit-code','origin/main','--','backend/src/main/resources/db/migration/') | Out-Host
    Git-Required @('diff','--check') | Out-Host
    & java -version
    if ($LASTEXITCODE -ne 0) { throw 'Java missing. Install Java 21 and configure JAVA_HOME.' }
    $stamp = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
    $logDir = Join-Path $Repository "docs/validacion/logs/authorized-$stamp"
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    $results = [Collections.Generic.List[object]]::new()
    function Run-Maven([string]$Name,[string[]]$Goals) {
        if ((& git branch --show-current) -ne $branch) { throw "Wrong branch: Maven blocked." }
        $started = [DateTime]::UtcNow.ToString('o')
        $raw = (& mvn -B -f backend/pom.xml @Goals 2>&1 | Out-String)
        $code = $LASTEXITCODE
        foreach ($name in @('TEST_DB_PASSWORD','CLOUDINARY_API_SECRET','JWT_SECRET')) {
            $secret = [Environment]::GetEnvironmentVariable($name)
            if ($secret) { $raw = $raw.Replace($secret,'[REDACTED]') }
        }
        [IO.File]::WriteAllText((Join-Path $logDir "$Name.log"),"UTC=$started`n$raw`nEXIT=$code`n")
        $results.Add([pscustomobject]@{name=$Name;utc=$started;exit=$code;state=$(if($code -eq 0){'APROBADO'}else{'PENDIENTE'})})
        Write-Host "$Name exit=$code"
    }
    Run-Maven 'maven-test' @('clean','test')
    Run-Maven 'maven-package' @('package')
    if ($Spatial) {
        foreach ($name in @('TEST_DB_URL','TEST_DB_USER','TEST_DB_PASSWORD')) {
            if (![Environment]::GetEnvironmentVariable($name)) { throw "Missing $name; isolated pre-migrated test database required." }
        }
        Run-Maven 'spatial' @('-Pspatial-tests','verify')
    }
    if ($Cloudinary) {
        foreach ($name in @('CLOUDINARY_CLOUD_NAME','CLOUDINARY_API_KEY','CLOUDINARY_API_SECRET')) {
            if (![Environment]::GetEnvironmentVariable($name)) { throw "Missing $name; synthetic assets only." }
        }
        Run-Maven 'cloudinary' @('-Pcloudinary-tests','verify')
    }
    if ($Migration) { Run-Maven 'migration' @('-Pmigration-tests','verify') }
    $results | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $logDir 'results.json') -Encoding utf8
    Git-Required @('diff','--check') | Out-Host
    if ($Publish) {
        if ($results | Where-Object exit -ne 0) { throw 'Failed validation; inspect logs before publishing.' }
        Git-Required @('add','--','backend','.github/workflows/edt51-validation.yml','tools','docs/validacion','.gitignore') | Out-Host
        & git diff --cached --quiet
        if ($LASTEXITCODE -eq 1) { Git-Required @('commit','-m','Complete EDT 5.1 validation implementation and evidence') | Out-Host }
        Git-Required @('fetch','origin','--prune') | Out-Host
        if (([string](Git-Required @('rev-parse','origin/main'))).Trim() -ne $testedMain) {
            throw 'main changed during validation; integrate it and rerun tests before publishing.'
        }
        Git-Required @('push','-u','origin',$branch) | Out-Host
        Write-Host 'Only validation branch published; push triggers CI. No PR or merge is created.'
    }
} finally { Pop-Location }