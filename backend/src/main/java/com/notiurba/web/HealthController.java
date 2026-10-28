package com.notiurba.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        try (Connection conexion = dataSource.getConnection()) {
            if (conexion.isValid(2)) {
                return ResponseEntity.ok(Map.of("estado", "ok"));
            }
        } catch (SQLException e) {
            // la BD no responde: se reporta como no disponible abajo
        }
        return ResponseEntity.status(503).body(Map.of("estado", "bd_no_disponible"));
    }
}
