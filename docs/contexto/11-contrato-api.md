# Contrato de la API REST (NOT-43)

Copia de trabajo del diseño de la API para la pestaña «Diseño técnico (arquitectura y
modelo de datos)» del Google Doc. Está escrita sin depender del repositorio.

Estado de cada endpoint:
- **EXISTE**: implementado y verificado en el código (rama `feature/contrato-api`, parte de `develop`).
- **FALTA**: por implementar.
- *propuesto*: no es una decisión cerrada; falta el visto bueno del equipo.

## 1. Convenciones

- Producción: `https://notiurba-api.onrender.com` (Render Free, el primer arranque en frío tarda ~2 min). Local: `http://localhost:8080`.
- JSON en UTF-8, nombres en camelCase, fechas en ISO-8601 con zona (`2026-10-31T14:05:00-05:00`).
- Autenticación: encabezado `Authorization: Bearer <JWT>`. El JWT vence a los 60 minutos y lleva el correo (`sub`) y el `rol` (CIUDADANO o ADMIN).
- Rutas: públicas `/api/health` y `/api/auth/**` y `/api/catalogos/**`; ciudadano `/api/...` (exige sesión, cualquier rol); panel `/api/admin/...` (exige rol ADMIN, Sprint 3).
- Todo endpoint protegido responde 401 sin token, con token inválido o vencido (RNF-03), y 403 si el rol no alcanza.
- CORS: solo los orígenes de `CORS_ORIGINS`, separados por coma. Cada uno es una dirección completa o un patrón con comodín; en Render va `https://notiurba.netlify.app,https://deploy-preview-*--notiurba.netlify.app` (producción y las vistas previas de Netlify, una por PR). Cualquier otro sitio recibe 403. El encabezado `Location` está expuesto para que el navegador lo pueda leer.

## 2. Formato de error único (EXISTE)

Toda respuesta de error tiene el mismo cuerpo:

    { "codigo": "VALIDACION", "mensaje": "Completa el campo «Correo».", "campo": "correo" }

- `codigo`: texto estable que usa el frontend para decidir qué hacer. No cambia.
- `mensaje`: en español, listo para mostrar al ciudadano. Nunca trae detalles internos.
- `campo`: nombre del campo con el problema, o `null`.
- `datos` (opcional, NOT-132): objeto con cifras que el frontend usa para explicar mejor el error (segundos de espera, intentos que quedan, datos base de un reporte parecido). **Solo aparece en los errores que lo dicen abajo**; en los demás no viene. Nunca trae datos personales de otra persona.
- Si fallan varios campos se devuelve el primero en el orden del formulario (el orden de los campos de la petición).

| HTTP | codigo | campo | Cuándo |
| --- | --- | --- | --- |
| 400 | VALIDACION | el campo | Dato faltante, mal formado o fuera de regla; también cuerpo que no es JSON (campo null) |
| 400 | ENLACE_INVALIDO | token | Enlace de recuperación no válido o vencido |
| 401 | NO_AUTENTICADO | null | Sin token, inválido o vencido; también token de Google o de registro con Google inválido |
| 401 | CREDENCIALES_INVALIDAS | null | Login fallido. No dice cuál dato falló. Trae `datos.intentosRestantes` (cuántos fallos más antes del bloqueo) |
| 429 | INTENTOS_EXCEDIDOS | null | Demasiados intentos de login con ese correo. `datos.esperarSegundos`, `datos.bloqueadoHasta` (fecha-hora ISO en UTC) y el encabezado `Retry-After` (segundos). Mismo comportamiento para correos que no existen o que son solo de Google |
| 429 | LIMITE_DIARIO_REPORTES | null | La cuenta ya envió el máximo de reportes del día (día de Colombia). `datos.maximo` y `datos.vuelveEn` (inicio del día siguiente, con la hora de Colombia) |
| 409 | POSIBLE_DUPLICADO | null | Hay un reporte parecido (misma categoría, cerca, reciente, sin resolver). Se puede enviar igual con `confirmarDuplicado: true`. `datos`: ver 3.4 |
| 422 | EVIDENCIA_REQUERIDA | totalFotos | Se repite un problema que ya se había resuelto en ese lugar y el reporte no lleva ninguna foto |
| 403 | SIN_PERMISO | null | El rol no alcanza |
| 403 | CORREO_NO_VERIFICADO | null | Login con contraseña correcta, pero el correo aún no se confirmó |
| 404 | NO_ENCONTRADO | null | No existe o no es suyo (mismo mensaje en ambos casos) |
| 409 | CORREO_YA_REGISTRADO | correo | Correo repetido (sin distinguir mayúsculas) |
| 409 | DOCUMENTO_YA_REGISTRADO | numeroDocumento | Tipo y número de documento repetidos |
| 409 | CUENTA_GOOGLE_YA_REGISTRADA | null | Esa cuenta de Google ya tiene usuario |
| 409 | CONFLICTO | null | Otro choque (por ejemplo, carrera al crear la cuenta) |
| 409 | FOTOS_MAXIMO | foto | El reporte ya tiene 3 fotos |
| 413 | ARCHIVO_MUY_GRANDE | foto | La foto pasa de 1 MB |
| 415 | FORMATO_NO_SOPORTADO | foto | No es JPG ni PNG |
| 422 | FUERA_DE_COBERTURA | latitud | El punto no cae en ninguna de las 10 comunas ni a menos de la distancia máxima (50 m por defecto) de una |
| 502 | ALMACEN_FOTOS_NO_DISPONIBLE | null | Cloudinary no respondió |
| 500 | ERROR_INTERNO | null | Error no previsto; sin detalles |

Mensajes fijos:
- NO_AUTENTICADO: «Tu sesión no es válida o venció. Inicia sesión de nuevo.»
- SIN_PERMISO: «No tienes permiso para esta acción.»
- CREDENCIALES_INVALIDAS: «Correo o contraseña incorrectos.»

Solo existen en el frontend (no los manda el servidor): `SIN_CONEXION` (no hubo red) y
`RESPUESTA_INESPERADA` (respuesta que no sigue este formato, por ejemplo Render despertando).
Cualquier otro error HTTP sin código propio se convierte así: 404 en NO_ENCONTRADO, otros 4xx en VALIDACION, 5xx en ERROR_INTERNO.

## 3. Los 6 endpoints del MVP

### 3.1 Registro — `POST /api/auth/registro` — EXISTE
Público. Crea siempre rol CIUDADANO.

Entrada (en este orden):
- `nombres`, `apellidos`: obligatorios, de 2 a 60 caracteres; empiezan con una letra y solo llevan letras (con tilde o ñ), espacios, apóstrofo, punto o guion. Los espacios sobrantes se recortan. Es la misma regla del frontend (`ReglasPerfil`); la usan el registro, el alta con Google y «Editar mis datos». Mensajes: «… son obligatorios», «… deben tener al menos 2 letras», «… no pueden pasar de 60 caracteres», «… solo pueden llevar letras, espacios, apóstrofo, punto o guion» (campo `nombres` o `apellidos`).
- `idTipoDocumento`: obligatorio, existente (1 CC, 2 CE, 3 PPT, 4 PAS).
- `numeroDocumento`: obligatorio, máx. 30; único junto con el tipo.
- `idBarrio`: obligatorio, existente.
- `direccionCorrespondencia`: obligatoria, máx. 300.
- `correo`: obligatorio, formato de correo, máx. 254, único sin distinguir mayúsculas.
- `telefono`: opcional, máx. 30.
- `password`: obligatoria, mínimo 8 caracteres con al menos una letra y un número, máx. 72 bytes UTF-8 (regla real en `ReglaContrasena`); si es demasiado larga el mensaje lo dice («La contraseña es demasiado larga: use como máximo 72 caracteres»). Mensaje: «La contraseña debe tener al menos 8 caracteres, con letras y números».
- `aceptaPrivacidad`: obligatorio, `true`. El servidor guarda el aviso vigente (hoy la versión 3, migración V12) y la fecha-hora, igual que `POST /api/auth/google/completar`.
- `declaraMayorEdad`: obligatorio, `true`; si falta o es `false` responde 400 VALIDACION con `campo: "declaraMayorEdad"` (implementado en NOT-75, también en `POST /api/auth/google/completar`). En pantalla es la misma casilla del aviso, de un solo clic: «Soy mayor de 18 años y acepto el tratamiento de mis datos personales (Ley 1581). Ver aviso de privacidad» (también en «completar perfil» de Google). La base no guarda un campo aparte para esta declaración: queda en la fecha y la versión del aviso aceptado (`acepto_privacidad_en`, `id_aviso_privacidad`).
- «Confirmar contraseña» se valida solo en el frontend.

Salida: 201 `{ correo, mensaje }` **sin token**: la cuenta se crea, se envía un correo con el enlace de confirmación y el ciudadano no puede ingresar hasta confirmarlo (decisión 1, sección 6). Cada registro gasta 1 correo de Resend (el plan gratuito permite 100 al día, compartidos con los reportes, las recuperaciones de contraseña y los reenvíos). El correo sale **después** de guardar la cuenta y en segundo plano (`@TransactionalEventListener` AFTER_COMMIT + `@Async`): la respuesta no espera a Resend, así que el registro responde en menos de un segundo aunque el correo tarde, y si el envío falla la cuenta ya existe y se puede pedir el reenvío (NOT-132).
Errores: 400 VALIDACION, 409 CORREO_YA_REGISTRADO, 409 DOCUMENTO_YA_REGISTRADO.

### 3.2 Login — `POST /api/auth/login` — EXISTE
Público. Entrada: `correo`, `password` (obligatorios).
Salida: 200 `{ token, rol }` con rol CIUDADANO o ADMIN.
Errores: 400 VALIDACION; 401 CREDENCIALES_INVALIDAS (también para cuentas creadas solo con Google, que no tienen contraseña); 403 CORREO_NO_VERIFICADO (solo se dice después de acertar la contraseña, para no revelar qué cuentas existen); 429 INTENTOS_EXCEDIDOS.

Límite de intentos (NOT-132, RF-34): se cuenta **por correo** (sin distinguir mayúsculas), no por IP ni por cuenta, y se aplica igual si el correo no existe o es solo de Google, para no revelar qué cuentas hay. Por defecto, 3 fallos dentro de 5 minutos bloquean ese correo 5 minutos (el administrador ajusta los tres valores, ver 5.5). Mientras dure el bloqueo no se mira la contraseña: responde 429 aunque sea la correcta. Un fallo viejo (fuera de la ventana) deja de contar. El 401 trae `datos.intentosRestantes`; con 1 el frontend avisa «le queda 1 intento». Un ingreso correcto, o restablecer la contraseña por el enlace del correo, borra el conteo y levanta el bloqueo. El 429 trae `Retry-After` (segundos, entero hacia arriba) y el mensaje dice los minutos que faltan sin lenguaje técnico. La tabla `intento_login` guarda el correo normalizado y las fechas; no guarda contraseñas ni IP.

### 3.3 Lista de categorías — `GET /api/categorias` — EXISTE
JWT de cualquier rol (*propuesto*: mantenerlo con sesión).
Salida: 200, arreglo de las categorías activas (hoy 15): `{ idCategoria, nombre, descripcion, requiereUbicacion, requiereEvidencia }`.
`requiereEvidencia` es `false` en todas: la foto es siempre opcional (decidido el 01/11/2026).
Error: 401.

### 3.4 Crear reporte — `POST /api/reportes` — EXISTE (versión mínima)
JWT. Sigue los 4 pasos del Figma (1 Categoría, 2 Ubicación, 3 Detalles, 4 Confirmación). Las categorías sin ubicación saltan del paso 1 al 3.

Entrada:
- `idCategoria`: obligatoria, existente y activa.
- `latitud`, `longitud`: ambas o ninguna; −90..90 y −180..180. Si la categoría `requiereUbicacion`, se exige GPS o barrio (regla B3 de la migración V7).
- `idBarrio`: opcional; obligatorio si no hay GPS y la categoría exige ubicación; existente.
- `descripcion`: obligatoria, 10–500 caracteres tras recortar espacios.
- `direccionIncidente`: opcional, máx. 300.
- `matriculaCuentaServicio`: opcional, máx. 50; solo categorías de empresas de servicios públicos (trigger B6).
- `radicadoOficial`: opcional, máx. 50.
- `totalFotos`: opcional, 0 a 3 (por defecto 0). Cuántas fotos va a subir el ciudadano después de crear el reporte; sirve para decidir si falta la evidencia que se exige al repetir un problema ya resuelto (ver abajo). Es una declaración: la comprobación real se repite al enviar el reporte.
- `confirmarDuplicado`: opcional, `true`/`false` (por defecto `false`). Con `true` el ciudadano confirma que su reporte es otro problema y se omite el aviso de reporte parecido (no el tope diario ni la evidencia).
- `autorizaEnvio`: obligatorio, `true`. Autoriza enviar el reporte a la entidad a nombre del ciudadano y con sus datos (RN-07). El servidor guarda la fecha-hora en `autorizacion_envio_en`. Es la única autorización: no hay una segunda ni se envía copia al ciudadano (decidido el 01/11/2026). Texto de la casilla del paso 4: «Autorizo a NotiUrba a enviar este reporte a la entidad competente, a mi nombre y con mis datos, ubicación y fotos (con enlace público). La entidad podrá responderme a mi correo. Ver aviso de privacidad».

Salida: 201 con `Location: /api/reportes/NU-000123` y un cuerpo mínimo `{ codigoSeguimiento, estado, creadoEn, vigenteHasta }` leído de la base tras guardar, en estado «Recibido». El código lo asigna el trigger de la base, no Java. El reporte completo se obtiene con 3.6 (NOT-89).
La comuna sale del GPS (PostGIS) o, sin GPS, del barrio (RF-08, RN-02). Se guarda en el reporte (`reporte.id_comuna`, V20) con un trigger que usa `fn_asignar_comuna`, la misma función que decide el rechazo; la base la recalcula si cambian la capa de comunas, la comuna de un barrio o la distancia máxima. Reglas con GPS (NOT-55):
- El punto cae dentro de una comuna (el borde cuenta como dentro): esa comuna.
- Cae dentro de dos o más (solape): la de menor número.
- Cae en un hueco: la comuna más cercana, si está a una distancia menor o igual a `configuracion.comuna_distancia_maxima_m` (50 m por defecto, valor propuesto; `0` desactiva la regla).
- Más lejos que eso de toda comuna: 422 FUERA_DE_COBERTURA y no se guarda nada.
- Las comunas sin polígono no participan. Si **ninguna** tiene polígono, no se puede decidir: no se rechaza y la comuna sale del barrio.
Detalle técnico: la base exige `autorizacion_envio_en <= creado_en`, así que el servidor debe fijar ambas fechas con el mismo instante.
Errores: 400 VALIDACION, 401, 422 FUERA_DE_COBERTURA (se aplica en cuanto `comuna.geom` tenga polígonos; mientras todos estén en NULL el reporte se guarda), 429 LIMITE_DIARIO_REPORTES, 409 POSIBLE_DUPLICADO, 422 EVIDENCIA_REQUERIDA.

Controles contra el abuso (NOT-132, RF-35, RF-36, RF-39, RN-22). Se aplican en este orden, después de validar el cuerpo, y todo el crear va en una transacción que bloquea la fila del ciudadano (dos peticiones a la vez no pueden pasar del tope):
1. **Tope diario por cuenta** (429 LIMITE_DIARIO_REPORTES). Por defecto 2 reportes por cuenta y por día de Colombia (de 00:00 a 24:00, hora de Bogotá). Cuenta los reportes creados ese día por esa cuenta, incluidos los no enviados. `datos { maximo, vuelveEn }`. Mensaje: «Hoy ya enviaste 2 reportes, que es el máximo por día. Podrás enviar otro desde mañana.»
2. **Reporte parecido** (409 POSIBLE_DUPLICADO), solo si hay punto GPS y la categoría exige ubicación, y no viene `confirmarDuplicado: true`. Es parecido el reporte de **cualquier cuenta** (también la propia) con la **misma categoría**, a una distancia menor o igual a la de esa categoría (`distancia_duplicado`, de 5 a 500 m; valores iniciales en 5.5: 5 m en vías y en basuras, 8 m en señalización, 10 m en alumbrado, riesgo sanitario y acueducto, 15 m en riesgo de desastres, gas, energía y aseo; Tuluá es pequeña y en el centro los problemas se repiten muy cerca, por eso las distancias son cortas), creado en los últimos `duplicado_ventana_dias` días (por defecto 30) y que **no esté «Resuelto»**. La búsqueda usa PostGIS (`ST_DWithin` sobre `geography` con una caja previa que aprovecha el índice GiST `ix_reporte_ubicacion`; con 10 000 reportes tarda menos de 100 ms). `datos` lleva solo datos base, nunca la identidad ni los datos de otra persona: `esPropio` (true si alguno es de la misma cuenta), `cantidad` (cuántos parecidos hay), `estadoExistente` (el del más reciente), `creadoEn` (fecha, sin hora), `descripcion` (resumen de hasta 140 caracteres, cortado en palabra) y `codigoExistente` **solo si es propio**. Con `confirmarDuplicado: true` se guarda igual.
3. **Evidencia al repetir un problema resuelto** (422 EVIDENCIA_REQUERIDA, campo `totalFotos`). Si en ese punto, con la misma categoría, la misma distancia hay un reporte de cualquier cuenta que ya está «Resuelto» y que pasó a ese estado dentro de la misma ventana de días, el reporte nuevo debe llevar al menos 1 foto: se exige `totalFotos >= 1` al crear y se vuelve a comprobar con las fotos reales al enviar (`POST /api/reportes/{codigo}/enviar` responde 422 si no hay ninguna). El reporte se marca `requiere_evidencia = true`. `confirmarDuplicado` no sustituye la foto. Mensaje: «En este lugar ya se resolvió un reporte parecido. Si el problema sigue o volvió, agrega una foto de hoy que lo muestre.»
Las tres reglas se aplican al crear; no cambian los reportes que ya existen.

### 3.5 Subir foto — `POST /api/reportes/{codigo}/fotos` — EXISTE
JWT, solo el autor del reporte. `multipart/form-data`, parte `foto`:
- Obligatoria, JPG o PNG. El tipo se decide por los primeros bytes del archivo, no por su extensión ni por el Content-Type que declare quien sube. Sin archivo o vacío: 400.
- Máximo 1 MB (1 048 576 bytes). El límite ya está configurado en el servidor y responde 413 con este formato.
- Hasta 3 fotos por reporte (la base permite `orden` de 1 a 3). Se usa el menor orden libre, así que si se borra una foto su lugar se reutiliza.

Salida: 201 `{ codigoSeguimiento, foto: { orden, url } }`. La foto vive en Cloudinary. La base guarda la URL y el identificador de la foto en Cloudinary (`id_publico_proveedor`, V11), que se usa para borrarla y no sale por la API. Mientras sube, la fila del reporte queda bloqueada: dos subidas a la vez no pueden pasar de 3 fotos. Si la foto sube pero no se alcanza a registrar, se borra de Cloudinary para no dejarla huérfana.
Errores: 400 VALIDACION, 401, 404, 409 FOTOS_MAXIMO, 413, 415, 502.

### 3.6 Consultar un reporte — `GET /api/reportes/{codigo}` — EXISTE
JWT, solo reportes propios. `codigo` con formato `NU-` + 6 o más dígitos (mal formado: 400 VALIDACION, campo `codigo`). Se aceptan minúsculas y espacios alrededor.

Salida 200:
- `codigoSeguimiento`, `estado` (Recibido, Enviado, En trámite o Resuelto).
- `categoria { idCategoria, nombre }`, `descripcion`.
- `latitud`, `longitud` (null si no hay GPS), `comuna` (1–10 o null), `barrio { idBarrio, nombre }` o null.
- `direccionIncidente`, `matriculaCuentaServicio`, `radicadoOficial`.
- `fotos [ { orden, url } ]` (arreglo vacío si no tiene).
- `creadoEn`, `vigenteHasta`.
- `historial [ { estado, observacion, cambiadoEn } ]`.
- `envio { estado, motivo }`: estado del último envío del correo a la entidad (PENDIENTE, ENVIADO o FALLIDO); null si aún no se ha enviado (RF-16). Si falló, `motivo` es un texto fijo para el ciudadano («No pudimos entregar el correo a la entidad.»): el detalle técnico de la base nunca sale por la API.

Errores: 400 VALIDACION, 401, 404 NO_ENCONTRADO. Un reporte que no existe y uno de otra persona dan exactamente la misma respuesta, para no revelar qué códigos existen. `comuna` hoy sale del barrio (los polígonos aún no están cargados) y es null si el reporte no tiene ni barrio ni GPS dentro de una comuna.

## 4. Endpoints complementarios (todos *propuestos*)

Fuera de los 6, pero necesarios para que ninguna pantalla del Figma quede sin datos. Cada uno lleva su estado (EXISTE o FALTA); al 9 de nov de 2026 solo faltan los del panel de mapa y estadísticas (sección 5) y el secreto del webhook de rebotes.

- `GET /api/reportes` — EXISTE. «Mis reportes». Exige sesión. Solo los del ciudadano, los más recientes primero (hasta 100). Filtro opcional `estado` (`Recibido`, `Enviado`, `En trámite`, `Resuelto`, o `RECIBIDO`, `ENVIADO`, `EN_TRAMITE`, `RESUELTO`; otro valor → 400 VALIDACION con `campo` estado). Salida: `[ { codigoSeguimiento, estado, categoria { idCategoria, nombre }, direccionIncidente, barrio { idBarrio, nombre } | null, creadoEn, vigenteHasta, envio { estado, motivo } | null } ]`. El reporte completo está en `GET /api/reportes/{codigo}`.
- `PATCH /api/reportes/{codigo}/actualizacion` — EXISTE. «Actualizar mi reporte». Solo el autor (404 si es de otra persona; 401 sin sesión). Entrada `{ tipo, radicado? }`: `tipo` es `RESPUESTA` («Me respondieron») o `RESUELTO` («Ya se resolvió»); `radicado` es opcional (máx. 50; vacío = no cambia el que ya había). **No cambia el estado oficial**: el aviso queda como una nota en el historial con el mismo estado que ya tenía (a nombre del ciudadano) y el administrador la confirma. Si hay radicado, se guarda en `radicadoOficial`. Devuelve el reporte completo (como `GET /api/reportes/{codigo}`). Errores: 400 VALIDACION (`campo` tipo o radicado), 401, 404.
- `POST /api/reportes/{codigo}/enviar` — EXISTE (PR #18). El frontend lo llama después de subir las fotos (si subió). Responde 202; el envío con Resend (API HTTP) corre en segundo plano (RNF-04) y es idempotente. Así el correo (RF-14) puede llevar el enlace a la foto. Sale un solo correo, a la entidad, con el correo del ciudadano en Reply-To y sin copia para él. El envío es una cola persistente: solo corre si `NOTIURBA_ENVIO_REPORTES_HABILITADO=true` (apagado por defecto). El estado se consulta con `GET /api/reportes/{codigo}/envio`; las categorías sin correo (gas, energía, aseo) responden con su canal oficial en vez de enviar. **Destino de prueba:** si `RESEND_PRUEBA_DESTINO` tiene valor, el correo sale a esa dirección y NO a la entidad real (asunto «[PRUEBA] …»); déjela puesta en pruebas y demos.

Verificación de correo (EXISTEN):
- `POST /api/auth/verificar` — público. Entrada `{ token }`, el del enlace del correo (usa el tipo de token «Verificación de correo»; el enlace vence en 24 h y se guarda solo su hash). Salida 200 `{ token, rol }`: confirma el correo e inicia sesión. Error 400 ENLACE_INVALIDO.
- `POST /api/auth/reenviar-verificacion` — público. Entrada `{ correo }`. Siempre 200 `{ mensaje }`, exista o no la cuenta. Con límite de un reenvío por minuto por cuenta, para cuidar la cuota de Resend. Solo envía si la cuenta tiene contraseña y no está verificada.
- Las cuentas con Google ya llegan verificadas. Las cuentas que ya existen (el administrador sembrado en V4) ya nacieron con el correo verificado, así que siguen ingresando sin cambios. El enlace del correo apunta a una página del frontend (`VERIFICACION_ENLACE`, por defecto `/app/verificar-correo`) que aún no existe.

Aviso de rebotes:
- `POST /api/webhooks/resend` — EXISTE (`ReboteController`, V16), con `RESEND_WEBHOOK_SECRET` puesta en Render (13 nov 2026); sin ella rechaza todo. El detalle del reporte lee el resultado con `GET /api/reportes/{codigo}/envio` (sin endpoint nuevo). Público (sin JWT) pero protegido: se valida la firma del evento con un secreto (`RESEND_WEBHOOK_SECRET`) y se rechaza lo que no esté firmado. Si Resend avisa de un rebote o de una queja, el envío pasa a FALLIDO con su motivo y se avisa al ciudadano (decisión 3). Los nombres exactos de los eventos se confirman en la documentación de Resend al implementar. El identificador de Resend se guarda en `registro_envio_correo.id_envio_proveedor` (V16).

Mensaje y canales — EXISTEN (NOT-130, 12 nov 2026; detalle en `docs/sdd/historico/NOT-130-correo-claro-y-canales/`):
- `POST /api/reportes/vista-previa` — ciudadano con sesión. Entrada: `idCategoria`, `latitud`, `longitud`, `idBarrio`, `descripcion`, `direccionIncidente`, `matriculaCuentaServicio`, `totalFotos` (0 a 3). Salida 200 `{ asunto, texto, secciones: [ { id, etiqueta, valor, paso } ], permiteCorreo, ruta, requiereEvidencia }`; `requiereEvidencia` es `true` si en ese punto se repite un problema ya resuelto (el frontend avisa que hace falta una foto antes de enviar; NOT-132); `paso` es `categoria`, `ubicacion` o `detalles` (a dónde vuelve «Editar»). **No guarda nada ni envía nada.** Aplica las mismas reglas que `POST /api/reportes` y la misma plantilla que el correo, así que el texto es idéntico salvo la hora y los enlaces de las fotos (aún no subidas). «Olor a gas» en la categoría de gas se trata como emergencia (ruta al 164). Errores: 400 VALIDACION con `campo`, 401, 422 FUERA_DE_COBERTURA. La categoría 15 no tiene ruta: 400 con campo `idCategoria` (pendiente del equipo).
- `GET /api/reportes/{codigo}/mensaje` — solo el autor. Salida 200 `{ asunto, texto }`: el mensaje encolado (tal como se envió, también los de formato anterior) o, si aún no hay envío, el que se enviaría con las fotos reales. Para «Copiar mi mensaje» cuando el correo rebota o falla. Errores: 400 con campo `codigo` si está mal escrito, 401, 404 (no existe o es de otra persona).
- `GET /api/enrutamiento/categorias[/{id}]` y `GET /api/reportes/{codigo}/envio` suman a `ruta` los campos `esEmpresa` y `canales: [ { tipo, acceso, finalidad, horario, detalle } ]`, con los canales activos no-correo de la entidad en orden. El frontend usa `permiteCorreo` en el paso 1: si es falso (gas, energía y aseo), muestra los canales y no crea reporte.
- El correo a la entidad pasa a tono formal, sin código NU-…, sin la palabra NotiUrba y sin dependencia. Asunto: «Petición ciudadana – [categoría] – [barrio], Comuna [n]» (spec, Anexo B).

Ya implementados (EXISTEN):
- `GET /api/catalogos/tipos-documento` — público. 200 lista `[ { idTipoDocumento, nombre, codigo } ]` ordenada por id (hoy CC, CE, PPT y PAS).
- `GET /api/catalogos/barrios` — público. 200 lista `[ { idBarrio, idComuna, nombre } ]` ordenada por nombre con las reglas del español (Céspedes antes que Chiminangos), igual en cualquier base. No incluye los barrios marcados como internos (`barrio.interno`, V15): no se pueden elegir al registrarse ni al reportar. El barrio «Administración NotiUrba» ya no existe (V18). V17 carga 133 barrios de la fuente histórica PMGR 2020 (ver `docs/datos/barrios-pmgr-2020.md`). (NOT-55)
- `GET /api/health` — público. 200 `{ estado, baseDeDatos }` ambos «ok»; 503 con «error» si la base no responde (`SELECT 1`).
- `POST /api/auth/google` — público. Entrada `{ credential }` (ID token de Google). Salida 200 `{ token, rol }` si ya hay cuenta (o si se vincula por correo, *propuesto*), o `{ estado: "PERFIL_INCOMPLETO", tokenRegistro, correo, nombres, apellidos }`. Error 401 NO_AUTENTICADO.
- `POST /api/auth/google/completar` — público. Entrada: `tokenRegistro` (dura 15 min), `nombres`, `apellidos`, `idTipoDocumento`, `numeroDocumento`, `idBarrio`, `direccionCorrespondencia`, `telefono` (opcional), `aceptaPrivacidad` (true), `declaraMayorEdad` (true). Salida 200 `{ token, rol }`. Errores 400, 401, 409.
- `POST /api/auth/recuperar` — público. Entrada `{ correo }`. Siempre 200 `{ mensaje }`, exista o no la cuenta. El enlace llega por correo (Resend) y vence en 1 hora; si el envío falla la respuesta no cambia (no se revela si la cuenta existe).
- `GET /api/usuarios/me` — EXISTE. Exige sesión. Devuelve `{ nombres, apellidos, idTipoDocumento, numeroDocumento, correo, telefono, idBarrio, direccionCorrespondencia }` (`telefono` puede ser null). Alimenta «Ajustes» y «Editar mis datos».
- `PUT /api/usuarios/me` — EXISTE. Exige sesión. Entrada: los mismos campos menos `correo` (el correo no se edita aquí); `telefono` opcional, vacío lo borra. Reglas del registro: nombres y apellidos obligatorios, tipo de documento y barrio existentes (400 VALIDACION con `campo`; un barrio interno no se puede elegir, pero se puede conservar), documento de otra cuenta → 409 DOCUMENTO_YA_REGISTRADO (`campo` numeroDocumento). Devuelve el perfil guardado.
- `DELETE /api/usuarios/me/datos` — EXISTE. «Eliminar mis datos» (habeas data, Ley 1581). Exige sesión. Borra todos los reportes del ciudadano con sus fotos, historial y envíos (y lo que subió o apoyó); **la cuenta queda**. 204. Primero se borran las fotos del almacén: si este falla responde 502 ALMACEN_FOTOS_NO_DISPONIBLE y **no se borra nada**, así que se puede reintentar. Los reportes que ya salieron por correo a una entidad no se pueden recuperar desde aquí.
- `DELETE /api/usuarios/me` — EXISTE. «Eliminar mi cuenta». Igual que el anterior y además borra la cuenta, sus enlaces de verificación y su identidad de Google. 204; después su token deja de servir (401). La cuenta de administración no se elimina por aquí (403 SIN_PERMISO).
- `POST /api/auth/restablecer` — público. Entrada `{ token, nuevaContrasena }`. Salida 204. Errores 400 VALIDACION (campo `nuevaContrasena`) o ENLACE_INVALIDO (campo `token`).

## 5. Panel administrativo

Todos bajo `/api/admin/...`, rol ADMIN. Los tres endpoints de estados y vigencia están implementados (sección 5.1); el resto conserva su estado indicado.
- `GET /api/admin/ping` — EXISTE. Marcador que confirma que la ruta exige ADMIN (200; 401 sin token; 403 con rol ciudadano).
- `GET /api/admin/reportes` — EXISTE (RF-19). Rol ADMIN (403 a un ciudadano). Los más recientes primero (hasta 500), con la misma forma que `GET /api/reportes/{codigo}` (incluye `fotos` e `historial`). Filtros opcionales: `estado` (igual que en «Mis reportes»), `idCategoria`, `comuna` (1 a 10), `desde` y `hasta` (fecha `AAAA-MM-DD` de creación, hora de Colombia, ambos inclusive; `desde` > `hasta` → 400). Datos del ciudadano: no se incluyen.
- `GET /api/admin/reportes/{codigo}`.
- `PATCH /api/admin/reportes/{codigo}/estado` - EXISTE. Estado y observación (RF-20); ver 5.1.
- `GET` y `PUT /api/admin/configuracion/vigencia` - EXISTEN (RF-21); ver 5.1.
- `GET` y `PUT /api/admin/configuracion/limites` - EXISTEN (NOT-132, RF-37); ver 5.5.
- `GET /api/admin/mapa` (RF-22) — EXISTE (NOT-129); ver 5.2.
- `GET /api/admin/mapa/calor` (RF-23) — EXISTE (NOT-129); ver 5.3.
- `GET /api/admin/estadisticas` (RF-24) — EXISTE (NOT-129); ver 5.4.
- `PUT /api/admin/categorias/{id}/correo-destino` (RF-25).

### 5.1 Estados y vigencia — IMPLEMENTADOS (RN-19, RN-20, RF-20, RF-21)

Los tres endpoints siguientes exigen JWT con rol ADMIN. Sin sesión: 401
`NO_AUTENTICADO`; con rol ciudadano: 403 `SIN_PERMISO`. Conservan el formato
de error `{ codigo, mensaje, campo }`. Los otros endpoints de la sección 5
mantienen su estado previo; esta entrega no implementa listados ni mapas nuevos.

**PATCH /api/admin/reportes/{codigo}/estado**

Petición:

```json
{ "estado": "EN_TRAMITE", "observacion": "Se revisó la evidencia y se inició el seguimiento interno." }
```

Respuesta 200 (ejemplo):

```json
{
  "codigoSeguimiento": "NU-000123",
  "estado": "EN_TRAMITE",
  "observacion": "Se revisó la evidencia y se inició el seguimiento interno.",
  "cambiadoEn": "2026-11-06T19:00:00-05:00",
  "cambiadoPor": 7,
  "vigenteHasta": "2026-12-01T10:00:00-05:00"
}
```

- `estado` usa los identificadores RECIBIDO, ENVIADO, EN_TRAMITE y RESUELTO;
  se resuelven contra los nombres existentes del catálogo PostgreSQL. Los DTO
  ciudadanos mantienen sus etiquetas actuales (Recibido, Enviado, En trámite, Resuelto).
- `observacion` es opcional y admite hasta 1000 caracteres; se recortan los espacios
  exteriores antes de guardarla. Si falta o solo trae espacios, el historial guarda
  «Estado actualizado por el administrador.» (cambio del 14 nov 2026).
- Se permiten RECIBIDO → EN_TRAMITE/RESUELTO, ENVIADO → EN_TRAMITE/RESUELTO
  y EN_TRAMITE → RESUELTO. RECIBIDO → ENVIADO se reserva a la aceptación de Resend.
- Se puede añadir una observación sin cambiar el estado mientras no esté RESUELTO.
  Si el último asiento tiene el mismo estado, observación normalizada y administrador,
  se devuelve ese asiento sin insertar otro. RESUELTO rechaza toda modificación,
  incluso solicitudes repetidas, con 409.
- El servidor obtiene el administrador del usuario autenticado; no acepta un actor
  ni una fecha suministrados por el cliente. `cambiadoPor` es su ID interno.
- Una transacción bloquea la fila del reporte, inserta el historial y deja que B4
  sincronice el estado actual. No se actualiza `vigente_hasta` ni el registro del correo.
  El bloqueo coincide con el usado por el procesamiento de la aceptación de Resend.
- Errores: 400 VALIDACION por código, estado u observación inválidos; 404
  NO_ENCONTRADO si el reporte no existe; 409 CONFLICTO por retroceso, estado
  terminal o intento manual de pasar a ENVIADO. El código acepta minúsculas y
  espacios exteriores, con la misma normalización de la consulta ciudadana.
- Los reportes vencidos también pueden recibir seguimiento administrativo.

**GET /api/admin/configuracion/vigencia**

Respuesta 200 inicial, leída de `configuracion.clave='vigencia_dias'` (semilla V3):

```json
{ "vigenciaDias": 30 }
```

**PUT /api/admin/configuracion/vigencia**

Petición y respuesta 200:

```json
{ "vigenciaDias": 15 }
```

Se exige un número JSON entero positivo representable por PostgreSQL INTEGER;
se rechazan valores ausentes, null, decimales, cadenas, booleanos, cero y negativos
con 400 VALIDACION (`campo: "vigenciaDias"`). Se guarda también el administrador
y la fecha en las columnas de auditoría existentes de `configuracion`.

Solo afecta a reportes creados posteriormente: B9 calcula y guarda
`vigente_hasta = creado_en + make_interval(days => vigencia_dias)` al insertar.
Los reportes anteriores conservan su fecha. No hay endpoint de vigencia individual.

**Vencimiento y consultas de mapas:** vigente significa `CURRENT_TIMESTAMP < vigente_hasta`;
vencido significa `CURRENT_TIMESTAMP >= vigente_hasta`. Se conserva `v_reporte_mapa`,
que exige ubicación y `vigente_hasta > now()`. Será la fuente común de marcadores
y agregaciones de calor cuando existan esos endpoints. El vencimiento no borra
el reporte, no cambia su estado ni afecta su consulta privada o su disponibilidad
para gestión administrativa. Los cambios de estado y el procesamiento de correo
no recalculan su vigencia. No se agregó ninguna migración.

### 5.2 Mapa del panel — IMPLEMENTADO (NOT-129, RF-22, RN-20)

Rol ADMIN. Sin sesión o con token inválido: 401 `NO_AUTENTICADO`; con rol ciudadano: 403
`SIN_PERMISO`. Errores en el formato `{ codigo, mensaje, campo }`.

**Filtros comunes** del mapa, del mapa de calor y de las estadísticas (todos opcionales, con las
mismas reglas y mensajes que `GET /api/admin/reportes`):

| Parámetro | Valor | Error |
| --- | --- | --- |
| `estado` | Nombre del catálogo («En trámite») o identificador (`EN_TRAMITE`) | 400 `VALIDACION`, campo `estado` |
| `idCategoria` | Número de la categoría. Una que no existe da una lista vacía | 400 si no es un número |
| `comuna` | 1 a 10 | 400 `VALIDACION`, campo `comuna` |
| `desde`, `hasta` | `AAAA-MM-DD`, fecha de creación en hora de Colombia, ambas inclusive | 400 `VALIDACION`, campo `desde` si `desde` > `hasta`; 400 si la fecha está mal escrita |

**GET /api/admin/mapa**

Reportes **vigentes y con ubicación** (el mismo criterio de `v_reporte_mapa`), los más recientes
primero, hasta **1.000**. Ejemplo: `GET /api/admin/mapa?comuna=5&estado=RECIBIDO&desde=2026-11-01`.

Respuesta 200:

```json
{
  "total": 37,
  "sinComuna": 0,
  "truncado": false,
  "reportes": [
    {
      "codigoSeguimiento": "NU-000035",
      "estado": "Recibido",
      "categoria": { "idCategoria": 1, "nombre": "Vías, huecos, andenes e infraestructura" },
      "barrio": { "idBarrio": 12, "nombre": "Salesiano" },
      "comuna": 5,
      "latitud": 4.0867,
      "longitud": -76.195,
      "creadoEn": "2026-11-05T10:07:00-05:00",
      "vigenteHasta": "2026-12-05T10:07:00-05:00"
    }
  ]
}
```

- `total`: cuántos reportes cumplen los filtros. `truncado` es `true` si son más de 1.000; el panel
  puede avisar «mostrando 1.000 de N».
- `sinComuna`: cuántos de ellos no tienen comuna determinada; en ese caso `comuna` es `null`. `barrio`
  también puede ser `null`.
- `estado` usa el nombre del catálogo (Recibido, Enviado, En trámite, Resuelto), igual que el listado.
- No trae datos del ciudadano ni la descripción (Ley 1581): el detalle completo está en
  `GET /api/admin/reportes`.
- Sin coincidencias: 200 con `reportes: []` y `total: 0`.
- Rendimiento medido (12/11/2026, base local, comuna guardada): con 10.000 reportes la consulta tarda
  unos 11 ms y 2 ms filtrando por comuna (índice `ix_reporte_comuna`); el criterio es ≤ 2 s.
- **JIT de PostgreSQL:** cuando la comuna se calculaba en cada lectura, PostGIS declaraba costos tan altos
  que el JIT compilaba estas consultas (1,2 s para 13 reportes) y había que apagarlo. Con la comuna
  guardada el costo estimado es de 1.000 a 2.300 con 10.000 reportes, lejos del umbral (100.000), y ya no
  se apaga. En Neon el JIT no está disponible (`pg_jit_available() = false`).

### 5.3 Mapa de calor y comunas críticas — IMPLEMENTADO (NOT-129, RF-23)

**GET /api/admin/mapa/calor**

Mismos reportes que el mapa (vigentes con ubicación) y mismos **filtros comunes** (5.2). Además:

| Parámetro | Valor | Error |
| --- | --- | --- |
| `pagina` | Página del ranking de comunas críticas; por defecto 1 | 400 `VALIDACION`, campo `pagina`, si es menor que 1 |
| `tamano` | Comunas por página, de 1 a 10; por defecto 5 | 400 `VALIDACION`, campo `tamano` |

Ejemplo: `GET /api/admin/mapa/calor?idCategoria=1&pagina=2`.

Respuesta 200:

```json
{
  "totalReportes": 10,
  "totalPuntos": 8,
  "truncado": false,
  "sinComuna": 0,
  "puntos": [ { "latitud": 4.0867, "longitud": -76.195, "peso": 3 } ],
  "comunasCriticas": {
    "pagina": 1,
    "tamano": 5,
    "totalComunas": 6,
    "totalPaginas": 2,
    "items": [
      { "posicion": 1, "comuna": 5, "total": 4,
        "categoriaPrincipal": { "idCategoria": 1, "nombre": "Vías, huecos, andenes e infraestructura" } }
    ]
  }
}
```

- **Peso:** cada reporte pesa 1 y los reportes en el mismo punto se suman (sirve tal cual para
  Leaflet.heat). Los puntos van de más a menos peso, hasta **1.000**; `totalPuntos` dice cuántos puntos
  distintos hay y `truncado` es `true` si son más de 1.000. `totalReportes` es la suma de todos los pesos.
- **Comunas críticas:** las comunas con al menos un reporte, de más a menos reportes; en empate va
  primero la de menor número. Como hay 10 comunas, con el tamaño por defecto son 2 páginas como máximo.
  `posicion` es el lugar en el ranking completo (en la página 2 empieza en 6).
- **Categoría principal:** la categoría con más reportes en esa comuna; en empate, la de menor número.
- Los reportes sin comuna no entran al ranking: se cuentan en `sinComuna`.
- Una página fuera de rango responde 200 con `items: []` (los puntos y los totales siguen llegando).
- Son dos consultas (puntos y conteo por comuna) en una transacción `REPEATABLE READ`: las dos ven los
  mismos reportes, así que el ranking más `sinComuna` siempre suma `totalReportes`.
- Rendimiento medido (12/11/2026, base local): con 10.000 reportes la consulta de puntos tarda unos 9 ms.

### 5.4 Estadísticas — IMPLEMENTADO (NOT-129, RF-24)

**GET /api/admin/estadisticas**

Cuenta **todos** los reportes (con o sin ubicación, vigentes o vencidos) con los **filtros comunes**
(5.2) y además:

| Parámetro | Valor | Error |
| --- | --- | --- |
| `vigencia` | `vigentes` (dentro del plazo), `fuera_de_plazo` (ya vencidos) o `todos` (por defecto); sin distinguir mayúsculas | 400 `VALIDACION`, campo `vigencia` |

«Vigente» se mide con `vigente_hasta` de cada reporte (hoy, 30 días desde su creación según
`configuracion.vigencia_dias`), igual que en el mapa.

Ejemplo: `GET /api/admin/estadisticas?vigencia=vigentes&comuna=5`.

Respuesta 200:

```json
{
  "vigencia": "todos",
  "total": 12,
  "atendidos": 3,
  "porcentajeAtendidos": 25.0,
  "sinComuna": 0,
  "porEstado": [
    { "estado": "Recibido", "total": 5 },
    { "estado": "Enviado", "total": 2 },
    { "estado": "En trámite", "total": 2 },
    { "estado": "Resuelto", "total": 3 }
  ],
  "porCategoria": [ { "idCategoria": 1, "categoria": "Vías, huecos, andenes e infraestructura", "total": 5 } ],
  "porComuna": [ { "comuna": 1, "total": 2 }, { "comuna": 2, "total": 3 } ]
}
```

- **Atendidos** = reportes en estado Resuelto. `porcentajeAtendidos` = atendidos ÷ total × 100, con un
  decimal (redondeo a la mitad hacia arriba); 0.0 si no hay reportes.
- `porEstado` trae siempre los 4 estados en ese orden; `porComuna`, las 10 comunas en orden; y
  `porCategoria`, todas las categorías activas (más cualquier categoría inactiva que tenga reportes), de más
  a menos reportes y en empate por número. Todas aparecen **con 0** cuando no tienen reportes.
- `sinComuna`: reportes cuya comuna no se pudo determinar; no entran en `porComuna`. Un reporte sin
  ubicación toma la comuna de su barrio.
- Las tres tablas salen de una sola consulta agrupada: siempre cuadran con `total`.
- Con filtro `idCategoria`, `porCategoria` trae solo esa categoría (un 0 de otra categoría sería
  engañoso: significaría «filtrada», no «sin reportes»).
- Rendimiento medido (12/11/2026, base local): con 10.000 reportes la consulta tarda unos 12 ms (31,5 s
  cuando la comuna se calculaba en cada lectura).
- QA de punta a punta contra el servidor real: `tools/qa-panel-admin/qa-panel.sh` (44 verificaciones); las
  cuatro rutas del panel responden en 6 a 11 ms.

### 5.5 Límites contra el abuso — IMPLEMENTADOS (NOT-132, RF-37, RN-23)

`GET` y `PUT /api/admin/configuracion/limites`. JWT con rol ADMIN (401 sin sesión, 403 a un ciudadano). Reúne en una sola pantalla del panel todo lo que el administrador ajusta de los controles de 3.2 y 3.4.

Forma (la misma en la lectura y en la escritura):

    {
      "login": { "intentosMaximos": 3, "ventanaMinutos": 5, "bloqueoMinutos": 5 },
      "reportesMaximosPorDia": 2,
      "duplicados": {
        "ventanaDias": 30,
        "categorias": [ { "idCategoria": 1, "nombre": "Vías, huecos, andenes e infraestructura", "metros": 5 }, … ]
      }
    }

Rangos (los mismos que hace respetar la base con restricciones `CHECK`):

| Campo | Rango | Valor inicial |
| --- | --- | --- |
| `login.intentosMaximos` | 1 a 10 | 3 |
| `login.ventanaMinutos` | 1 a 60 | 5 |
| `login.bloqueoMinutos` | 1 a 60 | 5 |
| `reportesMaximosPorDia` | 1 a 50 | 2 |
| `duplicados.ventanaDias` | 1 a 30 | 30 |
| `duplicados.categorias[].metros` | 5 a 500 | 5 vías (1) y basuras (3); 8 señalización (4); 10 alumbrado (2), riesgo sanitario (5) y acueducto (11); 15 riesgo de desastres (8), gas (12), energía (13) y aseo (14) |

`categorias` lista las 10 categorías que exigen ubicación (las demás no tienen punto que comparar y no se revisan).

`PUT`: **todo o nada**. Se validan todos los campos y, si alguno falla, no se guarda nada. Respuesta 200 con la misma forma, ya guardada. Cada valor guarda quién lo cambió y cuándo (`id_usuario_actualiza`, `actualizado_en`); no hay un historial aparte. Los valores se leen de la base en cada petición, así que un cambio vale de inmediato, sin reiniciar. Errores: 400 VALIDACION con `campo` (`intentosMaximos`, `ventanaMinutos`, `bloqueoMinutos`, `reportesMaximosPorDia`, `ventanaDias`, `categorias.<id>.metros`, o `login`, `duplicados` y `categorias` si falta el bloque; un valor que no es un número entero llega como cuerpo mal formado: VALIDACION con `campo` nulo), 401, 403.

## 6. Decisiones propuestas (no cerradas)

1. **Decidido (01/11/2026):** verificación de correo en el MVP. Quien se registra con correo y contraseña recibe un enlace de confirmación y no ingresa hasta usarlo. Evita que alguien envíe reportes con los datos de otra persona. Cuesta un correo por registro (comparte los 100 diarios con reportes y recuperaciones). Con Google no hace falta: Google ya garantiza el correo.
2. **Decidido (01/11/2026):** el envío del reporte es un paso aparte, `POST /api/reportes/{codigo}/enviar`, después de las fotos. **Implementado** (PR #18, ver sección 4).
3. **Decidido (01/11/2026):** el reporte pasa a «Enviado» cuando Resend acepta el envío (lo pone en cola). Eso no garantiza que el correo llegue: si después rebota (el buzón no existe o bloquea mensajes), Resend avisa por webhook, el envío queda FALLIDO y se notifica al ciudadano. El estado del reporte no retrocede (regla B11): el fallo se ve en `envio.estado = FALLIDO` de `GET /api/reportes/{codigo}`. El ciudadano se entera de dos formas: en la app y por un correo que le avisa del fallo (decidido el 01/11/2026; gasta un correo de la cuota y no importa). Esto vuelve necesario RF-16 (hoy «Condicional») y los webhooks (hoy «deseables», R.05). Qué puede hacer el ciudadano ante un fallo queda por definir.
4. Categorías con sesión: *propuesto* mantener `GET /api/categorias` con JWT.
5. Catálogos como endpoints públicos; el aviso de privacidad es estático en el frontend y el servidor guarda el vigente.
6. **Decidido (01/11/2026):** una sola autorización (`autorizaEnvio`) y el correo no se copia al ciudadano: su correo va en Reply-To y en el cuerpo del mensaje («responder a este correo»). Sale 1 correo por reporte (el límite de 100 al día alcanza para ~100 reportes). Los reportes anónimos siguen fuera del MVP.
7. **Decidido (01/11/2026), reemplaza la decisión anterior de no tener casilla:** el servicio es solo para mayores de 18 años y el registro (y «completar perfil» de Google) exige `declaraMayorEdad=true`, en la misma casilla que acepta el aviso de privacidad. Implementado en el backend (NOT-75): `declaraMayorEdad` es obligatorio en el registro y en «completar perfil» de Google.
8. **Decidido (01/11/2026):** la foto es siempre opcional; ninguna categoría la exige (`requiere_evidencia` queda en `false`). La app la ofrece para mejorar el reporte; la decisión es del ciudadano, porque los canales oficiales no la exigen.
9. **Decidido (01/11/2026):** el correo se envía tras una interfaz propia (`EnviadorCorreo`) con una implementación que llama a la API HTTP de Resend, igual que `EnviadorEnlaceRecuperacion`. Evita el bloqueo de puertos de Render y no suma complejidad al ciudadano. Lo mismo para las fotos: una interfaz y una implementación con Cloudinary. **Implementado el 01/11/2026** (`EnviadorCorreo`, `EnviadorCorreoResend`); se quitó `spring-boot-starter-mail`. Falta conectarlo a la recuperación de contraseña y al endpoint `enviar`.
10. **Decidido (01/11/2026):** aviso de privacidad **versión 3** como el vigente (migración V12; el mismo texto está en `frontend/src/compartido/textos/avisoPrivacidad.js` y una prueba comprueba que coincidan). Responsable: equipo NotiUrba, Universidad del Valle, sede Tuluá; canales: Ajustes y notiurba@gmail.com. Al eliminar la cuenta los datos se borran de inmediato, salvo los correos ya enviados a la entidad. Vigencia del reporte: 30 días. Detalle y puntos por validar con el docente o un asesor jurídico: ADR-0008.
11. **Decidido (07/11/2026):** «Actualizar mi reporte» sigue el Figma y el frontend: dos avisos (`RESPUESTA`, `RESUELTO`) más el radicado opcional, en `PATCH /api/reportes/{codigo}/actualizacion` (reemplaza al `PATCH /api/reportes/{codigo}` que solo guardaba el radicado). El aviso del ciudadano NO cambia el estado oficial: queda como nota en `historial_estado` con el estado vigente; solo el administrador cambia estados (NOT-19). No requiere migración.
12. **Decidido (13/11/2026, NOT-132):** controles contra el abuso, todos ajustables por el administrador (5.5). Login: 3 fallos en 5 minutos bloquean el correo 5 minutos. Reportes: máximo 2 por **cuenta** y por día de Colombia. Parecidos: misma categoría, cerca (5 a 15 m según la categoría), últimos 30 días, de **cualquier cuenta**, sin contar los ya resueltos; el ciudadano puede enviar de todas maneras. Si se repite un problema ya resuelto, la foto pasa a ser obligatoria (no reabre la decisión 8: sigue opcional en el resto de casos). Los textos de los avisos los afina el líder con el equipo de UX.

## 7. Discrepancias y brechas

### Resueltas en la rama `feature/contrato-api`
- **Formato de error único**: no existía. Ahora `ManejadorErrores` (errores de negocio y de validación) y `ManejadorSeguridad` (401 y 403) devuelven `{ codigo, mensaje, campo }`. Se desactivó `problemdetails`.
- **403 que salía como 401**: `sendError` redirigía internamente a `/error`, donde la petición ya no tenía sesión. Verificado con un servidor real: un ciudadano en `/api/admin/ping` recibía 401. Ahora recibe 403. Las pruebas con MockMvc no lo detectaban porque no hacen ese redespacho.
- **`Location` y CORS**: el navegador no podía leerlo entre dominios. Ahora CORS lo expone.
- **Límite de foto**: queda explícito en `application.yml` (1 MB por archivo, 2 MB por petición) y responde 413 ARCHIVO_MUY_GRANDE.
- **`cliente.js`**: ahora lee el cuerpo del error y lanza `ErrorApi { estado, codigo, mensaje, campo }`; no fija `Content-Type` con `FormData`; con 401 NO_AUTENTICADO estando con sesión cierra la sesión y lleva a `/app/ingresar`.
- **`RutaProtegida`** apuntaba a `/login`, que no existía. Ahora apunta a `/app/ingresar`, una pantalla mínima funcional (el diseño final es de Figma).
- **`reportesPrueba.js`** usaba ids `NTU-001`, estados Nuevo/En revision/Asignado/Resuelto, 6 categorías y comuna `'01'`. Ahora usa `NU-000001`, los 4 estados reales, las 15 categorías y comuna numérica 1–10.
- **Modo de envío BORRADOR**: sigue sembrado en V3. La migración V9 lo elimina (si ningún envío lo usa). Aplicar V9 en Neon al desplegar: Render redespliega solo con cada commit a la rama enlazada.
- **Menciones desactualizadas**: «Vercel» en `03-arquitectura-y-datos.md` cambiado a Netlify; `docs/README.md` actualizado.

- **`requiere_evidencia`**: decisión 8: la foto es siempre opcional; no hay cambio en la base.

### Pendientes
- **Postman**: la colección oficial (`docs/api/NotiUrba API`, ver `docs/api/README.md`) cubre login, categorías, crear y consultar reporte (NOT-89) y subir foto (NOT-52). Falta anotar la ejecución en Postman de los casos de consulta (200, 404, 400 y 401); el caso de «otra persona» queda omitido hasta tener `tokenOtraPersona`.
- **Código de seguimiento (NOT-69)**: la tarea hablaba de máximo 10 caracteres, pero la columna es `VARCHAR(20)`. El código mide 9 hasta el reporte 999 999 y 10 hasta el 9 999 999. Lo asigna el trigger `fn_reporte_antes_insertar` y `uq_reporte_codigo` lo hace único; ambos están probados contra PostGIS.
- **Estado «Enviado» y rebotes**: falta implementar el webhook, la columna del identificador de envío y la notificación al ciudadano (decisión 3). Confirmar con el equipo que RF-16 y los webhooks pasan a ser necesarios en el MVP.
- **Verificación de correo**: hecha de punta a punta (backend y página `/app/verificar-correo`, `ConfirmarCorreo.jsx`).
- **Endpoint `enviar`**: hecho (PR #18). Falta encender la cola en Render (`NOTIURBA_ENVIO_REPORTES_HABILITADO=true`, con `RESEND_PRUEBA_DESTINO` puesta) y la prueba de punta a punta con una cuenta real.
- **Aviso de privacidad**: la versión 3 ya es la vigente en la base (V12) y en el frontend; su **validación jurídica sigue pendiente** (ADR-0008). El aviso promete cosas que todavía no existen: Ajustes (editar datos, eliminar datos y cuenta, volver a leer el aviso), «Mis reportes» (`GET /api/reportes`, propuesto en la sección 4), el aviso al ciudadano cuando un correo rebota (webhook), el borrado de sus fotos en Cloudinary al eliminar sus datos y volver a aceptar el aviso si sube de versión. Hay que implementarlas antes de abrir el servicio al público.
- **Paso 4 del Figma**: si muestra dos casillas de autorización, debe quedar una sola (`autorizaEnvio`). Avisar a Amelia (diseño).
- **La capa de comunas es experimental y los barrios son de una fuente histórica provisional.** V14 instala la capa sintética y no oficial de NOT-31 en `comuna.geom` (la fuente queda en `configuracion.comunas_capa_fuente`); la regla de la comuna y `FUERA_DE_COBERTURA` (NOT-55, V13) ya actúan con ella. Para cambiarla por la oficial basta regenerar la migración con `backend/scripts/generar-capa-comunas.py` (migración V19 o la que siga). V17 carga 133 barrios del PMGR 2020 (fuente municipal histórica; no acredita vigencia en 2026 y sus 5 nombres dudosos B01-B05 los resolvió Andrés el 2026-11-08, ver `docs/datos/barrios-pmgr-2020.md`).
- **Código sin escribir**: conectar `EnviadorCorreo` a la recuperación de contraseña y el borrado de fotos y datos a pedido del ciudadano (la columna del identificador ya lo permite). El almacén de fotos (NOT-51) y el endpoint de subida (NOT-52) están hechos; se probaron en producción el 01/11/2026: una foto subida desde Render a Cloudinary, con su URL pública cargando sin sesión (las migraciones V10 y V11 ya estaban aplicadas en Neon; V11 se comprobó porque la subida guarda el identificador en esa columna). En Render también se verificó el rechazo de un archivo de 1,5 MB (413) y de un `.jpg` que no es imagen (415).
- **Prueba de Resend desde Render**: el 01/11/2026 se probó la API HTTP desde la máquina de Andrés (respuesta 200 con el subdominio verificado, enviando a una dirección de pruebas de Resend). El 01/11/2026 también se envió un correo real con `EnviadorCorreoResend` a una bandeja de Andrés. Falta probarlo desde Render: hay que desplegar y poner `RESEND_API_KEY` y `RESEND_REMITENTE` en sus variables. El SMTP por 2587 no se usa.
- **Rutas del frontend**: `RutaProtegida rol="ADMIN"` ya envuelve `/admin/*`; el mapa por comunas sigue con datos de prueba hasta NOT-135.
- **Linear**: NOT-69 («Generar el código único, guardarlo con restricción de unicidad y devolverlo al crear el reporte») ya lo hace el trigger `fn_reporte_antes_insertar` más `uq_reporte_codigo`: queda solo devolverlo en el 201. NOT-89 («consultar un reporte propio por código, con URL de foto nula si no tiene y 404 si no es suyo») habla de una sola foto: ahora son hasta 3, así que debe decir «lista de fotos vacía si no tiene».

## 8. Cambios al modelo de datos (para actualizar el diagrama ER)

Este contrato pide cambios mínimos a la base. Las migraciones ya aplicadas no se editan. Ya hechos: la V9 quitó el modo de envío BORRADOR (solo borró una fila del catálogo `modo_envio`) y la V10 agregó las dos columnas de `registro_envio_correo` de la tabla de abajo y la V11 agregó `evidencia_reporte.id_publico_proveedor`.

Columnas nuevas:

| Tabla | Columna | Tipo | Para qué |
| --- | --- | --- | --- |
| `registro_envio_correo` | `id_envio_proveedor` | VARCHAR(100), nulo, índice único | Guarda el identificador que Resend da a cada envío. El webhook lo usa para saber a qué envío pertenece un rebote |
| `registro_envio_correo` | `aviso_fallo_en` | TIMESTAMPTZ, nula | Fecha en que se avisó al ciudadano del fallo. Evita avisar dos veces si Resend repite el evento |
| `evidencia_reporte` | `id_publico_proveedor` | VARCHAR(255), nulo | **Hecho en la V11.** Identificador de la foto en Cloudinary, para poder borrarla si el ciudadano pide suprimir sus datos (el ADR-0006 dice que se guarda «la URL y el identificador») |

Sin columnas nuevas (ya existen y alcanzan):
- Verificación de correo: `usuario.correo_verificado_en` y la tabla `token_verificacion` con el tipo «Verificación de correo». El límite de un reenvío por minuto se calcula con `token_verificacion.creado_en`.
- Estado del correo a la entidad: `estado_envio` ya tiene PENDIENTE, ENVIADO y FALLIDO, y las reglas B5 y B10 de `registro_envio_correo`. Un envío ENVIADO que luego rebota pasa a FALLIDO guardando el motivo en `error`.
- Autorización de envío: `reporte.autorizacion_envio_en` (hay una sola autorización).
- Foto opcional: `categoria.requiere_evidencia` queda en `false` en todas.

Datos (no columnas) de la misma migración: marcar `correo_verificado_en` en las cuentas que ya existen (como el administrador sembrado), para que puedan seguir ingresando cuando el login exija correo verificado.

### 8.1 Migración V22 (NOT-132): límites y duplicados

Aditiva: no borra ni cambia datos existentes.

| Qué | Detalle |
| --- | --- |
| 5 claves nuevas en `configuracion` | `login_intentos_maximos` (3), `login_ventana_minutos` (5), `login_bloqueo_minutos` (5), `reportes_maximos_por_dia` (2), `duplicado_ventana_dias` (30). Cada una con su `CHECK` de rango (sección 5.5) |
| Tabla `distancia_duplicado` | `id_categoria` (PK, FK a `categoria`), `metros` (SMALLINT, 5 a 500), `id_usuario_actualiza`, `actualizado_en`. Una fila por categoría que exige ubicación (10 filas) |
| Tabla `intento_login` | `correo_normalizado` (PK), `fallidos`, `primer_fallo_en`, `bloqueado_hasta`, `actualizado_en`. Sin contraseñas ni IP; no depende de que el correo exista |
| Columna `reporte.requiere_evidencia` | BOOLEAN NOT NULL, por defecto `false`. La pone el sistema cuando el reporte repite un problema ya resuelto |
| Índice `ix_reporte_usuario_creado` | sobre `reporte (id_usuario, creado_en)`, para contar los reportes de una cuenta en el día |

Nota: `categoria.requiere_evidencia` (la decisión 8, foto siempre opcional) **no cambia**. La obligación de foto de NOT-132 es otra cosa: depende del lugar y de si se repite un problema resuelto, no de la categoría, y vive en `reporte.requiere_evidencia`.
