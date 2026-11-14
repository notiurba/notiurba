# Plan técnico — Despliegue final, respaldo y administrador real

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se construye lo que dice `spec.md`.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado** (Borrador → En revisión → Aprobado) |
| Especificación | [spec.md](spec.md) (aprobada) |
| Aprobado por / fecha | Andrés, 2026-11-14 |
| Última actualización | 2026-11-14 |

## 1. Enfoque general

Casi todo es documentación y operación, con **un cambio mínimo de código** y **un workflow**:

1. `application.yml`: `ADMIN_DEV_PASSWORD` deja de ser obligatoria; si falta, toma un valor aleatorio
   (`${ADMIN_DEV_PASSWORD:${random.uuid}}`). Así se puede borrar de Render y una base nueva no queda con clave conocida.
2. Workflow `.github/workflows/mantener-despierto.yml`: llama a `/api/admin/ping` (responde 401, sin tocar la base) cada 5 min. Solo corre desde `main`.
3. Guía `docs/contexto/15-guia-de-despliegue.md` con respaldo, restauración, admin real, arranque en frío y lista de verificación.
4. Script de lista de verificación `tools/verificar-despliegue.sh` (solo `curl`: salud y CORS; sin secretos).

Alternativa descartada para el admin: crear una cuenta nueva. Se eligió convertir la sembrada (decisión de Andrés).

## 2. Stack y límites (fijados)

Sin cambios de stack. Solo GitHub Actions, que el repo ya usa (`edt51-validation.yml`).

## 3. Cambios por capa

### Base de datos
- **Sin migración nueva** (la siguiente libre sigue siendo V23). V4 no se toca.
- **Dato de producción** (solo con visto bueno de Andrés, y después de un branch de respaldo en Neon): el
  administrador sembrado pasa a ser el real. El script de la guía usa variables de `psql`
  (`-v correo=… -v nombre=… -v clave=…`) para que **ningún valor quede en el repo**:
  `UPDATE usuario SET correo=:'correo', primer_nombre=…, primer_apellido=…, password_hash=crypt(:'clave', gen_salt('bf')), correo_verificado_en=now() WHERE correo='admin.dev@notiurba.local' AND id_rol=2;`
  - Antes se comprueba con un `SELECT` que el correo nuevo **no** exista ya como ciudadano (el correo es único). Si existe, se cambia el correo de esa cuenta o se avisa antes de seguir.
  - Los nombres de columnas se confirman en `10-modelo-de-datos.md` al escribir la guía.

### Backend
- Solo `backend/src/main/resources/application.yml` (valor por defecto aleatorio). Sin clases nuevas.
- Se comprueba que Flyway no marque la migración V4 como modificada (su suma se calcula sobre el archivo, no sobre el valor del marcador).

### API
- Sin endpoints nuevos.

### Frontend
- Sin cambios.

### Integraciones y configuración
- **Variables:** sin nuevas. Se **retira** `ADMIN_DEV_PASSWORD` de Render (después de verificar que el arranque funciona sin ella y de convertir al admin).
- **Workflow** (`schedule: cron "*/5 * * * *"` + `workflow_dispatch`): `curl` a `/api/admin/ping` y acepta 401 o 403 (no usa la base, para no gastar las horas de Neon). Sin secretos. Si falla, el job falla y GitHub avisa por correo.
- **Resend / Cloudinary:** no se tocan. `RESEND_PRUEBA_DESTINO` sigue puesta; el envío real lo activa Andrés con `14-activar-envio-real.md`.

### Documentación
- Nueva `15-guia-de-despliegue.md`: Netlify, Render, Neon, Cloudinary, Resend, Google, variables (solo nombres), respaldo y restauración, admin real, aviso periódico, arranque en frío medido, verificación, vuelta atrás.
- Se actualizan `02-stack.md`, `12-estado-actual-y-traspaso.md`, `08-pendientes.md` (respaldos automáticos), `13-sprint-3.md`, `14-activar-envio-real.md` (quitar la espera del CAIC), `04-requisitos.md` (IDs nuevos), `CLAUDE.md` (variable ya no obligatoria) y la skill `notiurba-pruebas-y-despliegue`.

## 4. Estrategia de pruebas

| Criterio (spec) | Tipo de prueba | Dónde |
| --- | --- | --- |
| CA-01 | Revisión: seguir la guía con `grep` de secretos y comparar variables con `application.yml` | manual + `grep` |
| CA-02 | `pg_dump` de Neon → restaurar en un contenedor `postgis` vacío → comparar conteos de usuarios, reportes, comunas y barrios | manual, registrado en la guía |
| CA-03 | Medir el arranque en frío (`curl -w` con servidor dormido) y ver que el workflow corre (`workflow_dispatch`) | manual |
| CA-04 | Iniciar sesión con el admin real; quitar la variable en Render y ver que el servidor arranca | manual |
| CA-05 | Arrancar una base **nueva** sin `ADMIN_DEV_PASSWORD`: arranca, y la clave vieja del admin sembrado no sirve | prueba de integración (`MigrationIntegrationTest` o una nueva) + manual |
| CA-06 | `git tag` apunta al último commit de `main`; Render muestra ese commit | manual |
| CA-07 | `tools/verificar-despliegue.sh` + registro, reporte con foto y panel a mano | script + manual |
| CA-08 | Revisión de enlaces entre guías | manual |

Las pruebas existentes (backend 454, frontend 230) deben seguir pasando; se corre la suite completa con las variables `DB_*`.

## 5. Riesgos y mitigaciones

| Riesgo | Prob. / impacto | Mitigación |
| --- | --- | --- |
| El `UPDATE` deja al admin sin acceso | Baja / alto | Branch de respaldo en Neon antes; probar el login antes de retirar la variable; el `UPDATE` es reversible con el branch |
| El correo del admin ya existe como ciudadano o Google | Media / medio | `SELECT` previo; la vinculación de Google por correo no cambia el rol |
| Quitar la variable rompe el arranque | Baja / alto | Primero desplegar el valor por defecto; probar local sin la variable; recién después borrarla de Render |
| Flyway falla por la suma de V4 | Baja / alto | Prueba local con una base que ya tenga V4 aplicada |
| Pruebas de Actions agotan horas de Render Free (750 h) | Baja / medio | Un solo servicio 24 h = ~744 h; entra justo. Si faltan horas, se desactiva el workflow (se documenta) |
| Actions pausa el cron tras 60 días sin actividad del repo | Media / bajo | Se documenta; basta una ejecución manual |
| El respaldo contiene datos personales | Media / alto | El volcado no se sube al repo ni se imprime; se guarda local y se borra tras la prueba |

## 6. Alternativas descartadas

- Crear un administrador nuevo y desactivar el sembrado: más pasos y una cuenta de más.
- UptimeRobot: exige una cuenta externa.
- Respaldos programados con workflow: son extra para el MVP; van a `08-pendientes.md`.

## 7. Decisiones pendientes

- [PENDIENTE] Etiqueta `v1.0.0` o versión previa — decide: Andrés, tras integrar el PR #42.
- Decidido: administrador real a nombre de Andrés López con el correo que Andrés dio en el chat (no se escribe en el repo; se pasa al script al ejecutarlo).

## 8. Aprobación

- [x] Cada criterio de `spec.md` tiene un cambio técnico y una prueba.
- [x] No introduce otro framework ni otra base de datos.
- [x] Contrato de API y migración revisados (no hay cambios).
- [x] **Aprobado por el usuario** → se puede pasar a `tasks.md`.
