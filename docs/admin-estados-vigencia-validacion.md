# Validación final: estados, historial y vigencia administrativos

Fecha: 6 de noviembre de 2026, hora de Colombia.
Repositorio: `C:\notiurba\noti-urba`.
Rama: `feat/admin-estados-vigencia`.
Base: `main`, commit `df3bdaf634f14850c84cf8b083f41ae3a0cdb7ea`.

## Funcionalidad entregada

| Endpoint | Resultado |
| --- | --- |
| PATCH `/api/admin/reportes/{codigo}/estado` | Exige ADMIN, valida transiciones y observación, registra actor autenticado y fecha, evita duplicados y serializa cambios concurrentes con bloqueo de fila. |
| GET `/api/admin/configuracion/vigencia` | Lee la configuración existente, inicialmente 30 días según V3. |
| PUT `/api/admin/configuracion/vigencia` | Valida entero JSON positivo, registra administrador y fecha de actualización y solo afecta a reportes futuros. |

Se respetan los saltos hacia adelante documentados, se rechazan retrocesos y
modificaciones de RESUELTO, y RECIBIDO → ENVIADO se reserva a la aceptación
automática de correo. Las observaciones adicionales conservan el estado mientras
no sea terminal; repetir el último asiento del mismo administrador no lo duplica.

Se reutilizan las tablas y triggers B4/B9/B11. No se crean ni editan migraciones.
La vigencia individual permanece congelada al crear el reporte. Los vencidos
quedan fuera de `v_reporte_mapa`, pero siguen disponibles para consulta y gestión;
ni el estado ni los reintentos de correo reinician su vigencia. No se implementan
mapas ni endpoints individuales de vigencia adicionales.

## Evidencia local verificada

El usuario ejecutó `docs/admin-estados-validar-local.ps1` desde PowerShell de
Windows con Java 21 y Docker Desktop operativo. La fase JDBC utiliza exclusivamente
un contenedor PostGIS desechable, credenciales sintéticas y un puerto limitado a
127.0.0.1. BaseDeDatosTemporal crea una BD nueva en ese servidor; Flyway aplica
las migraciones existentes. No se lee `.env` ni se conecta a producción.

Los proveedores externos de la integración son mocks. La cola está apagada en
pruebas y los valores de correo/fotos son sintéticos, sin claves auténticas.
El caso de reintento de correo utiliza su propio proveedor mock.

Se revisaron los XML locales, sin repetir la suite ni modificar el código:

| Fase administrativa | Aprobadas | Fallos | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: |
| AdminControllerTest | 32 | 0 | 0 | 0 |
| AdminIntegracionTest, PostgreSQL/PostGIS real | 13 | 0 | 0 | 0 |
| ConfiguracionIntegracionTest | 2 | 0 | 0 | 0 |
| **Total** | **47** | **0** | **0** | **0** |

Las 13 integraciones forman parte de las 47 anteriores; no son 13 pruebas
adicionales. Cubren persistencia del historial, avances y saltos, autorización,
30 días, cambios globales, vencimiento exacto, consulta/gestión de vencidos,
reintento de correo y concurrencia.

La fase general registró **156 pruebas: 137 aprobadas, 0 fallos, 0 errores y
19 omitidas**. Las **7 pruebas de ErroresHttpRealTest aprobaron**. El usuario
confirmó **BUILD SUCCESS** y el JAR ejecutable generado, presente en
`backend/target/backend-0.0.1-SNAPSHOT.jar`.

## Razones de las 19 omisiones de la fase general

El script conserva primero los XML administrativos y retira después las variables
de conexión antes de la fase general. Así evita que esta segunda fase dependa
de una BD externa. Las condiciones de JUnit son las existentes; no se excluyeron
clases o métodos para obtener un resultado verde.

| Clase | Omitidas registradas | Razón comprobada en Surefire |
| --- | ---: | --- |
| AdminIntegracionTest | 9 | DB_URL vacía en la segunda fase. Ya aprobó sus 13 ejecuciones reales en la fase administrativa; sin BD el método parametrizado se registra como un único método omitido. |
| EnrutamientoIntegracionTest | 7 | NOTIURBA_TEST_DB_URL vacía; estas integraciones de correo no se ejecutan en la fase general. |
| MigracionEnrutamientoTest | 1 | NOTIURBA_TEST_DB_URL vacía. |
| EnviadorCorreoResendRealTest | 1 | RESEND_API_KEY vacía; no se envía correo real. |
| AlmacenFotosCloudinaryRealTest | 1 | CLOUDINARY_CLOUD_NAME no definida; no se suben fotos reales. |
| **Total** | **19** | **Omitidas, no aprobadas.** |

Otras clases históricas que dependen de DB_URL pueden quedar abortadas antes de
descubrir sus métodos y registrar cero ejecuciones. No se presentan como pruebas
ejecutadas ni certificadas por esta entrega.

Evidencias revisadas, conservadas localmente pero excluidas del commit:

- `backend/target/admin-local-AdminControllerTest.xml`.
- `backend/target/admin-local-AdminIntegracionTest.xml`.
- `backend/target/admin-local-ConfiguracionIntegracionTest.xml`.
- `backend/target/surefire-reports/TEST-*.xml` de la fase general.
- JAR y demás resultados generados bajo `backend/target/`.

## Alcance de los archivos de entrega

- Ocho archivos nuevos en `backend/src/main/java/com/notiurba/admin/`:
  AdminController.java, AdminRepositorio.java, AdminServicio.java,
  CambiarEstadoRequest.java, CambioEstadoResponse.java, EstadoReporte.java,
  VigenciaRequest.java y VigenciaResponse.java.
- `backend/src/main/java/com/notiurba/reporte/ReporteServicio.java`: reutilización
  pública de la normalización existente de códigos, con Locale.ROOT.
- Tres pruebas administrativas: AdminControllerTest.java,
  AdminIntegracionTest.java y ConfiguracionIntegracionTest.java.
- `backend/src/test/resources/pruebas.properties`: contexto de pruebas completo.
- EnviadorCorreoResendTest.java y AlmacenFotosCloudinaryTest.java: cliente mock
  y verificación de cero llamadas en los casos sin credenciales, conservando sus
  aserciones originales.
- `docs/contexto/11-contrato-api.md`: endpoints, ejemplos, validaciones y errores.
- `docs/admin-estados-validar-local.ps1`: ejecución aislada reproducible.
- `docs/admin-estados-vigencia-validacion.md`: este informe.

Se excluyen completamente `docs/gis-not31/`, cachés, logs, JAR, credenciales y
archivos generados. Los dos cachés Python preexistentes de GIS se conservan sin
agregar ni borrar. La configuración de producción y las migraciones no cambian.
`feature/integracion-not31-edt51` conserva
`cd17f47e8d9db865bac43230b63c09932e73f3db`; main y develop no se modifican en
esta entrega. No se realiza merge ni se crea pull request.
