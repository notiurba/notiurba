# Hallazgos y repruebas — NOT-141

Fecha: 14/11/2026. No se crearon incidencias nuevas ni comentarios; se consultaron historias existentes para evitar duplicados. Responsables propuestos.

| ID | Entorno y evidencia | Prioridad | Responsable | Estado |
|---|---|---|---|---|
| H-141-01 | Código local d8e8b2b y archivo remoto; VM local con fallo API | Alta | Katherine; Luz revalida | Pendiente; relacionado con NOT-135 y NOT-140 |
| B-141-01 | Windows, curl exit 6 / Node ENOTFOUND | Alta para QA | Responsable del entorno de ejecución | Bloqueo externo; no es caída demostrada de la app |
| B-141-02 | Chrome conectado, navegación y CDP agotan tiempo | Alta para QA | Responsable del entorno; Luz | Bloqueo de automatización |
| B-141-03 | ADB 37 instalado; servidor tcp:5037 inaccesible | Alta para QA | Responsable del entorno Windows; Luz | Bloqueo móvil |
| B-141-04 | Destino efectivo Render y buzón QA sin confirmar | Alta | Andrés / Luz | Bloquea correos y reportes con envío |
| B-141-05 | Rama local V16 frente a Neon V22 y develop más avanzado | Alta para trazabilidad | Andrés / Luz | Contratos revisados por lectura remota; build pendiente |
| H-141-02 | Extensión previa exigía observación administrativa | Media | Luz QA | Corregido en colección; ejecución contra Render pendiente |

## H-141-01 — el mapa puede devolver datos ficticios tras un error

Dispositivo/navegador de reproducción ejecutada: Node 24.18.0, Windows, VM local aislada; sin UI.
Funcionalidad: obtener reportes del mapa administrativo.
Pasos ejecutados: leer el módulo sin modificarlo; retirar imports solo dentro de la VM; inyectar una función de API que lanza error; proporcionar una lista sentinela sintética; invocar obtenerReportesCiudadanos.
Esperado: propagar o representar un error, sin sustituir por datos simulados cuando se usan servicios reales.
Obtenido: se devuelve exactamente la lista reportesPrueba. Evidencia: [reproducción](evidencias/reproduccion-fallback-local.json) y [fuente remota](evidencias/revision-remota.json).
Alcance: defecto reproducido en la función local y presente en el archivo remoto inspeccionado. La versión de producción no está identificada; su impacto visible no se ha demostrado.
Propuesta: permitir simulación únicamente en modo de desarrollo expresamente habilitado; mostrar error y Reintentar en modo real. Tras autorización de cambio, probar respuesta 200, lista vacía y errores 401/403/500/red en UI; contrastar con listado y filtros reales. **No se cambió código funcional.**

## Bloqueos reproducibles

B-141-01: ejecutar GET /api/health y GET de catálogo desde la terminal. Esperado: respuesta del servicio; obtenido: error DNS, sin HTTP. [Disponibilidad](evidencias/disponibilidad-http.txt) y [12 intentos](evidencias/resultados-http-seguros.json). Causa identificable: resolución de nombres fallida en este proceso; causa raíz de la restricción no determinada. La consulta web alternativa tampoco pudo abrir las URLs.

B-141-02: abrir https://notiurba.netlify.app en una pestaña Chrome de QA; navegación agotó tiempo y la lectura posterior devolvió timeout CDP Runtime.evaluate. No se obtuvo pantalla verificable. Registro textual de herramienta en [herramientas](evidencias/herramientas.md). No se declara resultado visual ni captura existente.

B-141-03: ejecutar adb version y get-state con el serial indicado. Esperado: device; obtenido: servidor ADB inaccesible. [ADB](evidencias/adb.txt). No se tiene evidencia para culpar al cable o permiso USB.

B-141-04: sin variable de destino efectivo y mensajes QA identificados, no solicitar envío. La ausencia de coincidencias en el Gmail conectado no prueba un fallo del sistema. [Correos](VALIDACION-CORREOS.md).

B-141-05: comparar ramas en GitHub y metadatos de migración en Neon. Esperado: identificar la revisión ejecutada; obtenido: código local más antiguo y build desconocido. No intentar aplicar migraciones antiguas, fusionar o cambiar ramas automáticamente.

## Repruebas

| Caso | Acción posterior disponible | Resultado |
|---|---|---|
| H-141-01 | Inspección remota + reproducción aislada | Defecto sigue presente; producción pendiente |
| H-141-02 | Cambiar negativo a estado ausente | Script compila; prueba de Render bloqueada |
| HTTP | Reintento acotado con curl y 12 GET Node | Bloqueo DNS persiste |
| Chrome | Reconexión y lectura tras navegación | Bloqueo persiste |
| ADB | Consulta dirigida tras devices -l | Bloqueo persiste |

No hay hallazgos funcionales marcados «conforme tras volver a probar» en producción.


## Reanudación del 14/11/2026 — limpieza adicional y accesos

Esta sección registra avances posteriores a la primera revisión. Los resultados anteriores se conservan; repetir una solicitud no crea otro caso de aceptación. Estados: **APROBADO** solo con ejecución conforme y alcance explícito; **FALLIDO** con defecto reproducible; **BLOQUEADO** por acceso/precondición; **PENDIENTE** aún sin ejecución.

| ID | Resultado nuevo | Responsable propuesto | Estado |
|---|---|---|---|
| B-141-01 | DNS HostNotFound / 12 GET Node y 12 CLI ENOTFOUND | Entorno / Luz | BLOQUEADO; sin fallo funcional HTTP |
| B-141-03 | Socket ADB 5037 AccessDenied confirmado | Entorno Windows / Luz | BLOQUEADO; no cambiar protecciones |
| B-141-04 | Gmail de 7 días sin QA; destino Render no acreditado | Andrés / Luz | BLOQUEADO |
| B-141-05 | Netlify production identificado 4868ee4; Render sin SHA | Andrés / Luz | PARCIAL: metadatos Netlify APROBADOS, Render BLOQUEADO |
| B-141-06 | Postman CLI rechaza definición nativa actual y baseline original; JSON v2.1 aceptado | Luz QA / responsable de Postman | BLOQUEADO por compatibilidad/validación pendiente; no atribuir al código nuevo |
| H-141-01 | Función fallback no modificada durante limpieza | Katherine / Luz | PENDIENTE de corrección autorizada y prueba desplegada |

Los 34 fallos de aserción en CLI se deben a respuesta ausente por DNS; conservar como fallos del runner, no como 34 defectos de API. No se crearon incidencias duplicadas ni se publicaron comentarios en Linear. La asociación de responsables sigue siendo propuesta documental.


## Avance posterior en Chrome — 14/11/2026

B-141-02 queda **parcialmente recuperado**: navegación pública, registro y recuperación funcionan mediante control de pestañas. La automatización del botón Google en iframe continúa BLOQUEADA; no se atribuye todavía a CSS/producto, porque la captura visual no mostró obstrucción evidente y el diagnóstico procedía del locator. B-141-01 sigue afectando terminal/CLI, pero no impidió los GET reales de catálogos desde Chrome (200). B-141-04 ahora tiene bloqueo concreto adicional: Render Dashboard solicita login personal. Se mantiene H-141-01 sin cambios funcionales y sin validar mapa autenticado. [Evidencia real de Chrome](evidencias/reanudacion-20261114/chrome-ui.json).
