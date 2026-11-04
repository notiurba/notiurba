-- =====================================================================
-- NotiUrba · V8: categoría 10 «Otro / no clasificado» y canal SEDAMA activo (NOT-77)
-- Antes se hizo editando V3, lo que rompe el checksum de Flyway en las bases que ya
-- la aplicaron. Va después de V7 porque V7 busca la categoría 10 por su nombre anterior.
-- =====================================================================

UPDATE categoria SET nombre = 'Otro / no clasificado'
 WHERE nombre = 'Otro / eventos comunitarios';

-- SEDAMA confirmó su correo (26/10/2026). B7 solo impide cambiar el acceso o el tipo:
-- activar el canal y quitar la nota «sin confirmar» está permitido.
UPDATE canal_atencion SET detalle = NULL, activo = TRUE
 WHERE id_canal_atencion = 11 AND acceso = 'sedama@tulua.gov.co';
