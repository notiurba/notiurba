# Validación — Correcciones de cierre QA y despliegue

> Evidencia de la corrección sin datos personales ni secretos. Las cuentas se nombran `Cuenta QA-A`,
> `Cuenta QA-B` y `Administrador QA`. Los resultados de producción se completan solo después de
> integrar y confirmar el commit desplegado.

| Campo | Valor |
| --- | --- |
| Fecha y hora (America/Bogota) | 2026-11-17 08:48 -05 |
| Entorno local | Rama `codex/correcciones-qa-despliegue`; commit pendiente |
| Netlify | Rama y commit pendientes |
| Render | Rama y commit pendientes |
| Responsable | Andrés / Codex; revisión y correcciones T-17 a T-19: Claude |

## Matriz de criterios

| Criterio | Entorno | Resultado | Evidencia segura | Dato creado / limpieza |
| --- | --- | --- | --- | --- |
| CA-01 — Error real en el mapa | local | en verde | servicio/componente y Chrome: error seguro, cero fixture | no aplica |
| CA-02 — Reintento | local | en verde | Chrome: API caída → API sintética disponible → un reporte y un marcador | no aplica |
| CA-03 — Producción sin datos ficticios | local / Netlify | local en verde; despliegue pendiente | `dist` sin códigos ni descripciones de `reportesPrueba` | no aplica |
| CA-04 — Botón móvil accesible | producción observada / local corregido, 360 px | fallback en verde | producción confirmó solapamiento; reserva de 64 px dejó pasos 1, 2 y 3 enfocables, pulsables y sin solapamiento | no aplica |
| CA-05 — Recuperación tradicional | local / producción controlada | local en verde; producción pendiente | proveedor sintético aceptado; recepción real pendiente | token local desechable eliminado |
| CA-06 — Enlace seguro | local / producción controlada | local en verde; producción pendiente | solo queda activo el token nuevo cuando el proveedor acepta; uso real pendiente | contraseña QA sin modificar |
| CA-07 — Privacidad de la solicitud | local / producción controlada | local en verde; producción pendiente | respuesta pública genérica para cuenta no elegible | no aplica |
| CA-08 — Fallo del proveedor | local | en verde | fallo sintético elimina el token nuevo, conserva el anterior y no filtra datos en logs | base desechable eliminada |
| CA-09 — Versión y configuración | Netlify / Render | pendiente | ramas, commits y nombres de variables | no aplica |
| CA-10 — Reporte y fotografías | local / producción controlada | API local en verde; producción pendiente | suites y script QA; código/foto reales pendientes | ninguna creación en producción |
| CA-11 — Correos al buzón QA | local / producción controlada | encolado local en verde; entrega pendiente | `qa-correo.sh` con envío externo apagado | ninguna entrega externa |
| CA-12 — Aviso administrativo | local / producción controlada | panel local en verde; producción pendiente | `qa-panel.sh` y suite backend | `NU-000010` sin modificar |
| CA-13 — Resolución oficial | local / producción controlada | regresión local en verde; cambio pendiente | suite backend | ningún estado productivo modificado |
| CA-14 — Eliminación controlada | local / producción controlada | regresión local en verde; operación pendiente | suite backend | ninguna eliminación productiva |
| CA-15 — Evidencia sin repetición | todos | local documentado; cierre productivo pendiente | esta matriz | no se repiten campaña móvil ni 35 solicitudes |

## Pruebas ejecutadas

| Fecha | Comando / comprobación | Ejecutadas | Fallidas | Omitidas | Resultado |
| --- | --- | ---: | ---: | ---: | --- |
| 2026-11-16 | Pruebas dirigidas del frontend y recuperación | dirigidas | 0 | 0 | en verde tras demostrar y corregir los defectos |
| 2026-11-17 | `pnpm test` | 238 | 0 | 0 | en verde después de la auditoría; incluye reintento interactivo del mapa |
| 2026-11-17 | `pnpm lint` | 1 comando | 0 | 0 | en verde |
| 2026-11-17 | `pnpm build` | 260 módulos | 0 | 0 | en verde; inspección de fixtures sin hallazgos |
| 2026-11-17 | `mvn clean test` con PostGIS 16 desechable | 447 | 0 | 2 | en verde después de la auditoría; se omitieron solo llamadas reales a Resend y Cloudinary por ausencia deliberada de credenciales |
| 2026-11-17 | `MigrationIntegrationTest` con `env -i` y PostGIS 18 | 2 | 0 | 0 | en verde; 22 migraciones, Hibernate `validate` y perfil de solo lectura |
| 2026-11-17 | `tools/qa-correo-reporte/qa-correo.sh` | 40 verificaciones | 0 | 0 | en verde; envío externo apagado y 0 líneas `ERROR` |
| 2026-11-17 | `tools/qa-panel-admin/qa-panel.sh` | 44 verificaciones | 0 | 0 | en verde; 0 líneas `ERROR` |
| 2026-11-17 | Chrome: mapa escritorio y 360 px | error + reintento + éxito | 0 | 0 | un resultado de API, un marcador, cero fixture y sin desbordamiento horizontal |
| 2026-11-17 | Chrome: distintivo Netlify a 360 px, pasos 1 a 3 | 3 pasos | 0 | 0 | fallback local sin solapamiento; foco y clic avanzan en los tres pasos |
| 2026-11-17 | Revisión `code-reviewer` (T-17) | 8 hallazgos | — | — | «Corregir antes de cerrar»; los 8 se corrigieron en T-18 (ver `docs/sdd/tasks.md`) |
| 2026-11-17 | `pnpm test` tras las correcciones | 239 | 0 | 0 | en verde; `CargaReportesMapa.test.mjs` usa el contenedor real del mapa |
| 2026-11-17 | `pnpm lint` y `pnpm build` tras las correcciones | 261 módulos | 0 | 0 | en verde; 0 de 25 huellas de `reportesPrueba` en `dist` |
| 2026-11-17 | Segunda revisión `code-reviewer` | 6 hallazgos | — | — | los 8 originales resueltos; nuevo: la recuperación compartía la cola del correo de verificación. Corregido con una cola propia |
| 2026-11-17 | `mvn clean test` final con PostGIS 16 desechable, `DB_*` y `NOTIURBA_TEST_DB_*` | 477 | 0 | 2 | en verde; omitidas solo las llamadas reales a Resend y Cloudinary |
| 2026-11-17 | `MigrationIntegrationTest` con `env -i` y perfil `migration-tests` | 2 | 0 | 0 | en verde; 22 migraciones validadas y aplicadas |
| 2026-11-17 | Mutaciones manuales | 2 | 0 | 0 | sin `@Transactional` en `confirmar` y sin la rama de error del contenedor, las pruebas fallan |
| 2026-11-17 | Navegador integrado, simulación, mapa admin en escritorio | 30 marcadores, 10 comunas | 0 | 0 | sin errores de consola, un solo `<main>`, sin datos del fixture |

## Omisiones y riesgos residuales

- Campaña móvil de Amelia: no se repite; ya tiene evidencia aceptada.
- Importación temporal de 35 solicitudes en Postman: no se repite; ya tiene evidencia aceptada.
- Las dos pruebas de proveedor real (Resend y Cloudinary) se omitieron deliberadamente en la suite local;
  no se suministraron credenciales ni se hicieron llamadas externas. Se cubren después de la puerta de despliegue.
- Un primer intento de la suite backend no contó como evidencia porque el contenedor PostGIS no estaba activo;
  se repitió desde cero. Tras la auditoría de Codex fueron 447 ejecutadas, 0 fallidas y 2 omitidas. Tras las
  correcciones de T-18, y con `NOTIURBA_TEST_DB_*` para que corrieran también las de enrutamiento y administración,
  el resultado final se registra en la tabla de pruebas (omitidas solo las 2 de proveedores reales).
- Operaciones externas y destructivas: pendientes de sus puertas y confirmaciones en `docs/sdd/tasks.md`.
- Una corrida completa del backend falló una vez en una prueba nueva (`unPendienteRecienteNoSeBorraYOcupaCupo`):
  creaba filas con `now()` de la base y comparaba con la hora de Java, y el reloj de la VM de Docker puede desfasarse.
  Se corrigió para usar una sola hora (también en la prueba hermana) y la suite completa posterior pasó.
- Los scripts `qa-panel.sh` y `qa-correo.sh` no se repitieron tras las correcciones: no tocan recuperación ni el
  contenedor del mapa, y sus 44 y 40 verificaciones previas siguen vigentes.

## Limpieza

- PostGIS de la suite: contenedor temporal detenido y eliminado.
- Corridas de T-18 y T-19: contenedor propio `notiurba-claude-pg` (127.0.0.1:5441, credenciales aleatorias fuera
  del repo) eliminado al terminar. El contenedor `notiurba-cqa-postgis` de la sesión anterior sigue encendido y no se tocó.
- `MigrationIntegrationTest`: contenedor Testcontainers eliminado automáticamente.
- Scripts QA: sus dos bases temporales fueron eliminadas por la limpieza automática; `notiurba-db` volvió a su estado detenido anterior.
- Reportes, fotos y cuentas QA de producción: pendiente de registrar códigos y resultado sin correos ni IDs internos.
