#requires -Version 7.0
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if((git -C $repo branch --show-current).Trim() -ne 'fix/registro-pr20') { throw 'Rama incorrecta.' }
$container='notiurba-barrios-qa-20261108-153240-458c80'
$database='notiurba_barrios_qa'
$dbUser='qa_barrios'
$email='qa.not71.20261108.2147@example.test'
$api='http://127.0.0.1:18091'
$docker=(Get-Command docker -ErrorAction Stop).Source
$server=& $docker version --format '{{.Server.Version}}' 2>$null
if($LASTEXITCODE -ne 0 -or !$server) { throw 'Motor Docker inaccesible: detenido antes de modificar QA.' }
# No leer Config.Env: evita extraer o publicar credenciales del contenedor.
$portsText=& $docker inspect --format '{{json .NetworkSettings.Ports}}' $container
if($LASTEXITCODE -ne 0) { throw 'No existe el contenedor QA esperado.' }
$ports=$portsText | ConvertFrom-Json
$mapping=@($ports.'5432/tcp')
if($mapping.Count -ne 1 -or $mapping[0].HostIp -ne '127.0.0.1' -or $mapping[0].HostPort -ne '15435') { throw 'Mapeo PostgreSQL distinto del QA autorizado.' }
$image=(& $docker inspect --format '{{.Config.Image}}' $container).Trim()
if($LASTEXITCODE -ne 0 -or $image -ne 'postgis/postgis:16-3.4') { throw 'Imagen QA inesperada.' }
function Invoke-QaSql([string]$Sql) {
  $before=$OutputEncoding
  try {
    $OutputEncoding=[Text.UTF8Encoding]::new($false)
    $rows=$Sql | & $docker exec -i $container psql -X -qAt -v ON_ERROR_STOP=1 -U $dbUser -d $database
    if($LASTEXITCODE -ne 0) { throw 'SQL QA fallido; no continuar.' }
    return ($rows -join "`n").Trim()
  } finally { $OutputEncoding=$before }
}
$guard=@'
DO $qa$ BEGIN
 IF current_database()<>'notiurba_barrios_qa' OR current_user<>'qa_barrios' THEN RAISE EXCEPTION 'Destino fuera de QA'; END IF;
 IF (SELECT count(*) FROM usuario u JOIN rol r USING(id_rol)
     WHERE u.correo='qa.not71.20261108.2147@example.test' AND u.numero_documento='9711082147'
       AND u.id_barrio=43 AND r.nombre='CIUDADANO')<>1 THEN RAISE EXCEPTION 'La identidad sintetica no coincide'; END IF;
END $qa$;
'@
$stateSql=@"
BEGIN TRANSACTION READ ONLY;
$guard
SELECT json_build_object('verificado',correo_verificado_en IS NOT NULL,'idUsuario',id_usuario)::text
FROM usuario WHERE correo='$email';
COMMIT;
"@
$state=(Invoke-QaSql $stateSql) | ConvertFrom-Json
$issued=$false; $verificationStatus=$null; $replayStatus=$null
if(!$state.verificado) {
  # Fixture: token criptografico efimero, 15 minutos, limitado a esta cuenta QA.
  # No UPDATE del correo verificado, ni endpoint alternativo, ni JWT fabricado.
  $random=[Security.Cryptography.RandomNumberGenerator]::Create()
  $bytes=[byte[]]::new(32); $random.GetBytes($bytes); $random.Dispose()
  $token=[Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')
  $sha=[Security.Cryptography.SHA256]::Create()
  $digest=[BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($token))).Replace('-','').ToLowerInvariant(); $sha.Dispose()
  if($digest -notmatch '^[a-f0-9]{64}$') { throw 'Hash inesperado.' }
  $fixtureSql=@"
BEGIN;
SET LOCAL lock_timeout='3s';
SET LOCAL statement_timeout='10s';
$guard
SELECT id_usuario FROM usuario WHERE correo='$email' FOR UPDATE;
UPDATE token_verificacion t SET usado_en=now()
FROM usuario u,tipo_token tipo
WHERE t.id_usuario=u.id_usuario AND t.id_tipo_token=tipo.id_tipo_token
 AND u.correo='$email' AND tipo.nombre='Verificación de correo'
 AND t.usado_en IS NULL AND t.expira_en>now();
INSERT INTO token_verificacion(id_usuario,id_tipo_token,token_hash,creado_en,expira_en)
SELECT u.id_usuario,t.id_tipo_token,'$digest',now(),now()+interval '15 minutes'
FROM usuario u CROSS JOIN tipo_token t
WHERE u.correo='$email' AND u.correo_verificado_en IS NULL AND t.nombre='Verificación de correo';
"@
  $tokenAssert=@'
DO $qa$ BEGIN
 IF (SELECT count(*) FROM token_verificacion WHERE token_hash='__HASH__')<>1 THEN RAISE EXCEPTION 'No se creo exactamente un token'; END IF;
END $qa$;
COMMIT;
'@
  $fixtureSql+="`n"+$tokenAssert.Replace('__HASH__',$digest)
  Invoke-QaSql $fixtureSql | Out-Null
  $issued=$true
  $verification=Invoke-WebRequest -Uri "$api/api/auth/verificar" -Method Post -ContentType 'application/json' -Body (@{token=$token}|ConvertTo-Json -Compress) -SkipHttpErrorCheck -MaximumRedirection 0 -TimeoutSec 15
  $verificationStatus=[int]$verification.StatusCode
  if($verificationStatus -ne 200) { throw "Verificacion real fallo HTTP $verificationStatus; no se imprime el token." }
  $session=$verification.Content | ConvertFrom-Json
  if($session.rol -ne 'CIUDADANO' -or !$session.token) { throw 'Respuesta de sesion inesperada.' }
  $replay=Invoke-WebRequest -Uri "$api/api/auth/verificar" -Method Post -ContentType 'application/json' -Body (@{token=$token}|ConvertTo-Json -Compress) -SkipHttpErrorCheck -MaximumRedirection 0 -TimeoutSec 15
  $replayStatus=[int]$replay.StatusCode
  if($replayStatus -ne 400 -or ($replay.Content|ConvertFrom-Json).codigo -ne 'ENLACE_INVALIDO') { throw 'El token no se rechazo al reutilizarlo.' }
  $consumed=Invoke-QaSql "BEGIN TRANSACTION READ ONLY; SELECT (usado_en IS NOT NULL)::int FROM token_verificacion WHERE token_hash='$digest'; COMMIT;"
  if($consumed -ne '1') { throw 'El token no aparece consumido en la base QA.' }
  $session=$null; $verification=$null; $replay=$null; $token=$null; $digest=$null
}
# Credencial de la cuenta sintetica creada por Codex; nunca usar una cuenta real.
$login=Invoke-WebRequest -Uri "$api/api/auth/login" -Method Post -ContentType 'application/json' -Body (@{correo=$email;password='QaNot71-2026-2147!'}|ConvertTo-Json -Compress) -SkipHttpErrorCheck -MaximumRedirection 0 -TimeoutSec 15
$loginStatus=[int]$login.StatusCode
if($loginStatus -ne 200) { throw "Login QA fallo HTTP $loginStatus." }
$loginData=$login.Content | ConvertFrom-Json
if($loginData.rol -ne 'CIUDADANO' -or !$loginData.token) { throw 'Sesion de login inesperada.' }
$login=$null; $loginData=$null
$proofSql=@"
BEGIN TRANSACTION READ ONLY;
$guard
SELECT json_build_object(
 'base',current_database(),'usuarioSql',current_user,'idUsuario',u.id_usuario,'correo',u.correo,
 'idBarrio',u.id_barrio,'correoVerificadoEn',u.correo_verificado_en,
 'cantidadReportes',(SELECT count(*) FROM reporte r WHERE r.id_usuario=u.id_usuario),
 'cantidadFotos',(SELECT count(*) FROM evidencia_reporte e JOIN reporte r USING(id_reporte) WHERE r.id_usuario=u.id_usuario)
)::text FROM usuario u WHERE u.correo='$email';
COMMIT;
"@
$proof=(Invoke-QaSql $proofSql) | ConvertFrom-Json
if(!$proof.correoVerificadoEn) { throw 'La cuenta no aparece verificada en SQL QA.' }
$out=Join-Path $repo 'docs/validacion/evidencias-NOT-71-continuacion-20261108'
[IO.Directory]::CreateDirectory($out) | Out-Null
$result=[ordered]@{fecha=[DateTimeOffset]::Now.ToString('o');container=$container;api=$api;tokenFixtureEmitido=$issued;verificacionHttp=$verificationStatus;reutilizacionHttp=$replayStatus;loginHttp=$loginStatus;persistenciaSql=$proof;tokensGuardados=$false;correoExterno=$false;estadoNOT71='NO_APROBADA';alcance='Cuenta sintetica QA; no prueba entrega de correo ni subida de fotos'}
$file=Join-Path $out ('verificacion-controlada-'+[DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'.json')
$result | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $file -Encoding utf8
Write-Host 'Cuenta QA confirmada por el endpoint real; login HTTP 200 y estado SQL comprobados.'
Write-Host "Evidencia sin tokens: $file"
Write-Host 'Continua el recorrido Chrome en http://127.0.0.1:5175/login. NOT-71 no se declara terminada.'
