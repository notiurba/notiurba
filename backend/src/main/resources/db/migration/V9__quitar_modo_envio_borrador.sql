-- =====================================================================
-- NotiUrba · V9: se quita el modo de envío BORRADOR (ADR-0002, 29/10/2026)
-- El correo lo envía NotiUrba con Resend; el borrador que el ciudadano enviaba quedó descartado.
-- V3 no se edita (rompería el checksum de Flyway): se corrige con esta migración.
-- Solo se borra si ningún registro de envío lo usa.
-- =====================================================================

DELETE FROM modo_envio
 WHERE nombre = 'BORRADOR'
   AND NOT EXISTS (SELECT 1 FROM registro_envio_correo r WHERE r.id_modo_envio = modo_envio.id_modo_envio);
