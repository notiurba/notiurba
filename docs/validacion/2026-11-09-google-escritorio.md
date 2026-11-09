# Google en Chrome de escritorio: corrección local y bloqueo de configuración

Estado: correcciones locales verificadas por pruebas automatizadas; autenticación Google real pendiente.
Rama auditada: `fix/registro-pr20`. HEAD inicial: `46bf1b26353f29d838203d2b049fa4fc5d878382`.
El árbol estaba limpio al iniciar. No se hicieron commits, despliegues ni cambios productivos.

## Causa demostrada

Los módulos que Vite sirve en 5175 y 5176 responden HTTP 200 y contienen
`VITE_USAR_MOCKS=false`, `VITE_API_URL=''`, pero **no contienen VITE_GOOGLE_CLIENT_ID**.
Sin ese identificador, BotonGoogle muestra deliberadamente un botón deshabilitado.
El problema ocurre antes de abrir el selector de Google; no demuestra un error de origen autorizado.
La configuración aislada de Vite usa otro `envDir`, por lo que cambiar solamente
`frontend/.env.local` no configura estas instancias. Ese archivo se conservó intacto.

El backend de fotografías QA fija `notiurba.google.client-id=qa.apps.googleusercontent.com`
en `Not71ServidorQA.java`, con máxima prioridad. Es un identificador ficticio de pruebas.
Poner un identificador real solamente en Vite tampoco bastaría: el backend rechazaría su audiencia.
La búsqueda en los archivos rastreados relevantes y scripts de output no recuperó un Client ID real.
No se inspeccionó Google Cloud Console ni se afirma que allí no exista un cliente.

## Cambios realizados

| Archivo | Corrección |
| --- | --- |
| frontend/src/compartido/auth/googleSdk.js | Carga compartida de GIS, tiempo límite, reintento tras error, inicialización única y callback del componente vigente; popup explícito. |
| frontend/src/compartido/componentes/acceso/BotonGoogle.jsx | Usa el cargador compartido, descarta respuestas tras desmontar, evita peticiones concurrentes, limita simulación a desarrollo y mantiene el destino permitido recibido por state. |
| frontend/src/compartido/api/auth.js | Rechaza sesiones Google incompletas y perfiles sin tokenRegistro/correo antes de almacenar sesión o navegar. |
| frontend/src/compartido/paginas/CompletarPerfil.jsx | Retorno explícito con destinoTrasAutenticacion, conservación del destino al cancelar/volver y redirección a login cuando falta tokenRegistro en modo real. |
| backend/src/main/java/com/notiurba/auth/google/GoogleTokenVerificador.java | Exige sub y correo no vacíos; mantiene firma RSA, audiencia, emisor, expiración y email_verified. |
| backend/src/main/java/com/notiurba/auth/google/GoogleAuthServicio.java | Elimina la asociación automática basada solo en correo. Una identidad Google ya vinculada se busca por proveedor + sub. Una colisión con cuenta local no vinculada devuelve 409 sin modificarla. |
| Pruebas frontend y backend incluidas en el diff | Regresiones del SDK, contrato, asociación y claims obligatorios. |

La protección indirecta de retorno desde Completar perfil ya existía en las rutas;
no se atribuye a este cambio toda la funcionalidad previa. Ahora esa pantalla utiliza directamente
la misma utilidad de destinos permitidos y no depende exclusivamente de la intercepción posterior en /app.
El borrador del reporte no se modifica ni se envía al autenticar.

La vinculación de una cuenta tradicional existente necesita un flujo que confirme explícitamente
la titularidad de esa cuenta. **Ese flujo de vinculación no se implementó ni se considera aprobado.**
El usuario puede seguir entrando por contraseña. No se crearon cuentas duplicadas de prueba.

## Validación ejecutada

| Comprobación | Resultado | Alcance |
| --- | --- | --- |
| Node: googleSdk, retornoReporte y cliente HTTP | 35/35 aprobadas | Pruebas automatizadas; HTTP sustituido en las pruebas de contrato. No es autenticación con Google real. |
| ESLint sobre los cuatro módulos frontend modificados | Aprobado | Código fuente. |
| npm run build | Aprobado | Compilación; no despliegue. |
| GoogleTokenVerificadorTest | 11/11 aprobadas | Tokens RSA firmados con claves locales de prueba; firma ajena, audiencia, emisor, expiración, sub y correo inválidos rechazados. |
| GoogleAuthServicioTest | 13/13 aprobadas | Repositorios sustituidos: identidad vinculada, perfil nuevo, conflictos y ausencia de asociación por correo. |
| GoogleAuthControllerTest | 4/4 aprobadas | MockMvc; contrato y validaciones. |
| AuthControllerTest | 2/2 aprobadas | Regresión del acceso tradicional en MockMvc. |
| Chrome real | Bloqueado | La conexión cua.getState agotó su tiempo de espera. No se obtuvo captura nueva. |
| Selector Google, cuenta autorizada, sesión real, logout/reingreso y retorno real al reporte | Pendientes | Faltan configuración OAuth válida y acceso operativo a Chrome. |

El primer intento de Maven quedó detenido al intentar adjuntar dinámicamente el agente de Mockito:
el diagnóstico de Surefire contiene acceso denegado al canal javatool. Se interrumpió únicamente esa
ejecución de pruebas. El segundo intento cargó Byte Buddy mediante `-javaagent` al arrancar la JVM
y terminó BUILD SUCCESS: 30 pruebas, 0 fallos, 0 errores y 0 omitidas. No fue un fallo de la aplicación QA.
No se volvió a ejecutar el recorrido QA de registro, barrios, fotografía, reportes o persistencia SQL.

Evidencias nuevas: `evidencias-google-escritorio-20261109/resultados.json`,
`google-escritorio-frontend-tests.log` y `google-escritorio-build.log` en esa misma carpeta.
El JSON conserva recuentos y fechas de los informes Surefire, sin copiar tokens ni contraseñas.

## Configuración externa necesaria

1. Obtener del responsable un **Client ID público de tipo aplicación web autorizado para QA**.
   No hace falta comunicar contraseñas, Client Secret, tokens ni códigos de Google.
2. En Google Cloud Console → Google Auth Platform → Clients → cliente web correspondiente,
   comprobar los orígenes JavaScript autorizados. Para reutilizar QA de fotografías el origen es
   `http://127.0.0.1:5176`, sin `/login`. El responsable debe confirmar que ese origen está autorizado.
   No agregar por rutina localhost:5173, 127.0.0.1:5175 ni dominios productivos.
3. Configurar exactamente el mismo Client ID en Vite (`VITE_GOOGLE_CLIENT_ID`) y el verificador
   backend (`notiurba.google.client-id`, normalmente `GOOGLE_CLIENT_ID`). Para el harness QA hay que
   reemplazar controladamente su valor ficticio de máxima prioridad; una variable general no lo sobrescribe.
   Para el Vite aislado hay que configurar su envDir/define efectivo; `.env.local` no basta.
4. La implementación usa GIS con popup y callback JavaScript, seguido de POST `/api/auth/google`.
   **No utiliza un callback OAuth de redirección ni necesita agregar una URI de redirección** para este flujo.
   El endpoint Spring recibe la credencial desde el frontend; no es una URI de redirección de Google.
5. El proxy /api del QA de 5176 apunta a 18092. Si se utiliza acceso directo entre orígenes,
   CORS debe permitir exclusivamente el origen QA elegido. No se observó evidencia que justifique ampliar CORS.
6. No reiniciar los servidores que funcionan ni ejecutar el iniciador de fotografías para configurar Google:
   dicho script también contiene acciones de fotografía ya aprobadas. Preparar la configuración de Google
   como un cambio controlado separado cuando se disponga del Client ID autorizado.

Referencias primarias:
- https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid
- https://developers.google.com/identity/gsi/web/reference/js-reference
- https://developers.google.com/identity/gsi/web/guides/verify-google-id-token

## Próxima prueba real, sin producción

Con Client ID y origen confirmados, conectar únicamente a la base QA existente y comprobar en Chrome:
botón oficial → selector → intervención del usuario con cuenta de prueba autorizada → backend QA →
sesión o Completar perfil → retorno al paso del reporte guardado → cierre de sesión → nuevo acceso.
Verificar por lectura SQL que proveedor + sub conserva una identidad y que no aparece un usuario duplicado.
No enviar un nuevo reporte para probar el retorno. No probar Google contra producción ni usar una cuenta real
del usuario sin su intervención y autorización para esa cuenta de prueba.

No se considera que Google funcione de punta a punta todavía. Las aprobaciones anteriores de NOT-71
se conservan; este trabajo no autoriza cerrar NOT-71 ni cambia su estado de In Review.
