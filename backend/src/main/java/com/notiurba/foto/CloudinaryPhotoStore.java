package com.notiurba.foto;

import java.net.URI;
import java.time.Clock;
import java.util.*;
import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Isolated synthetic diagnostics. SDK owns REST routes and request signatures. */
@Service
@ConditionalOnProperty(name = "notiurba.diagnostics.photos-enabled", havingValue = "true")
public class CloudinaryPhotoStore {
    static final String PREFIX = "notiurba/edt51/";
    private final Cloudinary cloudinary;
    private final Clock clock;
    private final JpegValidator validator = new JpegValidator();

    @Autowired
    public CloudinaryPhotoStore(@Value("${CLOUDINARY_CLOUD_NAME}") String cloud,
            @Value("${CLOUDINARY_API_KEY}") String key,
            @Value("${CLOUDINARY_API_SECRET}") String secret) {
        this(configuredClient(cloud, key, secret), Clock.systemUTC());
    }

    // Package-private injection for SDK contract tests; no configurable remote host.
    CloudinaryPhotoStore(Cloudinary cloudinary, Clock clock) {
        this.cloudinary = Objects.requireNonNull(cloudinary);
        this.clock = Objects.requireNonNull(clock);
    }

    private static Cloudinary configuredClient(String cloud, String key, String secret) {
        if (cloud == null || !cloud.matches("[a-zA-Z0-9_-]+")
                || key == null || key.isBlank() || secret == null || secret.isBlank())
            throw new IllegalArgumentException("Configuracion Cloudinary incompleta");
        return new Cloudinary(new HashMap<>(Map.of("cloud_name", cloud, "api_key", key,
            "api_secret", secret, "secure", true, "signature_algorithm", "SHA256", "timeout", 30)));
    }

    public UUID upload(byte[] original) {
        byte[] sanitized = validator.validateAndSanitize(original);
        UUID id = UUID.randomUUID();
        String publicId = publicId(id);
        Map<?, ?> response;
        try {
            var options = authenticatedOptions();
            options.put("public_id", publicId);
            options.put("overwrite", false);
            options.put("allowed_formats", List.of("jpg"));
            options.put("backup", false); // Synthetic diagnostics must not leave backup copies.
            response = cloudinary.uploader().upload(sanitized, options);
        } catch (Exception e) {
            // A timeout may happen after the provider has stored the asset.
            cleanup(id, "authenticated");
            throw new IllegalStateException("No se confirmo la carga privada");
        }
        if (response == null || !publicId.equals(response.get("public_id"))
                || !"image".equals(response.get("resource_type"))
                || !"authenticated".equals(response.get("type"))
                || !"jpg".equals(response.get("format"))) {
            cleanup(id, "authenticated");
            // Only this call's exact random ID, never another provider-supplied ID.
            if (response != null && publicId.equals(response.get("public_id"))
                    && "image".equals(response.get("resource_type"))
                    && ("upload".equals(response.get("type")) || "private".equals(response.get("type"))))
                cleanup(id, response.get("type").toString());
            throw new IllegalStateException("El proveedor no confirmo almacenamiento authenticated");
        }
        return id;
    }

    public URI recoveryLink(UUID id) {
        try {
            var options = authenticatedOptions();
            options.put("attachment", false);
            options.put("expires_at", clock.instant().plusSeconds(60).getEpochSecond());
            URI uri = URI.create(cloudinary.privateDownload(publicId(id), "jpg", options));
            if (!"https".equals(uri.getScheme()) || !"api.cloudinary.com".equals(uri.getHost()))
                throw new IllegalStateException("Enlace no seguro");
            return uri;
        } catch (Exception e) {
            // Do not attach SDK errors: they may contain signed URLs or credentials.
            throw new IllegalStateException("No se pudo generar el enlace temporal");
        }
    }

    public void delete(UUID id) {
        destroy(id, "authenticated");
    }

    private void destroy(UUID id, String type) {
        try {
            var options = authenticatedOptions();
            options.put("type", type);
            options.put("invalidate", true);
            Object result = cloudinary.uploader().destroy(publicId(id), options).get("result");
            if (!"ok".equals(result) && !"not found".equals(result))
                throw new IllegalStateException("Borrado no confirmado");
        } catch (Exception e) {
            throw new IllegalStateException("No se confirmo la eliminacion");
        }
    }

    private void cleanup(UUID id, String type) {
        try { destroy(id, type); }
        catch (IllegalStateException e) {
            // ID is non-sensitive and permits operator cleanup; never include vendor errors.
            System.getLogger(CloudinaryPhotoStore.class.getName()).log(System.Logger.Level.WARNING,
                "Limpieza pendiente del recurso sintetico {0}, tipo {1}", publicId(id), type);
        }
    }

    private static HashMap<String,Object> authenticatedOptions() {
        return new HashMap<>(Map.of("resource_type", "image", "type", "authenticated"));
    }
    private static String publicId(UUID id) { return PREFIX + Objects.requireNonNull(id); }
}
