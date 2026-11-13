# Especificación — Registro e ingreso rápidos y sin abusos

> **Fase 1 de 5 del flujo SDD** (Especificación → Planificación → Tareas → Implementación → Pruebas).
> Aquí se define **qué** se construye y **por qué**. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (Borrador → En revisión → Aprobada) |
| Historia / ticket | NOT-132 (Linear), 5 puntos |
| Responsable | Andrés (Figma, backend y frontend) |
| Aprobada por / fecha | Andrés, 2026-11-13, con dos cambios: «reciente» va de 1 a 30 días; las pantallas nuevas entran en el alcance (orden Figma → backend → frontend) |
| Última actualización | 2026-11-13 (ajustada tras la revisión del plan: mensajes claros del ingreso, límite por cuenta, distancias más flexibles) |

## 1. Contexto y problema

La función central de NotiUrba es que el ciudadano **cree un reporte y llegue a la entidad correcta**. Para eso primero tiene que entrar a la app, y hoy esa puerta falla de cuatro formas:

1. **Registrarse tarda cerca de 10 segundos.** En la misma petición se cifra la contraseña (BCrypt) y se envía el correo de verificación por Resend, y la petición espera la respuesta de Resend (`RegistroServicio.registrar` llama a `VerificacionServicio.emitirYEnviar` dentro de la transacción). RNF-04 pide que el correo salga en segundo plano.
2. **«Continuar con Google» falló en un celular** con «No pudimos comunicarnos con el servidor» (Amelia lo reproduce). Ese mensaje sale en el frontend cuando el navegador no recibe respuesta; el backend no deja rastro de la causa.
3. **Nada frena a quien adivina contraseñas.** `POST /api/auth/login` acepta intentos sin límite.
4. **Nada frena a quien quiere saturar el sistema.** Una persona puede enviar reportes sin tope (el correo de pruebas y la cuota de Resend son de 100 al día) o repetir un reporte que ya existe en ese mismo lugar —lo más común es que otras personas ya hayan reportado lo mismo—.

Esto sostiene la función central: un reporte solo sirve si la persona entra rápido, y el envío a las entidades solo es confiable si no se puede inundar ni repetir.

## 2. Objetivo

Que **registrarse responda en 2 segundos o menos** sin esperar el correo, que **un fallo de Google deje su causa registrada**, y que el sistema **limite los intentos de login, los reportes por día y los reportes repetidos**, siempre con un mensaje claro en el formato de error del contrato.

## 3. Usuarios y historias

- Como **ciudadano**, quiero registrarme y que la app responda enseguida, para empezar a reportar sin esperar.
- Como **ciudadano**, quiero que el correo de verificación llegue aunque la app no lo haya esperado, y poder pedir otro si no llega, para no quedarme sin cuenta.
- Como **equipo (Amelia, Katherine, Andrés)**, quiero que un fallo de «Continuar con Google» quede en los registros con su causa, para saber qué arreglar sin ver el celular de la persona.
- Como **ciudadano**, quiero que mi cuenta se proteja de quien adivina mi contraseña, y entender cuánto debo esperar si me equivoco, para no quedarme sin saber qué pasó.
- Como **equipo**, quiero un tope diario de reportes configurable, para que nadie sature el correo o la cuota de Resend, pero sin estorbar las pruebas.
- Como **ciudadano**, quiero que me avisen si ya reporté lo mismo en el mismo lugar, para no enviar dos veces la misma petición.

## 4. Requisitos

Reutiliza IDs existentes; los nuevos se proponen aquí y se agregan a `docs/contexto/04-requisitos.md` al aprobar.

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RNF-04 (ajuste) | Responder el registro en ≤ 2 s sin esperar el envío del correo de verificación; el envío ocurre en segundo plano. | Esencial | RNF-04, NOT-132 |
| RF-01 (ajuste) | Si el correo de verificación falla, el registro **no** falla; la cuenta queda creada y el ciudadano puede pedir otro enlace (`reenviar-verificacion`). | Esencial | RF-01 |
| RF-33 (nuevo) | Registrar en el log la causa de un fallo de «Continuar con Google» (paso que falló y tipo de error) **sin** datos personales: sin correo, nombre, documento ni token. | Esencial | NOT-132 |
| RF-34 (nuevo) | Limitar el login: **3 intentos fallidos en una ventana de 5 minutos** bloquean nuevos intentos de ese correo durante **5 minutos**. | Esencial | NOT-132, RNF-03 |
| RF-35 (nuevo) | Limitar los reportes: **2 por cuenta por día** (cada cuenta tiene su propio cupo; no es un tope general de la app), valor configurable sin tocar el código. Al superarlo, el reporte no se guarda y se explica cuándo podrá volver a reportar. | Esencial | NOT-132 |
| RF-36 (nuevo) | Avisar de un posible duplicado: en los **últimos 30 días** (valor inicial, ajustable de 1 a 30) ya existe un reporte de la **misma categoría**, a menos de la distancia de su categoría (mínimo **5 m**) y aún sin resolver, **sea de la misma cuenta o de otra persona** (lo más común es que sean de otras cuentas). El aviso muestra solo los **datos base**: fecha, estado, cuántos hay y un **resumen de la descripción**; nunca el nombre, el correo, la dirección ni las fotos, y el código solo si es de la propia persona. | Esencial | NOT-132, R.06; Andrés, 13 nov 2026 |
| RN-22 (nueva) | La distancia de duplicado depende de la categoría y es configurable. Nunca es menor a 5 m. | Esencial | Andrés, 13 nov 2026 |
| RF-38 (nuevo) | Que cada situación del ingreso (datos incorrectos, bloqueo, correo sin confirmar, sin internet, servicio caído, fallo con Google) muestre un mensaje **claro, sin lenguaje técnico**, que diga qué pasó y qué puede hacer la persona. | Esencial | Andrés, 13 nov 2026 |
| RF-39 (nuevo) | Si en ese lugar y categoría ya hay un reporte **«Resuelto»** (de la misma cuenta o de otra) dentro de los días de «reciente», se permite reportar —el problema pudo volver— pero se **exige al menos una foto** como evidencia. Las pantallas de fotos piden **fotografías recientes**. | Esencial | Andrés, 13 nov 2026 |
| RF-37 (nuevo) | Dejar que el **administrador consulte y ajuste** los parámetros: intentos de login, ventana, bloqueo, reportes por día, ventana de «reciente» y la distancia de duplicado de cada categoría. El cambio aplica de inmediato y queda registrado quién lo hizo y cuándo. | Esencial | Andrés, 13 nov 2026 |
| RN-24 (nueva) | Ningún mensaje para el ciudadano muestra códigos, números de estado HTTP ni palabras técnicas («servidor», «token», «API», «error 500», «CORS»). Dice qué pasó y qué hacer. | Esencial | Andrés, 13 nov 2026 |
| RN-23 (nueva) | Los valores de límites viven en la base, no en el código, y solo el administrador los cambia dentro de rangos seguros (CA-26 a CA-28). | Esencial | NOT-132 |

## 5. Criterios de aceptación

Formato Dado-Cuando-Entonces. Cada criterio se enlaza desde `tasks.md` con una prueba.

**Registro rápido**

- **CA-01** — _Dado_ un registro con datos válidos, _cuando_ el servicio de correo tarda o falla, _entonces_ la respuesta llega en ≤ 2 s con la misma respuesta de hoy (`{ correo, mensaje }`) y la cuenta queda creada sin verificar.
- **CA-02** — _Dado_ un registro válido, _cuando_ se confirma la transacción, _entonces_ el correo de verificación se envía en segundo plano **después** de guardar la cuenta (nunca antes: no sale un enlace de una cuenta que no se creó).
- **CA-03** — _Dado_ que el envío del correo falló, _cuando_ el ciudadano pide `POST /api/auth/reenviar-verificacion`, _entonces_ recibe un enlace nuevo (con el límite de un reenvío por minuto que ya existe).
- **CA-04** — Caso de error: _dado_ datos no válidos o repetidos (correo o documento), _entonces_ la respuesta y los códigos de error siguen igual que hoy y no se envía correo.

**Google**

- **CA-05** — _Dado_ que falla «Continuar con Google» en el backend (token inválido, error de base de datos, rol o proveedor faltante), _cuando_ ocurre, _entonces_ el log guarda el paso y el tipo de error, y no guarda correo, nombre, documento, token ni `sub` de Google.
- **CA-06** — _Dado_ un token de Google inválido, _entonces_ la respuesta sigue siendo 401 `NO_AUTENTICADO` con el mensaje de hoy.
- **CA-07** — Se documenta la causa real del fallo del celular (o, si no se reproduce, qué se descartó y cómo se confirma cuando Amelia lo vuelva a probar). Ver pregunta abierta 1.

**Límite de intentos de login**

- **CA-08** — _Dado_ un correo con 2 intentos fallidos en los últimos 5 minutos, _cuando_ falla un tercero, _entonces_ se responde 429 `INTENTOS_EXCEDIDOS` con un mensaje que dice cuántos minutos esperar; los siguientes intentos de ese correo, **aunque lleven la contraseña correcta**, responden lo mismo hasta que pasen 5 minutos.
- **CA-09** — _Dado_ un correo con 1 o 2 intentos fallidos, _cuando_ ingresa bien, _entonces_ entra y su contador vuelve a cero.
- **CA-10** — _Dado_ que pasaron 5 minutos de bloqueo, _cuando_ intenta de nuevo, _entonces_ puede ingresar y el contador empieza de cero.
- **CA-11** — _Dado_ un correo que **no existe**, _cuando_ falla 3 veces, _entonces_ recibe el mismo bloqueo y el mismo mensaje que uno que existe (no se revela qué cuentas existen).
- **CA-12** — _Dado_ un intento fallido, _entonces_ el mensaje no dice si el correo existe ni cuál de los dos datos falló.
- **CA-40** — _Dado_ un correo al que le queda **un solo intento**, _cuando_ falla, _entonces_ el aviso de datos incorrectos agrega «Le queda 1 intento antes de un bloqueo de N minutos». Pasa **igual** con correos que existen y que no, y con cuentas solo-Google, así no revela cuentas. Con más de 1 intento restante no se avisa.
- **CA-13** — Los intentos fallidos de un correo no bloquean a otro correo.

**Mensajes claros en el ingreso**

Cada situación tiene su mensaje, escrito en la voz de la app (usted) y sin términos técnicos. Mensajes y botones se afinan en Figma (F-2); estos son el punto de partida:

| Situación | Qué ve la persona | Qué puede hacer |
| --- | --- | --- |
| Datos incorrectos | «El correo o la contraseña no coinciden. Revise los datos e inténtelo de nuevo. Si se registró con Google, use «Continuar con Google».» | Corregir, «¿Olvidó su contraseña?» o Google |
| Bloqueo por intentos | «Por su seguridad, bloqueamos el ingreso con este correo durante 5 minutos porque hubo varios intentos fallidos. Podrá intentarlo de nuevo a las 3:42 p. m.» y cuenta regresiva | Esperar, o «Restablecer mi contraseña» |
| Correo sin confirmar | «Falta confirmar su correo. Abra el enlace que le enviamos al registrarse.» | «Reenviar enlace» |
| Sin internet | «No hay conexión a internet. Revise su conexión e inténtelo de nuevo.» | Reintentar |
| NotiUrba no responde (fallo del servicio) | «NotiUrba no está respondiendo en este momento. No es un problema con sus datos. Inténtelo de nuevo en unos minutos.» | Reintentar |
| Falla de Google (cerró la ventana, cuenta no válida, no cargó) | «No pudimos confirmar su cuenta de Google. Inténtelo de nuevo o ingrese con su correo y contraseña.» | Reintentar o ingresar con correo |
| Cualquier otro caso no previsto | «No pudimos completar el ingreso. Inténtelo de nuevo en un momento.» | Reintentar |

- **CA-35** — _Dado_ cada situación de la tabla, _cuando_ ocurre en el ingreso (correo o Google), _entonces_ la pantalla muestra el mensaje de su fila y la acción que permite, y nunca el texto que trae el servicio en un fallo 5xx o desconocido.
- **CA-36** — _Dado_ el conjunto de todos los mensajes del ingreso y de los avisos nuevos (bloqueo, límite diario, duplicado), _cuando_ una prueba automática los revisa, _entonces_ ninguno contiene códigos, números de estado ni las palabras técnicas de RN-24.
- **CA-37** — _Dado_ un bloqueo vigente, _cuando_ la persona restablece su contraseña con el enlace del correo, _entonces_ el bloqueo de ese correo se levanta (demostró ser dueña del correo).

**Límite diario de reportes**

- **CA-14** — _Dado_ un ciudadano con 2 reportes creados hoy, _cuando_ envía un tercero, _entonces_ responde 429 `LIMITE_DIARIO_REPORTES` con un mensaje que dice el límite y cuándo puede volver, y **no se guarda** el reporte ni se envía correo.
- **CA-39** — _Dado_ dos cuentas distintas, _cuando_ una ya llegó a su límite del día, _entonces_ la otra puede reportar con su propio cupo completo: el límite es **por cuenta**, nunca general.
- **CA-15** — _Dado_ un ciudadano con menos de 2 reportes hoy, _cuando_ crea uno, _entonces_ se guarda como siempre.
- **CA-16** — _Dado_ que el administrador cambia el máximo de reportes por día, _cuando_ el ciudadano reporta, _entonces_ aplica el nuevo valor sin reiniciar la app.
- **CA-17** — La vista previa del mensaje (`/api/reportes/vista-previa`) **no cuenta** como reporte creado.
- **CA-18** — Dos envíos simultáneos del mismo ciudadano con 1 reporte hecho no pueden dejarlo con 3 reportes en el día.

**Duplicados**

- **CA-19** — _Dado_ que existe un reporte **de cualquier cuenta**, de la misma categoría, a menos de la distancia de esa categoría, reciente y no resuelto, _cuando_ la persona envía otro, _entonces_ responde 409 `POSIBLE_DUPLICADO` y no se guarda. La respuesta dice si el parecido es **propio** o de **otra persona**, cuántos parecidos hay, su estado, su fecha y un resumen de su descripción; el **código solo viaja si es propio** (CA-48).
- **CA-20** — _Dado_ un reporte a más distancia que la de su categoría, o de otra categoría, o más viejo que los días de «reciente», _entonces_ no es duplicado y se guarda.
- **CA-21** — _Dado_ dos categorías con distancias distintas (p. ej. huecos y luminarias), _entonces_ la misma separación puede ser duplicado en una y no serlo en la otra.
- **CA-48** — _Dado_ que el parecido es de **otra persona**, _entonces_ la respuesta y el mensaje traen **solo datos base**: la fecha, el estado, la cantidad y un **resumen de la descripción** (hasta 140 caracteres, cortado en una palabra completa y con «…» si se acortó). **No** traen su código, su dirección, su barrio, sus fotos, su nombre ni su correo. Si el parecido es **propio**, trae además el código para ver el reporte.
- **CA-49** — _Dado_ que el parecido es de otra persona, _entonces_ el mensaje lo dice con naturalidad, con el formato «El 12 de noviembre una persona reportó en esta misma categoría: «Hueco grande frente a la panadería…». Ya lo enviamos a la entidad. Puede ser lo mismo que usted quiere reportar», y no promete que sea el mismo problema. La descripción se muestra como texto plano, nunca como código.
- **CA-22** — _Dado_ un reporte **sin GPS** (solo barrio), _entonces_ no se revisa duplicado por distancia (no hay punto que comparar).
- **CA-23** — _Dado_ un reporte rechazado por duplicado o por límite diario, _entonces_ no cuenta contra el límite diario.

**Parámetros ajustables por el administrador**

- **CA-26** — _Dado_ un administrador con sesión, _cuando_ consulta `GET /api/admin/configuracion/limites`, _entonces_ recibe todos los parámetros con su valor actual y la distancia de duplicado de cada categoría activa que exige ubicación.
- **CA-27** — _Dado_ un administrador, _cuando_ envía `PUT /api/admin/configuracion/limites` con valores válidos, _entonces_ se guardan, aplican de inmediato (sin reiniciar), y quedan el administrador y la fecha del cambio en la auditoría de `configuracion`.
- **CA-28** — Caso de error: _dado_ un valor fuera de rango (p. ej. distancia menor a 5 m, 0 reportes por día, bloqueo de 0 minutos), _entonces_ responde 400 `VALIDACION` con el campo y el rango permitido, y no cambia nada.
- **CA-29** — _Dado_ un ciudadano o alguien sin sesión, _cuando_ llama a estos endpoints, _entonces_ recibe 403 o 401 como el resto de `/api/admin/**`.

**Pantallas (primero se diseñan en Figma, se validan, y luego se programan)**

- **CA-30** — _Dado_ un administrador, _cuando_ abre la pantalla «Configuración» del panel, _entonces_ ve y puede cambiar cada parámetro (con su rango y una frase que explique qué hace) y la distancia de duplicado de cada categoría, con estados de cargando, guardado y error.
- **CA-31** — _Dado_ un valor fuera de rango en esa pantalla, _entonces_ se muestra el rango permitido junto al campo y no se guarda.
- **CA-32** — _Dado_ un ciudadano bloqueado en el login, _entonces_ la pantalla de ingreso muestra el mensaje con los minutos de espera y no vuelve a llamar al servidor hasta que pase el tiempo.
- **CA-33** — _Dado_ un reporte rechazado por límite diario, _entonces_ la pantalla de confirmación explica el límite y cuándo vuelve a poder reportar, sin perder lo que escribió.
- **CA-34** — _Dado_ un posible duplicado, _entonces_ la pantalla de confirmación abre una **hoja de confirmación** (la misma que ya usa la app en «¿Enviar el reporte?» y «Cancelar el reporte», con el Design System) que explica con palabras sencillas lo que pasó y tiene dos botones: **«Enviar de todas maneras»** y **«Cancelar reporte»**. Si el parecido es de la propia persona, incluye su código con un enlace para verlo; si es de otra, no.
- **CA-46** — _Dado_ que el parecido ya se envió a la entidad (Enviado o En trámite), _entonces_ el mensaje lo dice: que ya hay un reporte parecido enviado, que puede ser lo mismo que quiere reportar, y que si es un problema distinto puede enviarlo igual. _Dado_ que aún está «Recibido» (sin enviar), _entonces_ el mensaje dice que ya hay uno parecido registrado. Si es propio, el texto habla de «usted lo reportó»; si es de otra persona, de «ya hay un reporte parecido». El mensaje se entiende para cualquier persona, sin términos técnicos (RN-24).
- **CA-47** — _Dado_ que la persona pulsa «Cancelar reporte», _entonces_ el borrador se descarta y vuelve al inicio, igual que en «Cancelar el reporte» del primer paso; _dado_ que pulsa «Enviar de todas maneras», _entonces_ se envía con `confirmarDuplicado: true` y cuenta contra el límite diario.

**Reportes que repiten uno ya resuelto (evidencia)**

- **CA-41** — _Dado_ que en ese lugar y categoría hay un reporte «Resuelto» (de cualquier cuenta) dentro de la ventana de «reciente» y **ninguno sin resolver**, _cuando_ la persona envía otro **sin foto**, _entonces_ responde 422 `EVIDENCIA_REQUERIDA` con un mensaje que explica que debe agregar una foto reciente, y no se guarda.
- **CA-42** — _Dado_ el mismo caso, _cuando_ envía el reporte **con al menos una foto**, _entonces_ se guarda sin aviso de duplicado, queda marcado como «requiere evidencia», y **no se envía a la entidad** si por cualquier motivo no llegó ninguna foto.
- **CA-43** — _Dado_ un reporte «Resuelto» cercano, _entonces_ **no** cuenta como duplicado (CA-19), pero uno cercano en otro estado (Recibido, Enviado, En trámite) sí. Si hay uno sin resolver y otro resuelto, gana el aviso de duplicado. El «reciente» de un resuelto se cuenta desde el día en que se resolvió.
- **CA-44** — _Dado_ que la vista previa detecta que el reporte repite uno resuelto, _entonces_ la respuesta lo indica (`requiereEvidencia`) y la pantalla de confirmación explica por qué la foto es necesaria y deja agregarla sin perder lo escrito.
- **CA-45** — _Dado_ el paso de fotos del reporte, _entonces_ el texto pide una foto **reciente** (tomada hoy o ahora), explica para qué sirve y, cuando la foto es obligatoria (CA-44), dice «obligatoria» en vez de «(opcional)».
- **CA-50** — _Dado_ el campo de descripción del reporte, _entonces_ el título, la etiqueta y el **texto de ayuda siempre visible** bajo la casilla (no un placeholder que desaparece al escribir) explican con palabras sencillas **qué escribir** (qué pasa, desde cuándo y cómo afecta), dan **un ejemplo**, avisan que **no hace falta repetir la dirección** si ya marcó el lugar, que **no se escriban datos de otras personas** y que **un resumen, sin su nombre, puede mostrarse a otras personas que reporten lo mismo**. El microcopy es entendible para cualquier ciudadano y pasa la revisión de RN-24.

**Contrato y pruebas**

- **CA-24** — Los errores nuevos (`INTENTOS_EXCEDIDOS`, `LIMITE_DIARIO_REPORTES`, `POSIBLE_DUPLICADO`) y los endpoints de parámetros salen en el formato `{ codigo, mensaje, campo }` y quedan documentados en `docs/contexto/11-contrato-api.md`.
- **CA-25** — Cada criterio anterior tiene al menos una prueba automática; las de tiempo (ventana de 5 min, día) no esperan de verdad: se controlan con un reloj de prueba.

## 6. Reglas y restricciones que aplican

- Reglas de negocio: RN-05 (solo ciudadano con cuenta crea reportes), RN-08, RN-11; se agregan RN-22 y RN-23.
- Decisiones cerradas (ADR) que condicionan: ADR-0002 (Resend; 1 correo por reporte y 100 al día, por eso el tope diario), ADR-0008 (privacidad).
- Ley 1581 de 2012 / datos personales: el aviso de duplicado cruza reportes de **todas** las cuentas y muestra un **resumen de la descripción** de otra persona, sin identificarla (CA-48). Como una descripción es texto libre y puede traer datos personales sin querer, se mitiga con tres cosas: resumen corto (140 caracteres), ninguna otra pista de identidad (sin código, nombre, correo, dirección ni fotos) y una ayuda en el campo de descripción que dice que no se escriban datos de otras personas y que un resumen puede verse (CA-50). **Riesgo anotado:** el aviso de privacidad (validación jurídica pendiente, ADR-0008) debe cubrir que un resumen pueda mostrarse a otras personas. El log de Google **no** guarda datos personales (CA-05). El conteo de intentos usa el correo solo para decidir el bloqueo; no se publica ni se devuelve.
- RNF-03 (seguridad): el bloqueo no debe revelar qué correos existen (CA-11).
- Valores decididos por Andrés el 13 de nov de 2026: login 3 intentos en 5 min y 5 min de descanso; reportes 2 por día (3 también serviría); duplicados por categoría con un mínimo de 5 m.
- No hace falta esperar la autorización del docente: lo acordado ya está aceptado (Andrés, 13 nov).
- Interfaz en español y tono claro. Los mensajes nuevos se escriben en la pantalla existente de errores; no se crean pantallas.

## 7. Fuera de alcance

- Bloqueo por IP, CAPTCHA o autenticación de dos pasos (el bloqueo es por correo).
- Límites en otros endpoints (reenviar verificación ya tiene el suyo; recuperación de contraseña queda como está).
- **Apoyar** un reporte parecido (sumarse al de otra persona) o fusionar reportes: aquí solo se avisa y se deja decidir.
- Cambiar los textos de los correos o el flujo de verificación, salvo moverlo a segundo plano.
- Reescribir los mensajes de **todas** las pantallas: esta historia cubre el ingreso (correo y Google) y los avisos nuevos. El aviso de «servicio despertando» y el repaso de tono de las demás pantallas quedan en las historias de pulido y de UX.
- Distancias distintas por **zona** (por ejemplo el centro): hoy la distancia depende solo de la categoría.
- Comprobar que la foto sea realmente reciente (fecha de la imagen, ubicación): solo se **indica** en pantalla.
- Una cola con reintentos del correo de verificación (el reenvío manual cubre el fallo).
- La pantalla para ajustar la **vigencia** de los reportes (tiene API, pero queda para otra historia; solo se deja el lugar listo en la misma pantalla de configuración).

## 8. Preguntas abiertas (todas resueltas el 13 nov 2026)

Andrés aprobó la especificación el 13 de nov con mis propuestas, salvo los cambios indicados arriba. Los valores quedan como **decididos**; si cambian, se actualiza esta tabla.

1. **[DECIDIDO]** **Causa del fallo de Google en el celular.** No se puede saber desde el código: «No pudimos comunicarnos» es un error de red en el navegador (Render dormido, CORS o bloqueo del celular), no una respuesta del backend. _Aprobado:_ primero poner el log (CA-05), probar en un celular real y desde ahí decidir si hay algo que arreglar. Si resulta ser Render dormido, se anota en la skill de pruebas y no se cambia código. — decide: Andrés.
2. **[DECIDIDO]** **Bloqueo por correo o por correo + IP.** _Aprobado:_ solo por correo (más simple; el servidor está detrás de Render y la IP real no es confiable). — decide: Andrés.
3. **[DECIDIDO]** **El administrador entra por el mismo login.** _Aprobado:_ misma regla para todos. Riesgo conocido: alguien podría bloquear 5 minutos la cuenta del administrador a propósito; es aceptable en el MVP. — decide: Andrés.
4. **[DECIDIDO]** **Qué cuenta como «día» del límite.** _Aprobado:_ día calendario en hora de Colombia (se reinicia a las 00:00). Es lo más fácil de explicar al ciudadano («vuelve mañana»). Alternativa: 24 horas corridas. — decide: Andrés.
5. **[DECIDIDO]** **Qué es «reciente» para un duplicado.** _Aprobado (Andrés, 13 nov):_ **los últimos 30 días**, valor inicial y máximo; el administrador puede acortarlo (rango 1 a 30, `duplicado_ventana_dias`).
6. **[DECIDIDO]** **¿El duplicado bloquea o solo avisa?** La historia dice «advertencia». _Aprobado:_ 409 la primera vez; si el ciudadano confirma con `confirmarDuplicado: true`, se guarda (cuenta contra el límite diario). Eso pide un botón nuevo en la pantalla de confirmación. Alternativa: bloquear siempre y la persona cambia la ubicación o espera. — decide: Andrés; si hay botón, Katherine lo revisa.
7. **[DECIDIDO]** **Distancia por categoría (mínimo 5 m).** El valor base es 5 m. _Aprobado como valores iniciales, ajustables por el administrador:_

   Tuluá urbano es pequeño y el centro junta muchos reportes muy cerca, así que las distancias son **cortas y más flexibles donde los problemas se repiten en el mismo tramo** (revisado el 13 nov). Nunca pasan de 15 m, y el ciudadano siempre puede confirmar con «Enviar de todos modos».

   | Categoría | Distancia inicial | Razón |
   | --- | --- | --- |
   | 1 Vías, huecos y andenes | 5 m (el mínimo) | Los huecos pueden estar pegados |
   | 3 Basuras, ambiente, árboles | 5 m | En el centro hay varios puntos de basura en una cuadra |
   | 4 Señalización, semáforos y tránsito | 8 m | Señales y semáforos están cerca en una esquina |
   | 2 Alumbrado, espacio público, convivencia y seguridad | 10 m | Luminarias seguidas a esa separación son distintas |
   | 5 Riesgo sanitario | 10 m | |
   | 11 Acueducto y alcantarillado | 10 m | Una fuga es un punto |
   | 12 Gas, 13 Energía, 14 Aseo (falla del servicio) | 15 m | La falla afecta la cuadra, pero no más |
   | 8 Riesgo de desastres | 15 m | |
   | 6, 7, 9, 10, 15 (sin ubicación obligatoria) | no aplica (se necesita GPS) | |

   — decide: Andrés (solo la columna de distancia; el administrador las ajusta después).
8. **[DECIDIDO]** **Límite diario: 2 o 3.** _Aprobado:_ **2**. Cubre el uso real (rara vez alguien reporta más de 2 problemas distintos al día), frena el abuso, y para probar la app alcanza con crear cuentas de prueba o subir el valor en la base unos minutos. 3 también es razonable. — decide: Andrés.
9. **[DECIDIDO]** **Cómo se «ve» el duplicado en la vista previa.** _Aprobado:_ la vista previa no avisa; solo avisa el envío final (CA-19). Si quieres avisar antes de confirmar, se agrega a esa llamada. — decide: Andrés.

10. ~~Pantalla de parámetros~~ **Decidido (Andrés, 13 nov):** entra en esta historia, con Figma primero. Queda por revisar si los 5 puntos alcanzan (ver el plan).
11. **[DECIDIDO]** **Rangos permitidos.** _Aprobado:_ intentos de 1 a 10; ventana y bloqueo de 1 a 60 minutos; reportes por día de 1 a 50; «reciente» de 1 a 30 días; distancia de duplicado de 5 a 500 m. Los valores iniciales son los decididos (3, 5, 5, 2, 30 y la tabla de la pregunta 7). — decide: Andrés.

12. **[DECIDIDO]** **Avisar los intentos que quedan** (Andrés, 13 nov): cuando le queda **un solo intento**, el aviso agrega «Le queda 1 intento antes de un bloqueo de N minutos». Es igual para todos los correos (CA-40).

13. **[DECIDIDO]** **Reportes sobre algo ya resuelto** (Andrés, 13 nov): un reporte cercano «Resuelto», de cualquier cuenta, no cuenta como duplicado, pero repetirlo exige evidencia (fotos) y las pantallas piden fotos recientes (RF-39).
16. **[DECIDIDO]** **Los parecidos casi siempre son de otras cuentas** (Andrés, 13 nov): el duplicado se busca entre los reportes de **todas** las cuentas, no solo los propios, con la misma categoría, una distancia cercana y los últimos 30 días. Se muestran solo datos base de la otra persona: fecha, estado, cantidad y un resumen de su descripción (CA-48).
14. **[DECIDIDO]** **Estimación:** la historia **se queda en 5 puntos** en Linear (no se toca, para no alterar el burn down ni la velocidad).
15. **[DECIDIDO]** **Textos de los avisos:** se afinan solo con Andrés; Amelia hará después una revisión más completa.

## 9. Aprobación

- [x] La especificación está completa y sin ambigüedades.
- [x] Las preguntas abiertas están resueltas o aceptadas como riesgo.
- [x] **Aprobada por el usuario (13 nov 2026)** → se puede pasar a `plan.md`.
