# Plan técnico — Registro e ingreso rápidos y sin abusos

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se construye lo que dice `spec.md`.
> Solo se redacta con la especificación **Aprobada**. No se escribe código hasta que
> este plan esté **Aprobado**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado** (Borrador → En revisión → Aprobado) |
| Especificación | [spec.md](spec.md) (aprobada el 13 nov 2026) |
| Aprobado por / fecha | Andrés, 2026-11-13, con un ajuste: la descripción lleva su guía como texto de ayuda siempre visible, sin texto de ejemplo dentro de la casilla |
| Última actualización | 2026-11-13 |

## 1. Enfoque general

El orden es el de Andrés: **Figma → backend → frontend → pruebas**. Sin visto bueno de las pantallas no empieza el backend.

Cuatro decisiones de diseño que explican el resto:

1. **El correo de verificación sale después de guardar la cuenta, en otro hilo.** Se crea el token dentro de la transacción (es solo un `INSERT`), y el envío a Resend lo hace un oyente `AFTER_COMMIT` asíncrono. Así la respuesta no espera a Resend (CA-01) y nunca sale un enlace de una cuenta que no se guardó (CA-02). El reenvío usa la misma ruta.
2. **Todos los parámetros viven en la base y se leen en cada uso.** Sin caché: «aplica de inmediato» (CA-16, CA-27) queda gratis, y cuesta una consulta pequeña por login o por reporte.
3. **Los contadores viven en la base, no en memoria.** Render Free duerme y reinicia; un contador en memoria se perdería. Los topes se hacen respetar con SQL atómico, no con «leer y luego escribir», para que dos peticiones a la vez no se los salten (CA-18).
4. **El reloj se inyecta (`Clock`).** Las ventanas de 5 minutos y el «día» se prueban sin esperar.

## 2. Stack y límites (fijados, no se cambian)

Java 21 + Spring Boot 3.3 · PostgreSQL 16 + PostGIS 3.4 · JWT · React + Vite + PWA (pnpm) · Leaflet · Cloudinary (`AlmacenFotos`) · Resend (`EnviadorCorreo`) · Netlify + Render + Neon. Este plan no agrega ninguna dependencia: usa `@Async` y eventos de Spring, `JdbcTemplate` y PostGIS, que ya están.

## 3. Cambios por capa

### Figma (primero; se valida con Andrés antes de seguir)

Se diseñan seis pantallas con los tokens del Design System (Nunito Sans, colores y chips ya definidos), en el archivo `qRLcQ7jjC71F52CI6S9m1j`:

| # | Pantalla | Estados | Criterios |
| --- | --- | --- | --- |
| F-1 | **Panel · Configuración** (escritorio): tres bloques, «Ingreso» (intentos, ventana, bloqueo), «Reportes» (máximo por día) y «Duplicados» (días de «reciente» y tabla de distancia por categoría). Cada campo con su rango y una frase de qué hace. Deja el lugar de la vigencia para otra historia. | cargando, valores, error de rango junto al campo, guardado, error del servidor | CA-30, CA-31 |
| F-2 | **Ingreso · todos los mensajes** (celular): una variante por cada fila de la tabla de la spec (datos incorrectos, bloqueo con minutos, hora de regreso y cuenta regresiva, correo sin confirmar, sin internet, NotiUrba no responde, falla de Google). El bloqueo deshabilita «Iniciar sesión» hasta que termine y ofrece «Restablecer mi contraseña». | una por situación, más «terminó el bloqueo» | CA-32, CA-35 |
| F-3 | **Confirmación · límite diario alcanzado**: aviso que explica el límite y cuándo vuelve a poder reportar; lo escrito se conserva. | aviso | CA-33 |
| F-4 | **Confirmación · posible duplicado**, como **hoja de confirmación** igual a la de «¿Enviar el reporte?» y «Cancelar el reporte» (ícono, título, texto, botón principal y botón de texto, con el Design System). Dos botones: «Enviar de todas maneras» y «Cancelar reporte». Variantes de texto: parecido **de otra persona** ya enviado (el caso más común), parecido **de otra persona** aún sin enviar, parecido **propio** ya enviado y parecido **propio** guardado sin enviar; solo en las variantes propias va el enlace «Ver el reporte NU-000123». Si hay varios parecidos, el texto lo dice («Ya hay varios reportes parecidos…»). | 4 variantes de texto, enviando | CA-34, CA-46, CA-47, CA-48, CA-49 |

| F-5 | **Reportar · descripción y fotos** (celular): en «Descripción y fotografías», el título, la etiqueta y el **texto de ayuda siempre visible** de la descripción, con su ejemplo (CA-50) y el texto que pide una foto **reciente** y explica para qué sirve, con la variante «obligatoria» en lugar de «(opcional)». | normal, foto obligatoria, falta la foto, error de descripción | CA-45, CA-50 |
| F-6 | **Confirmación · foto necesaria**: aviso «En este lugar ya se resolvió un reporte parecido. Si el problema sigue o volvió, agregue una foto reciente que lo muestre», con «Agregar foto»; el botón de enviar queda apagado hasta que haya una. | aviso, con foto | CA-44 |

Se entregan capturas a Andrés. **Puerta:** su visto bueno escrito. F-3, F-4 y F-6 tocan `PasoConfirmacion`, y F-5 toca `PasoDetalles`, ambos de Katherine: se le señalan.

**Riesgo de herramienta:** esta sesión el conector de Figma por Dev Mode respondió que su servidor está apagado. Antes de empezar F-1 se prueba el conector de la sesión pasada (crear sesión y `get_metadata`); si no responde, se le pide a Andrés encender Dev Mode o se entregan los marcos de otra forma que él elija.

### Base de datos — `V22__limites_y_duplicados.sql` (la siguiente libre; V21 es la última)

**Parámetros** (filas nuevas en `configuracion`, con sus rangos como `CHECK`, igual que `ck_config_distancia_comuna` de V13):

| Clave | Valor inicial | Rango |
| --- | --- | --- |
| `login_intentos_maximos` | 3 | 1 a 10 |
| `login_ventana_minutos` | 5 | 1 a 60 |
| `login_bloqueo_minutos` | 5 | 1 a 60 |
| `reportes_maximos_por_dia` | 2 | 1 a 50 |
| `duplicado_ventana_dias` | 30 | 1 a 30 |

**Tabla nueva `distancia_duplicado`** (una fila por categoría que exige ubicación): `id_categoria` (PK y FK a `categoria`, `ON DELETE CASCADE`), `metros` (`SMALLINT NOT NULL`, `CHECK` entre 5 y 500), `id_usuario_actualiza` (FK, `ON DELETE SET NULL`) y `actualizado_en`. Se eligió tabla y no una columna en `categoria` porque pide auditoría por cambio (CA-27) y así la tiene igual que `configuracion`. Valores iniciales (cortos y flexibles, porque Tuluá urbano es pequeño y el centro junta reportes cercanos): categorías 1 y 3 → 5 m; 4 → 8 m; 2, 5 y 11 → 10 m; 8, 12, 13 y 14 → 15 m. Las categorías 6, 7, 9, 10 y 15 no tienen fila («no aplica»).

**Tabla nueva `intento_login`** (una fila por correo, siempre en minúsculas y sin espacios): `correo_normalizado` (PK `VARCHAR(255)`), `fallidos` (`SMALLINT`), `primer_fallo_en`, `bloqueado_hasta` (nulo si no está bloqueado) y `actualizado_en`, con índice por `actualizado_en` para limpiar. No guarda IP ni contraseña. No tiene FK al usuario: así un correo inexistente se comporta igual que uno real (CA-11).

**Columna nueva** `reporte.requiere_evidencia BOOLEAN NOT NULL DEFAULT FALSE`: la marca el sistema cuando el reporte repite uno propio ya resuelto, y es lo que impide enviarlo sin foto. No se edita a mano.

Cambia el modelo de datos (de 29 a 31 tablas y una columna en `reporte`): al cerrar hay que actualizar `10-modelo-de-datos.md`, el draw.io, el Sheet y el Doc (skill `cierre-de-historia`).

### Backend (`backend/src/main/java/com/notiurba/…`)

**Registro rápido (CA-01 a CA-04)**
- `auth/registro/VerificacionServicio`: se parte `emitirYEnviar` en `emitir` (crea el token en la transacción y devuelve el enlace) y el envío. El envío vive en un oyente nuevo `CorreoVerificacionListener` con `@Async` y `@TransactionalEventListener(AFTER_COMMIT)`; un fallo solo se registra, como hoy. `reenviar` usa la misma ruta.
- `config/AsincronoConfig` (nueva): `@EnableAsync` y un `ThreadPoolTaskExecutor` acotado (2 hilos, cola de 50). Si la cola se llena, el correo se descarta con un log y el ciudadano pide el reenvío (CA-03): es preferible a bloquear el registro.
- `config/RelojConfig` (nueva): un bean `Clock` (UTC). La zona de negocio para el «día» es `America/Bogota`.
- Medir antes y después: la base actual (~10 s) frente a un `EnviadorCorreo` de prueba que tarda 8 s. Se revisa también el costo de BCrypt, por si parte de los 10 s no son Resend.

**Google (CA-05 a CA-07)**
- `auth/google/GoogleAuthServicio`: cada paso (`verificar`, `vincular`, `crear-cuenta`, `sesion`) captura el fallo, escribe `WARN` con `paso` y **solo el nombre de la clase** de la excepción (nunca `getMessage()`, que puede traer correo o documento) y relanza. 401 y mensajes no cambian.
- Diagnóstico del celular (CA-07), sin tocar código del servidor: preflight CORS a `/api/auth/google` con el origen de Netlify, tiempo del primer arranque de Render, y comparación con el `GOOGLE_CLIENT_ID` esperado. Se documenta el resultado en `docs/contexto/12-estado-actual-y-traspaso.md`. Si la causa está en el frontend o en Render, se anota y se decide con Andrés.

**Límite de intentos de login (CA-08 a CA-13)**
- `auth/IntentosLoginServicio` (nuevo): `verificarNoBloqueado(correo)`, `registrarFallo(correo)` y `limpiar(correo)`. `registrarFallo` es **una sola sentencia** `INSERT … ON CONFLICT DO UPDATE` que decide en SQL si reinicia la cuenta (el primer fallo salió de la ventana), suma uno, y al llegar al máximo fija `bloqueado_hasta = ahora + bloqueo` y deja `fallidos` en 0. Así dos intentos simultáneos no se saltan el tope. Cada fallo borra además las filas con `actualizado_en` de más de un día, para que mandar correos inventados no engorde la tabla.
- `auth/AuthController.login`: primero `verificarNoBloqueado` (antes de mirar la contraseña, así una contraseña correcta durante el bloqueo tampoco entra), luego las credenciales; un fallo llama a `registrarFallo`; un acierto llama a `limpiar`. El correo inexistente, la cuenta solo-Google y la contraseña mala hacen **lo mismo**, así que el conteo tampoco revela cuentas (CA-11). El mensaje de fallo no cambia ni dice cuántos intentos quedan (CA-12).
- La recuperación de contraseña (`auth/recuperacion/RecuperacionServicio.restablecer`) llama a `limpiar(correo)`: restablecer con el enlace del correo levanta el bloqueo (CA-37).
- Todo 401 por datos incorrectos lleva `datos.intentosRestantes` (máximo menos fallos acumulados), calculado igual para correos que existen, que no existen y cuentas solo-Google (CA-40). El login solo lo muestra cuando vale 1.
- Al bloquear, 429 `INTENTOS_EXCEDIDOS` con «Demasiados intentos. Espera N minutos para volver a intentar.» (N sube al minuto entero) y el encabezado `Retry-After`.

**Límite diario y duplicados (CA-14 a CA-23)**
- `reporte/ReporteServicio.crear`: tras `validar`, y dentro de la misma transacción, se toma `SELECT … FROM usuario WHERE id_usuario = ? FOR UPDATE` para **serializar a ese ciudadano** (CA-18). Después, en este orden: (1) límite diario; (2) duplicado, salvo que venga `confirmarDuplicado = true`; (3) insertar. `validar` y la vista previa no cambian (CA-17).
- `reporte/LimiteDiarioServicio` (nuevo): cuenta los reportes del ciudadano desde las 00:00 de Bogotá (`creado_en >= inicio del día`); si ya hay `reportes_maximos_por_dia`, 429 `LIMITE_DIARIO_REPORTES` con el máximo y la hora en que vuelve (medianoche de Bogotá). Un reporte rechazado no se guarda, así que no cuenta (CA-23).
- `reporte/DuplicadoServicio` (nuevo): si hay GPS y la categoría tiene fila en `distancia_duplicado`, busca entre los reportes de **todas las cuentas** los de la **misma categoría**, a menos de `metros`, **no «Resueltos»** (CA-43) y creados dentro de `duplicado_ventana_dias`. Primero filtra con el índice GiST de `ubicacion` (caja alrededor del punto) y luego mide la distancia exacta en metros con `ST_DWithin(ubicacion::geography, punto::geography, metros)`. Devuelve **cuántos** hay, el **más reciente** (si hay uno propio entre ellos, gana el propio, para poder enlazarlo), su **estado**, su fecha y si es **propio**. Sin GPS o sin fila no revisa (CA-22). 409 `POSIBLE_DUPLICADO`.
- **Datos base, no el reporte completo (CA-48).** La respuesta del duplicado lleva `esPropio`, `cantidad`, `estadoExistente`, `creadoEn` (solo la fecha, sin hora) y `descripcion`: un **resumen de hasta 140 caracteres** del más reciente, sin saltos de línea, cortado en una palabra completa y con «…» si se acortó. **`codigoExistente` solo si es propio.** Nunca viajan la dirección, el barrio, las fotos, el nombre ni el correo. Hay prueba que revisa el cuerpo completo de la respuesta con un parecido de otra persona. El frontend muestra la descripción como texto plano (React la escapa).
- Misma clase, método `repiteResuelto(...)`: la misma búsqueda pero contra reportes **«Resueltos» de cualquier cuenta**, contando la ventana desde el día en que se resolvieron (último cambio a «Resuelto» en `historial_estado`, ya indexado). Si hay uno y **no** hay un parecido sin resolver, el reporte necesita evidencia (RF-39).
- `reporte/ReporteServicio.crear`: si `repiteResuelto` y `totalFotos` es 0 → 422 `EVIDENCIA_REQUERIDA` y no se guarda (CA-41); si trae fotos, se guarda con `requiere_evidencia = true` (CA-42). Un duplicado **no resuelto** gana siempre: se avisa antes (CA-19).
- `correo/EnvioReporteServicio`: antes de armar el correo, si el reporte tiene `requiere_evidencia` y **cero** fotos subidas, 422 `EVIDENCIA_REQUERIDA`. Cubre el caso de que la foto declarada nunca llegara (CA-42).
- `correo/VistaPreviaServicio`: la respuesta suma `requiereEvidencia` (solo lectura, no guarda nada) para que la pantalla avise antes de enviar (CA-44).
- `reporte/CrearReporteRequest`: campos opcionales `confirmarDuplicado` y `totalFotos` (Integer de 0 a 3, lo que el cliente declara que subirá; el control final es el de `EnvioReporteServicio`). El constructor corto que usan las pruebas sigue valiendo.

**Parámetros del administrador (CA-26 a CA-29)**
- `admin/ParametrosServicio` (nuevo): lee y escribe los cinco valores y la tabla de distancias en **una sola transacción** (todo o nada), valida los rangos con el campo y el rango en el mensaje (CA-28) y guarda `id_usuario_actualiza` y `actualizado_en`. Es la **única** fuente de lectura de los demás servicios.
- `admin/ParametrosController` (nuevo): `GET` y `PUT /api/admin/configuracion/limites`. `SecurityConfig` ya restringe `/api/admin/**` a ADMIN (CA-29).

**Errores con datos extra**
- `web/error/ErrorApi` gana un campo opcional `datos` (un mapa, solo sale si existe). `ApiException` lo acepta. Para: `esperarSegundos` en `INTENTOS_EXCEDIDOS`, `maximo` y `vuelveEn` en `LIMITE_DIARIO_REPORTES`, `codigoExistente`, `estadoExistente` y `creadoEn` en `POSIBLE_DUPLICADO`. Es una ampliación compatible del formato `{ codigo, mensaje, campo }`: los clientes que no la leen siguen funcionando. Se documenta en el contrato.
- `web/error/Codigos`: tres códigos y sus mensajes fijos nuevos.

### API (contrato en `docs/contexto/11-contrato-api.md`)

| Método y ruta | Rol | Entrada | Salida | Errores |
| --- | --- | --- | --- | --- |
| `POST /api/auth/login` (cambia) | público | igual | igual | **nuevo** 429 `INTENTOS_EXCEDIDOS` con `datos.esperarSegundos` y `Retry-After`; el 401 de datos incorrectos suma `datos.intentosRestantes` |
| `POST /api/reportes` (cambia) | CIUDADANO | suma `confirmarDuplicado` y `totalFotos` (opcionales) | igual | **nuevos** 429 `LIMITE_DIARIO_REPORTES` (`datos.maximo`, `datos.vuelveEn`), 409 `POSIBLE_DUPLICADO` (`datos.esPropio`, `datos.cantidad`, `datos.estadoExistente`, `datos.creadoEn`, `datos.descripcion` (resumen) y, **solo si es propio**, `datos.codigoExistente`) y 422 `EVIDENCIA_REQUERIDA` |
| `POST /api/reportes/vista-previa` (cambia) | CIUDADANO | igual | suma `requiereEvidencia` | sin cambios |
| el envío del correo del reporte (cambia) | CIUDADANO | igual | igual | **nuevo** 422 `EVIDENCIA_REQUERIDA` si falta la foto |
| `POST /api/auth/registro` (cambia) | público | igual | igual, pero sin esperar el correo | sin cambios |
| `GET /api/admin/configuracion/limites` | ADMIN | — | `{ login: { intentosMaximos, ventanaMinutos, bloqueoMinutos }, reportesMaximosPorDia, duplicados: { ventanaDias, categorias: [{ idCategoria, nombre, metros }] } }` | 401/403 |
| `PUT /api/admin/configuracion/limites` | ADMIN | el mismo objeto | el objeto guardado | 400 `VALIDACION` con campo y rango; 401/403 |

No se inventan más endpoints. Al aprobar este plan se agregan al contrato con estado *PLANEADO*.

### Frontend (`frontend/src/…`)

Textos en español en `compartido/textos/`; los datos simulados (`simulacion/index.js`) cubren los casos nuevos.

- `compartido/api/errorApi.js` y `cliente.js`: `ErrorApi` conserva `datos` (hoy descarta lo que no sea `codigo`, `mensaje` y `campo`).
- **Mensajes del ingreso (CA-35, CA-36).** `compartido/auth/errorLogin.js` se reescribe: decide el mensaje por **`codigo` y `estado`** (no por el texto que traiga el servicio) y lo toma de un archivo nuevo `compartido/textos/ingreso.js`, en la voz de la app (usted). Un 5xx, una respuesta rara o un código desconocido **nunca** muestran `error.mensaje`. `cliente.js` deja de decir «servidor» en su mensaje de falta de conexión. `BotonGoogle.jsx` usa el mismo mapa (hoy muestra `fallo.mensaje`, que es justo el «No pudimos comunicarnos con el servidor» del celular).
- `compartido/paginas/Login.jsx`: una sola zona de aviso para cada situación de la tabla de la spec, con su acción (reenviar enlace, restablecer contraseña, reintentar). Con `INTENTOS_EXCEDIDOS` compone «bloqueamos el ingreso durante N minutos… podrá intentarlo a las HH:MM» con `esperarSegundos`, deshabilita «Iniciar sesión» con cuenta regresiva y no llama al servicio hasta que termine (CA-32). Los textos de la voz de la app se afinan en Figma con Amelia.
- `app/reportar/PasoConfirmacion.jsx` (Katherine): con `LIMITE_DIARIO_REPORTES` muestra el aviso y conserva lo escrito (CA-33); con `POSIBLE_DUPLICADO` abre una hoja inferior (**ajuste en la implementación, 13 nov:** `HojaInferior`, la base de `HojaConfirmacion`, porque en `HojaConfirmacion` tocar fuera de la hoja llama al botón secundario y aquí ese botón descarta el borrador; con `HojaInferior` tocar fuera solo cierra. Se ve igual) con ícono de alerta y el texto según `esPropio`, `cantidad` y `estadoExistente`; el enlace a `/app/reportes/:codigo` aparece **solo** si viene `codigoExistente` (es propio). Su botón principal es «Enviar de todas maneras» (repite el envío con `confirmarDuplicado: true`) y el botón de texto es «Cancelar reporte», que reutiliza la acción de `cancelarReporte` de `PasoCategoria.jsx` (descarta el borrador y vuelve al inicio); esa acción se mueve a un lugar común (`ReportarLayout` o `useReporte`) para no copiarla (CA-34, CA-46, CA-47).
- `admin/paginas/Configuracion.jsx` (nueva) con `Configuracion.css`, `admin/servicios/limitesService.js`, la ruta `configuracion` en `admin/rutas.jsx` y un enlace en `LayoutAdmin.jsx` (CA-30, CA-31): carga los valores, valida el rango junto a cada campo antes de enviar, guarda todo con un botón y confirma.
- **Descripción y fotos (CA-44, CA-45, CA-50).** `app/reportar/PasoDetalles.jsx` y `compartido/textos/`: se reescribe el microcopy de la descripción y de la foto (texto de partida más abajo, en la sección 7). La foto pide una foto reciente y «(opcional)» se vuelve «(obligatoria)» cuando `requiereEvidencia`. `PasoConfirmacion.jsx`: con `requiereEvidencia` y sin foto muestra el aviso F-6 con «Agregar foto» y apaga el envío; con `EVIDENCIA_REQUERIDA` muestra el mismo aviso. `useVistaPrevia.js` y `vistaPrevia.js` leen el campo nuevo; `armarSolicitudReporte` envía `totalFotos`.
- `Registro.jsx` no cambia: la mejora es de velocidad y se ve sola.

### Integraciones y configuración

- Variables de entorno nuevas: **ninguna**.
- Resend: ya no se espera su respuesta en el registro; `RESEND_PRUEBA_DESTINO` sigue puesta.
- Despliegue: V22 corre sola con Flyway al arrancar Render. Es aditiva (no borra ni cambia datos), así que no pide respaldo en Neon; el despliegue como tal queda en NOT-133.

## 4. Estrategia de pruebas

Casos de error y entradas hostiles incluidos. Las pruebas existentes (backend 361, frontend 156) deben seguir pasando. Las que prueban el registro con un envío simulado usan un ejecutor síncrono de prueba para no volverse intermitentes; el «≤ 2 s» se prueba aparte con un envío lento.

| Criterio (spec) | Tipo de prueba | Dónde |
| --- | --- | --- |
| CA-01, CA-02 | Integración: envío que tarda 8 s, la respuesta llega en < 2 s; el correo sale después del commit; si el registro falla (correo repetido) no sale correo | `backend/src/test/…/auth/registro/` |
| CA-03, CA-04 | Integración: con el envío roto, el registro funciona y `reenviar-verificacion` emite otro enlace; los errores de hoy no cambian | ídem |
| CA-05, CA-06 | Unitaria: se captura el log; no contiene correo, nombre, documento, token ni `sub`; 401 igual | `…/auth/google/` |
| CA-07 | Manual y documental: preflight CORS, arranque de Render, comparación de `GOOGLE_CLIENT_ID` | `12-estado-actual-y-traspaso.md` |
| CA-08 a CA-10, CA-13 | Integración con reloj fijo: 3 fallos en 5 min bloquean; con la contraseña buena sigue bloqueado; a los 5 min entra; un acierto reinicia; otro correo no se afecta | `…/auth/` |
| CA-11, CA-12, CA-40 | Integración: correo inexistente, solo-Google y contraseña mala dan el mismo bloqueo, el mismo mensaje y el mismo `intentosRestantes`; el mensaje no dice cuál dato falló | ídem |
| Concurrencia del login | Prueba con hilos: 20 intentos a la vez no dejan más de `max` fallos sin bloquear | ídem |
| CA-14 a CA-16, CA-23 | Integración: 2 reportes pasan, el 3.º da 429 y no se guarda; cambiar el valor en la base aplica sin reiniciar; el día cambia a las 00:00 de Bogotá | `…/reporte/` |
| CA-17 | Integración: la vista previa 5 veces no cuenta | ídem |
| CA-41 a CA-43 | Integración con PostGIS: repetir un lugar con un «Resuelto» (propio o de otra cuenta) sin foto da 422; con foto se guarda marcado; sin foto real no se envía; un «En trámite» cercano sí es duplicado y un «Resuelto» no; si hay resuelto y sin resolver, gana el duplicado; el resuelto cuenta desde el día en que se resolvió | ídem y `…/correo/` |
| CA-44 | Integración: la vista previa indica `requiereEvidencia` y no escribe nada | `…/correo/` |
| CA-45, CA-50 | Componente: `PasoDetalles` muestra el ejemplo, la ayuda y el texto de foto reciente, y «obligatoria» cuando corresponde; `PasoConfirmacion` apaga el envío sin foto; el microcopy pasa la prueba de RN-24 (CA-36) | `frontend/src/app/reportar/` |
| CA-18 | Prueba con hilos: dos envíos simultáneos con 1 reporte hecho dejan 2, no 3 | ídem |
| CA-19 a CA-22 | Integración con PostGIS: dentro y fuera de la distancia (5 m y 15 m), otra categoría, fuera de la ventana, sin GPS, con `confirmarDuplicado`; el parecido es de **otra cuenta** y también **propio**; con varios parecidos gana el propio y la cantidad es correcta | ídem |
| CA-48, CA-49 | Integración: con un parecido de otra persona la respuesta trae fecha, estado, cantidad y el resumen (140 caracteres, palabra completa, «…» si se cortó, sin saltos de línea), y el cuerpo completo no contiene su código, dirección, barrio, fotos, nombre ni correo; con uno propio sí trae el código | ídem |
| Filtro (misma categoría, cercanía, 30 días) | Integración con PostGIS: un parecido de otra categoría, a 20 m, o de hace 31 días **no** avisa; uno de la misma categoría, a menos de la distancia y de hace 29 días **sí** | ídem |
| CA-24 | Revisión del contrato y prueba del formato `{ codigo, mensaje, campo, datos }` | `…/web/error/` y `11-contrato-api.md` |
| CA-26 a CA-29 | Integración: GET con valores y categorías; PUT válido aplica y audita; fuera de rango da 400 con el campo; PUT parcial no deja nada a medias; ciudadano 403 y sin sesión 401 | `…/admin/` |
| Migración V22 | Prueba de migración: valores y rangos iniciales, `CHECK` rechazan valores fuera de rango, tabla de distancias con 10 filas | `…/MigrationIntegrationTest` o similar |
| CA-30, CA-31 | Componente y manual contra datos simulados y contra el backend local | `frontend/src/admin/…` |
| CA-32 a CA-34 | Componente (`node --test`) con errores simulados; manual en celular | `frontend/src/…` |
| CA-35 | Unitaria de `errorLogin.js`: cada pareja código y estado da su mensaje; un 500 con texto técnico, un código inventado y una respuesta vacía dan el mensaje genérico; `Login` y `BotonGoogle` muestran la acción de cada fila | `frontend/src/compartido/auth/` |
| CA-34, CA-46, CA-47 | Componente: la hoja sale con `POSIBLE_DUPLICADO`; el texto cambia entre «ya enviado» y «guardado»; «Enviar de todas maneras» repite el envío con `confirmarDuplicado`; «Cancelar reporte» limpia el borrador y navega al inicio; «Ver el reporte» enlaza al código correcto | `frontend/src/app/reportar/` |
| CA-36 | Prueba que recorre `textos/ingreso.js` y los textos de bloqueo, límite y duplicado, y falla si encuentra un número de estado, un código en mayúsculas o las palabras de RN-24 | `frontend/src/compartido/textos/` |
| CA-37 | Integración: bloquear un correo, restablecer su contraseña con el enlace y volver a ingresar | `…/auth/recuperacion/` |
| CA-39 | Integración: la cuenta A llega a su límite y la cuenta B reporta con su cupo completo | `…/reporte/` |
| Todo el flujo | Script de QA a mano contra el backend local (como `qa-correo-reporte`): registro, login bloqueado, 3 reportes, duplicado, ajustes del administrador | `tools/qa-limites/` |

## 5. Riesgos y mitigaciones

| Riesgo | Probabilidad / impacto | Mitigación |
| --- | --- | --- |
| La historia sigue en 5 puntos pero incluye seis pantallas de Figma, una pantalla nueva del panel y varios avisos | Alta / media: más trabajo que NOT-129, similar a NOT-130 | Decidido no tocar la estimación. Los avisos F-2 a F-6 son pequeños; lo más pesado es F-1 y su pantalla. Si el tiempo aprieta, se avisa antes de recortar |
| Alguien bloquea a propósito la cuenta de otra persona o del administrador (el bloqueo es por correo) | Media / bajo en el MVP | Decidido y aceptado en la spec (preguntas 2 y 3); el bloqueo dura solo 5 minutos y es configurable |
| Un mensaje técnico se cuela al ciudadano (un 5xx con texto de la base, o un código nuevo sin mapa) | Media / alto | El mapa decide por código y estado y no usa el texto del servicio; la prueba de CA-36 recorre todos los textos |
| Un correo con mayúsculas o espacios esquiva el contador | Media / alto | Se normaliza (minúsculas, sin espacios) antes de contar, y hay prueba hostil |
| Mandar correos inventados engorda `intento_login` | Media / bajo | Cada fallo borra las filas de más de un día |
| Dos peticiones a la vez superan el tope diario o el de intentos | Media / alto | Bloqueo de fila en el reporte y sentencia atómica en el login; pruebas con hilos |
| Una ráfaga de intentos de login a la vez prueba muchas contraseñas antes de que se cuente el primer fallo | Media / alto | Hallado en la revisión de código (13 nov): cada intento se cuenta **antes** de comparar la contraseña, con la fila del correo bloqueada; con el tope de 3, una ráfaga de 20 compara solo 3 contraseñas (prueba con un codificador que cuenta) |
| El envío asíncrono pierde un correo (cola llena o caída justo después del commit) | Baja / medio | Un log; el ciudadano ya tiene «reenviar verificación». Se documenta |
| `ST_DWithin` con `geography` no usa el índice GiST de `ubicacion`, y ahora la búsqueda cruza **todas** las cuentas | Media / medio | La consulta usa primero una caja con el índice GiST y solo mide con `geography` las pocas filas cercanas, con `id_categoria` indexado; se confirma con `EXPLAIN` y una prueba con 10 000 reportes |
| Una descripción trae datos personales sin querer y otra persona ve el resumen | Media / medio | Resumen de 140 caracteres, ninguna otra pista de identidad, y una ayuda en el campo que lo advierte (CA-50). El aviso de privacidad debe cubrirlo: se anota para la validación jurídica de ADR-0008 |
| Alguien consulta puntos sueltos para averiguar dónde hay reportes de otras personas y leer sus resúmenes | Baja / bajo | La respuesta dice que hay parecidos, cuántos, su estado, la fecha y un resumen de 140 caracteres (decidido en CA-48), nunca quién lo escribió. El 409 no cuenta contra el tope diario, así que se puede repetir. **[PENDIENTE]** decidir con Andrés si se limitan los avisos de parecido por cuenta (por ejemplo, 20 por hora); señalado en la revisión de código del 13 nov |
| La causa del fallo de Google no es del backend | Alta / medio | CA-07 solo exige diagnosticar y documentar; si es del frontend o de Render se decide con Andrés, no se promete un arreglo |
| Cambiar el constructor de `CrearReporteRequest` rompe pruebas existentes | Media / bajo | Se conserva el constructor corto |

## 6. Alternativas descartadas

- **Contadores en memoria (Caffeine o un mapa)** para el login: más simple, pero se pierden cuando Render duerme o reinicia. Un atacante que espere el reinicio recupera sus intentos.
- **Una librería de límites (Bucket4j, Resilience4j)**: es otra dependencia para algo que una fila y un `UPSERT` resuelven; el stack está fijado.
- **Bloqueo por IP**: detrás del proxy de Render la IP real no es de fiar, y quedó fuera en la spec.
- **Distancias como columna de `categoria`**: no deja registro de quién cambió qué (CA-27). Se usa una tabla con auditoría.
- **Claves `duplicado_distancia.<id>` en `configuracion`**: auditaría igual, pero no se puede validar con un `FOREIGN KEY` ni con el rango como `CHECK` por categoría.
- **Cola de envío con reintentos para el correo de verificación**: queda fuera de alcance en la spec; el reenvío manual cubre el fallo.
- **Caché de parámetros con vencimiento**: ahorraría una consulta, pero rompe «aplica de inmediato» o la complica.

## 7. Decisiones pendientes

- ~~Puntos de la historia~~ **Decidido (Andrés, 13 nov):** se queda en **5**; no se toca la estimación en Linear para no alterar el burn down ni la velocidad.
- ~~Reportes «Resueltos» y los duplicados~~ **Decidido (Andrés, 13 nov):** un propio «Resuelto» no cuenta como duplicado, pero repetirlo exige evidencia (RF-39), y las pantallas piden fotos recientes.
- ~~Avisar los intentos que quedan~~ **Decidido (Andrés, 13 nov):** sí, cuando queda uno (CA-40).
- ~~Quién afina los textos~~ **Decidido (Andrés, 13 nov):** solo con Andrés; Amelia hará después una revisión más completa. _Textos de partida (voz de la app, usted; se pulen en Figma):_
  - Bloqueo: «Por su seguridad, bloqueamos el ingreso con este correo durante 5 minutos porque hubo varios intentos fallidos. Podrá intentarlo de nuevo a las 3:42 p. m.»
  - Límite diario: «Hoy ya envió 2 reportes, que es el máximo por día. Podrá enviar otro desde mañana. Su reporte no se perdió: lo que escribió sigue aquí.»
  - Duplicado, parecido **de otra persona** ya enviado (hoja; el caso más común): título «Ya hay un reporte parecido». Texto «El 12 de noviembre una persona reportó en esta misma categoría: «Hueco grande frente a la panadería…». Ya lo enviamos a la entidad. Puede ser lo mismo que usted quiere reportar. Si es un problema distinto, puede enviarlo de todas maneras.» Botones «Enviar de todas maneras» y «Cancelar reporte». Si hay varios, cuenta cuántos y cita el más reciente: «Ya hay 3 reportes parecidos. El más reciente, del 12 de noviembre: «…»».
  - Duplicado, parecido **propio** ya enviado: título «Ya envió un reporte parecido». Texto «El 12 de noviembre usted reportó algo parecido en este mismo lugar (NU-000123) y ya lo enviamos a la entidad. Puede que sea lo mismo. Si es un problema distinto, puede enviarlo de todas maneras.» Con enlace «Ver el reporte NU-000123».
  - Duplicado, parecido aún sin enviar (propio o de otra persona): título «Ya hay un reporte parecido». Texto «El 12 de noviembre una persona reportó en esta misma categoría: «…». Todavía no se envía a la entidad. Si es otro problema, puede enviarlo de todas maneras.»
  - **Descripción** (hoy: título «Cuéntenos más», etiqueta «Descripción», ejemplo dentro de la casilla «Describa qué ocurre, desde cuándo y dónde.»). **Decidido por Andrés:** la guía va como **texto de ayuda siempre visible** bajo la casilla (no como placeholder, que desaparece al escribir). La casilla queda sin texto de ejemplo. De partida:
    - Título: «Cuéntenos qué pasa»
    - Etiqueta: «¿Qué problema quiere reportar?»
    - Ayuda, línea 1 (guía y ejemplo): «Aquí explicará el problema: qué está pasando, desde cuándo y a quién afecta. Ejemplo: Hay un hueco grande en la calle desde hace dos semanas y las motos frenan de golpe para esquivarlo.»
    - Ayuda, línea 2 (reglas): «Entre 10 y 500 caracteres. No hace falta repetir la dirección: ya marcó el lugar. No escriba datos de otras personas. Un resumen de lo que escriba, sin su nombre, puede mostrarse a otras personas que reporten lo mismo.» (la última frase solo mientras el aviso de duplicados esté activo)
  - **Foto**: etiqueta «Foto reciente del problema (opcional)» y ayuda «Una foto tomada hoy ayuda a que la entidad entienda y atienda el problema más rápido.» Obligatoria: «Foto reciente del problema (obligatoria)» y «En este lugar ya se resolvió un reporte parecido. Agregue una foto de hoy que muestre que el problema sigue.»
  - Foto necesaria: «En este lugar ya se resolvió un reporte parecido. Si el problema sigue o volvió, agregue una foto reciente que lo muestre.»
- ~~Aviso de fallo de red~~ **Decidido (Andrés, 13 nov):** se deja como está en la spec: cualquier fallo de red dice «No hay conexión a internet», aunque la causa sea que NotiUrba no responde. Se descartó distinguirlo con `navigator.onLine`.
- ~~Orden del límite diario y del duplicado~~ **Decidido (Andrés, 13 nov):** primero el límite diario y después el duplicado.

## 8. Aprobación

- [x] Cada criterio de `spec.md` tiene un cambio técnico y una prueba.
- [x] No introduce otro framework ni otra base de datos.
- [x] Contrato de API y migración revisados.
- [x] **Aprobado por el usuario (13 nov 2026)** → se puede pasar a `tasks.md`.
