# Registro de herramientas y límites

Fecha de la sesión: 14/11/2026, Colombia.

| Superficie | Acción real | Resultado verificable |
|---|---|---|
| Git local | status, rama, HEAD y remotos | Limpio al inicio; fix/registro-pr20, d8e8b2b |
| GitHub | Buscar rama NOT-141 y comparar con develop | Sin rama coincidente; diverged, 156/4; develop 8b8419a |
| Netlify | Buscar proyectos notiurba | Solo prototipo-evaluacion; no acredita sitio oficial |
| Terminal curl | Frontend y health | Exit 6; no resolución DNS; sin HTTP |
| HTTP Node | 12 GET seguros | ENOTFOUND; cuerpos no obtenidos |
| Herramienta web alternativa | Abrir frontend y health | URL no accesible; sin HTTP verificable |
| Chrome | Inventario, abrir pestaña QA, reconectar, navegar y leer | Timeout navegación; timeout CDP Runtime.evaluate |
| ADB | Ruta absoluta, version, devices y get-state | Binario funciona; daemon inaccesible |
| Gmail | Dos búsquedas acotadas | Sin correos de reporte QA identificados; sin envío |
| Neon | SELECT versión y metadatos | PG18.7; migraciones 1–22; 133 barrios públicos; 14 rutas |
| Newman | Resolver módulo/comando instalado | No instalado; no descarga ni ejecución |
| Postman UI | Importación y ejecución | No ejecutadas |
| Scripts Postman nuevos | Compilar y comprobar guardas | 114 recursos, 92 guardas bloquean por defecto |
| Mapa | Fallo de API inyectado en VM local | Devuelve sentinela simulada; no es una prueba UI |

Los errores de conectividad no se consideran respuestas HTTP 500, 404 o 000. No se capturaron pantallas de PC/Android ni mensajes reales de QA. Ninguna imagen se generó para reemplazar evidencias faltantes.
