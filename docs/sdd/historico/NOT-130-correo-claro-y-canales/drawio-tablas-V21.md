# Tablas para el draw.io — cambios de V21 (NOT-130)

Diagrama: https://drive.google.com/file/d/1fW4OvzK5Ftd6oeCLnb7mIpD7P_H4GX7y/view

Solo cambian **dos tablas**: una nueva (`finalidad_canal`) y una existente (`canal_atencion`, reemplazarla
entera). Pegue cada bloque como texto de la tabla. `PK` llave primaria, `FK` llave foránea, `UQ` único.

> El diagrama también está desactualizado respecto a V16 a V20 (enrutamiento y envíos, barrio interno,
> catálogo PMGR, comuna en el reporte). Eso no va aquí; dígame si quiere otro archivo igual para ellas.

## 1. Tabla NUEVA: `finalidad_canal` (catálogo)

```
finalidad_canal
─────────────────────────────────────
PK  id_finalidad_canal   SMALLSERIAL
UQ  nombre               VARCHAR(40)   NOT NULL
```

Filas: 1 Radicar PQR · 2 Reportar daño · 3 PQR anónimo · 4 Consultar estado · 5 Emergencia · 6 Información.

## 2. Tabla EXISTENTE: `canal_atencion` (reemplazar completa)

Las 5 últimas columnas son las nuevas.

```
canal_atencion
────────────────────────────────────────────────────────────
PK  id_canal_atencion     SERIAL
FK  id_tipo_canal         SMALLINT      NOT NULL   → tipo_canal
    acceso                VARCHAR(500)  NOT NULL
    detalle               VARCHAR(500)
FK  id_entidad_destino    INT                      → entidad_destino
FK  id_dependencia        INT                      → dependencia
    es_principal          BOOLEAN       NOT NULL   (por defecto FALSE)
    activo                BOOLEAN       NOT NULL   (por defecto TRUE)
FK  id_finalidad_canal    SMALLINT      NOT NULL   → finalidad_canal   (por defecto 6)   ← NUEVA
    orden                 SMALLINT      NOT NULL   (por defecto 100)                     ← NUEVA
    horario               VARCHAR(200)                                                   ← NUEVA
    fuente                VARCHAR(500)                                                   ← NUEVA
    verificado_en         DATE                                                           ← NUEVA
```

Reglas que no cambian: pertenece a una entidad **o** a una dependencia (nunca ambas ni ninguna); solo un
canal principal por entidad y tipo; un canal principal debe estar activo y tener entidad.

## 3. Relación NUEVA

| Desde | Hasta | Tipo | Texto de la línea |
| --- | --- | --- | --- |
| `finalidad_canal` | `canal_atencion` | 1 a N | «para qué sirve» (ON DELETE RESTRICT) |

Las demás relaciones de `canal_atencion` (con `tipo_canal`, `entidad_destino` y `dependencia`) no cambian.
