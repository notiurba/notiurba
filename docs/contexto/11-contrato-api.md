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
- Rutas: públicas `/api/health` y `/api/auth/**` (y `/api/catalogos/**` cuando exista); ciudadano `/api/...` (exige sesión, cualquier rol); panel `/api/admin/...` (exige rol ADMIN, Sprint 3).
- Todo endpoint protegido responde 401 sin token, con token inválido o vencido (RNF-03), y 403 si el rol no alcanza.
- CORS: solo el origen configurado (`CORS_ORIGINS`). El encabezado `Location` está expuesto para que el navegador lo pueda leer.

## 2. Formato de error único (EXISTE)

Toda respuesta de error tiene el mismo cuerpo:

    { "codigo": "VALIDACION", "mensaje": "Completa el campo «Correo».", "campo": "correo" }

- `codigo`: texto estable que usa el frontend para decidir qué hacer. No cambia.
- `mensaje`: en español, listo para mostrar al ciudadano. Nunca trae detalles internos.
- `campo`: nombre del campo con el problema, o `null`.
- Si fallan varios campos se devuelve el primero en el orden del formulario (el orden de los campos de la petición).

| HTTP | codigo | campo | Cuándo |
| --- | --- | --- | --- |
| 400 | VALIDACION | el campo | Dato faltante, mal formado o fuera de regla; también cuerpo que no es JSON (campo null) |
| 400 | ENLACE_INVALIDO | token | Enlace de recuperación no válido o vencido |
| 401 | NO_AUTENTICADO | null | Sin token, inválido o vencido; también token de Google o de registro con Google inválido |
| 401 | CREDENCIALES_INVALIDAS | null | Login fallido. No dice cuál dato falló |
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
| 422 | FUERA_DE_COBERTURA | latitud | El punto no cae en ninguna de las 10 comunas |
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
- `nombres`, `apellidos`: obligatorios, 1–255, no solo espacios.
- `idTipoDocumento`: obligatorio, existente (1 CC, 2 CE, 3 PPT, 4 PAS).
- `numeroDocumento`: obligatorio, máx. 30; único junto con el tipo.
- `idBarrio`: obligatorio, existente.
- `direccionCorrespondencia`: obligatoria, máx. 300.
- `correo`: obligatorio, formato de correo, máx. 254, único sin distinguir mayúsculas.
- `telefono`: opcional, máx. 30.
- `password`: obligatoria, mínimo 8 caracteres con al menos una letra y un número, máx. 72 bytes UTF-8 (regla real en `ReglaContrasena`). Mensaje: «La contraseña debe tener al menos 8 caracteres, con letras y números».
- `aceptaPrivacidad`: obligatorio, `true`. El servidor guarda el aviso vigente y la fecha-hora, igual que `POST /api/auth/google/completar`.
- «Confirmar contraseña» se valida solo en el frontend.

Salida: 201 `{ correo, mensaje }` **sin token**: la cuenta se crea, se envía un correo con el enlace de confirmación y el ciudadano no puede ingresar hasta confirmarlo (decisión 1, sección 6). No hay casilla de mayoría de edad (decisión 7).
Errores: 400 VALIDACION, 409 CORREO_YA_REGISTRADO, 409 DOCUMENTO_YA_REGISTRADO.

### 3.2 Login — `POST /api/auth/login` — EXISTE
Público. Entrada: `correo`, `password` (obligatorios).
Salida: 200 `{ token, rol }` con rol CIUDADANO o ADMIN.
Errores: 400 VALIDACION; 401 CREDENCIALES_INVALIDAS (también para cuentas creadas solo con Google, que no tienen contraseña); 403 CORREO_NO_VERIFICADO (solo se dice después de acertar la contraseña, para no revelar qué cuentas existen).

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
- `autorizaEnvio`: obligatorio, `true`. Autoriza enviar el reporte a la entidad a nombre del ciudadano y con sus datos (RN-07). El servidor guarda la fecha-hora en `autorizacion_envio_en`. No hay segunda autorización: el correo no se copia al ciudadano (decidido el 01/11/2026).

Salida: 201 con `Location: /api/reportes/NU-000123` y un cuerpo mínimo `{ codigoSeguimiento, estado, creadoEn, vigenteHasta }` leído de la base tras guardar, en estado «Recibido». El código lo asigna el trigger de la base, no Java. El reporte completo se obtiene con 3.6 (NOT-89).
La comuna sale del GPS (PostGIS) o, sin GPS, del barrio (RF-08, RN-02).
Detalle técnico: la base exige `autorizacion_envio_en <= creado_en`, así que el servidor debe fijar ambas fechas con el mismo instante.
Errores: 400 VALIDACION, 401, 422 FUERA_DE_COBERTURA (el 422 aún no se aplica: depende de cargar `comuna.geom` y los barrios, NOT-31 / NOT-54).

### 3.5 Subir foto — `POST /api/reportes/{codigo}/fotos` — FALTA
JWT, solo el autor del reporte. `multipart/form-data`, parte `foto`:
- Obligatoria, JPG o PNG (se valida el tipo real del archivo, no solo la extensión).
- Máximo 1 MB (1 048 576 bytes). El límite ya está configurado en el servidor y responde 413 con este formato.
- Hasta 3 fotos por reporte (la base permite `orden` de 1 a 3).

Salida: 201 `{ codigoSeguimiento, foto: { orden, url } }`. La foto vive en Cloudinary y la base guarda solo la URL.
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

Fuera de los 6, pero necesarios para que ninguna pantalla del Figma quede sin datos. Todos FALTA.

- `GET /api/reportes` — «Mis reportes». Lista propia, más recientes primero, filtro opcional `estado`. Salida: `[ { codigoSeguimiento, estado, categoria, creadoEn, vigenteHasta } ]`.
- `PATCH /api/reportes/{codigo}` — «Actualizar mi reporte». Solo cambia `radicadoOficial` (máx. 50). Devuelve el reporte.
- `POST /api/reportes/{codigo}/enviar` — el frontend lo llama después de subir las fotos (si subió). Responde 202; el envío con Resend (API HTTP) corre en segundo plano (RNF-04) y es idempotente. Así el correo (RF-14) puede llevar el enlace a la foto. Sale un solo correo, a la entidad, con el correo del ciudadano en Reply-To y sin copia para él.
- `GET /api/catalogos/barrios` (`{ idBarrio, idComuna, nombre }`) y `GET /api/catalogos/tipos-documento` — públicos. Al implementarlos hay que agregar `/api/catalogos/**` a las rutas públicas de `SecurityConfig`.

Verificación de correo (EXISTEN):
- `POST /api/auth/verificar` — público. Entrada `{ token }`, el del enlace del correo (usa el tipo de token «Verificación de correo»; el enlace vence en 24 h y se guarda solo su hash). Salida 200 `{ token, rol }`: confirma el correo e inicia sesión. Error 400 ENLACE_INVALIDO.
- `POST /api/auth/reenviar-verificacion` — público. Entrada `{ correo }`. Siempre 200 `{ mensaje }`, exista o no la cuenta. Con límite de un reenvío por minuto por cuenta, para cuidar la cuota de Resend. Solo envía si la cuenta tiene contraseña y no está verificada.
- Las cuentas con Google ya llegan verificadas. Las cuentas que ya existen (el administrador sembrado en V4) ya nacieron con el correo verificado, así que siguen ingresando sin cambios. El enlace del correo apunta a una página del frontend (`VERIFICACION_ENLACE`, por defecto `/app/verificar-correo`) que aún no existe.

Aviso de rebotes:
- `POST /api/webhooks/resend` — FALTA, público (sin JWT) pero protegido: se valida la firma del evento con un secreto (`RESEND_WEBHOOK_SECRET`) y se rechaza lo que no esté firmado. Si Resend avisa de un rebote o de una queja, el envío pasa a FALLIDO con su motivo y se avisa al ciudadano (decisión 3). Los nombres exactos de los eventos se confirman en la documentación de Resend al implementar. Requiere guardar el identificador que Resend da a cada envío: hoy `registro_envio_correo` no tiene columna para eso (migración nueva).

Ya implementados (EXISTEN):
- `GET /api/health` — público. 200 `{ estado, baseDeDatos }` ambos «ok»; 503 con «error» si la base no responde (`SELECT 1`).
- `POST /api/auth/google` — público. Entrada `{ credential }` (ID token de Google). Salida 200 `{ token, rol }` si ya hay cuenta (o si se vincula por correo, *propuesto*), o `{ estado: "PERFIL_INCOMPLETO", tokenRegistro, correo, nombres, apellidos }`. Error 401 NO_AUTENTICADO.
- `POST /api/auth/google/completar` — público. Entrada: `tokenRegistro` (dura 15 min), `nombres`, `apellidos`, `idTipoDocumento`, `numeroDocumento`, `idBarrio`, `direccionCorrespondencia`, `telefono` (opcional), `aceptaPrivacidad` (true). Salida 200 `{ token, rol }`. Errores 400, 401, 409.
- `POST /api/auth/recuperar` — público. Entrada `{ correo }`. Siempre 200 `{ mensaje }`, exista o no la cuenta.
- `POST /api/auth/restablecer` — público. Entrada `{ token, nuevaContrasena }`. Salida 204. Errores 400 VALIDACION (campo `nuevaContrasena`) o ENLACE_INVALIDO (campo `token`).

## 5. Panel administrativo (Sprint 3, solo reservados)

Todos bajo `/api/admin/...`, rol ADMIN. Todos «Sprint 3», salvo el primero.
- `GET /api/admin/ping` — EXISTE. Marcador que confirma que la ruta exige ADMIN (200; 401 sin token; 403 con rol ciudadano).
- `GET /api/admin/reportes` — filtros comuna, categoría, estado y fecha (RF-19).
- `GET /api/admin/reportes/{codigo}`.
- `PATCH /api/admin/reportes/{codigo}/estado` — estado + observación (RF-20). El estado no retrocede (regla B11 de la base).
- `GET` y `PUT /api/admin/configuracion/vigencia` (RF-21).
- `GET /api/admin/mapa` y `GET /api/admin/mapa/calor` (RF-22, RF-23).
- `GET /api/admin/estadisticas` (RF-24).
- `PUT /api/admin/categorias/{id}/correo-destino` (RF-25).

## 6. Decisiones propuestas (no cerradas)

1. **Decidido (01/11/2026):** verificación de correo en el MVP. Quien se registra con correo y contraseña recibe un enlace de confirmación y no ingresa hasta usarlo. Evita que alguien envíe reportes con los datos de otra persona. Cuesta un correo por registro (comparte los 100 diarios con reportes y recuperaciones). Con Google no hace falta: Google ya garantiza el correo.
2. **Decidido (01/11/2026):** el envío del reporte es un paso aparte, `POST /api/reportes/{codigo}/enviar`, después de las fotos. **Queda pendiente de implementar**: depende de que exista crear reporte (`POST /api/reportes`).
3. **Decidido (01/11/2026):** el reporte pasa a «Enviado» cuando Resend acepta el envío (lo pone en cola). Eso no garantiza que el correo llegue: si después rebota (el buzón no existe o bloquea mensajes), Resend avisa por webhook, el envío queda FALLIDO y se notifica al ciudadano. El estado del reporte no retrocede (regla B11): el fallo se ve en `envio.estado = FALLIDO` de `GET /api/reportes/{codigo}`. El ciudadano se entera de dos formas: en la app y por un correo que le avisa del fallo (decidido el 01/11/2026; gasta un correo de la cuota y no importa). Esto vuelve necesario RF-16 (hoy «Condicional») y los webhooks (hoy «deseables», R.05). Qué puede hacer el ciudadano ante un fallo queda por definir.
4. Categorías con sesión: *propuesto* mantener `GET /api/categorias` con JWT.
5. Catálogos como endpoints públicos; el aviso de privacidad es estático en el frontend y el servidor guarda el vigente.
6. **Decidido (01/11/2026):** una sola autorización (`autorizaEnvio`) y el correo no se copia al ciudadano. Sale 1 correo por reporte (el límite de 100 al día alcanza para ~100 reportes). Los reportes anónimos siguen fuera del MVP.
7. **Decidido (01/11/2026):** no hay casilla `declaraMayorEdad`. La base no acepta tarjeta de identidad, y con cédula de ciudadanía el usuario es mayor de edad. Para pasaporte, PPT y cédula de extranjería (que podrían ser de menores) basta una frase en el aviso de privacidad, cuyo texto legal se corrige en Figma (pendiente).
8. **Decidido (01/11/2026):** la foto es siempre opcional; ninguna categoría la exige (`requiere_evidencia` queda en `false`). La app la ofrece para mejorar el reporte; la decisión es del ciudadano, porque los canales oficiales no la exigen.
9. **Decidido (01/11/2026):** el correo se envía tras una interfaz propia (`EnviadorCorreo`) con una implementación que llama a la API HTTP de Resend, igual que `EnviadorEnlaceRecuperacion`. Evita el bloqueo de puertos de Render y no suma complejidad al ciudadano. Lo mismo para las fotos: una interfaz y una implementación con Cloudinary. **Implementado el 01/11/2026** (`EnviadorCorreo`, `EnviadorCorreoResend`); se quitó `spring-boot-starter-mail`. Falta conectarlo a la recuperación de contraseña y al endpoint `enviar`.

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

- **`autorizaCopia`**: se elimina (decisión 6): una sola autorización y sin copia al ciudadano.
- **`requiere_evidencia`**: decisión 8: la foto es siempre opcional; no hay cambio en la base.

### Pendientes
- **Código de seguimiento (NOT-69)**: la tarea hablaba de máximo 10 caracteres, pero la columna es `VARCHAR(20)`. El código mide 9 hasta el reporte 999 999 y 10 hasta el 9 999 999. Lo asigna el trigger `fn_reporte_antes_insertar` y `uq_reporte_codigo` lo hace único; ambos están probados contra PostGIS.
- **Estado «Enviado» y rebotes**: falta implementar el webhook, la columna del identificador de envío y la notificación al ciudadano (decisión 3). Confirmar con el equipo que RF-16 y los webhooks pasan a ser necesarios en el MVP.
- **Verificación de correo**: el backend está hecho; falta la página del frontend `/app/verificar-correo` y la de «reenviar enlace».
- **Endpoint `enviar`**: pendiente (decisión 2).
- **Aviso de privacidad**: el texto legal en Figma y en la base (V3 tiene un marcador) sigue pendiente; incluir la frase de mayoría de edad.
- **Paso 4 del Figma**: si muestra dos casillas de autorización, debe quedar una sola (`autorizaEnvio`). Avisar a Amelia (diseño).
- **`comuna.geom` es NULL y los barrios reales no están cargados** (solo hay un barrio administrativo de V4): `FUERA_DE_COBERTURA` no se puede verificar y nadie puede completar el perfil hasta NOT-31 / NOT-54. Depende de la carga de barrios y polígonos (Amelia).
- **Código sin escribir**: la subida a Cloudinary (falta la interfaz, la dependencia y las variables `CLOUDINARY_*`), el endpoint `enviar` y conectar `EnviadorCorreo` a la recuperación de contraseña.
- **Prueba de Resend desde Render**: el 01/11/2026 se probó la API HTTP desde la máquina de Andrés (respuesta 200 con el subdominio verificado, enviando a una dirección de pruebas de Resend). El 01/11/2026 también se envió un correo real con `EnviadorCorreoResend` a una bandeja de Andrés. Falta probarlo desde Render: hay que desplegar y poner `RESEND_API_KEY` y `RESEND_REMITENTE` en sus variables. El SMTP por 2587 no se usa.
- **Rutas del frontend**: `RutaProtegida` aún no envuelve `/admin/*`; el panel sigue abierto con datos de prueba.
- **Linear**: NOT-69 («Generar el código único, guardarlo con restricción de unicidad y devolverlo al crear el reporte») ya lo hace el trigger `fn_reporte_antes_insertar` más `uq_reporte_codigo`: queda solo devolverlo en el 201. NOT-89 («consultar un reporte propio por código, con URL de foto nula si no tiene y 404 si no es suyo») habla de una sola foto: ahora son hasta 3, así que debe decir «lista de fotos vacía si no tiene».

## 8. Cambios al modelo de datos (para actualizar el diagrama ER)

Este contrato pide cambios mínimos a la base. Las migraciones ya aplicadas no se editan. Ya hechos: la V9 quitó el modo de envío BORRADOR (solo borró una fila del catálogo `modo_envio`) y la V10 agregó las dos columnas de `registro_envio_correo` de la tabla de abajo (falta solo la propuesta de `evidencia_reporte`).

Columnas nuevas:

| Tabla | Columna | Tipo | Para qué |
| --- | --- | --- | --- |
| `registro_envio_correo` | `id_envio_proveedor` | VARCHAR(100), nulo, índice único | Guarda el identificador que Resend da a cada envío. El webhook lo usa para saber a qué envío pertenece un rebote |
| `registro_envio_correo` | `aviso_fallo_en` | TIMESTAMPTZ, nula | Fecha en que se avisó al ciudadano del fallo. Evita avisar dos veces si Resend repite el evento |
| `evidencia_reporte` | `id_publico_proveedor` | VARCHAR(255), nulo | *Propuesta, por decidir.* Identificador de la foto en Cloudinary, para poder borrarla si el ciudadano pide suprimir sus datos. El ADR-0006 dice que se guarda «la URL y el identificador», pero hoy la tabla solo tiene `url` |

Sin columnas nuevas (ya existen y alcanzan):
- Verificación de correo: `usuario.correo_verificado_en` y la tabla `token_verificacion` con el tipo «Verificación de correo». El límite de un reenvío por minuto se calcula con `token_verificacion.creado_en`.
- Estado del correo a la entidad: `estado_envio` ya tiene PENDIENTE, ENVIADO y FALLIDO, y las reglas B5 y B10 de `registro_envio_correo`. Un envío ENVIADO que luego rebota pasa a FALLIDO guardando el motivo en `error`.
- Autorización de envío: `reporte.autorizacion_envio_en` (hay una sola autorización).
- Foto opcional: `categoria.requiere_evidencia` queda en `false` en todas.

Datos (no columnas) de la misma migración: marcar `correo_verificado_en` en las cuentas que ya existen (como el administrador sembrado), para que puedan seguir ingresando cuando el login exija correo verificado.
