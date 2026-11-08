# Respaldo y limpieza de QA — 2026-11-08

Respaldo externo: C:\notiurba\respaldos-qa\limpieza-20261108-124838-1606
Inventario completo con rutas originales, rutas de copia y SHA256: inventario.json.
Archivos retirados, motivo y copia canónica: eliminados.json. Todos se verificaron por hash y mediante copia de recuperación antes de retirar el original.
Para recuperar: copiar el archivo indicado en Backup a su Path original, sin sobrescribir cambios posteriores.

Se conservaron documentación oficial, código, pruebas, fuentes municipales, CSV135, V17 candidata, auditoría, catálogo150 y evidencias únicas, incluidas comparaciones y originales/reducidas de NOT-88.
Linear: PDFs en las descripciones de NOT-25/59/62/68/88; presencia comprobada, contenido y equivalencia con originales no verificados. NOT-65 sin adjunto observado; NOT-71 pendiente.
Google Docs: informes de NOT-25/59/62/68/88; NOT-68 tiene dos imágenes incrustadas. NOT-88 no tiene sus comparaciones incrustadas. No se trataron referencias locales como respaldos externos.
Se conservaron caches Vite deps actuales por no poder comprobar su actividad, tres logs en uso y archivos citados por informes/scripts o con utilidad dudosa.

## Archivos retirados

| Ruta original | Motivo | Copia idéntica conservada |
|---|---|---|
| .playwright-cli/page-2026-11-07T20-26-06-010Z.yml | D: duplicado SHA256 idéntico sin referencia activa | .playwright-cli/page-2026-11-07T18-03-24-376Z.yml |
| .playwright-cli/page-2026-11-08T01-14-47-019Z.yml | D: duplicado SHA256 idéntico sin referencia activa | .playwright-cli/page-2026-11-07T18-03-24-376Z.yml |
| .playwright-cli/page-2026-11-08T10-43-42-330Z.yml | D: duplicado SHA256 idéntico sin referencia activa | .playwright-cli/page-2026-11-07T23-50-51-349Z.yml |
| output/vite-not25-qa.mjs.timestamp-1794104694400-9526e7933ede9.mjs | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/vite-not59-qa.mjs.timestamp-1794134500926-2bc227ab5ef6f.mjs | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/leaflet__heat.js | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/leaflet__heat.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/leaflet.js | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/leaflet.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react_jsx-dev-runtime.js | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react_jsx-dev-runtime.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react_jsx-runtime.js | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react_jsx-runtime.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react-dom_client.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react-dom.js | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react-dom.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react-leaflet.js | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react-leaflet.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react-router-dom.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/react.js.map | E: residuo regenerable de Vite o aviso repetido de consola |  |
| output/.vite-not25-cache/deps_temp_3c9d696a/rolldown-runtime-BPOCksWG.js | E: residuo regenerable de Vite o aviso repetido de consola |  |
| .playwright-cli/console-2026-11-08T01-14-45-271Z.log | E: residuo regenerable de Vite o aviso repetido de consola |  |
| .playwright-cli/foto-1.jpg | D: duplicado SHA256; sin referencia resoluble al archivo | docs/validacion/NOT-25-final-20261107/almacen-local/5fa5dd70dc87dc4cb4d87b60df195ab0296875ab84b1686295d1f33465f20ad3.jpg |
| docs/validacion/logs/continuacion/mvn-package.log | D: duplicado SHA256; sin referencia resoluble al archivo | docs/validacion/logs/continuacion/mvn-clean-test.log |
| .playwright-cli/foto-3.jpg | D: duplicado SHA256; sin referencia resoluble al archivo | docs/validacion/NOT-88-commons-20261108T011359Z/reducidas/foto-3.jpg |
| .playwright-cli/foto-2.jpg | D: duplicado SHA256; sin referencia resoluble al archivo | docs/validacion/NOT-88-commons-20261108T011359Z/reducidas/foto-2.jpg |

Nota de saneamiento local: los archivos de cache y logs locales ignorados se conservan como respaldo y no forman parte del PR. El inventario anterior se mantiene como registro historico.
