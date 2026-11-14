#requires -Version 7.0
# Offline safety checks only; never starts Maven or calls a provider.
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$names=@('EDT51_EXTERNAL_ALLOWED','TEST_DB_URL','TEST_DB_USER','TEST_DB_PASSWORD','JWT_SECRET','CLOUDINARY_CLOUD_NAME','CLOUDINARY_API_KEY','CLOUDINARY_API_SECRET','GITHUB_ACTIONS','GITHUB_EVENT_NAME','GITHUB_REF','GITHUB_SHA')
$saved=@{}
foreach($name in $names) { $saved[$name]=[Environment]::GetEnvironmentVariable($name); [Environment]::SetEnvironmentVariable($name,$null) }
$checks=0
function Expect-Rejection([string]$Provider,[string]$Message) {
    $rejected=$false
    try { & "$PSScriptRoot/Invoke-Edt51External.ps1" -Provider $Provider -MavenExecutable 'EDT51_MAVEN_MUST_NEVER_RUN' }
    catch { if ($_.Exception.Message -notlike $Message) { throw }; $rejected=$true }
    if (!$rejected) { throw 'Expected rejection before any provider calls.' }
}
$temp=Join-Path ([IO.Path]::GetTempPath()) ('edt51-guards-'+[guid]::NewGuid())
try {
    Expect-Rejection Neon '*approval missing*'; $checks++
    $env:EDT51_EXTERNAL_ALLOWED='true'
    Expect-Rejection Neon '*Missing authorized test configuration*'; $checks++
    Expect-Rejection Cloudinary '*Missing authorized test configuration*'; $checks++
    $env:TEST_DB_URL='jdbc:postgresql://ep-synthetic.us-east-2.aws.neon.tech/test?sslmode=require'
    $env:TEST_DB_USER='synthetic'; $env:TEST_DB_PASSWORD='synthetic-not-a-secret'; $env:JWT_SECRET='synthetic-test-key-at-least-32-characters'
    Expect-Rejection Neon '*JDBC must verify hostname*'; $checks++
    $env:GITHUB_ACTIONS='true'; $env:GITHUB_EVENT_NAME='push'; $env:GITHUB_REF='refs/heads/feature/edt-5.1-technical-validation'
    Expect-Rejection Cloudinary '*explicit dispatch*'; $checks++
    $env:GITHUB_EVENT_NAME='workflow_dispatch'; $env:GITHUB_SHA='0000000000000000000000000000000000000000'
    Expect-Rejection Cloudinary '*Unexpected checkout*'; $checks++
    New-Item -ItemType Directory $temp | Out-Null
    $file=Join-Path $temp 'raw.xml'
    '<testsuite name="com.notiurba.CloudinaryIntegrationTest"><properties><property name="secret" value="DO_NOT_EXPORT_SECRET"/></properties><testcase name="actualUploadRecoverDelete"><failure message="https://example.invalid/?signature=DO_NOT_EXPORT_SECRET">DO_NOT_EXPORT_SECRET</failure><system-out>DO_NOT_EXPORT_SECRET</system-out></testcase><system-err>DO_NOT_EXPORT_SECRET</system-err></testsuite>' | Set-Content $file
    . "$PSScriptRoot/Edt51SafeReports.ps1"
    foreach ($case in @(
        @{ Exit=0; Totals=@{Tests=1;Failures=0;Errors=0;Skipped=0}; Expected='APROBADO' },
        @{ Exit=1; Totals=@{Tests=1;Failures=1;Errors=0;Skipped=0}; Expected='FALLIDO' },
        @{ Exit=1; Totals=@{Tests=0;Failures=0;Errors=0;Skipped=0}; Expected='PENDIENTE' },
        @{ Exit=0; Totals=@{Tests=1;Failures=0;Errors=0;Skipped=1}; Expected='PENDIENTE' }
    )) {
        if ((Get-Edt51ExternalState -MavenExit $case.Exit -Totals $case.Totals) -cne $case.Expected) { throw 'Incorrect external execution state.' }
        $checks++
    }
    $result=Export-Edt51SafeReports -Files @((Get-Item $file)) -Destination $temp -ExpectedClass com.notiurba.CloudinaryIntegrationTest
    $export=[IO.File]::ReadAllText((Join-Path $temp 'TEST-com.notiurba.CloudinaryIntegrationTest.xml'))
    if($export.Contains('DO_NOT_EXPORT_SECRET') -or $export.Contains('https://') -or $result.Tests -ne 1 -or $result.Failures -ne 1) { throw 'Sensitive report data exported.' }; $checks++
    '<!DOCTYPE testsuite [<!ENTITY xxe SYSTEM "file:///not-read">]><testsuite name="com.notiurba.CloudinaryIntegrationTest">&xxe;</testsuite>' | Set-Content $file
    $blocked=$false
    try { Export-Edt51SafeReports -Files @((Get-Item $file)) -Destination $temp -ExpectedClass com.notiurba.CloudinaryIntegrationTest | Out-Null } catch { $blocked=$true }
    if (!$blocked) { throw 'DTD must be prohibited.' }; $checks++
    Write-Output "UTC=$([DateTime]::UtcNow.ToString('o')) offline_guard_checks=$checks failed=0 provider_calls=0"
} finally {
    foreach($name in $names) { [Environment]::SetEnvironmentVariable($name,$saved[$name]) }
    foreach($name in @('raw.xml','TEST-com.notiurba.CloudinaryIntegrationTest.xml')) {
        $p=Join-Path $temp $name; if(Test-Path -LiteralPath $p) { Remove-Item -LiteralPath $p }
    }
    if(Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp }
}
