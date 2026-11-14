# Informe final de la sesión — NOT-141

**Estado: EN PROGRESO / NO APTA PARA CIERRE.** Este informe cierra la sesión de auditoría, no la historia.
Responsable QA: Luz Amelia Ibarguen Ibarra. Fecha: 14/11/2026, Colombia. Evidencias con horas ISO y zona explícita; las marcas UTC se convierten restando cinco horas.

## A. Rama y revisiones realmente revisadas

- Repositorio: C:\notiurba\noti-urba. Rama conservada: `fix/registro-pr20`.
- HEAD: `d8e8b2bee07a3e4e59846f9fbde803a4b044fa95`. Árbol inicial limpio.
- Remoto: `git@github.com:notiurba/notiurba.git`. No se hizo fetch, cambio de rama, commit, push, PR ni despliegue.
- Linear indica `ibarguenluz/not-141-edt-95-probar-de-punta-a-punta-revisar`; no apareció en referencias locales ni en la búsqueda remota específica.
- Las referencias locales están desactualizadas. GitHub compara la rama actual contra `develop`: divergencia, 156 commits exclusivos de develop y cuatro exclusivos de la rama revisada. No corresponde hacer un avance rápido.
- `develop` remoto inspeccionado: `8b8419a435a296b56ce7175bf42abeffabd64db9`. Contiene implementación posterior, mapas administrativos y migraciones V17–V22. Se consultaron contratos por GitHub sin alterar archivos funcionales.
- No se identificó el commit efectivamente desplegado ni en Netlify ni en Render. Una rama remota y una migración no identifican por sí solas el binario en ejecución.

## B. Aprobaciones con alcance limitado

| Comprobación ejecutada | Resultado | Alcance |
|---|---|---|
| Git inicial y rama | Conforme | Local; sin cambios previos |
| Consulta SELECT a Neon production | Conforme | PostgreSQL 18.7, 40 tablas públicas |
| Metadatos de migraciones y catálogos | Conforme | V1–V22 exitosas; 133 barrios públicos; 14 rutas |
| Sintaxis de scripts de 114 recursos nuevos | Conforme | Compilación local; no prueba de la API |
| Guardas con valores predeterminados | Conforme | 92 solicitudes bloqueadas por precondiciones en comprobación local |
| Exportación temporal Postman v2.1 | Generada | No importada ni ejecutada por Newman |

**Ningún recorrido ciudadano ni caso de Render quedó aprobado.** La conexión SELECT a Neon no certifica que Render tenga conexión a esa misma base.

## C. Fallos y hallazgos

H-141-01, prioridad alta: `obtenerReportesCiudadanos()` devuelve `reportesPrueba` ante cualquier excepción. Sigue presente en la copia local y en el archivo remoto inspeccionado. Se reprodujo con un fallo inyectado en una VM local; no se alteró la API ni se simuló una prueba de producción. Responsable propuesto: Katherine; seguimiento relacionado: NOT-135 y NOT-140.

La rama local y el contrato actual difieren. La extensión previa consideraba inválida la observación administrativa vacía, pero ahora es opcional. Se reemplazó por un negativo con estado ausente. No se presenta el comportamiento antiguo del correo como fallo demostrado del despliegue nuevo.

No se obtuvo un fallo funcional HTTP de producción: hubo bloqueos de infraestructura de prueba sin respuesta del servidor. Detalles en [hallazgos](HALLAZGOS-Y-REPRUEBAS.md).

## D. Pruebas no ejecutadas y motivos

Registro, verificación, login tradicional, Google, navegación, persistencia de sesión, crear/consultar reporte, Mis reportes, GPS/mapa, cargas 1–4, recuperación, perfil, actualización/historial y borrados: no completados en producción. La terminal no resuelve ambos dominios; Chrome no respondió a navegación/lectura; ADB no pudo conectarse a su servidor; no existe una cuenta QA preparada con tokens verificables y no se comprobó el destino de prueba de Render.

No se pidió elegir una cuenta Google porque no se alcanzó el selector. Esa intervención personal sigue siendo necesaria cuando se recupere el navegador. No se enviaron correos ni se eliminaron cuentas o datos.

## E. Postman / Render

Se integraron **114 recursos nativos nuevos** en la colección oficial, sin crear una colección JSON adicional permanente. Se reutilizó y corrigió la extensión anterior. Los 24 casos NOT-45 se conservaron.

| Métrica | Resultado |
|---|---:|
| Casos nuevos preparados | 114 |
| Solicitudes GET intentadas en Render | 12 |
| Aprobadas contra Render | 0 |
| Fallidas con respuesta HTTP | 0 |
| Bloqueadas | 114 |
| Sin enviar solicitud (precondiciones/alcance de solo lectura) | 102 |
| Casos ejecutados por Newman | 0 |

Las 12 solicitudes intentadas terminaron en `ENOTFOUND`. Código HTTP obtenido: **no disponible**; `000` es una marca de curl, no una respuesta HTTP. El runner Node no ejecuta `pm.test`. Newman no está instalado; no se descargaron paquetes ni se inventaron resultados Postman.

El PUT válido de vigencia afecta a todos los usuarios y está bloqueado contra Render. Probarlo exige entorno aislado. Las eliminaciones quedan deshabilitadas y requieren cuenta QA expresamente identificada y confirmación específica.

## F. Xiaomi

ADB 37.0.0 existe y ejecuta `version` en Windows nativo. `devices -l` y la comprobación dirigida al serial indicado no pudieron conectarse al daemon en tcp:5037. No se acreditó un Xiaomi conectado. No hay capturas móviles nuevas ni resultados de GPS, responsive o Gmail Android. No se atribuye el problema a un permiso USB concreto sin evidencia.

## G. Correos

Dos búsquedas de solo lectura en Gmail no localizaron mensajes de prueba del sistema. Las coincidencias eran correspondencia o invitación de Figma, no correos ciudadanos. La cuenta conectada no se ha confirmado como bandeja QA del equipo. No se puede concluir que el envío falló o que el buzón correcto esté vacío.

El código contempla la sustitución del destinatario mediante `RESEND_PRUEBA_DESTINO`; su valor efectivo en Render no se verificó. Por ello no se solicitaron envíos. HTML, texto plano, asunto, destinatario, matrícula y miniaturas de Alcaldía/empresa siguen pendientes de revisión real en PC y Android.

## H. Responsables y repruebas

Katherine: fallback del mapa y UI. Andrés: identificar builds, revisar contrato/despliegue y destino QA, apoyar conectividad de servicios. Luz: preparar cuentas QA y ejecutar/repetir recorridos en ambos dispositivos. Son responsables propuestos; no se crearon tareas ni se publicaron comentarios en Linear. La historia ya estaba En progreso y no se cambió su estado.

## I. Archivos creados o modificados

- Los siete entregables de esta carpeta y `evidencias/`.
- `docs/api/NotiUrba API/NOT-141 N141-001.request.yaml` a `N141-114.request.yaml`.
- Variables de `docs/api/NotiUrba API/.resources/definition.yaml`.
- `docs/api/README.md` y `tools/not141-postman.mjs`.
- `indice-casos.json` identifica cada escenario y recurso. El JSON importable se exportó exclusivamente a una ruta temporal.
- No se modificaron frontend, backend, migraciones ni configuración de servicios.

## J. Pasos concretos de cierre

1. Restablecer DNS/red de la terminal y control de Chrome; identificar ambos builds desplegados.
2. Confirmar en Render el destino QA y acceso al buzón correcto antes de cualquier reporte enviado.
3. Recuperar el daemon ADB en la sesión de Windows y acreditar el serial USB.
4. Preparar cuentas ciudadanas y administrador de QA, y un JWT firmado realmente vencido; mantener secretos fuera del repositorio.
5. Importar/verificar los recursos en Postman y ejecutar primero lectura y negativos; después escrituras QA autorizadas.
6. Ejecutar el recorrido móvil completo, Google con intervención personal, fotos 1–4 y verificación de persistencia por código QA.
7. Revisar ambos correos reales en Gmail PC/Android y MIME; conservar evidencias anonimizadas.
8. Obtener autorización específica para borrar datos/cuenta QA; probar vigencia global solo en entorno aislado.
9. Corregir el fallback con autorización adicional y repetir NOT-135/NOT-140 y casos afectados. Completar matriz con resultados reales.

Referencias y límites: [matriz](MATRIZ-CRITERIOS-NOT-141.md), [Postman](RESULTADOS-POSTMAN.md), [Xiaomi](PRUEBAS-CELULAR-XIAOMI.md), [correos](VALIDACION-CORREOS.md), [pendientes](PENDIENTES-PARA-CIERRE.md).


## Reanudación del 14/11/2026 — limpieza adicional y accesos

Esta sección registra avances posteriores a la primera revisión. Los resultados anteriores se conservan; repetir una solicitud no crea otro caso de aceptación. Estados: **APROBADO** solo con ejecución conforme y alcance explícito; **FALLIDO** con defecto reproducible; **BLOQUEADO** por acceso/precondición; **PENDIENTE** aún sin ejecución.

**Estado actual: EN PROGRESO.** Limpieza de candidatos comprobados completada: 11 copias exactas fuera del índice, 297.015 bytes, todas conservadas físicamente. Se inventariaron 1.069 archivos, con consumidores de 321 candidatos revisados. Respaldo nuevo de 963 archivos y bundle verificados. [Informe de limpieza](../2026-11-14-limpieza-adicional-fix-registro-pr20.md).

**Avances reales:** Netlify identifica su despliegue publicado 6af7e3bc2a028d658a609c72, contexto production, rama develop, commit 4868ee4650273c2c04b142c6717dddc517e4ac58, estado ready. Publicado 14/11/2026 04:29:57 UTC (13/11/2026 23:29:57 Colombia). La preview del PR #27 usa d8e8b2b. Identificar el build está APROBADO como metadato; disponibilidad e interacción siguen BLOQUEADAS. Render continúa sin commit acreditado ni HTTP. PR #27: base main, head fix/registro-pr20, abierto y draft; no se cambió.

Se descubrió **Postman CLI 1.85.0 instalado**. Se ejecutó un subconjunto v2.1 de 12 GET sin credenciales: 12 solicitudes con ENOTFOUND, 12 test-scripts ejecutados y 34 aserciones evaluadas/fallidas por ausencia de respuesta. Clasificación QA de las 12 solicitudes: BLOQUEADO; 0 fallos funcionales de Render, 0 aprobaciones HTTP. Newman sigue ausente y sin ejecución. La definición nativa fue rechazada tanto en la colección actual como en una copia mínima extraída de HEAD anterior a NOT-141; compatibilidad/validación nativa pendiente. --output requiere login; se continuó sin esa opción ni credenciales.

El segundo runner Node también intentó 12 GET y quedó bloqueado. Las tres tandas (Node inicial, Node reanudación y CLI) no se suman como 36 casos distintos: son intentos sobre los mismos 12 escenarios de los 114 preparados. Los 102 restantes no se enviaron por precondiciones/alcance.

ADB: binario 37.0.0 ejecutable, pero socket 127.0.0.1:5037 devuelve **AccessDenied** para este proceso; no se puede acreditar teléfono ni recorrido móvil. DNS devuelve HostNotFound. No se cambiaron firewall, permisos, daemon ni otras protecciones. Gmail: búsqueda de 7 días sin correos QA encontrados; buzón de equipo y RESEND_PRUEBA_DESTINO aún sin confirmar. Sin correos enviados, borrados ni cambios productivos.

Pruebas locales de limpieza: 5/5 catálogo, 582 archivos protegidos idénticos, cero enlaces Markdown críticos rotos, SQL regenerado solo en memoria y scripts canónicos con sintaxis válida. Se conservan los 114 recursos; el helper añade --evidence-dir para registrar reintentos sin sobrescribir resultados anteriores. No se modificó frontend/backend ni el fallback.

Queda por cerrar: DNS/Chrome, ADB, versión Render, destino/buzón QA, compatibilidad nativa Postman, autenticaciones reales, recorridos en Xiaomi, fotos 1–4, perfil/recuperación/actualización, borrados QA autorizados y ambos correos MIME en PC/móvil. Evidencias nuevas: [servicios](evidencias/reanudacion-20261114/servicios.json), [HTTP Node](evidencias/reanudacion-20261114/resultados-http-seguros.json), [Postman CLI](evidencias/reanudacion-20261114/postman-cli.txt), [validación limpieza](evidencias/reanudacion-20261114/validacion-limpieza.json).


## Avance posterior en Chrome — 14/11/2026

**Este avance sustituye las limitaciones iniciales de navegador/catálogos, no los resultados de terminal o Postman. NOT-141 sigue En progreso.** Chrome se recuperó mediante control de pestañas autorizado, sin desactivar protecciones. Se vio inicio/login/registro/recuperación en el despliegue público.

APROBADO en PC: login vacío, campos obligatorios de registro pasos 1 y 2, selector de 133 barrios y atributo maxlength=100 de dirección, conservación de barrio/dirección al volver y avanzar, recuperación con correo vacío. Son validaciones negativas/de borrador; no acreditan creación de cuenta, autenticación, envío ni restablecimiento.

APROBADO en navegador: GET real /api/catalogos/tipos-documento y /api/catalogos/barrios, ambos **HTTP 200**, observados por eventos Network.responseReceived desde Netlify y reflejados en los selectores. Se acredita comunicación real Netlify–Render para esos dos endpoints, no para todo el MVP. /api/health terminó de despertar y mostró {estado:ok,baseDeDatos:ok}; se aprueba el contenido observado y la respuesta de salud/BD, con **HTTP no capturado**. No se convierte en 200 por inferencia.

Google: botón oficial GSI cargado y visible. El control del iframe no permitió completar su activación (intercepción de puntero y timeout de teclado); una captura real mostró el botón. No se seleccionó cuenta, no se introdujeron credenciales y no se acredita autenticación ni un defecto funcional de Google. Render Dashboard mostró login sin sesión, por lo que destino de correo y SHA del backend siguen pendientes. Se dejaron login NotiUrba y Render para intervención personal.

No se crearon cuentas/reportes ni se enviaron correos. Los únicos datos introducidos eran sintéticos y quedaron en un borrador de formulario. No hay recorrido físico Xiaomi ni capturas móviles nuevas. [Evidencia real de Chrome](evidencias/reanudacion-20261114/chrome-ui.json). La captura del login se mostró en la herramienta; no se afirma tener un PNG guardado.

Para continuar se necesitan la activación personal de Google con una cuenta QA, el inicio de sesión personal en Render para leer RESEND_PRUEBA_DESTINO y la recuperación de acceso ADB. Postman CLI sigue bloqueado por DNS y definición nativa pendiente; los 34 fallos de aserción por no respuesta se conservan sin contarlos como defectos de API.
