# Enrutamiento de correos y flujo ciudadano

Fecha: 6 de noviembre de 2026. Rama: `feat/enrutamiento-correos-notiurba`.

Actualización: las correcciones y los resultados posteriores (Java 21, build frontend y SQL real) están en [la auditoría del 6 de noviembre](AUDITORIA_CORRECCIONES_2026-11-06.md). Las limitaciones descritas al final de este documento corresponden a la primera implementación.

## Base y alcance

Worktree limpio: `C:\notiurba\enrutamiento-correos`, desde `origin/main` local `c18c3fd08a041ada4134816c5dd4e82de6ea20fe`. La consulta remota encontró `20ae16999c41d24a5f15715a46eb4c254a356dbf`: tres commits posteriores que agregan únicamente los tres archivos del aviso de privacidad. `fetch` falló por DNS; no se afirma sincronización con ese commit. No se importaron otras ramas, no se tocó el aviso ni se modificaron main o la rama de integración. No se crea PR ni se hacen envíos reales en esta tarea.

La base tenía el adaptador HTTP de Resend, persistencia de reportes/fotos y consulta por código, pero solo pantallas de ingreso, diagnóstico y un marcador de PWA. Se reutilizan esas piezas. Se agrega el flujo de reportes al mismo router, sin reconstruir el registro ni el mapa.

## Configuración aplicada

V13 reutiliza las 14 categorías existentes y mantiene la categoría histórica 15, sin ofrecerla en el nuevo catálogo de enrutamiento. No se eliminan categorías, canales ni evidencias. `configuracion_enrutamiento` tiene una fila por categoría y referencias a canales existentes; sus opciones son editables en BD mediante un procedimiento autorizado, sin desplegar Java ni añadir condiciones por cada categoría. No se incorpora panel administrativo.

| Código | ID actual | Categoría | Entidad / dependencia | Correo de referencia | Modo inicial |
|---|---:|---|---|---|---|
| A01 | 1 | Vías, huecos, andenes e infraestructura | Alcaldía / Hábitat e Infraestructura | servicioalciudadano@tulua.gov.co | Correo CAIC |
| A02 | 2 | Alumbrado, espacio público y convivencia | Alcaldía / Gobierno, Convivencia y Seguridad | servicioalciudadano@tulua.gov.co | Correo CAIC |
| A03 | 3 | Basuras, árboles y afectaciones ambientales | Alcaldía / SEDAMA | servicioalciudadano@tulua.gov.co | Correo CAIC |
| A04 | 4 | Señalización vial, semáforos y movilidad | Alcaldía / Movilidad | servicioalciudadano@tulua.gov.co | Correo CAIC |
| A05 | 5 | Riesgos sanitarios y salud pública | Alcaldía / Salud | servicioalciudadano@tulua.gov.co | Correo CAIC |
| A06 | 6 | Uso del suelo y licencias urbanas | Alcaldía / Planeación | servicioalciudadano@tulua.gov.co | Correo CAIC |
| A07 | 7 | Infraestructura y servicios educativos | Alcaldía / Educación | servicioalciudadano@tulua.gov.co | Correo CAIC |
| A08 | 8 | Desastres, inundaciones y deslizamientos | Alcaldía / Gestión del Riesgo | servicioalciudadano@tulua.gov.co | CAIC; competencia propuesta |
| A09 | 9 | Vivienda | Alcaldía / Área de Vivienda | servicioalciudadano@tulua.gov.co | CAIC; competencia propuesta |
| A10 | 10 | Otro / no clasificado | Alcaldía / CAIC | servicioalciudadano@tulua.gov.co | CAIC; marcar revisión |
| E01 | 11 | Acueducto y alcantarillado | Centroaguas | notificacionespqr@centroaguas.com | Sin correo confirmado: PBX (602) 231 7300 |
| E02 | 12 | Gas natural | Gases de Occidente | inquietudes@gdo.com.co | Correo solo no urgente; emergencia: 164 |
| E03 | 13 | Energía eléctrica | Celsia | servicioalcliente@celsia.com | Zona Clientes, sin envío automático |
| E04 | 14 | Recolección de basuras y barrido | Veolia Aseo Tuluá | pqr.tulua@veolia.com | Página institucional de atención, sin envío automático |

Las dependencias muestran los nombres institucionales ya guardados en la BD, no los nombres abreviados de esta tabla. Las diez municipales resuelven exclusivamente al correo principal de la Alcaldía, nunca al correo de la secretaría. La matrícula se ofrece opcionalmente a Centroaguas; no se elimina la compatibilidad previa del backend para matrículas de otras empresas.

Fuentes consultadas: [CAIC](https://tulua.gov.co/documentos/210/servicio-al-publico-normas-formularios-y-protocolos-de-atencion/), [GdO](https://www.gdo.com.co/Paginas/nuestros-canales-de-comunicacion.aspx), [GdO, correo no urgente](https://www.gdo.com.co/Paginas/Suspension.aspx), [Centroaguas](https://www.centroaguas.com/index.php/contactenos), [Celsia](https://www.celsia.com/es/zona-clientes/), [Veolia Tuluá](https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua).

La consulta de fuentes no demuestra entrega efectiva de un buzón. Centroaguas y Veolia permanecen bloqueadas para correo; no se sustituyen los correos de referencia por direcciones judiciales. Revisar nuevamente los canales y las competencias A08/A09 antes de habilitar un despliegue con envíos reales.

## API e integración

- `GET /api/enrutamiento/categorias`: catálogo de las 14 rutas, entidad, dependencia, canal disponible y advertencias.
- `GET /api/enrutamiento/categorias/{id}?emergencia=true|false`: vista previa de la decisión.
- `GET /api/reportes/opciones/barrios`: reutiliza el catálogo territorial disponible, sin inventar barrios.
- `POST /api/reportes`: se conserva el contrato, añadiendo `emergencia` opcional. Exige la autorización existente. Marca A10 para revisión; detecta conservadoramente términos de fuga/olor a gas para impedir que desmarcar la emergencia habilite el correo.
- `POST /api/reportes/{codigo}/fotos`: se reutiliza sin cambios.
- `POST /api/reportes/{codigo}/enviar`: implementa el endpoint reservado en el contrato; responde 202 con el estado consultable, no una confirmación de entrega. Siempre verifica propietario y autorización persistida.
- `GET /api/reportes/{codigo}/envio`: resultado del correo y ruta, motivo seguro, rebote y reintentabilidad. No devuelve payload ni errores técnicos.
- `GET /api/reportes/{codigo}`: se conserva la consulta existente.
- `GET /api/reportes`: hasta 100 reportes propios recientes, para enlazar Mis reportes.
- `POST /api/webhooks/resend`: solo eventos con firma Svix válida; guarda rebotes/quejas de forma idempotente. Tiene una cadena de seguridad limitada a esta ruta y no altera la autenticación de las demás.

Frontend: `/app/crear-reporte`, `/app/consultar-reporte?codigo=NU-000042`, `/app/mis-reportes`, accesibles desde el inicio ciudadano. Se usan el cliente HTTP y la protección de sesión existentes. Las fotos se suben antes de solicitar envío; si falla una operación posterior al guardado, se conserva el código y no se vuelve a crear el reporte. La consulta permite completar fotos antes de solicitar por primera vez el envío.

## Estados, cola y duplicados

| Situación | Reporte | Correo |
|---|---|---|
| Guardado sin solicitar envío | Recibido | SIN_SOLICITAR (respuesta de consulta, sin intento) |
| Encolado | Recibido | PENDIENTE |
| Proveedor no confirma | Recibido | FALLIDO |
| Resend devuelve identificador positivo | Enviado | ENVIADO |
| Rebote/queja firmado posterior | Enviado o estado posterior, sin retroceso | FALLIDO, rebote=true |
| Canal alternativo/emergencia | Conservado | NO_ENVIADO_CORREO, modo EXTERNO |

La cola usa el registro de envíos persistente, bloqueo corto `FOR UPDATE SKIP LOCKED` y marca de procesamiento. No mantiene una transacción abierta durante HTTP. Cada reintento permitido crea otro registro de intento, pero reutiliza el mensaje congelado y la misma clave de idempotencia del proveedor. El bloqueo del reporte evita dos solicitudes simultáneas. Un correo ya aceptado no vuelve a encolarse. La ventana de reintento se limita conservadoramente a 23 horas desde el primer intento; se comprueba también en el trabajador. Después exige conciliación manual para evitar duplicados fuera de la ventana del proveedor.

Ante caída del proceso durante HTTP o fallo de BD después de aceptación, el intento permanece PENDIENTE/procesando. No se libera automáticamente: el responsable debe conciliar el identificador/resultado con Resend antes de habilitar un reintento, especialmente pasado el plazo. No se promete entrega exactamente una vez ante todos los fallos distribuidos.

El mensaje se congela al solicitar el envío: contiene datos reales del reporte y del propietario autenticado, autorización, evidencias y Reply-To. Asunto exacto `NotiUrba #[codigo]`. Incluye texto plano y HTML de tablas con estilos inline; escapa valores, permite enlaces HTTPS de Cloudinary y OpenStreetMap y no incorpora credenciales. Los campos opcionales ausentes se indican como «No aportado». No se envía copia automática al ciudadano.

Los eventos de rebote recibidos antes de guardar el ID del proveedor también quedan registrados y se aplican al confirmar la aceptación. Los eventos repetidos no duplican cambios; no insertan un retroceso del historial. El aviso al ciudadano se muestra en su consulta, actualizada cada cinco segundos; no se agrega un segundo correo de aviso en esta tarea.

## Configuración de despliegue

El trabajador está deshabilitado por defecto. Tras revisar canales, aplicar V13 mediante el proceso de despliegue autorizado y configurar `NOTIURBA_ENVIO_REPORTES_HABILITADO=true`, junto con las variables existentes `RESEND_API_KEY` y `RESEND_REMITENTE`. Configurar `RESEND_WEBHOOK_SECRET` y el endpoint del webhook en el proveedor requiere una acción operativa posterior; no se configuró ningún proveedor aquí. La aplicación puede conservar reportes y encolar mientras la cola está deshabilitada, pero el correo no saldrá hasta habilitarla.

No desplegar ni aplicar las migraciones a producción hasta completar la compilación Java 21, pruebas integrales y validación visual pendientes. No se han modificado credenciales, dominios ni bases remotas.

## Diseño y verificación

Se consultaron los seis nodos de Figma indicados (`232:895`, `232:930`, `232:968`, `232:1003`, `232:1041`, `232:1078`). Se trasladaron textos, estados, colores, tarjetas, acciones y medidas principales a CSS del proyecto; no se añadió Tailwind ni TypeScript a una aplicación JavaScript. Hay reglas responsive, campos etiquetados, foco visible y avisos accesibles.

La descarga de SVG de Figma falló por DNS. La iconografía y la fuente local Nunito Sans no se pudieron integrar/verificar: se emplean controles textuales y la fuente de sistema como respaldo. No se afirma equivalencia visual completa ni se fabrican capturas.

Resultados ejecutados:

- `node --test frontend/src/app/componentes/estadoCorreo.test.mjs`: cinco pruebas aprobadas.
- Análisis sintáctico Babel del flujo: aprobado.
- `scripts/verificar-correo-ui.mjs`: Oxc compila el componente real y React SSR verifica cinco estados con un adaptador de Link de prueba. No sustituye navegador ni prueba de interacción.
- `git diff --check`: sin errores.
- ESLint del flujo y script de comprobación, con las reglas existentes: cero errores y cero advertencias.
- JUnit directo con las clases reales `PlantillaReporte`, `FirmaWebhook` y `CorreoSaliente`, compiladas con el JDK 17 disponible: tres pruebas aprobadas (campos/escape/asunto, autorización/enlaces y firma/replay). No equivale a compilar la aplicación con Java 21 ni a ejecutar el flujo PostGIS.
- `javac` sobre 19 archivos reales del correo, cambios de reportes y pruebas añadidas, con las dependencias locales y JDK 17: compilación de comprobación aprobada, una advertencia sobre procesamiento de anotaciones de fuentes implícitas. No se alteró la versión Java del proyecto y no sustituye `mvn test` con Java 21.
- Build frontend completo: bloqueado por dependencias no disponibles en el worktree. La instalación offline encontró la versión de pnpm del repositorio no disponible y restricciones para crear enlaces. No se alteró el manifiesto ni el lockfile para ocultarlo.
- Maven offline: bloqueado por `spring-security-oauth2-jose:6.3.4` ausente en la caché. Solo se encontró JDK 17 ejecutable, no se completó compilación Java 21.
- Pruebas PostGIS: escritas, no ejecutadas por no disponer de una BD desechable configurada. No se usó Neon ni producción.

Pruebas de integración: `EnrutamientoIntegracionTest` exige `NOTIURBA_TEST_DB_URL` con nombre de BD `notiurba_test...`, usuario y contraseña de pruebas mediante `NOTIURBA_TEST_DB_USER` / `NOTIURBA_TEST_DB_PASSWORD`. Aplica Flyway en esa BD, crea datos sintéticos y usa un proveedor Mockito. Comprueba 14 rutas, CAIC, gas, canal externo, Celsia, aceptación, conservación tras fallo, idempotencia, propiedad y rebote firmado sin retroceso. No configurar estas variables con una BD real. Ejecutar `mvn test` con Java 21 y dependencias resueltas; sin estas variables las pruebas de integración se omiten expresamente.

Pendientes verificables: compilar backend y frontend completos; ejecutar pruebas PostGIS; configurar/ensayar webhook y recuperaciones en ambiente aislado; comprobar móvil/escritorio en navegador e incorporar los iconos y fuente definitivos; validar canales de correo aún no confirmados y competencias propuestas. La implementación queda preparada en una rama local, sin PR ni fusión.
