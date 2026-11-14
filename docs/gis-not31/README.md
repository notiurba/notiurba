# Comunas de Tuluá — capa experimental NOT-31

Esta carpeta contiene el mapa de las **10 comunas de Tuluá** preparado para las pruebas académicas de NotiUrba. **No es cartografía oficial** y no debe usarse para asignar reportes ciudadanos reales.

## Archivos principales

- `comunas-tulua-NOT31-prototipo-experimental.geojson`: mapa de las diez comunas.
- `perimetro-experimental.geojson`: área de referencia para comprobar la cobertura.
- `pruebas/`: coordenadas y resultados de las verificaciones geométricas y de la carga a PostGIS.

Las coordenadas están en **EPSG:4326**. SHA256 del GeoJSON de comunas: `36f037dcc945184c1e918166d7f9ee06095d592e20537ccdf68c350e2e7489da`.

## Resultados de las pruebas

- **10/10** comunas con geometrías válidas.
- **0 huecos y 0 solapamientos** dentro del perímetro experimental.
- **74/74 puntos de prueba geométricos** con resultados esperados. No son mediciones comprobadas en campo.
- La importación se verificó dos veces en una **base PostGIS aislada**, sin duplicados. Se confirmó que los datos quedaron guardados y que las otras 23 tablas no cambiaron.

Para ver el detalle: `pruebas/resultado_validacion_prototipo.json` y `pruebas/resultado_postgis_publico.json`. Los CSV contienen las coordenadas empleadas y su clasificación en el mapa experimental.

## Límites del trabajo entregado

La capa es sintética y no tiene validación administrativa oficial. No se instaló en producción. Esta entrega no activa migraciones Flyway ni cambia las reglas de asignación de NOT-21 o el backend.

Los scripts de construcción, las consultas de importación, la propuesta de migración V13 y el informe técnico completo se conservaron en la copia de trabajo original y el historial del [commit inicial 511838a](https://github.com/notiurba/notiurba/commit/511838ad528908be8d630981c91ff8c5978ea167). Esta carpeta **no es un paquete autónomo** para regenerar el mapa o volver a importar los datos: necesita esos archivos técnicos y el entorno autorizado.

**Integración:** PR #15 hacia `develop`, pendiente de revisión del equipo.
