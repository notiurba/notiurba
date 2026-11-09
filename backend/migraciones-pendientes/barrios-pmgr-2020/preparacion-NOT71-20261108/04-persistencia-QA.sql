-- SOLO LECTURA. Solo el caso sintetico de esta revision; no imprime hashes ni tokens.
BEGIN TRANSACTION READ ONLY;
SELECT current_database(),current_user;
SELECT u.id_usuario,u.correo,u.id_barrio,b.nombre,b.id_comuna,
       u.correo_verificado_en IS NOT NULL AS correo_verificado,
       u.password_hash IS NOT NULL AS tiene_password,
       u.id_aviso_privacidad,u.acepto_privacidad_en,u.creado_en
FROM usuario u JOIN barrio b USING(id_barrio)
WHERE u.correo='qa.not71.20261108.2147@example.test';
SELECT r.id_reporte,r.codigo_seguimiento,r.id_usuario,r.id_categoria,c.nombre AS categoria,
       ST_X(r.ubicacion) AS longitud,ST_Y(r.ubicacion) AS latitud,
       vc.id_comuna,vc.origen AS origen_comuna,
       vd.id_entidad_destino,vd.entidad,vd.correo_destino,
       ev.id_evidencia,ev.url
FROM reporte r JOIN usuario u USING(id_usuario)
JOIN categoria c USING(id_categoria)
LEFT JOIN v_reporte_comuna vc USING(id_reporte)
LEFT JOIN v_reporte_destino vd USING(id_reporte)
LEFT JOIN evidencia_reporte ev USING(id_reporte)
WHERE u.correo='qa.not71.20261108.2147@example.test'
ORDER BY r.id_reporte,ev.orden;
SELECT r.codigo_seguimiento,re.id_canal_atencion,re.destino,re.id_estado_envio,re.enviado_en
FROM registro_envio_correo re JOIN reporte r USING(id_reporte) JOIN usuario u USING(id_usuario)
WHERE u.correo='qa.not71.20261108.2147@example.test';
COMMIT;
-- Actualmente no se ha creado un reporte con esta cuenta: autenticacion bloqueada.
-- Comuna/destino de las vistas son derivados; contrastar origen y mensaje/destino congelados.
