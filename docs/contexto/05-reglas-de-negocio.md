# Reglas de negocio

Reglas que el sistema debe cumplir siempre. Cada una tiene ID (RN) y fuente. Si una
regla cambia, se actualiza aquí y en su pestaña de origen.

## Territorio

- **RN-01**: cada reporte pertenece a una de las 10 comunas urbanas de Tuluá; no se
  usa otra sectorización.
- **RN-02**: la comuna se asigna por el polígono GeoJSON que contiene las coordenadas
  GPS, no por el barrio.
- **RN-03**: un reporte con coordenadas fuera de las 10 comunas se rechaza por estar
  fuera de cobertura.
- **RN-04**: la relación barrio–comuna es la oficial de la Alcaldía (2019); los barrios
  posteriores no están cubiertos.

## Ciudadano y cuenta

- **RN-05**: solo un ciudadano con cuenta y sesión activa crea reportes; no hay
  reportes anónimos. Quien no quiera identificarse usa el PQRSDF anónimo de la Alcaldía.
- **RN-06**: el registro exige aceptar el aviso de privacidad y guarda fecha y hora de
  la aceptación.
- **RN-07**: cada correo identifica al ciudadano con los datos del artículo 16 de la
  Ley 1755 de 2015 y deja constancia de que autorizó el envío a su nombre.

## Reporte

- **RN-08**: todo reporte tiene categoría de la lista vigente y descripción de 10 a 500
  caracteres (*propuesto*).
- **RN-09**: la foto es JPG o PNG, se reduce a ≤1 MB antes de subir (*propuesto*) y
  viaja en el correo como enlace seguro.
- **RN-10**: cada reporte recibe un código «seguimiento NotiUrba» único. No es el
  radicado oficial: el radicado lo asigna la entidad.

## Destino del reporte

- **RN-11**: cada categoría tiene una entidad de destino en una tabla configurable, no
  escrita en el código.
- **RN-12**: los reportes de categorías de la Alcaldía se envían siempre al CAIC
  (`servicioalciudadano@tulua.gov.co`); el asunto y el cuerpo indican la dependencia
  competente. Los correos de cada dependencia son solo de referencia.
- **RN-13**: los reportes de servicios públicos (acueducto, gas, energía, aseo) se
  envían a la empresa prestadora, no al CAIC.
- **RN-14**: si una categoría no tiene dependencia competente, el reporte va al CAIC y
  queda marcado para revisión.
- **RN-15**: si una empresa no acepta correo, NotiUrba muestra su canal oficial y marca
  el reporte como no enviado por correo.
- **RN-16**: fugas y emergencias de gas no se envían por correo: la app indica llamar a
  la línea 164.
- **RN-17**: el asunto sigue el formato «Reporte ciudadano – [categoría] – [barrio y
  comuna] – NotiUrba #[ID]» (*formato propuesto*).
- **RN-18**: NotiUrba no envía correos sin intervención del ciudadano. El mecanismo de
  envío (cuenta del ciudadano o Resend) está **pendiente** y se decide entre el final
  del Sprint 2 y el Sprint 3. Ver ADR-0002.

## Estados y vigencia

- **RN-19**: NotiUrba no radica ni conoce el estado oficial del trámite: el
  administrador actualiza estados y observaciones a mano.
- **RN-20**: el administrador define la vigencia de cada reporte en días; al vencer,
  sale del mapa pero sigue en el listado del administrador.
- **RN-21**: el MVP tiene un solo rol de administrador; la Alcaldía y las empresas no
  acceden al panel.

Fuente: pestaña «Reglas de negocio» del Google Doc.
