# Limpieza revisable de barrios temporales: sin borrar referencias

**No ejecutada. No es una migración activa. No aplicar en producción sin autorización.**

Auditar primero con `backend/scripts/auditar-barrios-temporales.sql`, que usa una transacción de solo lectura. Confirmar destino, comunas, `interno`, coincidencia del triple ID/comuna/nombre con la fuente de ejemplo, conteos de referencias y **todas** las claves foráneas hacia barrio. Los nombres por sí solos solo identifican candidatos; no prueban quién ejecutó el script.

En la auditoría Neon del 2026-11-07 no había temporales: solo Administración NotiUrba, interno, con cinco referencias desde usuario y cero desde reporte. Por eso `limpieza-revisable.sql` tiene una lista aprobada vacía y acaba en `ROLLBACK`. No introducir IDs copiados del fixture del navegador: sus nombres y asociaciones no coinciden con los triples de `db/datos_ejemplo.sql`.

Si otro entorno contiene temporales, Andrés/responsable debe aprobar cada triple exacto antes de incorporarlo a la lista temporal del SQL. La operación propuesta es `UPDATE barrio SET interno=TRUE` solo para esas identidades y si todavía son públicas. Reejecutarla no cambia una fila ya interna. No existe DELETE, reasignación de IDs ni modificación de usuario/reporte: se preservan los enlaces históricos.

Antes de autorizar un COMMIT, comparar conteos y claves foráneas, probar ambos catálogos ciudadanos, registro, edición de perfiles con residencia temporal y visualización histórica de reportes. La regla actual de `CatalogosRegistro` rechaza elegir internos nuevos; revisar la experiencia de usuarios existentes y pedir que elijan un barrio aprobado cuando corresponda, sin asignarlo automáticamente. Corregir la residencia exige confirmación del ciudadano y correspondencia validada; no reutilizar IDs de ejemplos ni inferirlos por comuna.

Conservar respaldo y auditoría de la lista y del cambio. Un eventual borrado físico es otro trabajo, sujeto a cero referencias y aprobación explícita; no forma parte de esta propuesta. No es necesario ni está autorizado aplicar V17 para excluir los temporales.

Prevención: el bloque de barrios de `db/datos_ejemplo.sql` ahora inserta ejemplos con `interno=TRUE`, exige V15 y conserva `ON CONFLICT(id_barrio) DO NOTHING`. No se ejecutó el script completo, que contiene otros datos históricos de demostración. Este cambio evita nuevas opciones públicas; no modifica filas ya existentes.
