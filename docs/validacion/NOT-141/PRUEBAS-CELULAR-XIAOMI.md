# Pruebas Xiaomi — NOT-141

Fecha: 14/11/2026, Colombia. Objetivo: Xiaomi Android 16, serial autorizado `DEQCAQEUVS9PNRDQ`.
**Estado: bloqueadas. No se verificó el recorrido móvil.**

Se ejecutó la ruta absoluta `C:\Users\Usuario\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
`Test-Path` devolvió True; `adb version` identificó 1.0.41 / platform-tools 37.0.0 en Windows 10.0.26300.

La consulta inicial `adb devices -l` y un diagnóstico dirigido `adb -s DEQCAQEUVS9PNRDQ get-state` devolvieron: `could not read ok from ADB Server`, `failed to start daemon`, `cannot connect to daemon`. El problema se produce al conectar/iniciar el servidor ADB de tcp:5037, antes de acreditar el teléfono. No se obtuvo un `EACCES` explícito ni evidencia suficiente para afirmar que falta depuración USB. No se reiniciaron procesos, permisos ni configuraciones del usuario.

Evidencia: [adb.txt](evidencias/adb.txt), con fecha exacta. No se generaron capturas sintéticas.

| Prueba física | Resultado 14/11/2026 | Dispositivo acreditado |
|---|---|---|
| Registro y verificación | No ejecutada: ADB inaccesible | Ninguno |
| Login correo / Google / retorno / sesión | No ejecutada | Ninguno |
| Categoría, barrio, dirección, GPS y marcador | No ejecutada | Ninguno |
| Crear con foto / revisar / código / Mis reportes | No ejecutada | Ninguno |
| Subir 1, 2, 3 y rechazar cuarta foto | No ejecutada | Ninguno |
| Recuperar/restablecer, perfil, actualizar e historial | No ejecutada | Ninguno |
| Borrado de datos/cuenta QA | No ejecutada; QA y autorización específica pendientes | Ninguno |
| Gmail Alcaldía/empresa, fotos y texto | No ejecutada; mensajes QA no identificados | Ninguno |
| Responsive y permisos GPS concedido/denegado | No ejecutada | Ninguno |

Reanudación: comprobar daemon ADB desde PowerShell del usuario, verificar serial con estado device y abrir el despliegue en Chrome Android. Preparar cuentas/destino QA antes de crear reportes. Capturar evidencia real de cada paso con hora, código QA y secretos ocultos. El tamaño móvil emulado en escritorio no sustituye esta validación.


## Reanudación del 14/11/2026 — limpieza adicional y accesos

Esta sección registra avances posteriores a la primera revisión. Los resultados anteriores se conservan; repetir una solicitud no crea otro caso de aceptación. Estados: **APROBADO** solo con ejecución conforme y alcance explícito; **FALLIDO** con defecto reproducible; **BLOQUEADO** por acceso/precondición; **PENDIENTE** aún sin ejecución.

Se repitió version y get-state con la ruta absoluta/serial autorizados. ADB mantiene el error de conexión al daemon. Un sondeo TCP de solo lectura a 127.0.0.1:5037 devolvió **SocketException / AccessDenied**. Esto identifica una denegación del acceso de este proceso al socket; no demuestra un problema del cable ni un permiso concreto del Xiaomi. No se cambió configuración o protecciones del sistema.

**BLOQUEADO:** todos los recorridos físicos, GPS, fotos y Gmail Android. No se generaron capturas ni se reemplazaron por escritorio emulado. La limpieza conserva las evidencias históricas NOT-65 y sus IDs; las imágenes deduplicadas tienen copia canónica y respaldo. No se reinterpretan como pruebas móviles nuevas.


## Avance posterior en Chrome — 14/11/2026

Chrome de Windows volvió a permitir navegación y validaciones públicas. **No es el Xiaomi**: ADB mantiene AccessDenied y no hay nuevas pruebas físicas. Ningún resultado de WEB-01 a API-WEB-03 se acredita como móvil. [Evidencia real de Chrome](evidencias/reanudacion-20261114/chrome-ui.json).
