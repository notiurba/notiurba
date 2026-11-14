# Validación de correos — NOT-141

**Pendiente de entrega y revisión reales.** Fecha: 14/11/2026, Colombia. No se enviaron correos al CAIC, Alcaldía ni empresas.

Se ejecutaron dos búsquedas en Gmail con NotiUrba y, de forma más específica, asuntos de prueba/códigos NU-000 en los últimos 30 días. No se encontraron mensajes ciudadanos del sistema. No se guardaron remitentes, destinatarios personales, cuerpos ni tokens de verificación en las evidencias. Una coincidencia de Figma no se considera un correo de reporte.

La cuenta Gmail accesible no ha sido confirmada como buzón de QA del equipo. No se puede afirmar ausencia de correos en otro buzón, fallo de entrega ni recepción exitosa.

## Seguridad y contrato

El código revisado de EnvioReporteServicio reemplaza los destinatarios al usar un destino de prueba y agrega una marca de prueba; sin esa configuración puede usar el destino real. El destino persistido en una cola de Neon no acredita el destino efectivo después de esa sustitución. No se comprobaron variables ni logs de Render: **no se autoriza activar el envío desde la colección** hasta confirmar destino y buzón.

La plantilla local es anterior al contrato remoto: no usar sus deficiencias como prueba de un defecto actual en producción. Se revisó el archivo remoto de la rama de correo y el controlador actual de develop. El contrato incorpora vista previa y mensaje del reporte, que se añadieron a Postman; tampoco sustituyen el MIME recibido en Gmail.

## Lista de aceptación

| Criterio | Alcaldía PC | Alcaldía Xiaomi | Empresa PC | Empresa Xiaomi |
|---|---|---|---|---|
| Destinatario QA efectivo, sin copias oficiales | Pendiente | Pendiente | Pendiente | Pendiente |
| Asunto y código de seguimiento | Pendiente | Pendiente | Pendiente | Pendiente |
| Categoría y descripción | Pendiente | Pendiente | Pendiente | Pendiente |
| Fecha legible y zona Colombia | Pendiente | Pendiente | Pendiente | Pendiente |
| Dirección y coordenadas legibles | Pendiente | Pendiente | Pendiente | Pendiente |
| Secciones ordenadas | Pendiente | Pendiente | Pendiente | Pendiente |
| Sin matrícula en Alcaldía | Pendiente | Pendiente | No aplica | No aplica |
| Matrícula cuando corresponde a categoría/contrato | No aplica | No aplica | Pendiente | Pendiente |
| Una, dos y tres fotos; miniaturas y enlaces | Pendiente | Pendiente | Pendiente | Pendiente |
| Alternativas MIME HTML y text/plain | Pendiente | Pendiente | Pendiente | Pendiente |

Para reanudar: identificar buzón QA, confirmar destino efectivo en Render, seleccionar dos reportes QA compatibles con las rutas actuales, solicitar envío autorizado y revisar respuesta/cola/proveedor sin confundir aceptación con entrega. Abrir ambos mensajes en PC y Xiaomi; inspeccionar MIME y anonimizar evidencias. No solicitar envíos a canales externos que no admiten correo.


## Reanudación del 14/11/2026 — limpieza adicional y accesos

Esta sección registra avances posteriores a la primera revisión. Los resultados anteriores se conservan; repetir una solicitud no crea otro caso de aceptación. Estados: **APROBADO** solo con ejecución conforme y alcance explícito; **FALLIDO** con defecto reproducible; **BLOQUEADO** por acceso/precondición; **PENDIENTE** aún sin ejecución.

Se repitió una búsqueda Gmail de los últimos siete días con asuntos NotiUrba/[PRUEBA] y códigos NU-000: cero mensajes QA identificados en la cuenta conectada. El buzón del equipo sigue sin confirmar; no se concluye que esté vacío ni que el sistema haya fallado. RESEND_PRUEBA_DESTINO efectivo en Render sigue sin verificarse.

**BLOQUEADO:** revisión real Alcaldía/empresa, destinatarios, matrícula, miniaturas, HTML/text/plain y Gmail Android. No se enviaron correos ni se solicitaron reportes con envío. Las evidencias finales históricas de docs/correo se conservaron íntegras.


## Avance posterior en Chrome — 14/11/2026

Render Dashboard se pudo abrir, pero exige iniciar sesión. No fue posible leer el destino efectivo RESEND_PRUEBA_DESTINO y no se enviaron reportes. La recuperación de contraseña se probó únicamente con correo vacío: mostró Escriba su correo y no acredita entrega de enlace. Google/Gmail del equipo y correos de Alcaldía/empresa siguen pendientes. [Evidencia real de Chrome](evidencias/reanudacion-20261114/chrome-ui.json).
