# Casos de Postman para subir fotos (NOT-52 / NOT-20)

La colección «NotiUrba API» (carpeta `docs/api/NotiUrba API`) ya trae los casos de **crear** y
**consultar** reportes. Faltan los de **subir foto**: llevan un cuerpo `form-data` con un archivo,
y ese formato lo tiene que generar la propia aplicación de Postman. Se crean una vez a mano y al
guardar quedan como archivos `.request.yaml` en la misma carpeta.

Archivos de prueba (en `docs/api/archivos-de-prueba/`):
- `foto-valida.png` — un PNG de 6 KB. Sirve para el caso feliz.
- `no-es-imagen.jpg` — texto con extensión `.jpg`. Debe rechazarse por su contenido.
- `documento.pdf` — un PDF mínimo. Debe rechazarse.
- Para el caso de más de 1 MB: cualquier foto de tu computador que pese más de 1 MB.

## Antes de empezar
1. Abre Postman en el espacio de trabajo del repositorio y elige el entorno **NotiUrba - Render** (arriba a la derecha).
2. Ejecuta **Login** con una cuenta real. Cuidado: Postman guarda en el archivo lo que escribas en el cuerpo.
   Al terminar, deshaz el cambio (Ctrl+Z en el cuerpo) o corre `git checkout -- "docs/api/NotiUrba API/Login.request.yaml"`.
   **Nunca subas una contraseña al repositorio.**
3. Ejecuta **Crear reporte**: guarda el código en la variable `codigoReporte`, que usan las solicitudes de abajo.

## Crear cada solicitud (carpeta «NotiUrba API», botón New → HTTP)
Todas: método **POST**, URL `{{baseUrl}}/api/reportes/{{codigoReporte}}/fotos`, pestaña
**Authorization** → Bearer Token → `{{token}}`, pestaña **Body** → `form-data`. En la fila del cuerpo
escribe la clave `foto` y cambia el desplegable de la derecha de *Text* a **File** para elegir el archivo.
No pongas la cabecera `Content-Type`: Postman la agrega sola con el límite del multipart.
Pega el script en la pestaña **Scripts → Post-response**.

| Nombre | Archivo | Resultado esperado |
| --- | --- | --- |
| Subir foto | `foto-valida.png` | 201 |
| Subir archivo que no es imagen (415) | `no-es-imagen.jpg` (repite con `documento.pdf`) | 415 FORMATO_NO_SOPORTADO |
| Subir foto de más de 1 MB (413) | una foto de más de 1 MB | 413 ARCHIVO_MUY_GRANDE |
| Subir sin archivo (400) | clave `otra` de tipo Text, sin la clave `foto` | 400 VALIDACION, campo `foto` |

### Script de «Subir foto»
```js
pm.test('Foto subida (201)', function () {
    pm.response.to.have.status(201);
});
if (pm.response.code === 201) {
    const r = pm.response.json();
    pm.test('Es el reporte consultado', function () {
        pm.expect(r.codigoSeguimiento).to.eql(pm.collectionVariables.get('codigoReporte'));
    });
    pm.test('Trae el orden y una URL https pública', function () {
        pm.expect(r.foto.orden).to.be.within(1, 3);
        pm.expect(r.foto.url).to.match(/^https:\/\//);
    });
}
```

### Script de los casos de error (cambia el código y el campo esperados)
```js
// 415: pm.response.to.have.status(415), codigo 'FORMATO_NO_SOPORTADO'
// 413: pm.response.to.have.status(413), codigo 'ARCHIVO_MUY_GRANDE'
// 400: pm.response.to.have.status(400), codigo 'VALIDACION'
pm.test('Rechazada (415)', function () {
    pm.response.to.have.status(415);
});
pm.test('Formato de error del contrato', function () {
    const r = pm.response.json();
    pm.expect(r.codigo).to.eql('FORMATO_NO_SOPORTADO');
    pm.expect(r.campo).to.eql('foto');
    pm.expect(r.mensaje).to.be.a('string').and.not.empty;
});
```

## Rutas de los archivos: cada persona debe reseleccionarlos
Postman guarda en cada solicitud la ruta **absoluta** del archivo, y las que están en el repositorio apuntan a la
carpeta de quien las creó (`/Users/andy/...`). En otro computador esa ruta no existe, así que cada persona debe
**volver a elegir el archivo** en la pestaña Body de estas solicitudes:
- «Subir foto» y «Subir archivo que no es imagen (415)»: reseleccionar los archivos de `docs/api/archivos-de-prueba/`.
- «Subir foto de más de 1 MB (413)»: apunta a una foto de la carpeta Descargas de Andrés. **Cada quien debe elegir una
  foto propia que pese más de 1 MB.**
- «Subir sin archivo (400)» no lleva archivo y no necesita nada.

No se convirtieron a rutas relativas porque no está confirmado que Postman las resuelva desde la carpeta del repositorio;
si lo hiciera, el cambio se haría con una prueba en otro computador.

## Ejecutar todo
Clic derecho en la colección → **Run**, o ejecuta una por una en este orden: Login → Crear reporte →
Consultar reporte → (casos de subida) → los casos 404, 400 y 401 de consulta. Todas las pruebas deben salir en verde.
La de «otra persona» se omite sola si no defines la variable `tokenOtraPersona`.

Cada ejecución de «Crear reporte» y de «Subir foto» deja datos de prueba en la base y en Cloudinary.

## Resultados registrados (2026-11-01, entorno «NotiUrba - Render»)
Ejecutados por Andrés en Postman contra la API desplegada:

| Solicitud | Resultado | Pruebas |
| --- | --- | --- |
| Subir foto | 201 | 3 de 3 |
| Subir archivo que no es imagen (415) | 415 | 2 de 2 |
| Subir sin archivo (400) | 400 | 2 de 2 |
| Subir foto de más de 1 MB (413) | 413 | 2 de 2 |
| Consultar reporte de otra persona (404) | SKIPPED | se omite hasta tener la variable `tokenOtraPersona` |

Los casos de consulta (200, 404, 400 y 401) se comprobaron el mismo día con un ejecutor local que corre los mismos
scripts contra la API con PostGIS (28 comprobaciones sin fallos). Falta anotar aquí su ejecución en Postman.
