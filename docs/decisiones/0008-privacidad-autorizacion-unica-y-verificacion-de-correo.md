# ADR-0008 — Privacidad, autorización única y verificación de correo

**Estado:** Aceptada (01/11/2026). Las decisiones de producto están cerradas; **el texto legal del aviso
está por validar** con el docente o un asesor jurídico (lista al final).

## Contexto

NotiUrba envía por correo, a nombre del ciudadano, un reporte con sus datos personales, su ubicación y
fotos con enlace público. El aviso de privacidad de la base era solo un marcador («PENDIENTE», versión
1.0 de la migración V3), el Figma tenía dos casillas de autorización y la verificación del correo
estaba como «propuesto: no hacerla». El 01/11/2026 se cerró lo siguiente.

## Decisión

1. **Una sola autorización de envío** (`autorizaEnvio`). No hay `autorizaCopia` y **no se envía copia al
   ciudadano**: su correo va en Reply-To y en el cuerpo del mensaje («responder a este correo»). Sale 1
   correo por reporte.
2. **Responsable del tratamiento:** el equipo NotiUrba, Universidad del Valle, sede Tuluá. Canales para
   ejercer derechos: **Ajustes** de la app y notiurba@gmail.com.
3. **Eliminar la cuenta o los datos** los borra de inmediato, salvo los correos ya enviados a la entidad
   (NotiUrba no puede retirarlos).
4. **Vigencia del reporte:** 30 días.
5. **Solo mayores de 18 años.** El registro y «completar perfil» exigen `declaraMayorEdad=true`, en la
   misma casilla que acepta el aviso (un solo clic). Reemplaza la decisión previa de no tener casilla.
6. **El correo se confirma con un enlace al registrarse.** Reemplaza la propuesta de no verificar:
   evita que alguien envíe reportes con los datos de otra persona. Cada registro gasta 1 correo de
   Resend (límite gratuito de 100 al día).
7. **Aviso de privacidad versión 3 como el vigente.** Va en la migración **V12** (la V3 no se editó; la
   versión 1.0 queda para las cuentas que ya la aceptaron) y el mismo texto está en
   `frontend/src/compartido/textos/avisoPrivacidad.js`. Una prueba comprueba que coincidan. El servidor
   guarda en cada usuario la versión vigente y la fecha-hora de aceptación.

Textos de las casillas:
- Registro y completar perfil: «Soy mayor de 18 años y acepto el tratamiento de mis datos personales
  (Ley 1581). Ver aviso de privacidad».
- Paso 4 del reporte: «Autorizo a NotiUrba a enviar este reporte a la entidad competente, a mi nombre y
  con mis datos, ubicación y fotos (con enlace público). La entidad podrá responderme a mi correo. Ver
  aviso de privacidad».

## Consecuencias

- El aviso v3 ya es el que se guarda al registrarse, aunque su texto legal no esté validado.
- Si el aviso cambia, se publica con otra migración y otro número de versión; no se edita el texto de
  una migración aplicada.
- La verificación de correo y la declaración de mayoría de edad tienen estas piezas en el backend: la
  verificación ya existe (`/api/auth/verificar`, `/api/auth/reenviar-verificacion`, error
  `CORREO_NO_VERIFICADO`); **`declaraMayorEdad` falta**.

## Compromisos del aviso que todavía no existen

El aviso ya los promete, así que deben estar antes de abrir el servicio al público:
- **Ajustes:** editar datos, eliminar datos, eliminar la cuenta y volver a leer el aviso (no hay
  endpoints ni pantallas).
- **Mis reportes:** `GET /api/reportes` (propuesto en el contrato, no existe).
- **Avisar si un correo no llega:** webhook de rebotes de Resend y notificación al ciudadano.
- **Borrar las fotos de Cloudinary** al eliminar los datos (la columna `id_publico_proveedor` ya
  permite hacerlo; falta el borrado).
- **Aceptar de nuevo** el aviso si sube de versión.
- **Pedir autorización antes de cada envío:** casilla del paso 4 en la PWA.
- **Responder** consultas en máximo 10 días hábiles y reclamos en 15: es un proceso humano y el buzón
  notiurba@gmail.com debe tener quien lo atienda.

## Por validar con el docente o asesor jurídico

- Si el art. 16 de la Ley 1755 de 2015 justifica enviar a la entidad el **documento y la dirección**
  del ciudadano.
- **Países de los servidores** de cada proveedor (Google, Resend, Cloudinary, Render, Neon y Netlify) y
  si hay transferencia internacional de datos.
- **Plazos de conservación y de borrado de las fotos** en Cloudinary.
- **Fotos con rostros** y de menores: qué hacer con ellas.
- El **canal oficial** para ejercer los derechos (hoy Ajustes y un correo de Gmail).

Otros puntos que salieron al armar el aviso:
- El aviso dice que a la entidad llega el **teléfono**, pero RF-04 solo exige nombre, documento y
  dirección: hay que alinear uno con el otro cuando se implemente el correo.
- Si un proyecto académico puede figurar como **responsable del tratamiento**, y quién responde ante la
  Superintendencia de Industria y Comercio.
