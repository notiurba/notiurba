-- =====================================================================
-- NotiUrba · V12: aviso de privacidad versión 3 (vigente desde el despliegue)
-- Reemplaza como vigente al marcador «PENDIENTE» de la versión 1.0 (V3), que no se edita: una migración
-- aplicada no se toca. El servidor guarda en cada usuario la versión vigente al registrarse
-- (CatalogosRegistro.avisoPrivacidadVigente: la de vigente_desde más reciente que ya se cumplió).
-- Quienes ya tenían cuenta conservan la versión que aceptaron.
-- El mismo texto vive en frontend/src/compartido/textos/avisoPrivacidad.js; su prueba comprueba que coincidan.
-- Pendiente de validación jurídica: ver docs/decisiones/0008-privacidad-autorizacion-unica-y-verificacion-de-correo.md
-- =====================================================================

INSERT INTO aviso_privacidad (nombre, version, texto, vigente_desde)
VALUES ('Aviso de privacidad NotiUrba v3', '3', $aviso$Ley 1581 de 2012 · Versión 3

Rige la Ley 1581 de 2012 y el Decreto 1377 de 2013 (Habeas Data). Al crear su cuenta y usar NotiUrba usted acepta este aviso. Puede volver a consultarlo cuando quiera desde Ajustes.

Responsable del tratamiento
NotiUrba, proyecto académico del equipo NotiUrba de la Universidad del Valle, sede Tuluá (Colombia). Contacto: notiurba@gmail.com.

Datos que recolectamos
Nombres y apellidos, tipo y número de documento, correo, teléfono (opcional), barrio y dirección de correspondencia. Si ingresa con Google, también su nombre y correo. En cada reporte: ubicación GPS, descripción y fotos.

Para qué los usamos
Para crear su cuenta, enviar los reportes que usted haga a la Alcaldía de Tuluá o a las empresas de servicios públicos, mostrarle su seguimiento y avisarle si un correo no llega. No los usamos para publicidad. Antes de cada envío le pedimos su autorización.

Con quién los compartimos
Únicamente con la entidad a la que usted dirige cada reporte. Recibe su nombre, documento, dirección, teléfono, ubicación y fotos, porque los necesita para tramitar su solicitud (art. 16, Ley 1755 de 2015). Los correos ya enviados quedan en poder de esa entidad y NotiUrba no puede retirarlos.

Su correo y la respuesta de la entidad
Su correo se comparte con la entidad como dirección de respuesta, para que le conteste directamente. NotiUrba no le envía copia del reporte: puede verlo en Mis reportes.

Fotos de sus reportes
Cada foto se guarda con un enlace público: cualquier persona que tenga el enlace puede verla. Evite fotografiar rostros, menores de edad y placas de vehículos. El enlace va incluido en el correo que recibe la entidad.

Servicios de terceros
Google (inicio de sesión opcional), Resend (correo), Cloudinary (fotos), OpenStreetMap (mapa) y los proveedores que alojan la aplicación y la base de datos (Render, Neon y Netlify). Algunos pueden tener servidores fuera de Colombia.

Seguridad de los datos
Las contraseñas se guardan cifradas, la conexión usa HTTPS y el acceso a los datos es restringido. Si un incidente afecta sus datos, se lo informaremos.

Conservación
Guardamos sus datos mientras tenga su cuenta. Si elimina sus datos o su cuenta desde Ajustes, se borran de inmediato, salvo los correos ya enviados a la entidad. Los reportes se muestran en la app durante su vigencia de 30 días.

Solo mayores de edad
NotiUrba es solo para personas mayores de 18 años. No se aceptan registros ni reportes de menores de edad. Al crear su cuenta usted declara que es mayor de edad.

Sus derechos (Ley 1581, art. 8)
• Conocer, actualizar y rectificar sus datos.
• Pedir prueba de la autorización que dio.
• Saber cómo se usan sus datos.
• Revocar su autorización y pedir que se eliminen.
• Presentar quejas ante la Superintendencia de Industria y Comercio.

Cómo ejercerlos
Desde Ajustes puede editar sus datos, eliminar sus datos o eliminar su cuenta. También puede escribir a notiurba@gmail.com con el asunto «Habeas Data – su nombre», indicando su nombre, documento y lo que solicita. Respondemos consultas en máximo 10 días hábiles y reclamos en máximo 15 (Ley 1581, arts. 14 y 15).

Cambios a este aviso
Si este aviso cambia, se lo informaremos en la app y subirá el número de versión. Podrá aceptarlo de nuevo o eliminar su cuenta.$aviso$, now());
