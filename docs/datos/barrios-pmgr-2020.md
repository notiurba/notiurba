# Catálogo de barrios (V17) — fuente histórica provisional

**Qué es.** 133 barrios de Tuluá con su comuna, tomados de la tabla 4 «División político administrativa» del
*Plan Municipal de Gestión del Riesgo V3, diciembre de 2020* (Alcaldía de Tuluá, pp. 16-21), con 5 decisiones de
nombres resueltas por Andrés López el 2026-11-08 (abajo). Los preparó y verificó Luz Amelia (NOT-71) como 135 filas.
El mismo contenido está en `barrios-pmgr-2020.csv` y en la migración
`backend/src/main/resources/db/migration/V17__catalogo_barrios_pmgr_2020.sql`.

| Comuna | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | Total |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Barrios | 12 | 8 | 21 | 5 | 15 | 16 | 21 | 17 | 15 | 3 | 133 |

**Qué NO es.** No es un catálogo vigente confirmado: es una fuente municipal de 2020 y no acredita la cobertura
administrativa de 2026. Igual que la capa de comunas (V14), se usa como aproximación hasta que el municipio
entregue datos oficiales. El barrio interno «Administración NotiUrba» (V15) no forma parte del catálogo y no se muestra.
Regla: **un barrio pertenece a una sola comuna**.

## Decisiones sobre los nombres dudosos (B01-B05)
Resueltas por Andrés López el 2026-11-08 con apoyo en el *Informe final de actualización catastral* (Valle Avanza SAS,
diciembre 2024, publicado por la Alcaldía; sección 3.4, p. 18 del PDF). El PDF lista, por comuna: «Diablos Rojos I»
(comuna 7); «Chiminangos» y «Los Diablos Rojos II» (comuna 8); «El Príncipe» (comuna 5).

| ID | Fuente 2020 | Decisión | Resultado en el catálogo |
|---|---|---|---|
| B01 | «El Principio» (5) | El barrio se llama **El Príncipe** (así lo trae también el catastro 2024). | `5, El Príncipe` |
| B02 | «Urbanización Lusitania» y «Lusitania» (5) | Se deja solo **Lusitania**. | `5, Lusitania` |
| B03 | «Diablos Rojos» (7 y 8) | Son **dos barrios distintos** y cada uno está en una sola comuna: I en la 7 y II en la 8 (el de la 8 es vecino de Chiminangos). | `7, Diablos Rojos I` y `8, Diablos Rojos II` |
| B04 | «San Luis» y «Urbanización San Luis» (8) | Se deja solo **San Luis**. | `8, San Luis` |
| B05 | «Santa Inés» y «Santa Inés - Comfamiliar» (8) | Son **dos barrios distintos** en la comuna 8 (confirmado por Andrés). El segundo se escribe como en el catastro 2024. | `8, Santa Inés` y `8, Santa Inés de Comfamiliar` |

## Cómo corregir después
Los usuarios y reportes referencian el barrio por su id, así que **no se borra ni se reemplaza**: se escribe una
migración nueva (V18, V19…) con `UPDATE barrio SET nombre = … WHERE id_comuna = … AND nombre = …`.
Para agregar barrios nuevos: `python3 backend/scripts/generar-barrios.py nuevos.csv > …/V18__….sql`.
