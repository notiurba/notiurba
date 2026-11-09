# NOT-71: diagnostico y condiciones para desbloquear produccion

Corte del 8 de noviembre de 2026, America/Bogota; algunos JSON tienen fecha UTC del 9 de noviembre. Rama `fix/registro-pr20`, HEAD `e61c44001231b595fdb2faa8866154fed79559ac`. **In Review. No habilitada la ejecucion de POST productivos.**

Se conservan las aprobaciones QA. No se repitieron registro, barrios, foto, SQL QA, responsive ni regresion de navegacion. No se creo otra cuenta/reporte ni se modifico produccion. Este corte agrega diagnostico de acceso, revision estatica, inventarios exactos y un caso futuro revisable.

## 1. Render: hechos y limites de diagnostico

Nueva ronda de GET /api/health, /api/catalogos/barrios y /api/catalogos/tipos-documento, con Origin https://notiurba.netlify.app: los tres agotaron aproximadamente 25000 ms, sin codigo HTTP ni cuerpo. Evidencia: `evidencias-NOT-71-desbloqueo-20261108/render-disponibilidad.json`.

Se encontro la pestana existente del dashboard Render en Chrome. Al reclamarla, el control agoto el tiempo del comando de conexion; no se pudieron leer estado del servicio, despliegue, logs o variables. La lectura ya esta autorizada por el usuario: el bloqueo es de acceso tecnico, no una nueva solicitud de permiso. No se reinicio, desplego ni cambio un servicio. No hay conector Render habilitado entre las herramientas disponibles.

La pagina publica https://status.render.com mostro All Systems Operational en esta consulta. No prueba que notiurba-api funcione, no descarta una incidencia de un servicio y no permite atribuir este timeout a infraestructura general. Una lectura independiente del endpoint de salud por el lector web tambien resulto inaccesible.

| Capa | Lo observado | Lo que NO puede concluirse | Evidencia para resolver |
|---|---|---|---|
| Cliente/red de la auditoria | Timeout GET y fallo de conexion del control del navegador | No demuestra caida global ni fallo Spring | Consulta desde panel o cliente autorizado con respuesta/logs correlacionados |
| Aplicacion | Ningun HTTP500/stacktrace runtime disponible | No atribuir a beans, autenticacion o codigo | Logs de arranque/ejecucion, exit code y version de artefacto |
| Base | Se conserva lectura Neon anterior con schema sano, V16 y 0 publicos | No acredita que Render use esa rama o consiga JDBC | Comparacion privada de DB_URL/base/rol, TLS y SELECT1 de /api/health |
| Infraestructura/puerto | Estado publico general operativo; sin metricas del servicio | No afirmar cold start, OOM o puerto incorrecto | Estado de deploy, memoria, reinicios, bind/PORT y health checks del servicio |

Version Render actual: **no identificada**. JDBC/Flyway/puerto/CORS efectivos: **no verificados**. Se conoce el contrato fuente: datasource por DB_URL/DB_USER/DB_PASSWORD; server.port por PORT (8080 por defecto); Flyway habilitado y ubicacion classpath:db/migration; Docker arranca Java21 con -jar. El harness QA que fija loopback/18092 esta en src/test, no debe arrancarse en Render.

Para completar esta fase, el propietario debe facilitar una sesion operativa de lectura o un extracto redactado de: SHA/imagen/ultimo deploy/estado/fecha; arranque y terminacion; puerto escuchado; presencia y destino no secreto de JDBC; Flyway info/errores; origenes CORS. No copiar claves, URL JDBC con password, JWT_SECRET, valores Resend/Cloudinary o tokens de correo. Revisar tambien presencia de propiedades obligatorias como ADMIN_DEV_PASSWORD segun el artefacto, sin rotarlas ni inventar una causa por su ausencia presunta.

**Causa de la falta de respuesta: no demostrada.** No se repiten indefinidamente los mismos sondeos ni se inventa un arreglo de infraestructura sin logs.

## 2. Versiones, inventario exacto y orden de revision

Se reconfirmo por metadatos Netlify el deploy publicado 6aeeca108ce1404128b3e366, ready; corresponde al commit 5e438067fd6088016e098517c4c8cd479cfa396a de la auditoria anterior. El PR27 se revisa en e61c440; sigue separado del artefacto productivo. El estado combinado de ese commit muestra Preview success y el workflow existente EDT 5.1 backend validation, run 42515044651, conclusion success. Son resultados ya existentes, no ejecuciones nuevas ni cobertura de las dos modificaciones locales posteriores.

Inventarios para el equipo:

- `evidencias-NOT-71-desbloqueo-20261108/inventario-exacto-versionado.csv`: **76 filas**, una por archivo que difiere entre produccion y HEAD; estado A/M, grupo/orden de revision, SHA256 actual y marca de modificacion posterior a HEAD.
- `inventario-local-qa-preparacion.csv`: **17 archivos sin seguimiento** de harness, prueba UI, scripts y preparacion de migracion; no equivale a autorizacion para activarlos en produccion. Evidencias y documentos se conservan aparte.
- `resumen-inventario.json`: conteos por grupo.

| Orden de revision | Archivos/conjunto | Decision necesaria |
|---|---|---|
| 10: contrato backend | SecurityConfig.java, CorreoReporteController.java, ListadoReportesController/Repositorio/Servicio y ReporteResumenResponse | Revisar CORS y contrato de GET /api/reportes. Retirar el handler antiguo y agregar el nuevo como un conjunto; no dejar dos mappings iguales ni ninguno |
| 15: alcance adicional | EliminacionDatosServicio.java, PerfilController.java y Rol.java | Revisar alcance de borrado de perfil/constante ADMIN, sus dependencias y permisos; no incluir ni excluir aisladamente por automatismo |
| 20: acceso/contratos frontend | App.jsx; api/{auth,catalogos,cliente}; retorno/NavegacionAcceso; registro/verificacion/privacidad y componentes compartidos del CSV | Alinear con backend autorizado y mantener los retornos y reglas ya verificados |
| 30: reporte frontend | reportar/* y servicios/{recuperacionReporte,correoReportes} segun diff/dependencias | Integrar recuperacion de codigo/borrador, categoria/ubicacion/detalles/confirmacion y presentacion coherentes |
| 40: dos correcciones locales | PasoConfirmacion.jsx y ReportarLayout.jsx | Revisar y versionar juntas sobre el conjunto compatible del PR27. No son un parche independiente para 5e43806 |
| 80/90: pruebas y simulacion | *.test.mjs, src/simulacion/*, frontend/qa/*, src/test/java/com/notiurba/qa/* | Conservar las evidencias; simulacion solo DEV. No incluir el adaptador local en el runtime ni publicar entradas de prueba |

La tabla describe orden de revision, no instrucciones de despliegue parcial. El backend compatible debe estar identificado y disponible antes de publicar un frontend que dependa de su contrato. No se aplico ningun orden mediante Git ni se prepararon commits.

Antes de cualquier push/merge, identificar los disparadores CD de ambos servicios. Si esa accion puede desplegar automaticamente, debe obtenerse tambien el permiso de despliegue ANTES del push/merge; no asumir que publicar codigo y desplegar son acciones independientes en la configuracion actual.

### Las dos correcciones

1. `PasoConfirmacion.jsx`: quita `limpiar()` antes del `navigate()` final. Conserva el avance del reporte mientras el paso de confirmacion sigue montado.
2. `ReportarLayout.jsx`: limpieza estable y diferida hasta que pathname coincida con `/reportar/enviado/` y el codigo ya guardado. Evita activar prematuramente la guarda de Categoria y limpia al llegar.

Ambas dependen de borrador.js (leerGuardado/guardarAvance), useReporte y recuperacionReporte.js/correoReportes. La version publicada carece del mismo conjunto de recuperacion. No copiar solo esos dos archivos sobre una base incompatible. La regresion UI 9/9 y las comprobaciones QA existentes se reutilizan; no se reejecutaron.

### Alcance real de la proteccion de duplicados

La revision estatica confirma bloqueo de envio concurrente mediante ocupado.current; guardado del codigo antes de las fotos; reutilizacion de guardado en guardarConEvidencias; conciliacion de fotos ya persistidas; y encolado sobre el mismo reporte. El traslado de limpieza no agrega una llamada a guardarReporte y conserva el avance mas tiempo. No se observa una ruta nueva de creacion duplicada introducida por estos dos cambios.

**Limite existente:** no se encontro una clave de idempotencia de creacion en ReporteController/Servicio/Repositorio. El INSERT genera un codigo nuevo para cada creacion. Si el servidor confirma el primer INSERT pero se pierde su respuesta antes de conocer el codigo, el frontend no dispone del codigo guardado y un reintento de creacion podria duplicar. Es una limitacion deducida del contrato, no un fallo nuevo reproducido ni una invalidacion del caso QA sin duplicados. No confundir la clave_idempotencia del correo con idempotencia de POST /api/reportes.

En el ensayo se permite un solo POST de creacion. Ante respuesta ambigua: detenerse; conciliar por usuario, ventana y marcador unico usando listado/SQL read-only; recuperar el codigo si existe. Si no puede determinarse el resultado definitivo, no volver a crear. Una mejora futura de idempotencia de creacion requeriria diseno y autorizacion propios; no se implemento aqui.

## 3. V17, decisiones y respaldo

Se conservaron las lecturas Neon anteriores: 0 barrios publicos, 1 interno, 10 comunas y V16 aplicada correctamente. No se consulto nuevamente Neon en esta fase. Comparacion de archivos actual: SQL y CSV coinciden exactamente en **135 pares** y V17 conserva SHA256 `e824e2b71011938b8c0666f07b302ac537a81bd27865689055048fb503dd4fec`. Evidencia nueva: correspondencia-V17-CSV.json. No es una nueva ejecucion SQL ni prueba QA.

| Decision de Andres/responsable de datos | Aprobacion pendiente |
|---|---|
| B01: El Principio/El Principe, comuna5 | Denominacion y fuente; no renombrar automaticamente |
| B02: Lusitania/Urbanizacion Lusitania, comuna5 | Entidades distintas o alias, sin perder referencias |
| B03: Diablos Rojos, comunas7/8 | Identidades y nombres definitivos; respaldo I/II previo no equivale a aprobacion |
| B04: San Luis/Urbanizacion San Luis, comuna8 | Correspondencia administrativa; ausencia en otra lista no prueba fusion |
| B05: Santa Ines/Santa Ines-Comfamiliar, comuna8 | Conservar entidades correctas y aprobar denominaciones |
| Alcance general | Vigencia de las 135 asociaciones o aceptacion explicita de uso historico provisional; CSV/SQL/hash/conteos/version finales |

aprobadoPor, aprobadoEn y decisiones siguen null. Si cambia el conjunto, preparar nueva candidata y evidencia afectada sin sobrescribir la historica. V17 sigue fuera de Flyway activo.

Dependencias: V1 aporta barrio/comuna, claves y restricciones; V4 creo la referencia administrativa que debe conservarse; V15 aporta interno y el filtrado publico; V13/V14 aportan vistas/asignacion y capa experimental para la comuna del reporte; V12 corresponde al aviso de privacidad; V16 aporta enrutamiento/cola del ensayo. La insercion de barrios no carga geometrias ni resuelve vigencia administrativa. Revisar todo el historial con Flyway; no reejecutar V1-V16 ni promover otras carpetas pendientes por accidente. Reconfirmar numeracion justo antes de una migracion autorizada.

Procedimientos revisados: `preparacion-NOT71-20261108/01-preflight.sql`, `02-postflight.sql`, GUIA-APLICACION.md y `docs/validacion/produccion-NOT71-20261108/05-preflight-complementario.sql`. Son lecturas/guardas de identidad, historial, claves, internos y secuencia; requieren revisar resultados y conteos, no son una aprobacion automatica para cargar. El complemento no se ejecuto como archivo.

**Estado de respaldo:** no consta en el paquete auditado evidencia de pg_dump productivo restaurado y verificado. Un respaldo de codigo o base QA no lo sustituye. El plan existe; su ejecucion permanece pendiente. Usar cliente PostgreSQL18 y conexion directa TLS, perfil/secretos fuera de Git, archivo custom cifrado/hash e inventarios previos. Restaurar primero en otra base vacia aislada autorizada con PostGIS compatible; pg_restore --list por si solo no prueba restauracion. No copiar esos datos a la QA sintetica ni reutilizar sus credenciales.

Revisar compatibilidad del ejecutor autorizado con PostgreSQL18 (JAR local: Flyway10.10.0/JDBC42.7.4; QA recuperada: PostgreSQL16). Solo despues de permisos, promover el SQL definitivo, revisar info/validate y todas las pendientes, aplicar con transaccion y realizar postflight. Ante fallo, verificar historial/transaccion; si ya confirmo una carga incorrecta, preparar correccion nueva conservando IDs/FK. No borrar masivamente ni restaurar encima de produccion sin permiso y plan para escrituras posteriores.

## 4. Garantia de correo limitada al caso, no basada solo en categoria14

Fuente revisada: EnrutamientoServicio resuelve la ruta desde configuracion activa; solicitar registra EXTERNO/NO_ENVIADO_CORREO con clave y mensaje null cuando permiteCorreo=false. El procesador selecciona PENDIENTE con clave_idempotencia y mensaje_json no nulos. ColaCorreoConfig solo programa el worker si reportes-habilitados=true; el servicio tambien comprueba ese flag. La bandera de categoria14 no controla todos los posibles correos de la aplicacion.

Emisores identificados: VerificacionServicio envia directamente al correo de la cuenta; EnviadorEnlaceRecuperacionCorreo es @Primary y envia recuperacion via Resend; el worker envia reportes. La implementacion Log de recuperacion NO significa que toda recuperacion sea local: el componente Primary registra y envia. El ensayo no acciona recuperar, reenviar, administracion, canales externos ni otras cuentas.

La categoria14 figuraba con requiere_ubicacion=true, permiteCorreo=false y canal alterno Web en la lectura previa. Antes de cualquier POST se requiere:

1. Identificar artefacto backend activo y comprobar que contiene esta logica; acceder a sus ajustes efectivos sin revelar secretos.
2. Aprobar el caso y estabilizar configuracion/canales de categoria14 durante la ventana. Volver a resolverla en la API real: si cambia permiteCorreo, no continuar.
3. Usar SOLO un buzon de prueba controlado/permitido por el equipo; verificar que el POST de registro no contiene una direccion de entidad. Permitir el correo transaccional de verificacion exclusivamente a ese buzon, con remitente y dominio autorizados.
4. Comprobar el enlace HTTPS `/app/verificar-correo`, CORS y configuracion Resend; no usar token fixture de QA ni UPDATE de verificacion en Neon. Mantener los tokens fuera de capturas/logs. No ejecutar recuperacion/reenvio en este caso.
5. Para el reporte, exigir EXTERNO/NO_ENVIADO_CORREO y clave_idempotencia/mensaje_json null, sin PENDIENTE-RESEND asociado. Esos datos se verifican solo en lectura del mismo caso. No abrir el sitio/formulario externo de la entidad.
6. No activar/desactivar globalmente el worker ni RESEND_PRUEBA_DESTINO durante esta auditoria. Worker apagado solo deja pendientes para el futuro; el override de destinatario es global y no intercepta todos los emisores transaccionales.

Estas condiciones previenen entregas de ESTE ensayo a entidades bajo el codigo/configuracion comprobados; no prometen detener trafico legitimo ajeno a la prueba. Sin control efectivo de version/ruta/destinatarios, la garantia no esta acreditada y los POST permanecen bloqueados. Si el equipo exige una categoria con correo, primero debe aprobar otro aislamiento por caso, no confiar en el flag de la categoria14.

Cloudinary: la app fuente exige cloud-name/api-key/api-secret y devuelve secure_url/public_id; arranca aunque falten valores, por lo que salud200 no valida la subida. Sigue pendiente lectura de presencia/configuracion efectiva en Render y aprobacion de la cuenta/carpeta de destino; no se uso ninguna clave real aqui. La prueba local con almacenamiento de archivos no valida Cloudinary productivo.

## 5. Caso unico concreto, no autorizado ni ejecutable aun

Archivo: `docs/validacion/produccion-NOT71-20261108/caso-unico-pendiente-autorizacion.json`, caso NOT71-PROD-20261108-01. Contiene identidad ficticia, documento propuesto, marcador unico, categoria14, punto propuesto ya usado tecnicamente en QA y JPEG publico/hash conocido. Buzon, password, ID definitivo de barrio y consentimientos siguen como placeholders, sin secretos. Verificar colision de identidad/documento y pertenencia del punto a la capa aprobada antes de autorizar; no asumir idBarrio43 en Neon.

Limites: una cuenta, un reporte, una foto, cero correos a entidades. El codigo productivo se anotara al recibirlo; no se impone NU-000001. Actualmente postEjecutados=0 y codigo productivo=null. No se ha registrado la identidad propuesta ni enviado un formulario.

## 6. Autorizaciones y orden seguro

| Gate | Decision/permiso necesario | Resultado verificable antes de avanzar |
|---|---|---|
| A0: lectura tecnica | Ya autorizado; falta acceso operativo a logs/estado/env de Render | SHA/imagen, estado, logs y configuracion redactada; causa clasificada solo con evidencia |
| A1: datos | Andres + responsable: B01-B05, vigencia, CSV/SQL/hash/version y capa/comuna aceptada | Registro firmado y conjunto esperado definitivo |
| A2: recuperacion | Responsable Neon: respaldo/exportacion restringida y restauracion en aislamiento | Hash, restauracion comprobada, inventarios e instrucciones de recuperacion aprobadas |
| A3: codigo | Equipo: revision del inventario y alcance PR27 + permiso separado de commit/push/integracion | Conjunto coherente, dos correcciones juntas, contratos y evidencias vinculados al nuevo SHA |
| A4: runtime | Responsable: permiso de cambios de configuracion y despliegue backend/frontend cuando procedan | Render disponible/contrato compatible, CORS/enlaces/proveedores comprobados; Netlify SHA final |
| A5: migracion | Permiso explicito sobre proyecto/rama/base/ventana, ejecutor y version definitiva | info/validate correctos, backup restaurable, migracion aprobada, postflight/catalogo correctos |
| A6: ensayo | Usuario/equipo: identidad/buzon, consentimientos, categoria/canal, punto/foto, POST permitidos, maximos y retencion | Caso JSON completado/aprobado, destinatarios controlados y regla sin correo estable |
| A7: limpieza/cierre | Autorizacion aparte si se borraran datos/archivos; revisor NOT71 acepta criterio completo | Evidencia del mismo caso y decision documentada; sin cierres basados solo en QA |

Orden operativo futuro: recuperar observabilidad y diagnosticar Render SIN cambios; cerrar decisiones de datos y respaldo; revisar contrato/codigo; obtener permisos de integracion y configuracion; asegurar backend compatible; aplicar la migracion definitiva solo con su permiso/backup; publicar frontend coherente con su permiso; verificar gates de lectura; ejecutar una sola E2E autorizada; conciliar resultados y decidir cierre. No se ejecuto ninguno de los pasos de escritura o despliegue en esta preparacion.

**Gates para comenzar POST productivos:** salud200/DBok, version activa identificada, CORS correcto, catalogo definitivo publico, respaldo restaurable, enlace/buzon/verificacion reales, Cloudinary configurado y destino de archivo aprobado, ruta sin correo estable bajo codigo verificado, caso/consentimientos/autorizacion explicitos y conciliacion ante respuesta ambigua. Falta cualquiera: no iniciar.

## Resultado

Linear aparecia Done en la lectura de este corte. Se restauro In Review siguiendo la instruccion explicita del usuario; no se alteraron descripcion, asignacion o adjuntos. Se registro el motivo y se conservaron las aprobaciones QA. La revision no aporta evidencia de la E2E productiva completa.

Diagnostico resuelto: se conserva causa de catalogo vacio por 0 filas publicas y filtro interno; la divergencia de version frontend y las dos correcciones estan identificadas exactamente. Fallo Render y configuracion efectiva: pendientes de acceso, sin causa inventada. Respaldo/restauracion: procedimiento revisado, ejecucion recuperable no acreditada. Se prepararon inventarios y un caso unico; no se altero V17 ni produccion. Mantener NOT-71 In Review.
