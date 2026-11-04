package com.notiurba.web;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /** estado: la API responde; baseDeDatos: resultado del SELECT 1. */
    public record HealthResponse(String estado, String baseDeDatos) {
    }

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/health")
    public ResponseEntity<HealthResponse> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(new HealthResponse("ok", "ok"));
        } catch (DataAccessException e) {
            // la BD no responde: 503 para que Render y las pruebas lo detecten
            return ResponseEntity.status(503).body(new HealthResponse("error", "error"));
        }
    }
}
