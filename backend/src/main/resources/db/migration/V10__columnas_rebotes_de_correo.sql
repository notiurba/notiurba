-- =====================================================================
-- NotiUrba · V10: columnas para avisar de los rebotes de correo (Resend)
-- · id_envio_proveedor: identificador que Resend da a cada envío; el webhook lo usa
--   para saber a qué registro pertenece un rebote.
-- · aviso_fallo_en: cuándo se avisó al ciudadano del fallo (evita avisar dos veces
--   si Resend repite el evento).
-- Ver docs/contexto/11-contrato-api.md (sección 8). Las cuentas existentes no necesitan
-- ajuste: el administrador de V4 y las de Google ya nacen con el correo verificado.
-- =====================================================================

ALTER TABLE registro_envio_correo
  ADD COLUMN id_envio_proveedor VARCHAR(100),
  ADD COLUMN aviso_fallo_en TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_envio_proveedor
  ON registro_envio_correo (id_envio_proveedor) WHERE id_envio_proveedor IS NOT NULL;
