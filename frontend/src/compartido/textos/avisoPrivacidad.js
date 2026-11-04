// Aviso de privacidad VIGENTE (versión 3, 01/11/2026) y textos de las casillas de autorización.
//
// Es la fuente del texto para la pantalla del aviso. La base guarda la misma versión en la migración
// V12__aviso_de_privacidad_v3.sql (tabla aviso_privacidad): el servidor registra qué versión aceptó cada
// persona y cuándo. avisoPrivacidad.test.mjs comprueba que este archivo y la migración digan lo mismo:
// si se cambia uno, hay que cambiar el otro (con una migración nueva y un número de versión nuevo).
//
// El texto está pendiente de validación jurídica: ver docs/decisiones/0008-privacidad-autorizacion-unica-y-verificacion-de-correo.md

export const VERSION_AVISO = '3'

export const AVISO_PRIVACIDAD = {
  version: VERSION_AVISO,
  cabecera: ['Ley 1581 de 2012', 'Versión 3'],
  intro:
    'Rige la Ley 1581 de 2012 y el Decreto 1377 de 2013 (Habeas Data). Al crear su cuenta y usar NotiUrba usted acepta este aviso. Puede volver a consultarlo cuando quiera desde Ajustes.',
  secciones: [
    {
      titulo: 'Responsable del tratamiento',
      parrafos: [
        'NotiUrba, proyecto académico del equipo NotiUrba de la Universidad del Valle, sede Tuluá (Colombia). Contacto: notiurba@gmail.com.',
      ],
    },
    {
      titulo: 'Datos que recolectamos',
      parrafos: [
        'Nombres y apellidos, tipo y número de documento, correo, teléfono (opcional), barrio y dirección de correspondencia. Si ingresa con Google, también su nombre y correo. En cada reporte: ubicación GPS, descripción y fotos.',
      ],
    },
    {
      titulo: 'Para qué los usamos',
      parrafos: [
        'Para crear su cuenta, enviar los reportes que usted haga a la Alcaldía de Tuluá o a las empresas de servicios públicos, mostrarle su seguimiento y avisarle si un correo no llega. No los usamos para publicidad. Antes de cada envío le pedimos su autorización.',
      ],
    },
    {
      titulo: 'Con quién los compartimos',
      parrafos: [
        'Únicamente con la entidad a la que usted dirige cada reporte. Recibe su nombre, documento, dirección, teléfono, ubicación y fotos, porque los necesita para tramitar su solicitud (art. 16, Ley 1755 de 2015). Los correos ya enviados quedan en poder de esa entidad y NotiUrba no puede retirarlos.',
      ],
    },
    {
      titulo: 'Su correo y la respuesta de la entidad',
      parrafos: [
        'Su correo se comparte con la entidad como dirección de respuesta, para que le conteste directamente. NotiUrba no le envía copia del reporte: puede verlo en Mis reportes.',
      ],
    },
    {
      titulo: 'Fotos de sus reportes',
      parrafos: [
        'Cada foto se guarda con un enlace público: cualquier persona que tenga el enlace puede verla. Evite fotografiar rostros, menores de edad y placas de vehículos. El enlace va incluido en el correo que recibe la entidad.',
      ],
    },
    {
      titulo: 'Servicios de terceros',
      parrafos: [
        'Google (inicio de sesión opcional), Resend (correo), Cloudinary (fotos), OpenStreetMap (mapa) y los proveedores que alojan la aplicación y la base de datos (Render, Neon y Netlify). Algunos pueden tener servidores fuera de Colombia.',
      ],
    },
    {
      titulo: 'Seguridad de los datos',
      parrafos: [
        'Las contraseñas se guardan cifradas, la conexión usa HTTPS y el acceso a los datos es restringido. Si un incidente afecta sus datos, se lo informaremos.',
      ],
    },
    {
      titulo: 'Conservación',
      parrafos: [
        'Guardamos sus datos mientras tenga su cuenta. Si elimina sus datos o su cuenta desde Ajustes, se borran de inmediato, salvo los correos ya enviados a la entidad. Los reportes se muestran en la app durante su vigencia de 30 días.',
      ],
    },
    {
      titulo: 'Solo mayores de edad',
      parrafos: [
        'NotiUrba es solo para personas mayores de 18 años. No se aceptan registros ni reportes de menores de edad. Al crear su cuenta usted declara que es mayor de edad.',
      ],
    },
    {
      titulo: 'Sus derechos (Ley 1581, art. 8)',
      items: [
        'Conocer, actualizar y rectificar sus datos.',
        'Pedir prueba de la autorización que dio.',
        'Saber cómo se usan sus datos.',
        'Revocar su autorización y pedir que se eliminen.',
        'Presentar quejas ante la Superintendencia de Industria y Comercio.',
      ],
    },
    {
      titulo: 'Cómo ejercerlos',
      parrafos: [
        'Desde Ajustes puede editar sus datos, eliminar sus datos o eliminar su cuenta. También puede escribir a notiurba@gmail.com con el asunto «Habeas Data – su nombre», indicando su nombre, documento y lo que solicita. Respondemos consultas en máximo 10 días hábiles y reclamos en máximo 15 (Ley 1581, arts. 14 y 15).',
      ],
    },
    {
      titulo: 'Cambios a este aviso',
      parrafos: [
        'Si este aviso cambia, se lo informaremos en la app y subirá el número de versión. Podrá aceptarlo de nuevo o eliminar su cuenta.',
      ],
    },
  ],
}

/** Casilla del registro y de completar perfil con Google (un solo clic: mayoría de edad y tratamiento de datos). */
export const TEXTO_CASILLA_REGISTRO =
  'Soy mayor de 18 años y acepto el tratamiento de mis datos personales (Ley 1581). Ver aviso de privacidad'

/** Casilla del paso 4 del reporte: la única autorización de envío (no hay copia al ciudadano). */
export const TEXTO_CASILLA_ENVIO =
  'Autorizo a NotiUrba a enviar este reporte a la entidad competente, a mi nombre y con mis datos, ubicación y fotos (con enlace público). La entidad podrá responderme a mi correo. Ver aviso de privacidad'

/**
 * El aviso como texto plano: cabecera, introducción y cada sección con su título en una línea y su
 * contenido debajo, separados por una línea en blanco. Es exactamente lo que guarda la migración V12.
 */
export function avisoComoTextoPlano(aviso = AVISO_PRIVACIDAD) {
  const bloques = [aviso.cabecera.join(' · '), aviso.intro]
  for (const seccion of aviso.secciones) {
    const cuerpo = seccion.items
      ? seccion.items.map((item) => `• ${item}`).join('\n')
      : seccion.parrafos.join('\n')
    bloques.push(`${seccion.titulo}\n${cuerpo}`)
  }
  return bloques.join('\n\n')
}
