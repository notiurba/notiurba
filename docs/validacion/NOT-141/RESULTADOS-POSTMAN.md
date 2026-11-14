# Resultados Postman y HTTP — NOT-141

Fecha de ejecución HTTP: 2026-11-14T15:05:15.765Z (UTC; Colombia UTC-5). Entorno: Render. Cliente: Node 24.18.0 en Windows.

**Newman y pm.test no ejecutados.** Newman no está instalado; la importación en Postman UI no fue posible. Se prepararon 114 recursos nativos en la colección oficial y un exportador temporal v2.1. Sintaxis y guardas se comprobaron localmente. El JSON no se mantiene como segunda colección.

| Métrica | Cantidad |
|---|---:|
| Casos nuevos preparados | 114 |
| GET seguros intentados | 12 |
| Sin envío por precondición/alcance | 102 |
| Aprobados Render | 0 |
| Fallidos con HTTP | 0 |
| Bloqueados | 114 |
| Casos Newman | 0 |

Los 12 intentos terminaron en ENOTFOUND. Sin HTTP no se evalúan aserciones de aplicación. Los códigos esperados proceden del contrato; no son respuestas capturadas. El estado BLOQUEADA incluye casos sin envío.

## Casos individuales

Cada fila corresponde a docs/api/NotiUrba API/NOT-141 ID.request.yaml; fecha y URL completa están en [evidencia JSON](evidencias/resultados-http-seguros.json).

| ID | Escenario | Método | HTTP esperado | Obtenido | Resultado | Motivo |
|---|---|---|---:|---|---|---|
| N141-001 | Tipos documento: público | GET | 200 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-002 | Barrios: público | GET | 200 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-003 | Verificar correo sin token de enlace | POST | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-004 | Verificar correo con token válido QA | POST | 200 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-005 | Reenviar verificación con correo QA | POST | 200 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-006 | Recuperación sin correo | POST | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-007 | Recuperación con correo QA | POST | 200 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-008 | Restablecer: falta token | POST | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-009 | Google: credencial ausente | POST | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-010 | Mis reportes: listado propio | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-011 | Mis reportes: sin JWT | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-012 | Mis reportes: JWT vencido | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-013 | Mis reportes: filtro por estado | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-014 | Detalle del reporte propio | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-015 | Reporte ajeno/inexistente: 404 | GET | 404 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-016 | Consultar mi perfil | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-017 | Consultar perfil sin sesión | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-018 | Editar perfil: falta nombres | PUT | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-019 | Editar perfil: caso válido QA | PUT | 200 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-020 | Eliminar mis datos: sin sesión | DELETE | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-021 | Eliminar cuenta: sin sesión | DELETE | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-022 | Eliminar MIS datos (solo QA aislada) | DELETE | 204 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-023 | Eliminar MI cuenta (solo QA aislada) | DELETE | 204 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-024 | Actualizar mi reporte sin sesión | PATCH | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-025 | Actualizar mi reporte: tipo inválido | PATCH | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-026 | Actualizar mi reporte: caso válido QA | PATCH | 200 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-027 | Cuarta foto (con tres subidas previas) | POST | 409 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-028 | Estado del envío de correo | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-029 | Consultar enrutamiento por categoría | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-030 | Solicitud de envío de correo QA | POST | 202 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-031 | Subir fotografía 1 QA | POST | 201 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-032 | Subir fotografía 2 QA | POST | 201 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-033 | Subir fotografía 3 QA | POST | 201 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-034 | Admin: listado (cuenta admin) | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-035 | Admin: sin sesión | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-036 | Admin: rol ciudadano prohibido | GET | 403 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-037 | Admin: filtrado por comuna/estado | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-038 | Vigencia actual | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-039 | Cambiar estado falta estado | PATCH | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-040 | Cambiar estado (QA) | PATCH | 200 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-041 | Configuración vigencia: sin sesión | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-042 | Diagnóstico espacial opcional | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-043 | Enrutamiento completo | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-044 | Enrutamiento categoría inválida | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-045 | Barrios opciones reporte | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-046 | Mis reportes estado inválido | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-047 | Listado admin comuna inválida | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-048 | Listado admin fechas invertidas | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-049 | PUT vigencia inválida | PUT | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-050 | PUT vigencia válida solo entorno aislado | PUT | 200 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-051 | Mapa real | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-052 | Mapa comuna inválida | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-053 | Mapa con filtros | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-054 | Calor real | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-055 | Calor comuna inválida | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-056 | Calor con filtros | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-057 | Estadísticas real | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-058 | Estadísticas comuna inválida | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-059 | Estadísticas con filtros | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-060 | Calor paginación inválida | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-061 | Estadísticas vigencia inválida | GET | 400 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-062 | Mensaje del reporte | GET | 200 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-063 | Vista previa sin categoría | POST | 400 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-064 | Vista previa válida sin persistencia | POST | 200 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-065 | Acceso otra QA GET /api/reportes/{{codigoReporte}} | GET | 404 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-066 | Acceso otra QA PATCH /api/reportes/{{codigoReporte}}/actualizacion | PATCH | 404 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-067 | Acceso otra QA GET /api/reportes/{{codigoReporte}}/envio | GET | 404 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-068 | Acceso otra QA GET /api/reportes/{{codigoReporte}}/mensaje | GET | 404 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-069 | Sin sesión GET /api/reportes/{{codigoReporte}} | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: recurso QA no preparado |
| N141-070 | JWT vencido GET /api/reportes/{{codigoReporte}} | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-071 | Sin sesión GET /api/reportes/{{codigoInexistente}} | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: recurso QA no preparado |
| N141-072 | JWT vencido GET /api/reportes/{{codigoInexistente}} | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-073 | JWT vencido GET /api/usuarios/me | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-074 | Sin sesión PUT /api/usuarios/me | PUT | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-075 | JWT vencido PUT /api/usuarios/me | PUT | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-076 | JWT vencido DELETE /api/usuarios/me/datos | DELETE | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-077 | JWT vencido DELETE /api/usuarios/me | DELETE | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-078 | JWT vencido PATCH /api/reportes/{{codigoReporte}}/actualizacion | PATCH | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-079 | Sin sesión POST /api/reportes/{{codigoReporte3Fotos}}/fotos | POST | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-080 | JWT vencido POST /api/reportes/{{codigoReporte3Fotos}}/fotos | POST | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-081 | Sin sesión GET /api/reportes/{{codigoReporte}}/envio | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: recurso QA no preparado |
| N141-082 | JWT vencido GET /api/reportes/{{codigoReporte}}/envio | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-083 | Sin sesión GET /api/enrutamiento/categorias/{{idCategoria}} | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: recurso QA no preparado |
| N141-084 | JWT vencido GET /api/enrutamiento/categorias/{{idCategoria}} | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-085 | Sin sesión POST /api/reportes/{{codigoReporte}}/enviar | POST | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-086 | JWT vencido POST /api/reportes/{{codigoReporte}}/enviar | POST | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-087 | JWT vencido GET /api/admin/reportes | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-088 | JWT vencido GET /api/admin/configuracion/vigencia | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-089 | Sin rol admin GET /api/admin/configuracion/vigencia | GET | 403 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-090 | Sin sesión PATCH /api/admin/reportes/{{codigoReporte}}/estado | PATCH | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-091 | JWT vencido PATCH /api/admin/reportes/{{codigoReporte}}/estado | PATCH | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-092 | Sin rol admin PATCH /api/admin/reportes/{{codigoReporte}}/estado | PATCH | 403 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-093 | Sin sesión GET /api/enrutamiento/categorias | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-094 | JWT vencido GET /api/enrutamiento/categorias | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-095 | Sin sesión GET /api/enrutamiento/categorias/0 | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-096 | JWT vencido GET /api/enrutamiento/categorias/0 | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-097 | Sin sesión GET /api/reportes/opciones/barrios | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-098 | JWT vencido GET /api/reportes/opciones/barrios | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-099 | Sin sesión PUT /api/admin/configuracion/vigencia | PUT | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-100 | JWT vencido PUT /api/admin/configuracion/vigencia | PUT | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-101 | Sin rol admin PUT /api/admin/configuracion/vigencia | PUT | 403 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-102 | Sin sesión GET /api/admin/mapa | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-103 | JWT vencido GET /api/admin/mapa | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-104 | Sin rol admin GET /api/admin/mapa | GET | 403 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-105 | Sin sesión GET /api/admin/mapa/calor | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-106 | JWT vencido GET /api/admin/mapa/calor | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-107 | Sin rol admin GET /api/admin/mapa/calor | GET | 403 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-108 | Sin sesión GET /api/admin/estadisticas | GET | 401 | Sin HTTP | BLOQUEADA | ENOTFOUND |
| N141-109 | JWT vencido GET /api/admin/estadisticas | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-110 | Sin rol admin GET /api/admin/estadisticas | GET | 403 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-111 | Sin sesión GET /api/reportes/{{codigoReporte}}/mensaje | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: recurso QA no preparado |
| N141-112 | JWT vencido GET /api/reportes/{{codigoReporte}}/mensaje | GET | 401 | Sin HTTP | BLOQUEADA | No ejecutada: necesita token QA verificado o administrador |
| N141-113 | Sin sesión POST /api/reportes/vista-previa | POST | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |
| N141-114 | JWT vencido POST /api/reportes/vista-previa | POST | 401 | Sin HTTP | BLOQUEADA | No ejecutada: solo lectura; requiere preparación y ejecución Postman |

## Cómo reanudar

Elegir el entorno Render existente; reutilizar token, codigoReporte y tokenOtraPersona. No guardar secretos en archivos. Ejecutar únicamente el subconjunto seguro antes de preparar recursos QA. Las guardas requieren Postman con pm.execution.skipRequest; si la versión no lo soporta, no ejecutar escrituras. La versión de Postman instalada no fue acreditada; importación pendiente.

La carpeta histórica NOT-45 (24 casos) permanece intacta y no se ejecutó: incluye operaciones que escriben/envían. No sumar sus casos a los 114 intentados o preparados en esta ampliación.

Los catálogos no reciben cuerpo ni filtros, por lo que datos faltantes/caso de rol prohibido no aplican a esos GET públicos. Las operaciones de perfil propio no exigen un rol exclusivo; el acceso a reportes de otra cuenta se oculta con 404, no 403. DELETE no recibe cuerpo: no se inventa un negativo por campo faltante. Token vencido requiere token firmado previamente válido, exp pasado y confirmación de su procedencia; un texto malformado no acredita expiración.

PUT válido de vigencia: solo entorno aislado, pues cambia configuración global. Cuarta foto: reporte QA con exactamente tres fotos ya persistidas y JPG/PNG válido de hasta 1 MiB. Se incluyen cargas 1, 2, 3 y cuarta rechazada. Antes de actualizar un reporte, consultar detalle para guardar estado/historial y verificar que se agrega una nota conservando estado oficial.

Contrato y ejemplos esperados: [CONTRATOS-Y-COBERTURA.md](CONTRATOS-Y-COBERTURA.md).


## Reanudación del 14/11/2026 — limpieza adicional y accesos

Esta sección registra avances posteriores a la primera revisión. Los resultados anteriores se conservan; repetir una solicitud no crea otro caso de aceptación. Estados: **APROBADO** solo con ejecución conforme y alcance explícito; **FALLIDO** con defecto reproducible; **BLOQUEADO** por acceso/precondición; **PENDIENTE** aún sin ejecución.

**Actualización:** Postman CLI 1.85.0 sí está instalado y se utilizó; Newman continúa ausente. Se exportaron temporalmente los recursos v2.1 y se seleccionaron exclusivamente 12 GET sin autenticación, sin variables secretas ni cuerpos. No se ejecutó la colección heredada completa.

| Ejecución nueva | Solicitudes intentadas | HTTP recibidos | Scripts test | Aserciones | Resultado QA |
|---|---:|---:|---:|---:|---|
| Runner Node seguro | 12 | 0 | No ejecuta pm.test | No aplica | 12 BLOQUEADAS por ENOTFOUND |
| Postman CLI 1.85.0, JSON v2.1 | 12 | 0 | 12 ejecutados | 34 evaluadas, 0 aprobadas y 34 fallidas | 12 BLOQUEADAS por ENOTFOUND |

Las 34 aserciones fallan porque pm.response no contiene código/cuerpo: expected 200/401 frente a undefined y JSONError. Se registra el fallo de las aserciones del runner, pero **0 fallos funcionales de Render demostrados**. Total de escenarios preparados: 114; mismos 12 GET repetidos, 102 sin enviar. No contar intentos repetidos como nuevos casos.

La CLI rechazó la definición nativa tanto con los recursos actuales como con definition.yaml + Health.request.yaml extraídos de HEAD en una carpeta temporal aislada. No se diagnosticó todavía por qué; no se alteró el formato oficial por conjetura. El subconjunto JSON v2.1 sí fue aceptado y sus scripts ejecutados. La opción --output exige login; se continuó sin ella, con --no-report-events, sin credenciales ni publicación acreditada.

Se conserva intacto el resultado Node original. Nuevas evidencias separadas: [HTTP Node](evidencias/reanudacion-20261114/resultados-http-seguros.json) y [consola Postman CLI](evidencias/reanudacion-20261114/postman-cli.txt). Falta resolver compatibilidad nativa y ejecutar todos los casos autorizados con acceso real y recursos QA.


## Avance posterior en Chrome — 14/11/2026

Se comprobaron fuera de Postman, mediante Chrome y el frontend real, los dos GET de catálogos con HTTP 200. No se cambian por ello las 12 solicitudes CLI bloqueadas ni se simula ejecución de pm.test. La diferencia de acceso entre navegador y terminal es real: la terminal mantiene ENOTFOUND. /api/health mostró cuerpo correcto, sin código HTTP capturado. [Evidencia real de Chrome](evidencias/reanudacion-20261114/chrome-ui.json).
