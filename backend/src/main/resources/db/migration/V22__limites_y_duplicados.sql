-- Límites contra el abuso: intentos de login, reportes por día y reportes parecidos.
-- Aditiva: no borra ni cambia datos existentes.

-- 1. Parámetros que el administrador puede ajustar (cada uno con su rango).
INSERT INTO configuracion (clave, valor) VALUES
    ('login_intentos_maximos', '3'),
    ('login_ventana_minutos', '5'),
    ('login_bloqueo_minutos', '5'),
    ('reportes_maximos_por_dia', '2'),
    ('duplicado_ventana_dias', '30');

ALTER TABLE configuracion
    ADD CONSTRAINT ck_config_login_intentos
        CHECK (clave <> 'login_intentos_maximos' OR valor ~ '^([1-9]|10)$'),
    ADD CONSTRAINT ck_config_login_ventana
        CHECK (clave <> 'login_ventana_minutos' OR valor ~ '^([1-9]|[1-5][0-9]|60)$'),
    ADD CONSTRAINT ck_config_login_bloqueo
        CHECK (clave <> 'login_bloqueo_minutos' OR valor ~ '^([1-9]|[1-5][0-9]|60)$'),
    ADD CONSTRAINT ck_config_reportes_por_dia
        CHECK (clave <> 'reportes_maximos_por_dia' OR valor ~ '^([1-9]|[1-4][0-9]|50)$'),
    ADD CONSTRAINT ck_config_duplicado_ventana
        CHECK (clave <> 'duplicado_ventana_dias' OR valor ~ '^([1-9]|[12][0-9]|30)$');

-- 2. Distancia, en metros, a la que dos reportes de la misma categoría se consideran parecidos.
-- Una fila por categoría que exige ubicación; sin fila no se revisa (no hay punto que comparar).
CREATE TABLE distancia_duplicado (
    id_categoria INT PRIMARY KEY REFERENCES categoria(id_categoria) ON DELETE CASCADE,
    metros SMALLINT NOT NULL,
    id_usuario_actualiza BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_distancia_duplicado_metros CHECK (metros BETWEEN 5 AND 500)
);

INSERT INTO distancia_duplicado (id_categoria, metros) VALUES
    (1, 5), (3, 5), (4, 8), (2, 10), (5, 10), (11, 10), (8, 15), (12, 15), (13, 15), (14, 15);

-- 3. Intentos de login fallidos por correo. No guarda contraseñas ni IP, y no depende de que el
-- correo exista, así que un correo inventado se bloquea igual que uno real.
CREATE TABLE intento_login (
    correo_normalizado VARCHAR(255) PRIMARY KEY,
    fallidos SMALLINT NOT NULL DEFAULT 0,
    primer_fallo_en TIMESTAMPTZ,
    bloqueado_hasta TIMESTAMPTZ,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_intento_login_actualizado ON intento_login (actualizado_en);

-- 4. Un reporte que repite uno ya resuelto exige evidencia: la marca la pone el sistema.
ALTER TABLE reporte ADD COLUMN requiere_evidencia BOOLEAN NOT NULL DEFAULT FALSE;

-- Cuenta de reportes por ciudadano y día.
CREATE INDEX ix_reporte_usuario_creado ON reporte (id_usuario, creado_en);
