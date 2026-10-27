#requires -Version 5.1
[CmdletBinding()]
param(
    [string]$Repository = (Split-Path $PSScriptRoot -Parent),
    [switch]$CheckOnly
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Repository = (Resolve-Path -LiteralPath $Repository).Path
$branch = 'codex/edt-5-1-validaciones'
function Assert-Branch {
    $active = & git -C $Repository branch --show-current
    if ($LASTEXITCODE -ne 0 -or $active -ne $branch) {
        throw 'Operacion bloqueada: la rama debe ser codex/edt-5-1-validaciones.'
    }
}
function Git-Checked([string[]]$Arguments) {
    Assert-Branch
    & git -C $Repository @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Git fallo en $($Arguments[0]); no se intentara forzar ni fusionar." }
}
Assert-Branch
$origin = & git -C $Repository remote get-url origin
if ($origin -ne 'https://github.com/notiurba/notiurba.git') { throw 'Origin inesperado.' }
if ((& git -C $Repository rev-parse --is-shallow-repository) -ne 'false') { throw 'Se requiere historial completo.' }
if (& git -C $Repository status --porcelain) { throw 'Hay cambios sin confirmar; conservarlos antes de publicar.' }
Git-Checked @('fetch','origin','--prune')
Git-Checked @('merge-base','--is-ancestor','origin/main','HEAD')
Git-Checked @('merge-base','--is-ancestor',"origin/$branch",'HEAD')
Git-Checked @('diff','--exit-code','origin/main','HEAD','--','frontend','backend/src/main/resources/db/migration/')
Git-Checked @('diff','--check',"origin/$branch",'HEAD')
foreach ($file in @('backend/pom.xml','backend/src/main/java/com/notiurba/geo/SpatialProbe.java',
    'backend/src/main/java/com/notiurba/foto/CloudinaryPhotoStore.java',
    'backend/src/test/java/com/notiurba/CloudinaryIntegrationTest.java',
    'backend/src/test/java/com/notiurba/foto/CloudinaryPhotoStoreTest.java',
    'backend/src/test/java/com/notiurba/MigrationIntegrationTest.java','.github/workflows/edt51-validation.yml')) {
    if (!(Test-Path -LiteralPath (Join-Path $Repository $file))) { throw "Falta $file" }
}
foreach ($file in @('backend/src/main/java/com/notiurba/foto/CloudinaryPhotoStore.java',
    'backend/src/test/java/com/notiurba/CloudinaryIntegrationTest.java',
    'backend/src/test/java/com/notiurba/foto/CloudinaryPhotoStoreTest.java')) {
    $source = [IO.File]::ReadAllText((Join-Path $Repository $file))
    if ($source -cnotmatch '"signature_algorithm",\s*"SHA256"' -or $source -cmatch '"signature_algorithm",\s*"sha256"') {
        throw "Configuracion SHA256 incorrecta en $file"
    }
}
Write-Host 'Preflight correcto. Solo se publicara la rama de validaciones; sin PR ni fusiones.'
if ($CheckOnly) { return }
Git-Checked @('push','origin',"HEAD:refs/heads/$branch")
Write-Host 'Push terminado. Consultar Actions para verificar resultados reales; esto no aprueba las integraciones externas.'
Write-Host 'https://github.com/notiurba/notiurba/actions?query=branch%3Acodex%2Fedt-5-1-validaciones'