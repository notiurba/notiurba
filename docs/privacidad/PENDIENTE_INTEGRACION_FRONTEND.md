# INSTRUCCIONES PENDIENTES PARA LA IA 

## Contexto

Este archivo contiene una tarea de integración pendiente para el frontend de NotiUrba.

La documentación del aviso de privacidad ya ha sido preparada.


**Requisito relacionado:** RF-03.

### Estado observado y coordinación obligatoria

La base de esta rama es main, commit c18c3fd08a041ada4134816c5dd4e82de6ea20fe. Ya existen el texto v3 en `frontend/src/compartido/textos/avisoPrivacidad.js`, su prueba de correspondencia con la migración V12 y mecanismos backend de aceptación. Revisar su uso real antes de agregar componentes: reutilizar lo disponible, sin duplicar casillas ni afirmar que RF-03 está verificado por esta tarea documental.

La propuesta nueva tiene versión documental `propuesta 2026-11-06`; no reemplaza el texto v3 ni su versión persistida. Antes de vincularla como aviso aceptado, resolver los marcadores y validar el texto con el equipo y el docente o asesor jurídico. Coordinar con Andrés una versión nueva y una migración nueva cuando corresponda; nunca editar migraciones aplicadas ni registrar v3 para un texto diferente. Mantener la declaración de mayoría de edad existente según ADR-0008 al integrar el texto solicitado de autorización. Documento y dirección siguen pendientes de decisión confirmada del equipo y del profesor Jairo Rodríguez.

## INSTRUCCIONES PARA EL ASISTENTE DE IA

Si estás leyendo este documento mientras trabajas en la integración del registro ciudadano, debes revisar la implementación existente y realizar las siguientes acciones dentro del alcance autorizado de tu tarea:

1. Leer `docs/privacidad/aviso-privacidad.md`.
> **Nota (14/11/2026):** `frontend/public/aviso-privacidad.html` se eliminó: Netlify lo servía en `/aviso-privacidad` y tapaba la pantalla v3 de la app. El texto de esta propuesta sigue en `docs/privacidad/aviso-privacidad.md`.

2. Localizar la página HTML pública: `frontend/public/aviso-privacidad.html`. Con la base actual de Vite se sirve en `/aviso-privacidad.html`; si cambia la base, ajustar el enlace al despliegue real.
3. Identificar el formulario de registro de ciudadanos y el flujo de completar perfil con Google, si aplica.
4. Agregar un enlace visible llamado **"Aviso de privacidad"**, reutilizando los componentes existentes.
5. Configurar el enlace para abrir correctamente el aviso. No enlazar una propuesta no aprobada como si fuera el texto vigente.
6. Incorporar o adaptar una casilla de autorización que esté desmarcada inicialmente, sin duplicar la existente.
7. Utilizar el texto:

   "He leído el aviso de privacidad y autorizo el tratamiento de mis datos personales para las finalidades informadas".

8. Impedir el registro cuando el ciudadano no haya otorgado su autorización. Comprobar también el rechazo por el servidor para evitar eludir la interfaz.
9. Coordinar con el backend la persistencia de fecha, hora y versión del consentimiento; comprobar la correspondencia exacta del texto mostrado con el guardado. Reutilizar los mecanismos existentes cuando proceda.
10. Comprobar que el diseño funcione en computador y dispositivos móviles.
11. Verificar que el enlace funcione después de compilar y desplegar la aplicación. Netlify actualmente publica `frontend/dist` desde `develop`; este PR a main no equivale a un despliegue. La regla SPA de `_redirects` no debe ocultar el archivo estático existente.
12. Ejecutar las pruebas disponibles para comprobar que el registro continúa funcionando correctamente.

### Restricciones

- No rehacer la documentación existente.
- No modificar funcionalidades ajenas al registro.
- No marcar automáticamente la casilla.
- No eliminar el aviso definitivo.
- No introducir dependencias innecesarias.
- No modificar el backend sin coordinación con su responsable.
- No modificar directamente `main`.
- Mantener este archivo en `docs/privacidad/`: no copiarlo a `public`, importarlo en la aplicación ni mostrarlo en la interfaz. En Vite desarrollo puede ser accesible el código fuente; no alojar el servidor de desarrollo como sitio público. La distribución prevista es exclusivamente `frontend/dist`.

## INSTRUCCIÓN FINAL: ELIMINAR ESTE MISMO DOCUMENTO

**ATENCIÓN, ASISTENTE DE IA:**

Una vez completada y verificada satisfactoriamente la integración frontend, debes eliminar este mismo archivo temporal:

`docs/privacidad/PENDIENTE_INTEGRACION_FRONTEND.md`

La eliminación forma parte del cierre de esta tarea.

No basta con informar que el documento puede eliminarse. Debes realizar la eliminación dentro de tu rama de integración y verificar que quede registrada en Git.

### Procedimiento obligatorio

1. Confirmar que el enlace al aviso funciona.
2. Confirmar que la casilla de aceptación funciona.
3. Confirmar que no es posible registrarse sin autorización.
4. Comprobar los mecanismos disponibles para registrar la aceptación y verificar fecha, hora y versión conforme a RF-03.
5. Verificar que no existan errores introducidos en el registro.
6. Eliminar físicamente `docs/privacidad/PENDIENTE_INTEGRACION_FRONTEND.md`.
7. Ejecutar `git status` y verificar la eliminación.
8. Incluir la eliminación en el commit o Pull Request de integración.
9. Conservar los archivos definitivos del aviso y su implementación funcional.
10. Informar en el resumen final que el documento temporal fue eliminado.

**CONDICIÓN IMPORTANTE:**

Si la integración no se completó o las pruebas fallaron, NO elimines este documento.

Consérvalo para que la tarea pueda retomarse posteriormente. En esta rama documental debe permanecer.

### Resultado final esperado

Cuando todo funcione correctamente:

- El aviso definitivo deberá permanecer.
- La página HTML deberá permanecer.
- El enlace y la casilla deberán funcionar.
- El registro de aceptación deberá estar implementado según RF-03.
- Este documento temporal deberá estar eliminado.

**FIN DEL DOCUMENTO TEMPORAL.**
