# Limpieza de la rama fix/registro-pr20 — 11 de noviembre de 2026

Repositorio: notiurba/notiurba. PR #27: develop <- fix/registro-pr20.
HEAD inicial: 479b0c853f9e1de98b0db7286eefa9e12b9c37f0. Arbol inicial limpio, sin cambios locales que guardar en stash.
Referencia origin/develop: e5d73c43da8331f3e6f524eda0724652471c4f83; 3 commits delante y 0 detras.
GitHub confirma head remoto 46bf1b26353f29d838203d2b049fa4fc5d878382 y base e5d73c4. Un commit local previo (Google) pendiente de publicar, conservado integro.
Fetch SSH y consulta HTTPS bloqueados por DNS del entorno. No se supone que fetch haya actualizado referencias.
Respaldo completo verificado: C:/notiurba/notiurba-antes-limpieza-20261111.bundle.

## Criterio y alcance

Se revisaron 205 archivos añadidos por esta rama en output y evidencias, con referencias por ruta/nombre en todos los textos versionados y hashes SHA256 de imagenes/PDF. El [inventario exacto](2026-11-11-limpieza-inventario.csv) contiene cada decision, motivo y consumidores potenciales (la coincidencia por nombre puede ser conservadora).

Se retiran del indice **38 archivos**, 5537907 bytes. Se mantienen las copias locales ignoradas: no se borran archivos fisicos ni se reescribe historial. Solo se seleccionaron archivos añadidos desde develop; los logs heredados de develop se conservan.

Carpetas: output (PDF duplicado y paginas renderizadas), output/playwright (7 capturas duplicadas e intentos de permisos superados), evidencias NOT-71 de arranque/foto-internet/produccion-auditoria y evidencias-google-escritorio.

Se conservan todos los fuentes React/Spring, pruebas, migraciones, aviso de privacidad, NOT-25/59/62/65/71/88, capturas unicas y finales, informes tecnicos, SQL/CSV/PDF del catalogo, candidata V17 y decisiones pendientes. No se retiran JSON finales o usados como fixtures por pruebas. Se conserva el extracto de fallo original y los resumenes de Google.

Los scripts de output se mantienen en su ruta: dependen de rutas relativas y documentos vigentes; moverlos requiere una refactorizacion adicional. Por eso output NO se ignora como carpeta. Los dos generadores PMGR ahora leen el PDF canonico de docs y no crean otra copia del mismo PDF. Las capturas duplicadas retiradas de output siguen siendo salidas regeneradas por los scripts, con copia canonica en docs.

Las copias minificadas remotas solo se utilizaban para el diagnostico historico. Sus hashes y fragmentos comparativos siguen en bundle-manifest.json y chunks-comparacion.json. Las menciones historicas en documentos/JSON se conservan como procedencia, no como imports del proyecto.

## Ignorados

Logs, temporales, node_modules, caches Vite, dist, target, coverage y reportes automaticos de Playwright. Las excepciones puntuales retiradas se enumeran por ruta exacta. No se ignoran carpetas completas de scripts ni de documentacion.

## Recuperacion y verificacion

Cualquier archivo retirado puede consultarse sin restaurar el arbol con: git show 479b0c853f9e1de98b0db7286eefa9e12b9c37f0:RUTA_DEL_ARCHIVO. Las copias locales siguen presentes y el bundle contiene el historial completo.

- 5/5 pruebas de preparacion del catalogo: hashes PDF/CSV, 135 pares, V17 fuera de Flyway activo, decisiones pendientes y selector.
- 5/5 pruebas frontend catalogo-real desde frontend: catalogos publicos, errores/vacio sin fixtures y datos de ejemplo internos.
- El primer comando conjunto ejecuto estas ultimas desde la raiz y fallo al resolver /src; se corrigio el directorio de ejecucion, sin cambiar pruebas ni codigo funcional.
- node --check aprobado en los dos generadores modificados; no se ejecutaron generadores que sobrescriben evidencias historicas ni dependencias PDF temporales.
- 0 enlaces Markdown a archivos retirados; referencias historicas en texto/JSON documentadas, no eliminadas para falsificar la evidencia.
- 38/38 rutas retiradas ignoradas y con copia local presente. Frontend, backend y db identicos al HEAD inicial.
- git diff --check y comprobacion del indice aprobados. Sin suites completas ni servicios productivos.

Publicacion: los accesos Git por SSH y HTTPS fallaron por resolucion DNS. El commit de limpieza queda preparado para push normal; el resultado del intento posterior se informa al cerrar la tarea. Ultimo PR consultado: abierto, draft, head remoto 46bf1b2, base develop/e5d73c4; mergeable=false comunicado por GitHub (no se infiere un conflicto local ni se intenta integrar develop).
