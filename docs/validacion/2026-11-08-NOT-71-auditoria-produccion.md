# NOT-71: auditoria final y preparacion para produccion

**Continuacion posterior:** [diagnostico, inventario exacto, limite de duplicacion y gates del caso unico](2026-11-08-NOT-71-desbloqueo-preparacion.md). Se conservan las evidencias inferiores; el nuevo corte no repite QA ni ejecuta escrituras productivas.

8 de noviembre de 2026, America/Bogota. Repositorio `C:\notiurba\noti-urba`, rama `fix/registro-pr20`, HEAD `e61c44001231b595fdb2faa8866154fed79559ac`. **NO LISTA para iniciar la E2E productiva con escrituras. Mantener In Review.**

Las aprobaciones QA se conservan, incluida la foto/SQL ejecutados por el usuario y la regresion UI aislada 9/9. No se repitieron esas pruebas. Esta auditoria utilizo GET publicos, metadatos de despliegues, lecturas de codigo y transacciones PostgreSQL de solo lectura. No se aplicaron migraciones, no se escribieron datos productivos, no se enviaron correos ni se hicieron commits, push o despliegues.

## Version publicada y diferencias

| Elemento | Resultado comprobado |
|---|---|
| Sitio oficial | notiurba, site ID 0356024d-e4e2-4c3e-9a8e-2ca0dfc245ea; no confundir con notiurba-prototipo-evaluacion |
| Netlify produccion | Deploy 6aeeca108ce1404128b3e366, contexto production, ready, rama develop, commit **5e438067fd6088016e098517c4c8cd479cfa396a**, publicado 7-nov a las 01:50 America/Bogota |
| Frontend servido | GET /registro 200. Principal `/assets/index-DtWzeXbA.js`, SHA256 c7abbca3862d81c747dd666358ef35e5bedcdf189bf661d9bb2d9e024540ba11; contiene API `https://notiurba-api.onrender.com` y transporte fetch real; no contiene el modulo de simulacion buscado |
| PR #27 | Abierto, draft, no merged; head fix/registro-pr20/e61c440, base develop/e5d73c4. Su Preview 6af0ccdee968e3c8aa8a239c esta ready, contexto deploy-preview, NO publicado como produccion |
| Diferencias versionadas | 5 commits desde el commit publicado al HEAD local; 76 archivos distintos en frontend/src y backend/src/main. Inventario TSV guardado, no se interpreta todo el PR como aprobado |
| Correccion posterior QA | PasoConfirmacion.jsx y ReportarLayout.jsx modificados sin commit: limpieza solo despues de llegar a la URL final. No estan en el commit publicado ni en el Preview de e61c440 |
| Confirmacion publicada | El chunk rutas-fvCRdP7h.js conserva limpieza antes de navigate y no contiene `notiurba.reporteGuardado`. Esto demuestra codigo anterior, no una nueva reproduccion del fallo en produccion |
| Backend Render | Commit/imagen activa NO verificables: el panel expiro al conectar y no hay conector Render disponible. El historial Flyway de Neon no identifica la version Java desplegada |

Los metadatos Netlify se obtuvieron por el ID oficial descubierto en el Preview del PR. La busqueda inicial del conector solo devolvia el prototipo; no se sustituyo el sitio oficial por ese proyecto diferente.

## Disponibilidad HTTP y variables

Primera ronda: GET de /api/health, /api/catalogos/barrios, /api/catalogos/tipos-documento, /api/enrutamiento/categorias, /api/reportes/opciones/barrios, /api/reportes, /api/auth/login y /api/auth/registro. Los ocho agotaron 20 segundos desde este cliente sin respuesta HTTP. Un unico reintento focalizado de salud y barrios tambien agoto 25 segundos. **No hay codigo HTTP observado ni nueva respuesta [] de Render en esta auditoria.** No se atribuye por inferencia a cold start, CORS, credenciales o caida global. Registrar y revisar logs/estado del servicio antes de repetir el recorrido.

Las rutas GET de auth son sondeos de disponibilidad, no pruebas de login/registro (esos contratos usan POST). Cuando la API responda se espera 200 en salud/catalogos publicos, 401 sin token en categorias/enrutamiento/reportes protegidos y 405 en GET de auth si el contrato activo coincide. No se intenta autenticacion productiva ni se consulta el NU-000001 local en produccion.

Las solicitudes incluyeron Origin https://notiurba.netlify.app, pero sin respuesta no se pudo comprobar CORS. Los valores administrativos de Netlify NO se pudieron leer: el lector de variables respondio que el sitio/equipo no existe en su contexto, aunque el lector de metadatos si pudo leer el sitio. No se infiere que las variables esten ausentes. Tampoco se observaron variables efectivas de Render. No se guardaron credenciales.

| Variable/ajuste | Valor o condicion requerida antes del ensayo | Evidencia actual |
|---|---|---|
| VITE_API_URL, build Netlify | https://notiurba-api.onrender.com | Confirmado en bundle servido; valor administrativo no accesible |
| VITE_USAR_MOCKS | false en build productivo; codigo local limita simulacion a DEV | Bundle con fetch real y sin modulo simulacion buscado; valor administrativo no accesible |
| Base/build/publish Netlify | Revisar que compila frontend y publica su dist, sin QA/servidores temporales | Artefactos Vite y fallback SPA publicados; comandos administrativos no disponibles por este conector |
| DB_URL/DB_USER/DB_PASSWORD Render | Base Neon y rama aprobadas, TLS, secretos gestionados | Identidad runtime no leida; no afirmar que Render apunte hoy a la rama auditada |
| CORS_ORIGINS | Incluir exactamente https://notiurba.netlify.app; Preview separado si se autoriza | Regla fuente revisada, cabeceras runtime no comprobadas |
| VERIFICACION_ENLACE | https://notiurba.netlify.app/app/verificar-correo | Fuente: esta ruta consume el token. `/verificar-correo` solo informa y requiere state.correo; no sirve como destino del enlace |
| RECUPERACION_ENLACE / RECUPERACION_MOSTRAR_ENLACE | URL HTTPS publica correcta / false | Defaults fuente localhost; .env.example es de desarrollo y muestra log=true. No son valores runtime comprobados |
| RESEND_API_KEY / RESEND_REMITENTE | Configurados en servidor, remitente autorizado y buzon de ensayo controlado | No verificables en Render. Registro 201 no garantiza envio: VerificacionServicio captura el fallo del proveedor |
| CLOUDINARY_CLOUD_NAME / API_KEY / API_SECRET | Tres valores no vacios en Render, cuenta/carpeta aprobadas | No verificables. La app arranca sin claves y falla solo al subir; salud 200 tampoco probaria fotos |
| URL/carpeta Cloudinary | API HTTPS oficial, carpeta productiva definida, sin localhost ni adaptador QA | Fuente application.yml: api.cloudinary.com, notiurba/reportes. No acredita configuracion efectiva |
| JWT_SECRET / GOOGLE_CLIENT_ID | Configuracion server valida; OAuth solo si el caso lo requiere | No se leyeron secretos ni se ensaya Google. No exponerlos en VITE_* |
| NOTIURBA_ENVIO_REPORTES_HABILITADO / RESEND_PRUEBA_DESTINO | Auditar presencia/valor funcional sin revelar secretos y elegir mecanismo sin entidades | Defaults false/vacio en fuente; runtime desconocido. El destino de prueba es GLOBAL para reportes |

## Catalogo: causa comprobada y decisiones

Neon, proyecto NotiUrba `restless-cake-46112011`, rama production `br-proud-waterfall-b44u8y7p`, base neondb/public, PostgreSQL 18: se ejecuto SET TRANSACTION READ ONLY y se confirmo transaction_read_only=on. Resultado actual: **0 barrios publicos, 1 interno, 10 comunas, ultima migracion V16 correcta, 0 migraciones fallidas**. El unico barrio es Administracion NotiUrba, ID 1/comuna 1/interno=true. La secuencia barrio_id_barrio_seq tiene last_value=1, is_called=true e incremento 1; no se llamo nextval/setval.

CatalogoController y CorreoReporteController consultan WHERE NOT interno. La ausencia de filas publicas explica [] en esa base; no es necesario quitar el filtro ni publicar al administrador. V17 permanece fuera de classpath:db/migration. La conexion efectiva Render -> esa rama aun requiere comprobarse, por lo que se separa la causa de datos observada de una afirmacion sobre su configuracion runtime.

V17 conserva SHA256 `e824e2b71011938b8c0666f07b302ac537a81bd27865689055048fb503dd4fec`, 135 asociaciones PMGR 2020, sin fijar IDs y con ON CONFLICT(id_comuna,nombre) DO NOTHING. SQL/CSV ya fueron probados en aislamiento; no se repitieron. Datos historicos, FK e idempotencia no certifican vigencia administrativa.

| Decision | Pendiente de aprobacion de Andres y responsable de datos |
|---|---|
| B01 | El Principio / El Principe, comuna 5: confirmar denominacion, sin renombrar por intuicion |
| B02 | Lusitania / Urbanizacion Lusitania, comuna 5: confirmar entidades distintas o alias |
| B03 | Diablos Rojos, comunas 7 y 8: antecedentes 2024 distinguen I/II; confirmar nombres y conservar identidades correctas |
| B04 | San Luis / Urbanizacion San Luis, comuna 8: confirmar correspondencia; ausencia en otra lista no acredita fusion |
| B05 | Santa Ines / Santa Ines-Comfamiliar, comuna 8: antecedente documental respalda dos entradas; aprobar nombres finales |

`revision-andres.json` sigue PENDIENTE_DE_APROBACION, aprobadoPor/aprobadoEn/decisiones null. Aprobar tambien vigencia/cobertura de las 135 asociaciones o su uso historico provisional explicitamente rotulado. B03/B05 aclaradas documentalmente NO significa aprobadas. Si se modifica el catalogo, conservar la candidata original y generar nuevos CSV/SQL/hashes aprobados y validaciones afectadas.

## Tabla de pendientes para habilitar produccion

| Problema pendiente | Servicio | Correccion necesaria | Archivos involucrados | Riesgos | Responsable/autorizacion | Evidencia para aprobar |
|---|---|---|---|---|---|---|
| Frontend publicado anterior al QA | Netlify/GitHub | Revisar PR27, integrar cambios seleccionados y despues desplegar con permiso | frontend/src/app/reportar/{PasoConfirmacion,ReportarLayout,ReporteEnviado,borrador}.jsx/js; cambios listados en TSV | Reintroducir regreso a Categoria o duplicacion; promover Preview como si fuera produccion | Equipo frontend + revisor PR27 + autorizacion commit/integracion/despliegue | SHA aprobado igual al deploy de produccion y confirmacion/recuperacion sin duplicados |
| API sin respuesta desde cliente auditor | Render | Revisar estado, ultimo deploy, logs, arranque y DB; corregir causa comprobada | backend/Dockerfile, backend/src/main/resources/application.yml, HealthController, configuracion del servicio | Diagnosticar CORS o DB por suposicion; probar contra una API no disponible | Responsable Render; permiso separado si se requiere cambio/redeploy | GET salud/catalogos con respuesta, logs sanitizados y SHA/imagen activa identificada |
| Variables runtime y CORS no auditables | Netlify/Render | Lectura administrativa autorizada de flags/presencia, destino API, enlaces y CORS; corregir solo diferencias verificadas | application.yml, SecurityConfig.java, frontend/src/compartido/api/cliente.js, ajustes Netlify/Render | API equivocada, enlace localhost, secretos VITE o bloqueo navegador | Propietarios de ambos servicios con acceso de lectura; cambios posteriores autorizados | Inventario redactado, bundle correcto y GET/OPTIONS CORS del origen exacto |
| 0 barrios publicos; V17 candidata | Neon/datos | Resolver B01-B05/vigencia; aprobar carga aditiva y version libre | backend/migraciones-pendientes/barrios-pmgr-2020/{README.md,V17__catalogo_barrios_pmgr_2020.sql}; revision-andres.json | Oficializar datos historicos, fusionar alias o afectar referencias | Andres + DAPM/responsable de datos; permiso expreso de migracion | CSV/SQL/hashes firmados, preflight, Flyway exitoso y claves naturales publicas correctas |
| Respaldo recuperable no acreditado | Neon/operacion | Respaldo directo cifrado, restauracion aislada y ventana/recuperacion acordadas | preparacion-NOT71-20261108/{01-preflight.sql,02-postflight.sql,GUIA-APLICACION.md}; complemento de esta auditoria | Perdida de escrituras posteriores, volcado con datos personales, version PostgreSQL/Flyway distinta | Responsable Neon/DBA + autorizacion de respaldo y restauracion aislada | Hash del volcado, listado, restauracion real verificada e inventarios antes/despues |
| Almacenamiento productivo sin verificar | Render/Cloudinary | Confirmar configuracion server sin valores sensibles y luego una subida productiva autorizada | FotosConfig.java, AlmacenFotosCloudinary.java, FotoServicio.java, application.yml | Salud positiva sin claves, 502, archivo huerfano o URL no recuperable | Responsable Render/Cloudinary + permiso de archivo/carpeta de ensayo | Configuracion redactada; POST201, SQLfoto, URL HTTPS y hash del mismo caso |
| Verificacion de correo productiva pendiente | Render/Resend/Netlify | Buzon de prueba controlado, remitente valido y enlace /app/verificar-correo; endpoint real sin bypass | VerificacionServicio.java, CorreoConfig.java, ConfirmarCorreo.jsx, VerificarCorreo.jsx, App.jsx | Cuenta creada201 sin mensaje recibido; token en logs; enlace a pantalla equivocada | Responsable correo + buzon/identidad sintetica aprobados | Correo al buzon autorizado, confirmacion real, sesion y SQL redactados; sin publicar token |
| Ensayo sin notificar entidades | Render/datos | Preferir categoria14 sin correo si el equipo acepta; congelar configuracion durante ensayo. Si se exige correo, definir aislamiento de destinatario por caso antes de cualquier POST | EnvioReporteServicio.java, EnrutamientoServicio.java, configuracion_enrutamiento y registro_envio_correo | Worker apagado deja pendientes enviables despues; override global afecta otros reportes | Equipo funcional + responsable correo + aprobacion del caso y de cualquier ajuste | Ruta vigente permiteCorreo=false y NO_ENVIADO_CORREO/EXTERNO para el caso; ninguna entrega externa |
| E2E productiva y criterio final sin demostrar | Netlify/Render/Neon | Autorizar y ejecutar un solo caso despues de resolver los bloqueos anteriores | Plan abajo, informe y evidencias NOT71 | Confundir QA/UI simulada con integracion productiva; duplicar al reintentar | Usuario/equipo autoriza escrituras y acepta evidencias; revisor NOT71 decide cierre | Mismo usuario/reporte/codigo, foto/URL/hash/SQL/comuna/destino y confirmacion reales desde Netlify |

## Procedimiento de catalogo, preparado y no ejecutado

1. Aprobar alcance, B01-B05 y artefactos definitivos. El numero 135 solo es exigible si se aprueban esas 135 claves sin cambios; si la revision altera identidades, documentar el nuevo conjunto esperado. No fijar los IDs 43/14 del QA.
2. Identificar proyecto/rama/base y conexion directa TLS, rol y ventana autorizados. Ejecutar el preflight existente y el complemento `produccion-NOT71-20261108/05-preflight-complementario.sql` de solo lectura. Revisar restricciones, comunas, equivalencias, internos, historial y secuencia, sin nextval/setval. Confirmar V17 libre en DB y en todas las ramas del ejecutor; si esta ocupada, generar otra version candidata y ajustar guardas bajo revision, sin modificar una migracion aplicada.
3. Obtener respaldo logico completo en formato custom con cliente PostgreSQL18 compatible, conexion directa y credenciales gestionadas fuera del repositorio. Guardarlo cifrado con fecha/hash y acceso restringido. Registrar inventarios de barrios/IDs, referencias y Flyway; un bundle Git no respalda la base.
4. Ejecutar pg_restore --list y restaurar el volcado en otra base vacia aislada autorizada, con PostGIS compatible, sin reutilizar el QA sintetico ni sus credenciales. Verificar objetos, restricciones, conteos e historial. La lista del volcado no prueba restaurabilidad. No se hizo copia ni restauracion de datos productivos en esta auditoria.
5. Validar el SQL final aprobado con el ejecutor que se usara y PostgreSQL18. El JAR local tiene Flyway10.10.0 y JDBC42.7.4; la QA recuperada usaba PostgreSQL16. Registrar compatibilidad y resultado del ejecutor final sin repetir el recorrido frontend QA. No se exige upgrade por inferencia: resolver solo incompatibilidades demostradas.
6. Con permisos separados de migracion e integracion, promover SOLO el SQL aprobado a db/migration, revisar todas las pendientes, ejecutar info y validate mediante el ejecutor autorizado y despues migrate. Nunca ejecutar un INSERT manual y luego Flyway, nunca clean/repair para forzar un resultado, nunca desactivar el filtro interno. PostgreSQL permite transaccion para esta carga DML; confirmar el resultado real del ejecutor.
7. Ejecutar postflight, comparar claves naturales y filas originales/internas, FK y el registro de version/checksum/success. Total esperado = publicos_antes + inserciones_previstas, no imponer 135 si ya existian otros barrios. Para el estado auditado (0 publicos), si se aprueban las 135 claves originales, se esperan 135 publicos y 1 interno intacto.
8. Con Render disponible, GET publico200 debe coincidir con esas claves y excluir al interno. Tras el despliegue frontend autorizado, comprobar que el registro servido por Netlify lee la API correcta. Eso es validacion productiva futura, no repeticion QA.

Plantilla futura de respaldo/restauracion, **NO ejecutada**, con perfiles de conexion previamente aprobados; los nombres son placeholders, no configuracion existente:

```text
pg_dump --dbname="service=notiurba_prod_direct_ro" --format=custom --no-owner --file="<RUTA_CIFRADA>/not71-antes.dump"
pg_restore --list "<RUTA_CIFRADA>/not71-antes.dump"
pg_restore --dbname="service=notiurba_restauracion_aislada" --no-owner --no-privileges --exit-on-error --single-transaction "<RUTA_CIFRADA>/not71-antes.dump"
```

No poner claves en argumentos, capturas, .env versionados o informes. Ante fallo transaccional, detenerse y revisar historial/datos antes de reintentar. Si la carga se confirmo pero es incorrecta, preparar correccion nueva aditiva que conserve IDs/FK. No hacer DELETE masivo ni restaurar sobre produccion: cualquier restauracion productiva requiere autorizacion separada y preservar las escrituras posteriores al respaldo. Una restauracion inicial es siempre aislada.

Referencias tecnicas primarias verificadas: [pg_dump](https://www.postgresql.org/docs/current/app-pgdump.html), [pg_restore](https://www.postgresql.org/docs/current/app-pgrestore.html), [Flyway validate](https://documentation.red-gate.com/flyway/reference/commands/validate), [transacciones Flyway](https://documentation.red-gate.com/fd/migration-transaction-handling-273973399.html).

## Ensayo sin correo a entidades

Se leyo la configuracion productiva: categoria14 Recoleccion de basuras y barrido, requiere_ubicacion=true, entidad Veolia Aseo Tulua, dependencia Aseo, canal alterno Web, permite_correo=false. EnvioReporteServicio.solicitar registra EXTERNO/NO_ENVIADO_CORREO cuando la ruta no permite correo; no pasa al proveedor. Propuesta: utilizar esa categoria, datos/texto explicitamente sinteticos y no abrir/contactar el canal externo, SI el equipo acepta este caso para NOT71. Reconfirmar la regla y congelar cambios de enrutamiento durante la ventana. La ruta aprobada debe quedar en las evidencias.

La consulta actual encontro 0 registros en registro_envio_correo. Es una fotografia de este momento, no una garantia para una ventana futura. Apagar el worker no evita crear PENDIENTE con destino real; al reactivarlo podria enviarse. RESEND_PRUEBA_DESTINO redirige globalmente los correos de reportes, no es un aislamiento por cuenta y puede afectar solicitudes legitimas. No cambiarlo en esta auditoria. Si el criterio exige categoria con correo, la prueba queda bloqueada hasta aprobar un mecanismo que limite el destinatario al caso y evite entregas posteriores a entidades.

Verificacion de cuenta es independiente del worker de reportes: VerificacionServicio usa EnviadorCorreo directamente hacia el correo de la cuenta, guarda hash del token y consume el endpoint real. Usar un buzon dedicado controlado por el equipo y expresamente autorizado; example.test no recibe el correo. No saltar verificacion con UPDATE SQL ni reutilizar el fixture QA en Neon.

## Plan E2E productivo, sin ejecutar

| Paso | Condicion/accion tras autorizacion | Evidencia requerida |
|---|---|---|
| G0: permiso | Aprobar entorno exacto, ventana, buzon, categoria/ruta sin correo, foto/licencia, maximo 1 cuenta y 1 reporte, responsable y conservacion/limpieza futura | Autorizacion explicita registrada. El permiso de auditar no autoriza estos POST |
| G1: disponibilidad/version | Netlify SHA aprobado, Render SHA/imagen identificados, salud200/DBok, CORS y enlaces correctos, catalogo definitivo cargado, almacenamiento confirmado | Metadatos despliegue, GET/OPTIONS y flags/presencia redactados |
| G2: cuenta | Abrir Netlify/registro, elegir barrio aprobado, identidad sintetica y buzon autorizado, aviso/consentimiento real | Registro201 y fila de cuenta sin datos sensibles; correo real solo al buzon aprobado |
| G3: verificar/acceso | Abrir enlace HTTPS /app/verificar-correo, confirmar y autenticar por el endpoint real; comprobar retorno al flujo y sesion | Resultado real y estado verificado; no publicar token/JWT, no usar cuenta real |
| G4: reporte unico | Crear desde Netlify con categoria14 si fue aceptada, descripcion PRUEBA NOT71 SIN INCIDENTE REAL, punto urbano aprobado, foto publica/licencia conservada | POST201 y codigo propio productivo. No exigir NU-000001: ese codigo pertenece a QA y puede existir o no en otra base |
| G5: foto/confirmacion | Adjuntar una foto a ese codigo, confirmar navegacion automatica; reintentar solo sobre el mismo codigo si falla | POSTfoto201, URL HTTPS recuperable, hash esperado, capturas de codigo y datos a 360/1366 en despliegue |
| G6: destino/seguridad | Verificar regla sin correo y registro EXTERNO/NO_ENVIADO_CORREO, no abrir canal externo, no activar worker para el caso | SQL/estado del mismo reporte sin PENDIENTE-RESEND ni entrega a entidades |
| G7: persistencia | Consulta read-only del mismo caso: ciudadano/categoria, coordenadas, comuna derivada de poligono/regla, entidad y registro de destino/canal, foto/url/codigo | SQL sanitizado y GET autenticado por codigo. Comuna y destino derivados deben contrastarse con las vistas y el canal congelado; no son necesariamente columnas de reporte |
| G8: cierre | Contrastar la cadena completa con lo aprobado y registrar diferencias; autorizar cualquier limpieza aparte | Aceptacion del responsable de NOT71. Sin esa evidencia completa, conservar In Review |

La capa comuna desplegada proviene de V14 experimental. Verificar la asignacion funcional contra la capa instalada y documentar su alcance; no certificar precision administrativa oficial por inferencia. Si el equipo exige otra capa oficial, su aprobacion/implementacion es una dependencia separada.

## Cambios a revisar, versionar e integrar despues

- Dos cambios runtime aun sin commit: `frontend/src/app/reportar/PasoConfirmacion.jsx` y `ReportarLayout.jsx`; conservar su regresion QA y revisar la limpieza posterior a navegacion.
- PR27 ya contiene un conjunto mas amplio de frontend/backend/documentacion respecto a produccion: revisar el inventario TSV, especialmente registro/retorno, recuperacion de reporte, contratos del listado y SecurityConfig/CorreoReporteController. No publicar un paquete frontend que requiera contratos backend no desplegados.
- SQL candidata/preparacion siguen fuera de ubicaciones activas. Su promocion requiere aprobacion de datos y de migracion; no incluirla por accidente al integrar el PR.
- Harness `backend/src/test/java/com/notiurba/qa/*`, `frontend/qa/*`, scripts output y evidencias son de QA/revision. Conservarlos en el repositorio si se decide, sin activar adapters/usuarios/pruebas en el JAR o rutas productivas. Revisar el manifiesto dist y exclusiones antes de publicar; no hacer git add . indiscriminado.
- No se preparo un commit ni se cambio rama/remoto. Un commit/push puede disparar CD; cualquier aprobacion futura debe distinguir commit/integracion, migracion y despliegue de cada servicio.

## Evidencia y decision

Carpeta `evidencias-NOT-71-produccion-auditoria-20261108/`: metadatos conectores sanitizados, lecturas Neon, manifest/bytes de bundle publico, chunks y fragmento de navegacion, GET Render/timeouts y reintento limitado, inventario de diferencias. No contiene claves ni tokens.

Los hechos suficientes para bloquear el inicio productivo son: catalogo sin filas publicas y sin autorizacion de V17, frontend publicado anterior a las correcciones, disponibilidad API no acreditada en esta auditoria y configuracion runtime de correo/fotos/conexion aun sin verificar. El respaldo recuperable, la ventana y el caso productivo con escritura tambien requieren aprobacion. **Mantener NOT-71 en In Review.**


## Conservacion tras limpieza del 11 de noviembre de 2026

Los logs completos, intentos fallidos provisionales y copias de builds remotos se retiraron del seguimiento. Las menciones anteriores describen el ensayo historico, no dependencias de ejecucion ni enlaces de descarga actuales. Se conservan los resultados resumidos, capturas unicas, hashes y scripts vigentes. El inventario exacto y la recuperacion historica estan en [el informe de limpieza](2026-11-11-limpieza-repositorio.md).
