# NOT-134 — Instalar NotiUrba en el celular

Alcance solicitado por Andrés el 2026-11-17: terminar y verificar los cambios locales
existentes en `feature/not-134-pwa-instalable`, desde `50b583d`. Sin commit, push,
cambio de rama ni despliegue. Este expediente conserva intacto el SDD anterior de QA,
que todavía registra verificaciones externas pendientes.

## Criterios solicitados

1. Chrome Android permite instalar NotiUrba con nombre e icono.
2. Manifiesto con nombre, iconos PNG 192/512, colores e inicio `/`, modo `standalone`.
3. El service worker guarda recursos estáticos de la app, sin teselas de OSM ni
   respuestas de la API (RNF-02). La exclusión de `/api` puede aparecer en `sw.js`.
4. Tras una primera carga con red y la activación del service worker, abrir o recargar
   sin red muestra «Sin conexión». Desconectar y reconectar no pierde el formulario.
   La primera visita sin internet y sin recursos instalados no puede funcionar.
5. Una nueva versión detectada actualiza el service worker y la pantalla abierta,
   manteniendo la estrategia `autoUpdate` solicitada.
6. Instalación verificada con las herramientas disponibles de Chrome; Lighthouse
   actual ya no ofrece la categoría PWA. Registrar la limitación y la prueba móvil pendiente.
7. Instalación de dependencias, lint, pruebas y build en verde; revisión de `code-reviewer`.

## Decisiones pendientes

- Icono definitivo: Amelia; conservar el logo provisional.
- Color del ciudadano: confirmar con el Design System; conservar `#082f6b` y `#f5f7f4`.
- La instalación física en Android requiere validación en un celular y HTTPS.

La app requiere internet para operar; no se incorpora captura de reportes offline.
La recarga automática por una actualización puede perder datos que sigan solo en
memoria. No se introduce persistencia de fotos ni se cambia a confirmación manual.
