# Plan de terminación — NOT-134

Implementa el alcance pedido en [spec.md](spec.md), conservando las decisiones
del usuario y corrigiendo los defectos encontrados en su implementación inicial.

1. Revisar el diff y ejecutar los cuatro comandos pedidos antes de cambiar código.
2. Mantener VitePWA y el manifiesto. Registrar con `virtual:pwa-register` desde
   `main.jsx` para conectar `autoUpdate` con la recarga de la pantalla.
3. Convertir `SinConexion` en un diálogo nativo: foco dentro del aviso, prioridad
   sobre otros diálogos, sin cerrar con Escape mientras no haya conexión.
   Las rutas permanecen montadas y el reintento consulta `navigator.onLine` sin recargar.
4. Probar los eventos de conexión, reintento y preservación del formulario;
   verificar el manifiesto y las reglas del service worker generado.
5. Validar en Chrome con perfil temporal: manifiesto, instalabilidad, caché real,
   navegación sin red, teclado y actualización entre dos versiones.
6. Repetir lint, pruebas y build tras las correcciones; pedir revisión final al subagente.
7. Actualizar la documentación local con resultados reales y pendientes de Android,
   diseño y despliegue. No modificar servicios externos ni el trabajo de QA anterior.

No cambia backend, contratos, base de datos ni rutas. La pantalla compartida nueva
requiere revisión visual de Katherine; el icono definitivo sigue a cargo de Amelia.
