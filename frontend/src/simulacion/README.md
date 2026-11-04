# Datos de prueba y contrato por pantalla (NOT-44)

Mientras la API no esté lista, las pantallas se construyen con estos JSON. Cada archivo es la
respuesta (o la solicitud) de **un endpoint**. Cuando el endpoint exista de verdad, la pantalla
no cambia: solo cambia de dónde vienen los datos.

**Estado:** `EXISTE` = ya está en `develop` (backend) · `PROPUESTO` = lo definió el frontend a
partir del modelo de datos v8 y los requisitos; **falta confirmarlo con Andrés** (NOT-17).

## Cómo se usan estos datos

Esta carpeta (`src/simulacion/`) **simula el backend dentro del frontend**. No toca el backend real.

1. Copie `frontend/.env.example` como `frontend/.env.local` (no se sube a Git) y reinicie `pnpm dev`.
2. Con `VITE_USAR_MOCKS=true` se simula toda la app: login, registro, recuperar/restablecer contraseña,
   completar perfil, barrios, tipos de documento, categorías, perfil y ajustes, reportes del ciudadano
   (crear, listar, detalle, actualizar) y el panel de administración (listar y cambiar estado).
   Los datos se guardan en `localStorage` (clave `notiurba.simulacion.v1`); para volver al inicio,
   borre esa clave en las herramientas del navegador. Cualquier ruta sin simulación va a la API real.
3. Cuentas de prueba para el login (contraseña `Clave1234`): `ciudadano@correo.com` entra a `/app` y
   `admin@correo.com` entra a `/admin`. Otro correo → «no encontramos una cuenta»; otra contraseña →
   «no es correcta».
4. Datos que provocan errores en el registro: el correo `repetido@correo.com` devuelve «ya registrado»
   y el documento `1098765432` devuelve «ya existe una cuenta con este documento».
5. Las rutas simuladas están en `src/simulacion/index.js`. Cuando Andrés publique un endpoint se borra
   su línea y la pantalla usa la API real sin otro cambio.
6. El build de producción **no incluye** esta carpeta.
7. **Para eliminar la simulación:** borre la carpeta `src/simulacion/` completa. No hay que editar
   nada más: `api/cliente.js` la busca con `import.meta.glob` y, si no está, usa la API real.

## Contrato por pantalla

| Pantalla | Endpoint | Estado | Envía | Recibe | Archivos |
|---|---|---|---|---|---|
| Login | `POST /api/auth/login` | EXISTE | `correo`, `password` | `{ token, rol }` · 401 si no coinciden. **Propuesto:** el 401 trae `campo` (`correo` o `password`) para mostrar el error bajo la casilla | `datos/auth/login.ok.*.json`, `datos/auth/login.error.*.json` |
| Registro | `POST /api/auth/registro` | PROPUESTO | `nombres`, `apellidos`, `idTipoDocumento`, `numeroDocumento`, `idBarrio`, `direccionCorrespondencia`, `correo`, `telefono?`, `password`, `aceptaPrivacidad` | mensaje de éxito · error con `campo` | `datos/auth/registro.*.json` |
| Verificar correo | por definir | PROPUESTO | — | el prototipo muestra «Abrir mi correo» y «Reenviar enlace» | — |
| Completar perfil (Google) | `POST /api/auth/google/completar` | EXISTE (campos por confirmar) | `nombres`, `apellidos`, `idTipoDocumento`, `numeroDocumento`, `telefono?`, `idBarrio`, `direccionCorrespondencia`, `aceptaPrivacidad` | sesión `{ token, rol }` | `datos/auth/google.completar-perfil.solicitud.json` |
| Recuperar contraseña | `POST /api/auth/recuperar` | EXISTE (campos por confirmar) | `correo` | mensaje igual exista o no la cuenta; enlace de 1 hora, un solo uso | `datos/auth/recuperar.*.json` |
| Nueva contraseña | `POST /api/auth/restablecer` | EXISTE (campos por confirmar) | `token`, `password` | OK · error si el enlace venció | `datos/auth/restablecer.*.json` |
| Registro (selectores) | `GET /api/tipos-documento` (público, sin sesión) | PROPUESTO | — | lista de tipos | `datos/catalogos/tipos-documento.json` |
| Registro (selectores) | `GET /api/barrios` (público, sin sesión) | PROPUESTO | — | lista con `idComuna` | `datos/catalogos/barrios.json` |
| Crear reporte (categoría) | `GET /api/categorias` | EXISTE (exige sesión) | — | lista de categorías activas | `datos/catalogos/categorias.json` |
| Crear reporte | `POST /api/reportes` | PROPUESTO | `idCategoria`, `descripcion` (10-500), `latitud`, `longitud`, `idBarrio`, `direccionIncidente?`, `matriculaCuentaServicio?`, `autorizaEnvio` | `idReporte`, `codigoSeguimiento`, `estado`, `idComuna` · error con `campo` | `datos/reportes/crear.*.json` |
| Crear reporte (foto) | `POST /api/reportes/{id}/fotos` | PROPUESTO | imagen JPG/PNG de máximo 1 MB, hasta 3 (`orden` 1-3) | URL de la foto | — |
| Confirmación | (respuesta de crear) | PROPUESTO | — | `codigoSeguimiento` | `datos/reportes/crear.ok.json` |
| Mis reportes | `GET /api/reportes/mios` | PROPUESTO | — | lista con estado | `datos/reportes/mis-reportes.json` |
| Seguimiento | `GET /api/reportes/{codigo}` | PROPUESTO | — | detalle + `historial` con observaciones · «no se encontró» | `datos/reportes/detalle*.json` |
| Perfil / ajustes | `GET` y `PUT /api/usuarios/me`; `DELETE /api/usuarios/me/datos` y `/me` | PROPUESTO | datos del perfil | perfil · 204 | `almacen.js` |
| Actualizar mi reporte | `PATCH /api/reportes/{codigo}/actualizacion` | PROPUESTO | `tipo` (`RESPUESTA` o `RESUELTO`), `radicado?` | reporte actualizado | `almacen.js` |
| Admin: reportes | `GET /api/admin/reportes` (rol ADMIN) | PROPUESTO | — | todos los reportes | `almacen.js` |
| Admin: cambiar estado | `PATCH /api/admin/reportes/{codigo}/estado` (rol ADMIN) | PROPUESTO | `estado`, `observacion?` | reporte con historial | `almacen.js` |
| Estados (colores/etiquetas) | `GET /api/estados` | PROPUESTO | — | 4 estados con `orden` | `datos/catalogos/estados.json` |

Errores: la pantalla solo necesita el **número de estado** (401, 409, 422…) y, cuando hay,
`campo` para saber bajo qué casilla mostrar el mensaje. El formato `{ codigo, mensaje, campo }`
es una propuesta; hoy el backend responde con el error por defecto de Spring.

## Lo que sale del modelo de datos v8 y de los requisitos

- El usuario guarda `nombres` y `apellidos` por separado, tipo y número de documento, barrio y
  dirección de correspondencia (Ley 1755, art. 16). RF-01 y RN-07.
- Aceptar el aviso de privacidad es obligatorio y se guarda con fecha y hora (RF-03, RN-06).
- Reporte: categoría y descripción de 10 a 500 caracteres (RF-05); foto JPG/PNG reducida a
  1 MB (RF-06); punto GPS que el ciudadano puede mover, o fijar tocando el mapa (RF-07).
  Fuera de las 10 comunas se rechaza (RF-08, RN-03).
- Estados: `Recibido`, `Enviado`, `En trámite`, `Resuelto`. Los cambia a mano el administrador
  y puede dejar una observación (RF-10).
- Categoría 12 (gas natural): las fugas y emergencias no se envían por correo; la app debe
  indicar llamar a la línea 164 (RN-16).

## Datos que faltan (para Andrés)

1. Endpoint de **registro** con correo y contraseña (hoy solo existe el de Google) y su respuesta.
2. Listas de **barrios** y de **tipos de documento**: no hay endpoint (`idBarrio` e
   `idTipoDocumento` son obligatorios).
3. Cómo se **crea el reporte**: ¿una sola llamada con la foto (multipart) o primero el reporte
   y luego las fotos? Y qué devuelve.
4. Formato del **código de seguimiento** (aquí se supone `NU-000123`).
5. **Envío del correo** (ADR-0002, aceptada el 29/10): se envía con Resend, con el ciudadano en
   Reply-To; el borrador quedó descartado. Falta saber si la API envía al crear el reporte o en un
   paso aparte, y qué devuelve si Resend falla (para el aviso con reintento, NOT-74).
6. Formato de **errores** (400, 401, 409, 422) y si habrá `campo`.
7. Los endpoints de **Mis reportes** y **seguimiento**, y si el seguimiento por código es
   solo de reportes propios (RF-10).
8. Declaración «Declaro ser mayor de edad»: el Doc la deja como propuesta pendiente (el MVP es solo para
   mayores, decisión del 28/10) y el prototipo nuevo **no** la incluye; solo trae la casilla de la
   Ley 1581. Falta que el equipo confirme que así queda.
9. Cómo se **crea una cuenta ADMIN** en producción (hoy hay un usuario sembrado en `V4`).
10. La ruta de «Nueva contraseña» debería estar **fuera de `/app`** (esa zona exige sesión).

11. **Verificación del correo**: el Doc la deja como decisión abierta (confirmar por enlace, que gasta
    cuota de Resend, o aceptar el riesgo en el MVP) pero el prototipo ya trae la pantalla «Revise su
    correo». ¿Se hace? Si sí: ¿la cuenta queda inactiva hasta abrir el enlace y cuál es el endpoint de
    «Reenviar enlace»?
12. **Errores por campo** en login y registro: `campo` = `correo`, `password`, `numeroDocumento`...
    Hoy el login devuelve el mismo 401 («Credenciales inválidas», `AuthController`, línea 37) para
    correo inexistente y para contraseña mala. Si el equipo decide decir cuál falló, debe separarse.
    Ojo: es una decisión que cambia RF-02 y el diseño, y permite saber qué correos están registrados.
13. Los cuerpos exactos de `google/completar`, `recuperar` y `restablecer` (el frontend aún no los usa).

Ya resueltos con el repo: los roles son `CIUDADANO` y `ADMIN`; vienen en la respuesta del
login y dentro del token; el token dura 60 minutos y no hay renovación; el despliegue del
frontend es Netlify desde `develop`; las variables de entorno son `VITE_API_URL` (Netlify).


**Nota:** «Pendiente de envío» no es un estado del reporte (es el estado del envío del correo); por eso no está en el catálogo de estados.
**Pendiente con Andrés:** el mapa de calor del panel sigue usando sus propios datos de prueba (`admin/datos/reportesPrueba.js`), no los reportes simulados.
