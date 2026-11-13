# Plan técnico — Correo formal, canales por entidad y vista previa del mensaje

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se construye lo que dice `spec.md`.
> Solo se redacta con la especificación **Aprobada**. No se escribe código hasta que
> este plan esté **Aprobado**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado** (Borrador → En revisión → Aprobado) |
| Especificación | [spec.md](spec.md) (aprobada el 2026-11-12 y modificada ese día con el atajo, opción A) |
| Aprobado por / fecha | Andrés, 2026-11-12 |
| Última actualización | 2026-11-12 |

## 1. Enfoque general

Una sola función arma el mensaje (`PlantillaReporte`) y alimenta tres usos: el **correo**, la **vista previa** y **«Copiar mi mensaje»**. Así el texto que el ciudadano revisa es idéntico al que se envía (CA-15) y las plantillas A, B y C salen de datos que ya existen: el grupo se deduce de `entidad_destino.id_tipo_entidad` y `categoria.requiere_ubicacion` (A = Alcaldía con ubicación; B = Alcaldía sin ubicación; C = empresa), sin columna nueva.

Los canales siguen en `canal_atencion` (ya admite varios por entidad). Se le agregan columnas y un catálogo pequeño, y la ruta devuelve **todos** los canales activos de la entidad en vez de uno.

**Atajo (opción A):** la decisión se toma en el paso 1 con un dato que la ruta ya trae, `permiteCorreo`. Si es falso (gas, energía y aseo), «Continuar» lleva a una pantalla de canales y el flujo termina ahí: no se crea reporte, así que el backend no cambia para esto. El resto de las categorías sigue los 4 pasos, con la vista previa antes de enviar.

El orden de trabajo es el de Andrés: **Figma → backend → frontend → pruebas funcionales**.

## 2. Stack y límites (fijados, no se cambian)

Java 21 + Spring Boot 3.3 · PostgreSQL 16 + PostGIS 3.4 · JWT · React + Vite + PWA (pnpm) · Cloudinary (`AlmacenFotos`) · Resend (`EnviadorCorreo`) · Netlify + Render + Neon. Esta solución no usa nada fuera de esa lista. `RESEND_PRUEBA_DESTINO` sigue puesta.

## 3. Cambios por capa

### Figma (primero; se valida con Andrés antes de seguir)
Se editan solo las pantallas de este flujo, con los tokens del Design System (Nunito Sans, colores y chips ya definidos):
- **«Paso 3 · Revisar y enviar»:** tarjeta «Así llegará tu mensaje» con las secciones (hechos, ubicación, fecha, evidencia, datos de contacto), un «Editar» por sección y los estados cargando, error y sin datos.
- **«Cómo reportar a {entidad}» (pantalla nueva, el atajo):** reemplaza los pasos 2 a 4 para gas, energía y aseo. Muestra los canales con ícono por tipo, acción directa (Abrir WhatsApp, Llamar, Abrir formulario), horario e instrucciones; el recomendado va primero. Sin barra de pasos y con «Atrás» al paso 1.
- **Resultado del envío** (solo rebote o fallo): la misma lista de canales más el botón «Copiar mi mensaje».
- Se entregan capturas a Andrés; sin su visto bueno no se pasa al backend.

### Base de datos — `V21__canales_y_finalidad.sql` (la siguiente libre; V20 es la última)
**Estructura**
- Tabla de catálogo `finalidad_canal` (`id_finalidad_canal`, `nombre` único): Radicar PQR, Reportar daño, PQR anónimo, Consultar estado, Emergencia, Información. Es la «una tabla por concepto» del ER.
- `canal_atencion` suma: `id_finalidad_canal` (FK, por defecto «Información»), `orden` (SMALLINT, por defecto 100), `horario` (VARCHAR 200, nulo), `fuente` (VARCHAR 500, nulo) y `verificado_en` (DATE, nulo).

**Datos** (nada se borra: un canal se desactiva y se crea otro; el acceso de un canal no se edita)
| Entidad | Cambio |
| --- | --- |
| Centroaguas | `info@centroaguas.com` (canal 13) pasa a **activo y principal**; `notificacionespqr@` (12) a inactivo, con su motivo en `detalle`. En `configuracion_enrutamiento` E01: `id_canal_correo` = 13 y `correo_confirmado` = verdadero. Se agrega la web `https://www.centroaguas.com/index.php/contactenos` (el aplicativo PQRS está en una IP con HTTP y no se enlaza) y se completan línea y oficina con su horario. |
| Gases de Occidente | `inquietudes@gdo.com.co` (18) a inactivo y no principal; E02: `correo_confirmado` = falso. Se agregan el formulario «Registro de PQR» (primero en el orden) y el teléfono del contact center (602) 418 7333; 164 queda como Emergencia. **El WhatsApp 315 665 8536 (17) pasa a inactivo** hasta verificarlo. |
| Celsia | Sin cambios de correo (ya inactivo). Orden: Zona Clientes (Radicar PQR) primero, WhatsApp 310 390 3468 (Reportar daño) y la línea. |
| Veolia | `pqr.tulua@` (26) con `detalle` del motivo y se registra `pqr.tulua.co@veolia.com` inactivo. **El WhatsApp (24) queda activo y primero**, con `detalle` «Elija Dto Palmira Buga Tuluá → Residuos → Aseo Valle», `fuente` «Verificado por Andrés, 12 nov 2026» y `verificado_en`. Se agrega el formulario «Contáctanos» de su sitio; la línea y la oficina siguen. |
| Alcaldía | Se agregan formulario PQRSDF, PQRSDF anónimo, consulta de estado, agente virtual y oficina presencial, con horario; el teléfono recibe su horario. |
| Todos | `fuente` y `verificado_en` (2026-11-12) en cada canal con evidencia. |

Orden de las sentencias: primero se quita lo principal de un canal y después se marca el otro (índice único parcial `uq_canal_principal` y `ck_canal_principal_entidad`). El trigger de `registro_envio_correo` solo mira el canal al insertar, así que los envíos históricos no se tocan.

### Backend (`backend/src/main/java/com/notiurba/…`)
- **`correo/PlantillaReporte`** (reescrita): `Datos` pierde `codigo` y `dependencia` y gana nombre, tipo y número de documento por separado y la bandera «con matrícula». Un método `componer(Datos, Grupo)` devuelve `Mensaje(asunto, texto, html, secciones)`; `crear` lo envuelve en `CorreoSaliente`. Reglas: sin filas vacías, fecha «9 de noviembre de 2026, 10:07 a. m.» (se reemplaza el espacio fino U+202F por un espacio normal), coordenadas `%.5f` con `Locale.ROOT`, fotos como `<img>` con texto alternativo y enlace (la misma URL con ancho fijo), versión en texto plano idéntica, escape de HTML, sin «NotiUrba» ni código.
- **`correo/GrupoPlantilla`** (enum nuevo A, B, C) y su resolución desde la ruta.
- **`correo/EnrutamientoServicio`**: `Ruta` suma `canales` (lista de `Canal(tipo, acceso, finalidad, horario, detalle)` con los canales activos no-correo de la entidad, ordenados por `orden`); una consulta, sin N+1. Los campos actuales no cambian para no romper el frontend. Es lo único que necesita el atajo: `GET /api/enrutamiento/categorias` ya trae `permiteCorreo` de las 14 categorías en una llamada. `POST /api/reportes` sigue aceptando las categorías 12 a 14 (compatibilidad); la interfaz ya no las envía por ahí.
- **`correo/EnvioReporteServicio`**: usa la plantilla nueva; el asunto sin código. Los reintentos conservan el mensaje ya guardado.
- **`correo/VistaPreviaServicio`** y controlador: arma el `Mensaje` desde el borrador (nombre y contacto salen de la sesión; barrio y comuna con `AsignadorDeComuna`), sin escribir en la base.
- La comuna y el cálculo siguen en PostGIS.

### API (se agrega a `docs/contexto/11-contrato-api.md` al aprobar el plan)
| Método y ruta | Rol | Entrada | Salida | Errores |
| --- | --- | --- | --- | --- |
| `POST /api/reportes/vista-previa` | Ciudadano | `idCategoria`, `latitud`, `longitud`, `idBarrio`, `descripcion`, `direccionIncidente`, `matriculaCuentaServicio`, `totalFotos` (0 a 3) | `{asunto, texto, secciones:[{id, etiqueta, valor, paso}], permiteCorreo, ruta}` | 400 `VALIDACION` con `campo`, 401 |
| `GET /api/reportes/{codigo}/mensaje` | Dueño del reporte | — | `{asunto, texto}` del mensaje con las fotos reales | 401, 404 |
| `GET /api/enrutamiento/categorias[/{id}]` y `GET /api/reportes/{codigo}/envio` | igual que hoy | — | `ruta` con el campo nuevo `canales` | igual que hoy |

`paso` indica a qué pantalla vuelve «Editar»: `categoria`, `ubicacion` o `detalles`.

*Ajuste al implementar (12 nov):* el rebote o fallo se muestra con canales y «Copiar mi mensaje» en `ReporteEnviado`. El rebote real llega más tarde por el webhook (NOT-131), cuando la persona ya salió de esa pantalla: mostrarlo también en el detalle del reporte (`/app/reportes/:codigo`) queda para NOT-131. `ConsultarReporte` y `ResultadoCorreo` (por defecto) no tienen ruta y no se tocaron.

### Frontend (`frontend/src/app/…`, zona del ciudadano)
- `servicios/correoReportes.js`: `vistaPrevia(borrador)` y `mensaje(codigo)`.
- `componentes/VistaPreviaMensaje.jsx` (nuevo): la tarjeta de solo lectura (asunto, mensaje, «Ver completo»), estados de carga y error; se usa en `reportar/PasoConfirmacion.jsx`. *Ajuste al implementar (12 nov):* «Editar» por sección son los enlaces «Cambiar» que ya tiene el resumen del paso 4, cada uno a su paso (categoría, ubicación, detalles), como se validó en Figma; `secciones` queda en la API para otros usos.
- `componentes/ListaCanales.jsx` (nuevo): lista de canales con acción directa (teléfono `tel:`, web `https:` en otra pestaña, WhatsApp `https://wa.me/57…`, presencial como texto con horario). Se usa en el atajo y en el resultado del envío.
- `reportar/PasoCanales.jsx` (nuevo, ruta `/reportar/canales` en `rutas.jsx`): la pantalla «Cómo reportar a {entidad}», con «Atrás» al paso 1.
- `reportar/PasoCategoria.jsx`: al continuar, si la ruta de la categoría no permite correo, navega a `/reportar/canales` en vez de a `/reportar/ubicacion`; la pregunta de fuga de gas y la línea 164 se conservan. Un guardia en el contenedor `ReportarLayout` redirige a `/reportar/canales` si alguien llega directo a un paso 2 a 4 con una categoría sin correo.
- `componentes/ResultadoCorreo.jsx`: `CanalOficial` usa `ListaCanales` y agrega «Copiar mi mensaje» cuando el correo rebotó o falló.
- `componentes/estadoCorreo.js`: `enlaceCanal` reconoce WhatsApp (número colombiano de 10 dígitos más `57`).
- `simulacion/index.js`: respuestas simuladas de los dos endpoints nuevos y `canales` en la ruta.
- Textos en español y tono claro; sin tocar la lógica de sesión ni de registro.

### Integraciones y configuración
- Variables nuevas: ninguna. Resend sin cambios de configuración; `RESEND_PRUEBA_DESTINO` activa (el correo de prueba conserva «[PRUEBA]» y el aviso del destino real).
- Neon: la migración corre al desplegar; antes, branch de respaldo. Después, una consulta de solo lectura que compruebe los canales.

## 4. Estrategia de pruebas

| Criterio (spec) | Tipo de prueba | Dónde |
| --- | --- | --- |
| CA-01 a CA-06 | Unitaria de `PlantillaReporte`: tres grupos, con y sin matrícula, filas vacías, fecha, coordenadas, fotos, escape de HTML, ausencia de «NotiUrba» y de NU-… | `backend/src/test/…/correo/PlantillaReporteTest` |
| CA-02 | Unitaria del grupo por categoría (las 14) | `GrupoPlantillaTest` |
| CA-07 | Texto plano igual al HTML (unitaria) y **manual** en Gmail del celular y del computador con el destino de pruebas | `PlantillaReporteTest` + guion funcional |
| CA-08, CA-12 | Integración con PostGIS: ruta por categoría (1 y 11 con correo; emergencia de gas sin correo) | `EnrutamientoIntegracionTest` |
| CA-09, CA-09b, CA-09c | Integración: las categorías 12, 13 y 14 devuelven `permiteCorreo` falso y sus `canales` en orden (WhatsApp de Veolia primero). Frontend (`node --test`): la decisión de destino en el paso 1, `enlaceCanal` (WhatsApp `wa.me/57…`, `tel:`, `https:`) y el guardia de pasos. **Manual:** elegir gas, energía y aseo, ver la pantalla de canales y comprobar en la pestaña de red que no se llama a `POST /api/reportes` | `EnrutamientoIntegracionTest`, `node --test` y guion funcional |
| CA-10, CA-10b | Integración: canales ordenados con finalidad y horario; el WhatsApp de Gases de Occidente inactivo no aparece | `EnrutamientoIntegracionTest` |
| CA-11 | Integración: un envío con rebote devuelve la `ruta` con `canales` | `EnrutamientoIntegracionTest` |
| CA-13, CA-14 | MockMvc/integración: la vista previa responde 200, no crea reporte ni fila de envío; frontend: la tarjeta y los enlaces «Editar» | `VistaPreviaIntegracionTest` y `node --test` |
| CA-15 | Integración: el texto de la vista previa, `GET /mensaje` y el `mensaje_json` encolado son iguales | `VistaPreviaIntegracionTest` |
| CA-16 | Integración: 400 con `campo` y 401 | `VistaPreviaIntegracionTest` |
| CA-17, CA-18 | Migración de V20 a V21 con reportes y envíos existentes: canales corregidos, ninguno borrado, columnas llenas | `CanalesMigracionIntegracionTest` (patrón de `ComunaGuardadaMigracionIntegracionTest`) |
| Frontend | `enlaceCanal` (WhatsApp, `tel:`, `https:`), armado del borrador, simulación | `node --test` |
| Punta a punta | Con el jar real y base desechable: crear un reporte, ver la vista previa y comprobar el correo encolado | script en `tools/` (como `qa-panel-admin`) + prueba funcional manual |

Incluye entradas hostiles (HTML en la descripción, enlaces de foto no seguros) y los casos de error. Las pruebas existentes (backend 306, frontend 139) siguen pasando; las que fijan el asunto con código se actualizan.

## 5. Riesgos y mitigaciones

| Riesgo | Prob. / impacto | Mitigación |
| --- | --- | --- |
| El correo ya no lleva el código: se pierde la forma de correlacionar un rebote | Media / Medio | El webhook usa el identificador del proveedor y `registro_envio_correo` guarda la clave de idempotencia; prueba de rebote. |
| V21 cambia los canales principales y choca con el índice único o el `CHECK` | Media / Alto | Orden de sentencias definido y prueba de migración; branch de respaldo en Neon. |
| Reportes ya encolados conservan el formato viejo y su reintento ve otra configuración | Baja / Bajo | Hay 4 reportes en Neon; el reintento ya se rechaza con «La configuración del canal cambió». |
| La vista previa se desfasa del correo real | Media / Alto | Misma función `componer` y prueba CA-15. |
| Clientes de correo bloquean imágenes | Media / Bajo | Texto alternativo y enlace «Fotografía n» siempre visibles. |
| Cambios en pantallas de Katherine (`PasoCategoria`, `PasoConfirmacion`) | Baja / Medio | Cambios pequeños y aislados (un componente por pantalla más una pantalla nueva); se le avisa al integrar. |
| Gas, energía y aseo ya no aparecen en el mapa de calor ni en las estadísticas | Alta / Bajo | Decisión de Andrés (12 nov), escrita en la spec y en el contrato; el panel muestra «sin datos» para esas categorías. |
| Alguien llega por URL directa a los pasos 2 a 4 con una categoría sin correo | Baja / Bajo | Guardia en `ReportarLayout` que redirige a `/reportar/canales`. |
| Los pasos del atajo cambian el recorrido ya probado con Luz | Media / Bajo | Prueba funcional del flujo completo con categorías de cada tipo antes de integrar. |
| Los canales cambian con el tiempo | Media / Bajo | `fuente` y `verificado_en` por canal; la verificación periódica queda en el cierre. |

## 6. Alternativas descartadas

- **Texto libre editable del mensaje:** pide una columna nueva y cambia todo el flujo; la vista previa con «Editar» da la misma precisión (fuera de alcance en la spec).
- **Armar la vista previa en el frontend:** duplica la plantilla y se desfasa del correo.
- **Tabla `ruta_canal` (categoría → canales):** la entidad ya agrupa sus canales; solo hace falta `finalidad` y `orden`.
- **Miniaturas con transformación de Cloudinary:** depende de la configuración de la cuenta; se usa la misma foto con ancho fijo (≤ 1 MB, ya comprimida).
- **Enviar copias a varias direcciones de una entidad:** rompe «un destino por reporte».
- **Columna de grupo de plantilla:** se deduce de datos existentes.
- **Atajo con «Guardar también en NotiUrba» (opción B):** descartada por Andrés; esas entidades no reciben correo y nadie haría doble seguimiento a mano.
- **Seguir con los 4 pasos para todas las categorías:** hace trabajar al ciudadano para terminar en otro canal.

## 7. Decisiones pendientes

- [PENDIENTE] El WhatsApp de Gases de Occidente (315 665 8536) está en el repo, pero no en su sitio oficial: queda inactivo hasta verificarlo. Activarlo es un cambio de datos. No bloquea. Decide: Andrés.
- Resuelto el 12 nov: el WhatsApp de Veolia está verificado y activo.

## 8. Aprobación

- [x] Cada criterio de `spec.md` tiene un cambio técnico y una prueba.
- [x] No introduce otro framework ni otra base de datos.
- [x] Contrato de API y migración revisados (endpoints agregados como PLANEADO en `docs/contexto/11-contrato-api.md` §4).
- [x] **Aprobado por el usuario** (2026-11-12) → se puede pasar a `tasks.md`.
