# V17 candidata: revisión y autorización del catálogo de barrios

**Estado: pendiente de aprobación. No ejecutar ni copiar todavía a una ubicación Flyway activa.**

`V17__catalogo_barrios_pmgr_2020.sql` tiene nombre válido para Flyway, es una migración nueva y conserva exactamente el INSERT idempotente validado previamente. La carpeta actual no está incluida en `spring.flyway.locations`. No se cambiaron V1–V16 ni se añadió una ubicación activa alternativa.

## Datos confirmados y límites

Confirmados: transcripción de las 135 filas de la tabla municipal de diciembre de 2020; sus asociaciones históricas a las diez comunas; ausencia de duplicados exactos por `(comuna, nombre)`; idempotencia e integridad demostradas anteriormente en PostgreSQL desechable; correspondencia entre la respuesta de QA y el selector del registro.

Pendientes: vigencia y cobertura administrativa en 2026 de **las 135 asociaciones**, y resolución de las incidencias de nombres. Integridad de la clave foránea o coincidencia con el PDF no acreditan vigencia territorial actual. No hay polígonos de barrios ni se deduce un barrio de coordenadas en esta carga.

Fuente y evidencia: [metadatos municipales](../../../docs/validacion/evidencias-catalogo-barrios-20261107/fuente.json), [CSV](../../../docs/validacion/evidencias-catalogo-barrios-20261107/barrios-pmgr-2020.csv), [transcripción por página](../../../docs/validacion/evidencias-catalogo-barrios-20261107/transcripcion.json), [informe completo](../../../docs/validacion/2026-11-07-bloqueo-catalogo-barrios.md).

## Decisiones que Andrés debe aprobar

| ID | Fuente | Decisión requerida | Estado |
|---|---|---|---|
| B01 | «El Principio», comuna 5, p. 18 | Confirmar nombre oficial. No sustituir por «El Príncipe» sin respaldo documental. | Pendiente |
| B02 | «Urbanización Lusitania» y «Lusitania», comuna 5, p. 18 | Confirmar entidades distintas o alias; resolver sin eliminar referencias existentes. | Pendiente |
| B03 | «Diablos Rojos», comunas 7 y 8, pp. 19–20 | Confirmar ambas asociaciones o documentar corrección. No deduplicar globalmente por nombre. | Pendiente |
| B04 | «San Luis» / «Urbanización San Luis», comuna 8, p. 20 | Confirmar si las variantes designan entidades distintas. No es un duplicado demostrado. | Pendiente |
| B05 | «Santa Inés» / «Santa Inés - Comfamiliar», comuna 8, p. 20 | Confirmar si las variantes designan entidades distintas. No es un duplicado demostrado. | Pendiente |

No hay duplicados del par comuna/nombre, incluso normalizando acentos, mayúsculas y espacios. El nombre duplicado normalizado entre comunas es «Diablos Rojos». La numeración global del PDF repite 41 y omite 46; se usa exclusivamente para trazabilidad, nunca para fijar `id_barrio`. «Cgto. Aguaclara» queda excluido por figurar como corregimiento.

Andrés debe obtener o adjuntar confirmación del DAPM/responsable de datos para la vigencia y las decisiones anteriores. Después debe aprobar expresamente:

1. CSV definitivo, nombres y correspondencias barrio-comuna, cobertura y exclusión del administrativo y corregimientos.
2. Resolución de B01–B05 con fuente, fecha y responsable; si acepta la fuente histórica provisional, documentar esa decisión y su alcance, sin presentarla como catálogo vigente confirmado.
3. SQL definitivo, comportamiento solo aditivo y estrategia ante nombres existentes. `ON CONFLICT DO NOTHING` no corrige alias, asociaciones erróneas ni registros internos; revisar el destino antes de la carga.
4. Hashes finales del PDF, CSV y SQL, número de filas, conteos por comuna y resultados de validación vinculados a esos bytes.
5. Versión Flyway libre, destino exacto, responsable y ventana de aplicación, respaldo/restauración y autorización explícita para escritura. No se solicita ni se registra aprobación ficticia en este documento.

[Registro de decisiones pendiente y hashes](../../../docs/validacion/evidencias-catalogo-barrios-20261107/revision-andres.json): `aprobadoPor`, `aprobadoEn`, decisiones y evidencias siguen en `null`.

## Condiciones y secuencia para aplicar después de la autorización

1. Resolver o aceptar expresamente las limitaciones anteriores. Si el catálogo cambia, guardar una versión aprobada nueva y regenerar SQL y hashes; conservar CSV/propuesta/evidencias históricas. No reinterpretar resultados anteriores como pruebas de un SQL diferente.
2. Con acceso autorizado, revisar en lectura barrios públicos/internos y comunas del destino, nombres equivalentes y `flyway_schema_history`. V17 es candidata porque el checkout actual llega a V16; reconfirmar que esté libre al aplicar. Si se ocupó, asignar el siguiente número disponible a la migración todavía no aplicada.
3. Si cambia el DML, validarlo en base desechable: primera y segunda ejecución, referencias, filtros y registro. El DML actual es idéntico al ya aprobado técnicamente; no se repitió esa ejecución en esta preparación.
4. Con aprobación documentada, promover **solo** la migración definitiva a `backend/src/main/resources/db/migration/` y ejecutar Flyway mediante el proceso autorizado. No aplicar el SQL manualmente y luego volver a ejecutarlo como migración; no editar migraciones aplicadas, hacer `repair`, desactivar filtros o habilitar el barrio interno.
5. Verificar en el destino autorizado GET público, IDs/comunas, orden, exclusión de internos, selector con mocks desactivados, conservación al retroceder y registro autorizado. Registrar resultados nuevos antes de dar por desbloqueadas NOT-59/NOT-71.

## Verificación adicional de esta preparación

```powershell
node --test docs/validacion/evidencias-catalogo-barrios-20261107/verificar-preparacion.test.mjs
```

Resultado guardado: [5/5 aprobadas](../../../docs/validacion/evidencias-catalogo-barrios-20261107/preparacion-resultados.txt). Son comprobaciones nuevas de artefactos y renderizado del componente real con la respuesta QA guardada; no son un nuevo recorrido Chrome ni una validación en producción. Verifican que los IDs/opciones provienen del CSV municipal histórico y que el registro no muestra el administrativo. No convierten la fuente histórica en confirmación de vigencia.

Las 3 pruebas backend, 32 frontend y evidencias Chrome anteriores se reutilizan sin reejecutarlas. No se consultó Neon ni Linear en esta preparación, ni hubo escrituras externas, despliegues, commits, push o PR.

## Revisión de habilitación — 2026-11-08

**V17 bloqueada para producción por B01–B05 y vigencia del catálogo.** No consta decisión nueva en `revision-andres.json`; no se modifica la transcripción para resolver dudas por intuición. Las 135 asociaciones corresponden a la fuente histórica de 2020, no a una certificación administrativa actual.

La consulta actual a Neon, en transacción de solo lectura, confirma `neondb/public`, comunas con IDs 1–10, 0 barrios públicos, 1 interno (`Administración NotiUrba`, ID 1/comuna 1), unicidad `(id_comuna,nombre)`, FK restrictivas y última migración V16 correcta. Los hashes de CSV/SQL coinciden con el registro y el DML sigue idéntico a la propuesta probada. Distribución por comunas 1–10: 12/8/21/5/16/16/21/18/15/3; sin duplicados exactos dentro de una comuna. Estas comprobaciones no resuelven B01–B05.

Se reutilizan las pruebas aisladas y el recorrido Chrome guardados: 135 opciones sin mocks, selección conservada, exclusión del interno e idempotencia previamente comprobada. No se repiten las suites. Dos solicitudes actuales a Render agotaron 25 segundos, también fuera del sandbox: no hubo respuesta HTTP; `000` de curl no equivale a un estado del servidor. El resultado anterior era HTTP 200 `[]`. No se afirma una reparación en Render ni en Netlify.

[Evidencia de esta revisión](../../../docs/validacion/evidencias-catalogo-barrios-20261107/auditoria-cierre-20261108.json). No hubo escrituras en Neon ni promoción de V17.

### Respaldo y recuperación previstos — no ejecutados

1. Tras resolver las decisiones, aprobar el hash definitivo, el destino y la ventana. Revalidar que V17 siga libre y revisar las migraciones pendientes del ejecutor antes de cualquier `migrate`.
2. El responsable autorizado obtiene un respaldo lógico completo con `pg_dump --format=custom` mediante conexión directa y credenciales gestionadas, sin imprimirlas ni guardarlas en Git. Guardarlo cifrado fuera del repositorio, con fecha, hash y acceso restringido; contiene datos personales. Registrar por separado IDs/nombres/estado interno de barrios y conteos de usuarios/reportes/historial Flyway previos.
3. Verificar el respaldo con `pg_restore --list` y restaurarlo en una base desechable aislada; comprobar integridad y conteos antes de autorizar la carga. Un plan escrito no acredita que ya exista un respaldo recuperable.
4. Usar el ejecutor Flyway autorizado, `validate`, revisión de `info` y luego `migrate`, con conexión directa. No ejecutar manualmente el INSERT ni alterar checksums, `repair` o migraciones aplicadas. No implica autorización para desplegar Render/Netlify.
5. Ante fallo, detener el proceso y verificar el estado real de la transacción/historial antes de reintentar. Si la carga se confirmó y se detecta un problema, detener nuevas altas según el procedimiento operativo autorizado y preparar una migración correctiva nueva que conserve IDs y referencias. No borrar masivamente ni restaurar sobre producción: la restauración inicial será aislada; cualquier recuperación de producción requiere decisión explícita del responsable y plan para preservar escrituras posteriores.

### Condiciones para NOT-71

Primero: decisión documentada sobre B01–B05 y uso histórico, autorización explícita del usuario para escribir en producción y ejecución oficial de Flyway. Después: verificar 135 públicos y el interno intacto, API con IDs/comunas válidos, selector del despliegue real sin ficticios y conservación de selección. Solo con autorización adicional para cuentas/reportes de prueba se verificará el recorrido Netlify + Render: registro, acceso, reporte, comuna/entidad, URL persistida y código recuperable. Las evidencias locales de NOT-20/21/23/25/59 no acreditan por sí solas ese recorrido desplegado.

## Contraste documental focalizado y preparación NOT-71 — 2026-11-08

Fuentes oficiales consultadas el 8 de noviembre de 2026 (fecha del documento, no del rastreo web):

- **S1, PMGR V3, diciembre 2020**, tabla 4, pp. 18-20: [Alcaldía, idFile 12112](https://tulua.gov.co/loader.php?lServicio=Tools2&lTipo=descargas&lFuncion=exposeDocument&idFile=12112). PDF local y transcripción ya conservados.
- **S2, Informe final de actualización catastral, diciembre 2024**, Valle Avanza SAS, sección 3.4, pp. 17-18: [publicación municipal, idFile 22080](https://tulua.gov.co/loader.php?lServicio=Tools2&lTipo=descargas&lFuncion=exposeDocument&idFile=22080). Se leyó el texto recuperado/indexado del PDF; descarga visual de páginas rechazada con HTTP 403. No se afirma haber descargado ni verificado su hash o cartografía. No constituye certificación de vigencia en 2026.

| Barrio | Comuna histórica | Hallazgo | Fuente | Decisión propuesta |
|---|---|---|---|---|
| El Principio | 5 | S1 usa ese nombre; S2 enumera El Príncipe. | S1 p.18; S2 p.17 | Posible error de denominación: confirmar correspondencia con DAPM antes de renombrar. B01 abierta. |
| Lusitania / Urbanización Lusitania | 5 | S2 distingue Lusitania y Conjunto Residencial Lusitania. | S1 p.18; S2 p.17 | Conservar separación; no está demostrada la equivalencia Urbanización/Conjunto. B02 abierta. |
| Diablos Rojos | 7 y 8 | S2 distingue I en 7 y II en 8. | S1 pp.19-20; S2 p.18 | B03 aclarada documentalmente: conservar ambas asociaciones; Andrés debe aprobar los nombres definitivos. |
| San Luis / Urbanización San Luis | 8 | S2 enumera San Luis; no enumera la segunda variante. | S1 p.20; S2 p.18 | La ausencia no demuestra fusión ni baja. B04 abierta; solicitar equivalencias municipales. |
| Santa Inés / Santa Inés-Comfamiliar | 8 | S2 enumera ambas por separado. | S1 p.20; S2 p.18 | B05 respaldada documentalmente: conservar dos entradas; Andrés debe aprobar denominaciones. |

B03/B05 tienen aclaración documental, no aprobación registrada. B01/B02/B04 requieren validación municipal; todas las decisiones B01-B05 necesitan aprobación de Andrés. Ninguna fuente examinada exige trasladar estas entradas a otra comuna; tampoco prueba que los nombres de 2020 sean todos válidos hoy. Los 135 registros se conservan como evidencia histórica; no se certifica su carga íntegra como catálogo vigente. No se modifica CSV, SQL, revision-andres.json ni cartografía. La lista de 2024 no se incorpora automáticamente: esta investigación abarca solo cinco discrepancias.

### Verificación técnica sin ejecutar SQL

CSV y V17 coinciden en sus 135 pares; 0 duplicados exactos, IDs de comuna entre 1 y 10. Hashes iguales a revision-andres.json. V17 contiene únicamente carga aditiva y ON CONFLICT; no incluye el nombre interno ni fija IDs de barrio. Se reutilizan las evidencias de CatalogoCargaBarriosIT (primera carga 135, segunda 0, 136 totales, FK válidas e interno excluido), 32 pruebas frontend y 5 de preparación; no se reejecutaron. El estado del interno en producción procede de la auditoría previa, sin nueva consulta.

El checkout conserva V1-V16 y ninguna V17 activa; no hay colisión local. Revalidar historial y versión libre en el destino justo antes de una ejecución autorizada. Respaldo de código/evidencias verificado: C:\notiurba\respaldo-registro-pr20-20261108-120557-9331 (501 archivos y bundle). **Ese respaldo no es un pg_dump de producción.** El procedimiento de recuperación anterior existe, pero el respaldo de base y su restauración aislada siguen pendientes del responsable autorizado.

**V17 continúa bloqueada:** confirmar B01/B02/B04, aprobar las cinco decisiones y vigencia/alcance histórico, fijar CSV/SQL definitivos y autorizar destino, respaldo de base y carga Flyway. No se promueve V17.

### NOT-71: reutilización y pendientes integrales

- NOT-20/NOT-23: se reutiliza el antecedente registrado en NOT-25; no se presenta como nueva comprobación de Cloudinary o dispositivos.
- NOT-21: se reutilizan docs/contexto/06-comunas-y-territorio.md y docs/gis-not31/README.md. Describen asignación por coordenadas y capa experimental; no certifican exactitud administrativa ni el recorrido desplegado. El barrio del usuario no prueba la comuna de un reporte.
- NOT-25: reutilizar NOT-25-final-20261107/NOT-25-informe.md y persistencia-postgresql.json: código recuperable, URL JPEG persistida y reintento probados en QA desechable con almacén local; sin verificar proveedor productivo ni entrega efectiva a entidad.
- NOT-59: reutilizar evidencias-NOT-59-20261108/resultados.json: rechazo 409 de correo duplicado, sin alta ni correo enviados, en entorno aislado.

| Paso NOT-71 | Posible sin escribir producción | Pendiente integral |
|---|---|---|
| Registro | Inspeccionar formulario, API de catálogo y estado vacío mediante GET. No ejecutado nuevamente aquí. | Alta ciudadana, correo y consentimiento: bloqueados por catálogo vacío y autorización de datos de prueba. |
| Login | Inspección de ruta; acceso con cuenta ya autorizada no depende intrínsecamente del catálogo. | Sesión de la nueva cuenta del recorrido, aún inexistente. |
| Categoría, ubicación y foto | Revisar UI, permisos locales y vista previa sin enviar; reutilizar QA. | POST de reporte/foto requiere autorización y cuenta. |
| Comuna y entidad | Revisar reglas y configuración en lectura, con acceso autorizado. | Demostrar asignación y destino del mismo reporte persistido; no enviar correos a entidades. |
| URL, código y confirmación | Revisar evidencias QA ya guardadas. | POST/GET autenticado del caso autorizado en Netlify/Render y URL recuperable. |

No se abrió un nuevo recorrido desplegado ni se realizaron solicitudes a producción. El catálogo vacío impide iniciar el alta completa; no demuestra que todo login o toda consulta esté averiada. Tras resolver datos, hacen falta autorización específica y cuenta/foto sintéticas para demostrar el recorrido conjunto. **NOT-71 no completada.**