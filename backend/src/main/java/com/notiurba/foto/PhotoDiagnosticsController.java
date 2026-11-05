package com.notiurba.foto;

import java.io.IOException;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Synthetic test resources only. Existing SecurityConfig enforces ROLE_ADMIN. */
@RestController
@RequestMapping("/api/admin/diagnostics/photos")
@ConditionalOnProperty(name = "notiurba.diagnostics.photos-enabled", havingValue = "true")
public class PhotoDiagnosticsController {
    private final CloudinaryPhotoStore store;
    public PhotoDiagnosticsController(CloudinaryPhotoStore store) { this.store = store; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            if (file.getSize() > JpegValidator.MAX_BYTES) throw new IllegalArgumentException("Máximo 1 MB");
            // Bound reads even if a MultipartFile implementation reports an incorrect size.
            byte[] bytes;
            try (var input = file.getInputStream()) { bytes = input.readNBytes(JpegValidator.MAX_BYTES + 1); }
            UUID id = store.upload(bytes);
            return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(Map.of("id", id, "recoveryEndpoint", "/api/admin/diagnostics/photos/" + id + "/link"));
        } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Almacenamiento no disponible"); }
    }
    @GetMapping("/{id}/link")
    public ResponseEntity<Map<String,String>> link(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("url", store.recoveryLink(id).toString(), "expiresInSeconds", "60"));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Recuperacion no disponible");
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try { store.delete(id); return ResponseEntity.noContent().build(); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Eliminación no confirmada"); }
    }
}
