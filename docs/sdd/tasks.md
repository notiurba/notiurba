# Tareas — Correo formal, canales por entidad y vista previa del mensaje

> **Fase 3 de 5 del flujo SDD.** Desglose de `plan.md` en pasos pequeños y
> verificables. Se crea con el plan **Aprobado** y se actualiza durante la
> implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) (aprobada el 2026-11-12, con el atajo de la opción A) |
| Plan | [plan.md](plan.md) (aprobado el 2026-11-12) |
| Estado general | **En curso**: Figma aprobado; Grupos 1 y 2 (backend) hechos; falta el frontend |
| Última actualización | 2026-11-12 |

## Cómo se usa

- Una tarea = un cambio pequeño que se puede probar solo.
- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Cada tarea nombra el criterio de aceptación (CA) que cubre y su prueba.
- Toda la historia va en **un solo PR** desde `feature/not-130-correo-claro-y-canales` hacia `develop` (decisión de Andrés, 12 nov), con un commit atómico por tarea y sin coautoría de Claude.
- Se implementa **en orden**: Figma → backend → frontend → pruebas. Si una tarea cambia el plan, se actualiza `plan.md` primero.
- Al terminar una tarea se marca aquí con su commit y, si cambió algo de fondo, se actualiza `docs/`.
- Al tocar código: subagente `code-reviewer` al final de cada grupo y antes de abrir el PR.

## Tareas

### Grupo 0 — Documentos SDD

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-00 | Archivar NOT-129, spec y plan aprobados, endpoints PLANEADO en el contrato, estas tareas | Docs | — | revisión de Andrés | [~] |

### Figma — sin PR; puerta: visto bueno de Andrés

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | «Paso 3 · Revisar y enviar»: tarjeta «Así llegará tu mensaje», «Editar» por sección, estados cargando, error y sin datos | Figma | CA-13 | capturas a Andrés | [x] marco «4 · Confirmación · Vista previa del mensaje» |
| T-02 | Pantalla nueva «Cómo reportar a {entidad}» (atajo de gas, energía y aseo): canales con ícono, acción directa, horario e instrucciones; «Atrás» al paso 1 | Figma | CA-09, CA-09c | capturas a Andrés | [x] marco «1b · Cómo reportar a la entidad (sin correo)» |
| T-03 | Resultado del envío con rebote o fallo: lista de canales y «Copiar mi mensaje» | Figma | CA-11 | capturas a Andrés | [x] marco «¡Listo! · Correo no entregado (rebote)» |
| T-04 | **Validación de Andrés** de T-01 a T-03 (con el Design System); sin ella no se empieza el backend | Figma | — | visto bueno escrito | [x] aprobado por Andrés el 12 nov |

### Grupo 1 — Canales en la base y en la ruta

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-05 | `V21__canales_y_finalidad.sql`, estructura: catálogo `finalidad_canal` y columnas `id_finalidad_canal`, `orden`, `horario`, `fuente`, `verificado_en` en `canal_atencion` | DB | CA-18 | `CanalesMigracionIntegracionTest` (migra V20 → V21 con reportes y envíos) | [x] `4be86db` |
| T-06 | V21, datos: Centroaguas a `info@`; GDO y su WhatsApp a inactivo; Veolia con WhatsApp activo y primero; canales nuevos de la Alcaldía, GDO, Centroaguas y Veolia con finalidad, orden, horario y fuente; nada se borra | DB | CA-08, CA-10, CA-10b, CA-17 | ídem + consulta de solo lectura en Neon tras desplegar | [x] `4be86db` (falta la consulta en Neon tras desplegar) |
| T-07 | `EnrutamientoServicio`: `Ruta.canales` en una consulta (sin N+1), ordenados; campos actuales intactos | Backend | CA-09, CA-10, CA-11 | `CanalesRutaIntegracionTest` y `EnrutamientoIntegracionTest` (1 y 11 con correo; 12, 13 y 14 sin correo con canales; emergencia de gas) | [x] `c4eceea` |

### Grupo 2 — Mensaje formal y vista previa

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-08 | `GrupoPlantilla` (A, B, C) deducido de la entidad y `requiere_ubicacion` | Backend | CA-02 | `GrupoPlantillaTest` (las 14 categorías) | [x] `769753f` |
| T-09 | `PlantillaReporte` reescrita: `componer` → `Mensaje(asunto, texto, html, secciones)`; tono formal, sin NotiUrba, código ni dependencia; sin filas vacías; fecha y coordenadas; fotos con texto alternativo; matrícula condicional; escape de HTML | Backend | CA-01, CA-03 a CA-06, CA-07 (texto) | `PlantillaReporteTest` con entradas hostiles | [x] `4a4e127` |
| T-10 | `EnvioReporteServicio` con la plantilla nueva; asunto sin código; pruebas que fijaban el asunto viejo actualizadas | Backend | CA-01, CA-08 | `EnrutamientoIntegracionTest`, `DestinoDePruebaTest` | [x] `4a4e127` |
| T-11 | `POST /api/reportes/vista-previa` (`VistaPreviaServicio` y controlador): sin escribir en la base | Backend | CA-13, CA-14, CA-16 | `VistaPreviaIntegracionTest` (200, no crea reporte ni envío, 400 con `campo`, 401) | [x] `962a663`, `ca11bf9` |
| T-12 | `GET /api/reportes/{codigo}/mensaje` (solo el autor) | Backend | CA-11 | `VistaPreviaIntegracionTest` (200, 401, 404 de otro usuario) | [x] `ca11bf9` |
| T-13 | Igualdad de textos: vista previa = `GET /mensaje` = `mensaje_json` encolado | Backend | CA-15 | `VistaPreviaIntegracionTest` | [x] `ca11bf9` (sin la línea de fecha: la vista previa usa la hora de ese momento) |

### Grupo 3 — Frontend: atajo, vista previa y canales

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-14 | `enlaceCanal` con WhatsApp (`wa.me/57…`) y componente `ListaCanales` | Frontend | CA-09c | `estadoCorreo.test.mjs` | [ ] |
| T-15 | Atajo: `PasoCategoria` decide el destino con `permiteCorreo`; `PasoCanales` en `/reportar/canales`; guardia en `ReportarLayout`; la pregunta de fuga de gas y la línea 164 se conservan | Frontend | CA-09, CA-09b, CA-12 | `node --test` de la decisión y del guardia + manual | [ ] |
| T-16 | `VistaPreviaMensaje` en `PasoConfirmacion` (servicio `vistaPrevia`, «Editar» por sección, carga y error) | Frontend | CA-13, CA-14 | `node --test` del armado + manual | [ ] |
| T-17 | `ResultadoCorreo`: `ListaCanales` y «Copiar mi mensaje» con rebote o fallo (servicio `mensaje`) | Frontend | CA-11 | manual + `estadoCorreo.test.mjs` | [ ] |
| T-18 | Simulación (`VITE_USAR_MOCKS`): endpoints nuevos y `canales` en la ruta | Frontend | — | `simulacion.test.mjs` | [ ] |

### Pruebas funcionales y cierre (Fase 5)

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-19 | Script de punta a punta en `tools/` con el jar real y base desechable: crear reporte, vista previa y correo encolado | Pruebas | CA-01, CA-15 | el script en verde | [ ] |
| T-20 | Prueba funcional manual: categorías 1, 6, 11, 12, 13 y 14; 360 px y 1366 px; en el atajo, la pestaña de red sin `POST /api/reportes`; correo de prueba abierto en Gmail del celular y del computador | Pruebas | CA-07, CA-09b, todos | guion con capturas | [ ] |
| T-21 | Subagente `code-reviewer` sobre toda la rama; hallazgos corregidos con pruebas y antes/después | Pruebas | — | informe | [ ] |
| T-22 | Cierre con la skill `cierre-de-historia`: `docs/contexto/04`, `05`, `07`, `10`, `11`, `12`; texto para draw.io, Sheet y Doc; Linear NOT-130 y aviso a Katherine; vault y memoria; archivo del SDD | Docs | — | lista de la skill completa | [ ] |

## Pruebas finales (Fase 5)

- [ ] Todos los criterios CA-01 a CA-18 verificados (tabla de trazabilidad).
- [ ] Backend: `mvn clean test` en verde (hoy 306 pruebas). Frontend: `pnpm test`, `pnpm lint` y `pnpm build` en verde.
- [ ] Verificación manual en local (flujo completo, español, 360 px y 1366 px).
- [ ] Documentos actualizados: `spec.md`, `plan.md`, `docs/contexto/` y los externos.

## Trazabilidad

| Criterio | Tareas | Prueba | Resultado |
| --- | --- | --- | --- |
| CA-01 | T-09, T-10, T-19 | `PlantillaReporteTest`, `EnrutamientoIntegracionTest`, script | verde en backend (falta script) |
| CA-02 | T-08 | `GrupoPlantillaTest` | verde |
| CA-03 a CA-06 | T-09 | `PlantillaReporteTest` | verde |
| CA-07 | T-09, T-20 | `PlantillaReporteTest` + Gmail | texto plano verde; falta Gmail |
| CA-08 | T-06, T-10 | `EnrutamientoIntegracionTest` | verde |
| CA-09, CA-09b, CA-09c | T-02, T-07, T-14, T-15, T-20 | `EnrutamientoIntegracionTest`, `node --test`, manual | pendiente |
| CA-10, CA-10b | T-06, T-07 | `CanalesRutaIntegracionTest`, `EnrutamientoIntegracionTest` | verde (12 nov) |
| CA-11 | T-03, T-07, T-12, T-17 | `EnrutamientoIntegracionTest`, `VistaPreviaIntegracionTest`, manual | pendiente |
| CA-12 | T-07, T-15 | `EnrutamientoIntegracionTest`, manual | pendiente |
| CA-13, CA-14 | T-01, T-11, T-16 | `VistaPreviaIntegracionTest`, `node --test`, manual | pendiente |
| CA-15 | T-13, T-19 | `VistaPreviaIntegracionTest`, script | verde en backend (falta script) |
| CA-16 | T-11 | `VistaPreviaIntegracionTest` | verde |
| CA-17, CA-18 | T-05, T-06 | `CanalesMigracionIntegracionTest` | verde (12 nov) |

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-12 | SDD de NOT-129 archivado; spec de NOT-130 escrita y aprobada | spec.md |
| 2026-11-12 | Atajo para gas, energía y aseo (opción A); WhatsApp de Veolia verificado | spec.md, plan.md |
| 2026-11-12 | Plan aprobado; endpoints PLANEADO en el contrato; tareas creadas | plan.md, tasks.md, `docs/contexto/11` |
| 2026-11-12 | Grupo 1 hecho: V21 y `Ruta.canales` | tasks.md |
| 2026-11-12 | Grupo 2 hecho: carta formal, vista previa y `GET /mensaje`. Backend 352 pruebas, 0 fallos, 2 omitidas | tasks.md |
| 2026-11-12 | Revisión del Grupo 1 corregida: canal alterno de gas y aseo = el recomendado; error honesto si el canal se cerró; prueba del envío a Centroaguas. Backend 313 pruebas, 0 fallos, 2 omitidas (proveedores reales) | tasks.md |
