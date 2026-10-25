# ADR-0001 — Comunas, no cuadrantes

**Estado:** Aceptada (05/10/2026).

## Contexto

El Documento de Alcance hablaba de agrupar los reportes por «cuadrantes». Existían dos
ideas: crear 10–12 cuadrantes propios, o usar los 18 cuadrantes de la Policía. No había
cartografía oficial vectorial disponible para descarga, y se esperaba respuesta de la
Alcaldía.

## Decisión

NotiUrba usa las **10 comunas urbanas** de Tuluá como unidad territorial del MVP. El
docente lo aprobó el 05/10/2026 y el Oficio S-44194 respalda la organización por
comunas. No se asume equivalencia entre comunas y los cuadrantes de la Policía.

## Consecuencias

- Cada reporte se clasifica por el polígono de comuna que contiene sus coordenadas (RN-01, RN-02, RF-08).
- Se descarta la sectorización por cuadrantes de la Policía (queda fuera del MVP).
- Las menciones a «cuadrante» en documentos viejos se leen como «comuna».
- Las explicaciones históricas sobre los cuadrantes de la Policía se conservan como
  contexto, no como estado actual.
