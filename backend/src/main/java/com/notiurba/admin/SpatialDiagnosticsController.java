package com.notiurba.admin;

import java.util.Map;
import com.notiurba.geo.SpatialProbe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/diagnostics")
@ConditionalOnProperty(name = "notiurba.diagnostics.enabled", havingValue = "true")
public class SpatialDiagnosticsController {
    private final SpatialProbe probe;
    public SpatialDiagnosticsController(SpatialProbe probe) { this.probe = probe; }
    @GetMapping("/spatial")
    public ResponseEntity<Map<String,Object>> spatial() {
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(probe.check()); }
        catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Diagnóstico espacial no disponible");
        }
    }
}
