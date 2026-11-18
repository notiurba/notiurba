# Tareas — Correcciones de cierre QA y despliegue

> **Fase 3 de 5 del flujo SDD.** Desglose verificable del [plan técnico aprobado](plan.md).
> No se modifica código, configuración externa ni datos hasta que estas tareas estén **Aprobadas**.

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) (aprobada el 2026-11-16) |
| Plan | [plan.md](plan.md) (aprobado el 2026-11-16) |
| Historia / ticket | Sin historia de Linear; correcciones de cierre QA |
| Rama / destino | `codex/correcciones-qa-despliegue` → `develop` |
| Estado general | **Revisión cerrada (Puerta 3); espera autorización de Andrés para T-20** (Borrador → En revisión → Aprobado) |
| Última actualización | 2026-11-17 |

## Cómo se usa

- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada · `[-]` no aplica, con motivo.
- Se mantiene un solo PR para esta corrección. NOT-133 permanece pausada y en «Todo».
- Cada tarea de implementación empieza con una prueba que demuestre el defecto cuando sea viable.
- Las identidades reales se suministran solo de forma interactiva y se registran como `Cuenta QA-A`, `Cuenta QA-B` y `Administrador QA`.
- Ninguna tarea autoriza por sí sola un `commit`, `push`, PR, despliegue o cambio externo. Esas operaciones se ejecutan únicamente cuando Andrés las solicite o confirme en su fase correspondiente.
- Antes de cualquier eliminación se resuelven y muestran los objetivos exactos, se valida su pertenencia a la preparación QA y se obtiene la confirmación de Andrés para ejecutar la operación destructiva.

## Parte A — Corrección local con pruebas primero

| ID | Tarea | Responsable | Cubre | Comprobación de salida | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | Agregar pruebas de `reportesService` para respuesta válida y propagación de error de red, 401, 403 y 500; comprobar que no retorna `reportesPrueba` | Codex | CA-01, CA-03 | Las pruebas fallan con el fallback actual y pasan tras T-02 | [x] |
| T-02 | Eliminar el fallback ficticio de `reportesService` y mantener la normalización de respuestas exitosas | Codex | CA-01, CA-03 | T-01 en verde; búsqueda de importaciones de producción sin `reportesPrueba` | [x] |
| T-03 | Agregar pruebas de interacción del mapa para carga, error seguro, ausencia de marcadores ficticios y «Reintentar» exitoso | Codex | CA-01, CA-02 | Las pruebas fallan con el flujo manual actual y pasan tras T-04 | [x] |
| T-04 | Migrar `MapaComunas` a `useCargar`, mostrar carga/error/reintento y retirar el texto que presenta datos de demostración ante fallos | Codex | CA-01, CA-02 | T-03 en verde y ningún dato anterior o simulado durante error/reintento | [x] |
| T-05 | Agregar pruebas de backend para envío aceptado, fallo de proveedor, limpieza del token nuevo, conservación del token anterior, límite no consumido y respuesta pública genérica | Codex | CA-05, CA-07, CA-08 | Pruebas unitarias/integración fallan con el flujo actual y pasan tras T-06/T-07 | [x] |
| T-06 | Implementar la operación de repositorio que invalida tokens de recuperación anteriores sin invalidar el hash nuevo; conservar la operación usada por verificación de correo | Codex | CA-05, CA-06, CA-08 | Pruebas de repositorio y servicio en verde; sin migración | [x] |
| T-07 | Ajustar `RecuperacionServicio` para persistir el token antes del envío, conservarlo solo si Resend acepta y eliminarlo si falla sin consumir el cupo ni invalidar un enlace anterior válido | Codex | CA-05, CA-06, CA-08 | T-05 en verde; respuesta idéntica para tradicional, solo Google e inexistente | [x] |
| T-08 | Agregar trazabilidad segura a recuperación y capturar el identificador aceptado por Resend, sin correo, token, contraseña ni URL en logs | Codex | CA-05, CA-08 | Pruebas de eventos/logs y búsqueda de datos sensibles en mensajes nuevos | [x] |
| T-09 | Actualizar el contrato API, el estado/traspaso y la guía de simulación para documentar el comportamiento corregido sin guardar identidades ni secretos | Codex | CA-03, CA-05, CA-08, CA-15 | Revisión cruzada con código y búsqueda de correos/secretos | [x] |
| T-10 | Crear `docs/validacion/correcciones-cierre-qa.md` con matriz vacía de fecha, entorno, commits, resultado, evidencia, limpieza y omisiones | Codex | CA-09, CA-15 | Plantilla enlaza CA-01 a CA-15 y usa solo etiquetas QA | [x] |

## Parte B — Verificación local e interfaz

| ID | Tarea | Responsable | Cubre | Comprobación de salida | Estado |
| --- | --- | --- | --- | --- | --- |
| T-11 | Ejecutar en `frontend/` `pnpm install`, `pnpm test`, `pnpm lint` y `pnpm build`; inspeccionar `dist` para descartar códigos, descripciones y coordenadas de `reportesPrueba` | Codex | CA-01 a CA-03 | Comandos en verde y hallazgo cero de fixtures ficticios en el build | [x] |
| T-12 | Ejecutar en `backend/` `mvn clean test` con PostGIS desechable y registrar pruebas ejecutadas, fallidas y omitidas | Codex | CA-05 a CA-08, CA-10, CA-12 a CA-14 | Suite completa en verde; cualquier omisión queda identificada y no cuenta como aprobación | [x] |
| T-13 | Ejecutar `MigrationIntegrationTest` en entorno limpio con solo las variables necesarias y sin Neon | Codex | Regresión de despliegue | Migraciones aplican desde cero sobre PostGIS desechable | [x] |
| T-14 | Auditar y ejecutar los scripts locales del panel y correo con base desechable y envío externo apagado | Codex | CA-10 a CA-14 | `qa-panel.sh` y `qa-correo.sh` pasan sin tocar producción ni enviar correo real | [x] |
| T-15 | Validar en navegador el mapa a 360 px y escritorio: error de red/HTTP, cero marcadores ficticios y reintento exitoso | Codex | CA-01, CA-02 | Evidencia local de ambos tamaños y estados | [x] |
| T-16 | Revisar el distintivo de Netlify a 360 px en los pasos 1, 2 y 3. Si la opción oficial no puede apagarse, implementar y probar la reserva móvil de espacio, incluida navegación por teclado | Codex | CA-04 | «Continuar» visible, enfocable y pulsable; la rama condicional usada queda documentada | [x] |

## Parte C — Auditoría obligatoria antes del despliegue

| ID | Tarea | Responsable | Cubre | Comprobación de salida | Estado |
| --- | --- | --- | --- | --- | --- |
| T-17 | Invocar el subagente `code-reviewer` definido en `.claude/agents/code-reviewer.md` sobre `git diff develop...HEAD` y `git diff HEAD`, incluyendo todas las funciones tocadas y sus llamadores | Subagente `code-reviewer` | Calidad, seguridad, privacidad, datos y pruebas | Informe de máximo 10 hallazgos, con archivos/líneas, y veredicto «Listo para cerrar» o «Corregir antes de cerrar» | [x] 8 hallazgos (1 alto, 3 medios, 4 bajos); «Corregir antes de cerrar» |
| T-18 | Corregir todos los hallazgos reales del `code-reviewer`; documentar cualquier desacuerdo con evidencia técnica y volver a solicitar revisión si hubo cambios sustanciales | Claude (continúa a Codex) | Calidad transversal | Cero hallazgos pendientes que impidan cerrar | [x] tres pasadas: 8 hallazgos, luego 6 (1 nuevo), luego 0; veredicto final «Listo para cerrar» |
| T-19 | Repetir las pruebas afectadas y después las verificaciones finales completas de frontend y backend tras la auditoría | Claude (continúa a Codex) | CA-01 a CA-08, CA-10, CA-12 a CA-14 | `pnpm test`, `pnpm lint`, `pnpm build` y `mvn clean test` en verde después de las correcciones | [x] frontend 239/0, lint y build en verde; backend 477/0/2; `MigrationIntegrationTest` 2/0 |

### Correcciones de la revisión (T-18)

| Hallazgo | Corrección | Prueba que lo demuestra |
| --- | --- | --- |
| 1 (alto) El ejecutor compartido pasó a rechazar tareas y afectaba registro y reenvío | `SegundoPlanoConfig` vuelve a descartar con aviso, igual que en `develop` | `SegundoPlanoConfigTest` (falló antes de corregir) |
| 2 (medio) La respuesta de `/recuperar` tardaba distinto según la cuenta | La petición solo encola; cuenta, límite, pendiente y envío pasan al hilo de fondo (plan §3.2) | `laPeticionSoloEncolaYNoConsultaLaBaseParaNingunCorreo` (falló antes de corregir) y `RecuperacionControllerTest` |
| 3 (bajo) La limpieza a los 5 min podía borrar tareas en cola | El pendiente nace al empezar la tarea; aviso si el enlace no quedó activo | `siElPendienteYaNoExisteAlConfirmarSeAvisaQueElEnlaceNoQuedoActivo` |
| 4 (bajo) El log no distinguía la causa del fallo | `CorreoNoEnviadoException.estadoHttp()`; el log registra tipo, reintentable y código HTTP, nunca el mensaje | `unRechazoDeResendRegistraSiEsReintentableYElCodigoSinElCuerpoDeLaRespuesta`, `siPrepararFallaNoSeLlamaAlProveedorYSeRegistraLaCausa` |
| 5 (medio) La prueba de «Reintentar» no usaba la página real | `CargaReportesMapa.jsx` decide carga/error/mapa y lo usa `MapaComunas`; `react-test-renderer` fijado en la versión de `react` | `CargaReportesMapa.test.mjs` (falla si se quita la rama de error) |
| 6 (medio) Pruebas de recuperación que pasaban sin probar | Pasos fijos en lugar de una carrera falsa, errores exactos (`ENLACE_INVALIDO`), rollback real de `confirmar`, pendiente reciente que ocupa cupo, reintento, sin umbral de tiempo | `RecuperacionIntegracionTest` (falla si `confirmar` deja de ser transaccional), `RecuperacionServicioTest` |
| 7 (bajo) Código muerto y comentarios viejos | Se borró `EnviadorEnlaceRecuperacionLog` y `@Primary`; javadocs, imports y `BooleanSupplier`; nota en `08-pendientes.md` | Suite completa |
| 8 (bajo) `<main>` anidado y alcance de la reserva móvil | `<section role="alert">` y `<div role="status">`; alcance real documentado en plan §3.5 | `CargaReportesMapa.test.mjs` |

**Segunda pasada (6 hallazgos):**

| Hallazgo | Corrección | Prueba que lo demuestra |
| --- | --- | --- |
| 1 (medio, nuevo) Al encolar toda solicitud, `/recuperar` (público) podía llenar la cola del correo de verificación | Cola propia `tareasRecuperacion` (2 hilos, 20 puestos), también con descarte y aviso | `laRecuperacionUsaUnaColaDistintaALaDelCorreoDeVerificacion` (falló antes de corregir) y `conLaColaDeRecuperacionLlenaElCorreoDeVerificacionSigueSaliendo` |
| 2 (bajo) Si Resend no responde a tiempo, el correo puede llegar con un enlace que no sirve | Documentado en plan §3.2 y en el contrato (seguro: no gasta cupo) | — |
| 3 (bajo) El `@BeforeEach` aún usaba `now()` de la base | Usa la hora de Java | `RecuperacionIntegracionTest` |
| 4 (bajo) Comprobaciones y nombres engañosos | Se quitó una comprobación que siempre se cumplía; nombres corregidos | — |
| 5 (bajo) Docs incompletos | plan §3.2 y §4.1; cifra de la evidencia | — |
| 6 (bajo) Import sin uso | Quitado | Compila |

La tercera pasada confirmó las 6 correcciones sin hallazgos nuevos.

Además: dos pruebas de integración creaban filas con `now()` de la base y comparaban con la hora de Java; una falló una vez en la suite completa (reloj de la VM de Docker desfasado). Ahora usan una sola hora.

**Fuera del SDD, pedido por Andrés el 2026-11-17:** el favicon era el logo por defecto de Vite. Se reemplazó por el de NotiUrba (`favicon-16x16.png`, `favicon-32x32.png` y `apple-touch-icon.png`, generados desde `src/compartido/assets/logo-notiurba.png`) y se actualizó la lista de `public/` en `publico.test.mjs`. Para Katherine: los íconos de 192 y 512 px del manifiesto de NOT-134 (PWA) deben salir del mismo logo.

## Parte D — Integración y preparación operativa

| ID | Tarea | Responsable | Cubre | Comprobación de salida | Estado |
| --- | --- | --- | --- | --- | --- |
| T-20 | Preparar el único PR hacia `develop` con resumen, criterios, pruebas, omisiones, riesgos y plan de reversión; ejecutar `commit`, `push` y creación del PR solo con autorización de Andrés | Codex / Andrés | CA-09, CA-15 | Diff limitado a la corrección y checks del PR en verde | [ ] |
| T-21 | Después de integrar y desplegar, confirmar rama y commit exactos de Netlify y Render, salud, CORS y presencia por nombre de las variables requeridas; no leer valores | Codex / Andrés | CA-09 | Ambos servicios corresponden al commit aprobado; `tools/verificar-despliegue.sh` pasa | [ ] |
| T-22 | Revisar la opción oficial `Powered by Netlify badge`; apagarla si está disponible y Andrés confirma el cambio. Si no está disponible, conservar el fallback validado en T-16 | Codex / Andrés | CA-04 | Resultado y evidencia a 360 px; sin bloquear scripts mediante CSP | [ ] |

## Parte E — QA controlado en producción

| ID | Tarea | Responsable | Cubre | Comprobación de salida | Estado |
| --- | --- | --- | --- | --- | --- |
| T-23 | Con `Cuenta QA-A`, solicitar una sola recuperación, correlacionar aceptación/entrega en Resend, abrir la ruta desplegada, cambiar la clave y verificar que el enlace no se reutiliza | Codex / Andrés | CA-05, CA-06 | Recepción real, acceso con clave nueva y segundo uso rechazado, sin exponer credenciales | [ ] |
| T-24 | Solicitar recuperación para una dirección inexistente reservada y comprobar la respuesta genérica sin correo; el caso solo-Google se mantiene cubierto por prueba automatizada | Codex | CA-07 | Misma respuesta pública y ningún evento de envío | [ ] |
| T-25 | Antes de crear reportes, confirmar `RESEND_PRUEBA_DESTINO` y el envío habilitado. Con `Cuenta QA-B`, crear un reporte de Alcaldía con foto y uno de Centroaguas; consultar reporte y fotografía por API | Codex / Andrés | CA-10, CA-11 | Códigos recuperables, fotos asociadas y persistencia visible solo para la cuenta autorizada | [ ] |
| T-26 | Confirmar en Resend y en el buzón QA que los dos correos fueron desviados al destino de prueba y que ninguna entidad real recibió mensajes | Codex / Andrés | CA-11 | Dos entregas QA correlacionadas y cero destinatarios reales | [ ] |
| T-27 | Consultar `NU-000010` con `Administrador QA`; validar aviso, contenido, propietario e historial sin modificar el estado | Codex / Andrés | CA-12 | Precondiciones documentadas; se detiene el caso si no corresponden | [ ] |
| T-28 | Solo si T-27 cumple y Andrés confirma el cambio, registrar oficialmente «Resuelto» y verificar historial y aparición en «Mis reportes» de la cuenta propietaria | Codex / Andrés | CA-13 | Estado, actor, fecha y observación visibles para admin y ciudadano | [ ] |
| T-29 | Preparar dos objetivos QA independientes para «Eliminar mis datos» y «Eliminar mi cuenta»; resolver IDs y propietarios y solicitar confirmación justo antes de cada `DELETE` | Codex / Andrés | CA-14 | Lista exacta de objetivos autorizados y efectos esperados; todavía sin eliminar | [ ] |
| T-30 | Tras la confirmación de T-29, ejecutar cada eliminación controlada y comprobar respuestas, fotografías removidas, restricciones y ausencia de impacto sobre terceros | Codex / Andrés | CA-14 | Evidencia posterior por preparación QA y verificación de no afectación | [ ] |
| T-31 | Ejecutar la verificación final del despliegue, completar la matriz CA-01 a CA-15 y registrar datos QA creados y limpieza realizada; no repetir la campaña móvil ni las 35 solicitudes de Postman | Codex | CA-09, CA-15 | Matriz completa, pruebas omitidas justificadas y riesgos residuales explícitos | [ ] |

## Puertas de avance

- [x] **Puerta 1 — Tareas aprobadas:** se permite comenzar T-01; todavía no se autoriza producción.
- [x] **Puerta 2 — Local listo:** T-01 a T-16 completas, con pruebas y navegador verificados.
- [x] **Puerta 3 — Revisión lista:** T-17 a T-19 completas y `code-reviewer` sin hallazgos pendientes de cierre.
- [ ] **Puerta 4 — Integración autorizada:** Andrés autoriza las operaciones de T-20.
- [ ] **Puerta 5 — Despliegue consistente:** T-21 y T-22 completas antes de usar producción como QA.
- [ ] **Puerta 6 — QA productivo cerrado:** T-23 a T-31 completas sin destinatarios ni datos ajenos.

## Trazabilidad

| Criterio | Tareas principales | Evidencia esperada | Resultado |
| --- | --- | --- | --- |
| CA-01 | T-01 a T-04, T-11, T-15 | Pruebas de servicio/mapa y navegador sin datos ficticios | local en verde |
| CA-02 | T-03, T-04, T-15 | Reintento recupera exclusivamente datos del API | local en verde |
| CA-03 | T-01, T-02, T-09, T-11 | Build sin fixture y documentación actualizada | local en verde |
| CA-04 | T-16, T-22 | Botón utilizable a 360 px con resultado operacional o CSS condicional | fallback local en verde; opción Netlify pendiente |
| CA-05 | T-05 a T-08, T-12, T-23 | Envío aceptado, recibido y ruta correcta | local en verde; producción pendiente |
| CA-06 | T-05 a T-07, T-12, T-23 | Enlace de una hora/un uso y login desbloqueado | local en verde; producción pendiente |
| CA-07 | T-05, T-07, T-12, T-24 | Respuesta indistinguible y cero envío no elegible | local en verde; producción pendiente |
| CA-08 | T-05 a T-08, T-12 | Fallo controlado, diagnóstico seguro y solicitud posterior válida | local en verde |
| CA-09 | T-10, T-20, T-21, T-31 | Commits y configuración por nombre | pendiente |
| CA-10 | T-12, T-14, T-25 | Reporte y foto persistidos y consultables | pendiente |
| CA-11 | T-14, T-25, T-26 | Correos de Alcaldía y Centroaguas solo al buzón QA | pendiente |
| CA-12 | T-12, T-14, T-27 | Aviso visible sin cambio automático de estado | pendiente |
| CA-13 | T-12, T-14, T-28 | Resolución oficial e historial en ambas vistas | pendiente |
| CA-14 | T-12, T-14, T-29, T-30 | Eliminaciones acotadas y fotos removidas | pendiente |
| CA-15 | T-09, T-10, T-31 | Matriz completa sin repetir evidencia aceptada | pendiente |

## Aprobación

- [x] Las tareas implementan el plan aprobado sin ampliar el alcance.
- [x] La auditoría del subagente `code-reviewer` es obligatoria antes del despliegue.
- [x] Las operaciones externas y destructivas tienen puertas de autorización explícitas.
- [x] **Aprobadas por el usuario** → se puede iniciar T-01.

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-16 | Tareas redactadas a partir de spec y plan aprobados; se incorpora auditoría obligatoria de `code-reviewer` | tasks.md |
| 2026-11-16 | Tareas aprobadas por Andrés; inicia la implementación local por T-01 | tasks.md |
| 2026-11-17 | T-01 a T-16 completadas; pruebas, scripts aislados y Chrome en verde; inicia T-17 | tasks.md |
| 2026-11-17 | T-17 cerrada (8 hallazgos); T-18 corrige los 8 y ajusta plan §3.2, §3.4 y §3.5; favicon de NotiUrba a pedido de Andrés | tasks.md, plan.md |
