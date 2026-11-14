# Matriz de criterios — NOT-141

Fecha de revisión: **14/11/2026**, Colombia. Responsable: Luz Amelia Ibarguen Ibarra.
Leyenda: **Conforme limitado** = ejecución con alcance explícito; **Bloqueado** = no acredita la aceptación; **Parcial** = entregable preparado sin validación final.
Ninguna ejecución local se convierte en aprobación de producción.

| ID | Criterio | Resultado | Fecha y dispositivo/cliente | Evidencia / impedimento |
|---|---|---|---|---|
| E01 | Frontend Netlify disponible | Bloqueado | 14/11/2026, Windows/curl y Chrome | DNS y timeout; sin pantalla ni HTTP |
| E02 | Render /api/health y BD del backend | Bloqueado | 14/11/2026, Windows/curl | Sin resolución DNS; no se pudo probar SELECT a través del backend |
| E03 | SELECT a Neon production | Conforme limitado | 14/11/2026, conector SQL | neon-metadatos.json; PG18.7, V22, 133 barrios |
| E04 | Comunicación Netlify–Render y CORS | Bloqueado | 14/11/2026, PC | No se obtuvo llamada real desde página |
| E05 | Versión desplegada y ausencia de mocks | Bloqueado | 14/11/2026, PC | Build no identificado; código DEV limita simulador pero fallback mapa sigue presente |
| E06 | Configuración pública Google y selector | Bloqueado | 14/11/2026, Chrome | Página no verificable; ID efectivo y dominios OAuth pendientes |
| P01 | Registro QA y campos obligatorios | Bloqueado | 14/11/2026, PC; Xiaomi no accesible | Sin cuenta QA creada |
| P02 | Recepción y confirmación de correo | Bloqueado | 14/11/2026, Gmail PC/Android | Buzón y destino QA no confirmados |
| P03 | Login tradicional | Bloqueado | 14/11/2026, PC/Android | Sin sesión QA verificada |
| P04 | Login Google | Bloqueado | 14/11/2026, PC/Android | No se alcanzó selector; intervención personal pendiente |
| P05 | Navegación tras login y persistencia sesión | Bloqueado | 14/11/2026, PC/Android | Recorrido sin ejecutar |
| P06 | Categoría, barrio, dirección y GPS/mapa | Bloqueado | 14/11/2026, Xiaomi requerido | ADB no conecta; ningún formulario completado |
| P07 | Reporte con foto y revisión/confirmación | Bloqueado | 14/11/2026, PC/Android | Sin reporte QA ni destino de envío acreditado |
| P08 | Código único, detalle, Mis reportes y persistencia | Bloqueado | 14/11/2026, PC/Android/API | No existe código QA nuevo de esta sesión |
| P09 | Ubicación, estado y enlace de foto persistidos | Bloqueado | 14/11/2026, API/SQL | Solo se consultaron metadatos, no registros de ciudadanos |
| P10 | Una, dos y tres fotos | Bloqueado | 14/11/2026, UI/API | Recursos preparados; no enviados |
| P11 | Rechazo cuarta foto UI y 409 FOTOS_MAXIMO | Bloqueado | 14/11/2026, UI/API | Límite inspeccionado en fuente; sin reporte QA con tres fotos |
| P12 | Recuperación y restablecimiento por enlace | Bloqueado | 14/11/2026, PC/Android | Sin envío QA y enlace; cambio de clave requiere intervención correspondiente |
| P13 | Consultar/editar perfil | Bloqueado | 14/11/2026, PC/Android | Sin usuario QA autenticado |
| P14 | Actualizar reporte e historial sin cambiar estado oficial | Bloqueado | 14/11/2026, PC/Android | Contrato revisado y test antes/después preparado; sin ejecución |
| P15 | Borrar datos preservando cuenta QA | Bloqueado | 14/11/2026, PC/Android | Falta cuenta descartable identificada y autorización específica |
| P16 | Eliminar cuenta QA | Bloqueado | 14/11/2026, PC/Android | Misma precondición; ningún borrado ejecutado |
| C01 | Correo Alcaldía completo y sin matrícula | Bloqueado | 14/11/2026, Gmail PC/Android | Ningún mensaje ciudadano QA identificado |
| C02 | Correo empresa completo y matrícula según contrato | Bloqueado | 14/11/2026, Gmail PC/Android | Destino y buzón QA pendientes |
| C03 | HTML/text/plain, fotos y miniaturas | Bloqueado | 14/11/2026, Gmail PC/Android | No se inspeccionó MIME de correo real |
| A01 | Listado y filtros admin desde API real | Bloqueado | 14/11/2026, Chrome/API | Sin sesión/admin y sin HTTP |
| A02 | Comunas, reportes y mapa según filtros | Bloqueado | 14/11/2026, Chrome/API | Sin evidencia visible |
| A03 | Fallo API sin reportes simulados | Hallazgo alto; producción pendiente | 14/11/2026, VM local | reproducción-fallback-local.json; requiere corrección autorizada |
| A04 | Transiciones administrativas y vigencia | Bloqueado | 14/11/2026, API | Casos preparados; PUT global válido solo aislado |
| T01 | Postman incluye endpoints nuevos y escenarios | Parcial | 14/11/2026, archivos nativos | 114 recursos y exportación v2.1; importación UI no verificada |
| T02 | pm.test y Newman contra Render | Bloqueado | 14/11/2026, Windows | Newman ausente; 12 intentos HTTP DNS, no ejecución pm.test |
| T03 | Sintaxis/guardas del material Postman | Conforme limitado | 14/11/2026, Node 24.18.0 | 114 recursos; 92 bloqueos por defecto |
| T04 | Cada fallo tiene pasos, responsable y evidencia | Conforme documental | 14/11/2026 | HALLAZGOS-Y-REPRUEBAS.md; responsables propuestos |
| T05 | Repruebas tras correcciones funcionales | Pendiente | 14/11/2026 | No se autorizó/modificó código funcional; no hay corrección desplegada acreditada |

Los archivos de evidencia están en [evidencias/](evidencias/). El recorrido físico, las dos autenticaciones y ambos correos continúan impidiendo el cierre.



## Reanudación del 14/11/2026 — limpieza adicional y accesos

Esta sección registra avances posteriores a la primera revisión. Los resultados anteriores se conservan; repetir una solicitud no crea otro caso de aceptación. Estados: **APROBADO** solo con ejecución conforme y alcance explícito; **FALLIDO** con defecto reproducible; **BLOQUEADO** por acceso/precondición; **PENDIENTE** aún sin ejecución.

| ID nuevo | Comprobación real | Estado | Fecha / cliente | Evidencia |
|---|---|---|---|---|
| R01 | Identificar build publicado Netlify 4868ee4 | APROBADO, solo metadatos | 14/11/2026, API Netlify | servicios.json |
| R02 | Identificar build de Render | BLOQUEADO | 14/11/2026, Windows | DNS sin HTTP |
| R03 | Repetir 12 GET Node contra Render | BLOQUEADO | 14/11/2026, Node 24.18.0 | reanudacion-20261114/resultados-http-seguros.json |
| R04 | Ejecutar 12 GET y pm.test con Postman CLI | BLOQUEADO por DNS | 14/11/2026, CLI 1.85.0 | 12 scripts / 34 aserciones fallidas por no respuesta |
| R05 | Cargar definición nativa actual y baseline HEAD | BLOQUEADO, CLI las rechaza | 14/11/2026, CLI 1.85.0 | postman-cli.txt; no prueba defecto nuevo |
| R06 | Servidor ADB para Xiaomi | BLOQUEADO | 14/11/2026, Windows | socket AccessDenied |
| R07 | Correos QA en Gmail | BLOQUEADO | 14/11/2026, búsqueda 7 días | sin mensajes identificados y buzón QA sin confirmar |
| R08 | Integridad tras limpieza | APROBADO, solo local | 14/11/2026, Windows/Node | 5/5 pruebas y 582 hashes intactos |

Los criterios ciudadanos y de correo anteriores siguen BLOQUEADOS/PENDIENTES. No hay pruebas HTTP o móviles nuevas aprobadas. Las aserciones fallidas del runner se preservan y se atribuyen al bloqueo de transporte; no son fallos funcionales demostrados de la API.


## Avance posterior en Chrome — 14/11/2026

| ID | Comprobación nueva | Estado | Cliente / fecha | Límite |
|---|---|---|---|---|
| WEB-01 | Inicio desplegado visible | APROBADO | Chrome Windows, 14/11/2026 | No recorrido autenticado |
| WEB-02 | Login vacío muestra dos obligatorios | APROBADO | Chrome Windows, 14/11/2026 | Negativo UI |
| WEB-03 | Registro paso 1 vacío bloquea avance | APROBADO | Chrome Windows, 14/11/2026 | No alta |
| WEB-04 | Registro paso 2 vacío bloquea alta | APROBADO | Chrome Windows, 14/11/2026 | No alta ni aceptación de aviso |
| WEB-05 | 133 barrios y maxlength dirección 100 | APROBADO | Chrome Windows, 14/11/2026 | Atributo/opciones; no prueba de 101 caracteres |
| WEB-06 | Barrio/dirección conservados al retroceder | APROBADO | Chrome Windows, 14/11/2026 | Borrador sintético |
| WEB-07 | Recuperación vacía bloquea enlace | APROBADO | Chrome Windows, 14/11/2026 | No envío/restablecimiento |
| API-WEB-01 | Tipos de documento API real | APROBADO, HTTP 200 | Chrome Windows, 14/11/2026 | Leído por Netlify |
| API-WEB-02 | Barrios API real | APROBADO, HTTP 200 | Chrome Windows, 14/11/2026 | Leído por Netlify, 133 opciones |
| API-WEB-03 | Health devuelve estado/BD ok | APROBADO, contenido | Chrome Windows, 14/11/2026 | HTTP no capturado |
| AUT-01 | Google completo | BLOQUEADO | Chrome Windows, 14/11/2026 | Activación iframe no completada; requiere intervención personal |
| CFG-01 | Leer destino efectivo de correo en Render | BLOQUEADO | Chrome Windows, 14/11/2026 | Dashboard pide login |

Las filas iniciales E01/E04 quedan parcialmente superadas por estas observaciones. No se aprueba todo E02/E05/E06 ni ningún recorrido completo ciudadano, correo o Xiaomi. [Evidencia real de Chrome](evidencias/reanudacion-20261114/chrome-ui.json).
