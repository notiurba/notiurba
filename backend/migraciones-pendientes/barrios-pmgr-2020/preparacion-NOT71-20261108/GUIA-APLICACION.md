# Catálogo histórico de barrios: preparación para aprobación

Estado al 8 de noviembre de 2026: **candidata, sin autorización productiva**. Este paquete no aplica ni despliega nada. La candidata original sigue en [V17](../V17__catalogo_barrios_pmgr_2020.sql), fuera de Flyway activo, sin cambios. Su SHA-256 es `e824e2b71011938b8c0666f07b302ac537a81bd27865689055048fb503dd4fec`.

## Verificación realizada

El [manifiesto](validacion-artefactos.json) contrasta exactamente SQL, CSV histórico y respuesta real del backend QA 18091: 135 pares, sin duplicados exactos ni normalizados dentro de una comuna; nombres de hasta 27 caracteres, compatibles con `barrio.nombre VARCHAR(100)`. Comunas 1–10 y distribución 12/8/21/5/16/16/21/18/15/3. No se fijan IDs de barrio: la identidad de carga es `(id_comuna,nombre)`.

La API QA actual devuelve 135 filas, pero no sustituye una nueva inspección SQL de FK/historial. Se reutiliza `CatalogoCargaBarriosIT` anterior: primera carga 135, segunda 0, 136 totales incluyendo el interno, FK válidas e interno excluido. **Los SQL nuevos no se ejecutaron en esta sesión:** acceso Docker denegado y sin conexión SQL autenticada disponible. No se afirma una nueva prueba Flyway ni una restauración de respaldo.

## Archivos preparados

| Archivo | Uso y resultado esperado |
|---|---|
| `01-preflight.sql` | Solo lectura: destino, historial, inventario de barrios, restricciones, comunas faltantes, coincidencias internas y similitudes. |
| `02-postflight.sql` | Solo lectura: 135 claves naturales públicas presentes; ausentes, duplicados y FK huérfanas = 0; comparar inventario antes/después. |
| `03-qa-idempotencia.sql` | Solo QA precargada: dos reejecuciones de V17 original, comparación completa y ROLLBACK. No sustituye la primera carga en vacío. |
| `04-persistencia-QA.sql` | Solo lectura del usuario sintético creado y sus reportes/fotos/destinos. No imprime hashes o tokens. |

Ejecutar con un cliente `psql` autorizado, `-X -v ON_ERROR_STOP=1 -f <archivo>`. El SQL 03 usa `\ir` relativo a su propio archivo; requiere que la candidata y esta carpeta estén disponibles juntas. No pegarlo por stdin sin resolver esas rutas. Solo se permite probarlo en `notiurba_barrios_qa` como `qa_barrios`, conexión del host a `127.0.0.1:15435`, o contenedor QA designado. No usar Neon. El número SERIAL puede avanzar con intentos `ON CONFLICT` incluso tras ROLLBACK; no resecuenciar ni reutilizar IDs para ocultarlo.

## Decisiones de datos pendientes

| ID | Decisión que debe aprobar Andrés con fuente y responsable |
|---|---|
| B01 | El Principio (2020) frente a El Príncipe (2024): confirmar equivalencia y denominación. |
| B02 | Lusitania / Urbanización Lusitania: confirmar separación y equivalencia con Conjunto Residencial Lusitania. |
| B03 | Diablos Rojos en 7 y 8: la revisión previa de 2024 distingue I/II; aprobar asociaciones y nombres definitivos. |
| B04 | San Luis / Urbanización San Luis: confirmar identidades; ausencia en otra lista no demuestra baja o fusión. |
| B05 | Santa Inés / Santa Inés-Comfamiliar: respaldo documental previo para conservar ambas; aprobación pendiente. |

Las cinco decisiones siguen sin aprobación en `revision-andres.json`; no se cambió ese registro. Además debe aprobarse la vigencia/cobertura de las 135 asociaciones o aceptar expresamente su uso histórico provisional. Ninguna prueba técnica certifica vigencia administrativa en 2026. No deduplicar globalmente por nombre: Diablos Rojos tiene dos comunas.

## Secuencia futura, solo después de aprobación

1. Aprobar CSV/SQL definitivos y hashes, incidencias, cobertura, destino exacto, responsable y ventana. Si cambian datos, generar una versión nueva conservando la evidencia histórica y repetir las pruebas afectadas en una base aislada.
2. Con conexión de lectura al destino autorizado, ejecutar preflight y guardar salida. Deben existir V16 y las restricciones esperadas, sin historial fallido. Comunas faltantes y coincidencias internas deben devolver 0 filas. Revisar manualmente similitudes, alias y nombres existentes antes de continuar; `ON CONFLICT DO NOTHING` no corrige asociaciones ni publica un interno.
3. Verificar numeración tanto en `flyway_schema_history` como en ramas/migraciones pendientes del ejecutor. En este checkout solo hay V1–V16 activas. Si V17 está ocupada, detenerse y asignar la siguiente versión libre a la candidata aún no aplicada; no editar migraciones ya aplicadas ni ejecutar `repair` para forzar la carga.
4. Obtener y comprobar un respaldo recuperable como se indica abajo. Registrar conteos de barrios públicos/internos, usuarios, reportes, evidencias y todo el historial antes de la ventana. No se afirma que ese respaldo exista hoy.
5. Solo con permiso explícito, promover la candidata aprobada a la ubicación Flyway activa y usar el ejecutor oficial autorizado: inspeccionar `info`, ejecutar `validate`, revisar **todas** las migraciones pendientes y después `migrate`. Mantener transacción y bloqueo de Flyway. No hacer INSERT manual en producción seguido de Flyway ni alterar checksums. Este documento no autoriza despliegues Render/Netlify.
6. Ejecutar postflight. Deben verse 135 coincidencias públicas por claves naturales, 0 ausentes/huérfanas/duplicados. El total público esperado es `públicos_antes + inserciones_previstas`; no exigir 135 totales si ya existían barrios adicionales. Comprobar que todos los IDs/filas originales e internos siguen intactos y que Flyway registró la versión aprobada con éxito.
7. En la API real comprobar HTTP 200 y nombres/comunas de las 135 claves aprobadas, exclusión de internos y coincidencia con la base; los IDs no tienen por qué ser 43/14 como en QA. En Netlify, confirmar su API, mocks desactivados y versión del despliegue autorizado; abrir registro a 360/1366, contar opciones, seleccionar Centro por nombre/comuna y conservarlo al retroceder y cambiar de barrio. Si hay nombres iguales entre comunas, verificar el ID correspondiente; no seleccionar solo por texto ambiguo.
8. Acordar una cuenta y foto sintéticas y una ruta de correo que no alcance entidades. Solo entonces ejecutar el recorrido completo Netlify → Render → base, incluido correo/verificación local o mecanismo de pruebas autorizado, sesión, reporte, foto recuperable y consulta de código/destino/comuna del mismo caso. Esto es lo pendiente para NOT-71.

## Respaldo y recuperación

El responsable autorizado debe usar conexión directa segura y credenciales gestionadas, nunca copiadas al repositorio o a QA. Obtener `pg_dump --format=custom --no-owner` de la base exacta aprobada y guardarlo cifrado fuera del repositorio, con fecha, hash y acceso restringido; contiene datos personales. Guardar también inventarios de claves y las salidas preflight.

Verificar el archivo con `pg_restore --list`, restaurarlo en **otra base vacía aislada** y comprobar conteos, restricciones e historial. Una lista legible no demuestra que la restauración funcione; conservar evidencia de ambas verificaciones. No restaurar ese volcado productivo en la QA sintética de esta tarea ni utilizarlo para pruebas funcionales sin autorización de tratamiento de datos.

Ante fallo, detener nuevas operaciones de migración y comprobar transacción e historial antes de reintentar. Si no confirmó la transacción, revisar la causa y aplicar el procedimiento oficial; no asumir que quedó aplicada. Si la carga se confirmó y es incorrecta, preparar una migración correctiva nueva que preserve IDs y referencias; no ejecutar DELETE masivo ni restaurar encima de producción. Una restauración productiva requiere autorización separada y un plan para conservar escrituras posteriores al respaldo. Primero diagnosticar/restaurar en aislamiento.

## Límites del modelo para la futura E2E

La comuna del reporte se deriva mediante `v_reporte_comuna` (V13), por punto o barrio; no es una columna persistida en `reporte`. El barrio de residencia del usuario no demuestra la comuna del incidente. El destino de `v_reporte_destino` es derivado; contrastarlo además con `registro_envio_correo.destino`, canal y mensaje congelado cuando exista una cola autorizada. La foto se relaciona en `evidencia_reporte`; verificar URL y su apertura sin usar credenciales de producción en QA.
