# Catálogo de barrios (V17) — fuente histórica provisional

**Qué es.** 135 barrios de Tuluá con su comuna, transcritos de la tabla 4 «División político administrativa» del
*Plan Municipal de Gestión del Riesgo V3, diciembre de 2020* (Alcaldía de Tuluá, pp. 16-21). Los preparó y
verificó Luz Amelia (NOT-71). El mismo contenido está en `barrios-pmgr-2020.csv` y en la migración
`backend/src/main/resources/db/migration/V17__catalogo_barrios_pmgr_2020.sql`.

| Comuna | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | Total |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Barrios | 12 | 8 | 21 | 5 | 16 | 16 | 21 | 18 | 15 | 3 | 135 |

**Qué NO es.** No es un catálogo vigente confirmado: es una fuente municipal de 2020 y no acredita la cobertura
administrativa de 2026. Igual que la capa de comunas (V14), se usa como aproximación hasta que el municipio
entregue datos oficiales. El barrio interno «Administración NotiUrba» (V15) no forma parte del catálogo y no se muestra.

## Nombres que siguen abiertos (B01-B05)
Se cargaron **tal como aparecen en la fuente**; no se corrigió ninguno por intuición.

| ID | Barrio (comuna) | Duda |
|---|---|---|
| B01 | «El Principio» (5) | Otra fuente (catastro 2024) dice «El Príncipe». Posible error de denominación. |
| B02 | «Urbanización Lusitania» y «Lusitania» (5) | ¿Son dos barrios o uno con alias? |
| B03 | «Diablos Rojos» (7 y 8) | Documentalmente son dos (I en la 7, II en la 8). Se conservan ambos. |
| B04 | «San Luis» y «Urbanización San Luis» (8) | ¿Entidades distintas? No es un duplicado demostrado. |
| B05 | «Santa Inés» y «Santa Inés - Comfamiliar» (8) | Documentalmente son dos. Se conservan ambos. |

## Cómo corregir después
Los usuarios y reportes referencian el barrio por su id, así que **no se borra ni se reemplaza**: se escribe una
migración nueva (V18, V19…) con `UPDATE barrio SET nombre = … WHERE id_comuna = … AND nombre = …`.
Para agregar barrios nuevos: `python3 backend/scripts/generar-barrios.py nuevos.csv > …/V18__….sql`.
