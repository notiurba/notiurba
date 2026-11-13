# Especificación — <nombre de la funcionalidad>

> **Fase 1 de 5 del flujo SDD** (Especificación → Planificación → Tareas → Implementación → Pruebas).
> Aquí se define **qué** se construye y **por qué**. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.

| Campo | Valor |
| --- | --- |
| Estado | **Borrador** (Borrador → En revisión → Aprobada) |
| Historia / ticket | NOT-__ (Linear) |
| Responsable | _(Andrés / Katherine / Amelia)_ |
| Aprobada por / fecha | _(pendiente)_ |
| Última actualización | 2026-11-11 |

## 1. Contexto y problema

_¿Qué problema del ciudadano o del administrador resuelve esto? ¿Qué parte de la
función central lo justifica («ubicar el reporte por comuna y hacerlo llegar a la
entidad correspondiente»)? Si no la sostiene, no entra al MVP (ver
`docs/contexto/01-vision-y-alcance.md`)._

## 2. Objetivo

_Una o dos frases: qué debe ser verdad cuando esto esté terminado._

## 3. Usuarios y historias

- Como **ciudadano / administrador**, quiero ___, para ___.

## 4. Requisitos

Reutiliza los IDs existentes (`RF-xx`, `RNF-xx`, `RN-xx`; ver `docs/contexto/04` y `05`).
Si hace falta uno nuevo, se propone aquí y se agrega a `docs/contexto/` al aprobar.

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RF-__ | ___ | Esencial / Condicional | ___ |

## 5. Criterios de aceptación

Formato Dado-Cuando-Entonces. Cada criterio debe poder verificarse con una prueba
(se enlaza desde `tasks.md`).

- **CA-01** — _Dado_ ___, _cuando_ ___, _entonces_ ___.
- **CA-02** — Caso de error: _dado_ datos no válidos ___, _entonces_ ___.

## 6. Reglas y restricciones que aplican

- Reglas de negocio: RN-__ (`docs/contexto/05-reglas-de-negocio.md`).
- Decisiones cerradas (ADR) que condicionan: ADR-____.
- Ley 1581 de 2012 / datos personales: _¿toca datos personales? ¿cuáles?_
- Interfaz en español, tono claro.

## 7. Fuera de alcance

- ___ (qué **no** hace esta funcionalidad, para evitar crecimiento de alcance).

## 8. Preguntas abiertas

Marcar como **[PENDIENTE]** lo que no está decidido. No se cierra sin el equipo.

- [PENDIENTE] ___ — decide: ___.

## 9. Aprobación

- [ ] La especificación está completa y sin ambigüedades.
- [ ] Las preguntas abiertas están resueltas o aceptadas como riesgo.
- [ ] **Aprobada por el usuario** → se puede pasar a `plan.md`.
