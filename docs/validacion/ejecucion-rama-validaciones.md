# Ejecucion exclusiva en codex/edt-5-1-validaciones

Corte inicial de esta ejecucion: 2026-10-27 UTC. Documento en progreso; los informes anteriores conservan antecedentes.

Se consulto C:/notiurba/noti-urba sin modificarlo: Git devuelve Permission denied. Java21.0.10 disponible. mvn no esta en PATH; se utiliza Maven3.9.16 incluido en IntelliJ. Test-NetConnection a Maven Central no confirma TCP443 y reporta acceso CIM denegado. Maven vuelve a fallar con Permission denied: getsockopt antes de descargar el parent3.3.5. No se cambian TLS, certificados ni restricciones del sistema.

Git fetch y git clone SI funcionan ahora. Se creo un clon completo autentico directamente en codex/edt-5-1-validaciones; --is-shallow-repository=false. main de referencia: 2d6ceca3804ceebe1df95e838ef90bab911bcd9b. Rama remota previa: 9bd16279dbb01c82e584e677225551d7676bf68c. Cero commits nuevos de main para incorporar; merge origin/main: Already up to date. Se aplico el delta acumulado revisado de EDT5.1, con --check previo y excluyendo render.yaml ya identico. No se transfirio el historial reconstruido. El frontend y V1-V4 quedan sin diferencias.

SHA256 ya esta aplicado en los tres clientes; no se reaplica el parche. Se agrega trigger push limitado a la rama de validaciones. Se elimina la creacion automatica de PR del runner; se refuerza su comprobacion de rama. No se ejecutan operaciones de escritura sobre main/develop ni se crean PR.

Primera publicacion y Actions: pendientes de consulta al momento de este corte. Los tests locales y remotos solo se aprobaran con logs reales. Las variables de Neon y Cloudinary ausentes no se sustituyen por mocks para acreditar integraciones.
## Corte actualizado: copia de publicacion y bloqueo de escritura

2026-10-27 UTC. Se recupero el clon autentico original y se comprobo c681a8a. Se incorporaron los commits remotos propios sin conflictos en ese clon, cuyo checkpoint es 84e459f. No se escribio sobre main ni develop y no se creo PR. main sigue en 2d6ceca3804ceebe1df95e838ef90bab911bcd9b.

El navegador publico cuatro archivos adicionales en validaciones: CloudinaryPhotoStore, CloudinarySignature, JpegValidator y PhotoUploadErrors. La rama remota quedo en 649fe0b7833817948c965b5674f999d5bf0915f3. Los seis archivos publicados antes de esta ejecucion conservan exactamente su contenido. El SDK publicado contiene SHA256. No se reaplico el parche.

El push por terminal sigue bloqueado por Git Credential Manager: sh.exe falla con NtCreateDirectoryObject 0xC0000022, seguido de imposibilidad de obtener Username. El conector GitHub devuelve 403 Resource not accessible by integration. La edicion web permitio commits parciales pero su editor interrumpe operaciones; no se afirma una publicacion completa.

Para dejar un push de avance directo sin mas fusiones, se creo la copia autentica notiurba-edt51-publicacion, directamente desde la ultima rama remota, y se aplico exclusivamente el delta faltante conservado, con git apply --check. No se reconstruyo historial ni se modificaron los diez archivos ya publicados. El cambio de newline del SDK remoto se conservo. Frontend y V1-V4 siguen identicos a origin/main. El trabajo previo no se elimina: permanece en notiurba-authentic-edt51.

Complete-Edt51.ps1 ahora rechaza estados desactualizados mediante merge-base --is-ancestor y no ejecuta git merge, gh pr create ni gh pr merge. El workflow tiene push limitado a codex/edt-5-1-validaciones; jobs Java21 de unitarias/paquete y migracion PostGIS18, con informes Surefire/Failsafe, sin secretos externos. El empaquetado usa mvn package sin omitir pruebas.

Se preparo tools/Publish-Edt51Branch.ps1 para Windows PowerShell5.1 o posterior. Verifica rama exacta, origin, historial completo, arbol limpio, commits remotos incluidos, archivos requeridos, SHA256, ausencia de diferencias en frontend/migraciones y diff --check. Solo puede ejecutar push de HEAD a refs/heads/codex/edt-5-1-validaciones. No crea commits, PR, fusiones, despliegues ni cambios de permisos. -CheckOnly permite revisar sin publicar. Si hay cambios concurrentes, se detiene en vez de sobrescribirlos.

Maven clean test se intento tambien desde esta copia: salida1 antes de compilar por Permission denied: getsockopt al resolver el parent Boot3.3.5. Evidencia: logs/publicacion-validaciones/maven-clean-test.log. La validacion sintactica de ambos scripts paso; no equivale a Maven ni a CI aprobado.

Actions aun no tiene ejecuciones y el workflow no esta publicado al momento de este corte. G01 sigue PENDIENTE, al igual que las integraciones Java/remotas y las migraciones no ejecutadas. No se genera ZIP.

Comando para publicar desde una terminal normal de Windows, fuera del entorno restringido de esta sesion:

```powershell
& "C:\Users\Usuario\.codex\.chatgpt-projects\g-p-6aacbd0a1a688191b438845119074d96\edt51-work\notiurba-edt51-publicacion\tools\Publish-Edt51Branch.ps1"
```

Despues del push, consultar https://github.com/notiurba/notiurba/actions?query=branch%3Acodex%2Fedt-5-1-validaciones y exigir logs/reportes reales antes de cambiar estados. La publicacion por si sola no acredita compilacion ni integraciones.
La ejecucion real del preflight detecto un falso rechazo de SHA256: -match de PowerShell ignora mayusculas y tambien reconocia SHA256 como sha256. Se corrigio con -cmatch/-cnotmatch. El intento fallido se conserva en logs/publicacion-validaciones/windows-publisher-preflight.log. La comprobacion se repite despues de guardar la correccion; esto no ejecuta push ni pruebas Java.

Cierre 2026-10-27T12:24:37.6737447Z : preflight repetido APROBADO (logs/publicacion-validaciones/windows-publisher-preflight-retry.log). Comprueba publicacion segura; no ejecuta Maven ni Actions. La publicacion completa y los jobs continuan pendientes por los bloqueos documentados.

Corte externo 2026-10-27T13:16:11.9393681Z : baseline a599593 y Actions40507497490 verificados; configuracion manual y controles locales preparados. Ver INTEGRACIONES-EXTERNAS.md. Ninguna integracion externa se declara aprobada.

### Cierre de publicacion de este corte

Los cambios de integraciones externas quedaron guardados localmente en 673e33f. El push exclusivo de validaciones fallo: Credential Manager/sh NtCreateDirectoryObject 0xC0000022 y unable to get password from user. La alternativa del conector devolvio HTTP403 Resource not accessible by integration. Ver logs/external-config/publication.log. NO estan publicados estos cambios; NO hay nueva ejecucion CI que acredite la prueba de arranque validation anadida. G01 queda pendiente para esta actualizacion, conservando la publicacion/CI previa de a599593.

Desde PowerShell normal de Windows, publicar con el script existente; este verifica arbol limpio, main incluido y ausencia de avances remotos concurrentes, y no fusiona ni crea PR:

```powershell
& "C:\Users\Usuario\.codex\.chatgpt-projects\g-p-6aacbd0a1a688191b438845119074d96\edt51-work\notiurba-edt51-publicacion\tools\Publish-Edt51Branch.ps1"
```

Despues de publicar, comprobar los jobs normales del nuevo push; no atribuirles integraciones externas. Para estas, configurar el Environment/secretos y comprobar el dispatch o utilizar el runner local como explica INTEGRACIONES-EXTERNAS.md. main conserva 2d6ceca3804ceebe1df95e838ef90bab911bcd9b. No se hicieron PR, fusiones ni despliegues.
