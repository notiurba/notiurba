# Tablas para el draw.io — cambios de V22 (NOT-132)

Diagrama: https://drive.google.com/file/d/1fW4OvzK5Ftd6oeCLnb7mIpD7P_H4GX7y/view

Cambian **tres cosas**: dos tablas nuevas (`distancia_duplicado` e `intento_login`) y una columna nueva en
`reporte`. Pegue cada bloque como texto de la tabla. `PK` llave primaria, `FK` llave foránea.

## 1. Tabla NUEVA: `distancia_duplicado`

```
distancia_duplicado
──────────────────────────────────────────────────────────
PK FK  id_categoria          INT          → categoria (borrado en cascada)
       metros                SMALLINT     NOT NULL   (5 a 500)
FK     id_usuario_actualiza  BIGINT       → usuario (nulo al borrar)
       actualizado_en        TIMESTAMPTZ  NOT NULL   DEFAULT now()
```

Relación: `categoria` 1 — 0..1 `distancia_duplicado`. Filas: una por cada una de las 10 categorías con
ubicación (1, 2, 3, 4, 5, 8, 11, 12, 13, 14).

## 2. Tabla NUEVA: `intento_login` (sin relaciones)

```
intento_login
──────────────────────────────────────────────────────────
PK  correo_normalizado   VARCHAR(255)
    fallidos             SMALLINT     NOT NULL   DEFAULT 0
    primer_fallo_en      TIMESTAMPTZ
    bloqueado_hasta      TIMESTAMPTZ
    actualizado_en       TIMESTAMPTZ  NOT NULL   DEFAULT now()
```

No tiene llave hacia `usuario` a propósito: cuenta igual los correos que no existen.

## 3. Tabla EXISTENTE: `reporte` (agregar una columna)

```
    requiere_evidencia   BOOLEAN      NOT NULL   DEFAULT FALSE
```

## 4. Datos (no estructura)

`configuracion` recibe 5 claves con su rango: `login_intentos_maximos` (3), `login_ventana_minutos` (5),
`login_bloqueo_minutos` (5), `reportes_maximos_por_dia` (2), `duplicado_ventana_dias` (30).
