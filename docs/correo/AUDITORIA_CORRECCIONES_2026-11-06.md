# Auditoría y correcciones del enrutamiento

Fecha: 6 de noviembre de 2026 (Colombia). Rama: `feat/enrutamiento-correos-notiurba`. Base del trabajo: `f5ad599db724476d56df0bb886b1c396bbdf533d`.

## Hallazgos y decisiones

| Punto solicitado | Resultado y corrección |
|---|---|
| Reporte e historial al aceptar el correo | V2 ya contiene `tg_historial_sincroniza`, AFTER INSERT sobre `historial_estado`, y `tg_historial_guardia`, que prohíbe retrocesos. El servicio inserta el historial dentro de la misma transacción que la confirmación del proveedor. No se añadió un UPDATE redundante de `reporte`. Se agregó una aserción de historial único a la integración. El mecanismo se comprobó directamente en PostGIS. |
| Ruta antigua en React | Confirmado: la ruta anterior seguía visible hasta resolver la petición siguiente o incluso después de fallar esta. Ahora cada respuesta está vinculada a categoría y emergencia; una selección distinta la invalida durante el render. Cambiar o limpiar categoría/emergencia descarta la respuesta anterior y un error deja ruta nula, sin habilitar el guardado final. |
| Recuperación de fotos | El código ya se conservaba antes de subir fotos, pero se podía abrir la recuperación mientras continuaban subidas; además el sondeo podía borrar el error y el panel de evidencias podía pertenecer a otra consulta. Se deshabilita la recuperación hasta terminar la operación, se suspende el sondeo mientras está ocupada, se conserva el error y se limita el panel al código consultado. Después de una subida, incluso si su respuesta falla, se vuelve a consultar el mismo reporte para reflejar las evidencias realmente guardadas. Nunca se crea otro reporte por este fallo. |
| Cola deshabilitada | El planificador era condicional, pero `procesarSiguiente()` podía invocarse directamente sin comprobar la bandera. Ahora retorna antes de acceder a la BD o al proveedor cuando `notiurba.correo.reportes-habilitados=false`, valor predeterminado. Prueba unitaria aprobada con verificación de cero interacciones. Se refiere a los correos de reportes; no se altera la autenticación ni su correo de verificación. |
| Migración sobre historial existente | Confirmado y reproducido en PostgreSQL: el índice único anterior impedía actualizar una base con dos pendientes históricos del mismo reporte, permitidos por V1–V12. El índice de V13 se limita a pendientes con clave de idempotencia. No se borran ni reclasifican los registros históricos; el trabajador ignora los que carecen de clave o mensaje preparado. Sigue prohibido duplicar pendientes nuevos. |
| Rebote y estado avanzado | La interfaz afirmaba siempre que se conservaba «Enviado», incluso si el reporte ya estaba «En trámite». Ahora muestra el estado real, sin modificar el historial. Hay regresión React SSR para ese caso. |
| Diferencias verificadas con Figma | Faltaban los iconos del correo, cerrar, copiar, información y chips, y la fuente Nunito Sans no estaba instalada. Se incorporaron los SVG originales y la fuente local con su licencia, manteniendo las dimensiones raíz 36/20/14 px y el círculo de 80 px. No se añadieron pantallas ni acciones nuevas. |

Se extrajo la secuencia de guardado/subida/envío a un helper usado por el formulario para probar los fallos con el mismo código de producción. No se añadieron endpoints, opciones de negocio ni dependencias al manifiesto.

## Pruebas y compilaciones ejecutadas

| Verificación | Resultado |
|---|---|
| Build completo Java 21 | Aprobado: Temurin 21.0.13+9, POM oficial Spring Boot 3.3.5 sin cambios. Maven compiló las fuentes y pruebas; `package -DskipTests` generó el JAR ejecutable. La clase del servicio tiene versión 65 (Java 21). El empaquetado con pruebas omitidas no se presenta como aprobación de las integraciones. |
| Suite Maven con integración habilitada | 113 pruebas: 103 aprobadas, 0 fallos de aserción, 8 errores y 2 omitidas. Los 8 errores corresponden a las 7 pruebas de `EnrutamientoIntegracionTest` y la de `MigracionEnrutamientoTest`: conexión JDBC bloqueada por `java.net.SocketException: Permission denied: getsockopt`, antes de ejecutar sus aserciones. |
| Pruebas backend relacionadas | Aprobadas: `ColaDeshabilitadaTest` (1), `FotoServicioTest` (9), `ReporteServicioTest` (9), `EnviadorCorreoResendTest` (7), `PlantillaReporteTest` (2), `FirmaWebhookTest` (1). La prueba de correo real permaneció omitida y no se proporcionaron claves de Resend. |
| Regresión frontend | 9 pruebas Node aprobadas: estados, enlaces, rutas antiguas/tardías, segunda foto fallida y conservación tras fallo al solicitar envío. |
| Cinco variantes React | Compilación Oxc y render SSR del componente real aprobados para pendiente, fallo inicial, aceptado, rebote y canal externo; adicionalmente rebote con estado general «En trámite». Link se adapta únicamente en el comprobador. No sustituye interacción ni medición de viewport en navegador. |
| Build frontend | Aprobado con Vite 8.3.4 y las dependencias existentes del manifiesto. Advertencia de bloque JS superior a 500 kB; no se cambió el fraccionamiento por estar fuera de esta auditoría. |
| ESLint y diff | Sin errores de ESLint ni advertencias; `git diff --check` sin errores. |
| Navegador y responsive | Se reconsultaron los diseños de Figma y se verificaron tamaños de SVG, ubicaciones de uso, estilos y recursos locales. La conexión de automatización del navegador agotó su tiempo de espera. No se afirma inspección visual completa, coincidencia píxel a píxel ni aprobación de móvil/escritorio; no se fabricaron capturas. |

Para resolver las herramientas sin tocar el proyecto se descargó el JDK desde Adoptium y se verificó su SHA-256. Las dependencias faltantes se obtuvieron de Maven Central y del registro oficial de npm en carpetas de validación/node_modules ignoradas. Se conservaron `backend/pom.xml`, `frontend/package.json` y `frontend/pnpm-lock.yaml`. La JVM no pudo abrir sockets propios y el primer intento de pruebas también encontró bloqueado el canal de attach; Surefire se ejecutó sin fork, cargando explícitamente el agente Byte Buddy existente al iniciar la JVM. No se desactivó TLS ni se cambiaron permisos del sistema.

## Comprobación SQL real sobre base desechable

Docker local estaba inaccesible (`permission denied` al motor). Se creó una BD nueva y vacía `notiurba_test_correo_20261106` dentro del proyecto de pruebas `NotiUrba-EDT51-isolated`, sin copiar datos de producción ni cambiar planes o computes. Motor confirmado: PostgreSQL 18.6; PostGIS disponible 3.6.4.

Tras fallar la conexión Java se ejecutó, mediante el conector, el SQL original de las migraciones V1–V12, una carga sintética de dos pendientes históricos y después V13: 163 sentencias en una transacción, incluidas las aserciones. Solo se sustituyó el placeholder de la contraseña del administrador por un valor sintético.

Resultado de las aserciones SQL: `SQL_AUDIT_OK`.

- 14 rutas presentes; las diez municipales apuntan al CAIC y el canal de emergencia de gas es 164.
- Dos registros pendientes históricos conservados después de V13.
- El índice corregido rechaza un segundo pendiente nuevo del mismo reporte.
- Un correo pendiente deja el reporte en Recibido.
- Al simular aceptación e insertar el historial, `tg_historial_sincroniza` actualiza el reporte a Enviado.
- Un rebote simulado cambia el correo a Fallido, manteniendo Enviado y el número de entradas del historial.
- El reporte y su evidencia sintética permanecen guardados.
- Una comprobación adicional reprodujo `unique_violation` al intentar crear el índice anterior, y confirmó cero pendientes históricos elegibles por la consulta del trabajador nuevo.

Esto comprueba compatibilidad del SQL y comportamiento de los triggers en una base real. **No equivale a ejecutar satisfactoriamente Flyway por JDBC ni a aprobar las integraciones Java bloqueadas.** V1–V12 permanecen intactas. V13 se corrigió en esta rama local antes de desplegarla; en esta ejecución no se aplicó a ninguna base del proyecto productivo. Una base que ya hubiera aplicado otra versión de V13 necesita gestionar esa diferencia mediante una nueva migración, sin reparar checksums a ciegas.

La base desechable y el archivo temporal con sus credenciales se eliminaron al concluir las comprobaciones. No se publican contraseñas ni cadenas de conexión.

## Pendientes y preservación

Pendientes: ejecutar las ocho integraciones Java en un entorno que permita JDBC y comprobar en navegador los cinco estados a tamaños móvil/escritorio. La compatibilidad de Flyway contra una base productiva concreta no se certifica sin acceso de lectura a su historial; esta tarea no lo solicitó ni lo modificó.

No se enviaron correos reales, no se configuraron proveedores, no se desplegó, no se creó ni fusionó PR. El aviso de privacidad, main y la rama `feature/integracion-not31-edt51` se conservaron. Esta última mantiene el commit `cd17f47e8d9db865bac43230b63c09932e73f3db`.
