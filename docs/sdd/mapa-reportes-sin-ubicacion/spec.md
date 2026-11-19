# Mapa del panel: reportes sin ubicación

> **Fase 1 de 5 del flujo SDD.** Define **qué** se corrige. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.
> Este expediente es independiente: no toca `docs/sdd/spec.md`, `plan.md` ni `tasks.md` (cierre QA).

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (Borrador → En revisión → Aprobada) |
| Historia / ticket | Sin historia de Linear. Defecto hallado en producción el 19/11/2026; relacionado con NOT-135 y NOT-140 |
| Responsable | Andrés; revisión de Katherine (pantalla del panel) |
| Aprobada por / fecha | Andrés, 2026-11-19 |
| Última actualización | 2026-11-19 |

## 1. Contexto y problema

La pestaña «Mapa de calor» del panel (`/admin/mapa`) queda **en blanco** en producción.

Evidencia (19/11/2026, `https://notiurba.netlify.app`, sesión de administrador):

- Consola: `TypeError: Cannot read properties of null (reading 'lat')`, con la pila `onAdd → update → latLngToLayerPoint` de Leaflet. Es un marcador con la posición en `null`.
- `GET /api/admin/reportes` devuelve 5 reportes y **4 no tienen ubicación** (latitud y longitud nulas): `NU-000010`, `NU-000008`, `NU-000005` y `NU-000004`. Solo `NU-000009` la tiene.
- La ubicación es opcional según la categoría (`docs/contexto/10-modelo-de-datos.md`), así que esto va a seguir pasando con datos reales.
- `MapaComunas.jsx` dibuja un marcador por cada reporte sin comprobar las coordenadas, y el panel no tiene ningún límite de errores. Un solo reporte sin ubicación tumba toda la pantalla.

## 2. Objetivo

El mapa del panel se muestra siempre: dibuja solo los reportes con ubicación, dice cuántos quedaron fuera y, si algo más falla al dibujar, muestra un aviso con «Reintentar» en vez de una pantalla en blanco.

## 3. Usuarios y historias

- Como **administrador**, quiero abrir el mapa aunque haya reportes sin ubicación, para ver los que sí la tienen.
- Como **administrador**, quiero saber cuántos reportes no aparecen en el mapa, para no creer que faltan datos.
- Como **administrador**, quiero un aviso claro si el mapa falla, para poder reintentar o avisar al equipo.

## 4. Requisitos

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RF-MSU-01 | Dibujar en el mapa y en el mapa de calor solo los reportes con latitud y longitud válidas | Esencial | RF-22, RF-23 |
| RF-MSU-02 | Informar cuántos reportes del listado no se dibujan por no tener ubicación | Esencial | RF-22 |
| RF-MSU-03 | Mostrar un aviso con «Reintentar» si falla el dibujo de cualquier pantalla del panel, sin dejarla en blanco | Esencial | RNF de usabilidad |

## 5. Criterios de aceptación

- **CA-01** — _Dado_ un listado con reportes con y sin ubicación, _cuando_ el administrador abre «Mapa de calor», _entonces_ el mapa se muestra y dibuja un marcador solo por cada reporte con ubicación.
- **CA-02** — _Dado_ el mismo listado, _cuando_ se activa la casilla «Mapa de calor», _entonces_ la capa de calor usa solo los reportes con ubicación.
- **CA-03** — _Dado_ N reportes sin ubicación (N > 0), _entonces_ el mapa muestra «N reportes no tienen ubicación y no aparecen en el mapa» (singular con N = 1). Con N = 0 no muestra el aviso.
- **CA-04** — _Dado_ que ningún reporte tiene ubicación, _entonces_ el mapa se muestra sin marcadores, con 0 reportes visibles y el aviso de CA-03.
- **CA-05** — Caso de error: _dado_ un reporte con coordenadas no numéricas (`null`, `undefined`, `NaN` o texto), _entonces_ se trata como «sin ubicación» y no rompe el mapa.
- **CA-06** — Caso de error: _dado_ que un componente de una pantalla del panel lanza un error al dibujarse, _entonces_ el marco del panel (cabecera y pestañas) sigue visible y el contenido muestra «No pudimos mostrar esta pantalla» con «Reintentar».
- **CA-07** — _Dado_ el aviso de CA-06, _cuando_ el administrador cambia de pestaña, _entonces_ la nueva pestaña se muestra normal (el error no se queda pegado).
- **CA-08** — _Dado_ un listado completo con ubicación, _entonces_ el mapa se comporta igual que hoy y no aparece ningún aviso nuevo.

## 6. Reglas y restricciones que aplican

- Reglas de negocio: la ubicación es opcional según la categoría; este cambio no la vuelve obligatoria.
- Ley 1581 de 2012: no se agregan datos personales; el aviso solo muestra un número.
- Interfaz en español, frases cortas. Textos en `compartido/textos/` si ya existe ese archivo para el panel.
- Cambio solo de frontend. No hay migración, ruta nueva ni cambio de contrato.
- Toca una pantalla del panel: **se señala para revisión de Katherine** en el PR.

## 7. Fuera de alcance

- Cambiar el mapa para usar `GET /api/admin/mapa` y `/api/admin/mapa/calor` (NOT-135 y NOT-136).
- Pantalla de estadísticas y puntos críticos.
- Ubicar los reportes sin GPS usando el barrio o la comuna.
- Límite de errores en la app del ciudadano (solo se cubre el panel).
- Cambios en el backend o en los datos de producción.
- Crear reportes de prueba con ubicación para la presentación (decisión de Andrés; toca producción).

## 8. Preguntas abiertas

- [PENDIENTE] Texto exacto del aviso de reportes sin ubicación y del error — decide: Andrés, con Katherine.
- [PENDIENTE] ¿Se crea una historia en Linear para este arreglo o se cuenta dentro de NOT-135/NOT-140? — decide: Andrés (no se crea sin su visto bueno).

## 9. Aprobación

- [x] La especificación está completa y sin ambigüedades.
- [x] Las preguntas abiertas están resueltas o aceptadas como riesgo (los textos y la historia de Linear siguen **[PENDIENTE]** y no bloquean).
- [x] **Aprobada por el usuario** → se puede pasar a `plan.md`.
