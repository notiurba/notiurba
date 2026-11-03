# Colección de Postman de NotiUrba

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
| 9 | Crear reporte | 201, `Location`, estado Recibido; guarda `codigoReporte` |
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
4. Cada ejecución de «Crear reporte» y de «Subir foto» deja datos de prueba en la base y en Cloudinary.
