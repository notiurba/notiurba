# ADR-0002 — Mecanismo de envío de correo

**Estado:** Abierta. Decisión entre el final del Sprint 2 y el Sprint 3.

## Contexto

El reporte se canaliza por correo a la entidad competente. El correo debe salir con la
identidad del ciudadano (su nombre en el remitente, su correo en copia/Reply-To) y
dejar trazabilidad (RN-07, RNF-07). Hay dos caminos y una restricción fuerte:

- **Resend** requiere un **dominio verificado**; sin él, solo envía al dueño de la cuenta.
- **Render Free bloquea los puertos SMTP 25, 465 y 587**. Resend acepta 2465 (SSL) y
  2587 (STARTTLS), que sí pasan, pero **no está probado** (ver `02-stack.md`).

## Decisión

**Pendiente.** Las dos opciones sobre la mesa:

1. **Resend** (RF-12): con dominio verificado, NotiUrba envía el correo. Mejor
   experiencia, pero depende del dominio y de que el puerto 2587 funcione en Render.
2. **Borrador en el cliente del ciudadano** (RF-13): sin dominio, NotiUrba abre un
   borrador con destinatario, asunto y cuerpo completos y el ciudadano lo envía.

El MVP puede empezar con la opción 2 y pasar a la 1 si se verifica el dominio.

## Consecuencias

- RN-18 queda abierta hasta esta decisión.
- El diseño de notificaciones (EDT 5.5) depende de este ADR.
- Decisión sobre el dominio antes del 8 de noviembre.
- Mientras tanto, no cerrar el código a una sola opción: aislar el envío detrás de una
  interfaz para poder cambiar de mecanismo.
