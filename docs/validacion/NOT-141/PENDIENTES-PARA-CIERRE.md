# Pendientes para cierre — NOT-141

**Mantener En progreso. No se cumplen los criterios de cierre.**

| Orden | Pendiente concreto | Responsable propuesto | Evidencia necesaria |
|---|---|---|---|
| 1 | Recuperar DNS/red de este proceso y control estable de Chrome | Entorno / Luz | HTTP real y página visible |
| 2 | Identificar revisiones Netlify y Render | Andrés | SHA/build de cada despliegue |
| 3 | Acreditar destino exclusivamente QA en Render y buzón del equipo | Andrés / Luz | Configuración efectiva revisada sin exponer secretos |
| 4 | Recuperar servidor ADB y verificar serial device | Entorno / Luz | ADB y captura real del Xiaomi |
| 5 | Preparar cuentas ciudadanas QA y administrador, JWT firmado vencido | Luz / Andrés | Identificadores QA, secretos fuera del repo |
| 6 | Registro, correo, ingreso, Google y retorno/sesión | Luz | Resultados por PC y Xiaomi; intervención personal en Google |
| 7 | Reporte con barrio/dirección/GPS/foto, código, detalle y Mis reportes | Luz | Capturas y consulta persistida del mismo código QA |
| 8 | Fotos 1–3 y rechazo 4 tanto UI como API | Luz | Mensaje claro y 409 FOTOS_MAXIMO con precondición verificada |
| 9 | Recuperación/restablecimiento, perfil, actualización e historial | Luz | Antes/después; estado oficial preservado |
| 10 | Autorizar específicamente borrados de cuenta/datos QA desechables | Luz | Cuenta identificada y ausencia de datos reales; resultados después |
| 11 | Revisar correos Alcaldía y empresa en Gmail PC/Android y MIME | Luz | Checklist completo, sin datos personales |
| 12 | Verificar importación nativa Postman y ejecutar pm.test/Newman | Luz | Resumen anonimizando cuerpos, tokens y cabeceras |
| 13 | Probar PUT válido de vigencia en entorno aislado | Andrés / Luz | Configuración y restauración de ese entorno; no Render productivo |
| 14 | Autorizar/corregir fallback y repetir mapa/panel | Katherine / Luz | API real, error visible, sin simulación; filtros y comunas |
| 15 | Completar matriz y asignar incidencias existentes | Equipo | Fecha/dispositivo/resultado/evidencia por criterio |

No faltan permisos para escribir los documentos o continuar con lecturas ya autorizadas: estas actividades quedaron realizadas. Los bloqueos descritos son de acceso efectivo, trazabilidad y precondiciones de QA. No se solicitan contraseñas ni JWT por chat.


## Reanudación del 14/11/2026 — limpieza adicional y accesos

Esta sección registra avances posteriores a la primera revisión. Los resultados anteriores se conservan; repetir una solicitud no crea otro caso de aceptación. Estados: **APROBADO** solo con ejecución conforme y alcance explícito; **FALLIDO** con defecto reproducible; **BLOQUEADO** por acceso/precondición; **PENDIENTE** aún sin ejecución.

Avance cerrado con alcance limitado: build publicado de Netlify identificado, commit 4868ee4. Sigue faltando el build de Render y la disponibilidad HTTP. Añadir al pendiente Postman: revisar por qué CLI 1.85.0 rechaza la definición nativa incluso con baseline HEAD; el export JSON v2.1 de lectura fue aceptado, pero sus 12 solicitudes quedaron sin HTTP por DNS. No se necesita instalar Newman para reconocer la ejecución real ya realizada por CLI; la suite completa sigue pendiente.

ADB requiere recuperar acceso autorizado de este proceso al socket 5037 (AccessDenied), sin desactivar protecciones. Gmail necesita confirmar buzón/destino de QA. Los criterios de autenticación, reporte/fotos, perfil/recuperación/actualización, borrados QA autorizados, correos y recorrido Xiaomi permanecen sin completar. Limpieza e integridad local ya verificadas; no condicionan por sí solas el cierre funcional.


## Avance posterior en Chrome — 14/11/2026

Ya no falta recuperar toda la navegación de Chrome: se aprobaron formularios negativos, catálogos reales, conservación del borrador y contenido de salud. Faltan autenticación Google/tradicional con QA, cuenta/verificación, reportes/fotos/consulta, perfil/recuperación completa/actualización, correos, borrados QA y Xiaomi.

Intervención personal concreta: iniciar sesión en Render en la página dejada abierta para revisar el destino de pruebas; activar Continuar con Google en el login abierto y escoger una cuenta QA. No compartir contraseñas, códigos ni JWT por chat. La terminal/Postman necesita recuperar DNS; ADB, acceso autorizado al socket 5037. [Evidencia real de Chrome](evidencias/reanudacion-20261114/chrome-ui.json).
