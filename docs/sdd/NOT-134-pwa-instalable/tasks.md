# Tareas y evidencia — NOT-134

Estado: implementación y verificación local listas. Sin commit, push ni despliegue.

- [x] Revisar diff y contexto: rama `feature/not-134-pwa-instalable`, base `50b583d`.
- [x] Ejecutar `pnpm install`, `pnpm lint`, `pnpm test`, `pnpm build` iniciales.
  Pasan; el ejecutor informa 37 archivos de pruebas aprobados, 0 fallidos.
- [x] Revisión inicial `code-reviewer`: tres defectos (recarga de actualización ausente,
  foco detrás del aviso, reintento que pierde campos/fotos).
- [x] Reproducir los defectos del aviso: 2 pruebas fallan y 1 pasa antes de corregir.
  Chrome también confirma que reintentar sin red vacía el correo escrito en login.
- [x] Corregir registro y aviso; agregar 6 regresiones. Se declara `workbox-window`
  directamente porque Vite no resolvía el módulo virtual con la dependencia indirecta.
- [x] Comprobar manifiesto, caché, modo sin conexión y actualización en Chrome:
  8 verificaciones aprobadas, 0 errores de instalabilidad y 0 errores JS en la página observada.
- [x] Ejecutar comprobaciones finales: 245 pruebas aprobadas, 0 fallidas, 0 omitidas;
  39 archivos; lint y build en verde. Segunda revisión del subagente sin hallazgos bloqueantes.
- [x] Documentar resultados y pendientes en [la evidencia](../../validacion/not-134/README.md).
  Commit sugerido: `feat(frontend): habilitar PWA instalable de NotiUrba (NOT-134)`.

## Cierre externo pendiente

- [ ] Instalar y abrir desde el icono en Chrome Android con HTTPS.
- [ ] Validar icono con Amelia y color con el Design System.
- [ ] Revisión visual de Katherine del aviso compartido.
- [ ] Commit/PR/integración/despliegue: los decide y ejecuta Andrés cuando corresponda.
- [ ] Actualizaciones de Linear/Figma/Doc/vault: no realizadas en esta tarea local.

No se archiva como historia cerrada ni se marca Done antes de integrar y verificar el celular.
