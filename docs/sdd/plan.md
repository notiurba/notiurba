# Plan técnico — Correcciones de cierre QA y despliegue

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se implementará la [especificación aprobada](spec.md).
> No se escribe código ni se cambia producción hasta que este plan esté **Aprobado**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado** (Borrador → En revisión → Aprobado) |
| Especificación | [spec.md](spec.md) (aprobada el 2026-11-16) |
| Historia / ticket | Sin historia de Linear; correcciones de cierre QA |
| Aprobado por / fecha | Andrés, 2026-11-16 |
| Última actualización | 2026-11-17 (§3.2 y §3.5 ajustados tras la revisión de código) |

## 1. Enfoque general

La intervención se separa en tres puertas para no diagnosticar producción mientras el código todavía cambia:

1. **Corrección local y pruebas automatizadas.** El frontend deja propagar los errores del API, el mapa reutiliza el patrón de carga/reintento que ya usa el listado administrativo y la recuperación conserva una respuesta pública genérica, pero deja trazabilidad segura y limpia el token si Resend no acepta el correo.
2. **Validación aislada.** Se prueban errores de red, respuestas HTTP, fallo del proveedor, persistencia, historial y eliminación con dobles de prueba y PostGIS desechable. No se usa Neon ni se envía correo real.
3. **Producción controlada.** Después de integrar y desplegar el commit aprobado, se verifican versión, variables por nombre, recuperación real, reportes, fotografías, buzón QA y `NU-000010`. Se usan únicamente las identidades autorizadas, nombradas en la evidencia como `Cuenta QA-A`, `Cuenta QA-B` y `Administrador QA`.

No se repiten la campaña móvil ni las 35 solicitudes temporales de Postman. Solo se ejecutan los casos dirigidos de esta especificación.

## 2. Stack y límites fijados

Se conserva Java 21 + Spring Boot 3.3, PostgreSQL/PostGIS, React 19 + Vite 8, Cloudinary, Resend, Netlify, Render y Neon. No se agrega framework, proveedor ni servicio de pago.

- Gestor frontend: `pnpm`; no se genera `package-lock.json`.
- La respuesta de `POST /api/auth/recuperar` seguirá siendo idéntica para cuenta tradicional, solo Google e inexistente.
- `RESEND_PRUEBA_DESTINO` permanece configurada para los correos de reportes.
- Los correos de recuperación se envían al correo propio de la cuenta QA; no pasan por `RESEND_PRUEBA_DESTINO`.
- Los secretos y las direcciones autorizadas no se escriben en documentos, comandos versionados, capturas ni logs.
- No se modifican datos ajenos a las cuentas QA autorizadas.

## 3. Cambios por capa

### 3.1 Base de datos

- **Sin migración nueva.** Se reutilizan `token_verificacion`, `historial_estado`, `registro_envio_correo` y `evidencia_reporte`.
- No se edita ninguna migración aplicada.
- En pruebas locales se usa `BaseDeDatosTemporal` sobre PostGIS desechable.
- Neon se usa únicamente durante la validación controlada mediante las API existentes; no se ejecutan sentencias SQL manuales sobre producción para fabricar resultados.

### 3.2 Backend — recuperación de contraseña

Archivos principales:

- `auth/recuperacion/RecuperacionServicio.java` (solo encola)
- `auth/recuperacion/EnvioRecuperacionPendiente.java` (trabajo en segundo plano)
- `auth/recuperacion/PersistenciaRecuperacion.java` (transacciones cortas e independientes)
- `auth/recuperacion/EnviadorEnlaceRecuperacionCorreo.java`
- `usuario/TokenVerificacionRepository.java` y `usuario/UsuarioRepository.java` (bloqueo de la cuenta)
- `config/SegundoPlanoConfig.java` (cola propia de recuperación)

Flujo (ajustado el 2026-11-17 tras la revisión del `code-reviewer`):

1. La petición HTTP solo genera el token, calcula su hash y encola la tarea. Hace el mismo trabajo para cualquier correo y no consulta la base, para que ni la respuesta ni su duración revelen si la cuenta existe.
2. En el hilo de fondo se comprueban la cuenta tradicional y el límite de tres solicitudes por hora (con bloqueo de la cuenta) y se crea el token nuevo como **pendiente y no utilizable** en una transacción independiente, antes de llamar al proveedor; nunca se registra el token ni el enlace. `usado_en = expira_en` es la marca inequívoca compatible con las restricciones actuales.
3. La recuperación usa su propia cola (`tareasRecuperacion`: 2 hilos y 20 puestos), separada de la del correo de verificación: `/recuperar` es público y un abuso no debe dejar sin correo a quien se registra. Si esa cola está llena, la tarea se descarta con un aviso, igual que el correo de verificación: no queda nada guardado ni se consume cupo, y la persona puede volver a pedir el enlace.
4. Si Resend **acepta** el correo, conservar el token nuevo e invalidar los tokens anteriores en otra transacción independiente y atómica que deja activo el hash recién creado.
5. Si Resend **rechaza** o no puede recibir la solicitud, eliminar el token nuevo en una transacción independiente y conservar el enlace anterior que todavía fuera válido. Si Resend no responde dentro del tiempo de espera, se trata igual que un rechazo; como pudo haber aceptado el correo, este puede llegar con un enlace que ya no sirve. Es seguro (no gasta cupo y se puede pedir otro), pero en QA no debe confundirse con un fallo del backend. La respuesta HTTP pública no cambia. Un fallo de esta limpieza se registra por separado, se reintenta y cualquier pendiente residual de más de cinco minutos se reconcilia antes de una nueva solicitud. Como el pendiente nace cuando la tarea empieza a correr, la espera en la cola no cuenta para esos cinco minutos.

`TokenVerificacionRepository` conservará `invalidarVigentes`, utilizado por la verificación de correo, y agregará una operación específica y atómica que active el token aceptado e invalide los demás tokens de recuperación vigentes. La preparación, confirmación y limpieza usarán límites transaccionales independientes para que el token ya sea visible cuando se llame al proveedor y un fallo de envío no cuente dentro del límite horario.

La trazabilidad utilizará eventos de log sin correo, contraseña, token ni URL:

- solicitud omitida por cuenta no elegible;
- solicitud omitida por límite;
- envío aceptado, con identificador de proveedor;
- envío fallido, con el tipo de fallo, si es reintentable y el código HTTP del proveedor (nunca el cuerpo de su respuesta, que puede traer correos);
- correo enviado cuyo enlace no quedó activo (por ejemplo, porque el pendiente ya se había reconciliado).

El identificador de Resend se usa solo como evidencia operativa; no se crea una tabla nueva para correos de autenticación.

### 3.3 API

No se crean endpoints ni cambia el cuerpo del contrato:

| Método y ruta | Cambio observable |
| --- | --- |
| `POST /api/auth/recuperar` | Ninguno: siempre devuelve el mismo `200 { mensaje }` para impedir enumeración |
| `POST /api/auth/restablecer` | Ninguno: conserva `204` y `400 ENLACE_INVALIDO` |
| `GET /api/admin/reportes` | Ninguno; el frontend dejará de ocultar sus errores |
| Endpoints de reportes, fotos, envío, actualización y administración | Sin cambios; se usarán en QA dirigido |

`docs/contexto/11-contrato-api.md` se ajustará solo para documentar que un envío de recuperación rechazado no deja un token vigente ni consume el cupo. No se documentarán detalles internos que permitan enumerar cuentas.

### 3.4 Frontend — mapa administrativo

Archivos principales:

- `admin/servicios/reportesService.js`
- `admin/paginas/MapaComunas.jsx`
- `admin/paginas/MapaComunas.css`
- `admin/datos/reportesPrueba.js` y pruebas geográficas que lo usan

Cambios:

- Quitar de `reportesService.js` el `catch` que devuelve `reportesPrueba`; un `ErrorApi` o error de red se propagará a la pantalla.
- Mantener `reportesPrueba.js` únicamente como fixture de las pruebas geométricas existentes. Ningún módulo de producción lo importará, por lo que Vite no lo incluirá en los chunks desplegados.
- Reemplazar el `useEffect` manual del mapa por `useCargar(obtenerReportesCiudadanos)`, igual que el listado `Reportes.jsx`. La decisión entre carga, error y mapa vive en `admin/componentes/CargaReportesMapa.jsx` (sin Leaflet), para que la prueba de «Reintentar» ejercite el código real de la página. Esa prueba usa `react-test-renderer` como dependencia de desarrollo, fijada en la misma versión que `react` (está deprecado en React 19; anotado en `08-pendientes.md`).
- Mostrar carga inicial, estado de error seguro y botón «Reintentar». Durante el reintento no se dibujarán puntos anteriores ni datos ficticios.
- El mensaje visible usará el texto sanitizado de `ErrorApi` cuando sea útil y un texto genérico para errores desconocidos; nunca mostrará HTML o detalles del servidor.
- Retirar el texto que afirma que los reportes son de demostración cuando el servidor no responde.
- Actualizar `frontend/src/simulacion/README.md`: la simulación explícita sigue disponible con `VITE_USAR_MOCKS=true`, pero ya no existe fallback automático en producción.

No se cambia a `/api/admin/mapa` dentro de esta corrección: integrar filtros y el contrato completo de ese endpoint corresponde al alcance funcional del mapa, mientras este defecto se limita a no falsificar datos cuando falla `/api/admin/reportes`.

### 3.5 Netlify — distintivo móvil

La primera opción es operacional, no CSS: en Netlify se revisará `Project configuration > General > Powered by Netlify badge` y se apagará el distintivo si el plan lo permite. Netlify documenta que el distintivo se inyecta en un frame aislado, que el CSS del proyecto no puede modificarlo y que el cambio se aplica sin redesplegar: [Powered by Netlify badge](https://docs.netlify.com/manage/projects/powered-by-netlify-badge/).

Si la opción está bloqueada por el plan gratuito:

- se medirá el área ocupada por el distintivo a 360 px;
- `Reportar.css` reservará en pantallas de hasta 480 px una franja inferior de 64 px para que la barra sticky y «Continuar» queden por encima del frame. Se aplica al flujo de reporte (pasos 1 a 3), que es lo que cubre CA-04; login y registro quedan fuera de esta corrección. La reserva se aplica aunque el distintivo no esté visible (por ejemplo, en localhost); si T-22 logra apagarlo, se revisa si sigue siendo necesaria;
- se comprobarán los pasos de categoría, ubicación y detalles, con y sin teclado abierto;
- no se bloqueará el script con CSP ni se dependerá de que cada visitante oculte el distintivo manualmente.

### 3.6 Integraciones y configuración

En Render se comprobarán, solo por nombre y estado configurado, `RESEND_API_KEY`, `RESEND_REMITENTE` y `RECUPERACION_ENLACE`. La ruta de recuperación deberá corresponder a la pantalla desplegada. Se retirará el modo heredado `RECUPERACION_MOSTRAR_ENLACE`: ningún entorno escribirá el enlace o token en logs.

Antes de QA se registrarán:

- rama y commit publicado por Netlify;
- rama y commit publicado por Render;
- confirmación de `RESEND_PRUEBA_DESTINO` para reportes;
- confirmación de que el envío de reportes está habilitado durante los dos casos dirigidos;
- estado de salud y CORS mediante `tools/verificar-despliegue.sh`.

Si Netlify y Render no ejecutan el mismo commit aprobado, se detiene la validación. Cualquier cambio de rama o variable se hará en la fase operativa con Andrés y se verificará con un nuevo despliegue; no se inferirá la versión desde `/api/health`.

### 3.7 Evidencias y documentación

Se creará `docs/validacion/correcciones-cierre-qa.md` con:

- fecha y hora de Colombia;
- entorno, rama y commit;
- criterio ejecutado y resultado;
- referencia a captura o evento del proveedor sin datos sensibles;
- dato creado y limpieza realizada, usando solo códigos de reporte y etiquetas QA;
- pruebas omitidas y motivo.

Se actualizarán las afirmaciones obsoletas sobre fallback ficticio en `docs/contexto/12-estado-actual-y-traspaso.md` y `docs/contexto/11-contrato-api.md`. NOT-133 seguirá pausada y no se marcará terminada.

## 4. Estrategia de pruebas

### 4.1 Automatizadas y aisladas

| Criterio | Tipo | Prueba prevista |
| --- | --- | --- |
| CA-01, CA-02 | Frontend/API simulada | Prueba de `reportesService`: éxito normalizado y propagación de red, 401, 403 y 500; validación manual del estado y reintento del mapa |
| CA-03 | Build | `pnpm build` y búsqueda en `dist` de códigos, descripciones y coordenadas de `reportesPrueba` |
| CA-05 a CA-08 | Backend unitaria e integración | `RecuperacionServicioTest`, `EnviadorEnlaceRecuperacionCorreoTest`, `RecuperacionControllerTest`, `SegundoPlanoConfigTest` y, con PostGIS desechable, `RecuperacionIntegracionTest` (aceptación, fallo, limpieza, límite, enlace anterior y rollback) y `RecuperacionAsincronaIntegracionTest` (la respuesta no espera al proveedor) |
| CA-06 | Backend existente | Reutilizar y ampliar las pruebas de token de un uso, vencimiento y desbloqueo de login |
| CA-07 | Controller | Respuesta idéntica para tradicional, solo Google e inexistente |
| CA-10, CA-12, CA-13, CA-14 | Integración | Suites existentes de fotos, consulta, actualización, administración y eliminación sobre `BaseDeDatosTemporal` |
| CA-11 | QA local sin envío | `tools/qa-correo-reporte/qa-correo.sh`, con envío externo apagado y base desechable |
| Panel y mapas | QA local | `tools/qa-panel-admin/qa-panel.sh`, sin producción ni correo externo |

Comandos finales obligatorios:

```bash
cd frontend
pnpm install
pnpm test
pnpm lint
pnpm build

cd ../backend
mvn clean test
```

La corrida backend se hará con PostGIS desechable. Se informarán pruebas ejecutadas, fallidas y omitidas; una prueba omitida por falta de proveedor real no contará como aprobada. `MigrationIntegrationTest` se ejecutará en entorno limpio aunque no haya migración nueva, para descartar regresiones.

### 4.2 Navegador antes del despliegue

- Vista de 360 px y escritorio del mapa administrativo.
- Error controlado de red y HTTP, sin marcadores ficticios.
- Reintento que recupera los reportes reales.
- Botón «Continuar» en pasos 1, 2 y 3, con distintivo visible si no se pudo desactivar.
- Sesión ADMIN y sesión ciudadana separadas para evitar confundir permisos o caché.

### 4.3 Producción controlada

Orden de ejecución:

1. Confirmar commit, salud, CORS y configuración por nombre.
2. Verificar recuperación con `Cuenta QA-A`: una solicitud, evento aceptado en Resend, recepción, apertura, cambio y reutilización rechazada.
3. Verificar respuesta genérica con una dirección inexistente reservada; no se intentará convertir una cuenta personal en solo-Google para probar ese caso, porque ya queda cubierto automáticamente.
4. Crear con `Cuenta QA-B` un reporte de Alcaldía con foto y otro de Centroaguas; consultar reporte y fotografía por API.
5. Confirmar en Resend y en el buzón QA que ambos correos fueron desviados y que no hubo destinatario real.
6. Consultar `NU-000010` con `Administrador QA`; verificar aviso, detalle e historial. Solo si el aviso y el propietario esperado existen, registrar oficialmente «Resuelto» y comprobar «Mis reportes» con la cuenta propietaria.
7. Probar eliminación de datos y de cuenta únicamente sobre la preparación QA destinada a ello. Antes de cada `DELETE` se resolverán el usuario y los códigos exactos; después se comprobará que no afecta otros reportes.
8. Ejecutar `tools/verificar-despliegue.sh` de cierre y completar la matriz de evidencia.

Las contraseñas se introducirán de forma interactiva o mediante variables efímeras y no aparecerán en historial, archivos o respuestas.

## 5. Riesgos y mitigaciones

| Riesgo | Probabilidad / impacto | Mitigación |
| --- | --- | --- |
| Un correo de reporte llega a una entidad real | Baja / alto | Confirmar `RESEND_PRUEBA_DESTINO` antes de crear o enviar; detenerse si no puede comprobarse |
| Se elimina información personal o de otra cuenta | Baja / alto | Usar preparación QA separada, resolver IDs antes del `DELETE`, comprobar propietario y no usar SQL directo |
| La cuenta administradora o ciudadana queda sin acceso | Media / alto | No cambiar correo ni rol; recuperación solo sobre QA-A; conservar una sesión separada y probar la clave nueva antes de cerrar |
| Tres intentos previos bloquean la recuperación | Media / medio | Revisar ventana por eventos/logs, hacer una sola solicitud real y esperar el intervalo si el límite ya se consumió |
| Resend acepta pero el buzón clasifica como spam | Media / medio | Correlacionar identificador del proveedor, estado de entrega y carpetas del buzón antes de diagnosticar backend |
| El distintivo no puede apagarse en el plan gratuito | Media / bajo | Reserva móvil de espacio medida y validada a 360 px |
| Render está en otro commit o tarda en despertar | Alta / medio | Confirmar commit en el panel y separar sueño, despliegue y fallo antes de ejecutar casos |
| Los logs revelan cuentas o tokens | Baja / alto | Eventos sin correo/token/URL; proveedor ID solo en aceptación; revisión de logs en pruebas |
| `NU-000010` no existe o no pertenece a una cuenta autorizada | Media / medio | Tratarlo como precondición; no fabricar ni reasignar datos para hacer pasar el criterio |

## 6. Alternativas descartadas

- **Entorno Render/Neon separado:** descartado por tiempo; la validación real se hará en producción controlada y los fallos intencionales localmente.
- **Mostrar al cliente que Resend falló:** descartado porque revelaría que la cuenta existe.
- **Cambiar a otro proveedor de correo:** contradice ADR-0002 y no ataca la causa.
- **Bloquear el distintivo con CSP:** frágil y ajeno a la configuración oficial; puede afectar otros scripts.
- **Ocultar el distintivo solo en el navegador de QA:** no corrige el problema para otros usuarios.
- **Cambiar el mapa a `/api/admin/mapa` en esta corrección:** amplía el alcance funcional; primero se elimina el fallback falso y se conserva el contrato actual.
- **Guardar correos de QA en documentos o scripts:** innecesario y contrario a la protección de datos.

## 7. Decisiones cerradas

- Producción actual será el entorno QA real autorizado.
- Los errores intencionales, pruebas destructivas preliminares y proveedores fallidos se validan localmente.
- Las identidades concretas se mantienen fuera del repositorio y se referencian mediante etiquetas QA.
- No se repiten pruebas móviles ni la colección temporal de Postman.
- Se usa un solo PR para toda esta corrección desde `codex/correcciones-qa-despliegue` hacia `develop`.

## 8. Aprobación

- [x] Cada criterio de `spec.md` tiene un cambio técnico o una verificación concreta.
- [x] No introduce otro framework, proveedor ni base de datos.
- [x] No requiere migración ni cambia el cuerpo público de la API.
- [x] Las operaciones de producción están separadas de las pruebas con fallos intencionales.
- [x] **Aprobado por el usuario** → se puede redactar `tasks.md`.
