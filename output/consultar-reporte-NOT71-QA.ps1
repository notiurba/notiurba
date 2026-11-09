#requires -Version 7.0
param([switch]$PassThru)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$container='notiurba-barrios-qa-20261108-153240-458c80'
$docker=(Get-Command docker -ErrorAction Stop).Source
$sql=@'
BEGIN TRANSACTION READ ONLY;
DO $qa$ BEGIN
 IF current_database()<>'notiurba_barrios_qa' OR current_user<>'qa_barrios' THEN RAISE EXCEPTION 'Destino incorrecto'; END IF;
END $qa$;
SELECT json_build_object('base',current_database(),'usuarioSql',current_user,'reporte',(
 SELECT json_build_object('idReporte',r.id_reporte,'codigo',r.codigo_seguimiento,'idUsuario',u.id_usuario,
 'correo',u.correo,'idCategoria',r.id_categoria,'categoria',c.nombre,
 'latitud',ST_Y(r.ubicacion),'longitud',ST_X(r.ubicacion),'comuna',vc.id_comuna,'origenComuna',vc.origen,
 'entidadId',vd.id_entidad_destino,'entidad',vd.entidad,'destinoDerivado',vd.correo_destino,
 'fotos',(SELECT COALESCE(json_agg(json_build_object('idEvidencia',e.id_evidencia,'idUsuario',e.id_usuario,'url',e.url,'orden',e.orden)), '[]'::json) FROM evidencia_reporte e WHERE e.id_reporte=r.id_reporte),
 'envios',(SELECT COALESCE(json_agg(json_build_object('canal',ec.id_canal_atencion,'destino',ec.destino,'estado',ee.nombre,'enviadoEn',ec.enviado_en)), '[]'::json) FROM registro_envio_correo ec JOIN estado_envio ee USING(id_estado_envio) WHERE ec.id_reporte=r.id_reporte))
 FROM reporte r JOIN usuario u USING(id_usuario) JOIN categoria c USING(id_categoria)
 LEFT JOIN v_reporte_comuna vc USING(id_reporte) LEFT JOIN v_reporte_destino vd USING(id_reporte)
 WHERE r.codigo_seguimiento='NU-000001' AND u.correo='qa.not71.20261108.2147@example.test'
))::text;
COMMIT;
'@
$old=$OutputEncoding
try {
 $OutputEncoding=[Text.UTF8Encoding]::new($false)
 $lines=$sql | & $docker exec -i $container psql -X -qAt -v ON_ERROR_STOP=1 -U qa_barrios -d notiurba_barrios_qa
 if($LASTEXITCODE -ne 0){throw 'No se pudo ejecutar la consulta SQL de solo lectura.'}
} finally {$OutputEncoding=$old}
$proof=($lines -join "`n")|ConvertFrom-Json
if(!$proof.reporte -or $proof.reporte.idUsuario -ne 2 -or $proof.reporte.idCategoria -ne 1 -or $proof.reporte.comuna -ne 4){throw 'Datos del reporte distintos del caso QA esperado.'}
if([Math]::Abs($proof.reporte.latitud-4.084847878371718)-gt 0.0000001 -or [Math]::Abs($proof.reporte.longitud-(-76.1953353881836))-gt 0.0000001){throw 'Coordenadas distintas del caso QA.'}
$out=Join-Path $repo 'docs/validacion/evidencias-NOT-71-reporte-20261108'
[IO.Directory]::CreateDirectory($out)|Out-Null
$result=@{fecha=[DateTimeOffset]::Now.ToString('o');lecturaSql=$true;codigo='NU-000001';persistencia=$proof;fotoUrlPersistida=(@($proof.reporte.fotos).Count-gt 0);fotoHttp='no comprobado por este script';NOT71='NO_APROBADA'}
$file=Join-Path $out ('persistencia-SQL-'+[DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'.json')
$result|ConvertTo-Json -Depth 9|Set-Content -LiteralPath $file -Encoding utf8
Write-Host "Consulta SQL real completada; evidencia: $file"
Write-Host "Fotos persistidas: $(@($proof.reporte.fotos).Count). Su URL aun requiere GET y comparacion de bytes."
if($PassThru){return $result}
