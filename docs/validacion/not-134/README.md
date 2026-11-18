# NOT-134 — Verificación local, 17 de noviembre de 2026

Rama `feature/not-134-pwa-instalable`, base `50b583d`. Cambios sin commit;
no se cambió de rama, no hubo push, PR ni despliegue. Backend y base sin cambios.

## Resultado

| Comprobación | Resultado real |
| --- | --- |
| `pnpm install` | Correcto, pnpm 12.15.0 |
| `pnpm lint` | Correcto, sin errores |
| `pnpm test` | 39 archivos aprobados, 0 fallidos |
| Conteo de casos, ejecutando cada `*.test.mjs` con Node | **245 aprobados, 0 fallidos, 0 omitidos**; [detalle](pruebas.json) |
| `pnpm build` | Correcto; 63 entradas estáticas precargadas, 1229,17 KiB |
| Chrome 150.0.7871.186, perfil temporal persistente, headless | **8 comprobaciones aprobadas**; [evidencia](chrome.json) |
| `code-reviewer`, segunda revisión | Tres hallazgos corregidos; ninguno nuevo que impida cerrar la revisión local |

Seis pruebas nuevas: tres del aviso y tres del build/manifiesto/worker. La suite previa
contenía 239 casos en 37 archivos. La ejecución inicial de esos archivos pasó. Las
regresiones del aviso demostraron dos fallos antes de corregirlos y luego pasaron las tres.
El build falló al incorporar el registro virtual por no resolver `workbox-window`;
se declaró la dependencia directa 7.4.1 y volvió a pasar.

## Qué se comprobó en Chrome

Se ejecutó `pnpm preview --host 127.0.0.1 --port 4173 --strictPort`. Mediante Chrome
DevTools Protocol se consultó `Page.getAppManifest` y `Page.getInstallabilityErrors`,
las comprobaciones del navegador usadas para inspeccionar el manifiesto. No se inspeccionó
visualmente el panel DevTools. El resultado fue manifiesto válido y lista de errores vacía.

1. Manifiesto con NotiUrba, inicio `/`, `standalone`, colores e iconos PNG de 192/512 reales.
2. Desconectar muestra un diálogo modal; Escape no lo cierra y reintentar no recarga.
3. Reconectar conserva el correo escrito en login y devuelve el foco al campo.
4. El aviso queda encima de un diálogo modal que ya estaba abierto.
5. Recargar `/login` sin red muestra la app; sin desbordamiento horizontal a 360, 390 y 1366 px.
6. Abrir sin red `/reportar/categoria`, una ruta no visitada previamente, muestra el aviso.
7. Cache Storage contiene solo estáticos, incluso después de pedir una URL `/api/` local
   y una tesela simulada. No se consultó la API real ni se descargó esa tesela del proveedor.
8. Se generó una segunda compilación en `/tmp`, con una marca distinta en el HTML.
   En otro puerto local se sirvió primero el build original y luego el nuevo. Tras solicitar
   la comprobación de actualización, Chrome activó el worker, cambió la revisión del HTML
   y recargó la pantalla automáticamente. Ambas compilaciones usan `registerSW`.

Captura: [pantalla sin conexión a 390 px](sin-conexion-390.png).

`dist/manifest.webmanifest` existe. `dist/sw.js` **no contiene `openstreetmap`**;
sí contiene la expresión de exclusión `/^\/api\//`. Esa referencia impide que el
fallback de navegación trate las rutas de API como páginas. No es una regla de caché.
`runtimeCaching` permanece vacío.

## Límites y pendientes

- **Android físico:** no probado. El resultado local de instalabilidad no sustituye
  instalar desde Chrome Android y abrir desde el icono después de desplegar por HTTPS.
- **Lighthouse:** no se ejecutó una auditoría Lighthouse. Su categoría PWA fue retirada;
  no es posible obtener el antiguo resultado «instalable» en Lighthouse actual.
  Se usaron las comprobaciones nativas de Chrome. [Confirmación oficial del equipo](https://github.com/GoogleChrome/lighthouse/issues/16106).
- Una primera visita sin red y sin el worker instalado puede mostrar el error del navegador.
  El aviso requiere haber cargado la app con internet y completado la activación.
- `navigator.onLine` indica conectividad según el navegador; no demuestra que la API responda.
  Los fallos del servidor siguen usando los mensajes y reintentos existentes.
- Una **actualización automática** puede perder fotos o campos solo en memoria al recargar.
  Se conserva `autoUpdate`, como se solicitó. El reintento de conexión ya no produce esa pérdida.
  [Comportamiento documentado del plugin](https://vite-pwa-org.netlify.app/guide/auto-update.html).
- Logo definitivo: Amelia. Color del ciudadano: confirmar con Design System. Se conservan
  el logo actual y los colores solicitados. Katherine debe revisar el aviso compartido.
- No se ejecutaron pruebas de backend: esta historia solo cambia frontend.
- Linear, Figma, Google Doc y vault no se modificaron; la historia no se marca Done.

## Verificación manual después de desplegar

1. En Chrome Android abrir la URL HTTPS y usar «Instalar aplicación» del menú.
2. Abrir desde el icono: comprobar nombre, logo y ventana sin barra del navegador.
3. Tras cargar con red, activar modo avión, cerrar/abrir y comprobar «Sin conexión».
4. Reconectar: comprobar que vuelve la pantalla y que un formulario abierto conserva sus campos.
5. Con otra versión desplegada, abrir/reabrir la app y verificar que actualiza.
6. En Chrome de escritorio, Application → Manifest / Service Workers / Cache Storage:
   comprobar worker activo y ausencia de URLs de API o teselas.

No se cierran decisiones visuales ni se confirma instalación móvil hasta completar estos pasos.
