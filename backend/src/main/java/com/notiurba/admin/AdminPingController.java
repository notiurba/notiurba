package com.notiurba.admin;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Marcador de posición: confirma que /api/admin/** exige ROLE_ADMIN.
 * Los controladores reales de administración llegan en el Sprint 3.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminPingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("estado", "ok");
    }
}
