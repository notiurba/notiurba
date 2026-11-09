# Verificación controlada de la cuenta sintética de NOT-71

Este auxiliar no añade endpoints al backend ni desactiva filtros, verificación de correo, contraseñas o firma JWT. No cambia directamente `usuario.correo_verificado_en`. Se limita al contenedor `notiurba-barrios-qa-20261108-153240-458c80`, PostgreSQL publicado en `127.0.0.1:15435`, base `notiurba_barrios_qa`, usuario SQL `qa_barrios` y cuenta `qa.not71.20261108.2147@example.test` / documento `9711082147` / barrio 43 / rol CIUDADANO. Detiene el proceso si no coinciden los controles.

Ejecutar desde PowerShell 7 con acceso al Docker Desktop local:

```powershell
& 'C:\notiurba\noti-urba\output\verificar-cuenta-NOT71-QA.ps1'
```

No necesita credenciales productivas. Contiene únicamente la contraseña sintética que se utilizó en el alta QA previa. No crea una cuenta ni repite el registro o pruebas de barrios/responsive. No cambia V17 ni ejecuta migraciones.

Si la cuenta sigue sin confirmar, genera un token aleatorio de 32 bytes en memoria, invalida los tokens aún vigentes de **esa cuenta QA**, inserta solo su hash SHA-256 con 15 minutos de vigencia y llama a `POST http://127.0.0.1:18091/api/auth/verificar`. El backend real consume el token y actualiza el estado de verificación. Después prueba rechazo de reutilización HTTP 400, comprueba por SQL el consumo, ejecuta el login real y comprueba por SQL la cuenta verificada. Si la cuenta ya estaba verificada, no emite otro token y registra ese alcance.

Es un fixture de verificación QA, **no una prueba de entrega de correo**. No llama a Resend ni reenvía enlaces; no fabrica JWT ni almacena tokens o sesiones. La evidencia JSON incluye estados HTTP, cuenta sintética, fecha de verificación SQL y conteos de reportes/fotos; no confirma contenido de reportes ni apertura de fotografías.

Ante un error se detiene y conserva los datos para diagnóstico. Si ya se insertó un token y falla el HTTP, el token vence en 15 minutos; no se imprime. No repetir el comando indefinidamente ante errores de destino, permisos o identidad.

Estado de esta revisión: sintaxis PowerShell validada; **no se ejecutó el flujo SQL/HTTP del auxiliar**, porque la comprobación de acceso a Docker falló antes de cualquier escritura. No se acredita todavía confirmación de cuenta, login posterior, foto, reporte o persistencia integral.

La salida satisfactoria se guarda en `docs/validacion/evidencias-NOT-71-continuacion-20261108/verificacion-controlada-<fecha-hora>.json`. Tras generarla, continuar en Chrome con la cuenta sintética en `http://127.0.0.1:5175/login`, y ejecutar el reporte solo en QA. Si las fotos siguen deshabilitadas, registrar el fallo real y conservar el reporte sin declarar aprobada su foto. NOT-71 requiere por separado el ensayo completo desplegado.
