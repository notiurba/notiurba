# Comunas y territorio

## Decisión

NotiUrba usa las **10 comunas urbanas** de Tuluá como unidad territorial del MVP.
Reemplaza la idea inicial de crear cuadrantes propios y la sectorización por los 18
cuadrantes de la Policía. Aprobado por el docente el 05/10/2026 (Oficio S-44194).
Ver ADR-0001.

## Flujo territorial

    reporte → coordenadas GPS → polígono GeoJSON → comuna

Un punto dentro de un solo polígono se asigna a esa comuna. Un punto en un borde o en
dos polígonos queda «pendiente de revisión» (sin comuna arbitraria). Un punto fuera de
la cobertura queda «sin determinar» (no implica estar fuera de Tuluá).

## Capa GeoJSON

- La capa se almacena en GeoJSON, un polígono por comuna, con ID (`01`–`10`) y nombre
  únicos, en EPSG:4326.
- La Alcaldía informó que **no** entregará la cartografía digitalizada del POT en
  formato vectorial editable.

### Estado real (según el prototipo del frontend, 2026-10-11)

> Importante: la pestaña del Doc dice que el equipo «digitalizará» los límites desde
> un PDF. En la práctica, el prototipo **no** digitalizó: convirtió la capa oficial del
> POT 2015 (SHP) a GeoJSON. La documentación formal debe decir lo que realmente se hizo.

- Fuente: Colombia OT, registro 30349, capa `01_P_Comunas_urbanas_2014`, atribuida a
  la Alcaldía de Tuluá, instrumento POT 2015.
- Conversión EPSG:3115 (MAGNA Colombia Oeste) → EPSG:4326 con PyProj. No se digitalizó
  el PDF ni se modificaron vértices.
- Resultado: 10 polígonos válidos con códigos `01`–`10`.
- Marcada como **provisional, pendiente de validación administrativa**: no confirma la
  división vigente en 2026.

### Calidad de la capa (diagnóstico del prototipo)

- 27 huecos interiores (~25 ha en total) y 23 solapamientos. Solo 16 superan 0,01 m²;
  el resto son micro-superficies numéricas sin efecto práctico.
- El hueco mayor (H001) sigue el corredor del río Tuluá: posible exclusión
  hidrográfica, no un error. Un reporte que caiga ahí queda sin comuna.
- **Falta definir la regla** para huecos y solapamientos (asignar la comuna más cercana,
  enviar a revisión del administrador, etc.). Hoy no está en las RN.

## Pendientes

- Recibir y validar el GeoJSON de las 10 comunas (ya solicitado al compañero).
- Validarlo: 10 polígonos, ID y nombre únicos, sin huecos ni solapes grandes, EPSG:4326.
- Probar la consulta punto-en-polígono en PostGIS con coordenadas reales de cada comuna.
- Validar límites y vigencia con Planeación Municipal (DAPM). Ver ADR-0003.
- Documentar la fuente y el método real (conversión del SHP, no digitalización).

Fuente: pestaña «Comunas y cuadrantes de Tuluá» del Google Doc y `frontend/src/docs/`
del repo.
