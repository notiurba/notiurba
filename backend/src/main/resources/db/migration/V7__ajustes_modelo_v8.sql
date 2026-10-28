-- =====================================================================
-- NotiUrba · V7: ajustes del modelo v8
-- · B3 pasa a ser «GPS o barrio» cuando la categoría exige ubicación.
-- · reporte.radicado_oficial: número que asigna la Alcaldía (lo escribe el ciudadano).
-- · categoria.descripcion: ejemplos de incidentes que muestra la app en cada tarjeta
--   (editables desde la base; la app no los tiene escritos).
-- · Asegura el tipo de token «Recuperación de contraseña» (se reutiliza token_verificacion).
-- reporte.ubicacion ya se llama así desde V1 y acepta NULL: no hay que renombrar.
-- =====================================================================

-- B3 · B6 · B9 · código de seguimiento y estado inicial (al insertar un reporte)
-- Igual que en V2, salvo B3: si la categoría exige ubicación, basta el GPS o el barrio.
CREATE OR REPLACE FUNCTION fn_reporte_antes_insertar() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  exige_ubicacion BOOLEAN;
  dias INT;
BEGIN
  -- B9: vigencia congelada al crear
  SELECT valor::int INTO dias FROM configuracion WHERE clave = 'vigencia_dias';
  IF dias IS NULL THEN
    RAISE EXCEPTION 'Falta configuracion.vigencia_dias' USING ERRCODE = 'check_violation';
  END IF;
  NEW.vigente_hasta := NEW.creado_en + make_interval(days => dias);

  -- Código de seguimiento: NU-000001 (se amplía si supera 6 dígitos)
  IF NEW.codigo_seguimiento IS NULL THEN
    NEW.codigo_seguimiento := 'NU-' || lpad(NEW.id_reporte::text, greatest(6, length(NEW.id_reporte::text)), '0');
  END IF;

  -- Estado inicial: el de menor orden (Recibido)
  IF NEW.id_estado_reporte IS NULL THEN
    SELECT id_estado_reporte INTO NEW.id_estado_reporte FROM estado_reporte ORDER BY orden LIMIT 1;
  END IF;

  -- B3: categorías que exigen ubicación (GPS o barrio)
  SELECT requiere_ubicacion INTO exige_ubicacion FROM categoria WHERE id_categoria = NEW.id_categoria;
  IF exige_ubicacion AND NEW.ubicacion IS NULL AND NEW.id_barrio IS NULL THEN
    RAISE EXCEPTION 'La categoría exige ubicación: GPS o barrio' USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;

-- Radicado oficial de la Alcaldía (sin UNIQUE: lo escribe el ciudadano).
-- No reemplaza a codigo_seguimiento (NU-000000), que es de NotiUrba.
ALTER TABLE reporte ADD COLUMN radicado_oficial VARCHAR(50);

-- Ejemplos de incidentes por categoría (idempotente)
UPDATE categoria SET descripcion = 'Huecos, grietas, andenes rotos'          WHERE nombre = 'Vías, huecos y andenes';
UPDATE categoria SET descripcion = 'Bombillo apagado, ruido, riñas'          WHERE nombre = 'Alumbrado, espacio público, convivencia y seguridad';
UPDATE categoria SET descripcion = 'Basura acumulada, quema, tala'           WHERE nombre = 'Basuras, ambiente, árboles y contaminación';
UPDATE categoria SET descripcion = 'Semáforo dañado, señal caída'            WHERE nombre = 'Señalización, semáforos y tránsito';
UPDATE categoria SET descripcion = 'Fuga de agua, sin agua, caño tapado'     WHERE nombre = 'Acueducto y alcantarillado';
UPDATE categoria SET descripcion = 'Apagón, cable caído, transformador'      WHERE nombre = 'Energía eléctrica';
UPDATE categoria SET descripcion = 'Olor a gas, sin servicio de gas'         WHERE nombre = 'Gas natural';
UPDATE categoria SET descripcion = 'Camión no pasó, basura sin recoger'      WHERE nombre = 'Aseo (falla del servicio)';
UPDATE categoria SET descripcion = 'Derrumbe, inundación, árbol por caer'    WHERE nombre = 'Riesgo de desastres';
UPDATE categoria SET descripcion = 'Casa en mal estado, riesgo de colapso'   WHERE nombre = 'Vivienda';
UPDATE categoria SET descripcion = 'Plagas, aguas estancadas, alimentos'     WHERE nombre = 'Riesgo sanitario y salud pública';
UPDATE categoria SET descripcion = 'Construcción sin licencia, invasión'     WHERE nombre = 'Uso del suelo, planeación y licencias';
UPDATE categoria SET descripcion = 'Problema en un colegio o escuela'        WHERE nombre = 'Instituciones educativas';
UPDATE categoria SET descripcion = 'Una pregunta o trámite en la Alcaldía'   WHERE nombre = 'Solicitud de información';
UPDATE categoria SET descripcion = 'Un evento o caso que no encaja'          WHERE nombre = 'Otro / eventos comunitarios';

-- Recuperar contraseña: V3 ya siembra este tipo (id 2); se asegura por si falta
INSERT INTO tipo_token (nombre)
SELECT 'Recuperación de contraseña'
WHERE NOT EXISTS (SELECT 1 FROM tipo_token WHERE nombre = 'Recuperación de contraseña');
