# ADR-0006 — Almacenamiento de fotos: Cloudinary

**Estado:** Aceptada (29/10/2026). Cloudinary, plan gratuito. Resuelve NOT-28 en lo que toca a las fotos.

## Contexto

El correo lleva un **enlace** a la foto, no la adjunta (RF-14). Criterios: sin costo, de
preferencia sin tarjeta, enlace público estable y sin depender del disco del servidor
(en Render Free es efímero). Se comparó Cloudinary con Mega.

## Decisión

**Usar Cloudinary.** Descartado Mega.

- Cloudinary entrega URL pública directa, CDN y redimensión o conversión por URL.
- Mega está pensado para archivos cifrados de extremo a extremo: no entrega URL directa de
  imagen para un `<img>` ni CDN, y su API no sirve para publicar contenido. (Conclusión
  por diseño del producto, no probada en este proyecto.)
- Respaldo si se agotan los créditos: Cloudflare R2 (ADR-0004).

## Límites del plan gratuito

- 25 créditos al mes, compartidos entre almacenamiento, ancho de banda y transformaciones
  (1 crédito ≈ 1 GB de almacenamiento, 1 GB de tráfico o 1.000 transformaciones).
- **Dato de una fuente de terceros marcada como «no verificado»** (agentdeals.dev). Confirmar
  en cloudinary.com/pricing antes de depender de la cifra. La página oficial de preguntas
  frecuentes solo habla de «límite generoso» sin cifras.

## Consecuencias

- Flujo: el frontend comprime la foto a ≤1 MB (RF-06); el backend la sube con la API key
  y guarda en PostgreSQL solo la URL y el identificador.
- Variables nuevas en Render (sin valores en el repo): `CLOUDINARY_CLOUD_NAME`,
  `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
- Riesgo de cuota: una sola bolsa de créditos. Mantener las fotos pequeñas y usar
  transformaciones por URL con moderación.
- **Privacidad (Ley 1581):** la URL es pública. Las fotos pueden mostrar personas; el aviso
  de privacidad debe cubrirlas. Consulta al docente aún pendiente.
- Aislar la subida detrás de una interfaz para poder pasar a R2 sin tocar el resto.
- **Hecho (01/11/2026):** interfaz `AlmacenFotos` y su implementación con la API HTTP de Cloudinary (subida firmada, sin SDK), probada subiendo y borrando una imagen real. Cloudinary asigna un identificador aleatorio a cada foto, así que el enlace no se puede adivinar. Probado también en producción (Render) el 01/11/2026: la foto subida por la API desplegada se abre sin sesión.
