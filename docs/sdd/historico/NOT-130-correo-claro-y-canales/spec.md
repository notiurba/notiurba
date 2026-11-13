# Especificación — Correo formal, canales por entidad y vista previa del mensaje

> **Fase 1 de 5 del flujo SDD** (Especificación → Planificación → Tareas → Implementación → Pruebas).
> Aquí se define **qué** se construye y **por qué**. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (Borrador → En revisión → Aprobada) |
| Historia / ticket | NOT-130 (Linear) |
| Responsable | Andrés (Figma, backend y frontend) |
| Aprobada por / fecha | Andrés, 2026-11-12; modificada y vuelta a aprobar el mismo día con el atajo para gas, energía y aseo (opción A, sección 8) |
| Última actualización | 2026-11-12 |

## 1. Contexto y problema

La función central de NotiUrba es **hacer llegar el reporte a la entidad correspondiente**. Hoy ese último paso falla de cuatro formas:

1. **El correo es confuso.** El del 9 de nov muestra la matrícula siempre («No aportado»), la fecha en texto crudo de Java, coordenadas con 15 decimales, filas vacías, fotos solo como enlaces, el código y el nombre de NotiUrba, y una dependencia que el CAIC no necesita.
2. **Los correos de varias empresas no funcionan** (evidencia del correo de Andrés, 29 sep a 14 oct 2026; ver Anexo A): Celsia rechaza los mensajes; Gases de Occidente exige pulsar un enlace en 24 horas o descarta la solicitud; Veolia no respondió; y el repo tenía mal el correo de Centroaguas (el autorizado para PQR es `info@centroaguas.com`).
3. **El ciudadano solo ve un canal alterno.** Las entidades tienen más canales verificables (formulario PQRSDF de la Alcaldía, PQRSDF anónimo, líneas, WhatsApp, oficinas) y la base solo guarda uno por categoría.
4. **El ciudadano no ve lo que se enviará en su nombre** antes de confirmar.
5. **Para gas, energía y aseo el ciudadano llena 4 pasos y termina en otro canal**, porque esas empresas no reciben correo útil. Es trabajo perdido y molesto.

Esto sostiene la función central; sin un mensaje claro y un canal que responda, el reporte se pierde.

## 2. Objetivo

Que cada reporte por correo llegue con un **mensaje formal y completo** por un canal con evidencia de que responde y que el ciudadano **vea el mensaje antes de enviarlo**; que, si el correo rebota, tenga **otros canales oficiales y su mensaje listo para copiar**; y que, para las entidades **sin correo útil**, llegue a su canal oficial **justo después de elegir la categoría**, sin llenar más pasos.

## 3. Usuarios y historias

- Como **ciudadano**, quiero ver el mensaje tal como se enviará y poder corregirlo, para presentar bien mi inconformidad.
- Como **ciudadano** que reporta gas, energía o aseo, quiero ver el canal oficial de la entidad apenas elijo la categoría, para resolverlo rápido sin llenar pasos que no se usarán.
- Como **ciudadano** cuyo correo rebotó, quiero ver los otros canales oficiales y copiar mi mensaje, para no volver a escribirlo.
- Como **funcionario de la entidad**, quiero un correo formal, ordenado y con los datos que pide mi oficina, para radicarlo sin pedir más información.
- Como **equipo**, quiero que cada canal guarde su finalidad, horario y fuente, para saber de dónde sale cada dato.

## 4. Requisitos

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RF-14 *(modifica)* | Armar el correo con la plantilla del grupo de la categoría (A, B o C; Anexo B), en tono formal y en primera persona del ciudadano. Sin código de seguimiento, sin mención a NotiUrba y sin dependencia. | Esencial | NOT-130; Andrés 12 nov |
| RF-26 *(nuevo)* | Omitir toda fila opcional vacía; mostrar la fecha como «9 de noviembre de 2026, 10:07 a. m.» (hora de Colombia) y las coordenadas con 5 decimales. | Esencial | NOT-130 |
| RF-27 *(nuevo)* | Mostrar hasta 3 miniaturas con enlace y texto alternativo; sin fotos, omitir la sección. Incluir la versión en texto plano con los mismos datos. | Esencial | NOT-130 |
| RF-28 *(nuevo)* | Enviar por correo solo a los destinos con evidencia de que responden (CAIC y Centroaguas). Las demás entidades (gas, energía y aseo) siguen el atajo de RF-32. | Esencial | Anexo A |
| RF-17 *(modifica)* | Mostrar **todos** los canales oficiales verificados de la entidad, ordenados, con su finalidad (radicar PQR, reportar daño, anónimo, consultar estado, emergencia), horario y enlace o número. | Esencial | NOT-130 |
| RF-32 *(nuevo)* | **Atajo:** si la ruta de la categoría no permite correo, al tocar «Continuar» en el paso 1 mostrar «Cómo reportar a {entidad}» con sus canales y acciones directas (abrir WhatsApp, llamar, abrir formulario). No se piden ubicación, descripción ni autorización, y **no se crea ningún reporte**. | Esencial | Andrés 12 nov (opción A) |
| RF-29 *(nuevo)* | Ofrecer «Copiar mi mensaje» (texto de la plantilla) cuando un correo enviado rebotó o falló. | Esencial | Andrés 12 nov |
| RF-30 *(nuevo)* | En el paso «Revise su reporte», mostrar la vista previa de solo lectura del mensaje, con «Editar» hacia el paso donde se captura cada dato. La vista previa no guarda ni envía nada. | Esencial | Andrés 12 nov |
| RF-31 *(nuevo)* | Guardar por canal su finalidad, orden, horario, fuente y fecha de verificación. | Esencial | NOT-130 |
| RN-12 *(modifica)* | El asunto y el cuerpo ya no indican la dependencia: el CAIC redirige según lo que pida el ciudadano. | Esencial | Andrés 12 nov |
| RN-17 *(modifica)* | Asunto formal sin código: «Petición ciudadana – [categoría] – [barrio y comuna]». El código NU-… queda solo en la app y la base. | Esencial | Andrés 12 nov |
| RN-15 *(modifica)* | Si una empresa no acepta correo, NotiUrba muestra su canal oficial **al elegir la categoría** y no registra el reporte (antes: se registraba y se marcaba «no enviado por correo»). | Esencial | Andrés 12 nov |
| RF-04, RF-12, RF-15, RF-16, RN-07, RN-13, RN-16, RN-18 | Se conservan. RN-07: se mantiene la constancia de autorización en una línea. | — | — |

## 5. Criterios de aceptación

**Correo**
- **CA-01** — _Dado_ un reporte de la categoría 1, _cuando_ se envía, _entonces_ el correo está en tono formal, trae nombre, tipo y número de documento, dirección, teléfono y correo del ciudadano, hechos, ubicación, fecha, evidencia, solicitud y firma, y **no** contiene «NotiUrba», el código NU-… ni la dependencia.
- **CA-02** — _Dado_ un reporte de cada grupo con correo (A: categorías 1, 2, 3, 4, 5 y 8; B: 6, 7, 9 y 10; C: 11), _entonces_ el correo usa la plantilla de su grupo. La plantilla C queda lista para las demás empresas si algún día aceptan correo.
- **CA-03** — _Dado_ un dato opcional vacío (dirección del incidente, teléfono, matrícula), _entonces_ su fila no aparece y no dice «No aportado».
- **CA-04** — _Dadas_ la fecha y las coordenadas, _entonces_ se leen «9 de noviembre de 2026, 10:07 a. m.» y `4.08672, -76.19500`.
- **CA-05** — _Dado_ un reporte con 2 fotos, _entonces_ salen 2 miniaturas con enlace; _sin fotos_, la sección no aparece.
- **CA-06** — _Dado_ un reporte del grupo C con matrícula o cuenta, _entonces_ empieza «en calidad de usuario del servicio… (matrícula o cuenta N.º …)»; _sin ella_, empieza «como ciudadano».
- **CA-07** — _Dado_ el correo abierto en Gmail del celular y del computador, _entonces_ no hay desplazamiento horizontal, y la versión en texto plano trae los mismos datos.

**Canales**
- **CA-08** — _Dado_ un reporte de la Alcaldía o de acueducto, _cuando_ se envía, _entonces_ va a `servicioalciudadano@tulua.gov.co` o a `info@centroaguas.com`.
- **CA-09** — _Dado_ un ciudadano que elige gas, energía o aseo en el paso 1, _cuando_ toca «Continuar», _entonces_ ve «Cómo reportar a {entidad}» con los canales de la entidad y **no** pasa por ubicación, detalles ni confirmación.
- **CA-09b** — _Dada_ la pantalla del atajo, _entonces_ no se crea reporte, no se pide autorización de datos y no se envía nada a nadie.
- **CA-09c** — _Dado_ cada canal de la pantalla del atajo, _entonces_ tiene su acción directa: WhatsApp abre `https://wa.me/57…`, el teléfono abre `tel:`, la web abre su enlace `https` en otra pestaña y la oficina muestra dirección y horario. El canal más rápido para ese caso va primero.
- **CA-10** — _Dados_ los canales de una entidad, _entonces_ aparecen ordenados, con finalidad y horario; los de la Alcaldía incluyen formulario PQRSDF, PQRSDF anónimo, consulta de estado, agente virtual, presencial y teléfono.
- **CA-10b** — _Dado_ un canal sin verificar (hoy el WhatsApp de Gases de Occidente), _entonces_ está guardado como inactivo, no se muestra y se activa con un cambio de datos, sin cambiar código.
- **CA-11** — _Dado_ un correo que rebota (webhook de NOT-131), _entonces_ el ciudadano ve los mismos canales y «Copiar mi mensaje».
- **CA-12** — _Dada_ una emergencia de gas, _entonces_ la app indica la línea 164 y no envía correo (RN-16).

**Vista previa**
- **CA-13** — _Dado_ el paso «Revise su reporte», _entonces_ el ciudadano ve el mensaje tal como se enviará; _al tocar_ «Editar» en una sección, vuelve al paso de ese dato y, al regresar, ve el cambio.
- **CA-14** — _Dada_ la vista previa, _entonces_ no crea reporte, no sube fotos y no envía correo.
- **CA-15** — _Dado_ el mismo reporte, _entonces_ el texto de la vista previa y el del correo enviado son idénticos (misma plantilla), salvo la hora del registro y los enlaces de las fotos: la vista previa dice cuántas fotos van, pero sus enlaces solo existen al subirlas. *(Precisado el 12 nov al implementar.)*
- **CA-16** — Caso de error: _dados_ datos no válidos o sin sesión, _entonces_ responde 400 con `{codigo, mensaje, campo}` o 401, igual que el resto de la API.

**Datos**
- **CA-17** — _Dada_ la migración V21, _entonces_ aplica sobre la base con los reportes existentes sin perder datos; los canales con evidencia en contra quedan **inactivos** con el motivo en `detalle`, no borrados.
- **CA-18** — _Dado_ cada canal, _entonces_ tiene `finalidad` y `orden`; los canales **activos de una entidad** tienen además `fuente` y `verificado_en`. *(Precisado el 12 nov: los correos de referencia de las dependencias y los canales inactivos no se verificaron.)*

## 6. Reglas y restricciones que aplican

- Reglas de negocio: RN-07, RN-13, RN-15, RN-16, RN-18 (`docs/contexto/05-reglas-de-negocio.md`). Se modifican RN-12 y RN-17.
- Decisiones cerradas: ADR-0002 (envío con Resend), ADR-0008 (autorización única).
- Ley 1581 de 2012: la vista previa muestra solo los datos del propio ciudadano y no los guarda; el correo conserva la constancia de autorización; el atajo no recoge ni envía datos personales.
- **Decisión de alcance (Andrés, 12 nov):** las categorías 12, 13 y 14 **no se guardan en NotiUrba** al usar el atajo; por eso no aparecen en el mapa de calor ni en las estadísticas del panel. Se acepta como límite del MVP (esas entidades no reciben correo y nadie haría el seguimiento manual).
- El backend sigue aceptando reportes de esas categorías por la API (compatibilidad); la interfaz ya no los ofrece.
- Una sola ruta de envío por reporte: no se mandan copias a varias direcciones de la misma entidad.
- `RESEND_PRUEBA_DESTINO` sigue puesta: **esta historia no activa el envío real** (NOT-131 y NOT-133).
- Interfaz y correo en español, tono formal.
- **Orden de trabajo** (decidido por Andrés el 12 nov): primero **Figma** (se valida con Andrés), después el **backend** siguiendo esta spec y por último el **frontend**, con pruebas funcionales al final.
- La tarjeta de «Revise su reporte» (`PasoConfirmacion`, pantalla de Katherine) la implementan Andrés y Claude junto con el Figma y el backend, porque el cambio es pequeño. Se le avisa a Katherine al integrar.

## 7. Fuera de alcance

- Activar el envío real y el webhook de rebotes (NOT-131, NOT-133).
- Texto libre editable en el mensaje (se evalúa después de usar la vista previa).
- Enviar formularios web de las entidades en nombre del ciudadano.
- Validar los canales con las empresas (15 días): se usa evidencia pública y el correo de Andrés.
- «Guardar también en NotiUrba» para gas, energía y aseo (opción B): se evaluó y se descartó el 12 nov.
- Quitar las categorías 12 a 14 del catálogo o de la API: solo cambia lo que ve el ciudadano.

## 8. Preguntas abiertas

**Decididas por Andrés el 12 nov 2026**
- Quién implementa la tarjeta del frontend: **Andrés y Claude**, junto con Figma y backend (ver sección 6).
- Texto de las plantillas (Anexo B), con la frase «en ejercicio del derecho de petición (Constitución Política, art. 23)»: **aprobado**.
- Veolia: **sin correo**; el medio preferido es WhatsApp.
- **WhatsApp de Veolia (317 372 5078): verificado** por Andrés el 12 nov con capturas. Es la cuenta verificada de Veolia Colombia, un bot general: se elige «Dto Palmira Buga Tuluá» → «Residuos» → «Aseo Valle». Queda **activo** y con esas instrucciones.
- **Atajo, opción A:** gas, energía y aseo van directo a sus canales después del paso 1; no se registra el reporte (sin «Guardar también en NotiUrba»). Razón: esas entidades no reciben correo y nadie haría doble seguimiento manual.

**Abiertas**
- [PENDIENTE] La categoría 15 («Solicitud de información») está activa pero no tiene ruta en `configuracion_enrutamiento`: `POST /api/reportes` la acepta y la vista previa responde 400 «Categoría sin ruta disponible». Viene de antes de esta historia. Opciones: desactivarla o darle ruta al CAIC. Decide: el equipo.
- [PENDIENTE] El WhatsApp de Gases de Occidente (315 665 8536) está en el repo, pero no en su sitio oficial. No bloquea: se guarda inactivo hasta verificarlo. Decide: Andrés.

## 9. Aprobación

- [x] La especificación está completa y sin ambigüedades.
- [x] Las preguntas abiertas están resueltas o aceptadas como riesgo (queda la verificación del WhatsApp de Gases de Occidente, que no bloquea).
- [x] **Aprobada por el usuario** (2026-11-12), incluida la modificación del atajo (opción A) → se puede pasar a `plan.md`.

---

## Anexo A — Canales y evidencia (12 nov 2026)

| Entidad | Correo | Decisión | Evidencia |
| --- | --- | --- | --- |
| **Alcaldía (CAIC)** | `servicioalciudadano@tulua.gov.co` | ✅ Correo | Respondió al día siguiente con radicado E-.29570 y pidió nombre, cédula, dirección y teléfono. |
| **Centroaguas** | `info@centroaguas.com` | ✅ Correo | Su acto GAC-003660-26 (13 oct) dice que es el único autorizado para PQR; radicado CO260929-3786; respondió en 14 días. `notificacionespqr@` solo es el remitente de sus respuestas. |
| **Gases de Occidente** | `inquietudes@gdo.com.co` | ❌ | Responde con un aviso automático: hay que pulsar «Confirmar envío» en 24 h o no se recibe. |
| **Celsia** | `servicioalcliente@celsia.com` | ❌ | Rechazo 550 5.7.1: la cuenta no recibe correos; remite a Zona Clientes. |
| **Veolia Tuluá** | `pqr.tulua.co@veolia.com` | ❌ (decidido) | Aceptó el correo del 5 oct y no respondió en 38 días. El repo tenía `pqr.tulua@veolia.com`, que no coincide con su sitio. |

| Entidad | Otros canales (finalidad) |
| --- | --- |
| **Alcaldía** | Formulario PQRSDF (tulua.gov.co/6404) · PQRSDF anónimo (tulua.tramiter.co/documentos/seleccionar-tramite-anonimo) · consultar estado (tulua.tramiter.co/documentos/consultar-tramite-web) · agente virtual 24 h · Carrera 25 #25-04, lunes a jueves 7:30–12:00 y 1:30–6:00, viernes hasta las 5:00 · (602) 233 9300 |
| **Centroaguas** | Aplicativo PQRS web · línea 231 7300 (3 = PQR, 116 = daños) · Calle 25 #32A-31, lunes a viernes 7:30–11:30 y 1:30–4:00 |
| **Celsia** | Zona Clientes → PQR (clientes.celsia.com) · línea 01 8000 112 115 · WhatsApp 310 390 3468 (daños) |
| **Gases de Occidente** | Formulario «Registro de PQR» (gdo.com.co/Paginas/Registro-de-pqrs.aspx) · línea 164 (emergencias) · contact center 24/7: (602) 418 7333 y 01 8000 52 88 88 |
| **Veolia Tuluá** | WhatsApp 317 372 5078 (**verificado 12 nov**: «Dto Palmira Buga Tuluá» → «Residuos» → «Aseo Valle») · línea 01 8000 95 00 96 · Carrera 26 #26-20 local 104, lunes a viernes 8:00–16:00 · formulario «Contáctanos» (valle.veolia.co/contactanos) |

**Canal que se muestra primero en el atajo:** gas → formulario «Registro de PQR» (la línea 164 si es emergencia); energía → Zona Clientes → PQR (WhatsApp para daños); aseo → WhatsApp de Veolia.

## Anexo B — Plantillas (texto aprobado por Andrés el 12 nov 2026)

Asunto: **A** «Petición ciudadana – {categoría} – {barrio}, Comuna {n}» · **B** «Solicitud ciudadana – {categoría}» · **C** «Petición ciudadana – {servicio}».

**A — Problema en un lugar (categorías 1, 2, 3, 4, 5, 8)**

> Respetados señores:
>
> Yo, {nombre completo}, con {tipo de documento} No. {número}, en ejercicio del derecho de petición (Constitución Política, art. 23), me dirijo a ustedes para informar la siguiente situación:
>
> **Hechos:** {descripción}
> **Ubicación:** {dirección}, barrio {barrio}, Comuna {n} · {enlace al mapa} · {coordenadas}
> **Fecha del registro:** {fecha}
> **Evidencia:** {n} fotografías (enlaces).
>
> Solicito atender esta situación y comunicarme el trámite adelantado y el número de radicado asignado.
>
> Autorizo el uso de mis datos para el trámite de esta petición. Recibiré respuesta en {correo} · {teléfono} · {dirección de correspondencia}.
>
> Atentamente,
> {nombre completo} · {tipo de documento} {número}

**B — Consulta o solicitud (categorías 6, 7, 9, 10):** igual que A, con «**Solicitud:** {descripción}» en lugar de «Hechos», y sin ubicación ni evidencia cuando no las hay.

**C — Falla de servicio público (hoy solo la categoría 11, Centroaguas, envía correo):** dirigida a {empresa}. Con matrícula o cuenta: «…en calidad de usuario del servicio de {servicio} (matrícula o cuenta No. {X}), me dirijo a ustedes…». Sin ella: «…como ciudadano, me dirijo a ustedes…». El resto igual que A. La solicitud es general: «Solicito atender esta situación y comunicarme el trámite adelantado y el número de radicado asignado.»

## Anexo C — Cambios a documentos existentes al aprobar

`docs/contexto/04` (RF-14, RF-17 y nuevos RF-26 a RF-32), `05` (RN-12, RN-15, RN-17), `07` (canales), `10` y `11` (modelo y contrato), y los externos: draw.io del ER, Sheet del ER y Google Doc (ver skill `cierre-de-historia`).
