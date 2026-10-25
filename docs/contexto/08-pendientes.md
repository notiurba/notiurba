# Pendientes

Esta lista refleja la pestaña «Pendientes» del Google Doc. Prioridad de arriba hacia abajo.

## Entregables del Sprint 1

- **Requisitos y reglas de negocio**: existen (ver 04 y 05). Falta confirmar los tiempos
  de RNF-04, la respuesta del docente sobre datos personales (Ley 1581) y validar las
  categorías 8 y 9.
- **Diseño técnico (arquitectura y modelo de datos)**: parcial. Falta todo lo listado en
  `03-arquitectura-y-datos.md` (diagramas de arquitectura y despliegue, modelo de datos
  completo, ER, API REST, flujo de estados, notificaciones). **Responsable: Andrés.**
- **Prototipo navegable en Figma**: existe, falta el enlace en el documento y una pestaña
  que lo describa. **Responsable: Amelia.**
- **Delimitación de las 10 comunas**: base real lista (GeoJSON del POT 2015 convertido);
  falta validarlo con Planeación y aclarar que se convirtió, no se digitalizó.

## Decisiones abiertas

- Mecanismo de envío del correo: cuenta del ciudadano vs Resend (ADR-0002). Fin del
  Sprint 2 o Sprint 3.
- Categorías 8 y 9: confirmar con la Alcaldía.
- Framework/estructura del frontend: sin definir.
- Dominio para Resend antes del 8 de noviembre (si no, solo modo borrador, RF-13).

## Verificaciones técnicas

- Prueba mínima del mapa: OSM + marcadores + GeoJSON de comunas + Leaflet.heat (NOT-9).
- Envío de Resend por el puerto 2587 (o 2465) desde Render (Render Free bloquea 25/465/587).
- PostGIS en Neon en el plan Free (`CREATE EXTENSION postgis`).

## Reglas de negocio por definir

- Qué hace el sistema con un punto que cae en un **hueco** o **solapamiento** de la capa
  de comunas (asignar la más cercana, enviar a revisión, etc.). Hoy no está en las RN.

## Deuda del prototipo del frontend

- Sacar los archivos pesados del repo: `plano-p28-tulua-2015-300dpi.png` (~39 MB) y
  `plano-p28-tulua-2015.pdf` (~5 MB). Ya están en el historial de git; GitHub bloquea
  sobre 100 MB por archivo. Moverlos fuera de `src/` o usar Git LFS.
- Mover la asignación de comuna al backend (PostGIS) como fuente de verdad.
- El panel «Diagnóstico de límites» es una herramienta interna: ocultarlo en producción.
- Normalizar tildes en los textos de la interfaz.
