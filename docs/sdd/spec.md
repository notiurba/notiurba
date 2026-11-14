# Especificación — Despliegue final, respaldo y administrador real

> **Fase 1 de 5 del flujo SDD** (Especificación → Planificación → Tareas → Implementación → Pruebas).
> Aquí se define **qué** se construye y **por qué**. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (Borrador → En revisión → Aprobada) |
| Historia / ticket | NOT-133 (Linear), se deja en «Todo» por decisión de Andrés |
| Responsable | Andrés |
| Aprobada por / fecha | Andrés, 2026-11-14 |
| Última actualización | 2026-11-14 |

## 1. Contexto y problema

El MVP ya funciona en producción (Netlify + Render + Neon + Cloudinary + Resend), pero hoy:

- No hay una guía para montarlo desde cero ni para recuperarlo si algo falla.
- No hay respaldo de la base ni se comprobó que se pueda restaurar.
- Render Free se duerme y el primer usuario espera 60 a 90 s.
- El administrador de producción es `admin.dev@notiurba.local` con la clave de `ADMIN_DEV_PASSWORD`.
  Además esa variable es **obligatoria** para arrancar (`application.yml`), así que borrarla de Render
  hoy rompe el servidor.
- `main` estaba desactualizada (PR #42 de `develop` a `main`) y no hay etiqueta de versión.

## 2. Objetivo

Una versión final desplegada, etiquetada en `main`, con administrador real, respaldo comprobado y una guía que
permita repetir todo. El envío real a las entidades queda activable con un paso corto y documentado.

## 3. Usuarios y historias

- Como **equipo**, quiero una guía de despliegue, para repetirlo o recuperarlo sin depender de una sola persona.
- Como **administrador**, quiero entrar con una cuenta real y clave propia, para que producción no use datos de desarrollo.
- Como **ciudadano**, quiero que la app responda sin esperar un minuto, para poder reportar rápido.

## 4. Requisitos

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RNF-despliegue | Existir una guía con los pasos y las variables de cada servicio, sin secretos | Esencial | NOT-133 |
| RNF-respaldo | Existir un procedimiento de respaldo de Neon con restauración comprobada | Esencial | NOT-133 |
| RNF-arranque | Evitar o documentar el arranque en frío de Render Free | Esencial | NOT-133 |
| RN-admin | El administrador de producción usa cuenta real; el servidor arranca sin `ADMIN_DEV_PASSWORD` | Esencial | NOT-133 |

(Los IDs `RNF-*` son propuestos; se agregan a `docs/contexto/04-requisitos.md` al aprobar.)

## 5. Criterios de aceptación

- **CA-01** — _Dada_ la guía `docs/contexto/15-guia-de-despliegue.md`, _entonces_ explica cómo montar Netlify,
  Render, Neon, Cloudinary, Resend y el ingreso con Google desde cero, con la lista de variables y ningún secreto.
- **CA-02** — _Dado_ Neon, _cuando_ se sigue el procedimiento de respaldo, _entonces_ se obtiene una copia de la
  base y se comprobó, en una base vacía desechable, que se restaura (mismos conteos de tablas clave).
- **CA-03** — _Dado_ Render Free, _entonces_ hay un aviso periódico que evita que se duerma **y** el arranque en
  frío queda medido y escrito (con y sin aviso).
- **CA-04** — _Dado_ el administrador de producción, _entonces_ usa una cuenta real con clave nueva, y
  `ADMIN_DEV_PASSWORD` se retira de Render sin que el servidor deje de arrancar.
- **CA-05** — _Dado_ que una base nueva ya no recibe la clave de desarrollo, _cuando_ se crea desde cero,
  _entonces_ el admin sembrado no queda con una clave conocida (clave aleatoria) y la guía explica cómo crear el
  administrador real.
- **CA-06** — _Dada_ la versión final, _entonces_ está etiquetada en `main` y el commit en producción es ese.
- **CA-07** — _Dada_ la versión desplegada, _entonces_ `/api/health` responde, CORS acepta el sitio y las vistas
  previas, y el registro, el reporte con foto y el panel funcionan (lista de verificación escrita y ejecutada).
- **CA-08** — _Dado_ el envío real, _entonces_ la guía `14-activar-envio-real.md` queda enlazada desde la guía de
  despliegue con los pasos de activación (prueba con un reporte controlado y luego quitar `RESEND_PRUEBA_DESTINO`)
  y de vuelta a pruebas. **La activación la hace Andrés**; esta historia no la ejecuta.

## 6. Reglas y restricciones que aplican

- ADR-0004 (hosting gratuito), ADR-0006 (Cloudinary), ADR-0002 y ADR-0007 (correo y dominio).
- El envío real está **validado por Andrés** (13 nov 2026): no se espera respuesta del CAIC ni del docente.
- Ningún secreto en el repo ni en mis respuestas. Las claves las escribe Andrés en Render/Neon.
- Todo cambio sobre la base de producción (admin real) requiere el visto bueno de Andrés y un branch de respaldo en
  Neon antes.
- Ley 1581: el administrador real es un dato personal; su correo y nombre no se escriben en el repo.

## 7. Fuera de alcance

- Pasar a planes de pago, dominio propio para el frontend o CI/CD nuevo.
- Respaldos automáticos programados (se deja el procedimiento manual; lo automático va a `08-pendientes.md`).
- Rediseñar el panel o crear pantalla para administrar administradores.
- Activar el envío real (paso de Andrés).

## 8. Preguntas abiertas

- **Decidido (14 nov):** el aviso periódico (CA-03) es un workflow de GitHub Actions en el repo.
- **Decidido (14 nov):** el administrador real (CA-04) se obtiene **convirtiendo la cuenta sembrada** (`UPDATE` en
  Neon de correo, nombre y clave, con branch de respaldo antes).
- [PENDIENTE] ¿A nombre de quién y con qué correo queda el administrador real? — lo escribe Andrés en Neon; no se
  guarda en el repo.
- [PENDIENTE] Etiqueta: `v1.0.0` o versión previa (CA-06) — decide: Andrés, tras integrar el PR #42.

## 9. Aprobación

- [x] La especificación está completa y sin ambigüedades.
- [x] Las preguntas abiertas están resueltas o aceptadas como riesgo.
- [x] **Aprobada por el usuario** → se puede pasar a `plan.md`.
