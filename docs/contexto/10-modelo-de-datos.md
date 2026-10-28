# NotiUrba · Modelo de datos v8: contexto y razones de diseño

> Para una IA o una persona que deba trabajar con la base de datos (diccionario de datos, backend, pruebas). Explica **qué** hay y **por qué**. El esquema real está en `backend/src/main/resources/db/migration/` (V1 esquema, V2 reglas y vistas, V3 catálogos, V4 admin de desarrollo, V5 login con Google, V6 nombres y apellidos, V7 ajustes v8). Referencia viva con ejemplos: Google Sheet «Tablas Diagrama ER en excel», pestañas `v5` y `Reglas v5`.

## 1. Qué es NotiUrba (en una línea)
Un ciudadano de Tuluá crea un reporte (categoría, descripción, foto y ubicación GPS opcionales según la categoría). El sistema lo asocia a una **comuna** (1 a 10) y lo hace llegar **por correo** a la entidad competente: la Alcaldía (siempre vía el CAIC) o la empresa de servicios públicos. NotiUrba **no radica** el trámite ni conoce su estado oficial: un administrador actualiza el estado a mano.

## 2. Reglas de diseño (pedidas por el autor)
- **3FN**: una tabla por concepto; nada repetido.
- **Todo valor tipo lista es una tabla** (rol, tipo de documento, estados, tipos de canal…), no un texto ni un enum. Así se administran sin cambiar el esquema.
- **Claves** `id_<tabla>` tanto en PK como en FK, para que cada relación se lea sola.
- **Nombres y apellidos en dos campos** (`nombres`, `apellidos`, desde v7): como en el Figma. Reemplazan a los cuatro campos fragmentados de v5 (V6 los migró y los borró).
- **Longitudes generosas**: es un servicio público con datos heterogéneos; las reglas estrictas viven en CHECK y en el backend, no en el tamaño de la columna (p. ej. la descripción mide 10 a 500 por CHECK).
- **Comunas, no cuadrantes**: los cuadrantes de la Policía quedaron obsoletos por decisión aprobada.

## 3. Tablas y por qué existen (26)
**Catálogos (11)**: `rol` (CIUDADANO, ADMIN), `tipo_documento` (CC, CE, PPT, PAS; sin TI), `aviso_privacidad` (versionado: se guarda qué versión aceptó cada usuario), `tipo_token`, `tipo_entidad`, `tipo_dependencia`, `tipo_canal`, `estado_reporte` (con `orden`), `modo_envio`, `estado_envio`, `proveedor_identidad` (GOOGLE).

**Territorio**
- `comuna`: id 1 a 10 con `geom` (MultiPolygon, EPSG:4326). El polígono llega después; mientras tanto la comuna se infiere por el barrio.
- `barrio`: pertenece a una comuna. La lista se carga aparte.
- No hay tabla `ciudad`: todo es Tuluá (se agrega con ALTER si hace falta).

**Personas**
- `usuario`: `nombres` y `apellidos` (no vacíos), documento (tipo + número únicos), barrio y dirección de correspondencia (la ley exige estos datos en el correo a la entidad), correo único sin distinguir mayúsculas, `correo_verificado_en` (fuente de verdad de la verificación), aviso de privacidad aceptado y cuándo. `password_hash` es opcional desde V5: las cuentas que solo entran con Google no tienen contraseña (regla B13).
- `identidad_externa`: la cuenta de un proveedor externo vinculada a un usuario; guarda el `sub` del ID token de Google en `identificador_externo`. Única por (proveedor, identificador) y por (usuario, proveedor): una cuenta de Google es de un solo usuario y un usuario tiene a lo sumo una de cada proveedor. Se borra en cascada con el usuario (habeas data).
- `proveedor_identidad`: catálogo de proveedores (hoy solo GOOGLE). Es una tabla y no un texto, por la regla de diseño.
- `token_verificacion`: tokens de un solo uso (verificar correo, recuperar contraseña); guarda el hash SHA-256, no el token. «Recuperar contraseña» lo reutiliza: vence en 1 hora y un enlace nuevo invalida los anteriores.
- `configuracion`: pares clave-valor editables por el admin; hoy `vigencia_dias` = 30.

**Destinos (a quién llega un reporte)**
- `entidad_destino`: Alcaldía de Tuluá y las empresas (Centroaguas, Gases de Occidente, Celsia, Veolia).
- `dependencia`: secretarías, departamentos, oficinas, áreas o servicios públicos; pertenecen a una entidad y pueden tener padre (`id_dependencia_padre`).
- `canal_atencion`: correo, web, teléfono, WhatsApp o presencial. Pertenece a **una entidad o a una dependencia** (nunca ambas ni ninguna).
- `categoria`: lo que el ciudadano elige; apunta a **una** dependencia. `descripcion` guarda los ejemplos que muestra la tarjeta de la app («Huecos, grietas, andenes rotos»); se editan en la base, la app no los tiene escritos (V7). Cadena: **categoría → dependencia → entidad**. Por eso un reporte tiene un solo destino.
- Banderas `requiere_ubicacion` y `requiere_evidencia` en la categoría: el barrio y el GPS son opcionales según el tipo de incidente.

**Reportes**
- `reporte`: código de seguimiento `NU-000001`, autor, categoría, barrio y ubicación (opcionales), estado actual, descripción, dirección del incidente, matrícula del servicio (solo empresas), vigencia, fecha de autorización de envío y `radicado_oficial` (opcional, sin UNIQUE: el número que asigna la Alcaldía y que el ciudadano escribe en «Actualizar mi reporte»; no reemplaza al `NU-000000`, que es de NotiUrba). El GPS ubica el punto dentro de una comuna; el barrio lo elige la persona y complementa la ubicación.
- `evidencia_reporte`: fotos como **URL** (no archivos). Cada foto pertenece al usuario que la subió; máximo **3 por usuario y reporte**. Pueden subir quien reportó o quien apoyó.
- `apoyo_reporte`: otros ciudadanos que respaldan un reporte (un apoyo por usuario).
- `historial_estado`: cada cambio de estado, quién lo hizo y cuándo. Solo se inserta; el estado no retrocede (puede saltar etapas).
- `registro_envio_correo`: cada intento de envío (canal usado, modo RESEND o BORRADOR, estado PENDIENTE/ENVIADO/FALLIDO, error).

## 4. Decisiones clave y su razón
- **Un solo destino por reporte**: simplifica el envío y evita duplicados.
- **Solo se envía al Correo principal activo de la entidad** (Alcaldía = CAIC, que redirige). Los correos de cada secretaría son solo de referencia. Un índice único parcial impide dos correos principales por entidad.
- **Entidad sin correo principal activo** (hoy Gases de Occidente, Celsia, Veolia): el reporte queda «no enviado por correo» (vista `v_reporte_no_enviado_por_correo`) y la app muestra los demás canales.
- **El acceso de un canal no se edita**: se desactiva y se crea otro, para no reescribir el destino de envíos pasados.
- **La comuna no se guarda en el reporte**: se deriva (vista `v_reporte_comuna`) del polígono que contiene el punto (`ST_Covers`) o, si no hay, del barrio. Evita datos contradictorios al actualizar polígonos.
- **Habeas data**: borrar un usuario borra en cascada sus reportes y todo lo que cuelga (evidencia, apoyos, historial, envíos, tokens). Los autores de auditoría (`id_usuario_actualiza`, `id_usuario_cambia`) pasan a NULL. El backend debe borrar además los archivos de las fotos del almacenamiento.
- **Fotos por usuario**: permite que los apoyantes aporten evidencia sin pisar la del autor.
- **Estado y vigencia**: ver redundancias.

## 5. Redundancias controladas (adrede)
- `reporte.id_estado_reporte` = último registro de `historial_estado` (trigger). Evita un JOIN en cada listado.
- `reporte.vigente_hasta` = `creado_en` + `vigencia_dias`, **congelado al crear**: cambiar la configuración después no altera reportes existentes. El mapa público solo muestra reportes con ubicación y vigentes (`v_reporte_mapa`).
- `codigo_seguimiento` se genera desde el id.

## 6. Reglas que impone la base (triggers de V2)
B1 dependencia hija de la misma entidad y sin ciclos · B2 solo reportante o apoyante sube evidencia · B3 categoría que exige ubicación trae GPS **o** barrio (V7; antes exigía GPS) · B4 estado = último historial · B5 envío solo al Correo principal activo de la entidad del reporte · B6 matrícula solo en categorías de empresa · B7 canal inmutable · B9 vigencia · B10 coherencia estado/campos del envío · B11 historial solo inserta y no retrocede · B12 el autor no apoya su reporte y debe estar vigente · **B13** (V5) un usuario sin contraseña tiene al menos una `identidad_externa`. Como un CHECK no puede mirar otra tabla, B13 es un CONSTRAINT TRIGGER diferido: se revisa al confirmar la transacción, para poder crear el usuario y luego su identidad. **Queda para el backend**: B8 (llenar `correo_verificado_en` al usar el token de verificación) y validar el formato de `canal_atencion.acceso` según el tipo de canal.

## 7. Qué NO guarda el modelo
- Contraseñas en claro (solo `password_hash`, NULL en cuentas solo-Google); «confirmar contraseña» no se guarda.
- El ID token de Google (solo su `sub`, en `identidad_externa`).
- Archivos de fotos (solo la URL).
- El estado oficial del trámite en la entidad.
- La comuna del reporte (se calcula).

## 8. Datos pendientes o por verificar
- Barrios y polígonos de comunas: sin cargar. Hasta que haya barrios nadie puede registrarse (`usuario.id_barrio` es obligatorio).
- Texto oficial del aviso de privacidad: hoy es un marcador.
- Correos «sin confirmar» (Gases de Occidente, Celsia, Veolia): canales inactivos. El de SEDAMA (sedama@tulua.gov.co) quedó confirmado el 26/10/2026; es solo de referencia (el envío va al CAIC). Falta marcarlo activo en V3.
- Nombre del estado 2: el modelo usa «Enviado»; Linear dice «Enviado a Secretaría».

## 9. Mapa de campos de formularios → columnas (borrador, verificar contra el Figma)
**Registro**: nombres → `usuario.nombres` · apellidos → `usuario.apellidos` · tipo y número de documento → `id_tipo_documento`, `numero_documento` · barrio → `id_barrio` · dirección → `direccion_correspondencia` · correo → `correo` · teléfono (opcional) → `telefono` · contraseña → `password_hash` · aceptación del aviso → `id_aviso_privacidad` + `acepto_privacidad_en`.
**Continuar con Google**: Google entrega correo, nombres y apellidos (editables) → `usuario.correo`, `nombres`, `apellidos`; el `sub` → `identidad_externa.identificador_externo`. Lo demás (documento, barrio, dirección, teléfono, aviso) se pide en «completar perfil». Sin `password_hash`.
**Actualizar mi reporte**: radicado de la Alcaldía → `reporte.radicado_oficial`.
**Reporte**: categoría → `reporte.id_categoria` · descripción (10–500) → `descripcion` · ubicación GPS → `ubicacion` · barrio (opcional) → `id_barrio` · dirección del incidente (opcional) → `direccion_incidente` · matrícula (solo empresas) → `matricula_cuenta_servicio` · autorización de envío → `autorizacion_envio_en` · fotos 1 a 3 → `evidencia_reporte.url` + `orden`.
**Del sistema (no son campos)**: `codigo_seguimiento`, `id_estado_reporte`, `vigente_hasta`, `creado_en`, `requiere_revision`.
