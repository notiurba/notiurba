package com.notiurba.foto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cloudinary por su API HTTP con subida firmada: la firma es el SHA-1 de los parámetros más el
 * secreto; el secreto nunca viaja. Cloudinary asigna un identificador aleatorio a cada foto, así
 * que el enlace público no se puede adivinar a partir del código del reporte.
 */
public class AlmacenFotosCloudinary implements AlmacenFotos {

    private final RestClient cliente;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String carpeta;
    private final Clock reloj;

    /** @param cliente RestClient con la base {@code https://api.cloudinary.com/v1_1/{cloudName}} */
    public AlmacenFotosCloudinary(RestClient cliente, String cloudName, String apiKey, String apiSecret,
            String carpeta, Clock reloj) {
        this.cliente = cliente;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.carpeta = carpeta;
        this.reloj = reloj;
    }

    @Override
    public FotoGuardada guardar(byte[] contenido, String tipoMime) {
        exigirConfiguracion();
        String marca = marcaDeTiempo();
        var cuerpo = new LinkedMultiValueMap<String, Object>();
        var cabeceraFoto = new HttpHeaders();
        cabeceraFoto.setContentType(MediaType.parseMediaType(tipoMime));
        cabeceraFoto.setContentDisposition(ContentDisposition.formData().name("file").filename("foto").build());
        cuerpo.add("file", new HttpEntity<>(new ByteArrayResource(contenido), cabeceraFoto));
        cuerpo.add("api_key", apiKey);
        cuerpo.add("timestamp", marca);
        cuerpo.add("folder", carpeta);
        cuerpo.add("signature", firmar("folder=" + carpeta + "&timestamp=" + marca));

        Map<?, ?> respuesta = enviar("/image/upload", cuerpo, MediaType.MULTIPART_FORM_DATA);
        Object url = respuesta.get("secure_url");
        Object id = respuesta.get("public_id");
        if (url == null || id == null) {
            throw new AlmacenFotosNoDisponibleException("Cloudinary respondió sin el enlace de la foto");
        }
        return new FotoGuardada(url.toString(), id.toString());
    }

    @Override
    public void borrar(String idPublico) {
        exigirConfiguracion();
        String marca = marcaDeTiempo();
        var cuerpo = new LinkedMultiValueMap<String, String>();
        cuerpo.add("public_id", idPublico);
        cuerpo.add("api_key", apiKey);
        cuerpo.add("timestamp", marca);
        cuerpo.add("signature", firmar("public_id=" + idPublico + "&timestamp=" + marca));

        Object resultado = enviar("/image/destroy", cuerpo, MediaType.APPLICATION_FORM_URLENCODED).get("result");
        // «ok» la borró; «not found» ya no estaba: en ambos casos la foto no queda
        if (!"ok".equals(resultado) && !"not found".equals(resultado)) {
            throw new AlmacenFotosNoDisponibleException("Cloudinary no confirmó el borrado de la foto");
        }
    }

    private Map<?, ?> enviar(String ruta, Object cuerpo, MediaType tipo) {
        try {
            Map<?, ?> respuesta = cliente.post().uri(ruta)
                    .contentType(tipo)
                    .body(cuerpo).retrieve().body(Map.class);
            if (respuesta == null) {
                throw new AlmacenFotosNoDisponibleException("Cloudinary respondió vacío");
            }
            return respuesta;
        } catch (RestClientResponseException e) {
            // Solo el código: el cuerpo del error puede repetir parámetros de la petición
            throw new AlmacenFotosNoDisponibleException("Cloudinary rechazó la petición (HTTP "
                    + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            throw new AlmacenFotosNoDisponibleException("No se pudo contactar a Cloudinary", e);
        }
    }

    private void exigirConfiguracion() {
        if (vacio(cloudName) || vacio(apiKey) || vacio(apiSecret)) {
            throw new AlmacenFotosNoDisponibleException("Las fotos no están configuradas: faltan "
                    + "CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY o CLOUDINARY_API_SECRET");
        }
    }

    private String marcaDeTiempo() {
        return Long.toString(reloj.instant().getEpochSecond());
    }

    /** SHA-1 en hexadecimal de «parámetros ordenados por nombre» + secreto, como pide Cloudinary. */
    private String firmar(String parametros) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest((parametros + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
