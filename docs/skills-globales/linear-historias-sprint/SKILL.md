---
name: linear-historias-sprint
description: Planear un sprint y crear historias de usuario en Linear con el formato de equipo (Historia, Decisiones, Criterios Dado-Cuando-Entonces, Dependencia, Sin resolver, Fuentes), puntos Fibonacci, reparto por integrante y verificación de cobertura. Incluye qué se puede y qué no se puede hacer con la API de Linear (ciclos, columnas del tablero) y cómo hacerlo por el navegador. Úsese al planear o crear historias de un sprint, o al ajustar ciclos y vistas de Linear.
---

# Historias de sprint en Linear

Probado en NotiUrba (Gestión de Proyectos, 3 sprints). Sirve para cualquier equipo académico o pequeño con Linear.
Hable en español claro. **No cree nada en Linear sin el visto bueno explícito** del usuario: crear historias cambia
gráficos y métricas del equipo.

## 1. Antes de proponer el reparto

1. Lea las historias de sprints anteriores para **copiar su formato**: `list_issues` con `cycle` (use el id completo del
   ciclo, no un prefijo; `list_cycles` con la clave del equipo, p. ej. `NOT`) y `get_issue` con `description`, `estimate`,
   `labels`, `priority`, `project`.
2. Revise el **estado real del código** en la rama de integración (no el del último documento): qué rutas, pantallas y
   pruebas existen. Marque «ya está hecho» lo que lo esté.
3. Liste los pendientes reales (tareas del WBS/EDT, errores que reportó el usuario, deuda conocida) y verifique cada
   afirmación de «falta X» leyendo el código o probando producción. No asuma.
4. Pregunte (una sola vez) lo que cambie el plan: fechas de los ciclos, topes por persona, qué ya aceptó el docente.

## 2. Reglas de estimación y reparto

- Puntos **Fibonacci** (1, 2, 3, 5, 8). Nada de 4 ni 6. Un 8 es señal de partir la historia; solo se deja si el tope de
  historias por persona lo obliga, y se explica por qué.
- Respete el tope de historias por persona que dé el usuario (en NotiUrba: máximo 5) y **no use subtareas** si lo pide.
- Compare con la velocidad del sprint anterior (suma de puntos de las historias, sin subtareas). Si el plan la supera,
  dígalo y dé una lista de qué recortar.
- Ponga dependencias entre personas explícitas (qué se entrega en la primera semana) y cárguelas en Linear como
  `blocks`/`blockedBy` después de crear las historias.
- Separe lo que depende de terceros (docente, entidad externa) en «Sin resolver (fuera del equipo)»; no deje que bloquee
  el cierre de la historia.

## 3. Formato de cada historia

Título: `[EDT x.y] Verbo + lo que logra el usuario` (o `[Mejora]` / `[Pruebas]` si no hay código del WBS).
Etiqueta: Feature, Improvement, Design o Docs. Prioridad: 1 Urgente, 2 Alta, 3 Media. Proyecto del equipo.

```
**Historia**
Como <rol>, quiero <qué>, para <para qué>.

**Decisiones tomadas**
* hechos ya acordados, límites, qué NO entra

**Reparto:** (solo si la historia se comparte) Persona A 2 pts, Persona B 1 pt.

**Criterios de aceptación**
* Dado <contexto>, cuando <acción>, entonces <resultado verificable>.
* (7 a 9 criterios; incluya error, vacío, móvil 360 px y escritorio 1366 px si es una pantalla)

**Dependencia:** qué necesita y a quién bloquea.
**Sin resolver (fuera del equipo):** ...
**Fuentes:** WBS, requisitos, documentos, enlaces.
```

Qué hace un criterio bueno: se puede comprobar sin preguntar al autor; trae cifras (2 s, 3 fotos, 4.5:1 de contraste);
nombra la pantalla o la ruta. Los valores que decide el equipo se dejan como «los define el equipo».

## 4. Verificación de cobertura (hágala siempre antes de presentar)

Tabla «Lo que falta» → historia que lo cubre. Incluya errores que reportó el usuario, pruebas, mejoras de UX/accesibilidad,
despliegue, informe y documentación. Si algo queda sin historia, dígalo y explique por qué.

## 5. Crear en Linear (solo tras aprobación)

- `save_issue` sin `id`: `team` (nombre del equipo), `cycle` (id completo), `project`, `title`, `assignee` (nombre
  completo tal como lo devuelve `list_users`), `estimate` (número), `priority`, `labels`, `description` en Markdown.
  Las claves `NOT-n` dentro del texto se convierten solas en enlaces.
- Después: `save_issue` con `id` y `blocks` para las dependencias (es append-only).
- Verifique: `list_issues` del ciclo con `estimate`, `assignee`, `parentId` y sume puntos por persona.
- Para corregir texto: `save_issue` con `id` y `patch` (`replace`) en vez de reenviar toda la descripción.

## 6. Lo que la API de Linear NO puede hacer (hágalo por el navegador, con su permiso)

Use el navegador del usuario (ya con sesión) y verifique con una captura:

- **Cambiar fechas de un ciclo:** Cycles del equipo → clic derecho en el ciclo → «Change cycle dates» → «Move end
  date». Linear guarda el fin a las 00:00 del día siguiente (UTC-5 se ve como el día anterior). Entre ciclos queda un
  hueco «Cycles paused».
- **Quitar ciclos futuros sobrantes:** no hay «borrar ciclo». Ajustes → Teams → Cycles → «Auto-create cycles» → 1
  (el mínimo) elimina los vacíos de más. Linear puede volver a crear uno si el siguiente ciclo empieza.
- **Fecha final del proyecto:** abrir el proyecto → Overview → clic en la fecha final.
- **Columnas del tablero que no se ven (In Progress, In Review):** no es un error: el tablero oculta columnas vacías.
  Menú de visualización (ícono de ajustes) → «Show empty columns». Es una opción por usuario; «Set default for
  everyone» la deja para todos. Si el punto azul del ícono desaparece, la vista ya coincide con la predeterminada.
- **Capacidad:** Linear muestra «% of capacity» del ciclo; más de 100 % es la alerta de sprint cargado.

## 7. Después de crear

Dé la tabla final con claves, puntos por persona, dependencias, y lo que quedó pendiente de decisión del equipo.
