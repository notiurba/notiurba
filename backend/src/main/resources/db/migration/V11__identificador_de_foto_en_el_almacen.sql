-- =====================================================================
-- NotiUrba · V11: identificador de la foto en el almacén (Cloudinary)
-- Hace falta para poder borrar la foto si el ciudadano pide suprimir sus datos (Ley 1581):
-- la url sola no alcanza para pedirle a Cloudinary que la borre. Nulo en las filas que ya existan.
-- =====================================================================

ALTER TABLE evidencia_reporte ADD COLUMN id_publico_proveedor VARCHAR(255);
