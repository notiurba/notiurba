# Plan técnico — Mapa del panel: reportes sin ubicación

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se construye lo que dice `spec.md`.
> Solo se redacta con la especificación **Aprobada**. No se escribe código hasta que
> este plan esté **Aprobado**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado** (Borrador → En revisión → Aprobado) |
| Especificación | [spec.md](spec.md) |
| Aprobado por / fecha | Andrés, 2026-11-19 |
| Última actualización | 2026-11-19 |

## 1. Enfoque general

Dos cambios pequeños, solo en el frontend del panel:

1. **Separar** los reportes con y sin ubicación con una función pura, y que el mapa dibuje solo los primeros. Un aviso dice cuántos quedaron fuera.
2. **Poner un límite de errores** alrededor del contenido del panel, para que un fallo de dibujo muestre un aviso con «Reintentar» y no una pantalla en blanco.

La lógica va en funciones y componentes pequeños que se prueban sin Leaflet. `MapaComunas.jsx` importa Leaflet y no se puede cargar en `node --test`; por eso solo cambia en lo mínimo (usa la función y pinta el aviso).

## 2. Stack y límites (fijados, no se cambian)

React 19 + Vite 8 + pnpm · Leaflet 1.9 + Leaflet.heat. No se agrega ninguna dependencia. Un límite de errores en React 19 sigue necesitando un componente de clase (`getDerivedStateFromError`); no hace falta una librería.

## 3. Cambios por capa

### Base de datos
- Ninguno.

### Backend
- Ninguno. `GET /api/admin/reportes` sigue devolviendo todos los reportes (los sin ubicación llegan con `latitud` y `longitud` nulas).

### API
- Sin cambios en el contrato.

### Frontend (`frontend/src/…`)
- `admin/servicios/reportesService.js`: nueva función exportada `separarPorUbicacion(reportes)` → `{ conUbicacion, sinUbicacion }`. Un reporte tiene ubicación si `Number.isFinite(latitud)` y `Number.isFinite(longitud)` son verdaderos (`null`, `undefined`, `NaN` y texto no cuentan). `obtenerReportesCiudadanos` no cambia.
- `admin/componentes/AvisoSinUbicacion.jsx`: recibe `cantidad`; con 0 no pinta nada; con 1 dice «1 reporte no tiene ubicación y no aparece en el mapa»; con más, el plural. Usa el componente de avisos que ya existe en `compartido/componentes/avisos/`.
- `admin/paginas/MapaComunas.jsx` (`VistaMapa`): al principio separa `reportes` con `useMemo`; todo lo demás (filtros, conteos, marcadores y capa de calor) usa `conUbicacion`; pinta `AvisoSinUbicacion` en el resumen del mapa. Sin cambios de estilo salvo lo que pida el aviso.
- `compartido/componentes/LimiteErrores.jsx`: componente de clase con `getDerivedStateFromError`. Ante un error muestra `EstadoVacio` (tono alerta) con el título «No pudimos mostrar esta pantalla», una frase corta (con «recargue la página» si sigue igual) y el botón «Reintentar», que limpia el estado. No agrega `componentDidCatch`: React 19 ya registra con `console.error` los errores que atrapa un límite, y agregarlo duplicaría el registro. Límite conocido: si falla la carga diferida de una pantalla (`lazy`, por ejemplo tras un despliegue nuevo), «Reintentar» no la vuelve a pedir; ahí solo sirve recargar la página.
- `admin/componentes/LayoutAdmin.jsx`: envuelve `<Outlet />` con `<LimiteErrores key={location.pathname}>`. La cabecera y las pestañas quedan fuera del límite; el `key` reinicia el límite al cambiar de pestaña (CA-07).

### Integraciones y configuración
- Sin variables nuevas. Resend y Cloudinary no se ven afectados (`RESEND_PRUEBA_DESTINO` sigue activa).

## 4. Estrategia de pruebas

Primero se escribe cada prueba y se comprueba que **falla** sin el cambio.

| Criterio | Tipo de prueba | Dónde |
| --- | --- | --- |
| CA-01, CA-04, CA-05, CA-08 | Unitaria de `separarPorUbicacion`: mezcla, todos sin ubicación, `null`, `undefined`, `NaN`, texto, `0` válido, y no muta la entrada | `admin/servicios/reportesService.test.mjs` |
| CA-03 | Componente `AvisoSinUbicacion`: 0, 1 y N | `admin/componentes/AvisoSinUbicacion.test.mjs` |
| CA-06 | Componente `LimiteErrores`: un hijo que lanza error muestra el aviso y «Reintentar»; con un hijo sano no cambia nada | `compartido/componentes/LimiteErrores.test.mjs` |
| CA-06, CA-07 | `LimiteErrores` con `key` distinta se reinicia; «Reintentar» vuelve a dibujar | mismo archivo |
| CA-01, CA-03, CA-04, CA-08 | Componente `MapaComunas` con Leaflet sustituido por stubs (el marcador falla con posición nula, como el real): mezcla, ninguno con ubicación y todos con ubicación | `admin/paginas/MapaComunas.test.mjs` y `admin/paginas/soporte-pruebas/` |
| CA-01, CA-02, CA-03 | **Manual** en producción tras el despliegue: con la sesión de administrador, `/admin/mapa` se ve, muestra el mapa con 1 marcador (`NU-000009`) y el aviso «4 reportes no tienen ubicación…» | Lista en `tasks.md` |

Se usa el mismo método de las pruebas actuales (`node --test`, `react-test-renderer` y `vite` en modo servidor, como `CargaReportesMapa.test.mjs`). Las pruebas existentes (frontend: 230 en `docs/contexto/12`) deben seguir pasando. Se corre `pnpm test && pnpm lint && pnpm build`.

## 5. Riesgos y mitigaciones

| Riesgo | Probabilidad / impacto | Mitigación |
| --- | --- | --- |
| `MapaComunas.jsx` no se prueba de forma automática (importa Leaflet) | Media / media | Toda la lógica queda en funciones y componentes probados; el archivo solo cambia en 3 puntos; verificación manual en producción |
| El límite de errores esconde errores reales | Baja / media | Se registra con `console.error` y el aviso lo dice con claridad; no se traga el error |
| El mapa de la presentación queda con 1 solo punto | Alta / media | Es el estado real de los datos. Crear reportes con ubicación es decisión de Andrés (fuera de alcance) |
| Conflicto con NOT-135 (que rehará este mapa con `/api/admin/mapa`) | Media / baja | El cambio es pequeño y ese endpoint ya trae solo reportes con ubicación; NOT-135 puede retirar `separarPorUbicacion` |
| Toca la pantalla de Katherine | — | Se señala en el PR y en Linear para su revisión |

## 6. Alternativas descartadas

- **Filtrar en el backend** (cambiar `GET /api/admin/reportes`): el listado de «Reportes» necesita los reportes sin ubicación.
- **Usar `GET /api/admin/mapa` ya**: es NOT-135, mucho más grande; no cabe antes del lunes.
- **Un límite de errores en toda la app**: más cobertura, pero toca la app del ciudadano; se deja para otra historia.
- **Instalar `react-error-boundary`**: una dependencia nueva para 30 líneas.
- **Dibujar los sin ubicación en el centro de su comuna o barrio**: inventa datos; la ubicación real no existe.

## 7. Decisiones pendientes

- [PENDIENTE] Textos exactos del aviso y del error — decide: Andrés, con Katherine.
- [PENDIENTE] ¿Historia nueva en Linear o dentro de NOT-135/NOT-140? — decide: Andrés.
- [PENDIENTE] ¿Extender `LimiteErrores` a la app del ciudadano en otra historia? — decide: Andrés.

## 8. Aprobación

- [x] Cada criterio de `spec.md` tiene un cambio técnico y una prueba.
- [x] No introduce otro framework ni otra base de datos.
- [x] Contrato de API y migración revisados (no hay).
- [x] **Aprobado por el usuario** → se puede pasar a `tasks.md`.
