# Contratos y cobertura de Postman — NOT-141

Fuentes: controladores/DTO locales d8e8b2b y lectura de develop remoto 8b8419a. La fuente remota es posterior y no acredita despliegue. Los ejemplos son **expectativas del contrato con datos sintéticos**, nunca capturas ni respuestas inventadas del servidor. Las estructuras se extraen de los records Java; la ejecución podrá revelar diferencias de versión.

| Método/ruta | Éxito del contrato | Cuerpo/estructura esperada | Casos negativos preparados |
|---|---:|---|---|
| GET /api/health | 200 | estado, baseDeDatos; 503 si SELECT falla | Caso existente oficial; disponibilidad bloqueada |
| GET /api/catalogos/tipos-documento | 200 | Lista de {idTipoDocumento,nombre,codigo} | Público; sin cuerpo ni filtro obligatorio |
| GET /api/catalogos/barrios | 200 | Lista de {idBarrio,idComuna,nombre} | Público; no inventar 401/403 |
| GET /api/reportes | 200 | Lista ReporteResumenResponse | Estado inválido 400, sin sesión/vencido 401 |
| GET /api/usuarios/me | 200 | PerfilResponse con ocho campos | Sin sesión/vencido 401; recurso propio |
| PUT /api/usuarios/me | 200 | PerfilResponse persistido | Falta nombres 400, sin sesión/vencido 401 |
| DELETE /api/usuarios/me/datos | 204 | Sin cuerpo; cuenta permanece | Sin sesión/vencido 401; no recibe cuerpo |
| DELETE /api/usuarios/me | 204 | Sin cuerpo; cuenta eliminada | Sin sesión/vencido 401; prohibición admin está en servicio, falta caso autenticado admin QA |
| PATCH /api/reportes/{codigo}/actualizacion | 200 | ReporteDetalleResponse, estado preservado y nota en historial | Tipo inválido 400, sin sesión/vencido 401, otra QA 404 |
| GET /api/admin/reportes | 200 | Lista ReporteDetalleResponse | Comuna inválida/fechas invertidas 400, sin sesión/vencido 401, ciudadano 403 |
| PATCH /api/admin/reportes/{codigo}/estado | 200 | codigoSeguimiento,estado,observacion,cambiadoEn,cambiadoPor,vigenteHasta | Estado ausente 400, sin sesión/vencido 401, ciudadano 403 |
| GET /api/admin/configuracion/vigencia | 200 | {vigenciaDias: entero} | Sin sesión/vencido 401, ciudadano 403 |
| PUT /api/admin/configuracion/vigencia | 200 | {vigenciaDias: entero positivo} | Cero 400, sin sesión/vencido 401, ciudadano 403 |
| GET /api/enrutamiento/categorias | 200 | Lista Ruta, con canales y esEmpresa en contrato actual | Sin sesión/vencido 401 |
| GET /api/enrutamiento/categorias/{id} | 200 | Ruta | ID 0: 400, sin sesión/vencido 401 |
| GET /api/reportes/opciones/barrios | 200 | Lista de barrios públicos | Sin sesión/vencido 401 |
| GET /api/reportes/{codigo}/envio | 200 | estado,motivo,reintentable,rebote,ruta | Otra QA 404; sin sesión/vencido 401 |
| POST /api/reportes/{codigo}/enviar | 202 | Estado de envío; no garantiza entrega | Sin sesión/vencido 401; válido protegido por destino QA |
| POST /api/reportes/{codigo}/fotos | 201 | codigoSeguimiento,foto:{orden,url} | Cuarta: 409 FOTOS_MAXIMO; sin sesión/vencido 401 |
| GET /api/admin/mapa | 200 | total,sinComuna,truncado,reportes | Filtros inválidos 400; sin sesión/vencido 401; ciudadano 403 |
| GET /api/admin/mapa/calor | 200 | totalReportes,totalPuntos,truncado,sinComuna,puntos,comunasCriticas | Comuna/página inválidas 400, seguridad 401/403 |
| GET /api/admin/estadisticas | 200 | vigencia,total,atendidos,porcentajeAtendidos,sinComuna,porEstado,porCategoria,porComuna | Comuna/vigencia inválidas 400, seguridad 401/403 |
| POST /api/reportes/vista-previa | 200 | asunto,texto,secciones,permiteCorreo,ruta,requiereEvidencia | Categoría ausente 400, seguridad 401 |
| GET /api/reportes/{codigo}/mensaje | 200 | asunto,texto | Otra QA 404, seguridad 401 |

El endpoint diagnóstico espacial es condicional y no debe exigirse como criterio del MVP. Su caso permanece omitido por defecto. Las consultas de mapa actual son adicionales al listado usado por el frontend antiguo: no se supone que la UI ya las consuma.

## Ejemplos documentales

PUT /api/admin/configuracion/vigencia con cuerpo {"vigenciaDias":30} devuelve HTTP 200 y {"vigenciaDias":30} si se autoriza y guarda ese valor. Es una configuración global: caso válido prohibido en Render por la guarda.

DELETE de datos/cuenta, autorizado sobre QA, devuelve 204 sin JSON. Un test no debe intentar parsear cuerpo en ese éxito. La ausencia de cuerpo no acredita por sí sola que se borró: comprobar cuenta/listado/fotos después.

GET /api/admin/mapa con filtros sin coincidencias puede devolver {"total":0,"sinComuna":0,"truncado":false,"reportes":[]}. Es un ejemplo condicionado a un conjunto vacío conocido; no afirmar que la base actual esté vacía.

GET /api/reportes de una QA sin reportes devuelve []. Para comprobar persistencia no basta validar array: crear previamente un reporte QA, consultar su código, detalle, foto e historial y encontrarlo en Mis reportes.

Los errores usan codigo, mensaje y campo. Para 401 el código es NO_AUTENTICADO, para 403 SIN_PERMISO y para acceso a reporte ajeno 404 NO_ENCONTRADO. El texto de mensaje se valida por contrato de campo, sin inventar una cadena del despliegue. En el rechazo de cuarta foto se exige explícitamente FOTOS_MAXIMO.

## Límites de cobertura pendientes de ejecución

Los casos válidos requieren IDs del catálogo real y recursos QA propios; los placeholders vacíos deben mantenerse bloqueados. El caso de JWT vencido requiere además confirmar que se obtuvo como JWT firmado previamente válido; decodificar exp localmente no verifica la firma.

Las reglas de transición terminal/retroceso y prohibición de eliminar administrador se revisaron en código, pero faltan escenarios autenticados adicionales y sus ejecuciones en QA. Las 114 solicitudes no equivalen a cobertura exhaustiva de todos los caminos. No se declara terminada la colección desde el punto de vista de importación y ejecución en Postman.

