# Tareas — Despliegue final, respaldo y administrador real

> **Fase 3 de 5 del flujo SDD.** Desglose de `plan.md` en pasos pequeños y verificables.

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) (aprobada) |
| Plan | [plan.md](plan.md) (aprobado) |
| Estado general | **Sin iniciar** (NOT-133 sigue en «Todo» en Linear por decisión de Andrés) |
| Última actualización | 2026-11-14 |

## Cómo se usa

- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Una tarea = un cambio pequeño. Un solo PR (rama `feature/not-133-despliegue-final`), commits atómicos.
- «Andrés» marca lo que solo puede hacer él (Render, Neon, GitHub). Yo dejo el comando listo.
- Todo cambio de datos en producción espera su visto bueno y un branch de respaldo en Neon.

## Tareas

### Parte A — lo que se hace ya (código y documentos)

| ID | Tarea | Quién | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | Prueba que falla primero: una base nueva debe arrancar sin `ADMIN_DEV_PASSWORD` y la clave vieja del admin sembrado no debe servir | Claude | CA-05 | prueba de integración nueva | [ ] |
| T-02 | `application.yml`: `adminPassword: ${ADMIN_DEV_PASSWORD:${random.uuid}}`; comprobar que Flyway no rechaza V4 en una base que ya la tiene | Claude | CA-04, CA-05 | T-01 en verde + migración sobre base con V4 | [ ] |
| T-03 | Workflow `.github/workflows/mantener-despierto.yml` (cron cada 10 min + manual, `curl` a `/api/health`, sin secretos) | Claude | CA-03 | revisión del YAML; se ejecuta a mano tras integrarse | [ ] |
| T-04 | `tools/verificar-despliegue.sh`: salud y CORS (sitio y vista previa) con `curl`, sin secretos | Claude | CA-07 | se ejecuta contra producción | [ ] |
| T-05 | `docs/contexto/15-guia-de-despliegue.md`: Netlify, Render, Neon, Cloudinary, Resend, Google, variables (solo nombres), aviso periódico, vuelta atrás; enlaza a `14-activar-envio-real.md` | Claude | CA-01, CA-08 | `grep` de secretos + variables contra `application.yml` | [ ] |
| T-06 | En la guía: procedimiento de respaldo (branch de Neon + `pg_dump`) y restauración en base vacía con conteos de comparación | Claude | CA-02 | se ejecuta en T-09 | [ ] |
| T-07 | En la guía: script del administrador real con variables de `psql` (sin valores en el repo), `SELECT` previo del correo, columnas verificadas en `10-modelo-de-datos.md` | Claude | CA-04 | se ejecuta en T-11 | [ ] |
| T-08 | Actualizar `02-stack`, `04-requisitos` (RNF nuevos), `08-pendientes` (respaldos automáticos), `12-estado-actual`, `13-sprint-3`, `14-activar-envio-real` (quitar espera del CAIC), `CLAUDE.md` y la skill `notiurba-pruebas-y-despliegue` | Claude | CA-01, CA-08 | revisión cruzada | [ ] |

### Parte B — operación en producción (después de integrar la Parte A)

| ID | Tarea | Quién | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-09 | Respaldo de Neon y restauración en un contenedor `postgis` vacío; comparar conteos de usuarios, reportes, comunas y barrios; borrar el volcado | Andrés (yo guío) | CA-02 | conteos iguales, anotados en la guía | [ ] |
| T-10 | Medir el arranque en frío (servidor dormido) y con el aviso activo; ejecutar el workflow a mano | Andrés (yo guío) | CA-03 | tiempos anotados en la guía | [ ] |
| T-11 | Branch de respaldo en Neon → `SELECT` del correo → `UPDATE` del admin real (Andrés López) → probar inicio de sesión | Andrés | CA-04 | login con la cuenta real | [ ] |
| T-12 | Borrar `ADMIN_DEV_PASSWORD` de Render y comprobar que el servidor arranca | Andrés | CA-04 | `/api/health` 200 tras el despliegue | [ ] |
| T-13 | Integrar el PR #42, decidir `v1.0.0` o versión previa y crear la etiqueta; ver que Render use ese commit | Andrés | CA-06 | etiqueta = último commit de `main` | [ ] |
| T-14 | Ejecutar `tools/verificar-despliegue.sh` y la lista manual (registro, reporte con foto, panel) | Andrés / Amelia | CA-07 | lista marcada | [ ] |
| T-15 | Activar el envío real cuando Andrés lo decida (guía 14); no es parte de esta historia | Andrés | CA-08 | — | [ ] |

## Pruebas finales (Fase 5)

- [ ] Todos los criterios CA-01 a CA-08 verificados.
- [ ] Backend: `mvn clean test` completo (con variables `DB_*`) en verde. Frontend sin cambios: `pnpm test`, `pnpm lint`, `pnpm build` en verde.
- [ ] Revisión con el subagente `code-reviewer` y hallazgos corregidos.
- [ ] Documentos al día y SDD archivado en `docs/sdd/historico/NOT-133-despliegue-final/`.

## Trazabilidad

| Criterio | Tareas | Prueba | Resultado |
| --- | --- | --- | --- |
| CA-01 | T-05, T-08 | `grep` de secretos y variables | pendiente |
| CA-02 | T-06, T-09 | restauración con conteos iguales | pendiente |
| CA-03 | T-03, T-10 | tiempos medidos y workflow ejecutado | pendiente |
| CA-04 | T-02, T-07, T-11, T-12 | login real y arranque sin la variable | pendiente |
| CA-05 | T-01, T-02 | prueba de integración | pendiente |
| CA-06 | T-13 | etiqueta = commit de `main` | pendiente |
| CA-07 | T-04, T-14 | script y lista manual | pendiente |
| CA-08 | T-05, T-08, T-15 | enlaces entre guías | pendiente |

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-14 | Tareas escritas con spec y plan aprobados | tasks.md |
