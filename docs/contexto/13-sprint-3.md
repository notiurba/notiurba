# Sprint 3 (último): 10 al 29 de noviembre de 2026

Reemplaza la guía antigua `sprint-3-historias.md` (13 historias, 60 puntos), que se escribió antes
de que se terminara buena parte del trabajo. Las historias ya están creadas en Linear (ciclo 3,
proyecto «NotiUrba - mvp»). El proyecto cierra el **30 de noviembre** en Linear y la materia el
13 de diciembre.

**Meta del sprint:** pulir la app y completar el MVP: mapas del panel, correo claro, pruebas,
usabilidad y accesibilidad, despliegue final e informe. No hay más sprints después de este.

## Reparto: 14 historias, 65 puntos

El Sprint 2 cerró con 55 puntos; este va cargado (Linear muestra ~309 % de capacidad). Si hay que
recortar, lo más fácil es parte del manual de NOT-142 y bajar
NOT-137.

### Andrés — backend y GIS (21)

| Linear | Historia | Pts |
| --- | --- | --- |
| NOT-129 | [EDT 8.3] API del mapa de reportes, del mapa de calor y de las estadísticas (Urgente) | 5 |
| NOT-130 | [Mejora] Correo claro y completo en la entidad (hecho 12 nov; en PR) | 5 |
| NOT-131 | [Mejora] Envío real a las entidades y marcar rebotes (PR #38 integrado 14 nov; abierta: ajustes y QA) | 3 |
| NOT-132 | [Mejora] Registro e ingreso rápidos y sin abusos (hecho 13 nov; en PR) | 5 |
| NOT-133 | [EDT 10.3] Versión final desplegada y respaldo de datos | 3 |

### Katherine — frontend (21)

| Linear | Historia | Pts |
| --- | --- | --- |
| NOT-134 | [Mejora] Instalar la app en el celular (PWA) | 3 |
| NOT-135 | [EDT 8.3] Reportes en el mapa con datos reales | 5 |
| NOT-136 | [EDT 8.4] Mapa de calor, puntos críticos y estadísticas | 5 |
| NOT-137 | [Mejora] Corregir la app: servidor despertando, usabilidad, tono y accesibilidad | 8 |

### Luz Amelia — UX/UI y pruebas (23)

| Linear | Historia | Pts |
| --- | --- | --- |
| NOT-138 | [Mejora] Interfaz más amigable: usabilidad, tono y voz | 5 |
| NOT-139 | [Pruebas] Auditar la accesibilidad (WCAG 2.1 AA, TalkBack) | 5 |
| NOT-140 | [EDT 9.3] Probar el mapa y el panel | 3 |
| NOT-141 | [EDT 9.5] Pruebas de punta a punta, revisión del correo y Postman | 5 |
| NOT-142 | [EDT 10.4] Informe final y manuales | 5 |

## Dependencias (ya cargadas en Linear como «bloquea»)

- NOT-129 → NOT-135, NOT-136 (la API del mapa se entrega en la primera semana).
- NOT-138 y NOT-139 → NOT-137 (los hallazgos de Amelia alimentan las correcciones de Katherine).
- NOT-130 → NOT-131 (el correo claro va antes del envío real).
- NOT-135 y NOT-136 → NOT-140.

## Decisiones tomadas para este sprint

- No se repiten pruebas con personas: el design thinking (15 encuestados y 10 evaluadores) ya
  dijo qué quieren. La historia de Amelia revisa usabilidad, **tono y voz** y accesibilidad.
- Barrios y comunas provisionales: el docente aceptó la limitación; no hay historia de
  validación con Planeación.
- Hasta 3 fotos por reporte (ya implementado).
- El correo a las entidades sigue siendo formal; el tono cercano es de la app y de los correos al
  ciudadano.
- `RESEND_PRUEBA_DESTINO` sigue puesta hasta que Andrés active el envío real (ya no se espera al CAIC ni al docente).

## Por decidir (lo decide el equipo, no una IA)

- Límite diario de reportes por ciudadano, número de intentos de login antes de bloquear y
  distancia para considerar un reporte duplicado (NOT-132).
- Si el correo al ciudadano por cambio de estado entra al MVP (no se verificó en el código).

## Plantilla de historia (formato de los Sprint 1 y 2)

Título: `[EDT x.y] Verbo + lo que logra el usuario`. Si no hay código EDT: `[Mejora]` o `[Pruebas]`.
Etiqueta: Feature, Improvement, Design o Docs. Prioridad: Urgente, Alta o Media.

```
**Historia**
Como <rol>, quiero <qué>, para <para qué>.

**Decisiones tomadas**
* ...

**Criterios de aceptación**
* Dado <contexto>, cuando <acción>, entonces <resultado>.

**Dependencia:** ...
**Sin resolver (fuera del equipo):** ...
**Fuentes:** ...
```

Los Sprint 1 y 2 también usaron «**Reparto:** Katherine 2 pts, Luz 1 pt» cuando una historia se
comparte, y subtareas con «Qué hacer / Listo cuando / Horas». Para el Sprint 3 Andrés pidió **no
usar subtareas**.

## Linear: ajustes hechos por el navegador (la API no los permite)

- Ciclo 3: se acortó a terminar el 29 de noviembre (menú del ciclo → «Change cycle dates»).
- Se bajó «Auto-create cycles» a 1 para que no quedaran los ciclos 4 y 5.
- Tablero del proyecto: «Show empty columns» activo para ver In Progress e In Review vacías.
- Fecha final del proyecto: 30 de noviembre.
