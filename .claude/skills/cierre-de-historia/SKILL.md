---
name: cierre-de-historia
description: Lista de pasos para cerrar una historia de NotiUrba dejando toda la documentación al día (docs del repo, SDD, modelo de datos, Linear, Figma, vault y memoria). Úsese cada vez que una historia con código o con decisiones nuevas esté lista para cerrarse, antes de decir que terminó.
---

# Cierre de historia en NotiUrba

Una historia **no está cerrada** hasta que su documentación está al día. Se hace al terminar la
implementación y las pruebas, y antes de abrir el PR (o en el mismo PR, en commits aparte).
Contexto del repo: `CLAUDE.md` y `docs/contexto/12-estado-actual-y-traspaso.md`.

## 1. Antes de documentar: que todo esté verificado

- Pruebas del backend en verde (`mvn clean test`, ver la skill `notiurba-pruebas-y-despliegue`) y, si
  se tocó el frontend, `pnpm test && pnpm lint && pnpm build`.
- Si cambió una ruta del panel: `tools/qa-panel-admin/qa-panel.sh` contra el jar real.
- Subagente `code-reviewer` sobre la rama; hallazgos corregidos con pruebas.
- Anota el antes/después de lo medido (tiempos, pruebas, errores). Sin números no se documenta «mejoró».

## 2. Documentación del repo (`docs/`)

Revisa cada archivo y cambia solo lo que la historia tocó. Busca con `grep` los términos viejos
(nombres de tablas, rutas, cifras) para no dejar frases contradictorias.

| Archivo | Se actualiza si… |
| --- | --- |
| `docs/contexto/10-modelo-de-datos.md` | cambió una tabla, columna, trigger, vista o índice |
| `docs/contexto/11-contrato-api.md` | cambió una ruta, un parámetro, un error o un tiempo medido |
| `docs/contexto/07-asignacion-de-correos.md` | cambió un canal, un correo o una plantilla |
| `docs/contexto/12-estado-actual-y-traspaso.md` | siempre: pruebas, migraciones (siguiente número), problemas conocidos |
| `docs/contexto/08-pendientes.md` y `13-sprint-3.md` | la historia cerró algo pendiente o abrió algo nuevo |
| `docs/decisiones/` (ADR) | se tomó una decisión de fondo; no se reabren las cerradas |
| `.claude/skills/notiurba-pruebas-y-despliegue/SKILL.md` | cambió la meta de pruebas o la siguiente migración |
| `CLAUDE.md` | cambió el stack, una regla de trabajo o una decisión abierta |

## 3. SDD (`docs/sdd/`)

1. Marca en `tasks.md` cada tarea con su commit, rellena la trazabilidad y el registro de cambios.
2. Al cerrar la historia, archiva `spec.md`, `plan.md` y `tasks.md` en
   `docs/sdd/historico/NOT-<nn>-<nombre>/` y deja los tres archivos de `docs/sdd/plantillas/` listos
   para la siguiente historia. Un commit aparte: `docs(sdd): archivar NOT-<nn>`.

## 4. Modelo de datos fuera del repo

Enlaces vigentes (confirmados por Andrés el 12 nov 2026): carpeta Drive «NotiUrba»
`1O6IreUnIaHKUzV9-nVBGBg6HSw8XbvM5`; **Diagrama ER (draw.io)** `1fW4OvzK5Ftd6oeCLnb7mIpD7P_H4GX7y`;
**Sheet «Tablas Diagrama ER en excel»** `1eqkDyoNt2_BAUTiUmpy3oIQ96jYpPSc5FriT_bK0r9k`; componentes y
despliegue `1DZ8XC3TJaDDj9_BVyyOcUbN4zdmCk4GX`; Figma `qRLcQ7jjC71F52CI6S9m1j`.

- **Draw.io del ER:** Claude no lo edita. Entrega a Andrés un archivo local (en la carpeta del SDD archivado)
  con las tablas completas que cambiaron, listas para pegar, y qué relaciones se agregan.
- **Google Sheet:** Andrés pidió que Claude lo actualice. Es aditivo: una hoja nueva fechada con las tablas,
  columnas, reglas y datos de la historia, y solo se corrigen las celdas de `v5` que quedaron falsas. Las
  pestañas `Diccionario` y `Reglas v5` son cuadrículas grandes: se avisa si quedan sin actualizar.
- **Google Doc «NotiUrba»:** igual: una sección «Actualización <fecha>» al final de las pestañas afectadas
  («Asignación de correos», «Modelado», «Reglas de negocio» y las que toque), sin reescribir lo anterior.
- **Cómo:** por Chrome (Claude in Chrome), pegando con un evento de portapapeles. Antes de editar, confirmar que
  el documento está en modo edición; después, comprobar con una captura y avisar a Andrés qué se tocó.
- **Neon:** lo aplican las migraciones al desplegar. Antes de una migración que cambia datos, branch
  de respaldo si Andrés lo pide; después, una consulta de solo lectura que compruebe el resultado.

## 5. Linear, Figma y quien depende de la historia

- **Linear:** actualiza la descripción de la historia (decisiones, resultado, rama o PR) y deja un
  comentario en las historias que dependen de ella, empezando con «Actualización:», sin saludo y como si
  ya estuviera en `develop`. La historia pasa a Done cuando el PR se integra, no antes. No se crean
  historias nuevas sin el visto bueno de Andrés.
- **Figma:** si cambió una pantalla, dilo y espera su visto bueno antes de editar. Si toca pantallas de
  Katherine, señálalo expresamente en el PR y en Linear.

## 6. Vault de Obsidian y memoria

- Vault (`/Users/andy/Documents/Obsidian Vault/wiki/`): `proyectos/noti-urba/noti-urba-sprint-3.md`
  (sección «Actualización <fecha>»), `noti-urba-estado-actual.md`, `personal/hoy.md`,
  `personal/por-verificar.md` si se resolvió algo, y una entrada en `log.md`.
- Memoria de Claude: guarda solo lo que no se puede leer en el repo (decisiones de Andrés, evidencia
  externa, errores a evitar). No repitas código ni historial de git.

## 7. Commits y PR

- Commits atómicos y sin líneas de coautoría de Claude. La documentación va en commits propios.
- PR pequeño hacia `develop`, con: qué trae, antes/después, pruebas, qué se debe revisar con cuidado
  y qué no se verificó. No se mezcla nada que cambie datos de producción sin su aprobación.

## 8. Cierre

Termina diciendo qué quedó al día, qué no (por ejemplo, el draw.io o el Doc que Andrés debe pegar) y
qué falta antes de desplegar.
