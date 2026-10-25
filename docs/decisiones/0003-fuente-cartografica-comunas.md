# ADR-0003 — Fuente cartográfica de las comunas

**Estado:** Aceptada como fuente **provisional**; pendiente de validación administrativa.

## Contexto

Se necesita una capa vectorial de las 10 comunas para asignar cada reporte. La Alcaldía
informó que no entregará la cartografía digitalizada del POT en formato vectorial
editable. El Acta previó «digitalizar» los límites desde un PDF.

## Decisión

El prototipo **no digitalizó** el PDF. Convirtió la capa oficial del **POT 2015** (SHP,
`01_P_Comunas_urbanas_2014`, Colombia OT registro 30349) de EPSG:3115 a EPSG:4326 con
PyProj, sin alterar vértices. Resultado: 10 polígonos con códigos `01`–`10`, guardados
en `comunas-tulua-POT2015-candidato.geojson`. Se marca como provisional.

## Consecuencias

- La documentación formal debe decir que se **convirtió** el SHP oficial, no que se
  digitalizó un PDF (corrige la pestaña «Comunas»).
- La capa es provisional: no confirma la división vigente en 2026. Hay que validarla con
  Planeación Municipal (DAPM).
- La capa tiene huecos y solapamientos (ver `06-comunas-y-territorio.md`); falta una
  regla para los puntos que caen en ellos.
- Alternativa documentada: capa DANE 2018, pero solo tiene 9 comunas (le falta la 10).
