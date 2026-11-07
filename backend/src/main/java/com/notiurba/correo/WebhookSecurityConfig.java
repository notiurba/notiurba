package com.notiurba.correo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

/** Cadena limitada al webhook firmado; no cambia el registro ni la autenticación del ciudadano. */
@Configuration
public class WebhookSecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain webhook(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/webhooks/resend")
            .csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.anyRequest().permitAll()).build();
    }
}
