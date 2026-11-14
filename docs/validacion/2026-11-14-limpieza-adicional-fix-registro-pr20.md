# Limpieza adicional — fix/registro-pr20 — 14/11/2026

**Estado: limpieza de los candidatos comprobados completada; NOT-141 sigue En progreso.** No se han borrado archivos físicos ni realizado commits, push, merges o despliegues.

## Resumen

- Rama: fix/registro-pr20. HEAD inicial y final: d8e8b2bee07a3e4e59846f9fbde803a4b044fa95.
- PR #27 consultado: abierto, draft, base main (5c8737813d45132bee7765fd88bd848b9aa53598), head fix/registro-pr20 (d8e8b2b). No se cambió la base.
- Inventario inicial: 831 archivos versionados y 132 no versionados, 963 en total. Adicionalmente, 106 ignorados preexistentes dentro del alcance, sin borrarlos: **1069 archivos inventariados**. Se buscaron consumidores por ruta/nombre de 321 candidatos, no se afirma revisión semántica exhaustiva de todo el código.
- Diez grupos con igualdad binaria; **11 copias retiradas del seguimiento**, diez copias canónicas preservadas. **297015 bytes (290,05 KiB)** menos en el árbol versionado. El historial y el tamaño físico local no se reducen.
- 1058 archivos conservados. No se eliminó ningún no versionado preexistente. No se declararon seguros otros candidatos por nombre/fecha solamente.
- Los 114 recursos Postman, siete documentos NOT-141, evidencias, índice de casos, helper y variables oficiales están protegidos. El JSON de resultados anterior permanece idéntico.
- La limpieza del 11/11 retiró 38 archivos / 5.537.907 bytes. Esta añade 11 / 297.015 bytes distintos; no revierte aquella decisión ni reañade sus rutas. Acumulado documental: 49 / 5.834.922 bytes, sin descontar crecimiento posterior del repositorio.

## Respaldo verificado antes de retirar

C:\notiurba\respaldos-qa\limpieza-adicional-20261114T152220Z

Contiene 963 archivos, 57413137 bytes, todos comprobados por SHA-256, con rutas originales en manifiesto.json; cambios-iniciales.patch, estado inicial, index-inicial y historial.bundle. El bundle nuevo incluye HEAD d8e8b2b y pasó git bundle verify. El antiguo C:/notiurba/notiurba-antes-limpieza-20261111.bundle también es válido, pero su HEAD es 479b0c8 y no contenía los cambios locales actuales; no se usó como único respaldo. Los ignorados preexistentes no se tocaron y no se presentan como incluidos en el respaldo nuevo.

## Inventario de retiradas

El respaldo de la tabla es relativo al directorio anterior. [Inventario completo](2026-11-14-limpieza-adicional-inventario.csv): ruta absoluta, tamaño, tipo, hash, estado inicial, consumidores de código/pruebas/documentos, copias, decisiones y recuperación. La relación con adjuntos de Linear no fue verificada archivo por archivo; ninguna evidencia única se retiró.

| Ruta | Bytes | Motivo | Copia canónica / dependencias | Decisión | Respaldo |
|---|---:|---|---|---|---|
| output/playwright/not65-desktop-08-retorno.png | 133291 | SHA-256 exacto; conservar copia física | output/playwright/not65-desktop-06-cambio.png | RETIRAR_SEGUIMIENTO | archivos/output/playwright/not65-desktop-08-retorno.png |
| output/playwright/not65-responsive-1366-mapa.png | 100457 | SHA-256 exacto; conservar copia física | output/playwright/not65-responsive-1366-inicio.png | RETIRAR_SEGUIMIENTO | archivos/output/playwright/not65-responsive-1366-mapa.png |
| output/playwright/not65-map03-manual-sin-gps.png | 43764 | SHA-256 exacto; conservar copia física | output/playwright/not65-map02-gps-real.png | RETIRAR_SEGUIMIENTO | archivos/output/playwright/not65-map03-manual-sin-gps.png |
| docs/validacion/evidencias-barrios-temporales-20261107/chrome-verificacion.js | 2128 | SHA-256 exacto; conservar copia física | output/playwright/barrios-sin-temporales.js | RETIRAR_SEGUIMIENTO | archivos/docs/validacion/evidencias-barrios-temporales-20261107/chrome-verificacion.js |
| docs/validacion/evidencias-barrios-temporales-20261107/generar-sql-qa.cjs | 3035 | SHA-256 exacto; conservar copia física | output/validar-sql-temporales.cjs | RETIRAR_SEGUIMIENTO | archivos/docs/validacion/evidencias-barrios-temporales-20261107/generar-sql-qa.cjs |
| output/temporales-qa.sql | 4381 | SHA-256 exacto; conservar copia física | docs/validacion/evidencias-barrios-temporales-20261107/sql-qa-ejecutado.sql | RETIRAR_SEGUIMIENTO | archivos/output/temporales-qa.sql |
| docs/validacion/evidencias-catalogo-barrios-20261107/chrome-verificacion.js | 2999 | SHA-256 exacto; conservar copia física | output/playwright/catalogo-barrios-qa.js | RETIRAR_SEGUIMIENTO | archivos/docs/validacion/evidencias-catalogo-barrios-20261107/chrome-verificacion.js |
| docs/validacion/evidencias-catalogo-barrios-20261107/vite-qa.config.referencia.mjs | 499 | SHA-256 exacto; conservar copia física | output/vite-barrios-qa.mjs | RETIRAR_SEGUIMIENTO | archivos/docs/validacion/evidencias-catalogo-barrios-20261107/vite-qa.config.referencia.mjs |
| output/playwright/not65-preparar.js | 2499 | SHA-256 exacto; conservar copia física | output/playwright/not65-map03.js | RETIRAR_SEGUIMIENTO | archivos/output/playwright/not65-preparar.js |
| docs/validacion/logs/mvn-test.log | 1981 | SHA-256 exacto; conservar copia física | docs/validacion/logs/continuacion/mvn-clean-test.log | RETIRAR_SEGUIMIENTO | archivos/docs/validacion/logs/mvn-test.log |
| docs/validacion/logs/mvn-package.log | 1981 | SHA-256 exacto; conservar copia física | docs/validacion/logs/continuacion/mvn-clean-test.log | RETIRAR_SEGUIMIENTO | archivos/docs/validacion/logs/mvn-package.log |

## Referencias e identificadores de casos

Los JSON de MAP-08, RESP-1366 y MAP-03 apuntan ahora a las imágenes canónicas. Conservan nombre original, IDs, fechas, medidas, observaciones y resultados; se declara que compartir bytes no demuestra dos instantes o coordenadas independientes. Los scripts de captura siguen escribiendo sus salidas originales: son productores, no lectores que queden rotos; las salidas retiradas se ignoran por ruta exacta. Una nueva evidencia distinta requiere revisión antes de incorporarla. No se modifica la ejecución de React o Spring.

Las dos copias de scripts de barrios temporales y las de catálogo/configuración no tenían consumidores activos encontrados, aparte del inventario histórico; se conservan los originales ejecutables y sus ubicaciones, evitando romper rutas relativas. temporales-qa.sql sale del índice: queda su copia ejecutada canónica y el generador original. Dos logs idénticos del mismo bloqueo Maven se remiten a continuacion/mvn-clean-test.log. Los inventarios históricos no se reescriben: sus menciones quedan como procedencia y esta tabla da la recuperación.

## Conservados y revisión futura

- Todos los informes históricos con decisiones, fallos o resultados propios de NOT-25/31/59/62/65/68/71/88 y las evidencias finales de correo. No se encontró prueba suficiente para declarar obsoletas las evidencias únicas. NOT-139/140 no se dan por aprobadas por esta limpieza.
- Todos los SQL/migraciones pendientes y activos, en especial V14 y los recursos de docs/gis-not31. Las copias de SQL que verifican procedencia/contrato en pruebas se conservan incluso si contienen DML equivalente; igualdad semántica no significa archivo descartable.
- Los scripts de output con rutas relativas y los logs únicos conservados por la auditoría anterior. Ser standalone o no tener import no prueba falta de utilidad.
- P-28: PNG **39.918.499 bytes**, PDF **5.350.857 bytes**, total **45.269.356 bytes**. No se encontraron imports de ambos binarios por código React/Spring; sí referencias de procedencia y trabajo GIS en mapas-notiurba, pendientes y decisión 0005. **Se conservan intactos por instrucción expresa.** Recomendación separada: mantener original y hashes en almacenamiento documental externo con acceso estable y una previsualización reducida; actualizar enlaces/procedencia y verificar QGIS antes de cualquier traslado. No se usa LFS ni se reescribe historial. Moverlos en un commit futuro no reduce el historial ya publicado.
- Revisión manual futura: necesidad académica de artefactos únicos y política de P-28. No hay más duplicados exactos seguros pendientes entre los diez grupos de esta auditoría; no se autoriza borrar lo incierto por extensión.

## Validaciones ejecutadas

- git diff --check y git diff --cached --check: sin errores de espacio; avisos informativos de normalización CRLF, sin cambiar configuración Git.
- 582 archivos protegidos comparados por hash: frontend, backend, db, GIS, 114 recursos y resultado HTTP previo intactos.
- 11 copias físicas, canónicas y respaldos coinciden por SHA-256; las 11 rutas salen del índice y quedan ignoradas.
- 0 enlaces Markdown críticos de los documentos iniciales apuntan a una ruta retirada. Capturas principales referenciadas por JSON siguen versionadas; nombres originales quedan como historial.
- Generador SQL ejecutado en VM con escritura capturada en memoria: idéntico al SQL canónico normalizando CRLF/LF. Ningún SQL se ejecutó contra base alguna.
- 5/5 pruebas de preparación del catálogo aprobadas; fuentes, hashes, 135 asociaciones históricas, V17 candidata y selector real con fixture. No acreditan catálogo vigente de producción.
- Sintaxis de los scripts QA canónicos comprobada; 114 recursos Postman compilan, 92 guardas bloquean con valores iniciales. No equivalen a HTTP aprobado. No se repitió suite completa.
- Sin migraciones, controladores, componentes React ni pruebas activas eliminados. No se añadieron credenciales de QA o servicios; los nuevos informes guardan metadatos, no cabeceras o cuerpos autenticados.

## Estado de Git y recuperación

11 eliminaciones preparadas en índice por git rm --cached, sin commit. .gitignore y dos JSON de referencias cambiados; se conservaron las modificaciones previas de Postman y sus archivos no seguidos. Se añaden este informe/inventario y avances documentales de NOT-141.

Las copias físicas siguen en su ruta original. Para recuperación, verificar SHA-256 y retirar únicamente la regla puntual de .gitignore antes de volver a añadir la ruta si se decide. También se puede inspeccionar git show d8e8b2bee07a3e4e59846f9fbde803a4b044fa95:RUTA, o copiar archivos/RUTA del respaldo sin sobrescribir cambios posteriores. No usar reset --hard, clean ni restaurar todo el índice de forma indiscriminada. El bundle nuevo permite recuperar el historial en otra ubicación si hiciera falta, sin alterar esta rama.

El informe de NOT-141 distingue resultados locales, metadatos del despliegue y pruebas HTTP/celular bloqueadas. La limpieza no cierra la historia.


## Avance de NOT-141 posterior a la limpieza

Chrome se recuperó para navegación y validaciones públicas. Los dos catálogos respondieron HTTP 200 desde el frontend, y health mostró estado/BD ok sin capturar código HTTP. El avance se detalla en NOT-141/INFORME-FINAL-NOT-141.md; no cierra autenticación, correo ni recorrido móvil. Las 11 retiradas y los archivos funcionales no cambiaron durante estas pruebas.
