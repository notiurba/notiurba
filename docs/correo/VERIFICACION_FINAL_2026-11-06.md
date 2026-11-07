# Verificación final del enrutamiento — 6 de noviembre de 2026

Base de trabajo: `748a4a0b4be389d93fadc2fd18f3452f59d704cf`, rama `feat/enrutamiento-correos-notiurba`. Alcance: verificaciones pendientes y corrección de defectos reproducibles. No se añadieron funcionalidades.

## Ocho integraciones JDBC

Se creó exclusivamente `notiurba_test_correo_20261106_final`, vacía, dentro del proyecto de pruebas `NotiUrba-EDT51-isolated`. No se copió ni utilizó la base productiva. El proyecto aislado dispone de PostgreSQL 18 y PostGIS; la inicialización de esta base mediante Flyway quedó bloqueada antes de ejecutar SQL.

Ejecución real con Eclipse Adoptium Java **21.0.13**, Maven 3.9.9 y dependencias oficiales existentes, sin modificar POM ni versiones. Surefire se ejecutó sin fork, con el agente Byte Buddy existente cargado al iniciar Maven, por las restricciones del entorno.

Selección Maven: `-Dtest=EnrutamientoIntegracionTest,MigracionEnrutamientoTest,ColaDeshabilitadaTest -DforkCount=0 test`. Se configuró únicamente la conexión a la base desechable, con TLS `verify-full`, y se retiraron del proceso las variables de Resend y de base productiva. `NOTIURBA_ENVIO_REPORTES_HABILITADO=false`.

| Prueba | Resultado de esta ejecución |
| --- | --- |
| `propiedadImpideConsultarOEnviarReporteAjeno` | Bloqueada al obtener conexión JDBC |
| `gasEmergenteYCanalesAlternosNoLlamanProveedor` | Bloqueada al obtener conexión JDBC |
| `catorceRutasConservanIdsYMunicipalesVanSoloAlCaic` | Bloqueada al obtener conexión JDBC |
| `pendienteAceptadoYReintentoNoCreanReportesNiDuplicanCorreos` | Bloqueada al obtener conexión JDBC |
| `falloConservaReporteYReintentoReutilizaMensajeYClave` | Bloqueada al obtener conexión JDBC |
| `reboteFirmadoNoRetrocedeHistorialYEsIdempotente` | Bloqueada al obtener conexión JDBC |
| `colaDeshabilitadaConservaPendienteSinLlamarAlProveedor` | Bloqueada al obtener conexión JDBC |
| `conservaPendientesHistoricosYElTriggerSincronizaReporte` | Bloqueada al obtener conexión JDBC |

Resultado Surefire: **9 pruebas, 0 fallos de aserción, 8 errores de inicialización, 0 omitidas; 1 aprobada**. La aprobada es `ColaDeshabilitadaTest.llamadaDirectaNoAccedeABaseNiProveedorCuandoEstaApagada`.

Diagnóstico común de las ocho integraciones:

```text
SQL State: 08001
Unable to obtain connection from database
Caused by: java.net.SocketException: Permission denied: getsockopt
```

La restricción impide a la JVM establecer el socket. No es un fallo de aserción del enrutamiento ni demuestra compatibilidad de Flyway por JDBC. **Las aserciones de las ocho integraciones siguen pendientes de ejecución satisfactoria.** No se sustituyó este resultado por SQL mediante conector ni se relajaron TLS, permisos o aislamiento. La comprobación SQL anterior está documentada por separado en `AUDITORIA_CORRECCIONES_2026-11-06.md` y no se vuelve a contabilizar como integración Java.

## Navegador real y Figma

Se ejecutó el frontend React/Vite real en un navegador Chromium mediante una instalación local temporal. Un middleware externo al repositorio simuló todas las rutas `/api/`; no se inició el backend ni se conectó Resend. Usuario, códigos, descripciones y estados de las capturas son sintéticos. Estas verificaciones comprueban la interfaz, no las transiciones reales de la base de datos.

Se compararon los nodos `232:895`, `232:930`, `232:968`, `232:1003`, `232:1041` y `232:1078` del [Figma indicado](https://www.figma.com/design/qRLcQ7jjC71F52CI6S9m1j/prototipo---notiurba?node-id=202-1636). Los cuatro estados originales conservan sus iconos, Nunito Sans, tarjetas, colores semánticos, avisos y acciones. El canal externo se verificó contra el requisito funcional: no hay un quinto frame específico en esos nodos. No se declara equivalencia píxel a píxel: la consulta conserva su formulario y el pie fluye con el documento, en vez del encuadre fijo de 900 px del diseño.

El ajuste de viewport de alto nivel no se aplicó correctamente a las pestañas existentes. Se repitió toda la matriz con `Emulation.setDeviceMetricsOverride`, verificando `innerWidth` antes de cada captura. Las evidencias finales reemplazan las capturas iniciales y contienen las medidas efectivas en archivos JSON adyacentes. La anchura de imagen puede excluir la barra vertical; la anchura de viewport queda registrada en el JSON.

| Estado observado | Reporte / correo | 390 px | 768 px | 1440 px |
| --- | --- | --- | --- | --- |
| Pendiente | Recibido / PENDIENTE | [Captura](evidencias-finales/390-pendiente.jpg) | [Captura](evidencias-finales/768-pendiente.jpg) | [Captura](evidencias-finales/1440-pendiente.jpg) |
| Fallo inicial | Recibido / FALLIDO | [Captura](evidencias-finales/390-fallido.jpg) | [Captura](evidencias-finales/768-fallido.jpg) | [Captura](evidencias-finales/1440-fallido.jpg) |
| Aceptado | Enviado / ENVIADO | [Captura](evidencias-finales/390-enviado.jpg) | [Captura](evidencias-finales/768-enviado.jpg) | [Captura](evidencias-finales/1440-enviado.jpg) |
| Rebote | Enviado / FALLIDO (rebote) | [Captura](evidencias-finales/390-rebote.jpg) | [Captura](evidencias-finales/768-rebote.jpg) | [Captura](evidencias-finales/1440-rebote.jpg) |
| Canal externo | Recibido / NO_ENVIADO_CORREO | [Captura](evidencias-finales/390-externo.jpg) | [Captura](evidencias-finales/768-externo.jpg) | [Captura](evidencias-finales/1440-externo.jpg) |

Las 15 combinaciones mostraron la anchura solicitada, fuente cargada, todos los SVG cargados y `scrollWidth <= clientWidth`: sin desbordamiento horizontal. Textos largos se acomodan en móvil; los botones principales conservan 52 px de altura. Se inspeccionaron las capturas y el árbol accesible: títulos, región de resultado nombrada, estados anunciables, etiquetas y nombres de los controles. Los iconos decorativos tienen texto alternativo vacío.

Interacciones comprobadas: copiar código anuncia «Código copiado»; Tab muestra foco de 3 px y alcanza el reintento y la casilla de emergencia; Enter en «Ver mis reportes» navega a la lista; «Hacer otro reporte» abre el paso 1. El reintento realizó exactamente una petición al middleware local, sin crear otro reporte ni llamar a un proveedor. El enlace externo tiene `target="_blank"` y `rel="noopener noreferrer"`; se inspeccionó su destino, sin radicar nada en el sitio externo. No hubo errores de consola en la comprobación final. Esto no constituye una auditoría exhaustiva WCAG ni una prueba con lector de pantalla.

## Defecto confirmado y corrección

`frontend/index.html` declaraba `lang="en"`. El navegador confirmó esa declaración sobre contenido español. Se cambió únicamente a `lang="es"` y se verificó en las 15 combinaciones finales. No se modificaron pantallas, rutas, lógica de correo, autenticación, registro ni aviso de privacidad.

## Pruebas y compilación adicionales

- Nueve pruebas Node de `estadoCorreo.test.mjs` y `recuperacionReporte.test.mjs`: **9 aprobadas**, incluidas rutas anteriores/tardías y recuperación tras fallo de fotografía o encolado.
- ESLint del frontend: **aprobado**, salida 0.
- Build con el ejecutable oficial instalado `node node_modules/vite/bin/vite.js build`: **aprobado**, Vite 8.3.4, 103 módulos. Persiste advertencia de bloque JS superior a 500 kB.
- El primer intento de `npm run build` no encontró el shim `vite` en PATH; se resolvió invocando su archivo oficial, sin cambiar dependencias. El repositorio no define script `npm test`; se ejecutaron directamente los archivos Node existentes.
- No se repitió el empaquetado completo del backend: no hubo cambios Java desde el commit base. El intento de integraciones ejecutó las fases de compilación con Java 21 y terminó en los errores JDBC anteriores.
- `git diff --check`: sin errores.

## Cola y limpieza

La propiedad mantiene `NOTIURBA_ENVIO_REPORTES_HABILITADO:false` como valor predeterminado. `ColaCorreoConfig` solo habilita el programador si la propiedad es `true`; `procesarSiguiente()` retorna antes de consultar BD o proveedor cuando está apagada. La prueba unitaria aprobada verificó cero interacciones con ambos. Las integraciones usan un proveedor Mockito y el navegador solo respuestas locales; no se suministraron credenciales de correo ni se enviaron correos reales.

La base desechable se eliminó y el listado posterior confirmó que solo permanece `notiurba_edt51` en la rama de pruebas. Se eliminó el archivo temporal con la cadena de conexión. Se detuvieron los dos servidores locales temporales, se restauró la configuración Vite byte por byte y se retiraron las emulaciones de viewport. Se conservaron únicamente las capturas y métricas como evidencia de esta revisión y las herramientas reutilizables ya existentes.

## Conclusión

**La rama puede pasar a revisión de código e interfaz con un bloqueo explícito de validación: no está aprobada para integrar hasta ejecutar satisfactoriamente las ocho integraciones JDBC en un entorno que permita conectarse a PostgreSQL/PostGIS.** No se certifica la compatibilidad contra un historial Flyway productivo no inspeccionado.

`main` permanece en `0b99f7c55a3be2a56c40ad22fbb1333691f71ffd`; la rama de integración conserva `cd17f47e8d9db865bac43230b63c09932e73f3db` y su directorio limpio. No se desplegó, publicó rama, creó PR ni fusionó nada.
