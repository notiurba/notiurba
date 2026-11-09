---
name: traspaso-de-sesion
description: Cerrar una sesión larga dejando todo listo para que otra IA (u otra persona) empiece con datos frescos: actualizar la documentación del repositorio, el vault de notas (Obsidian), la memoria de Claude y crear skills de los flujos repetidos. Úsese cuando el usuario se despide, cambia de cuenta o pide "actualiza todo lo desactualizado".
---

# Traspaso de sesión

Objetivo: quien llegue después no debe repetir preguntas ni redescubrir lo ya aprendido. Hablar en español claro.

## Orden de trabajo

1. **Ubique las fuentes** (nada de suponer): `CLAUDE.md` y `docs/` del repo; vault de notas (`CLAUDE.md` del vault manda:
   p. ej. `raw/` solo lectura, notas en `wiki/` con «Fuente:», `index.md`, `log.md`, `hoy.md`, `pendientes.md`,
   `por-verificar.md`); carpeta de memoria de Claude (`~/.claude/projects/<proyecto>/memory/` con `MEMORY.md`); skills
   existentes (`~/.claude/skills/`, `.claude/skills/`).
2. **Trabaje sobre la rama de integración al día**, no sobre una rama vieja: cree una rama/worktree `docs/traspaso-...`
   desde `origin/develop`. Un worktree de planeación puede estar muy atrás.
3. **Audite lo desactualizado** comparando cada documento con el código y con lo hecho en la sesión: estado, versiones
   (migraciones, dependencias), fechas de sprint, decisiones nuevas, pendientes ya resueltos, variables de entorno,
   enlaces, nombres. Reescriba el archivo entero si más de la mitad está viejo (p. ej. una lista de pendientes).
4. **Agregue un documento de «estado actual y traspaso»** en el repo: qué funciona, qué falta (con claves de Linear),
   cómo se corre y se prueba, producción y reglas de oro, datos de negocio que se confunden, problemas conocidos y cómo
   se trabaja con el usuario. Enlácelo desde `CLAUDE.md` y desde el índice de `docs/`.
5. **Vault:** actualice la nota principal del proyecto y las notas por tema, agregue notas nuevas si hace falta, y cumpla
   las reglas del vault (primera línea «Fuente:», marcar «por verificar» lo no confirmado, «(propuesta)» lo que propuso la
   IA, una línea en `log.md`, entradas en `index.md`, `hoy.md`, `pendientes.md`). No copie repositorios al vault ni toque `raw/`.
6. **Memoria de Claude:** un archivo por hecho (frontmatter `name`, `description`, `metadata.type`), enlazados con
   `[[nombre]]`, más una línea en `MEMORY.md`. Estado del proyecto (`project`), preferencias y correcciones del usuario
   (`feedback`), punteros a recursos (`reference`). No guarde lo que el repo ya registra; actualice en vez de duplicar.
7. **Skills:** si hubo un flujo repetido o una habilidad aprendida, créela como skill. Lo útil en otros proyectos va en
   `~/.claude/skills/<nombre>/SKILL.md` (global); lo propio de un repo, en `.claude/skills/`. `description` debe decir
   cuándo usarla. Deje copia de los globales en el repo (`docs/skills-globales/`) para que otra cuenta los instale.
8. **No toque secretos**, no suba nada a remoto ni haga commit sin que el usuario lo pida. Deje los cambios en una rama
   local y diga cómo hacer el commit y el PR (sin líneas de coautoría si el usuario lo prohíbe).
9. **Verifique** que los enlaces y rutas que escribió existen, que no copió secretos y que los números (puntos,
   pruebas, migraciones) salen de una fuente. Cierre con la lista de archivos cambiados y lo que NO pudo verificar.

## Qué suele quedar viejo

Listas de pendientes, cronograma y sprints, estado del despliegue (versión de migraciones, variables), decisiones
«abiertas» ya cerradas, números de pruebas y de historias, guías de planeación escritas antes del trabajo, cifras de
presentaciones, enlaces y fechas límite de la herramienta de gestión.
