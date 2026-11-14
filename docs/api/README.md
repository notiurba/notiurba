# Colección de Postman de NotiUrba

## Ampliación NOT-141 (14/11/2026)

Los recursos `NotiUrba API/NOT-141 N141-*.request.yaml` forman parte de esta misma colección oficial: 114 casos nuevos, con scripts `pm.test`, precondiciones y guardas. Se reutilizan `baseUrl`, `token`, `codigoReporte`, `tokenOtraPersona` y el entorno Render existente. No hay una segunda colección persistente.

Contratos revisados: copia local `d8e8b2b` y `develop` remoto `8b8419a`. No se certifica que ninguno sea el despliegue actual. La observación del cambio administrativo es opcional en el contrato nuevo; el negativo usa estado ausente. Se agregan `/api/admin/mapa`, `/api/admin/mapa/calor`, `/api/admin/estadisticas`, vista previa y mensaje del reporte.

**No ejecutar toda la colección heredada en producción:** incluye registro, recuperación y cargas que escriben o envían correo. Ejecutar primero únicamente los GET públicos y negativos sin sesión de NOT-141. Los valores de seguridad quedan en `false`; un caso omitido está bloqueado, nunca aprobado. Tokens y claves se introducen solamente en valores locales secretos de Postman, sin sincronizarlos ni guardarlos en archivos versionados.

El PUT válido de vigencia afecta la configuración global: está bloqueado contra Render incluso con la guarda activada y debe probarse en un entorno aislado. Para borrados se necesita una cuenta QA identificada, confirmación de que todos sus datos son descartables y autorización específica antes de ejecutar. Para enviar correo se necesita comprobar `RESEND_PRUEBA_DESTINO` en Render; un dato en Neon no demuestra esa configuración.

`node tools/not141-postman.mjs --validate` verifica sintaxis y guardas sin HTTP. `--safe-get` intenta solamente GET sin token y guarda resultados anonimizados; es un runner HTTP limitado, **no Newman ni ejecución de pm.test**. `--export RUTA_TEMPORAL.json` exporta los recursos NOT-141 a v2.1 para Newman; la fuente oficial sigue siendo YAML. La importación en Postman y la ejecución real de sus scripts permanecen pendientes.

Informe y detalle por caso: [NOT-141](../validacion/NOT-141/INFORME-FINAL-NOT-141.md). No se han modificado los 24 casos históricos de NOT-45.

## Cuál es la oficial
**`docs/api/NotiUrba API/`** es la única colección. Los dos entornos están en esta misma carpeta:
`NotiUrba - local.environment.yaml` (`http://localhost:8080`) y `NotiUrba - Render.environment.yaml`
(la API desplegada). Postman las lee según `.postman/resources.yaml`.

Ya no existen:
- `postman/collections/NotiUrba API (copia)/`: era un duplicado con 4 solicitudes. Sus pruebas
  (Health, Categorías y Admin ping) se pasaron a la colección oficial antes de retirarla.
- Los `.json` antiguos (`notiurba.postman_collection.json` y sus entornos): estaban en formato viejo y siguen en el
  historial de git.

Si tu Postman todavía muestra una colección «(copia)», elimínala desde la aplicación (y de la nube) y
trabaja solo con «NotiUrba API».

## Solicitudes
| Orden | Solicitud | Qué comprueba |
| --- | --- | --- |
| 1 | Health | 200 y base de datos en `ok` |
| 1.1 | Registro | 201, sin token; crea un correo y un documento nuevos en cada ejecución (NOT-75) |
| 1.2 | Registro con correo repetido (409) | 409 `CORREO_YA_REGISTRADO`, campo `correo` (ejecutar después de «Registro») |
| 1.3 | Registro con campo vacío (400) | 400 `VALIDACION`, campo `nombres` |
| 1.4 | Registro sin declarar mayoría de edad (400) | 400 `VALIDACION`, campo `declaraMayorEdad` |
| 2 | Login | 200 y guarda `token` |
| 3–4 | Login con Google, Completar perfil de Google | flujo con Google |
| 5–6 | Recuperar contraseña, Restablecer contraseña | recuperación |
| 7 | Categorías | 200 y las 15 categorías |
| 8 | Admin ping | 200 con rol ADMIN |
| 9 | Crear reporte | 201, `Location`, estado Recibido, vigencia de 30 días; guarda `codigoReporte` |
| 9.1 | Crear reporte sin categoría (400) | 400 `VALIDACION`, campo `idCategoria` |
| 9.2 | Crear reporte sin coordenadas (400) | categoría 1 sin GPS ni barrio: 400 `VALIDACION`, campo `idBarrio` |
| 9.3 | Crear reporte con descripción de 9 caracteres (400) | 400 `VALIDACION`, campo `descripcion` |
| 9.4 | Crear reporte con descripción de 501 caracteres (400) | 400 `VALIDACION`, campo `descripcion` |
| 9.5 | Crear reporte sin sesión (401) | 401 `NO_AUTENTICADO` (NOT-79) |
| 10 | Consultar reporte, inexistente (404), código mal formado (400), sin sesión (401), de otra persona (404) | NOT-89 |
| 11 | Subir foto, no es imagen (415), más de 1 MB (413), sin archivo (400) | NOT-52 |

Variables de colección: `baseUrl`, `token`, `codigoReporte`, `correoRegistro`, `documentoRegistro`, `documentoOtro` (las tres últimas las llena «Registro» solo) y `tokenOtraPersona` (opcional: sin ella se omite el caso de
«reporte de otra persona»).

## Antes de usarla
1. Elige un entorno y ejecuta **Login** con una cuenta real. Postman guarda en el archivo lo que escribas en el cuerpo:
   deshaz el cambio al terminar. **Nunca subas contraseñas ni tokens al repositorio.**
2. Las solicitudes de subir foto guardan la ruta **absoluta** del archivo. Cada persona debe volver a elegir los archivos
   de `docs/api/archivos-de-prueba/`. Detalles y pasos en [postman-casos-de-subida.md](postman-casos-de-subida.md).
3. Cada ejecución de «Registro» crea una cuenta de prueba (correo `delivered+…@resend.dev`, que Resend acepta sin entregar a nadie) y gasta 1 correo de los 100 diarios.
4. Cada ejecución de «Crear reporte» (el caso válido) y de «Subir foto» deja datos de prueba en la base y en Cloudinary.
