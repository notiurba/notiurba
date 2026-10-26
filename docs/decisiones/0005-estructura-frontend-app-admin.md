# ADR-0005 — Estructura del frontend: `app/` / `admin/` / `compartido/`

**Estado:** Aceptada (25/10/2026).

## Contexto

El frontend tenía un solo prototipo sin router (`App.jsx` → `MapaTulua`) con mapa,
filtros, mapa de calor y diagnóstico de límites — trabajo de Katherine para probar el
mapa (NOT-9). Funcionalmente eso es contenido de administración (todos los reportes,
gestión visual, una herramienta interna de diagnóstico), no el flujo del ciudadano
(crear reporte con foto + GPS, consultar "mis reportes"), que todavía no existe.

Había que decidir dónde vive ese trabajo al introducir la separación por rol
(`/app/*` ciudadano, `/admin/*` administrador) sin perderlo ni duplicar la capa
geográfica que el flujo ciudadano también va a necesitar.

## Decisión

- `frontend/src/admin/`: contenido real de `/admin`, no un marcador de posición. El
  prototipo completo de Katherine (mapa, filtros, mapa de calor, diagnóstico de
  límites) migró aquí tal cual, solo con imports corregidos.
- `frontend/src/app/`: vacío por ahora, con una sola ruta marcador de posición. La PWA
  ciudadana (crear reporte, mis reportes) se construye en el Sprint 2.
- `frontend/src/compartido/`: lo reusable por ambos roles — capa GeoJSON de comunas,
  `geoUtils.js` (punto-en-polígono), estilos base, assets de marca, y el andamiaje de
  sesión (`cliente.js`, `sesion.js`, `RutaProtegida.jsx`) para cuando exista el login.
- `react-router-dom` se agregó como dependencia nueva para soportar esta separación de
  rutas.
- `RutaProtegida` existe pero **no está conectada todavía** a `/app` ni `/admin`: no
  hay página de `/login` ni endpoint de backend probado de punta a punta. Conectarla es
  el siguiente paso, no una decisión pendiente de diseño.

## Consecuencias

- El trabajo de Katherine no se perdió ni se reescribió: se movió con `git mv`
  (historial conservado) y solo cambiaron las rutas de import.
- `admin/` deja de estar "vacío" desde ya — si el Sprint 3 agrega más vistas de
  administración, se suman junto al mapa, no lo reemplazan.
- El panel "Diagnóstico de límites" sigue siendo una herramienta interna (ver
  `08-pendientes.md`); vivir dentro de `admin/` es consistente con ocultarla en
  producción más adelante.
- Los archivos pesados (`plano-p28-tulua-2015-300dpi.png`, `.pdf`) no se tocaron: ya
  están en el historial de git y sacarlos de `src/` no libera espacio sin reescribir
  historia, que es una decisión distinta y más delicada (ver deuda en
  `08-pendientes.md`).
